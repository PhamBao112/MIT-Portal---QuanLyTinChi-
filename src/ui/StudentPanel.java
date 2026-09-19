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

    private LichHocPanel lichHocPanel;
    private DashboardPanel dashboardPanel;
    private DangKyPanel dangKyPanel;
    private DiemPanel diemPanel;

    public StudentPanel(StudentManagerService service, String maSV) {
        this.service = service;
        this.currentMaSV = maSV;
        loadHeaderInfo();

        setLayout(new BorderLayout());
        setBackground(UIUtils.BG_APP);

        // --- 1. SIDEBAR (dùng UIUtils.createSidebarPanel — đồng bộ với AdminPanel) ---
        JPanel sidebar = UIUtils.createSidebarPanel();
        sidebar.setPreferredSize(new Dimension(250, 0));

        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(0, 6, 14, 6));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel brandTitle = new JLabel("MIT PORTAL");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandSub = new JLabel("Cổng thông tin sinh viên");
        brandSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        brandSub.setForeground(UIUtils.SIDEBAR_TEXT_MUTED);
        brandSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.add(brandTitle);
        brand.add(brandSub);
        sidebar.add(brand);

        // --- Thẻ hồ sơ SV (avatar + tên + mã SV-lớp) ---
        JPanel profileCard = new JPanel();
        profileCard.setLayout(new BoxLayout(profileCard, BoxLayout.Y_AXIS));
        profileCard.setOpaque(true);
        profileCard.setBackground(new Color(255, 255, 255, 18));
        profileCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        profileCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel avatar = new JLabel(new AvatarIcon(hoTen));
        avatar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblName = new JLabel(hoTen);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblName.setForeground(Color.WHITE);
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblName.setBorder(new EmptyBorder(8, 0, 0, 0));

        JLabel lblID = new JLabel(currentMaSV + " · " + maLop);
        lblID.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblID.setForeground(UIUtils.SIDEBAR_TEXT_MUTED);
        lblID.setAlignmentX(Component.LEFT_ALIGNMENT);

        profileCard.add(avatar);
        profileCard.add(lblName);
        profileCard.add(lblID);
        sidebar.add(profileCard);
        sidebar.add(Box.createVerticalStrut(6));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 25));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(6));

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

        // --- 3. CARD LAYOUT ---
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(UIUtils.BG_APP);
        contentArea.setBorder(new EmptyBorder(25, 30, 25, 30));

        dashboardPanel = new DashboardPanel(currentMaSV);
        lichHocPanel = new LichHocPanel(service, currentMaSV);
        dangKyPanel = new DangKyPanel(service, currentMaSV);
        diemPanel = new DiemPanel(currentMaSV);

        contentArea.add(dashboardPanel, "TRANG_CHU");
        contentArea.add(lichHocPanel, "LICH_HOC");
        contentArea.add(diemPanel, "DIEM");
        contentArea.add(dangKyPanel, "DANG_KY");
        contentArea.add(new CongNoPanel(service, currentMaSV), "CONG_NO");
        contentArea.add(new TotNghiepPanel(service, currentMaSV), "TOT_NGHIEP");

        // --- 4. SIDEBAR NAV (dùng UIUtils.createSidebarButton) ---
        sidebar.add(UIUtils.createSidebarGroupLabel("Tổng quan"));
        JButton btnHome = createNavBtn("▣", "Trang chủ tổng quan");
        sidebar.add(btnHome);

        sidebar.add(UIUtils.createSidebarGroupLabel("Học tập"));
        JButton btnLH   = createNavBtn("📅", "Lịch học - TKB");
        JButton btnDiem = createNavBtn("📖", "Bảng kết quả học tập");
        JButton btnDK   = createNavBtn("📝", "Đăng ký học phần");
        JButton btnTN   = createNavBtn("🎓", "Thẩm định tốt nghiệp");
        sidebar.add(btnLH);
        sidebar.add(btnDiem);
        sidebar.add(btnDK);
        sidebar.add(btnTN);

        sidebar.add(UIUtils.createSidebarGroupLabel("Tài chính"));
        JButton btnCN = createNavBtn("💳", "Công nợ học phí");
        sidebar.add(btnCN);

        btnHome.addActionListener(e -> switchTab(btnHome, "TRANG_CHU", "Trang Chủ Tổng Quan"));
        btnLH.addActionListener(e -> switchTab(btnLH, "LICH_HOC", "Lịch Học Thời Khóa Biểu"));
        btnDiem.addActionListener(e -> switchTab(btnDiem, "DIEM", "Bảng Kết Quả Học Tập"));
        btnDK.addActionListener(e -> switchTab(btnDK, "DANG_KY", "Đăng Ký Học Phần"));
        btnCN.addActionListener(e -> switchTab(btnCN, "CONG_NO", "Thông Tin Công Nợ Học Phí"));
        btnTN.addActionListener(e -> switchTab(btnTN, "TOT_NGHIEP", "Thẩm Định Xét Tốt Nghiệp"));

        sidebar.add(Box.createVerticalGlue());
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new Color(255, 255, 255, 25));
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(sep2);
        sidebar.add(Box.createVerticalStrut(6));

        JButton btnLogout = UIUtils.createSidebarButton("🚪", "Đăng xuất");
        btnLogout.addActionListener(e -> {
            Container parent = this.getParent();
            if (parent != null && parent.getLayout() instanceof CardLayout) {
                ((CardLayout) parent.getLayout()).show(parent, "LOGIN");
            }
        });
        sidebar.add(btnLogout);

        UIUtils.setSidebarActive(btnHome, true);

        rightPanel.add(header, BorderLayout.NORTH);
        rightPanel.add(contentArea, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    private JButton createNavBtn(String icon, String text) {
        JButton btn = UIUtils.createSidebarButton(icon, text);
        sidebarButtons.add(btn);
        return btn;
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

    private void switchTab(JButton activeBtn, String cardName, String title) {
        lblHeaderTitle.setText(title);
        for (JButton btn : sidebarButtons) UIUtils.setSidebarActive(btn, btn == activeBtn);
        cardLayout.show(contentArea, cardName);
    }

    /** Avatar tròn hiển thị chữ cái đầu của tên SV (thay cho icon vector cũ). */
    class AvatarIcon implements Icon {
        private final String initial;
        AvatarIcon(String ten) {
            String t = (ten == null || ten.isBlank()) ? "SV" : ten.trim();
            String[] parts = t.split("\\s+");
            this.initial = parts[parts.length - 1].substring(0, 1).toUpperCase();
        }
        public int getIconWidth() { return 40; }
        public int getIconHeight() { return 40; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(x, y, UIUtils.MIT_YELLOW, x + 40, y + 40, UIUtils.MIT_ORANGE));
            g2.fillOval(x, y, 40, 40);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (40 - fm.stringWidth(initial)) / 2;
            int ty = y + (40 + fm.getAscent()) / 2 - 4;
            g2.drawString(initial, tx, ty);
            g2.dispose();
        }
    }
}
