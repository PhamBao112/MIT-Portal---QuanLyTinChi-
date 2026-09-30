package ui;

import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.ArrayList;

/**
 * Khung chinh man hinh Giang vien - sidebar + CardLayout, dong bo phong cach voi
 * StudentPanel/AdminPanel (dung chung UIUtils.createSidebarPanel/createSidebarButton).
 */
public class GiangVienPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final StudentManagerService service;
    private final String currentMaGV;
    private String hoTen = "Đang tải...";
    private String email = "";

    private CardLayout cardLayout;
    private JPanel contentArea;
    private final ArrayList<JButton> sidebarButtons = new ArrayList<>();
    private JLabel lblHeaderTitle;

    public GiangVienPanel(StudentManagerService service, String maGV) {
        this.service = service;
        this.currentMaGV = maGV;
        loadHeaderInfo();

        setLayout(new BorderLayout());
        setBackground(UIUtils.BG_APP);

        // --- SIDEBAR ---
        JPanel sidebar = UIUtils.createSidebarPanel();
        sidebar.setPreferredSize(new Dimension(250, 0));

        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(0, 6, 14, 6));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(2000, 60));

        JLabel brandTitle = new JLabel("MIT PORTAL");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandSub = new JLabel("Cổng thông tin giảng viên");
        brandSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        brandSub.setForeground(UIUtils.SIDEBAR_TEXT_MUTED);
        brandSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.add(brandTitle);
        brand.add(brandSub);
        sidebar.add(brand);

        // --- Profile card ---
        JPanel profileCard = new JPanel();
        profileCard.setLayout(new BoxLayout(profileCard, BoxLayout.Y_AXIS));
        profileCard.setOpaque(true);
        profileCard.setBackground(new Color(255, 255, 255, 18));
        profileCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        profileCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileCard.setMaximumSize(new Dimension(2000, 100));

        JLabel avatar = new JLabel(new AvatarIcon(hoTen));
        avatar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblName = new JLabel(hoTen);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblName.setForeground(Color.WHITE);
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblName.setBorder(new EmptyBorder(8, 0, 0, 0));

        JLabel lblEmail = new JLabel(currentMaGV + " · " + email);
        lblEmail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEmail.setForeground(UIUtils.SIDEBAR_TEXT_MUTED);
        lblEmail.setAlignmentX(Component.LEFT_ALIGNMENT);

        profileCard.add(avatar);
        profileCard.add(lblName);
        profileCard.add(lblEmail);
        sidebar.add(profileCard);
        sidebar.add(Box.createVerticalStrut(6));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 25));
        sep.setMaximumSize(new Dimension(6000, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(6));

        // --- HEADER ---
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(UIUtils.BG_APP);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtils.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIUtils.BORDER), new EmptyBorder(15, 30, 15, 30)));

        lblHeaderTitle = new JLabel("Trang Chủ Tổng Quan");
        lblHeaderTitle.setFont(UIUtils.FONT_TITLE);
        header.add(lblHeaderTitle, BorderLayout.WEST);

        // --- NAV BUTTONS ---
        sidebar.add(UIUtils.createSidebarGroupLabel("Tổng quan"));
        JButton btnHome = createNavBtn("dashboard", "Trang chủ tổng quan");
        sidebar.add(btnHome);

        sidebar.add(UIUtils.createSidebarGroupLabel("Giảng dạy"));
        JButton btnLopHP = createNavBtn("book", "Lớp học phần & nhập điểm");
        JButton btnLichDay = createNavBtn("calendar", "Lịch giảng dạy");
        sidebar.add(btnLopHP);
        sidebar.add(btnLichDay);

        sidebar.add(UIUtils.createSidebarGroupLabel("Cố vấn học tập"));
        JButton btnCoVan = createNavBtn("users", "Lớp cố vấn");
        sidebar.add(btnCoVan);

        // --- CARD LAYOUT ---
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(UIUtils.BG_APP);
        contentArea.setBorder(new EmptyBorder(25, 30, 25, 30));

        GVDashboardPanel dashboardPanel = new GVDashboardPanel(service, currentMaGV, hoTen);
        GVLopHocPhanPanel lopHocPhanPanel = new GVLopHocPhanPanel(service, currentMaGV);
        GVLichDayPanel lichDayPanel = new GVLichDayPanel(service, currentMaGV);
        GVCoVanPanel coVanPanel = new GVCoVanPanel(service, currentMaGV);

        contentArea.add(wrapScrollable(dashboardPanel), "TRANG_CHU");
        contentArea.add(lopHocPhanPanel, "LOP_HOC_PHAN");
        contentArea.add(lichDayPanel, "LICH_DAY");
        contentArea.add(coVanPanel, "CO_VAN");

        btnHome.addActionListener(e -> switchTab(btnHome, "TRANG_CHU", "Trang Chủ Tổng Quan"));
        btnLopHP.addActionListener(e -> switchTab(btnLopHP, "LOP_HOC_PHAN", "Lớp Học Phần & Nhập Điểm"));
        btnLichDay.addActionListener(e -> switchTab(btnLichDay, "LICH_DAY", "Lịch Giảng Dạy"));
        btnCoVan.addActionListener(e -> switchTab(btnCoVan, "CO_VAN", "Lớp Cố Vấn Học Tập"));

        sidebar.add(Box.createVerticalGlue());
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new Color(255, 255, 255, 25));
        sep2.setMaximumSize(new Dimension(6000, 1));
        sep2.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(sep2);
        sidebar.add(Box.createVerticalStrut(6));

        JButton btnLogout = UIUtils.createSidebarButton("logout", "Đăng xuất");
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

    private JScrollPane wrapScrollable(JPanel content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIUtils.BG_APP);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private void loadHeaderInfo() {
        Object[] info = service.getThongTinGV(currentMaGV);
        if (info != null) {
            hoTen = info[0] != null ? (String) info[0] : "Giảng viên";
            email = info[1] != null ? (String) info[1] : "";
        }
    }

    private void switchTab(JButton activeBtn, String cardName, String title) {
        lblHeaderTitle.setText(title);
        for (JButton btn : sidebarButtons) UIUtils.setSidebarActive(btn, btn == activeBtn);
        cardLayout.show(contentArea, cardName);
    }

    /** Avatar tròn hiển thị chữ cái đầu tên GV (giống AvatarIcon của StudentPanel). */
    class AvatarIcon implements Icon {
        private final String initial;
        AvatarIcon(String ten) {
            String t = (ten == null || ten.isBlank()) ? "GV" : ten.trim();
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
