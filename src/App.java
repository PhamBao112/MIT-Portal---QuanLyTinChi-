import service.StudentManagerService;
import ui.AdminPanel;
import ui.StudentPanel;
import ui.GiangVienPanel;
import config.DBConnect;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Random;

public class App {
    private static CardLayout rootLayout;
    private static JPanel rootPanel;
    private static StudentManagerService service;

    // ══════════════════════════════════════════════════════
    // BẢNG MÀU DARK THEME — TÔNG ĐỎ ĐÔ ẤM
    // ══════════════════════════════════════════════════════
    private static final Color DK_BG1        = new Color(22, 14, 18);
    private static final Color DK_BG2        = new Color(38, 22, 30);
    private static final Color DK_CARD       = new Color(32, 20, 26, 240);
    private static final Color DK_CARD_BDR   = new Color(90, 50, 60, 70);
    private static final Color DK_GLOW_RED   = new Color(180, 40, 40);
    private static final Color DK_GLOW_ORA   = new Color(220, 100, 35);
    private static final Color DK_INPUT_BG   = new Color(42, 28, 34);
    private static final Color DK_INPUT_BDR  = new Color(80, 50, 60);
    private static final Color DK_INPUT_FOC  = new Color(200, 55, 50, 170);
    private static final Color DK_TEXT       = new Color(240, 235, 238);
    private static final Color DK_TEXT_MUTED = new Color(160, 130, 145);
    private static final Color DK_BTN_L     = new Color(170, 30, 30);
    private static final Color DK_BTN_R     = new Color(220, 90, 25);
    private static final Color DK_GREEN     = new Color(50, 210, 50);
    private static final Color DK_TOGGLE_ON = new Color(170, 35, 35);
    private static final Color DK_TOGGLE_OFF= new Color(55, 35, 42);

    // ══════════════════════════════════════════════════════
    // TRẠNG THÁI ANIMATION
    // ══════════════════════════════════════════════════════
    private static float glowPhase = 0f;
    private static float logoScale = 1.0f;
    private static boolean logoGrowing = true;
    private static final ArrayList<Particle> particles = new ArrayList<>();
    private static final Random rand = new Random();
    private static int shakeOffset = 0, shakeCount = 0;
    private static javax.swing.Timer shakeTimer, animTimer;
    private static boolean isHoveringBtn = false;
    private static float btnGlow = 0f;
    private static boolean isSinhVien = true;
    // true = dang o man hinh dang nhap Quan tri vien (mo bang nut nho o goc tren-phai the)
    private static boolean isAdminMode = false;
    
    // NEW ANIMATION VARIABLES
    private static float togglePos = 0f;
    private static float cardAlpha = 0f;

    private static JPanel loginBg, loginCard, toggleTrack;
    private static JButton btnLoginMain;
    private static JLabel lblStatus, lblCopy;

    private static final int CARD_W = 470, CARD_H = 570;
    private static final String PH = "Nhập mã tài khoản...";

    // ══════════════════════════════════════════════════════
    // MAIN
    // ══════════════════════════════════════════════════════
    public static void main(String[] args) {
        // FlatLaf dark cho man hinh dang nhap (dong bo voi theme toi cua loginBg/loginCard).
        // Cac component tu ve (JButton, JPanel custom) khong bi anh huong; chi cac control
        // mac dinh (JCheckBox, JOptionPane...) duoc FlatLaf ve lai cho hop tong.
        try { UIManager.setLookAndFeel(new FlatDarkLaf()); } catch (Exception ignored) {}

        service = new StudentManagerService();

        JFrame frame = new JFrame("Hệ Thống Quản Lý Đào Tạo - MIT PORTAL");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);

        rootLayout = new CardLayout();
        rootPanel = new JPanel(rootLayout);

        for (int i = 0; i < 50; i++) particles.add(new Particle(rand, 1920, 1080));

        rootPanel.add(createLoginPanel(frame), "LOGIN");
        frame.add(rootPanel);
        frame.setVisible(true);

        startAnimations();
    }

    // ══════════════════════════════════════════════════════
    // MÀN HÌNH ĐĂNG NHẬP
    // ══════════════════════════════════════════════════════
    private static JPanel createLoginPanel(JFrame frame) {

        // ── NỀN ──
        loginBg = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, DK_BG1, getWidth(), getHeight(), DK_BG2));
                g2.fillRect(0, 0, getWidth(), getHeight());

                for (Particle p : particles) {
                    g2.setColor(new Color(p.r, p.g, p.b, (int)(p.alpha * 45)));
                    g2.fillOval((int)p.x - p.size, (int)p.y - p.size, p.size * 3, p.size * 3);
                    g2.setColor(new Color(p.r, p.g, p.b, (int)(p.alpha * 180)));
                    g2.fillOval((int)p.x, (int)p.y, p.size, p.size);
                }
                g2.dispose();
            }
        };
        loginBg.setBackground(DK_BG1);

        // ── CARD ──
        loginCard = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                if (cardAlpha <= 0f) return; // Wait until alpha is visible
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, cardAlpha));
                
                int w = getWidth(), h = getHeight(), arc = 24;

                for (int i = 5; i >= 1; i--) {
                    float t = (glowPhase + i * 0.07f) % 1.0f;
                    Color c = lerpColor(DK_GLOW_RED, DK_GLOW_ORA, t);
                    int a = Math.min(12 + (5 - i) * 18, 255);
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), a));
                    g2.setStroke(new BasicStroke(i * 2.5f));
                    g2.draw(new RoundRectangle2D.Float(i * 2, i * 2, w - i * 4, h - i * 4, arc, arc));
                }

                g2.setColor(DK_CARD);
                g2.fill(new RoundRectangle2D.Float(8, 8, w - 16, h - 16, arc, arc));
                g2.setColor(DK_CARD_BDR);
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Float(8, 8, w - 16, h - 16, arc, arc));

                // LOGO
                int cx = w / 2, cy = 65;
                int r = (int)(34 * logoScale);

                for (int i = 3; i >= 1; i--) {
                    g2.setColor(new Color(180, 40, 40, 12 + i * 8));
                    g2.fillOval(cx - r - i * 7, cy - r - i * 7, (r + i * 7) * 2, (r + i * 7) * 2);
                }

                g2.setPaint(new RadialGradientPaint(cx, cy, r,
                    new float[]{0f, 0.6f, 1f},
                    new Color[]{new Color(200, 50, 40), new Color(160, 30, 28), new Color(120, 20, 20)}));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);

                g2.setColor(new Color(255, 100, 70, 120));
                g2.setStroke(new BasicStroke(2.2f));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, (int)(22 * logoScale)));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("MIT", cx - fm.stringWidth("MIT") / 2, cy + fm.getAscent() / 2 - 2);

                g2.dispose();
            }
        };
        loginCard.setOpaque(false);
        loginBg.add(loginCard);

        // ── NỘI DUNG CARD ──
        int px = 50, pw = CARD_W - 100;
        int y = 112;

        JLabel lblSub = mkLabel("CỔNG THÔNG TIN ĐÀO TẠO", 12, Font.PLAIN, DK_TEXT_MUTED, SwingConstants.CENTER);
        lblSub.setBounds(px, y, pw, 20); loginCard.add(lblSub);
        y += 22;

        JLabel lblTitle = mkLabel("Chào mừng trở lại", 28, Font.BOLD, DK_TEXT, SwingConstants.CENTER);
        lblTitle.setBounds(px, y, pw, 40); loginCard.add(lblTitle);
        y += 42;

        JLabel lblDesc = mkLabel("Đăng nhập để tiếp tục hành trình học tập", 13, Font.PLAIN, DK_TEXT_MUTED, SwingConstants.CENTER);
        lblDesc.setBounds(px, y, pw, 22); loginCard.add(lblDesc);
        y += 36;

        // ── TOGGLE SLIDER (ANIMATED) ──
        toggleTrack = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                
                // Track background
                g2.setColor(DK_TOGGLE_OFF);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                
                // Sliding active pill
                int pillW = w / 2;
                int pillX = (int) (togglePos * pillW);
                g2.setColor(DK_TOGGLE_ON);
                g2.fillRoundRect(pillX, 0, pillW, h, 14, 14);
                g2.setColor(new Color(255, 90, 70, 30));
                g2.fillRoundRect(pillX, 0, pillW, h, 14, 14);
                
                g2.dispose();
            }
        };
        toggleTrack.setOpaque(false);
        toggleTrack.setBounds(px, y, pw, 42);
        
        JButton btnSV = mkToggleLabelBtn("Sinh Viên", true);
        JButton btnGV = mkToggleLabelBtn("Giảng Viên", false);
        btnSV.setBounds(0, 0, pw / 2, 42);
        btnGV.setBounds(pw / 2, 0, pw / 2, 42);
        toggleTrack.add(btnSV);
        toggleTrack.add(btnGV);
        loginCard.add(toggleTrack);

        JLabel lblUserLabel = mkLabel("Mã sinh viên", 14, Font.BOLD, DK_TEXT, SwingConstants.LEFT);
        btnSV.addActionListener(e -> { isSinhVien = true;  lblUserLabel.setText("Mã sinh viên"); });
        btnGV.addActionListener(e -> { isSinhVien = false; lblUserLabel.setText("Email giảng viên"); });
        y += 58;

        // ── NÚT NHỎ CHUYỂN SANG ĐĂNG NHẬP QUẢN TRỊ VIÊN (góc trên-phải thẻ) ──
        JButton btnAdminToggle = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(isAdminMode ? DK_GLOW_ORA : DK_TEXT_MUTED);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(3, 3, w - 6, h - 6, 8, 8);
                // Icon banh rang don gian: 1 vong tron + 4 rang nho
                int cx = w / 2, cy = h / 2, r = 4;
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                g2.drawLine(cx, cy - r - 3, cx, cy - r);
                g2.drawLine(cx, cy + r, cx, cy + r + 3);
                g2.drawLine(cx - r - 3, cy, cx - r, cy);
                g2.drawLine(cx + r, cy, cx + r + 3, cy);
                g2.dispose();
            }
        };
        btnAdminToggle.setOpaque(false);
        btnAdminToggle.setContentAreaFilled(false);
        btnAdminToggle.setBorderPainted(false);
        btnAdminToggle.setFocusPainted(false);
        btnAdminToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdminToggle.setToolTipText("Đăng nhập dành cho Cán Bộ Đào Tạo");
        btnAdminToggle.setBounds(CARD_W - 100, 24, 32, 32);
        loginCard.add(btnAdminToggle);

        // ── USER INPUT ──
        lblUserLabel.setBounds(px, y, pw, 22); loginCard.add(lblUserLabel);
        y += 26;

        JTextField txtUser = mkDarkInput(PH);
        txtUser.setBounds(px, y, pw, 46); loginCard.add(txtUser);
        y += 58;

        // Gan hanh dong cho nut chuyen Admin (dat sau khi co lblUserLabel/toggleTrack/lblTitle/txtUser)
        btnAdminToggle.addActionListener(e -> {
            isAdminMode = !isAdminMode;
            toggleTrack.setVisible(!isAdminMode);
            if (isAdminMode) {
                lblTitle.setText("Đăng Nhập Quản Trị");
                lblUserLabel.setText("Tài khoản quản trị");
            } else {
                lblTitle.setText("Chào mừng trở lại");
                lblUserLabel.setText(isSinhVien ? "Mã sinh viên" : "Email giảng viên");
            }
            txtUser.setText(PH);
            txtUser.setForeground(DK_TEXT_MUTED);
            btnAdminToggle.repaint();
        });

        // ── PASS INPUT ──
        JLabel lblPass = mkLabel("Mật khẩu", 14, Font.BOLD, DK_TEXT, SwingConstants.LEFT);
        lblPass.setBounds(px, y, pw, 22); loginCard.add(lblPass);
        y += 26;

        JPasswordField txtPass = new JPasswordField();
        txtPass.setEchoChar('●');
        styleDark(txtPass);
        txtPass.setBounds(px, y, pw, 46); loginCard.add(txtPass);
        y += 54;

        // ── CHECKBOX ──
        JCheckBox chkShow = new JCheckBox("Hiện mật khẩu");
        chkShow.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkShow.setForeground(DK_TEXT_MUTED);
        chkShow.setOpaque(false);
        chkShow.setFocusPainted(false);
        chkShow.setBounds(px, y, pw, 24); loginCard.add(chkShow);
        chkShow.addActionListener(e -> txtPass.setEchoChar(chkShow.isSelected() ? (char)0 : '●'));
        y += 36;

        // ── NÚT ĐĂNG NHẬP ──
        btnLoginMain = new JButton("ĐĂNG NHẬP") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                if (btnGlow > 0) {
                    g2.setColor(new Color(200, 60, 25, (int)(btnGlow * 55)));
                    g2.fillRoundRect(-5, -3, w + 10, h + 6, 18, 18);
                }
                g2.setPaint(new GradientPaint(0, 0, DK_BTN_L, w, 0, DK_BTN_R));
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                String txt = getText();
                g2.drawString(txt, (w - fm.stringWidth(txt)) / 2, (h + fm.getAscent()) / 2 - 3);
                g2.dispose();
            }
        };
        btnLoginMain.setOpaque(false);
        btnLoginMain.setContentAreaFilled(false);
        btnLoginMain.setBorderPainted(false);
        btnLoginMain.setFocusPainted(false);
        btnLoginMain.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLoginMain.setBounds(px, y, pw, 48);
        loginCard.add(btnLoginMain);

        btnLoginMain.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { isHoveringBtn = true; }
            public void mouseExited(MouseEvent e)  { isHoveringBtn = false; }
        });

        // ══════════════════════════════════════════════════
        // XỬ LÝ ĐĂNG NHẬP
        // ══════════════════════════════════════════════════
        ActionListener loginAction = e -> {
            String role = isAdminMode ? "Cán Bộ Đào Tạo" : (isSinhVien ? "Sinh Viên" : "Giảng Viên");
            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();
            if (user.equals(PH)) user = "";

            if (user.isEmpty() || pass.isEmpty()) {
                triggerShake();
                JOptionPane.showMessageDialog(frame, "Vui lòng nhập đầy đủ tài khoản và mật khẩu!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (authenticateDB(role, user, pass)) {
                if (animTimer != null) animTimer.stop();

                while (rootPanel.getComponentCount() > 1) rootPanel.remove(1);

                // ── Đổi sang FlatLaf Light cho các panel còn lại (đồng bộ với UIUtils) ──
                try { UIManager.setLookAndFeel(new FlatLightLaf()); } catch (Exception ignored) {}

                if (role.equals("Sinh Viên")) {
                    String maSV = user.toUpperCase();
                    rootPanel.add(new StudentPanel(service, maSV), "STUDENT_" + maSV);
                    rootLayout.show(rootPanel, "STUDENT_" + maSV);
                } else if (role.equals("Giảng Viên")) {
                    String maGV = service.getMaGVByEmail(user);
                    rootPanel.add(new GiangVienPanel(service, maGV), "GV_" + maGV);
                    rootLayout.show(rootPanel, "GV_" + maGV);
                } else {
                    rootPanel.add(new AdminPanel(service), "ADMIN");
                    rootLayout.show(rootPanel, "ADMIN");
                }

                txtUser.setText(""); txtPass.setText("");
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            } else {
                triggerShake();
                JOptionPane.showMessageDialog(frame,
                    "Sai tài khoản hoặc mật khẩu!\n(Hoặc mã sinh viên không tồn tại trong hệ thống)",
                    "Lỗi đăng nhập", JOptionPane.ERROR_MESSAGE);
            }
        };

        btnLoginMain.addActionListener(loginAction);
        txtPass.addActionListener(loginAction);

        // ── STATUS + COPYRIGHT ──
        lblStatus = mkLabel("● Đã kết nối CSDL", 12, Font.PLAIN, DK_GREEN, SwingConstants.CENTER);
        loginBg.add(lblStatus);

        lblCopy = mkLabel("© 2025 MIT Portal — Đại học Công Nghệ Miền Đông", 11, Font.PLAIN, new Color(100, 70, 80), SwingConstants.CENTER);
        loginBg.add(lblCopy);

        new Thread(() -> {
            try (Connection conn = DBConnect.getConnection()) {
                if (conn == null) throw new Exception();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("● Chưa kết nối CSDL");
                    lblStatus.setForeground(new Color(239, 68, 68));
                });
            }
        }).start();

        // CĂN GIỮA ĐỘNG
        loginBg.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int bw = loginBg.getWidth(), bh = loginBg.getHeight();
                int cx = (bw - CARD_W) / 2;
                int cy = (bh - CARD_H) / 2 - 20;
                loginCard.setBounds(cx + shakeOffset, cy, CARD_W, CARD_H);
                lblStatus.setBounds(cx, cy + CARD_H + 16, CARD_W, 20);
                lblCopy.setBounds(0, bh - 40, bw, 20);

                for (Particle p : particles) { p.maxW = bw; p.maxH = bh; }
            }
        });

        return loginBg;
    }

    // ══════════════════════════════════════════════════════
    // XÁC THỰC
    // ══════════════════════════════════════════════════════
    private static boolean authenticateDB(String role, String username, String password) {
        if (role.equals("Cán Bộ Đào Tạo")) {
            return username.equals("admin123") && password.equals("123");
        }
        if (role.equals("Giảng Viên")) {
            // Dang nhap GV bang Email, mat khau hardcode "123" (chua co cot mat khau rieng trong DB)
            try (Connection conn = DBConnect.getConnection()) {
                if (conn == null) return false;
                String sql = "SELECT MaGV FROM GIANG_VIEN WHERE Email = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next() && password.equals("123")) return true;
                }
            } catch (Exception e) { e.printStackTrace(); }
            return false;
        }
        try (Connection conn = DBConnect.getConnection()) {
            if (conn == null) return username.equals(password);
            String sql = "SELECT * FROM SINH_VIEN WHERE MaSV = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (password.equals("123")) return true;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // ══════════════════════════════════════════════════════
    // ANIMATION ENGINE
    // ══════════════════════════════════════════════════════
    private static void startAnimations() {
        animTimer = new javax.swing.Timer(33, e -> {
            glowPhase = (glowPhase + 0.006f) % 1.0f;

            if (logoGrowing) { logoScale += 0.0025f; if (logoScale >= 1.06f) logoGrowing = false; }
            else             { logoScale -= 0.0025f; if (logoScale <= 0.94f) logoGrowing = true;  }

            if (isHoveringBtn && btnGlow < 1f) btnGlow = Math.min(1f, btnGlow + 0.08f);
            if (!isHoveringBtn && btnGlow > 0f) btnGlow = Math.max(0f, btnGlow - 0.06f);

            // ANIMATION SLIDER & FADE IN
            float toggleTarget = isSinhVien ? 0f : 1f;
            togglePos += (toggleTarget - togglePos) * 0.22f;
            if (Math.abs(toggleTarget - togglePos) < 0.005f) togglePos = toggleTarget;

            if (cardAlpha < 1f) cardAlpha = Math.min(1f, cardAlpha + 0.045f);

            for (Particle p : particles) p.update();

            if (loginBg != null) loginBg.repaint();
            if (loginCard != null) loginCard.repaint();
            if (toggleTrack != null) toggleTrack.repaint();
            if (btnLoginMain != null) btnLoginMain.repaint();
        });
        animTimer.start();
    }

    private static void triggerShake() {
        shakeCount = 10;
        if (shakeTimer == null) {
            shakeTimer = new javax.swing.Timer(22, e -> {
                if (loginBg == null) return;
                int bw = loginBg.getWidth();
                int cx = (bw - CARD_W) / 2;
                int cy = loginCard.getY();

                if (shakeCount > 0) {
                    shakeOffset = (shakeCount % 2 == 0) ? 14 : -14;
                    shakeCount--;
                    loginCard.setLocation(cx + shakeOffset, cy);
                } else {
                    shakeOffset = 0;
                    loginCard.setLocation(cx, cy);
                    shakeTimer.stop();
                }
            });
        }
        shakeTimer.start();
    }

    // ══════════════════════════════════════════════════════
    // UI HELPERS
    // ══════════════════════════════════════════════════════
    private static JLabel mkLabel(String text, int size, int style, Color color, int align) {
        JLabel lbl = new JLabel(text, align);
        lbl.setFont(new Font("Segoe UI", style, size));
        lbl.setForeground(color);
        return lbl;
    }

    private static JTextField mkDarkInput(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        tf.setForeground(DK_TEXT_MUTED);
        styleDark(tf);
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (tf.getText().equals(placeholder)) { tf.setText(""); tf.setForeground(DK_TEXT); }
            }
            public void focusLost(FocusEvent e) {
                if (tf.getText().isEmpty()) { tf.setText(placeholder); tf.setForeground(DK_TEXT_MUTED); }
            }
        });
        return tf;
    }

    private static void styleDark(JComponent c) {
        c.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        c.setBackground(DK_INPUT_BG);
        c.setForeground(DK_TEXT);
        if (c instanceof JTextField) ((JTextField) c).setCaretColor(DK_TEXT);
        setDkBorder(c, false);
        c.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { setDkBorder(c, true); }
            public void focusLost(FocusEvent e) { setDkBorder(c, false); }
        });
    }

    private static void setDkBorder(JComponent c, boolean f) {
        c.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(f ? DK_INPUT_FOC : DK_INPUT_BDR, f ? 2 : 1, true),
            new EmptyBorder(f ? 8 : 9, f ? 14 : 15, f ? 8 : 9, f ? 14 : 15)));
    }

    /** Nút chữ trong suốt đặt chồng lên toggleTrack (chỉ vẽ text, nền do toggleTrack đảm nhiệm). */
    private static JButton mkToggleLabelBtn(String text, boolean isSVBtn) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = isSVBtn == isSinhVien;

                g2.setColor(active ? Color.WHITE : DK_TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent()) / 2 - 3);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    // ══════════════════════════════════════════════════════
    // PARTICLE
    // ══════════════════════════════════════════════════════
    static class Particle {
        float x, y, vx, vy, alpha;
        int r, g, b, size, maxW, maxH;
        Random rng;

        Particle(Random rng, int maxW, int maxH) {
            this.rng = rng; this.maxW = maxW; this.maxH = maxH; spawn();
        }

        void spawn() {
            x = rng.nextFloat() * maxW; y = rng.nextFloat() * maxH;
            vx = (rng.nextFloat() - 0.5f) * 0.3f; vy = (rng.nextFloat() - 0.5f) * 0.3f;
            alpha = rng.nextFloat() * 0.3f + 0.12f; size = rng.nextInt(4) + 2;
            switch (rng.nextInt(4)) {
                case 0: r = 190; g = 50; b = 50; break;
                case 1: r = 200; g = 100; b = 40; break;
                case 2: r = 160; g = 55; b = 80; break;
                default: r = 130; g = 60; b = 90; break;
            }
        }

        void update() {
            x += vx; y += vy;
            if (x < -15 || x > maxW + 15 || y < -15 || y > maxH + 15) spawn();
        }
    }
}