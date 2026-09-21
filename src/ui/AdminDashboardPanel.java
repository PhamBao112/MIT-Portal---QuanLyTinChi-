package ui;

import config.DBConnect;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminDashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private String currentMaHK = "";
    private DefaultTableModel tableModel;
    private JPanel statsRow;
    private JLabel lblTableTitle;
    private JTable table;

    public AdminDashboardPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(UIUtils.BG_APP);

        // --- 1. KHU VỰC THẺ THỐNG KÊ (TOP) ---
        statsRow = new JPanel(new GridLayout(1, 4, 20, 0));
        statsRow.setBackground(UIUtils.BG_APP);
        statsRow.setPreferredSize(new Dimension(0, 140));

        // --- 2. KHU VỰC BẢNG DANH SÁCH (BOTTOM) - card shell dùng chung ---
        JPanel tablePanel = UIUtils.createCardShell("Lớp học phần đang mở (Kỳ này)", UIUtils.MIT_RED);
        lblTableTitle = (JLabel) ((JPanel) tablePanel.getComponent(0)).getComponent(0);

        String[] columns = {"Mã LHP", "Tên Môn", "TC", "Lịch học", "Phòng", "Sĩ số", "Trạng thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UIUtils.styleTable(table);

        table.setShowGrid(true);
        table.setGridColor(UIUtils.BORDER);
        table.setIntercellSpacing(new Dimension(1, 1));

        ZebraRenderer zebra = new ZebraRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        tablePanel.add(scrollTable, BorderLayout.CENTER);

        add(statsRow, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
    }

    // ========================================================
    // HÀM ĐƯỢC GỌI TỪ ADMINPANEL KHI CHỌN COMBOBOX HỌC KỲ
    // ========================================================
    public void updateData(String maHK) {
        this.currentMaHK = maHK;

        int tongSV = 0;
        int tongLHP = 0;
        double doanhThu = 0.0;
        int svNoTien = 0;

        tableModel.setRowCount(0);

        try (Connection conn = DBConnect.getConnection()) {
            if (conn == null) return;

            String sqlSV = "SELECT COUNT(*) FROM SINH_VIEN";
            try (PreparedStatement ps = conn.prepareStatement(sqlSV); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) tongSV = rs.getInt(1);
            }

            String sqlLHP = "SELECT COUNT(*) FROM LOP_HOC_PHAN WHERE MaHK = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlLHP)) {
                ps.setString(1, maHK);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) tongLHP = rs.getInt(1);
                }
            }

            String sqlTien = "SELECT SUM(TongTienPhaiDong) FROM CONG_NO_HOC_PHI WHERE MaHK = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlTien)) {
                ps.setString(1, maHK);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) doanhThu = rs.getDouble(1);
                }
            }

            String sqlNo = "SELECT COUNT(DISTINCT MaSV) FROM CONG_NO_HOC_PHI WHERE MaHK = ? AND TrangThai != N'Đã hoàn thành'";
            try (PreparedStatement ps = conn.prepareStatement(sqlNo)) {
                ps.setString(1, maHK);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) svNoTien = rs.getInt(1);
                }
            }

            String sqlTable = "SELECT lhp.MaLHP, m.TenMon, m.SoTinChi, lhp.Thu, lhp.TietHoc, lhp.PhongHoc, lhp.SucChua, " +
                              "(SELECT COUNT(*) FROM KET_QUA_DANG_KY WHERE MaLHP = lhp.MaLHP) as DaDK " +
                              "FROM LOP_HOC_PHAN lhp JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE lhp.MaHK = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlTable)) {
                ps.setString(1, maHK);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int daDk = rs.getInt("DaDK");
                        int sucChua = rs.getInt("SucChua");
                        String lichHoc = "Thứ " + rs.getString("Thu") + " (Tiết " + rs.getString("TietHoc") + ")";
                        String siSo = daDk + " / " + sucChua;
                        String trangThai = daDk >= sucChua ? "Đã đầy" : "Còn chỗ";

                        tableModel.addRow(new Object[]{
                            rs.getString("MaLHP"), rs.getString("TenMon"), rs.getInt("SoTinChi"),
                            lichHoc, rs.getString("PhongHoc"), siSo, trangThai
                        });
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Cập nhật lại UI Thẻ thống kê (V3 - dùng UIUtils.createKpiCard, đồng bộ mockup)
        statsRow.removeAll();
        statsRow.add(UIUtils.createKpiCard("👥", String.format("%,d SV", tongSV), "Tổng sinh viên", new Color(30, 64, 175)));
        statsRow.add(UIUtils.createKpiCard("📚", String.format("%,d Lớp", tongLHP), "Lớp học phần mở", UIUtils.GREEN_500));
        statsRow.add(UIUtils.createKpiCard("💰", String.format("%,.0f Đ", doanhThu), "Doanh thu dự kiến", UIUtils.MIT_ORANGE));
        statsRow.add(UIUtils.createKpiCard("⚠", String.format("%,d SV", svNoTien), "SV nợ học phí", UIUtils.RED_500));

        statsRow.revalidate();
        statsRow.repaint();

        lblTableTitle.setText("Danh sách Lớp học phần đang mở (" + maHK + ")");
    }

    class ZebraRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // Cột Trạng thái (6): hiển thị dạng badge tròn màu thay vì chữ thường
            if (column == 6 && value != null) {
                String text = value.toString();
                int type = text.equals("Đã đầy") ? UIUtils.BADGE_BAD : UIUtils.BADGE_OK;
                JLabel badge = UIUtils.createBadge(text, type);
                badge.setOpaque(false);
                JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                wrap.setBackground(isSelected ? UIUtils.MIT_RED_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
                wrap.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(6, 15, 6, 15)
                ));
                wrap.add(badge);
                return wrap;
            }

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
            } else {
                c.setBackground(UIUtils.MIT_RED_LIGHT);
            }
            c.setForeground(UIUtils.TEXT_MAIN);
            c.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(0, 15, 0, 15)
            ));
            return c;
        }
    }
}
