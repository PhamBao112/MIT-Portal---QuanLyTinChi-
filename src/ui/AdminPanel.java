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

public class AdminPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private StudentManagerService service;
    private CardLayout cardLayout;
    private JPanel contentArea;
    private ArrayList<JButton> sidebarButtons = new ArrayList<>();
    private JLabel lblHeaderTitle;

    // Khai báo chuẩn 6 Module con
    private AdminDashboardPanel adminDashboardPanel;
    private AdminUserPanel adminUserPanel;
    private AdminLopHocPhanPanel adminLopHocPhanPanel;
    private AdminCongNoPanel adminCongNoPanel;
    private AdminBaoCaoPanel adminBaoCaoPanel;
    private AdminImportPanel adminImportPanel; // <-- Module riêng biệt

    public AdminPanel(StudentManagerService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBackground(UIUtils.BG_APP);

        // --- 1. SIDEBAR (V3 - dùng UIUtils.createSidebarPanel) ---
        JPanel sidebar = UIUtils.createSidebarPanel();
        sidebar.setPreferredSize(new Dimension(240, 0));

        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(0, 6, 18, 6));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(2000, 60));

        JLabel brandTitle = new JLabel("MIT PORTAL");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandSub = new JLabel("Quản trị hệ thống");
        brandSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        brandSub.setForeground(UIUtils.SIDEBAR_TEXT_MUTED);
        brandSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.add(brandTitle);
        brand.add(brandSub);
        sidebar.add(brand);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 25));
        sep.setMaximumSize(new Dimension(6000, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(6));

        // --- 2. HEADER CÓ CHỌN HỌC KỲ ĐỘNG ---
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

        JPanel hkPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hkPanel.setBackground(UIUtils.WHITE);
        hkPanel.add(new JLabel("Học kỳ làm việc:"));
        JComboBox<String> cbHocKy = new JComboBox<>();
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT MaHK, TenHK FROM HOC_KY ORDER BY NamHoc DESC, MaHK DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) cbHocKy.addItem(rs.getString("MaHK") + " - " + rs.getString("TenHK"));
        } catch (Exception e) {
            cbHocKy.addItem("HK1_2425 - Học kỳ 1 2024-2025");
        }
        cbHocKy.setFont(UIUtils.FONT_BOLD);
        cbHocKy.setBackground(UIUtils.WHITE);
        hkPanel.add(cbHocKy);
        header.add(hkPanel, BorderLayout.EAST);

        // --- 3. CARD LAYOUT (GẮN 6 MODULE VÀO) ---
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(UIUtils.BG_APP);
        contentArea.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Khởi tạo các panel chức năng
        adminDashboardPanel = new AdminDashboardPanel();
        adminUserPanel = new AdminUserPanel(service);
        adminLopHocPhanPanel = new AdminLopHocPhanPanel();
        adminCongNoPanel = new AdminCongNoPanel();
        adminBaoCaoPanel = new AdminBaoCaoPanel(service);
        adminImportPanel = new AdminImportPanel(service);

        // Đổ 6 màn hình vào bộ khung
        contentArea.add(adminDashboardPanel, "DASHBOARD");
        contentArea.add(adminUserPanel, "QL_USERS");
        contentArea.add(adminLopHocPhanPanel, "QL_HOCPHAN");
        contentArea.add(adminCongNoPanel, "QL_CONGNO");
        contentArea.add(adminBaoCaoPanel, "BAO_CAO");
        contentArea.add(adminImportPanel, "IMPORT");

        // --- 4. TẠO 6 NÚT SIDEBAR (V3 - dùng UIUtils.createSidebarButton) ---
        sidebar.add(UIUtils.createSidebarGroupLabel("Tổng quan"));
        JButton btnDash = createNavBtn("dashboard", "Trang chủ tổng quan");
        sidebar.add(btnDash);

        sidebar.add(UIUtils.createSidebarGroupLabel("Quản lý"));
        JButton btnQLUser = createNavBtn("users", "SV & Giảng viên");
        JButton btnQLLHP  = createNavBtn("book", "Lớp học phần & xếp lịch");
        JButton btnCongNo = createNavBtn("card", "Thu công nợ");
        JButton btnBaoCao = createNavBtn("chart", "Thống kê & báo cáo");
        JButton btnImport = createNavBtn("upload", "Nạp dữ liệu Excel");
        sidebar.add(btnQLUser);
        sidebar.add(btnQLLHP);
        sidebar.add(btnCongNo);
        sidebar.add(btnBaoCao);
        sidebar.add(btnImport);

        btnDash.addActionListener(e -> switchTab(btnDash, "DASHBOARD", "Trang Chủ Tổng Quan"));
        btnQLUser.addActionListener(e -> switchTab(btnQLUser, "QL_USERS", "Danh Sách Sinh Viên và Giảng Viên"));
        btnQLLHP.addActionListener(e -> switchTab(btnQLLHP, "QL_HOCPHAN", "Quản Lý Lớp Học Phần & Xếp Lịch"));
        btnCongNo.addActionListener(e -> switchTab(btnCongNo, "QL_CONGNO", "Quản Lý Thu Học Phí Sinh Viên"));
        btnBaoCao.addActionListener(e -> switchTab(btnBaoCao, "BAO_CAO", "Báo Cáo & Thống Kê Tổng Hợp"));
        btnImport.addActionListener(e -> switchTab(btnImport, "IMPORT", "Nạp Dữ Liệu Từ Tệp Excel"));

        // Đẩy nút Đăng xuất xuống cuối sidebar
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

        // Đặt trạng thái active mặc định cho tab đầu tiên
        UIUtils.setSidebarActive(btnDash, true);

        rightPanel.add(header, BorderLayout.NORTH);
        rightPanel.add(contentArea, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        // THIET LAP TRIGGER DOI HOC KY CHO CAC MODULE LIEN QUAN
        // FIX: tach logic ra Runnable va GOI THU CONG 1 lan sau khi setSelectedIndex(0), vi Swing
        // KHONG ban ActionEvent khi setSelectedIndex() duoc goi voi index dang la index da chon san
        // (combo mac dinh da chon index 0 ngay khi them item dau tien) -> truoc day Trang Chu Admin,
        // Cong No, Lop Hoc Phan deu trong trong cho den khi admin tu tay doi combo.
        Runnable applyHocKy = () -> {
            String fullHocKy = (String) cbHocKy.getSelectedItem();
            if (fullHocKy != null) {
                String maHK = fullHocKy.contains("-") ? fullHocKy.split("-")[0].trim() : fullHocKy;
                if (adminDashboardPanel != null) adminDashboardPanel.updateData(maHK);
                if (adminCongNoPanel != null) adminCongNoPanel.updateData(maHK);
                if (adminLopHocPhanPanel != null) adminLopHocPhanPanel.updateData(maHK);
            }
        };
        cbHocKy.addActionListener(e -> applyHocKy.run());
        if (cbHocKy.getItemCount() > 0) cbHocKy.setSelectedIndex(0);
        applyHocKy.run();
    }

    /** Tạo 1 nút sidebar dùng UIUtils.createSidebarButton + đăng ký vào danh sách để quản lý active state. */
    private JButton createNavBtn(String icon, String text) {
        JButton btn = UIUtils.createSidebarButton(icon, text);
        sidebarButtons.add(btn);
        return btn;
    }

    private void switchTab(JButton activeBtn, String cardName, String title) {
        lblHeaderTitle.setText(title);
        for (JButton btn : sidebarButtons) UIUtils.setSidebarActive(btn, btn == activeBtn);
        cardLayout.show(contentArea, cardName);
    }
}
