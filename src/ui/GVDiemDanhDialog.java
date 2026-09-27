package ui;

import exception.BusinessLogicException;
import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dialog diem danh SV cho 1 lop hoc phan theo 1 ngay cu the.
 * GV chon ngay (gioi han trong khoang NgayBatDauHoc - NgayKetThucHoc cua lop),
 * bang se tu nap lai diem danh da luu cho ngay do (neu co) de sua, hoac mac dinh
 * "Co mat" cho tat ca neu chua diem danh ngay nay bao gio.
 */
public class GVDiemDanhDialog extends JDialog {

    private final StudentManagerService service;
    private final String maGV;
    private final String maLHP;
    private DefaultTableModel tableModel;
    private JSpinner dateSpinner;
    private static final String[] TRANG_THAI_OPTIONS = {"Có mặt", "Vắng", "Vắng có phép"};

    public GVDiemDanhDialog(Window owner, StudentManagerService service, String maGV, String maLHP,
                             String tenMonHienThi, java.util.Date ngayBatDau, java.util.Date ngayKetThuc) {
        super(owner, "Điểm danh - " + tenMonHienThi, ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.maGV = maGV;
        this.maLHP = maLHP;

        setSize(720, 560);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIUtils.BG_APP);

        buildUI(tenMonHienThi, ngayBatDau, ngayKetThuc);
    }

    private void buildUI(String tenMonHienThi, java.util.Date ngayBatDau, java.util.Date ngayKetThuc) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtils.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIUtils.BORDER), new EmptyBorder(16, 22, 10, 22)));

        JLabel lblTitle = new JLabel("Điểm danh lớp " + maLHP + " - " + tenMonHienThi);
        lblTitle.setFont(UIUtils.FONT_TITLE);
        header.add(lblTitle, BorderLayout.WEST);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        datePanel.setBackground(UIUtils.WHITE);
        datePanel.add(new JLabel("Ngày học:"));

        java.util.Date min = ngayBatDau != null ? ngayBatDau : new java.util.Date(0);
        java.util.Date max = ngayKetThuc != null ? ngayKetThuc : new java.util.Date();
        java.util.Date initial = new java.util.Date();
        if (initial.before(min)) initial = min;
        if (initial.after(max)) initial = max;

        SpinnerDateModel dateModel = new SpinnerDateModel(initial, min.before(max) ? min : max, min.before(max) ? max : min, java.util.Calendar.DAY_OF_MONTH);
        dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        dateSpinner.setPreferredSize(new Dimension(130, 30));
        dateSpinner.addChangeListener(e -> loadData());
        datePanel.add(dateSpinner);
        header.add(datePanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        String[] columns = {"MSSV", "Họ tên", "Trạng thái", "Ghi chú"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return col == 2 || col == 3; }
        };

        JTable table = new JTable(tableModel);
        UIUtils.styleTable(table);
        table.setRowHeight(38);
        JComboBox<String> comboEditor = new JComboBox<>(TRANG_THAI_OPTIONS);
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(comboEditor));

        loadData();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIUtils.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIUtils.BORDER), new EmptyBorder(14, 22, 14, 22)));

        JLabel lblHint = new JLabel("Mặc định \"Có mặt\" - chỉ cần đổi trạng thái cho SV vắng.");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblHint.setForeground(UIUtils.TEXT_MUTED);
        footer.add(lblHint, BorderLayout.WEST);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBox.setBackground(UIUtils.WHITE);
        JButton btnHuy = UIUtils.createSecondaryBtn("Đóng");
        btnHuy.addActionListener(e -> dispose());
        JButton btnLuu = UIUtils.createPrimaryBtn("check", "Lưu điểm danh");
        btnLuu.addActionListener(e -> luuDiemDanh());
        btnBox.add(btnHuy);
        btnBox.add(btnLuu);
        footer.add(btnBox, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);
    }

    private Date getSelectedSqlDate() {
        java.util.Date d = (java.util.Date) dateSpinner.getValue();
        return new Date(d.getTime());
    }

    private void loadData() {
        tableModel.setRowCount(0);
        Date ngay = getSelectedSqlDate();
        List<Object[]> svList = service.getSinhVienTrongLopHP(maLHP);
        Map<String, Object[]> daDiemDanh = service.getDiemDanhTheoNgay(maLHP, ngay);

        for (Object[] sv : svList) {
            String maSV = (String) sv[0];
            String hoTen = (String) sv[1];
            String trangThai = "Có mặt";
            String ghiChu = "";
            if (daDiemDanh.containsKey(maSV)) {
                Object[] info = daDiemDanh.get(maSV);
                trangThai = (String) info[0];
                ghiChu = info[1] != null ? (String) info[1] : "";
            }
            tableModel.addRow(new Object[]{ maSV, hoTen, trangThai, ghiChu });
        }
    }

    private void luuDiemDanh() {
        List<Object[]> danhSach = new ArrayList<>();
        int n = tableModel.getRowCount();
        for (int i = 0; i < n; i++) {
            String maSV = (String) tableModel.getValueAt(i, 0);
            String trangThai = (String) tableModel.getValueAt(i, 2);
            String ghiChu = (String) tableModel.getValueAt(i, 3);
            danhSach.add(new Object[]{ maSV, trangThai, ghiChu });
        }
        try {
            service.luuDiemDanh(maGV, maLHP, getSelectedSqlDate(), danhSach);
            JOptionPane.showMessageDialog(this, "Đã lưu điểm danh cho " + n + " sinh viên!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (BusinessLogicException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Có lỗi xảy ra", JOptionPane.WARNING_MESSAGE);
        }
    }
}
