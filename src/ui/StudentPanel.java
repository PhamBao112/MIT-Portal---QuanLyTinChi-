package ui;

import service.StudentManagerService;
import utils.UIUtils;
import config.DBConnect;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class StudentPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private StudentManagerService service;
    private CardLayout cardLayout;
    private JPanel contentArea;
    private ArrayList<JButton> sidebarButtons = new ArrayList<>();
    private JLabel lblHeaderTitle;
    
    private String currentMaSV; 
    private String hoTen = "Đang tải...";
    private String maLop = "K2024";

    // KHAI BÁO CÁC BIẾN ĐỂ NHẬN TÍN HIỆU TỪ COMBOBOX
    private LichHocPanel lichHocPanel;
    private DashboardPanel dashboardPanel;
    private DangKyPanel dangKyPanel;
    private DiemPanel diemPanel; // THÊM BIẾN BẢNG ĐIỂM

    public StudentPanel(StudentManagerService service, String maSV) {
        this.service = service;
        this.currentMaSV = maSV; 
        loadHeaderInfo();

        setLayout(new BorderLayout());
        setBackground(UIUtils.BG_APP);

        // --- 1. SIDEBAR (MENU TRÁI) ---
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UIUtils.MIT_RED);
        sidebar.setPreferredSize(new Dimension(310, 0)); 

        JPanel topSidebar = new JPanel();
        topSidebar.setLayout(new BoxLayout(topSidebar, BoxLayout.Y_AXIS));
        topSidebar.setBackground(UIUtils.MIT_RED);

        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setBackground(UIUtils.MIT_RED);
        logoPanel.setBorder(new EmptyBorder(30, 20, 15, 20));
        
        JLabel logo1 = new JLabel(" MIT PORTAL");
        logo1.setIcon(new LogoIcon());
        logo1.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo1.setForeground(Color.WHITE);
        logo1.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoPanel.add(logo1);

        JPanel profileSidebar = new JPanel();
        profileSidebar.setLayout(new BoxLayout(profileSidebar, BoxLayout.Y_AXIS));
        profileSidebar.setBackground(UIUtils.MIT_RED);
        profileSidebar.setBorder(new EmptyBorder(0, 25, 25, 20));

        JLabel avatar = new JLabel(new BigAvatarIcon());
        avatar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblName = new JLabel(hoTen);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblName.setForeground(Color.WHITE);
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblID = new JLabel(currentMaSV + "-" + maLop);
        lblID.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblID.setForeground(new Color(255, 230, 230)); 
        lblID.setAlignmentX(Component.LEFT_ALIGNMENT);

        profileSidebar.add(avatar);
        profileSidebar.add(Box.createVerticalStrut(12));
        profileSidebar.add(lblName);
        profileSidebar.add(Box.createVerticalStrut(2));
        profileSidebar.add(lblID);

        topSidebar.add(logoPanel);
        topSidebar.add(profileSidebar);

        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(UIUtils.MIT_RED);

        // --- 2. HEADER ---
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(UIUtils.BG_APP);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtils.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIUtils.BORDER), new EmptyBorder(15, 30, 15, 30)
        ));

        lblHeaderTitle = new JLabel("Trang Chủ Tổng Quan");
        lblHeaderTitle.setFont(UIUtils.FONT_TITLE);
        header.add(lblHeaderTitle, BorderLayout.WEST);
        // Combo Học kỳ chung đã Bỏ ĐI - mỗi luồng (Điểm/Lịch học/Đăng ký) tự quản lý combo Học kỳ riêng của nó, lọc đúng theo SV đang đăng nhập.

        // --- 3. CARD LAYOUT (VÙNG HIỂN THỊ CHỨC NĂNG) ---
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(UIUtils.BG_APP);
        contentArea.setBorder(new EmptyBorder(25, 30, 25, 30));

        // KHỞI TẠO VÀ GÁN CÁC PANEL VÀO BIẾN ĐỂ NHẬN SỰ KIỆN
        dashboardPanel = new DashboardPanel(currentMaSV);
        lichHocPanel = new LichHocPanel(service, currentMaSV);
        dangKyPanel = new DangKyPanel(service, currentMaSV);
        diemPanel = new DiemPanel(currentMaSV); // KHỞI TẠO BẢNG ĐIỂM

        contentArea.add(dashboardPanel, "TRANG_CHU"); 
        contentArea.add(lichHocPanel, "LICH_HOC");      
        contentArea.add(diemPanel, "DIEM"); // GẮN BIẾN VÀO GIAO DIỆN           
        contentArea.add(dangKyPanel, "DANG_KY");
        contentArea.add(new CongNoPanel(service, currentMaSV), "CONG_NO");
        contentArea.add(new TotNghiepPanel(service, currentMaSV), "TOT_NGHIEP");

        // --- 4. TẠO NÚT BẤM VECTOR CHO MENU ---
        JButton btnHome = createSidebarBtn(1, "Trang Chủ Tổng Quan", true);
        JButton btnLH   = createSidebarBtn(2, "Lịch Học Thời Khóa Biểu", false);
        JButton btnDiem = createSidebarBtn(3, "Bảng Kết Quả Học Tập", false);
        JButton btnDK   = createSidebarBtn(4, "Đăng Ký Học Phần", false);
        JButton btnCN   = createSidebarBtn(5, "Thông Tin Công Nợ Học Phí", false);
        JButton btnTN   = createSidebarBtn(6, "Thẩm Định Tốt Nghiệp", false);

        btnHome.addActionListener(e -> switchTab(btnHome, "TRANG_CHU", "Trang Chủ Tổng Quan"));
        btnLH.addActionListener(e -> switchTab(btnLH, "LICH_HOC", "Lịch Học Thời Khóa Biểu"));
        btnDiem.addActionListener(e -> switchTab(btnDiem, "DIEM", "Bảng Kết Quả Học Tập"));
        btnDK.addActionListener(e -> switchTab(btnDK, "DANG_KY", "Đăng Ký Học Phần"));
        btnCN.addActionListener(e -> switchTab(btnCN, "CONG_NO", "Thông Tin Công Nợ Học Phí"));
        btnTN.addActionListener(e -> switchTab(btnTN, "TOT_NGHIEP", "Thẩm Định Xét Tốt Nghiệp"));

        menuPanel.add(btnHome); 
        menuPanel.add(btnLH); 
        menuPanel.add(btnDiem);
        menuPanel.add(btnDK); 
        menuPanel.add(btnCN); 
        menuPanel.add(btnTN);

        // Nút đăng xuất
        JPanel logoutPanel = new JPanel(new BorderLayout()); 
        logoutPanel.setBackground(UIUtils.MIT_RED); 
        logoutPanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        JButton btnLogout = createSidebarBtn(7, "Đăng Xuất Hệ Thống", false);
        btnLogout.addActionListener(e -> { 
            Container parent = this.getParent(); 
            if(parent != null && parent.getLayout() instanceof CardLayout) { 
                ((CardLayout)parent.getLayout()).show(parent, "LOGIN"); 
            }
        });
        logoutPanel.add(btnLogout, BorderLayout.CENTER);

        sidebar.add(topSidebar, BorderLayout.NORTH); 
        sidebar.add(menuPanel, BorderLayout.CENTER); 
        sidebar.add(logoutPanel, BorderLayout.SOUTH);
        
        rightPanel.add(header, BorderLayout.NORTH); 
        rightPanel.add(contentArea, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST); 
        add(rightPanel, BorderLayout.CENTER);

    }

    private void loadHeaderInfo() {
        try (Connection conn = DBConnect.getConnection()) {
            if (conn == null) return;
            String sqlInfo = "SELECT HoTen, MaLop FROM SINH_VIEN WHERE MaSV = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlInfo)) {
                ps.setString(1, currentMaSV); 
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    hoTen = rs.getString("HoTen");
                    maLop = rs.getString("MaLop");
                    if (maLop == null || maLop.isEmpty()) maLop = "K2024";
                }
            }
        } catch (Exception e) {}
    }

    // ==========================================
    // UI UTILS: CÁC NÚT BẤM VÀ ĐIỀU HƯỚNG
    // ==========================================
    private JButton createSidebarBtn(int iconType, String text, boolean isActive) {
        JButton btn = new JButton(text); 
        btn.setIcon(new MenuIcon(iconType));
        btn.setIconTextGap(15);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI()); 
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false); 
        btn.setBorderPainted(false); 
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); 
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        
        setSidebarBtnStyle(btn, isActive);
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { 
                if (!btn.getBackground().equals(UIUtils.MIT_ORANGE)) btn.setBackground(new Color(160, 20, 20)); 
            }
            public void mouseExited(java.awt.event.MouseEvent evt) { 
                if (!btn.getBackground().equals(UIUtils.MIT_ORANGE)) btn.setBackground(UIUtils.MIT_RED); 
            }
        });
        
        if (iconType != 7) sidebarButtons.add(btn);
        return btn;
    }

    private void setSidebarBtnStyle(JButton btn, boolean isActive) {
        if (isActive) { 
            btn.setBackground(UIUtils.MIT_ORANGE); 
            btn.setForeground(UIUtils.WHITE); 
            btn.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 5, 0, 0, UIUtils.MIT_YELLOW), new EmptyBorder(12, 20, 12, 15)
            )); 
        } else { 
            btn.setBackground(UIUtils.MIT_RED); 
            btn.setForeground(new Color(255, 200, 200)); 
            btn.setBorder(new EmptyBorder(12, 25, 12, 15)); 
        }
    }

    private void switchTab(JButton activeBtn, String cardName, String title) {
        lblHeaderTitle.setText(title);
        for (JButton btn : sidebarButtons) setSidebarBtnStyle(btn, btn == activeBtn);
        cardLayout.show(contentArea, cardName);
    }

    // ==========================================
    // CÁC LỚP VẼ ĐỒ HỌA VECTOR ICON
    // ==========================================
    class LogoIcon implements Icon {
        public int getIconWidth() { return 36; } public int getIconHeight() { return 36; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE); g2.fillRoundRect(x, y, 36, 36, 10, 10);
            g2.setColor(UIUtils.MIT_RED); g2.setFont(new Font("Segoe UI", Font.BOLD, 22)); g2.drawString("M", x + 8, y + 26); g2.dispose();
        }
    }

    class BigAvatarIcon implements Icon {
        public int getIconWidth() { return 56; } public int getIconHeight() { return 56; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create(); 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 120)); 
            g2.fillOval(x, y, 56, 56);
            g2.setColor(Color.WHITE); 
            g2.fillOval(x + 16, y + 12, 24, 24); 
            g2.fillArc(x + 8, y + 38, 40, 32, 0, 180); 
            g2.dispose();
        }
    }

    class MenuIcon implements Icon {
        private int type; public MenuIcon(int type) { this.type = type; }
        public int getIconWidth() { return 24; } public int getIconHeight() { return 24; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getForeground()); g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (type) {
                case 1: g2.drawPolygon(new int[]{x+2, x+12, x+22}, new int[]{y+12, y+2, y+12}, 3); g2.drawRect(x+5, y+12, 14, 10); break;
                case 2: g2.drawRoundRect(x+2, y+4, 20, 18, 4, 4); g2.drawLine(x+2, y+10, x+22, y+10); g2.drawLine(x+7, y+2, x+7, y+6); g2.drawLine(x+17, y+2, x+17, y+6); break;
                case 3: g2.drawLine(x+2, y+22, x+22, y+22); g2.drawLine(x+2, y+2, x+2, y+22); g2.fillRect(x+6, y+12, 4, 10); g2.fillRect(x+12, y+6, 4, 16); g2.fillRect(x+18, y+16, 4, 6); break;
                case 4: g2.drawRect(x+4, y+2, 16, 20); g2.drawLine(x+8, y+8, x+16, y+8); g2.drawLine(x+8, y+12, x+16, y+12); g2.drawLine(x+8, y+16, x+12, y+16); break;
                case 5: g2.drawOval(x+2, y+2, 20, 20); g2.drawString("$", x+8, y+17); break;
                case 6: g2.drawLine(x+2, y+10, x+12, y+4); g2.drawLine(x+12, y+4, x+22, y+10); g2.drawLine(x+22, y+10, x+12, y+16); g2.drawLine(x+12, y+16, x+2, y+10); g2.drawRect(x+8, y+14, 8, 6); break;
                case 7: g2.drawRect(x+2, y+2, 12, 20); g2.drawLine(x+10, y+12, x+22, y+12); g2.drawLine(x+18, y+8, x+22, y+12); g2.drawLine(x+18, y+16, x+22, y+12); break;
            }
            g2.dispose();
        }
    }
}