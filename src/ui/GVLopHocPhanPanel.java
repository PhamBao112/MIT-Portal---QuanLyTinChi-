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
import java.util.List;

/**
 * Man hinh "Lop hoc phan" cua Giang vien: liet ke cac lop GV nay dang day,
 * double-click 1 dong (hoac bam nut) de mo dialog nhap diem cho lop do.
 */
public class GVLopHocPhanPanel extends JPanel {

    private final StudentManagerService service;
    private final String maGV;
    private DefaultTableModel tableModel;
    private JTable table;
    private List<Object[]> rawData = new ArrayList<>();

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

        JButton btnRefresh = UIUtils.createSecondaryBtn("refresh", "Tải lại");
        btnRefresh.addActionListener(e -> loadData());
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topRight.setBackground(UIUtils.BG_APP);
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
        JButton btnNhapDiem = UIUtils.createPrimaryBtn("edit", "Nhập điểm lớp đã chọn");
        btnNhapDiem.addActionListener(e -> moNhapDiem());
        JPanel footerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footerRight.setBackground(UIUtils.WHITE);
        footerRight.add(btnDiemDanh);
        footerRight.add(btnThongKe);
        footerRight.add(btnSoDauBai);
        footerRight.add(btnNhapDiem);
        footer.add(footerRight, BorderLayout.EAST);
        tableContainer.add(footer, BorderLayout.SOUTH);

        add(topBar, BorderLayout.NORTH);
        add(tableContainer, BorderLayout.CENTER);
    }

    private void loadData() {
        tableModel.setRowCount(0);
        rawData = service.getLopHocPhanByGV(maGV);
        for (Object[] row : rawData) {
            // row: MaLHP, TenMon, SoTinChi, MaHK, TenHK, SucChua, SiSo, Thu, TietHoc, PhongHoc, NgayBatDauHoc, NgayKetThucHoc
            String maLHP = (String) row[0];
            String tenMon = (String) row[1];
            int soTC = (int) row[2];
            String tenHK = row[4] != null ? (String) row[4] : (String) row[3];
            int sucChua = (int) row[5];
            int siSo = (int) row[6];
            String thu = row[7] != null ? (String) row[7] : "Chưa xếp";
            String tiet = row[8] != null ? (String) row[8] : "";
            String phong = row[9] != null ? (String) row[9] : "Chưa xếp";

            String lichHoc = tiet.isEmpty() ? thu : (thu + ", tiết " + tiet);

            tableModel.addRow(new Object[]{ maLHP, tenMon, soTC, tenHK, siSo + " / " + sucChua, lichHoc, phong });
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
        java.util.Date ngayBatDau = row < rawData.size() ? (java.util.Date) rawData.get(row)[10] : null;
        java.util.Date ngayKetThuc = row < rawData.size() ? (java.util.Date) rawData.get(row)[11] : null;

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
        java.util.Date ngayBatDau = row < rawData.size() ? (java.util.Date) rawData.get(row)[10] : null;
        java.util.Date ngayKetThuc = row < rawData.size() ? (java.util.Date) rawData.get(row)[11] : null;

        if (ngayBatDau == null || ngayKetThuc == null) {
            JOptionPane.showMessageDialog(this, "Lớp này chưa được xếp lịch (chưa có ngày bắt đầu/kết thúc học), chưa thể ghi sổ đầu bài.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        GVSoDauBaiDialog dialog = new GVSoDauBaiDialog(owner, service, maGV, maLHP, tenMon, ngayBatDau, ngayKetThuc);
        dialog.setVisible(true);
    }
}
