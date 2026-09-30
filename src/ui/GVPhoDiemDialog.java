package ui;

import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pho diem lop hoc phan: ve bieu do cot ty le xep loai A/B/C/D/F bang Graphics2D
 * thuan (khong dung thu vien chart ngoai). Thang quy doi diem 10 -> chu (co the
 * dieu chinh tai THANG_DIEM neu truong dung barem khac):
 *   A: 8.5-10 | B: 7-8.4 | C: 5.5-6.9 | D: 4-5.4 | F: duoi 4
 * SV co TrangThai = "Chua co diem" duoc tinh rieng, khong gop vao pho diem vi
 * chua co ket qua that su.
 */
public class GVPhoDiemDialog extends JDialog {

    private static final String[] NHAN_XEP_LOAI = {"A", "B", "C", "D", "F"};
    private static final Color[] MAU_XEP_LOAI = {
        new Color(34, 197, 94),   // A - xanh la
        new Color(59, 130, 246),  // B - xanh duong
        new Color(234, 179, 8),   // C - vang
        new Color(249, 115, 22),  // D - cam
        new Color(239, 68, 68)    // F - do
    };

    public GVPhoDiemDialog(Window owner, StudentManagerService service, String maLHP, String tenMonHienThi) {
        super(owner, "Phổ điểm - " + tenMonHienThi, ModalityType.APPLICATION_MODAL);
        setSize(640, 560);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIUtils.BG_APP);

        buildUI(service, maLHP, tenMonHienThi);
    }

    private void buildUI(StudentManagerService service, String maLHP, String tenMonHienThi) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtils.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIUtils.BORDER), new EmptyBorder(16, 22, 16, 22)));
        JLabel lblTitle = new JLabel("Phổ điểm lớp " + maLHP + " - " + tenMonHienThi);
        lblTitle.setFont(UIUtils.FONT_TITLE);
        header.add(lblTitle, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        List<Object[]> ds = service.getSinhVienTrongLopHP(maLHP);

        Map<String, Integer> soLuong = new LinkedHashMap<>();
        for (String h : NHAN_XEP_LOAI) soLuong.put(h, 0);
        int soChuaCoDiem = 0;
        int soDat = 0;
        double tongDiem = 0;
        int soDaChamDiem = 0;

        for (Object[] sv : ds) {
            String trangThai = (String) sv[6];
            if ("Chưa có điểm".equals(trangThai)) { soChuaCoDiem++; continue; }
            double diem = (double) sv[5];
            tongDiem += diem;
            soDaChamDiem++;
            soLuong.merge(xepLoai(diem), 1, Integer::sum);
            if ("Đạt".equals(trangThai)) soDat++;
        }

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UIUtils.WHITE);
        content.setBorder(new EmptyBorder(20, 22, 10, 22));

        if (soDaChamDiem == 0) {
            JLabel lblEmpty = new JLabel("Lớp này chưa có sinh viên nào được chấm điểm.");
            lblEmpty.setForeground(UIUtils.TEXT_MUTED);
            lblEmpty.setBorder(new EmptyBorder(40, 0, 40, 0));
            lblEmpty.setHorizontalAlignment(SwingConstants.CENTER);
            content.add(lblEmpty, BorderLayout.CENTER);
        } else {
            BieuDoPanel bieuDo = new BieuDoPanel(soLuong, soDaChamDiem);
            bieuDo.setPreferredSize(new Dimension(560, 320));
            content.add(bieuDo, BorderLayout.CENTER);

            JPanel summary = new JPanel(new GridLayout(1, 3, 15, 0));
            summary.setBackground(UIUtils.WHITE);
            summary.setBorder(new EmptyBorder(16, 0, 0, 0));
            summary.add(summaryBox("Điểm trung bình lớp", String.format("%.2f", tongDiem / soDaChamDiem)));
            double tyLeDat = soDaChamDiem > 0 ? soDat * 100.0 / soDaChamDiem : 0;
            summary.add(summaryBox("Tỷ lệ đạt", String.format("%.1f%%", tyLeDat)));
            summary.add(summaryBox("Chưa có điểm", soChuaCoDiem + " SV"));
            content.add(summary, BorderLayout.SOUTH);
        }

        add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIUtils.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIUtils.BORDER), new EmptyBorder(12, 22, 12, 22)));
        JLabel lblGhiChu = new JLabel("Thang xếp loại: A 8.5-10 · B 7-8.4 · C 5.5-6.9 · D 4-5.4 · F dưới 4");
        lblGhiChu.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblGhiChu.setForeground(UIUtils.TEXT_MUTED);
        footer.add(lblGhiChu, BorderLayout.WEST);
        JButton btnDong = UIUtils.createSecondaryBtn("Đóng");
        btnDong.addActionListener(e -> dispose());
        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnBox.setBackground(UIUtils.WHITE);
        btnBox.add(btnDong);
        footer.add(btnBox, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private static String xepLoai(double diem) {
        if (diem >= 8.5) return "A";
        if (diem >= 7.0) return "B";
        if (diem >= 5.5) return "C";
        if (diem >= 4.0) return "D";
        return "F";
    }

    private JPanel summaryBox(String label, String value) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(UIUtils.BG_APP);
        box.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel lValue = new JLabel(value);
        lValue.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lValue.setForeground(UIUtils.MIT_RED);
        JLabel lLabel = new JLabel(label);
        lLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lLabel.setForeground(UIUtils.TEXT_MUTED);
        box.add(lValue);
        box.add(lLabel);
        return box;
    }

    /** Bieu do cot ve thuan bang Graphics2D - khong dung thu vien chart ngoai. */
    private static class BieuDoPanel extends JPanel {
        private final Map<String, Integer> soLuong;
        private final int tongSo;

        BieuDoPanel(Map<String, Integer> soLuong, int tongSo) {
            this.soLuong = soLuong;
            this.tongSo = tongSo;
            setBackground(UIUtils.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int soCot = NHAN_XEP_LOAI.length;
            int maxSoLuong = 1;
            for (int sl : soLuong.values()) maxSoLuong = Math.max(maxSoLuong, sl);

            int padTop = 30, padBottom = 50, padSide = 40;
            int chartH = h - padTop - padBottom;
            int chartW = w - 2 * padSide;
            int gap = 24;
            int colW = (chartW - gap * (soCot - 1)) / soCot;

            // Duong ke ngang lam moc + gia tri truc y
            g2.setColor(new Color(230, 230, 230));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            int soMoc = 4;
            for (int i = 0; i <= soMoc; i++) {
                int y = padTop + chartH - (chartH * i / soMoc);
                g2.drawLine(padSide, y, w - padSide, y);
                int giaTri = maxSoLuong * i / soMoc;
                g2.setColor(UIUtils.TEXT_MUTED);
                g2.drawString(String.valueOf(giaTri), 8, y + 4);
                g2.setColor(new Color(230, 230, 230));
            }

            int x = padSide;
            for (int i = 0; i < soCot; i++) {
                String nhan = NHAN_XEP_LOAI[i];
                int sl = soLuong.get(nhan);
                int barH = (int) ((double) sl / maxSoLuong * chartH);
                int barY = padTop + chartH - barH;

                Color mau = MAU_XEP_LOAI[i];
                g2.setColor(mau);
                g2.fill(new RoundRectangle2D.Double(x, barY, colW, barH, 8, 8));

                // So luong tren dinh cot
                g2.setColor(UIUtils.TEXT_MAIN);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                String slText = String.valueOf(sl);
                int slW = g2.getFontMetrics().stringWidth(slText);
                g2.drawString(slText, x + colW / 2 - slW / 2, barY - 8);

                // Ty le % duoi so luong
                double tyLe = tongSo > 0 ? sl * 100.0 / tongSo : 0;
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(UIUtils.TEXT_MUTED);
                String tyLeText = String.format("%.0f%%", tyLe);
                int tyLeW = g2.getFontMetrics().stringWidth(tyLeText);
                g2.drawString(tyLeText, x + colW / 2 - tyLeW / 2, padTop + chartH + 16);

                // Nhan xep loai duoi truc x
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                g2.setColor(mau);
                int nhanW = g2.getFontMetrics().stringWidth(nhan);
                g2.drawString(nhan, x + colW / 2 - nhanW / 2, padTop + chartH + 34);

                x += colW + gap;
            }

            g2.dispose();
        }
    }
}
