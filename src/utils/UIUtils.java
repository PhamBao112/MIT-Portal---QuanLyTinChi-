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
import java.awt.geom.Path2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import config.DBConnect;

/**
 * ============================================================
 *  UIUtils — PHIÊN BẢN NÂNG CẤP GIAO DIỆN (V2)
 * ------------------------------------------------------------
 *  - Giữ NGUYÊN toàn bộ tên hằng số màu/font, tên & chữ ký các
 *    hàm public đang được các Panel khác gọi tới
 *    (createPrimaryBtn, createInput, createFormRow, styleTable,
 *    loadHocKyOptionsForStudent...) để KHÔNG phải sửa bất kỳ
 *    file Panel nào khác.
 *  - Hàm loadHocKyOptionsForStudent GIỮ NGUYÊN 100% logic/SQL,
 *    không đụng vào luồng dữ liệu.
 *  - Chỉ nâng cấp phần "vẽ" (Graphics2D) để giao diện đẹp,
 *    hiện đại, bo góc, đổ bóng, hover/focus mượt hơn.
 * ============================================================
 */
public class UIUtils {
    // BỘ MÀU CHUẨN CỦA ĐẠI HỌC CÔNG NGHỆ MIỀN ĐÔNG (MIT)
    public static final Color MIT_RED       = new Color(139, 0, 0);      // Đỏ đô
    public static final Color MIT_ORANGE    = new Color(230, 81, 0);     // Cam đậm
    public static final Color MIT_YELLOW    = new Color(255, 193, 7);    // Vàng
    public static final Color MIT_RED_LIGHT = new Color(253, 240, 240);  // Nền Tab

    public static final Color BG_APP      = new Color(248, 250, 252);
    public static final Color BORDER      = new Color(226, 232, 240);
    public static final Color TEXT_MAIN   = new Color(30, 41, 59);
    public static final Color TEXT_MUTED  = new Color(100, 116, 139);
    public static final Color GREEN_500   = new Color(34, 197, 94);
    public static final Color RED_500     = new Color(239, 68, 68);
    public static final Color WHITE       = Color.WHITE;

    public static final Font FONT_NORMAL  = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 18);

    // --- FONT ĐẶC BIỆT ĐỂ HIỂN THỊ EMOJI TRÊN WINDOWS ---
    public static final Font FONT_EMOJI   = new Font("Segoe UI Emoji", Font.BOLD, 14);
    public static final Font FONT_LOGO    = new Font("Segoe UI Emoji", Font.BOLD, 22);

    // Màu phụ trợ mới (chỉ dùng nội bộ cho hiệu ứng vẽ, không phá vỡ gì)
    private static final Color SHADOW       = new Color(15, 23, 42, 40);
    private static final Color ROW_STRIPE   = new Color(250, 251, 253);
    private static final Color ROW_HOVER    = new Color(255, 246, 246);

    // ================== V3 — SIDEBAR / KPI CARD / BADGE / PROGRESS ==================
    // Bổ sung cho bản nâng cấp giao diện mới (sidebar nav + dashboard kiểu card).
    // Không đụng tới bất kỳ hằng số/hàm nào ở trên — các Panel cũ vẫn chạy y nguyên.

    public static final Color SIDEBAR_BG      = new Color(42, 20, 20);   // nâu đỏ đậm (trên)
    public static final Color SIDEBAR_BG_DARK = new Color(30, 12, 12);   // nâu đỏ đậm hơn (dưới)
    public static final Color SIDEBAR_ACTIVE  = new Color(255, 255, 255, 30);
    public static final Color SIDEBAR_ACCENT  = MIT_YELLOW;
    public static final Color SIDEBAR_TEXT       = new Color(232, 217, 210);
    public static final Color SIDEBAR_TEXT_MUTED = new Color(169, 137, 127);

    public static final Color BADGE_OK_BG      = new Color(232, 245, 233);
    public static final Color BADGE_OK_FG      = new Color(30, 142, 90);
    public static final Color BADGE_WARN_BG    = new Color(255, 248, 225);
    public static final Color BADGE_WARN_FG    = new Color(165, 107, 0);
    public static final Color BADGE_BAD_BG     = new Color(253, 234, 234);
    public static final Color BADGE_BAD_FG     = new Color(198, 40, 40);
    public static final Color BADGE_PENDING_BG = new Color(236, 239, 241);
    public static final Color BADGE_PENDING_FG = new Color(84, 110, 122);

    public static final int BADGE_OK = 0, BADGE_WARN = 1, BADGE_BAD = 2, BADGE_PENDING = 3;

    // ================== CARD SHELL DUNG CHUNG (thay cho LineBorder + tableHeader/formHeader
    // tu che rieng trong tung Panel) - bo goc THAT (14px), tieu de + gach chan mau accent. ==================
    public static JPanel createCardShell(String title, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                paintElevatedCard((Graphics2D) g.create(), getWidth(), getHeight(), 14);
            }
        };
        card.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 2, 0, accent), new EmptyBorder(13, 20, 11, 20)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(TEXT_MAIN);
        header.add(lbl, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);
        return card;
    }

    /** Nut xoa/huy (nguy hiem) - dung chung thay vi moi Panel tu che mau do rieng. */
    public static JButton createDangerBtn(String text) {
        return buildFancyButton(null, text, RED_500, RED_500.darker(), WHITE, true);
    }
    public static JButton createDangerBtn(String icon, String text) {
        return buildFancyButton(icon, text, RED_500, RED_500.darker(), WHITE, true);
    }

    // ================== NÚT BẤM CHÍNH (PRIMARY) ==================

    public static JButton createPrimaryBtn(String text) {
        return buildFancyButton(null, text, MIT_RED, MIT_ORANGE, WHITE, true);
    }
    public static JButton createPrimaryBtn(String icon, String text) {
        return buildFancyButton(icon, text, MIT_RED, MIT_ORANGE, WHITE, true);
    }

    // Bổ sung thêm (KHÔNG thay thế API cũ) — nút phụ dạng viền,
    // các Panel cũ vẫn hoạt động bình thường vì không gọi tới hàm này.
    public static JButton createSecondaryBtn(String text) {
        return buildFancyButton(null, text, WHITE, MIT_RED_LIGHT, MIT_RED, false);
    }
    public static JButton createSecondaryBtn(String icon, String text) {
        return buildFancyButton(icon, text, WHITE, MIT_RED_LIGHT, MIT_RED, false);
    }

    private static JButton buildFancyButton(String icon, String text, Color base, Color hover, Color fg, boolean filled) {
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
                int w = getWidth(), h = getHeight(), arc = 12;
                boolean pressed = getModel().isPressed();

                // Đổ bóng mềm phía dưới nút
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

                // V8: icon vector (tuy chon) dung truoc chu, gom chung 1 khoi va can giua ca cum.
                g2.setFont(FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int iconSize = 15, gap = icon != null ? 8 : 0;
                int textW = fm.stringWidth(text);
                int totalW = (icon != null ? iconSize + gap : 0) + textW;
                int startX = (w - totalW) / 2;
                int ty = (h - yOff + fm.getAscent()) / 2 - 4 + yOff;

                if (icon != null) {
                    paintIcon(g2, icon, startX, (h - iconSize) / 2f + yOff, iconSize, fg);
                }
                g2.setColor(fg);
                g2.drawString(text, startX + (icon != null ? iconSize + gap : 0), ty);
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

    /** JTextField tự vẽ nền bo góc + viền đổi màu khi focus (không đổi hành vi nhập liệu). */
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
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(focused ? MIT_RED : BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1.2f));
            g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, getWidth() - 2.2f, getHeight() - 2.2f, 10, 10));
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

    // DUNG CHUNG cho DiemPanel + LichHocPanel (truoc day 2 file copy y het doan nay).
    // Chi liet ke nhung hoc ky ma SV nay THUC SU co dang ky (JOIN KET_QUA_DANG_KY).
    // DangKyPanel KHONG dung ham nay vi no can them ca hoc ky moi nhat chua dang ky.
    // Tra ve String[]{maHK, tenHK} da duoc resolve + da setSelectedItem tren combo truyen vao.
    // >>> KHÔNG THAY ĐỔI BẤT KỲ DÒNG LOGIC/SQL NÀO BÊN DƯỚI <<<
    public static String[] loadHocKyOptionsForStudent(JComboBox<String> combo, String maSV, String currentMaHK) {
        combo.removeAllItems();
        // FIX: SQL Server bat buoc moi cot trong ORDER BY phai co mat trong SELECT DISTINCT.
        // Truoc day order theo h.NamHoc nhung khong select no -> nem SQLServerException,
        // combo rong va man hinh bao "Chua co du lieu" du SV co du lieu that.
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
        combo.setSelectedItem(target); // set truoc khi goi noi con lai; nen goi ham nay TRUOC khi gan addActionListener
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

        // Header: bo đậm chữ + viền nhấn đỏ phía dưới cho hiện đại
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

        // Hover theo hàng: lưu chỉ số hàng đang rê chuột trên client property
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

    // ================== HÀM PHỤ TRỢ NỘI BỘ ==================

    private static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            clamp((int) (a.getRed()   + (b.getRed()   - a.getRed())   * t)),
            clamp((int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t)),
            clamp((int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)));
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    // V4 - Do bong mem dung chung cho moi loai "card" (createCardShell, roundedCardPanel,
    // createKpiCard...) de tao chieu sau (elevation) thay vi chi co vien mong phang nhu truoc,
    // giup giao dien nhin "day dan" / chuyen nghiep hon ma khong phai sua tung Panel rieng le.
    private static void paintElevatedCard(Graphics2D g2, int w, int h, int arc) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 5; i >= 1; i--) {
            g2.setColor(new Color(15, 23, 42, 4 + i * 2));
            g2.fill(new RoundRectangle2D.Float(i * 0.6f, i * 1.3f, w - 2 - i * 0.6f, h - 2 - i * 1.3f, arc, arc));
        }
        g2.setColor(WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 3, h - 4, arc, arc));
        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 4f, h - 5f, arc, arc));
    }

    // ================== SIDEBAR NAV ==================

    /** Panel nền sidebar, vẽ gradient đỏ đô đậm (trên → dưới). Dùng BoxLayout dọc, tự add các nav button vào. */
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

    /** Nhãn tiêu đề nhỏ trong sidebar (ví dụ: "QUẢN LÝ"). */
    public static JLabel createSidebarGroupLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(SIDEBAR_TEXT_MUTED);
        l.setBorder(new EmptyBorder(14, 10, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /**
     * Nút điều hướng trong sidebar. Gọi setSidebarActive(btn, true/false) để chuyển trạng thái
     * khi người dùng chuyển màn hình (tự vẽ thanh vàng bên trái + nền sáng khi active).
     */
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

                // V7: doi icon emoji -> icon vector (paintIcon) - dong nhat 1 kieu net cho
                // toan bo app, khong con phu thuoc font Segoe UI Emoji cua Windows.
                Color fg = active ? Color.WHITE : SIDEBAR_TEXT;
                paintIcon(g2, icon, 12, (getHeight() - 16) / 2f, 16, fg);

                g2.setColor(fg);
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

    /** Bật/tắt trạng thái active của 1 nút sidebar (do createSidebarButton tạo ra). */
    public static void setSidebarActive(JButton btn, boolean active) {
        btn.putClientProperty("active", active);
        btn.repaint();
    }

    // ================== KPI CARD (DASHBOARD) ==================

    /** Thẻ KPI kiểu dashboard: icon màu + số liệu lớn + nhãn mô tả. */
    public static JPanel createKpiCard(String icon, String value, String label, Color accent) {
        JPanel card = roundedCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 18, 14, 18));

        JLabel iconLbl = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 32));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // V7: icon vector thay emoji, canh giua trong khung 44x44.
                int iconSize = 22;
                paintIcon(g2, icon, (getWidth() - iconSize) / 2f, (getHeight() - iconSize) / 2f, iconSize, accent);
                g2.dispose();
            }
        };
        iconLbl.setMaximumSize(new Dimension(44, 44));
        iconLbl.setPreferredSize(new Dimension(44, 44));
        iconLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 25));
        valLbl.setForeground(TEXT_MAIN);
        valLbl.setBorder(new EmptyBorder(13, 0, 3, 0));
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

    /** Panel trắng bo góc dùng chung cho card/dashboard (nền trắng + viền mỏng, không đổ bóng đậm). */
    public static JPanel roundedCardPanel() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                paintElevatedCard((Graphics2D) g.create(), getWidth(), getHeight(), 14);
            }
        };
        p.setOpaque(false);
        return p;
    }

    // ================== BADGE TRẠNG THÁI ==================

    /** Nhãn badge tròn viền màu (BADGE_OK / BADGE_WARN / BADGE_BAD / BADGE_PENDING). */
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
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

    // ================== ICON VECTOR (V7 - thay emoji theo khuyen nghi skill UI/UX Pro Max:
    // "No emojis used as icons", "Consistent Icon Sizing", "Stroke Consistency") ==================
    // Ve trong khung logic 24x24, dung 1 do day net duy nhat, scale theo size truyen vao ->
    // moi icon trong app CUNG mot phong cach net ve, khong con lech kieu/mau nhu emoji Unicode.
    public static void paintIcon(Graphics2D g2in, String key, float x, float y, float size, Color color) {
        Graphics2D g2 = (Graphics2D) g2in.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        float s = size / 24f;
        g2.scale(s, s);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (key) {
            case "dashboard": {
                g2.draw(new RoundRectangle2D.Float(2, 2, 9, 9, 2, 2));
                g2.draw(new RoundRectangle2D.Float(13, 2, 9, 9, 2, 2));
                g2.draw(new RoundRectangle2D.Float(2, 13, 9, 9, 2, 2));
                g2.draw(new RoundRectangle2D.Float(13, 13, 9, 9, 2, 2));
                break;
            }
            case "users": {
                g2.draw(new Ellipse2D.Float(8, 3, 8, 8));
                Path2D.Float body = new Path2D.Float();
                body.moveTo(3, 21);
                body.curveTo(3, 14, 21, 14, 21, 21);
                g2.draw(body);
                break;
            }
            case "book": {
                g2.draw(new RoundRectangle2D.Float(3, 4, 18, 16, 2, 2));
                g2.draw(new Line2D.Float(12, 4, 12, 20));
                g2.draw(new Line2D.Float(6, 9, 10, 9));
                g2.draw(new Line2D.Float(6, 13, 10, 13));
                g2.draw(new Line2D.Float(14, 9, 18, 9));
                g2.draw(new Line2D.Float(14, 13, 18, 13));
                break;
            }
            case "card": {
                g2.draw(new RoundRectangle2D.Float(2, 5, 20, 14, 3, 3));
                g2.draw(new Line2D.Float(2, 10, 22, 10));
                g2.draw(new Line2D.Float(5, 15, 9, 15));
                break;
            }
            case "chart": {
                g2.draw(new Line2D.Float(3, 21, 21, 21));
                g2.draw(new Line2D.Float(7, 15, 7, 21));
                g2.draw(new Line2D.Float(12, 10, 12, 21));
                g2.draw(new Line2D.Float(17, 5, 17, 21));
                break;
            }
            case "upload": {
                g2.draw(new Line2D.Float(12, 4, 12, 16));
                g2.draw(new Line2D.Float(12, 4, 7, 9));
                g2.draw(new Line2D.Float(12, 4, 17, 9));
                g2.draw(new Line2D.Float(5, 20, 19, 20));
                break;
            }
            case "logout": {
                g2.draw(new RoundRectangle2D.Float(4, 4, 10, 16, 2, 2));
                g2.draw(new Line2D.Float(10, 12, 20, 12));
                g2.draw(new Line2D.Float(20, 12, 16, 8));
                g2.draw(new Line2D.Float(20, 12, 16, 16));
                break;
            }
            case "calendar": {
                g2.draw(new RoundRectangle2D.Float(3, 5, 18, 16, 2, 2));
                g2.draw(new Line2D.Float(3, 10, 21, 10));
                g2.draw(new Line2D.Float(8, 2, 8, 6));
                g2.draw(new Line2D.Float(16, 2, 16, 6));
                break;
            }
            case "edit": {
                g2.draw(new Line2D.Float(5, 19, 16, 8));
                Path2D.Float tip = new Path2D.Float();
                tip.moveTo(16, 8);
                tip.lineTo(19, 5);
                tip.lineTo(19, 8);
                tip.closePath();
                g2.fill(tip);
                g2.draw(new Line2D.Float(4, 20, 6, 18));
                break;
            }
            case "graduation": {
                Path2D.Float cap = new Path2D.Float();
                cap.moveTo(12, 5);
                cap.lineTo(21, 10);
                cap.lineTo(12, 15);
                cap.lineTo(3, 10);
                cap.closePath();
                g2.draw(cap);
                g2.draw(new Line2D.Float(18, 11, 18, 17));
                g2.draw(new Ellipse2D.Float(16.7f, 17, 2.6f, 2.6f));
                break;
            }
            case "star": {
                g2.draw(starPath());
                break;
            }
            case "trend": {
                Path2D.Float zig = new Path2D.Float();
                zig.moveTo(3, 18);
                zig.lineTo(9, 12);
                zig.lineTo(13, 16);
                zig.lineTo(21, 6);
                g2.draw(zig);
                g2.draw(new Line2D.Float(15, 6, 21, 6));
                g2.draw(new Line2D.Float(21, 6, 21, 12));
                break;
            }
            case "dollar": {
                g2.draw(new Ellipse2D.Float(3, 3, 18, 18));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String t = "$";
                g2.drawString(t, 12 - fm.stringWidth(t) / 2f, 12 + fm.getAscent() / 2f - 1);
                break;
            }
            case "warning": {
                Path2D.Float tri = new Path2D.Float();
                tri.moveTo(12, 3);
                tri.lineTo(21, 20);
                tri.lineTo(3, 20);
                tri.closePath();
                g2.draw(tri);
                g2.draw(new Line2D.Float(12, 10, 12, 15));
                g2.fill(new Ellipse2D.Float(11, 16.5f, 2, 2));
                break;
            }
            case "target": {
                g2.draw(new Ellipse2D.Float(3, 3, 18, 18));
                g2.draw(new Ellipse2D.Float(8, 8, 8, 8));
                g2.fill(new Ellipse2D.Float(11, 11, 2, 2));
                break;
            }
            case "medal": {
                g2.draw(new Ellipse2D.Float(6, 3, 12, 12));
                g2.draw(new Line2D.Float(9, 13, 6, 21));
                g2.draw(new Line2D.Float(15, 13, 18, 21));
                break;
            }
            case "institution": {
                Path2D.Float roof = new Path2D.Float();
                roof.moveTo(2, 9);
                roof.lineTo(12, 3);
                roof.lineTo(22, 9);
                g2.draw(roof);
                g2.draw(new Line2D.Float(3, 20, 21, 20));
                g2.draw(new Line2D.Float(6, 11, 6, 18));
                g2.draw(new Line2D.Float(12, 11, 12, 18));
                g2.draw(new Line2D.Float(18, 11, 18, 18));
                break;
            }
            case "laptop": {
                g2.draw(new RoundRectangle2D.Float(4, 4, 16, 11, 2, 2));
                g2.draw(new Line2D.Float(2, 19, 22, 19));
                break;
            }
            case "plus": {
                g2.draw(new Line2D.Float(12, 4, 12, 20));
                g2.draw(new Line2D.Float(4, 12, 20, 12));
                break;
            }
            case "trash": {
                g2.draw(new Line2D.Float(4, 7, 20, 7));
                g2.draw(new RoundRectangle2D.Float(6, 7, 12, 14, 2, 2));
                g2.draw(new Line2D.Float(9, 4, 15, 4));
                g2.draw(new Line2D.Float(10, 11, 10, 17));
                g2.draw(new Line2D.Float(14, 11, 14, 17));
                break;
            }
            case "refresh": {
                Path2D.Float arc1 = new Path2D.Float();
                arc1.append(new java.awt.geom.Arc2D.Float(4, 4, 16, 16, 40, 260, java.awt.geom.Arc2D.OPEN), false);
                g2.draw(arc1);
                g2.draw(new Line2D.Float(19, 5, 19, 10));
                g2.draw(new Line2D.Float(19, 5, 14, 6));
                break;
            }
            case "check": {
                Path2D.Float ck = new Path2D.Float();
                ck.moveTo(4, 12);
                ck.lineTo(10, 18);
                ck.lineTo(20, 6);
                g2.draw(ck);
                break;
            }
            case "download": {
                g2.draw(new Line2D.Float(12, 3, 12, 15));
                g2.draw(new Line2D.Float(12, 15, 7, 10));
                g2.draw(new Line2D.Float(12, 15, 17, 10));
                g2.draw(new Line2D.Float(5, 20, 19, 20));
                break;
            }
            default: {
                g2.draw(new Ellipse2D.Float(4, 4, 16, 16));
            }
        }
        g2.dispose();
    }

    private static Path2D.Float starPath() {
        Path2D.Float p = new Path2D.Float();
        double cx = 12, cy = 12, rOuter = 10, rInner = 4.2;
        for (int i = 0; i < 10; i++) {
            double ang = Math.PI / 2 + i * Math.PI / 5;
            double r = (i % 2 == 0) ? rOuter : rInner;
            double px = cx + r * Math.cos(ang);
            double py = cy - r * Math.sin(ang);
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }

    /** Component icon tai su dung (thay JLabel emoji rai rac o cac Panel khac). */
    public static JComponent createIconLabel(String key, int size, Color color) {
        JLabel lbl = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                paintIcon((Graphics2D) g.create(), key, 0, 0, size, color);
            }
        };
        lbl.setPreferredSize(new Dimension(size, size));
        lbl.setOpaque(false);
        return lbl;
    }

    /**
     * V9 - "DOT PHA": Vong tron tien do (radial progress ring) - thay the KPI card phang
     * thuong thay o moi ung dung Swing. Ve tay bang Graphics2D, khong dung anh/asset ngoai.
     * @param percent 0-100
     * @param bigText chu lon giua vong (VD "34%")
     * @param smallLabel nhan nho duoi chu lon (VD "Tín chỉ")
     * @param ringColor mau vong tron da hoan thanh
     * @param trackColor mau ray track (vong nen mo)
     * @param textColor mau chu giua
     */
    public static JComponent createRadialProgress(int percent, String bigText, String smallLabel,
                                                    Color ringColor, Color trackColor, Color textColor) {
        int pct = Math.max(0, Math.min(100, percent));
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int d = Math.min(w, h) - 6;
                float sw = d * 0.115f;
                float x = (w - d) / 2f + sw / 2f, y = (h - d) / 2f + sw / 2f;
                float dd = d - sw;

                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(trackColor);
                g2.draw(new java.awt.geom.Ellipse2D.Float(x, y, dd, dd));

                if (pct > 0) {
                    g2.setColor(ringColor);
                    double angle = 360.0 * pct / 100.0;
                    g2.draw(new java.awt.geom.Arc2D.Double(x, y, dd, dd, 90, -angle, java.awt.geom.Arc2D.OPEN));
                }

                g2.setColor(textColor);
                g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(14, d / 4)));
                FontMetrics fmB = g2.getFontMetrics();
                int tx = (w - fmB.stringWidth(bigText)) / 2;
                int ty = h / 2 - 2;
                g2.drawString(bigText, tx, ty);

                if (smallLabel != null && !smallLabel.isEmpty()) {
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, Math.max(10, d / 10)));
                    FontMetrics fmS = g2.getFontMetrics();
                    int sx = (w - fmS.stringWidth(smallLabel)) / 2;
                    g2.drawString(smallLabel, sx, ty + fmS.getHeight() - 2);
                }
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    /** V9: banner gradient bo goc dung chung cho khu vuc "chao mung" dau trang Dashboard. */
    public static JPanel createHeroBanner() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, MIT_RED, w, h, MIT_ORANGE));
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 18, 18));
                // Vong tron trang tri mo o goc phai cho co chieu sau, khong phang
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(w - 160, -60, 220, 220);
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillOval(w - 90, h - 80, 140, 140);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    /** Loi chao theo gio trong ngay (khong dung emoji, dung icon vector "dashboard"-style rieng). */
    public static String greetingByHour() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 11) return "Chào buổi sáng";
        if (hour < 13) return "Chào buổi trưa";
        if (hour < 18) return "Chào buổi chiều";
        return "Chào buổi tối";
    }

    // ================== PROGRESS BAR (CÔNG NỢ, TỲ LỆ...) ==================

    /** Thanh tiến độ bo góc, gradient cam→vàng, kèm nhãn + % bên phải. */
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
                g2.fillRoundRect(0, 0, w, h, h, h);
                int fillW = (int) (w * (pct / 100.0));
                if (fillW > 0) {
                    g2.setPaint(new GradientPaint(0, 0, MIT_ORANGE, fillW, 0, MIT_YELLOW));
                    g2.fillRoundRect(0, 0, fillW, h, h, h);
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