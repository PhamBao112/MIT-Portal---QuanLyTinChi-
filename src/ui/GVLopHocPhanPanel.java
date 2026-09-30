package ui;

import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Man hinh "Lop hoc phan" cua Giang vien: liet ke cac lop GV nay dang day,
 * double-click 1 dong (hoac bam nut) de mo dialog nhap diem cho lop do.
 * Co combo loc theo Hoc ky vi 1 GV thuong day nhieu hoc ky cung luc, danh
 * sach phang se rat lan lon neu khong loc.
 */
public class GVLopHocPhanPanel extends JPanel {

    private static final String TAT_CA_HOC_KY = "Tất cả học kỳ";

    private final StudentManagerService service;
    private final String maGV;
    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<String> cbHocKy;
    private List<Object[]> rawData = new ArrayList<>();      // toan bo lop GV nay day (chua loc)
    private List<Object[]> displayedData = new ArrayList<>(); // dung y voi cac dong dang hien trong bang

    public GVLopHocPhanPanel(StudentManagerService service, String maGV) {
        this.service = service;
        this.maGV = maGV;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(10, 0, 0, 0));

        buildUI();
    }

    private void buildUI() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UIUtils.BG_APP);
        JLabel lblTitle = new JLabel("Lớp Học Phần Đang Giảng Dạy");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topRight.setBackground(UIUtils.BG_APP);
        topRight.add(new JLabel("Học kỳ:"));
        cbHocKy = new JComboBox<>();
        cbHocKy.addActionListener(e -> applyFilter());
        topRight.add(cbHocKy);
        JButton btnRefresh = UIUtils.createSecondaryBtn("refresh", "Tải lại");
        btnRefresh.addActionListener(e -> loadData());
        topRight.add(btnRefresh);
        topBar.add(topRight, BorderLayout.EAST);

        JPanel tableContainer = UIUtils.createCardShell("Danh sách lớp học phần", UIUtils.MIT_RED);

        String[] columns = {"Mã LHP", "Môn học", "Số TC", "Học kỳ", "Sĩ số / Sức chứa", "Lịch học", "Phòng"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);
        UIUtils.styleTable(table);
        table.setRowHeight(42);

        int[] widths = {90, 220, 60, 140, 130, 130, 90};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) moNhapDiem();
            }
        });

        loadData();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        tableContainer.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIUtils.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIUtils.BORDER), new EmptyBorder(12, 20, 12, 20)));
        JLabel lblHint = new JLabel("Nhấp đúp vào 1 lớp (hoặc chọn rồi bấm nút) để nhập điểm cho sinh viên.");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblHint.setForeground(UIUtils.TEXT_MUTED);
        footer.add(lblHint, BorderLayout.WEST);

        JButton btnDiemDanh = UIUtils.createSecondaryBtn("calendar", "Điểm danh lớp đã chọn");
        btnDiemDanh.addActionListener(e -> moDiemDanh());
        JButton btnThongKe = UIUtils.createSecondaryBtn("chart", "Thống kê điểm danh");
        btnThongKe.addActionListener(e -> moThongKeDiemDanh());
        JButton btnSoDauBai = UIUtils.createSecondaryBtn("book", "Sổ đầu bài");
        btnSoDauBai.addActionListener(e -> moSoDauBai());
        JButton btnPhoDiem = UIUtils.createSecondaryBtn("chart", "Xem phổ điểm");
        btnPhoDiem.addActionListener(e -> moPhoDiem());
        JButton btnNhapDiem = UIUtils.createPrimaryBtn("edit", "Nhập điểm lớp đã chọn");
        btnNhapDiem.addActionListener(e -> moNhapDiem());
        JPanel footerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footerRight.setBackground(UIUtils.WHITE);
        footerRight.add(btnDiemDanh);
        footerRight.add(btnThongKe);
        footerRight.add(btnSoDauBai);
        footerRight.add(btnPhoDiem);
        footerRight.add(btnNhapDiem);
        footer.add(footerRight, BorderLayout.EAST);
        tableContainer.add(footer, BorderLayout.SOUTH);

        add(topBar, BorderLayout.NORTH);
        add(tableContainer, BorderLayout.CENTER);
    }

    private void loadData() {
        rawData = service.getLopHocPhanByGV(maGV);

        // Nap lai danh sach hoc ky trong combo (giu nguyen lua chon cu neu con ton tai)
        String dangChon = (String) cbHocKy.getSelectedItem();
        LinkedHashSet<String> dsHocKy = new LinkedHashSet<>();
        dsHocKy.add(TAT_CA_HOC_KY);
        for (Object[] row : rawData) {
            String tenHK = row[4] != null ? (String) row[4] : (String) row[3];
            dsHocKy.add(tenHK);
        }
        cbHocKy.removeAllItems();
        for (String hk : dsHocKy) cbHocKy.addItem(hk);
        if (dangChon != null && dsHocKy.contains(dangChon)) {
            cbHocKy.setSelectedItem(dangChon);
        } else {
            cbHocKy.setSelectedIndex(0); // mac dinh "Tat ca hoc ky"
        }

        applyFilter();
    }

    // Loc rawData theo hoc ky dang chon trong combo, do lai bang + dong bo displayedData
    // (displayedData phai dung thu tu voi cac dong tren bang de cac ham lay theo row-index
    // - moNhapDiem, moDiemDanh,... - khong bi lech du lieu).
    private void applyFilter() {
        if (tableModel == null) return; // combo con goi actionListener trong luc dang khoi tao
        String hocKyChon = (String) cbHocKy.getSelectedItem();

        tableModel.setRowCount(0);
        displayedData = new ArrayList<>();

        for (Object[] row : rawData) {
            String tenHK = row[4] != null ? (String) row[4] : (String) row[3];
            if (hocKyChon != null && !hocKyChon.equals(TAT_CA_HOC_KY) && !hocKyChon.equals(tenHK)) continue;

            String maLHP = (String) row[0];
            String tenMon = (String) row[1];
            int soTC = (int) row[2];
            int sucChua = (int) row[5];
            int siSo = (int) row[6];
            String thu = row[7] != null ? (String) row[7] : "Chưa xếp";
            String tiet = row[8] != null ? (String) row[8] : "";
            String phong = row[9] != null ? (String) row[9] : "Chưa xếp";
            String lichHoc = tiet.isEmpty() ? thu : (thu + ", tiết " + tiet);

            tableModel.addRow(new Object[]{ maLHP, tenMon, soTC, tenHK, siSo + " / " + sucChua, lichHoc, phong });
            displayedData.add(row);
        }
    }

    private void moNhapDiem() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 lớp học phần trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String maLHP = (String) tableModel.getValueAt(row, 0);
        String tenMon = (String) tableModel.getValueAt(row, 1);
        String tenHK = (String) tableModel.getValueAt(row, 3);

        Window owner = SwingUtilities.getWindowAncestor(this);
        GVNhapDiemDialog dialog = new GVNhapDiemDialog(owner, service, maGV, maLHP, tenMon, tenHK);
        dialog.setVisible(true);
    }

    private void moDiemDanh() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 lớp học phần trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String maLHP = (String) tableModel.getValueAt(row, 0);
        String tenMon = (String) tableModel.getValueAt(row, 1);
        java.util.Date ngayBatDau = row < displayedData.size() ? (java.util.Date) displayedData.get(row)[10] : null;
        java.util.Date ngayKetThuc = row < displayedData.size() ? (java.util.Date) displayedData.get(row)[11] : null;

        if (ngayBatDau == null || ngayKetThuc == null) {
            JOptionPane.showMessageDialog(this, "Lớp này chưa được xếp lịch (chưa có ngày bắt đầu/kết thúc học), không thể điểm danh lúc này.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        GVDiemDanhDialog dialog = new GVDiemDanhDialog(owner, service, maGV, maLHP, tenMon, ngayBatDau, ngayKetThuc);
        dialog.setVisible(true);
    }

    private void moThongKeDiemDanh() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 lớp học phần trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String maLHP = (String) tableModel.getValueAt(row, 0);
        String tenMon = (String) tableModel.getValueAt(row, 1);

        Window owner = SwingUtilities.getWindowAncestor(this);
        GVThongKeDiemDanhDialog dialog = new GVThongKeDiemDanhDialog(owner, service, maLHP, tenMon);
        dialog.setVisible(true);
    }

    private void moSoDauBai() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 lớp học phần trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String maLHP = (String) tableModel.getValueAt(row, 0);
        String tenMon = (String) tableModel.getValueAt(row, 1);
        java.util.Date ngayBatDau = row < displayedData.size() ? (java.util.Date) displayedData.get(row)[10] : null;
        java.util.Date ngayKetThuc = row < displayedData.size() ? (java.util.Date) displayedData.get(row)[11] : null;

        if (ngayBatDau == null || ngayKetThuc == null) {
            JOptionPane.showMessageDialog(this, "Lớp này chưa được xếp lịch (chưa có ngày bắt đầu/kết thúc học), chưa thể ghi sổ đầu bài.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        GVSoDauBaiDialog dialog = new GVSoDauBaiDialog(owner, service, maGV, maLHP, tenMon, ngayBatDau, ngayKetThuc);
        dialog.setVisible(true);
    }

    private void moPhoDiem() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 lớp học phần trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String maLHP = (String) tableModel.getValueAt(row, 0);
        String tenMon = (String) tableModel.getValueAt(row, 1);

        Window owner = SwingUtilities.getWindowAncestor(this);
        GVPhoDiemDialog dialog = new GVPhoDiemDialog(owner, service, maLHP, tenMon);
        dialog.setVisible(true);
    }
}
