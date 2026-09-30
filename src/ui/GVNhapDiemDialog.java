package ui;

import exception.BusinessLogicException;
import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Dialog nhap diem cho toan bo SV trong 1 lop hoc phan.
 * GV chi sua duoc 3 cot: Diem chuyen can / Diem giua ky / Diem cuoi ky.
 * Diem tong ket + Trang thai duoc TU DONG TINH o tang DAO (khong cho GV go tay,
 * tranh sai lech cong thuc giua cac lop/GV khac nhau).
 */
public class GVNhapDiemDialog extends JDialog {

    private final StudentManagerService service;
    private final String maGV;
    private final String maLHP;
    private final String tenHK;
    private DefaultTableModel tableModel;
    private JTable table;
    private TableRowSorter<DefaultTableModel> sorter;

    public GVNhapDiemDialog(Window owner, StudentManagerService service, String maGV, String maLHP, String tenMonHienThi, String tenHK) {
        super(owner, "Nhập điểm - " + tenMonHienThi, ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.maGV = maGV;
        this.maLHP = maLHP;
        this.tenHK = tenHK;

        setSize(860, 560);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIUtils.BG_APP);

        buildUI(tenMonHienThi);
    }

    private void buildUI(String tenMonHienThi) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtils.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIUtils.BORDER), new EmptyBorder(16, 22, 16, 22)));
        JLabel lblTitle = new JLabel("Bảng điểm lớp " + maLHP + " - " + tenMonHienThi);
        lblTitle.setFont(UIUtils.FONT_TITLE);
        header.add(lblTitle, BorderLayout.WEST);

        JTextField txtTimKiem = new JTextField(16);
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Tìm theo MSSV/tên...");
        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            private void locLai() {
                String tuKhoa = txtTimKiem.getText().trim();
                if (tuKhoa.isEmpty()) { sorter.setRowFilter(null); return; }
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(tuKhoa), 0, 1));
            }
            @Override public void insertUpdate(DocumentEvent e) { locLai(); }
            @Override public void removeUpdate(DocumentEvent e) { locLai(); }
            @Override public void changedUpdate(DocumentEvent e) { locLai(); }
        });
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchBox.setBackground(UIUtils.WHITE);
        searchBox.add(new JLabel("Tìm SV:"));
        searchBox.add(txtTimKiem);
        header.add(searchBox, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] columns = {"MSSV", "Họ tên", "Chuyên cần", "Giữa kỳ", "Cuối kỳ", "Tổng kết", "Trạng thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2 || col == 3 || col == 4; // chỉ 3 cột điểm thành phần được sửa
            }
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 2 || col == 3 || col == 4 || col == 5) return Double.class;
                return String.class;
            }
        };

        loadData();

        table = new JTable(tableModel);
        UIUtils.styleTable(table);
        table.setRowHeight(38);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        int[] widths = {90, 170, 90, 80, 80, 90, 130};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIUtils.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIUtils.BORDER), new EmptyBorder(14, 22, 14, 22)));

        JLabel lblGhiChu = new JLabel("Điểm tổng kết = Chuyên cần 10% + Giữa kỳ 30% + Cuối kỳ 60% (tự động tính).");
        lblGhiChu.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblGhiChu.setForeground(UIUtils.TEXT_MUTED);
        footer.add(lblGhiChu, BorderLayout.WEST);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBox.setBackground(UIUtils.WHITE);
        JButton btnHuy = UIUtils.createSecondaryBtn("Đóng");
        btnHuy.addActionListener(e -> dispose());
        JButton btnIn = UIUtils.createSecondaryBtn("book", "In bảng điểm");
        btnIn.addActionListener(e -> inBangDiem(tenMonHienThi, table));
        JButton btnXuatExcel = UIUtils.createSecondaryBtn("download", "Xuất Excel");
        btnXuatExcel.addActionListener(e -> xuatExcel(tenMonHienThi));
        JButton btnLuu = UIUtils.createPrimaryBtn("check", "Lưu toàn bộ điểm");
        btnLuu.addActionListener(e -> luuDiem());
        btnBox.add(btnHuy);
        btnBox.add(btnIn);
        btnBox.add(btnXuatExcel);
        btnBox.add(btnLuu);
        footer.add(btnBox, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);
    }

    private void loadData() {
        tableModel.setRowCount(0);
        List<Object[]> ds = service.getSinhVienTrongLopHP(maLHP);
        for (Object[] row : ds) {
            // row: MaSV, HoTen, DiemChuyenCan, DiemGiuaKy, DiemCuoiKy, DiemTongKet, TrangThai
            tableModel.addRow(new Object[]{ row[0], row[1], row[2], row[3], row[4], row[5], row[6] });
        }
        if (ds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lớp học phần này chưa có sinh viên đăng ký.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void luuDiem() {
        int soLuong = tableModel.getRowCount();
        int thanhCong = 0;
        StringBuilder loi = new StringBuilder();

        for (int i = 0; i < soLuong; i++) {
            String maSV = (String) tableModel.getValueAt(i, 0);
            try {
                double cc = toDouble(tableModel.getValueAt(i, 2));
                double gk = toDouble(tableModel.getValueAt(i, 3));
                double ck = toDouble(tableModel.getValueAt(i, 4));
                service.capNhatDiemSinhVien(maGV, maSV, maLHP, cc, gk, ck);
                thanhCong++;
            } catch (BusinessLogicException ex) {
                loi.append("- ").append(maSV).append(": ").append(ex.getMessage()).append("\n");
            }
        }

        if (loi.length() == 0) {
            JOptionPane.showMessageDialog(this, "Đã lưu điểm cho " + thanhCong + " sinh viên!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Lưu thành công " + thanhCong + "/" + soLuong + " SV.\nLỗi:\n" + loi, "Có lỗi xảy ra", JOptionPane.WARNING_MESSAGE);
        }
        loadData(); // nạp lại để hiện Điểm tổng kết + Trạng thái vừa tính
        tableModel.fireTableDataChanged();
    }

    private void inBangDiem(String tenMon, JTable table) {
        try {
            java.text.MessageFormat header = new java.text.MessageFormat("Bảng điểm lớp " + maLHP + " - " + tenMon);
            java.text.MessageFormat footer = new java.text.MessageFormat("Trang {0}");
            boolean daIn = table.print(JTable.PrintMode.FIT_WIDTH, header, footer);
            if (!daIn) {
                // Nguoi dung bam Cancel o hop thoai in - khong bao loi, chi bo qua.
            }
        } catch (java.awt.print.PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi in: " + ex.getMessage(), "Có lỗi xảy ra", JOptionPane.WARNING_MESSAGE);
        }
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        try { return Double.parseDouble(value.toString().trim().replace(",", ".")); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private void xuatExcel(String tenMon) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("BangDiem_" + maLHP + ".xlsx"));
        int ketQua = chooser.showSaveDialog(this);
        if (ketQua != JFileChooser.APPROVE_OPTION) return;

        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".xlsx")) path += ".xlsx";

        try {
            service.xuatBangDiemExcel(maLHP, tenMon, tenHK, path);
            JOptionPane.showMessageDialog(this, "Xuất file thành công!\n" + path, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (BusinessLogicException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Có lỗi xảy ra", JOptionPane.WARNING_MESSAGE);
        }
    }
}
