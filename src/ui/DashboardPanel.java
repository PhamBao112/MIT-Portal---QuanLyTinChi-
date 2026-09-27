package ui;

import config.DBConnect;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    // V5 - "Thao tác nhanh" gọi thẳng về đúng hành vi chuyển tab của sidebar (StudentPanel
    // truyền vào), thay vì chỉ là card trang trí, giúp Dashboard thật sự hữu dụng.
    private Runnable onDangKy, onXemDiem, onCongNo, onLichHoc;

    public DashboardPanel(String maSV, Runnable onDangKy, Runnable onXemDiem, Runnable onCongNo, Runnable onLichHoc) {
        this.currentMaSV = maSV;
        this.onDangKy = onDangKy;
        this.onXemDiem = onXemDiem;
        this.onCongNo = onCongNo;
        this.onLichHoc = onLichHoc;
        setLayout(new BorderLayout(0, 20));
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

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);
        topSection.add(createStudentProfileCard());
        topSection.add(Box.createVerticalStrut(20));
        topSection.add(createStatsGrid());

        add(topSection, BorderLayout.NORTH);
        add(createBottomWrapper(), BorderLayout.CENTER);

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
        gridStats.setMaximumSize(new Dimension(6000, 150));
        gridStats.setAlignmentX(Component.LEFT_ALIGNMENT);

        gridStats.add(UIUtils.createKpiCard("star", tinChiTichLuy + " / 150", "Tín chỉ tích lũy", UIUtils.MIT_RED));
        gridStats.add(UIUtils.createKpiCard("trend", String.format("%.2f", gpa), "Điểm tích lũy (GPA)", UIUtils.MIT_ORANGE));
        gridStats.add(UIUtils.createKpiCard("book", tinChiKyNay + " TC", "Tín chỉ kỳ này", UIUtils.GREEN_500));
        gridStats.add(UIUtils.createKpiCard("card", String.format("%,.0f đ", tongNo), "Công nợ hiện tại", new Color(37, 99, 235)));

        return gridStats;
    }

    // ========================================================
    // KHU VỰC BÊN DƯỚI - V5: chia 2 cột, MỖI CARD CAO THEO ĐÚNG NỘI DUNG
    // (không ép giãn hết chiều cao trang -> tránh khoảng trắng rỗng phía dưới khi ít dữ liệu).
    // Cột trái: bảng lịch sử học tập + thẻ "Tiến độ học tập" (progress bar).
    // Cột phải: nhắc nhở hệ thống + thẻ "Thao tác nhanh" (điều hướng thật, không chỉ trang trí).
    // ========================================================
    private JPanel createBottomWrapper() {
        JPanel bottomWrapper = new JPanel(new BorderLayout(20, 0));
        bottomWrapper.setOpaque(false);
        bottomWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);
        leftColumn.add(createHistoryTableCard());
        leftColumn.add(Box.createVerticalStrut(20));
        leftColumn.add(createProgressCard());

        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);
        rightColumn.setPreferredSize(new Dimension(300, 0));
        rightColumn.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));
        rightColumn.add(createAlertCard());
        rightColumn.add(Box.createVerticalStrut(20));
        rightColumn.add(createQuickActionsCard());

        bottomWrapper.add(leftColumn, BorderLayout.CENTER);
        bottomWrapper.add(rightColumn, BorderLayout.EAST);
        return bottomWrapper;
    }

    private JPanel createHistoryTableCard() {
        JPanel tablePanel = UIUtils.createCardShell("Lịch sử học tập gần đây (" + currentHKLabel + ")", UIUtils.MIT_RED);
        tablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

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
        table.setFillsViewportHeight(false); // V5: khong keo dai hang trang -> bang cao dung so hang du lieu thuc te

        int lastColumnIndex = table.getColumnCount() - 1;
        ZebraRenderer zebra = new ZebraRenderer();
        for (int i = 0; i < lastColumnIndex; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        table.getColumnModel().getColumn(lastColumnIndex).setCellRenderer(new BadgeRenderer());

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        scrollTable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollTable.getViewport().setBackground(Color.WHITE);
        // Cao vua du so hang thuc te (toi da 6 hang truoc khi can cuon), khong con khoang trang rong
        int visibleRows = Math.max(1, Math.min(model.getRowCount(), 6));
        int neededH = 46 + visibleRows * table.getRowHeight() + 2;
        scrollTable.setPreferredSize(new Dimension(10, neededH));

        tablePanel.add(scrollTable, BorderLayout.CENTER);
        // V5-fix: gioi han max-height = dung preferred height thuc te, KHONG de Integer.MAX_VALUE
        // (BoxLayout se coi la "co the gian vo han" va keo day card rong nhu loi cu).
        tablePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, tablePanel.getPreferredSize().height));
        return tablePanel;
    }

    /** Thẻ "Tiến độ học tập" - tái sử dụng UIUtils.createProgressRow (trước đây có sẵn nhưng chưa nơi nào dùng). */
    private JPanel createProgressCard() {
        JPanel card = UIUtils.createCardShell("Tiến độ học tập", UIUtils.GREEN_500);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 20, 12, 20));

        int pctTinChi = (int) Math.round(Math.min(100.0, tinChiTichLuy * 100.0 / 150.0));
        int pctGpa = (int) Math.round(Math.min(100.0, gpa * 100.0 / 4.0));
        content.add(UIUtils.createProgressRow("Tín chỉ (/150)", pctTinChi));
        content.add(UIUtils.createProgressRow("GPA quy đổi (/4.0)", pctGpa));

        card.add(content, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private JPanel createAlertCard() {
        JPanel alertPanel = UIUtils.createCardShell("Nhắc nhở từ hệ thống", UIUtils.MIT_ORANGE);
        alertPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel alertList = new JPanel();
        alertList.setLayout(new BoxLayout(alertList, BoxLayout.Y_AXIS));
        alertList.setOpaque(false);
        alertList.setBorder(new EmptyBorder(14, 18, 14, 18));

        String noMsg = (tongNo > 0) ? "Bạn đang còn nợ học phí. Vui lòng thanh toán trước ngày 30/12." : "Bạn đã hoàn thành 100% học phí.";
        alertList.add(createAlertItem("Tài chính", noMsg, tongNo > 0));
        alertList.add(Box.createVerticalStrut(12));
        alertList.add(createAlertItem("Học vụ", "Hệ thống mở cổng đăng ký tín chỉ " + currentHKLabel + ".", false));

        alertPanel.add(alertList, BorderLayout.CENTER);
        alertPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, alertPanel.getPreferredSize().height));
        return alertPanel;
    }

    /** Thẻ "Thao tác nhanh" - lối tắt điều hướng thật (gọi lại đúng switchTab của sidebar). */
    private JPanel createQuickActionsCard() {
        JPanel card = UIUtils.createCardShell("Thao tác nhanh", UIUtils.MIT_RED);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 12, 12, 12));

        content.add(createQuickActionRow("edit", "Đăng ký học phần", onDangKy));
        content.add(createQuickActionRow("book", "Xem bảng điểm", onXemDiem));
        content.add(createQuickActionRow("calendar", "Xem lịch học", onLichHoc));
        content.add(createQuickActionRow("card", "Thanh toán học phí", onCongNo));

        card.add(content, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private JComponent createQuickActionRow(String icon, String text, Runnable action) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                if (Boolean.TRUE.equals(getClientProperty("hover"))) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UIUtils.BG_APP);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.dispose();
                }
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(9, 8, 9, 8));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel iconLbl = (JLabel) UIUtils.createIconLabel(icon, 16, UIUtils.TEXT_MUTED);
        JLabel textLbl = new JLabel(text);
        textLbl.setFont(UIUtils.FONT_BOLD);
        textLbl.setForeground(UIUtils.TEXT_MAIN);
        left.add(iconLbl);
        left.add(textLbl);

        JLabel chevron = new JLabel("›");
        chevron.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chevron.setForeground(UIUtils.TEXT_MUTED);

        row.add(left, BorderLayout.WEST);
        row.add(chevron, BorderLayout.EAST);

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { row.putClientProperty("hover", true); row.repaint(); }
            @Override public void mouseExited(MouseEvent e) { row.putClientProperty("hover", false); row.repaint(); }
            @Override public void mouseClicked(MouseEvent e) { if (action != null) action.run(); }
        });
        return row;
    }

    // V9 - "DOT PHA": banner gradient chao mung + vong tron tien do (thay the card trang
    // phang lap lai o moi Panel). Chi Panel nay doi, cac Panel khac van dung roundedCardPanel
    // trang binh thuong (giu su khac biet co chu dich cho khu vuc quan trong nhat cua trang).
    private JPanel createStudentProfileCard() {
        JPanel hero = UIUtils.createHeroBanner();
        hero.setLayout(new BorderLayout(24, 0));
        hero.setBorder(new EmptyBorder(24, 32, 24, 30));
        hero.setMaximumSize(new Dimension(6000, 172));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- Trai: loi chao theo gio trong ngay + ten + trang thai ---
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel lblGreeting = new JLabel(UIUtils.greetingByHour() + ", " + hoTen + "!");
        lblGreeting.setFont(new Font("Segoe UI", Font.BOLD, 23));
        lblGreeting.setForeground(Color.WHITE);
        lblGreeting.setAlignmentX(Component.LEFT_ALIGNMENT);

        String todayStr;
        try {
            todayStr = new java.text.SimpleDateFormat("EEEE, dd/MM/yyyy", new java.util.Locale("vi", "VN")).format(new java.util.Date());
        } catch (Exception ex) {
            todayStr = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date());
        }
        JLabel lblSub = new JLabel("MSSV " + currentMaSV + "  ·  Lớp " + maLop + "  ·  " + todayStr);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(255, 255, 255, 215));
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblSub.setBorder(new EmptyBorder(6, 0, 14, 0));

        JLabel lblStatus = new JLabel(trangThaiHocTap);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(255, 255, 255, 40));
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblStatus.setBorder(new EmptyBorder(5, 12, 5, 12));
        lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(Box.createVerticalGlue());
        left.add(lblGreeting);
        left.add(lblSub);
        left.add(lblStatus);
        left.add(Box.createVerticalGlue());

        // --- Giua: luoi thong tin nhanh, chu trang tren nen gradient ---
        String khoaHoc = (maLop != null && maLop.length() >= 5) ? maLop.substring(0, 5) : "N/A";
        JPanel infoGrid = new JPanel(new GridLayout(3, 2, 26, 10));
        infoGrid.setOpaque(false);
        infoGrid.add(createHeroAttr("calendar", ngaySinh));
        infoGrid.add(createHeroAttr("users", gioiTinh));
        infoGrid.add(createHeroAttr("graduation", khoaHoc));
        infoGrid.add(createHeroAttr("institution", maLop));
        infoGrid.add(createHeroAttr("book", "Đại học"));
        infoGrid.add(createHeroAttr("laptop", maCTDT));

        JPanel infoCenterer = new JPanel();
        infoCenterer.setLayout(new BoxLayout(infoCenterer, BoxLayout.Y_AXIS));
        infoCenterer.setOpaque(false);
        infoCenterer.add(Box.createVerticalGlue());
        infoCenterer.add(infoGrid);
        infoCenterer.add(Box.createVerticalGlue());

        // --- Phai: vong tron tien do tin chi tich luy (diem nhan chinh, khac biet nhat trang) ---
        int pctTinChi = (int) Math.round(Math.min(100.0, tinChiTichLuy * 100.0 / 150.0));
        JComponent ring = UIUtils.createRadialProgress(pctTinChi, pctTinChi + "%", "Tín chỉ",
            UIUtils.MIT_YELLOW, new Color(255, 255, 255, 55), Color.WHITE);
        ring.setPreferredSize(new Dimension(112, 112));
        ring.setOpaque(false);

        hero.add(left, BorderLayout.WEST);
        hero.add(infoCenterer, BorderLayout.CENTER);
        hero.add(ring, BorderLayout.EAST);
        return hero;
    }

    /** Muc thong tin nho, chu trang, dung tren banner gradient (khac voi createProfileAttr chu den tren nen trang). */
    private JComponent createHeroAttr(String icon, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.add(UIUtils.createIconLabel(icon, 15, new Color(255, 255, 255, 210)));
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 13));
        v.setForeground(Color.WHITE);
        p.add(v);
        return p;
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
