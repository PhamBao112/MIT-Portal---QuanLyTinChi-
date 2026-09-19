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

public class DashboardPanel extends JPanel {
    private String currentHK = "HK1_2425";
    private String currentHKLabel = "Học kỳ 1 – 2024-2025";

    private String currentMaSV;
    private String hoTen = "...", maLop = "...", maCTDT = "...";
    private String trangThaiHocTap = "Đang học", ngaySinh = "...", gioiTinh = "...";
    private int tinChiTichLuy = 0, tinChiKyNay = 0;
    private double gpa = 0.0, tongNo = 0.0;

    public DashboardPanel(String maSV) {
        this.currentMaSV = maSV;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UIUtils.BG_APP);
        resolveCurrentHocKy();
        buildUI();
    }

    private void resolveCurrentHocKy() {
        try (Connection conn = DBConnect.getConnection()) {
            if (conn == null) return;
            String sql = "SELECT TOP 1 h.MaHK, h.TenHK FROM HOC_KY h " +
                         "JOIN LOP_HOC_PHAN lhp ON lhp.MaHK = h.MaHK " +
                         "JOIN KET_QUA_DANG_KY kq ON kq.MaLHP = lhp.MaLHP " +
                         "WHERE kq.MaSV = ? ORDER BY h.NamHoc DESC, h.MaHK DESC";
            boolean found = false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentMaSV);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    currentHK = rs.getString("MaHK");
                    currentHKLabel = rs.getString("TenHK");
                    found = true;
                }
            }
            if (!found) {
                String sqlLatest = "SELECT TOP 1 MaHK, TenHK FROM HOC_KY ORDER BY NamHoc DESC, MaHK DESC";
                try (PreparedStatement ps2 = conn.prepareStatement(sqlLatest)) {
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        currentHK = rs2.getString("MaHK");
                        currentHKLabel = rs2.getString("TenHK");
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void buildUI() {
        removeAll();
        loadDataFromDatabase();

        add(createStudentProfileCard());
        add(Box.createVerticalStrut(20));
        add(createStatsGrid());
        add(Box.createVerticalStrut(20));
        add(createBottomWrapper());

        revalidate();
        repaint();
    }

    private void loadDataFromDatabase() {
        try (Connection conn = DBConnect.getConnection()) {
            if (conn == null) return;
            String sqlInfo = "SELECT HoTen, MaLop, MaCTDT, TrangThaiHocTap, NgaySinh, GioiTinh FROM SINH_VIEN WHERE MaSV = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlInfo)) {
                ps.setString(1, currentMaSV);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    hoTen = rs.getString("HoTen"); maLop = rs.getString("MaLop"); maCTDT = rs.getString("MaCTDT");
                    trangThaiHocTap = rs.getString("TrangThaiHocTap");

                    String rawDate = rs.getString("NgaySinh");
                    if (rawDate != null && !rawDate.isEmpty()) {
                        try {
                            if (rawDate.contains(" ")) rawDate = rawDate.split(" ")[0];
                            java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(rawDate);
                            ngaySinh = new java.text.SimpleDateFormat("dd/MM/yyyy").format(date);
                        } catch (Exception ex) {
                            ngaySinh = rawDate;
                        }
                    } else {
                        ngaySinh = "Chưa cập nhật";
                    }

                    gioiTinh = rs.getString("GioiTinh") != null ? rs.getString("GioiTinh") : "Chưa cập nhật";
                }
            }
            String sqlDiem = "SELECT " +
                             "SUM(CASE WHEN kq.TrangThai = N'Đạt' THEN CAST(m.SoTinChi AS INT) ELSE 0 END) as TCLuyKe, " +
                             "SUM(CASE WHEN kq.DiemTongKet IS NOT NULL THEN CAST(kq.DiemTongKet AS FLOAT) * CAST(m.SoTinChi AS FLOAT) ELSE 0 END) as TongDiemNhanTC, " +
                             "SUM(CASE WHEN kq.DiemTongKet IS NOT NULL THEN CAST(m.SoTinChi AS FLOAT) ELSE 0 END) as TongTCCoDiem " +
                             "FROM KET_QUA_DANG_KY kq JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE kq.MaSV = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDiem)) {
                ps.setString(1, currentMaSV); ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    tinChiTichLuy = rs.getInt("TCLuyKe");
                    double tongTCCoDiem = rs.getDouble("TongTCCoDiem");
                    double gpa10 = tongTCCoDiem > 0 ? rs.getDouble("TongDiemNhanTC") / tongTCCoDiem : 0.0;
                    gpa = (gpa10 / 10.0) * 4.0;
                }
            }
            String sqlKyNay = "SELECT SUM(CAST(m.SoTinChi AS INT)) as TCKyNay FROM KET_QUA_DANG_KY kq JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE kq.MaSV = ? AND lhp.MaHK = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlKyNay)) {
                ps.setString(1, currentMaSV); ps.setString(2, currentHK); ResultSet rs = ps.executeQuery();
                if (rs.next()) tinChiKyNay = rs.getInt("TCKyNay");
            }
            String sqlNo = "SELECT ISNULL(SUM(CAST(TongTienPhaiDong AS FLOAT) - CAST(SoTienDaDong AS FLOAT)), 0) as ConNo " +
                           "FROM CONG_NO_HOC_PHI WHERE MaSV = ? " +
                           "AND CAST(TongTienPhaiDong AS FLOAT) - CAST(SoTienDaDong AS FLOAT) > 0";
            try (PreparedStatement ps = conn.prepareStatement(sqlNo)) {
                ps.setString(1, currentMaSV); ResultSet rs = ps.executeQuery();
                if (rs.next()) tongNo = rs.getDouble("ConNo");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ========================================================
    // KHU VỰC THỐNG KÊ (KPI CARD DÙNG CHUNG - UIUtils.createKpiCard)
    // ========================================================
    private JPanel createStatsGrid() {
        JPanel gridStats = new JPanel(new GridLayout(1, 4, 15, 0));
        gridStats.setBackground(UIUtils.BG_APP);
        gridStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        gridStats.add(UIUtils.createKpiCard("⭐", tinChiTichLuy + " / 150", "Tín chỉ tích lũy", UIUtils.MIT_RED));
        gridStats.add(UIUtils.createKpiCard("📈", String.format("%.2f", gpa), "Điểm tích lũy (GPA)", UIUtils.MIT_ORANGE));
        gridStats.add(UIUtils.createKpiCard("📖", tinChiKyNay + " TC", "Tín chỉ kỳ này", UIUtils.GREEN_500));
        gridStats.add(UIUtils.createKpiCard("💳", String.format("%,.0f đ", tongNo), "Công nợ hiện tại", new Color(37, 99, 235)));

        return gridStats;
    }

    // ========================================================
    // KHU VỰC BÊN DƯỚI (BẢNG & THÔNG BÁO)
    // ========================================================
    private JPanel createBottomWrapper() {
        JPanel bottomWrapper = new JPanel(new BorderLayout(20, 0));
        bottomWrapper.setBackground(UIUtils.BG_APP);
        bottomWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- BẢNG LỊCH SỬ HỌC TẬP (card shell dùng chung) ---
        JPanel tablePanel = UIUtils.createCardShell("Lịch sử học tập gần đây (" + currentHKLabel + ")", UIUtils.MIT_RED);

        DefaultTableModel model = new DefaultTableModel(new String[]{"Mã HP", "Tên Môn", "Điểm", "Kết quả"}, 0){
            public boolean isCellEditable(int r, int c) { return false; }
        };
        try (Connection conn = DBConnect.getConnection()) {
            String sql = "SELECT lhp.MaMon, m.TenMon, kq.DiemTongKet, kq.TrangThai FROM KET_QUA_DANG_KY kq JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE kq.MaSV = ? AND lhp.MaHK = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentMaSV);
            ps.setString(2, currentHK);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) model.addRow(new Object[]{rs.getString("MaMon"), rs.getString("TenMon"), rs.getDouble("DiemTongKet"), rs.getString("TrangThai")});
        } catch (Exception e) {}

        JTable table = new JTable(model);
        UIUtils.styleTable(table);

        int lastColumnIndex = table.getColumnCount() - 1;
        ZebraRenderer zebra = new ZebraRenderer();
        for (int i = 0; i < lastColumnIndex; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        table.getColumnModel().getColumn(lastColumnIndex).setCellRenderer(new BadgeRenderer());

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        scrollTable.getViewport().setBackground(Color.WHITE);

        tablePanel.add(scrollTable, BorderLayout.CENTER);

        // --- KHUNG NHẮC NHỞ TỪ HỆ THỐNG (card shell dùng chung) ---
        JPanel alertPanel = UIUtils.createCardShell("Nhắc nhở từ hệ thống", UIUtils.MIT_ORANGE);
        alertPanel.setPreferredSize(new Dimension(300, 0));

        JPanel alertList = new JPanel();
        alertList.setLayout(new BoxLayout(alertList, BoxLayout.Y_AXIS));
        alertList.setOpaque(false);
        alertList.setBorder(new EmptyBorder(14, 18, 14, 18));

        String noMsg = (tongNo > 0) ? "Bạn đang còn nợ học phí. Vui lòng thanh toán trước ngày 30/12." : "Bạn đã hoàn thành 100% học phí.";
        alertList.add(createAlertItem("Tài chính", noMsg, tongNo > 0));
        alertList.add(Box.createVerticalStrut(12));
        alertList.add(createAlertItem("Học vụ", "Hệ thống mở cổng đăng ký tín chỉ " + currentHKLabel + ".", false));
        alertList.add(Box.createVerticalGlue());

        alertPanel.add(alertList, BorderLayout.CENTER);

        bottomWrapper.add(tablePanel, BorderLayout.CENTER);
        bottomWrapper.add(alertPanel, BorderLayout.EAST);
        return bottomWrapper;
    }

    private JPanel createStudentProfileCard() {
        JPanel profileCard = UIUtils.roundedCardPanel();
        profileCard.setLayout(new BorderLayout(25, 0));
        profileCard.setBorder(new EmptyBorder(20, 25, 20, 25));
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        profileCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftProfile = new JPanel(new BorderLayout(15, 0));
        leftProfile.setOpaque(false);
        String firstChar = hoTen.length() > 0 && !hoTen.equals("Đang tải...") ? hoTen.substring(0, 1) : "A";
        JLabel lblAvatar = new JLabel(new CircleAvatarIcon(firstChar));

        JPanel namePanel = new JPanel(); namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS)); namePanel.setOpaque(false);
        JLabel lblName = new JLabel(hoTen); lblName.setFont(new Font("Segoe UI", Font.BOLD, 20)); lblName.setForeground(UIUtils.TEXT_MAIN);
        JLabel lblMSSV = new JLabel("MSSV: " + currentMaSV); lblMSSV.setForeground(UIUtils.TEXT_MUTED);
        JLabel lblStatus = UIUtils.createBadge(trangThaiHocTap, UIUtils.BADGE_OK);
        namePanel.add(Box.createVerticalStrut(5)); namePanel.add(lblName); namePanel.add(Box.createVerticalStrut(5)); namePanel.add(lblMSSV); namePanel.add(Box.createVerticalStrut(6)); namePanel.add(lblStatus);

        leftProfile.add(lblAvatar, BorderLayout.WEST); leftProfile.add(namePanel, BorderLayout.CENTER);

        JPanel gridInfo = new JPanel(new GridLayout(3, 2, 20, 10));
        gridInfo.setOpaque(false); gridInfo.setBorder(new MatteBorder(0, 1, 0, 0, UIUtils.BORDER));
        String khoaHoc = (maLop != null && maLop.length() >= 5) ? maLop.substring(0, 5) : "N/A";
        gridInfo.add(createProfileAttr("Ngày sinh:", ngaySinh)); gridInfo.add(createProfileAttr("Khóa học:", khoaHoc));
        gridInfo.add(createProfileAttr("Giới tính:", gioiTinh)); gridInfo.add(createProfileAttr("Bậc đào tạo:", "Đại học"));
        gridInfo.add(createProfileAttr("Lớp học:", maLop)); gridInfo.add(createProfileAttr("Ngành:", maCTDT));

        JPanel rightWrapper = new JPanel(new BorderLayout()); rightWrapper.setOpaque(false); rightWrapper.setBorder(new EmptyBorder(0, 20, 0, 0)); rightWrapper.add(gridInfo, BorderLayout.CENTER);
        profileCard.add(leftProfile, BorderLayout.WEST); profileCard.add(rightWrapper, BorderLayout.CENTER);
        return profileCard;
    }

    private JPanel createProfileAttr(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); p.setOpaque(false);
        JLabel l1 = new JLabel(label + "  "); l1.setForeground(UIUtils.TEXT_MUTED);
        JLabel l2 = new JLabel(value); l2.setFont(new Font("Segoe UI", Font.BOLD, 14)); l2.setForeground(UIUtils.TEXT_MAIN);
        p.add(l1); p.add(l2); return p;
    }

    /** Avatar tròn gradient cam/vàng thay cho ô vuông cũ - đồng bộ với StudentPanel. */
    class CircleAvatarIcon implements Icon {
        private final String initial;
        CircleAvatarIcon(String ch) { this.initial = ch; }
        public int getIconWidth() { return 76; }
        public int getIconHeight() { return 76; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(x, y, UIUtils.MIT_YELLOW, x + 76, y + 76, UIUtils.MIT_ORANGE));
            g2.fillOval(x, y, 76, 76);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 30));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (76 - fm.stringWidth(initial)) / 2;
            int ty = y + (76 + fm.getAscent()) / 2 - 6;
            g2.drawString(initial, tx, ty);
            g2.dispose();
        }
    }

    /** Thẻ nhắc nhở với vạch màu trái - đồng bộ badge palette với UIUtils. */
    private JPanel createAlertItem(String tag, String body, boolean isWarn) {
        Color accentColor = isWarn ? UIUtils.BADGE_BAD_FG : UIUtils.MIT_RED;

        JPanel p = new JPanel(new BorderLayout(15, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(accentColor);
                g2.fillRect(0, 0, 4, getHeight());
                g2.dispose();
            }
        };
        p.setOpaque(true);
        p.setBackground(new Color(248, 250, 252));
        p.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(12, 16, 12, 13)
        ));

        JLabel lTag = UIUtils.createBadge(tag, isWarn ? UIUtils.BADGE_BAD : UIUtils.BADGE_PENDING);

        JLabel lBody = new JLabel("<html><div style='width: 220px; line-height: 1.5;'>" + body + "</div></html>");
        lBody.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lBody.setForeground(UIUtils.TEXT_MAIN);
        lBody.setBorder(new EmptyBorder(8, 0, 0, 0));
        lBody.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);
        JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tagRow.setOpaque(false);
        tagRow.add(lTag);
        tagRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        textCol.add(tagRow);
        textCol.add(lBody);

        p.add(textCol, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    class ZebraRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
            else c.setBackground(UIUtils.MIT_RED_LIGHT);
            c.setForeground(UIUtils.TEXT_MAIN);
            setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(0, 15, 0, 15)));
            return c;
        }
    }

    /** Cột "Kết quả" -> badge tròn màu dùng chung UIUtils, thay cho JLabel tự tô màu ad-hoc. */
    class BadgeRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            wrap.setBackground(isSelected ? UIUtils.MIT_RED_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
            wrap.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(6, 15, 6, 15)));
            if (value != null) {
                String text = value.toString();
                int type = (text.equalsIgnoreCase("Đạt") || text.equalsIgnoreCase("Đã hoàn thành")) ? UIUtils.BADGE_OK
                         : text.toLowerCase().contains("không đạt") ? UIUtils.BADGE_BAD
                         : UIUtils.BADGE_PENDING; // bao gom ca "Chua co diem" (mon dang hoc, chua thi xong)
                wrap.add(UIUtils.createBadge(text, type));
            }
            return wrap;
        }
    }
}
