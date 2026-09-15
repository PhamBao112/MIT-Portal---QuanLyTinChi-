package ui;

import config.DBConnect;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardPanel extends JPanel {
    // Đã gỡ bỏ static final để biến thành biến có thể thay đổi động
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
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentMaSV);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    currentHK = rs.getString("MaHK");
                    currentHKLabel = rs.getString("TenHK");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ========================================================
    // HÀM MỚI: NHẬN TÍN HIỆU TỪ STUDENT PANEL (KHÔNG CÒN BÁO ĐỎ)
    // ========================================================
    // (Đã gỡ bỏ updateData() cũ - Trang Chủ tự resolveCurrentHocKy() thay vì nhận từ combo global)

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
                    
                    // Xử lý chuyển đổi định dạng ngày sinh từ yyyy-MM-dd sang dd/MM/yyyy
                    String rawDate = rs.getString("NgaySinh");
                    if (rawDate != null && !rawDate.isEmpty()) {
                        try {
                            // Cắt bỏ phần giờ phút (nếu có) để parse chính xác ngày
                            if (rawDate.contains(" ")) rawDate = rawDate.split(" ")[0];
                            java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(rawDate);
                            ngaySinh = new java.text.SimpleDateFormat("dd/MM/yyyy").format(date);
                        } catch (Exception ex) {
                            ngaySinh = rawDate; // Nếu lỗi format thì in nguyên gốc
                        }
                    } else {
                        ngaySinh = "Chưa cập nhật";
                    }

                    gioiTinh = rs.getString("GioiTinh") != null ? rs.getString("GioiTinh") : "Chưa cập nhật";
                }
            }
            // FIX #5: GPA phai tinh theo TRONG SO tin chi thay vi AVG() don gian (moi mon deu co diem duoc tinh,
            // khong chi rieng mon 'Dat', de phan anh dung ca nhung mon khong dat vao GPA tich luy)
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
            // Đã thay CURRENT_HK bằng biến currentHK
            String sqlKyNay = "SELECT SUM(CAST(m.SoTinChi AS INT)) as TCKyNay FROM KET_QUA_DANG_KY kq JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE kq.MaSV = ? AND lhp.MaHK = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlKyNay)) {
                ps.setString(1, currentMaSV); ps.setString(2, currentHK); ResultSet rs = ps.executeQuery();
                if (rs.next()) tinChiKyNay = rs.getInt("TCKyNay");
            }
            String sqlNo = "SELECT SUM(CAST(TongTienPhaiDong AS FLOAT) - CAST(SoTienDaDong AS FLOAT)) as ConNo FROM CONG_NO_HOC_PHI WHERE MaSV = ? AND TrangThai != N'Đã hoàn thành'";
            try (PreparedStatement ps = conn.prepareStatement(sqlNo)) {
                ps.setString(1, currentMaSV); ResultSet rs = ps.executeQuery();
                if (rs.next()) tongNo = rs.getDouble("ConNo");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ========================================================
    // KHU VỰC THỐNG KÊ (THẺ STATS)
    // ========================================================
    private JPanel createStatsGrid() {
        JPanel gridStats = new JPanel(new GridLayout(1, 4, 15, 0));
        gridStats.setBackground(UIUtils.BG_APP);
        gridStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // Sử dụng Icon Type (1: Sao, 2: Biểu đồ, 3: Sách, 4: Tiền) để vẽ đồ họa Vector thay vì dùng Emoji bị lỗi
        gridStats.add(createColoredStatCard("Tín chỉ tích lũy", tinChiTichLuy + " / 150", 1, new Color(254, 242, 242), new Color(220, 38, 38))); 
        gridStats.add(createColoredStatCard("Điểm tích lũy (GPA)", String.format("%.2f", gpa), 2, new Color(255, 247, 237), new Color(234, 88, 12))); 
        gridStats.add(createColoredStatCard("Tín chỉ kỳ này", tinChiKyNay + " TC", 3, new Color(240, 253, 244), new Color(22, 163, 74))); 
        gridStats.add(createColoredStatCard("Công nợ hiện tại", String.format("%,.0f đ", tongNo), 4, new Color(239, 246, 255), new Color(37, 99, 235))); 
        
        return gridStats;
    }

    private JPanel createColoredStatCard(String title, String val, int iconType, Color bgColor, Color textColor) {
        JPanel card = new JPanel(new BorderLayout(10, 0)); 
        card.setBackground(bgColor); 
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(bgColor.darker(), 1, true), new EmptyBorder(20, 20, 20, 20)
        ));
        
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5)); 
        textPanel.setBackground(bgColor);
        
        JLabel lT = new JLabel(title); 
        lT.setFont(new Font("Segoe UI", Font.BOLD, 13)); 
        lT.setForeground(textColor.darker());
        
        JLabel lV = new JLabel(val); 
        lV.setFont(new Font("Segoe UI", Font.BOLD, 24)); 
        lV.setForeground(textColor);
        
        textPanel.add(lT); textPanel.add(lV);
        
        // Gắn Icon Tự vẽ (Tuyệt đối không bị ô vuông)
        JLabel lIcon = new JLabel(new StatIcon(iconType, textColor));
        
        card.add(textPanel, BorderLayout.CENTER); 
        card.add(lIcon, BorderLayout.EAST); 
        return card;
    }

    // ========================================================
    // KHU VỰC BÊN DƯỚI (BẢNG & THÔNG BÁO)
    // ========================================================
    private JPanel createBottomWrapper() {
        JPanel bottomWrapper = new JPanel(new BorderLayout(20, 0));
        bottomWrapper.setBackground(UIUtils.BG_APP);

        // --- BẢNG LỊCH SỬ HỌC TẬP ---
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(new LineBorder(UIUtils.BORDER, 1, true));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setBackground(new Color(239, 246, 255));
        tableHeader.setBorder(BorderFactory.createCompoundBorder(
        new MatteBorder(0, 0, 1, 0, new Color(191, 219, 254)), 
        // Viền dưới nổi nhẹ
        new EmptyBorder(15, 20, 15, 20)

));
    
    

// 2. Đổi màu chữ của JLabel tiêu đề thành Xanh dương đậm
        
        // Thay đổi title để hiển thị tên Học kỳ động
        JLabel lblTableTitle = new JLabel("Lịch sử học tập gần đây (" + currentHKLabel + ")");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTableTitle.setForeground(new Color(30, 64, 175));// Màu xanh dương đậm
        lblTableTitle.setAlignmentX(Component.CENTER_ALIGNMENT);        
        tableHeader.add(lblTableTitle, BorderLayout.WEST);

        DefaultTableModel model = new DefaultTableModel(new String[]{"Mã HP", "Tên Môn", "Điểm", "Kết quả"}, 0){
            public boolean isCellEditable(int r, int c) { return false; }
        };
        try (Connection conn = DBConnect.getConnection()) {
            // Đã bổ sung lhp.MaHK = ? để lọc bảng theo Học kỳ
            String sql = "SELECT lhp.MaMon, m.TenMon, kq.DiemTongKet, kq.TrangThai FROM KET_QUA_DANG_KY kq JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE kq.MaSV = ? AND lhp.MaHK = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentMaSV);
            ps.setString(2, currentHK);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) model.addRow(new Object[]{rs.getString("MaMon"), rs.getString("TenMon"), rs.getDouble("DiemTongKet"), rs.getString("TrangThai")});
        } catch (Exception e) {}

        JTable table = new JTable(model);
        UIUtils.styleTable(table);
        
       
        DefaultTableCellRenderer customHeaderRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setBackground(new Color(30, 64, 175)); // Nền Xanh dương đậm
                label.setForeground(Color.WHITE);            // Chữ Trắng
                label.setFont(new Font("Segoe UI", Font.BOLD, 14));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 0, 1, 1, new Color(40, 74, 185)), // Vạch chia cột
                    new EmptyBorder(10, 10, 10, 10)
                ));
                return label;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(customHeaderRenderer);
        }
        // =========================================================

        // Gắn ZebraRenderer cho 3 cột đầu và BadgeRenderer (Huy hiệu xanh/đỏ) cho cột cuối
        ZebraRenderer zebra = new ZebraRenderer();
        for (int i = 0; i < table.getColumnCount() - 1; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        int lastColumnIndex = table.getColumnCount() - 1;
        table.getColumnModel().getColumn(lastColumnIndex).setCellRenderer(new BadgeRenderer());
        
        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        scrollTable.getViewport().setBackground(Color.WHITE);
        
        tablePanel.add(tableHeader, BorderLayout.NORTH);
        tablePanel.add(scrollTable, BorderLayout.CENTER);

        // --- KHUNG NHẮC NHỞ TỪ HỆ THỐNG ---
        JPanel alertPanel = new JPanel();
        alertPanel.setLayout(new BoxLayout(alertPanel, BoxLayout.Y_AXIS));
        alertPanel.setBackground(Color.WHITE);
        alertPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.BORDER, 1, true), new EmptyBorder(20, 20, 20, 20)
        ));
        alertPanel.setPreferredSize(new Dimension(280, 0));

        JPanel alertHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        alertHeaderPanel.setBackground(Color.WHITE);
        JLabel lblAlertTitle = new JLabel("Nhắc nhở từ hệ thống");
        lblAlertTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblAlertTitle.setForeground(UIUtils.TEXT_MAIN);
        alertHeaderPanel.add(lblAlertTitle);
        alertHeaderPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        String noMsg = (tongNo > 0) ? "Bạn đang còn nợ học phí. Vui lòng thanh toán trước ngày 30/12." : "Bạn đã hoàn thành 100% học phí.";
        alertPanel.add(alertHeaderPanel);
        alertPanel.add(createAlertItem("Tài chính", noMsg, tongNo > 0 ? "warn" : "info"));
        alertPanel.add(Box.createVerticalStrut(15));
        
        // Cập nhật tên Học kỳ trong thẻ thông báo
        alertPanel.add(createAlertItem("Học vụ", "Hệ thống mở cổng đăng ký tín chỉ " + currentHKLabel + ".", "info"));
        alertPanel.add(Box.createVerticalGlue()); 

        bottomWrapper.add(tablePanel, BorderLayout.CENTER);
        bottomWrapper.add(alertPanel, BorderLayout.EAST);
        return bottomWrapper;
    }

    private JPanel createStudentProfileCard() {
        JPanel profileCard = new JPanel(new BorderLayout(25, 0));
        profileCard.setBackground(Color.WHITE);
        profileCard.setBorder(BorderFactory.createCompoundBorder(new LineBorder(UIUtils.BORDER, 1, true), new EmptyBorder(20, 25, 20, 25)));
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JPanel leftProfile = new JPanel(new BorderLayout(15, 0));
        leftProfile.setBackground(Color.WHITE);
        String firstChar = hoTen.length() > 0 && !hoTen.equals("Đang tải...") ? hoTen.substring(0, 1) : "A";
        JLabel lblAvatar = new JLabel(firstChar, SwingConstants.CENTER);
        lblAvatar.setFont(new Font("Segoe UI", Font.BOLD, 36)); lblAvatar.setForeground(Color.WHITE); lblAvatar.setBackground(UIUtils.MIT_ORANGE); lblAvatar.setOpaque(true); lblAvatar.setPreferredSize(new Dimension(80, 80));
        
        JPanel namePanel = new JPanel(); namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS)); namePanel.setBackground(Color.WHITE);
        JLabel lblName = new JLabel(hoTen); lblName.setFont(new Font("Segoe UI", Font.BOLD, 20)); lblName.setForeground(UIUtils.TEXT_MAIN);
        JLabel lblMSSV = new JLabel("MSSV: " + currentMaSV); lblMSSV.setForeground(UIUtils.TEXT_MUTED);
        JLabel lblStatus = new JLabel(" " + trangThaiHocTap + " "); lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12)); lblStatus.setBackground(new Color(220, 252, 231)); lblStatus.setForeground(new Color(22, 101, 52)); lblStatus.setOpaque(true);
        namePanel.add(Box.createVerticalStrut(5)); namePanel.add(lblName); namePanel.add(Box.createVerticalStrut(5)); namePanel.add(lblMSSV); namePanel.add(Box.createVerticalStrut(5)); namePanel.add(lblStatus);

        leftProfile.add(lblAvatar, BorderLayout.WEST); leftProfile.add(namePanel, BorderLayout.CENTER);

        JPanel gridInfo = new JPanel(new GridLayout(3, 2, 20, 10));
        gridInfo.setBackground(Color.WHITE); gridInfo.setBorder(new MatteBorder(0, 1, 0, 0, UIUtils.BORDER)); 
        String khoaHoc = (maLop != null && maLop.length() >= 5) ? maLop.substring(0, 5) : "N/A";
        gridInfo.add(createProfileAttr("Ngày sinh:", ngaySinh)); gridInfo.add(createProfileAttr("Khóa học:", khoaHoc));
        gridInfo.add(createProfileAttr("Giới tính:", gioiTinh)); gridInfo.add(createProfileAttr("Bậc đào tạo:", "Đại học"));
        gridInfo.add(createProfileAttr("Lớp học:", maLop)); gridInfo.add(createProfileAttr("Ngành:", maCTDT));

        JPanel rightWrapper = new JPanel(new BorderLayout()); rightWrapper.setBackground(Color.WHITE); rightWrapper.setBorder(new EmptyBorder(0, 20, 0, 0)); rightWrapper.add(gridInfo, BorderLayout.CENTER);
        profileCard.add(leftProfile, BorderLayout.WEST); profileCard.add(rightWrapper, BorderLayout.CENTER);
        return profileCard;
    }

    private JPanel createProfileAttr(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); p.setBackground(Color.WHITE);
        JLabel l1 = new JLabel(label + "  "); l1.setForeground(UIUtils.TEXT_MUTED);
        JLabel l2 = new JLabel(value); l2.setFont(new Font("Segoe UI", Font.BOLD, 14)); l2.setForeground(UIUtils.TEXT_MAIN);
        p.add(l1); p.add(l2); return p;
    }

    // ========================================================
    // TẠO THẺ THÔNG BÁO VỚI VIỀN TRÁI NỔI BẬT NHƯ WEB
    // ========================================================
    private JPanel createAlertItem(String tag, String body, String type) {
        Color accentColor = type.equals("warn") ? new Color(239, 68, 68) : new Color(59, 130, 246);
        Color badgeBg = type.equals("warn") ? new Color(254, 226, 226) : new Color(219, 234, 254); 
        Color badgeFg = type.equals("warn") ? new Color(185, 28, 28) : new Color(30, 64, 175);
        
        // Vẽ Panel với Vạch màu bên trái
        JPanel p = new JPanel(new BorderLayout(15, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(accentColor);
                g2.fillRect(0, 0, 4, getHeight()); // Vạch nhấn màu bên trái
                g2.dispose();
            }
        };
        p.setBackground(new Color(248, 250, 252)); 
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.BORDER, 1, true), new EmptyBorder(12, 16, 12, 13)
        ));
        
        JLabel lTag = new JLabel(tag, SwingConstants.CENTER); 
        lTag.setFont(new Font("Segoe UI", Font.BOLD, 11)); 
        lTag.setOpaque(true); lTag.setBackground(badgeBg); lTag.setForeground(badgeFg); 
        lTag.setBorder(new EmptyBorder(4, 8, 4, 8));
        
        JPanel tagWrapper = new JPanel(new BorderLayout()); 
        tagWrapper.setBackground(p.getBackground()); 
        tagWrapper.add(lTag, BorderLayout.NORTH);
        
        JLabel lBody = new JLabel("<html><div style='width: 200px; line-height: 1.4;'>" + body + "</div></html>"); 
        lBody.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lBody.setForeground(UIUtils.TEXT_MAIN);
        
        p.add(tagWrapper, BorderLayout.WEST); 
        p.add(lBody, BorderLayout.CENTER); 
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90)); 
        p.setAlignmentX(Component.LEFT_ALIGNMENT); 
        return p;
    }

    // ========================================================
    // CÁC LỚP RENDERER VẼ ĐỒ HỌA (ICONS, BẢNG)
    // ========================================================

    // Lớp vẽ ICONS Vector cho 4 ô Thống kê (Chống lỗi ô vuông hoàn toàn)
    class StatIcon implements Icon {
        private int type;
        private Color color;
        public StatIcon(int type, Color color) { this.type = type; this.color = color; }
        public int getIconWidth() { return 36; }
        public int getIconHeight() { return 36; }
        
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            switch(type) {
                case 1: // Ngôi sao (Tín chỉ tích lũy)
                    int[] xP = {x+18, x+23, x+34, x+25, x+29, x+18, x+7, x+11, x+2, x+13};
                    int[] yP = {y+2, y+12, y+13, y+22, y+34, y+28, y+34, y+22, y+13, y+12};
                    g2.fillPolygon(xP, yP, 10);
                    break;
                case 2: // Biểu đồ tăng trưởng (GPA)
                    g2.drawLine(x+4, y+32, x+32, y+32); // Trục X
                    g2.drawLine(x+4, y+4, x+4, y+32);   // Trục Y
                    g2.drawLine(x+8, y+22, x+16, y+12);
                    g2.drawLine(x+16, y+12, x+24, y+18);
                    g2.drawLine(x+24, y+18, x+32, y+4);
                    g2.fillOval(x+14, y+10, 5, 5);
                    g2.fillOval(x+22, y+16, 5, 5);
                    g2.fillOval(x+30, y+2, 5, 5);
                    break;
                case 3: // Sách (Tín chỉ kỳ này)
                    g2.drawRect(x+6, y+8, 24, 22);
                    g2.drawLine(x+18, y+8, x+18, y+30);
                    g2.drawLine(x+8, y+14, x+15, y+14);
                    g2.drawLine(x+8, y+20, x+15, y+20);
                    g2.drawLine(x+21, y+14, x+28, y+14);
                    break;
                case 4: // Tiền xu (Công nợ)
                    g2.drawOval(x+6, y+6, 24, 24);
                    g2.drawOval(x+10, y+10, 16, 16);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    g2.drawString("đ", x+13, y+25);
                    break;
            }
            g2.dispose();
        }
    }

    // Zebra Renderer (Kẻ sọc cho bảng)
    class ZebraRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
            else c.setBackground(UIUtils.MIT_RED_LIGHT);
            c.setForeground(UIUtils.TEXT_MAIN);
            setBorder(new EmptyBorder(0, 15, 0, 15)); return c;
        }
    }

    // Badge Renderer (Tạo huy hiệu nền màu cho cột Kết quả)
    class BadgeRenderer extends DefaultTableCellRenderer {
        private JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 6));
        private JLabel lbl = new JLabel();

        public BadgeRenderer() {
            p.setOpaque(true); lbl.setOpaque(true);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
            p.add(lbl);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            p.setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
            if (value != null) {
                String text = value.toString();
                lbl.setText(text);
                if (text.equalsIgnoreCase("Đạt") || text.equalsIgnoreCase("Đã hoàn thành")) {
                    lbl.setBackground(new Color(220, 252, 231)); 
                    lbl.setForeground(new Color(21, 128, 61));
                } else if (text.toLowerCase().contains("không đạt") || text.toLowerCase().contains("chưa")) {
                    lbl.setBackground(new Color(254, 226, 226)); 
                    lbl.setForeground(new Color(220, 38, 38));
                } else {
                    lbl.setBackground(new Color(241, 245, 249)); 
                    lbl.setForeground(new Color(71, 85, 105));
                }
            } else {
                lbl.setText("");
            }
            return p;
        }
    }
}