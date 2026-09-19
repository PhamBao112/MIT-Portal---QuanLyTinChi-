package utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import config.DBConnect;

/**
 * ============================================================
 *  AdminUIUtils — GIAO DIỆN ADMIN MỚI
 * ------------------------------------------------------------
 *  - Clone từ UIUtils nhưng tùy chỉnh màu sắc và bo góc
 *    để khớp hoàn toàn với bản thiết kế HTML mit-portal-preview.html
 * ============================================================
 */
public class AdminUIUtils {
    // BỘ MÀU CHUẨN CỦA ĐẠI HỌC CÔNG NGHỆ MIỀN ĐÔNG (MIT) - Dành riêng cho ADMIN
    public static final Color MIT_RED       = Color.decode("#7a0c0c");      // Maroon
    public static final Color MIT_ORANGE    = Color.decode("#e65100");      // Ember
    public static final Color MIT_YELLOW    = Color.decode("#ffb300");      // Gold
    public static final Color MIT_RED_LIGHT = Color.decode("#fdf6f0");      // Hover row

    public static final Color BG_APP      = Color.decode("#f3f1ee");
    public static final Color BORDER      = Color.decode("#e7e2da");
    public static final Color TEXT_MAIN   = Color.decode("#241c1a");
    public static final Color TEXT_MUTED  = Color.decode("#6b5f58");
    public static final Color GREEN_500   = Color.decode("#1e8e5a");
    public static final Color RED_500     = Color.decode("#c62828");
    public static final Color WHITE       = Color.decode("#ffffff");

    public static final Font FONT_NORMAL  = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 18);

    // --- FONT ĐẶC BIỆT ĐỂ HIỂN THỊ EMOJI TRÊN WINDOWS ---
    public static final Font FONT_EMOJI   = new Font("Segoe UI Emoji", Font.BOLD, 14);
    public static final Font FONT_LOGO    = new Font("Segoe UI Emoji", Font.BOLD, 22);

    // Màu phụ trợ mới
    private static final Color SHADOW       = new Color(15, 23, 42, 40);
    private static final Color ROW_STRIPE   = new Color(250, 251, 253);
    private static final Color ROW_HOVER    = Color.decode("#fdf6f0");

    // ================== SIDEBAR / KPI CARD / BADGE / PROGRESS ==================
    public static final Color SIDEBAR_BG      = Color.decode("#2a1414");
    public static final Color SIDEBAR_BG_DARK = Color.decode("#5c0909");
    public static final Color SIDEBAR_ACTIVE  = new Color(255, 255, 255, 30);
    public static final Color SIDEBAR_ACCENT  = MIT_YELLOW;
    public static final Color SIDEBAR_TEXT       = Color.decode("#e8d9d2");
    public static final Color SIDEBAR_TEXT_MUTED = Color.decode("#a9897f");

    public static final Color BADGE_OK_BG      = Color.decode("#e8f5e9");
    public static final Color BADGE_OK_FG      = Color.decode("#1e8e5a");
    public static final Color BADGE_WARN_BG    = Color.decode("#fff8e1");
    public static final Color BADGE_WARN_FG    = Color.decode("#a56b00");
    public static final Color BADGE_BAD_BG     = Color.decode("#fdeaea");
    public static final Color BADGE_BAD_FG     = Color.decode("#c62828");
    public static final Color BADGE_PENDING_BG = Color.decode("#eceff1");
    public static final Color BADGE_PENDING_FG = Color.decode("#546e7a");

    public static final int BADGE_OK = 0, BADGE_WARN = 1, BADGE_BAD = 2, BADGE_PENDING = 3;

    // ================== CARD SHELL ==================
    public static JPanel createCardShell(String title, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.setColor(BORDER);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 2f, getHeight() - 2f, 14, 14));
                g2.dispose();
            }
        };
        card.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 2, 0, accent), new EmptyBorder(14, 20, 12, 20)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(TEXT_MAIN);
        header.add(lbl, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);
        return card;
    }

    public static JButton createDangerBtn(String text) {
        return buildFancyButton(text, RED_500, RED_500.darker(), WHITE, true);
    }

    // ================== NÚT BẤM CHÍNH (PRIMARY) ==================
    public static JButton createPrimaryBtn(String text) {
        return buildFancyButton(text, MIT_RED, MIT_ORANGE, WHITE, true);
    }

    public static JButton createSecondaryBtn(String text) {
        return buildFancyButton(text, WHITE, MIT_RED_LIGHT, MIT_RED, false);
    }

    private static JButton buildFancyButton(String text, Color base, Color hover, Color fg, boolean filled) {
        JButton btn = new JButton(text) {
            private float hoverT = 0f;
            private final javax.swing.Timer anim = new javax.swing.Timer(15, e -> {
                float target = getModel().isRollover() ? 1f : 0f;
                hoverT += (target - hoverT) * 0.25f;
                if (Math.abs(target - hoverT) < 0.01f) hoverT = target;
                repaint();
            });
            {
                anim.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int arc = 9; // Góc bo 9px như thiết kế HTML
                boolean pressed = getModel().isPressed();

                if (!pressed) {
                    g2.setColor(SHADOW);
                    g2.fill(new RoundRectangle2D.Float(2, 4, w - 4, h - 4, arc, arc));
                }

                Color from = filled ? base : WHITE;
                Color to   = filled ? hover : hover;
                Color cur  = lerpColor(from, to, hoverT);

                int yOff = pressed ? 2 : 0;
                if (filled) {
                    g2.setPaint(new GradientPaint(0, 0, cur, w, h, lerpColor(cur, cur.darker(), 0.15f)));
                } else {
                    g2.setColor(cur);
                }
                g2.fill(new RoundRectangle2D.Float(0, yOff, w - 2, h - 2 - yOff, arc, arc));

                if (!filled) {
                    g2.setColor(base);
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.draw(new RoundRectangle2D.Float(0.7f, yOff + 0.7f, w - 3.4f, h - 3.4f - yOff, arc, arc));
                }

                g2.setColor(fg);
                g2.setFont(FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h - yOff + fm.getAscent()) / 2 - 4 + yOff;
                g2.drawString(text, tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(FONT_BOLD);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(12, 30, 12, 30));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ================== Ô NHẬP LIỆU ==================
    public static JTextField createInput() {
        JTextField tf = new RoundedTextField();
        tf.setFont(FONT_NORMAL);
        tf.setForeground(TEXT_MAIN);
        tf.setBackground(WHITE);
        tf.setBorder(new EmptyBorder(10, 15, 10, 15));
        tf.setOpaque(false);
        return tf;
    }

    private static class RoundedTextField extends JTextField {
        private boolean focused = false;

        RoundedTextField() {
            addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent e) { focused = true; repaint(); }
                public void focusLost(java.awt.event.FocusEvent e)   { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 9, 9)); // Góc bo 9px
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(focused ? MIT_RED : BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1.2f));
            g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, getWidth() - 2.2f, getHeight() - 2.2f, 9, 9));
            g2.dispose();
        }
    }

    public static JPanel createFormRow(String labelText, JComponent input) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(WHITE);
        wrapper.setMaximumSize(new Dimension(500, 70));
        wrapper.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_MUTED);
        lbl.setBorder(new EmptyBorder(0, 2, 6, 0));

        wrapper.add(lbl, BorderLayout.NORTH);
        wrapper.add(input, BorderLayout.CENTER);
        return wrapper;
    }

    public static String[] loadHocKyOptionsForStudent(JComboBox<String> combo, String maSV, String currentMaHK) {
        combo.removeAllItems();
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT DISTINCT h.MaHK, h.TenHK, h.NamHoc FROM HOC_KY h " +
                 "JOIN LOP_HOC_PHAN lhp ON lhp.MaHK = h.MaHK " +
                 "JOIN KET_QUA_DANG_KY kq ON kq.MaLHP = lhp.MaLHP " +
                 "WHERE kq.MaSV = ? ORDER BY h.NamHoc DESC, h.MaHK DESC")) {
            ps.setString(1, maSV);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) combo.addItem(rs.getString("MaHK") + " - " + rs.getString("TenHK"));
        } catch (Exception e) { e.printStackTrace(); }

        if (combo.getItemCount() == 0) {
            return new String[]{"", "Chưa có dữ liệu"};
        }
        String target = null;
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i);
            if (item.startsWith(currentMaHK + " ")) { target = item; break; }
        }
        if (target == null) target = combo.getItemAt(0);
        combo.setSelectedItem(target); 
        String maHK = target.split("-")[0].trim();
        String tenHK = target.substring(target.indexOf("-") + 1).trim();
        return new String[]{maHK, tenHK};
    }

    // ================== BẢNG DỮ LIỆU (TABLE) ==================
    public static void styleTable(JTable table) {
        table.setFont(FONT_NORMAL);
        table.setRowHeight(42);
        table.setSelectionBackground(MIT_RED_LIGHT);
        table.setSelectionForeground(MIT_RED);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(WHITE);
        table.setFillsViewportHeight(true);

        JTableHeader th = table.getTableHeader();
        th.setFont(FONT_BOLD);
        th.setForeground(TEXT_MAIN);
        th.setBackground(BG_APP);
        th.setPreferredSize(new Dimension(100, 46));
        th.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                l.setHorizontalAlignment(JLabel.LEFT);
                l.setBackground(BG_APP);
                l.setForeground(TEXT_MAIN);
                l.setFont(FONT_BOLD);
                l.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 0, 2, 0, MIT_RED),
                    new EmptyBorder(6, 14, 6, 10)));
                return l;
            }
        });

        table.putClientProperty("hoveredRow", -1);
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                Object old = table.getClientProperty("hoveredRow");
                if (old == null || !old.equals(row)) {
                    table.putClientProperty("hoveredRow", row);
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                table.putClientProperty("hoveredRow", -1);
                table.repaint();
            }
        });

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, f, row, col);
                l.setBorder(new EmptyBorder(6, 14, 6, 10));
                l.setFont(FONT_NORMAL);

                Integer hovered = (Integer) table.getClientProperty("hoveredRow");
                if (sel) {
                    l.setBackground(MIT_RED_LIGHT);
                    l.setForeground(MIT_RED);
                    l.setFont(FONT_BOLD);
                } else if (hovered != null && hovered == row) {
                    l.setBackground(ROW_HOVER);
                    l.setForeground(TEXT_MAIN);
                } else {
                    l.setBackground(row % 2 == 0 ? WHITE : ROW_STRIPE);
                    l.setForeground(TEXT_MAIN);
                }
                return l;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            clamp((int) (a.getRed()   + (b.getRed()   - a.getRed())   * t)),
            clamp((int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t)),
            clamp((int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)));
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    // ================== SIDEBAR NAV ==================
    public static JPanel createSidebarPanel() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, SIDEBAR_BG, 0, getHeight(), SIDEBAR_BG_DARK));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(18, 10, 18, 10));
        return p;
    }

    public static JLabel createSidebarGroupLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(SIDEBAR_TEXT_MUTED);
        l.setBorder(new EmptyBorder(14, 10, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    public static JButton createSidebarButton(String icon, String text) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = Boolean.TRUE.equals(getClientProperty("active"));
                boolean hover = getModel().isRollover();
                if (active) {
                    g2.setColor(SIDEBAR_ACTIVE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
                    g2.setColor(SIDEBAR_ACCENT);
                    g2.fillRoundRect(0, 3, 3, getHeight() - 6, 3, 3);
                } else if (hover) {
                    g2.setColor(new Color(255, 255, 255, 14));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
                }

                Color fg = active ? Color.WHITE : SIDEBAR_TEXT;
                g2.setColor(fg);

                g2.setFont(FONT_EMOJI.deriveFont(15f));
                FontMetrics fmIcon = g2.getFontMetrics();
                int iconY = (getHeight() + fmIcon.getAscent()) / 2 - 4;
                g2.drawString(icon, 14, iconY);

                g2.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, 42, (getHeight() + fm.getAscent()) / 2 - 3);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(200, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.putClientProperty("active", false);
        return btn;
    }

    public static void setSidebarActive(JButton btn, boolean active) {
        btn.putClientProperty("active", active);
        btn.repaint();
    }

    // ================== KPI CARD (DASHBOARD) ==================
    public static JPanel createKpiCard(String icon, String value, String label, Color accent) {
        JPanel card = roundedCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 18, 14, 18));

        JLabel iconLbl = new JLabel(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLbl.setFont(FONT_EMOJI);
        iconLbl.setForeground(accent);
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        iconLbl.setMaximumSize(new Dimension(38, 38));
        iconLbl.setPreferredSize(new Dimension(38, 38));
        iconLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valLbl.setForeground(TEXT_MAIN);
        valLbl.setBorder(new EmptyBorder(12, 0, 2, 0));
        valLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLbl = new JLabel(label);
        lblLbl.setFont(FONT_NORMAL);
        lblLbl.setForeground(TEXT_MUTED);
        lblLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(iconLbl);
        card.add(valLbl);
        card.add(lblLbl);
        return card;
    }

    public static JPanel roundedCardPanel() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 2f, getHeight() - 2f, 14, 14));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    // ================== BADGE TRẠNG THÁI ==================
    public static JLabel createBadge(String text, int type) {
        final Color bg, fg;
        switch (type) {
            case BADGE_OK:      bg = BADGE_OK_BG;      fg = BADGE_OK_FG;      break;
            case BADGE_WARN:    bg = BADGE_WARN_BG;    fg = BADGE_WARN_FG;    break;
            case BADGE_BAD:     bg = BADGE_BAD_BG;     fg = BADGE_BAD_FG;     break;
            default:            bg = BADGE_PENDING_BG; fg = BADGE_PENDING_FG; break;
        }
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // Đổi thành bo góc 20px
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setOpaque(false);
        lbl.setForeground(fg);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setBorder(new EmptyBorder(4, 12, 4, 12));
        return lbl;
    }

    // ================== PROGRESS BAR ==================
    public static JPanel createProgressRow(String label, int percent) {
        int pct = Math.max(0, Math.min(100, percent));
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_NORMAL);
        lbl.setForeground(TEXT_MAIN);
        lbl.setPreferredSize(new Dimension(90, 20));

        JPanel track = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(BG_APP);
                g2.fillRoundRect(0, 0, w, h, 5, 5); // Bo góc 5px
                int fillW = (int) (w * (pct / 100.0));
                if (fillW > 0) {
                    g2.setPaint(new GradientPaint(0, 0, MIT_ORANGE, fillW, 0, MIT_YELLOW));
                    g2.fillRoundRect(0, 0, fillW, h, 5, 5); // Bo góc 5px
                }
                g2.dispose();
            }
        };
        track.setOpaque(false);
        track.setPreferredSize(new Dimension(100, 8));

        JLabel pctLbl = new JLabel(pct + "%");
        pctLbl.setFont(FONT_BOLD);
        pctLbl.setForeground(TEXT_MAIN);
        pctLbl.setPreferredSize(new Dimension(40, 20));
        pctLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(lbl, BorderLayout.WEST);
        row.add(track, BorderLayout.CENTER);
        row.add(pctLbl, BorderLayout.EAST);
        return row;
    }
}
