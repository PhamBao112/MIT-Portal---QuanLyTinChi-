package ui;

import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Lich giang day dang luoi tuan (2 buoi Sang/Chieu x 6 Thu), dong bo phong cach voi
 * LichHocPanel cua Sinh vien. Khac SV o cho: khong dieu huong theo tuan/ngay thuc te vi
 * GV day theo khung tiet co dinh moi hoc ky, khong can theo doi tung tuan; thay vao do co
 * combo loc theo HOC KY vi 1 GV thuong day nhieu hoc ky cung mot luc (list phang truoc day
 * de bi lan lon giua cac hoc ky khac nhau).
 */
public class GVLichDayPanel extends JPanel {

    private static final int SO_HANG = 2;
    private static final int SO_COT = 6;
    private static final String[] TEN_BUOI = {"Sáng", "Chiều"};
    private static final String[] TEN_THU = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};
    private static final String[] MAU_NEN = {"blue", "green", "amber", "purple", "rose"};

    private final StudentManagerService service;
    private final String maGV;
    private List<Object[]> allLop = new ArrayList<>();
    private java.util.Map<String, String> tenHKToMaHK = new java.util.LinkedHashMap<>();
    private JComboBox<String> cbHocKy;
    private JPanel gridContainer;

    public GVLichDayPanel(StudentManagerService service, String maGV) {
        this.service = service;
        this.maGV = maGV;

        setLayout(new BorderLayout());
        setBackground(UIUtils.WHITE);
        setBorder(new LineBorder(UIUtils.BORDER, 1, true));

        allLop = service.getLopHocPhanByGV(maGV);
        buildUI();
    }

    private void buildUI() {
        removeAll();

        // row: MaLHP, TenMon, SoTinChi, MaHK, TenHK, SucChua, SiSo, Thu, TietHoc, PhongHoc, NgayBatDauHoc, NgayKetThucHoc
        tenHKToMaHK.clear();
        LinkedHashSet<String> dsTenHK = new LinkedHashSet<>();
        for (Object[] row : allLop) {
            String maHK = (String) row[3];
            String tenHK = row[4] != null ? (String) row[4] : maHK;
            if (dsTenHK.add(tenHK)) tenHKToMaHK.put(tenHK, maHK);
        }

        JPanel comboRow = new JPanel(new BorderLayout());
        comboRow.setBackground(UIUtils.WHITE);
        comboRow.setBorder(new EmptyBorder(15, 20, 0, 20));
        JPanel hkBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hkBox.setBackground(UIUtils.WHITE);
        hkBox.add(new JLabel("Học kỳ:"));
        cbHocKy = new JComboBox<>(dsTenHK.toArray(new String[0]));
        cbHocKy.setFont(UIUtils.FONT_BOLD);
        cbHocKy.setBackground(UIUtils.WHITE);
        cbHocKy.addActionListener(e -> { gridContainer.removeAll(); populateGrid(); gridContainer.revalidate(); gridContainer.repaint(); });
        hkBox.add(cbHocKy);
        comboRow.add(hkBox, BorderLayout.EAST);

        JPanel northWrapper = new JPanel();
        northWrapper.setLayout(new BoxLayout(northWrapper, BoxLayout.Y_AXIS));
        northWrapper.setBackground(UIUtils.WHITE);
        northWrapper.add(comboRow);

        if (dsTenHK.isEmpty()) {
            JLabel lblEmpty = new JLabel("Bạn chưa được phân công lớp học phần nào.");
            lblEmpty.setForeground(UIUtils.TEXT_MUTED);
            lblEmpty.setBorder(new EmptyBorder(30, 20, 30, 20));
            add(northWrapper, BorderLayout.NORTH);
            add(lblEmpty, BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtils.MIT_RED_LIGHT);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIUtils.MIT_RED), new EmptyBorder(15, 20, 15, 20)));
        JLabel lblTitle = new JLabel("Thời khóa biểu giảng dạy");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.MIT_RED);
        header.add(lblTitle, BorderLayout.WEST);
        northWrapper.add(header);

        gridContainer = new JPanel(new GridLayout(SO_HANG + 1, SO_COT + 1, 1, 1));
        gridContainer.setBackground(UIUtils.BORDER);
        gridContainer.setBorder(new MatteBorder(1, 1, 1, 1, UIUtils.BORDER));
        populateGrid();

        JScrollPane scrollGrid = new JScrollPane(gridContainer);
        scrollGrid.setBorder(null);
        scrollGrid.setBackground(UIUtils.WHITE);
        scrollGrid.getViewport().setBackground(UIUtils.WHITE);
        scrollGrid.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollGrid.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(northWrapper, BorderLayout.NORTH);
        add(scrollGrid, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void populateGrid() {
        gridContainer.add(createHeaderCell("Buổi"));
        for (int i = 0; i < SO_COT; i++) gridContainer.add(createHeaderCell(TEN_THU[i]));

        String tenHKChon = (String) cbHocKy.getSelectedItem();
        String maHKChon = tenHKToMaHK.get(tenHKChon);
        List<Object[]>[] cells = buildScheduleGrid(maHKChon);

        int colLabelWidth = 90, colThuWidth = 170, headerRowHeight = 36, bodyRowHeight = 150;
        gridContainer.setPreferredSize(new Dimension(colLabelWidth + SO_COT * colThuWidth, headerRowHeight + SO_HANG * bodyRowHeight));

        for (int row = 0; row < SO_HANG; row++) {
            gridContainer.add(createTimeCell(TEN_BUOI[row]));
            for (int col = 0; col < SO_COT; col++) {
                List<Object[]> danhSach = cells[row * SO_COT + col];
                gridContainer.add(danhSach.isEmpty() ? createEmptyCell() : createCellWithClasses(danhSach));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object[]>[] buildScheduleGrid(String maHK) {
        List<Object[]>[] cells = new List[SO_HANG * SO_COT];
        for (int i = 0; i < cells.length; i++) cells[i] = new ArrayList<>();

        for (Object[] row : allLop) {
            String rowMaHK = (String) row[3];
            if (!java.util.Objects.equals(rowMaHK, maHK)) continue;

            String thu = row[7] != null ? (String) row[7] : null;
            String tiet = row[8] != null ? (String) row[8] : null;
            int col = parseThu(thu);
            int[] tietBDKT = parseTietRange(tiet);
            if (col == -1 || tietBDKT == null) continue;

            int rowIdx = tietBDKT[0] <= 6 ? 0 : 1;
            cells[rowIdx * SO_COT + col].add(row);
        }
        for (List<Object[]> l : cells) {
            l.sort((a, b) -> {
                int[] t1 = parseTietRange((String) a[8]);
                int[] t2 = parseTietRange((String) b[8]);
                return Integer.compare(t1[0], t2[0]);
            });
        }
        return cells;
    }

    private int parseThu(String thu) {
        if (thu == null) return -1;
        if (thu.contains("2")) return 0;
        if (thu.contains("3")) return 1;
        if (thu.contains("4")) return 2;
        if (thu.contains("5")) return 3;
        if (thu.contains("6")) return 4;
        if (thu.contains("7")) return 5;
        return -1;
    }

    private int[] parseTietRange(String tietHoc) {
        if (tietHoc == null || tietHoc.isBlank() || !tietHoc.contains("-")) return null;
        try {
            String[] parts = tietHoc.trim().split("-");
            return new int[]{ Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) };
        } catch (Exception e) { return null; }
    }

    // Uoc luong gio hoc tu so tiet (giong quy uoc dang dung o LichHocPanel cua SV):
    // tiet 1 bat dau 07:00, moi tiet 45 phut, buoi chieu bat dau lai tu 13:00 o tiet 7.
    private String tinhGioHoc(int tietBD, int tietKT) {
        java.time.LocalTime moc = tietBD <= 6
            ? java.time.LocalTime.of(7, 0).plusMinutes((long) (tietBD - 1) * 45)
            : java.time.LocalTime.of(13, 0).plusMinutes((long) (tietBD - 7) * 45);
        long soTiet = tietKT - tietBD + 1;
        java.time.LocalTime ketThuc = moc.plusMinutes(soTiet * 45);
        java.time.format.DateTimeFormatter tf = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        return moc.format(tf) + " - " + ketThuc.format(tf);
    }

    private JPanel createHeaderCell(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIUtils.BG_APP);
        p.setPreferredSize(new Dimension(0, 36));
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(UIUtils.FONT_BOLD);
        l.setForeground(UIUtils.TEXT_MAIN);
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private JPanel createTimeCell(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIUtils.WHITE);
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(UIUtils.TEXT_MAIN);
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private JPanel createEmptyCell() {
        JPanel p = new JPanel();
        p.setBackground(UIUtils.WHITE);
        return p;
    }

    // Moi o co the chua NHIEU lop (vd tiet 1-3 va tiet 4-6 cung la "buoi Sang" nhung khac
    // mon) - khac ban cua SV (mot SV khong the hoc 2 lop cung luc nen chi giu 1), GV thi
    // hoan toan co the day nhieu lop khac tiet trong cung 1 buoi nen phai liet ke het.
    private JPanel createCellWithClasses(List<Object[]> danhSach) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(UIUtils.WHITE);
        wrapper.setBorder(new EmptyBorder(4, 6, 4, 6));

        for (Object[] row : danhSach) {
            String tenMon = (String) row[1];
            String theme = MAU_NEN[Math.abs(tenMon.hashCode()) % MAU_NEN.length];
            wrapper.add(createClassCard(row, theme));
            wrapper.add(Box.createVerticalStrut(4));
        }
        return wrapper;
    }

    private JPanel createClassCard(Object[] row, String colorTheme) {
        String maLHP = (String) row[0];
        String tenMon = (String) row[1];
        int siSo = (int) row[6];
        int sucChua = (int) row[5];
        String tiet = (String) row[8];
        String phong = row[9] != null ? (String) row[9] : "Chưa xếp";
        int[] tietBDKT = parseTietRange(tiet);
        String gioHoc = tietBDKT != null ? tinhGioHoc(tietBDKT[0], tietBDKT[1]) : "";

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(borderColorForTheme(colorTheme), 1, true), new EmptyBorder(6, 8, 6, 8)));
        inner.setCursor(new Cursor(Cursor.HAND_CURSOR));
        inner.setToolTipText("Nhấp để xem chi tiết");

        Color bg = UIUtils.WHITE, fg = UIUtils.TEXT_MAIN;
        if (colorTheme.equals("blue")) { bg = new Color(239, 246, 255); fg = new Color(30, 64, 175); }
        else if (colorTheme.equals("green")) { bg = new Color(240, 253, 244); fg = new Color(22, 101, 52); }
        else if (colorTheme.equals("amber")) { bg = new Color(255, 251, 235); fg = new Color(153, 84, 0); }
        else if (colorTheme.equals("purple")) { bg = new Color(250, 245, 255); fg = new Color(107, 33, 168); }
        else if (colorTheme.equals("rose")) { bg = new Color(255, 241, 242); fg = new Color(159, 18, 57); }
        inner.setBackground(bg);

        String widthStyle = "text-align:center; width:145px; white-space:normal;";
        JLabel lSubj = new JLabel("<html><div style='" + widthStyle + "'><b>" + tenMon + "</b></div></html>", SwingConstants.CENTER);
        lSubj.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lSubj.setForeground(fg);
        lSubj.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lTiet = new JLabel("<html><div style='" + widthStyle + "'>Tiết: " + tiet + " (" + gioHoc + ")</div></html>", SwingConstants.CENTER);
        lTiet.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lTiet.setForeground(fg);
        lTiet.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lRoom = new JLabel("<html><div style='" + widthStyle + "'>Phòng: " + phong + "</div></html>", SwingConstants.CENTER);
        lRoom.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lRoom.setForeground(fg);
        lRoom.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(lSubj);
        inner.add(Box.createVerticalStrut(3));
        inner.add(lTiet);
        inner.add(Box.createVerticalStrut(3));
        inner.add(lRoom);

        inner.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showChiTietDialog(maLHP, tenMon, tiet, gioHoc, phong, siSo, sucChua);
            }
        });
        return inner;
    }

    private Color borderColorForTheme(String theme) {
        switch (theme) {
            case "blue": return new Color(191, 219, 254);
            case "green": return new Color(187, 247, 208);
            case "amber": return new Color(253, 230, 138);
            case "purple": return new Color(233, 213, 255);
            case "rose": return new Color(254, 205, 211);
            default: return UIUtils.BORDER;
        }
    }

    private void showChiTietDialog(String maLHP, String tenMon, String tiet, String gioHoc, String phong, int siSo, int sucChua) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(6, 10, 6, 10));
        content.setBackground(UIUtils.WHITE);

        JLabel lblTen = new JLabel(tenMon);
        lblTen.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTen.setForeground(new Color(30, 64, 175));
        lblTen.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(lblTen);
        content.add(Box.createVerticalStrut(10));

        content.add(chiTietRow("Mã lớp học phần", maLHP));
        content.add(chiTietRow("Tiết học", tiet + " (" + gioHoc + ")"));
        content.add(chiTietRow("Phòng học", phong));
        content.add(chiTietRow("Sĩ số / Sức chứa", siSo + " / " + sucChua));

        JOptionPane.showMessageDialog(this, content, "Chi tiết tiết học", JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel chiTietRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(UIUtils.WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(360, 24));
        JLabel lLabel = new JLabel(label + ":");
        lLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lLabel.setForeground(UIUtils.TEXT_MUTED);
        lLabel.setPreferredSize(new Dimension(150, 20));
        JLabel lValue = new JLabel(value);
        lValue.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lValue.setForeground(UIUtils.TEXT_MAIN);
        row.add(lLabel, BorderLayout.WEST);
        row.add(lValue, BorderLayout.CENTER);
        return row;
    }
}
