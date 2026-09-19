import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * === LOGIN DEMO - DARK THEME + ANIMATED GLOWING BORDER ===
 * File demo độc lập, chạy riêng để xem trước giao diện.
 * Không ảnh hưởng đến App.java gốc.
 */
public class LoginDemo extends JFrame {

    // ══════════════════════════════════════════════════════
    // BẢNG MÀU DARK THEME
    // ══════════════════════════════════════════════════════
    private static final Color BG_PRIMARY    = new Color(18, 18, 42);
    private static final Color BG_SECONDARY  = new Color(28, 32, 62);
    private static final Color CARD_BG       = new Color(26, 26, 52, 235);
    private static final Color CARD_BORDER   = new Color(65, 65, 105, 70);
    private static final Color GLOW_RED      = new Color(200, 40, 40);
    private static final Color GLOW_ORANGE   = new Color(245, 125, 40);
    private static final Color INPUT_BG      = new Color(36, 36, 62);
    private static final Color INPUT_BORDER  = new Color(65, 65, 100);
    private static final Color INPUT_FOCUS   = new Color(210, 65, 55, 160);
    private static final Color TEXT_PRIMARY  = new Color(232, 232, 248);
    private static final Color TEXT_MUTED    = new Color(128, 128, 168);
    private static final Color BTN_LEFT      = new Color(185, 35, 35);
    private static final Color BTN_RIGHT     = new Color(235, 105, 30);
    private static final Color GREEN_DOT     = new Color(50, 210, 50);
    private static final Color TOGGLE_ON     = new Color(185, 40, 40);
    private static final Color TOGGLE_OFF    = new Color(48, 48, 78);

    // ══════════════════════════════════════════════════════
    // TRẠNG THÁI ANIMATION
    // ══════════════════════════════════════════════════════
    private float glowPhase = 0f;
    private float logoScale = 1.0f;
    private boolean logoGrowing = true;
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final Random rand = new Random();
    private int shakeOffset = 0;
    private int shakeCount = 0;
    private javax.swing.Timer shakeTimer;
    private boolean isHoveringBtn = false;
    private float btnGlow = 0f;

    // ══════════════════════════════════════════════════════
    // UI COMPONENTS
    // ══════════════════════════════════════════════════════
    private JPanel bgPanel, cardPanel;
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnSV, btnCB, btnLogin;
    private JLabel lblUserLabel;
    private boolean isSinhVien = true;

    private static final int FRAME_W = 950, FRAME_H = 680;
    private static final int CARD_W = 420, CARD_H = 540;
    private static final String PLACEHOLDER = "Nhập mã tài khoản...";

    // ══════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ══════════════════════════════════════════════════════
    public LoginDemo() {
        setTitle("MIT PORTAL - Quản lý tín chỉ [DEMO]");
        setSize(FRAME_W, FRAME_H);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        for (int i = 0; i < 40; i++) {
            particles.add(new Particle(rand, FRAME_W, FRAME_H));
        }

        buildUI();
        startAnimations();
    }

    // ══════════════════════════════════════════════════════
    // XÂY DỰNG GIAO DIỆN
    // ══════════════════════════════════════════════════════
    private void buildUI() {

        // ── NỀN CHÍNH (Gradient + Particles) ──
        bgPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, BG_PRIMARY, getWidth(), getHeight(), BG_SECONDARY));
                g2.fillRect(0, 0, getWidth(), getHeight());

                for (Particle p : particles) {
                    g2.setColor(new Color(p.r, p.g, p.b, (int)(p.alpha * 50)));
                    g2.fillOval((int)p.x - p.size, (int)p.y - p.size, p.size * 3, p.size * 3);
                    g2.setColor(new Color(p.r, p.g, p.b, (int)(p.alpha * 190)));
                    g2.fillOval((int)p.x, (int)p.y, p.size, p.size);
                }
                g2.dispose();
            }
        };
        setContentPane(bgPanel);

        // ── CARD ĐĂNG NHẬP (Glowing Border) ──
        int cardX = (FRAME_W - CARD_W) / 2;
        int cardY = (FRAME_H - CARD_H) / 2 - 25;

        cardPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), arc = 24;

                // ── Animated glowing border (5 lớp mờ dần) ──
                for (int i = 5; i >= 1; i--) {
                    float t = (glowPhase + i * 0.07f) % 1.0f;
                    Color c = lerpColor(GLOW_RED, GLOW_ORANGE, t);
                    int a = 12 + (5 - i) * 18;
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(a, 255)));
                    g2.setStroke(new BasicStroke(i * 2.5f));
                    g2.draw(new RoundRectangle2D.Float(i * 2, i * 2, w - i * 4, h - i * 4, arc, arc));
                }

                // Card fill
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(8, 8, w - 16, h - 16, arc, arc));
                g2.setColor(CARD_BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Float(8, 8, w - 16, h - 16, arc, arc));

                // ── LOGO MIT (nhịp đập phóng to/thu nhỏ) ──
                int cx = w / 2, cy = 62;
                int r = (int)(32 * logoScale);

                // Outer glow rings
                for (int i = 3; i >= 1; i--) {
                    g2.setColor(new Color(200, 50, 50, 15 + i * 8));
                    g2.fillOval(cx - r - i * 6, cy - r - i * 6, (r + i * 6) * 2, (r + i * 6) * 2);
                }

                // Logo gradient circle
                g2.setPaint(new RadialGradientPaint(cx, cy, r,
                    new float[]{0f, 0.7f, 1f},
                    new Color[]{new Color(220, 60, 50), new Color(180, 35, 35), new Color(140, 25, 25)}));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);

                // Ring
                g2.setColor(new Color(255, 110, 80, 130));
                g2.setStroke(new BasicStroke(2.2f));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);

                // "MIT" text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, (int)(20 * logoScale)));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("MIT", cx - fm.stringWidth("MIT") / 2, cy + fm.getAscent() / 2 - 2);

                g2.dispose();
            }
        };
        cardPanel.setOpaque(false);
        cardPanel.setBounds(cardX, cardY, CARD_W, CARD_H);
        bgPanel.add(cardPanel);

        // ── NỘI DUNG TRONG CARD ──
        int px = 45;                    // padding trái
        int pw = CARD_W - 90;          // chiều rộng nội dung
        int y = 105;                    // bắt đầu dưới logo

        // Subtitle
        JLabel lblSub = makeLabel("CỔNG THÔNG TIN ĐÀO TẠO", 11, Font.PLAIN, TEXT_MUTED, SwingConstants.CENTER);
        lblSub.setBounds(px, y, pw, 18); cardPanel.add(lblSub);
        y += 20;

        // Title
        JLabel lblTitle = makeLabel("Chào mừng trở lại", 25, Font.BOLD, TEXT_PRIMARY, SwingConstants.CENTER);
        lblTitle.setBounds(px, y, pw, 36); cardPanel.add(lblTitle);
        y += 38;

        // Description
        JLabel lblDesc = makeLabel("Đăng nhập để tiếp tục hành trình học tập", 12, Font.PLAIN, TEXT_MUTED, SwingConstants.CENTER);
        lblDesc.setBounds(px, y, pw, 20); cardPanel.add(lblDesc);
        y += 34;

        // ── TOGGLE BUTTONS (Sinh Viên / Cán Bộ) ──
        btnSV = createToggleBtn("Sinh Viên");
        btnCB = createToggleBtn("Cán Bộ Đào Tạo");
        btnSV.setBounds(px, y, pw / 2 - 4, 38); cardPanel.add(btnSV);
        btnCB.setBounds(px + pw / 2 + 4, y, pw / 2 - 4, 38); cardPanel.add(btnCB);
        btnSV.addActionListener(e -> { isSinhVien = true;  updateToggle(); });
        btnCB.addActionListener(e -> { isSinhVien = false; updateToggle(); });
        y += 54;

        // ── USER INPUT ──
        lblUserLabel = makeLabel("Mã sinh viên", 13, Font.BOLD, TEXT_PRIMARY, SwingConstants.LEFT);
        lblUserLabel.setBounds(px, y, pw, 20); cardPanel.add(lblUserLabel);
        y += 24;

        txtUser = createDarkInput(PLACEHOLDER);
        txtUser.setBounds(px, y, pw, 44); cardPanel.add(txtUser);
        y += 54;

        // ── PASSWORD INPUT ──
        JLabel lblPass = makeLabel("Mật khẩu", 13, Font.BOLD, TEXT_PRIMARY, SwingConstants.LEFT);
        lblPass.setBounds(px, y, pw, 20); cardPanel.add(lblPass);
        y += 24;

        txtPass = new JPasswordField();
        txtPass.setEchoChar('●');
        styleDarkField(txtPass);
        txtPass.setBounds(px, y, pw, 44); cardPanel.add(txtPass);
        y += 50;

        // ── CHECKBOX HIỆN MẬT KHẨU ──
        JCheckBox chkShow = new JCheckBox("Hiện mật khẩu");
        chkShow.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkShow.setForeground(TEXT_MUTED);
        chkShow.setOpaque(false);
        chkShow.setFocusPainted(false);
        chkShow.setBounds(px, y, pw, 22); cardPanel.add(chkShow);
        chkShow.addActionListener(e -> txtPass.setEchoChar(chkShow.isSelected() ? (char)0 : '●'));
        y += 32;

        // ── NÚT ĐĂNG NHẬP (Gradient + Hover Glow) ──
        btnLogin = new JButton("ĐĂNG NHẬP") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Hover outer glow
                if (btnGlow > 0) {
                    int gAlpha = (int)(btnGlow * 50);
                    g2.setColor(new Color(220, 70, 30, gAlpha));
                    g2.fillRoundRect(-5, -3, w + 10, h + 6, 16, 16);
                }

                // Gradient fill
                g2.setPaint(new GradientPaint(0, 0, BTN_LEFT, w, 0, BTN_RIGHT));
                g2.fillRoundRect(0, 0, w, h, 12, 12);

                // Text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                FontMetrics fm = g2.getFontMetrics();
                String txt = getText();
                g2.drawString(txt, (w - fm.stringWidth(txt)) / 2, (h + fm.getAscent()) / 2 - 3);
                g2.dispose();
            }
        };
        btnLogin.setOpaque(false);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setBounds(px, y, pw, 46);
        cardPanel.add(btnLogin);

        btnLogin.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { isHoveringBtn = true; }
            public void mouseExited(MouseEvent e)  { isHoveringBtn = false; }
        });
        btnLogin.addActionListener(e -> handleLogin());
        txtPass.addActionListener(e -> handleLogin());

        // ── DB STATUS INDICATOR ──
        JLabel lblStatus = new JLabel("● Đã kết nối CSDL (Demo)", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(GREEN_DOT);
        lblStatus.setBounds(cardX, cardY + CARD_H + 18, CARD_W, 20);
        bgPanel.add(lblStatus);

        // ── COPYRIGHT FOOTER ──
        JLabel lblCopy = new JLabel("© 2025 MIT Portal — Đại học Công Nghệ Miền Đông", SwingConstants.CENTER);
        lblCopy.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCopy.setForeground(new Color(80, 80, 120));
        lblCopy.setBounds(0, FRAME_H - 60, FRAME_W, 20);
        bgPanel.add(lblCopy);
    }

    // ══════════════════════════════════════════════════════
    // XỬ LÝ ĐĂNG NHẬP (DEMO)
    // ══════════════════════════════════════════════════════
    private void handleLogin() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword()).trim();

        // Không lấy placeholder làm input
        if (user.equals(PLACEHOLDER)) user = "";

        if (user.isEmpty() || pass.isEmpty()) {
            triggerShake();
            showStyledMessage("Vui lòng nhập đầy đủ tài khoản và mật khẩu!", "Cảnh báo", false);
            return;
        }

        // Demo: admin123/123 hoặc bất kỳ SV nào + pass 123
        boolean ok = false;
        if (!isSinhVien && user.equals("admin123") && pass.equals("123")) ok = true;
        if (isSinhVien && user.toUpperCase().startsWith("SV") && pass.equals("123")) ok = true;

        if (ok) {
            showStyledMessage(
                "Đăng nhập thành công!\n\nVai trò: " + (isSinhVien ? "Sinh Viên" : "Cán Bộ Đào Tạo")
                + "\nTài khoản: " + user.toUpperCase(),
                "Thành công", true);
        } else {
            triggerShake();
            showStyledMessage("Sai tài khoản hoặc mật khẩu!\n\n(Demo: SV001 / 123  hoặc  admin123 / 123)", "Lỗi đăng nhập", false);
        }
    }

    private void showStyledMessage(String msg, String title, boolean success) {
        UIManager.put("OptionPane.background", new Color(35, 35, 60));
        UIManager.put("Panel.background", new Color(35, 35, 60));
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        JOptionPane.showMessageDialog(this, msg, title,
            success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    // ══════════════════════════════════════════════════════
    // HELPER: TẠO LABEL
    // ══════════════════════════════════════════════════════
    private JLabel makeLabel(String text, int size, int style, Color color, int align) {
        JLabel lbl = new JLabel(text, align);
        lbl.setFont(new Font("Segoe UI", style, size));
        lbl.setForeground(color);
        return lbl;
    }

    // ══════════════════════════════════════════════════════
    // HELPER: TẠO INPUT DARK THEME + PLACEHOLDER
    // ══════════════════════════════════════════════════════
    private JTextField createDarkInput(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        tf.setForeground(TEXT_MUTED);
        styleDarkField(tf);

        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (tf.getText().equals(placeholder)) {
                    tf.setText("");
                    tf.setForeground(TEXT_PRIMARY);
                }
            }
            public void focusLost(FocusEvent e) {
                if (tf.getText().isEmpty()) {
                    tf.setText(placeholder);
                    tf.setForeground(TEXT_MUTED);
                }
            }
        });
        return tf;
    }

    private void styleDarkField(JComponent comp) {
        comp.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comp.setBackground(INPUT_BG);
        comp.setForeground(TEXT_PRIMARY);
        if (comp instanceof JTextField) ((JTextField)comp).setCaretColor(TEXT_PRIMARY);
        setFieldBorder(comp, false);

        comp.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { setFieldBorder(comp, true);  }
            public void focusLost(FocusEvent e)   { setFieldBorder(comp, false); }
        });
    }

    private void setFieldBorder(JComponent c, boolean focused) {
        if (focused) {
            c.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(INPUT_FOCUS, 2, true), new EmptyBorder(7, 13, 7, 13)));
        } else {
            c.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(INPUT_BORDER, 1, true), new EmptyBorder(8, 14, 8, 14)));
        }
    }

    // ══════════════════════════════════════════════════════
    // HELPER: TẠO TOGGLE BUTTON
    // ══════════════════════════════════════════════════════
    private JButton createToggleBtn(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean active = text.startsWith("Sinh") ? isSinhVien : !isSinhVien;

                // Background
                g2.setColor(active ? TOGGLE_ON : TOGGLE_OFF);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                // Subtle inner highlight when active
                if (active) {
                    g2.setColor(new Color(255, 100, 80, 35));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }

                // Text
                g2.setColor(active ? Color.WHITE : TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
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

    private void updateToggle() {
        lblUserLabel.setText(isSinhVien ? "Mã sinh viên" : "Mã cán bộ");
        btnSV.repaint();
        btnCB.repaint();
    }

    // ══════════════════════════════════════════════════════
    // ANIMATION: RUNG LẮC KHI SAI MẬT KHẨU
    // ══════════════════════════════════════════════════════
    private void triggerShake() {
        shakeCount = 10;
        if (shakeTimer == null) {
            shakeTimer = new javax.swing.Timer(22, e -> {
                if (shakeCount > 0) {
                    shakeOffset = (shakeCount % 2 == 0) ? 12 : -12;
                    shakeCount--;
                    cardPanel.setLocation((FRAME_W - CARD_W) / 2 + shakeOffset, cardPanel.getY());
                } else {
                    shakeOffset = 0;
                    cardPanel.setLocation((FRAME_W - CARD_W) / 2, cardPanel.getY());
                    shakeTimer.stop();
                }
            });
        }
        shakeTimer.start();
    }

    // ══════════════════════════════════════════════════════
    // ANIMATION: VÒNG LẶP CHÍNH (30 FPS)
    // ══════════════════════════════════════════════════════
    private void startAnimations() {
        new javax.swing.Timer(33, e -> {
            // Glow phase xoay liên tục
            glowPhase = (glowPhase + 0.006f) % 1.0f;

            // Logo nhịp đập
            if (logoGrowing) { logoScale += 0.0025f; if (logoScale >= 1.06f) logoGrowing = false; }
            else             { logoScale -= 0.0025f; if (logoScale <= 0.94f) logoGrowing = true;  }

            // Button glow fade in/out
            if (isHoveringBtn && btnGlow < 1f) btnGlow = Math.min(1f, btnGlow + 0.08f);
            if (!isHoveringBtn && btnGlow > 0f) btnGlow = Math.max(0f, btnGlow - 0.06f);

            // Particles di chuyển
            for (Particle p : particles) p.update();

            bgPanel.repaint();
            cardPanel.repaint();
            if (btnGlow > 0) btnLogin.repaint();
        }).start();
    }

    // ══════════════════════════════════════════════════════
    // TIỆN ÍCH MÀU SẮC
    // ══════════════════════════════════════════════════════
    private static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }

    // ══════════════════════════════════════════════════════
    // LỚP PARTICLE (Hạt trôi nổi trên nền)
    // ══════════════════════════════════════════════════════
    static class Particle {
        float x, y, vx, vy, alpha;
        int r, g, b, size, maxW, maxH;
        Random rng;

        Particle(Random rng, int maxW, int maxH) {
            this.rng = rng;
            this.maxW = maxW;
            this.maxH = maxH;
            spawn();
        }

        void spawn() {
            x = rng.nextFloat() * maxW;
            y = rng.nextFloat() * maxH;
            vx = (rng.nextFloat() - 0.5f) * 0.35f;
            vy = (rng.nextFloat() - 0.5f) * 0.35f;
            alpha = rng.nextFloat() * 0.35f + 0.15f;
            size = rng.nextInt(4) + 2;
            switch (rng.nextInt(4)) {
                case 0:  r=200; g=55;  b=55;  break;   // đỏ
                case 1:  r=225; g=115; b=45;  break;   // cam
                case 2:  r=170; g=65;  b=130; break;   // tím hồng
                default: r=100; g=100; b=180; break;   // xanh dương nhạt
            }
        }

        void update() {
            x += vx;
            y += vy;
            if (x < -15 || x > maxW + 15 || y < -15 || y > maxH + 15) spawn();
        }
    }

    // ══════════════════════════════════════════════════════
    // MAIN
    // ══════════════════════════════════════════════════════
    public static void main(String[] args) {
        // Tắt LookAndFeel hệ thống để giữ dark theme thuần
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LoginDemo().setVisible(true));
    }
}

