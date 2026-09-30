package ui;

import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Trang chu tong quan cho Giang vien: vai KPI card don gian
 * (so lop dang day, tong so SV, si so trung binh/lop).
 */
public class GVDashboardPanel extends JPanel {

    private final StudentManagerService service;
    private final String maGV;
    private final String hoTen;

    public GVDashboardPanel(StudentManagerService service, String maGV, String hoTen) {
        this.service = service;
        this.maGV = maGV;
        this.hoTen = hoTen;

        setLayout(new BorderLayout(0, 20));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(10, 0, 0, 0));

        buildUI();
    }

    private void buildUI() {
        List<Object[]> lop = service.getLopHocPhanByGV(maGV);
        int soLop = lop.size();
        int tongSV = 0;
        for (Object[] row : lop) tongSV += (int) row[6];
        double siSoTB = soLop > 0 ? (double) tongSV / soLop : 0.0;

        JPanel hero = UIUtils.createHeroBanner();
        hero.setLayout(new BorderLayout());
        hero.setBorder(new EmptyBorder(20, 28, 20, 26));
        hero.setMaximumSize(new Dimension(6000, 90));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel heroLeft = new JPanel();
        heroLeft.setLayout(new BoxLayout(heroLeft, BoxLayout.Y_AXIS));
        heroLeft.setOpaque(false);
        JLabel lblGreet = new JLabel(UIUtils.greetingByHour() + ", " + hoTen);
        lblGreet.setFont(new Font("Segoe UI", Font.BOLD, 19));
        lblGreet.setForeground(Color.WHITE);
        JLabel lblSub = new JLabel("Bạn đang giảng dạy " + soLop + " lớp học phần với tổng " + tongSV + " sinh viên.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(255, 255, 255, 215));
        lblSub.setBorder(new EmptyBorder(6, 0, 0, 0));
        heroLeft.add(lblGreet);
        heroLeft.add(lblSub);
        hero.add(heroLeft, BorderLayout.WEST);

        JPanel kpiPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        kpiPanel.setBackground(UIUtils.BG_APP);
        kpiPanel.setMaximumSize(new Dimension(6000, 140));
        kpiPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        kpiPanel.add(UIUtils.createKpiCard("book", String.valueOf(soLop), "Lớp học phần đang dạy", new Color(37, 99, 235)));
        kpiPanel.add(UIUtils.createKpiCard("users", String.valueOf(tongSV), "Tổng số sinh viên", UIUtils.MIT_ORANGE));
        kpiPanel.add(UIUtils.createKpiCard("target", String.format("%.1f", siSoTB), "Sĩ số trung bình / lớp", UIUtils.MIT_RED));

        JPanel northWrap = new JPanel();
        northWrap.setLayout(new BoxLayout(northWrap, BoxLayout.Y_AXIS));
        northWrap.setBackground(UIUtils.BG_APP);
        northWrap.add(hero);
        northWrap.add(Box.createVerticalStrut(15));
        northWrap.add(kpiPanel);
        northWrap.add(Box.createVerticalStrut(15));
        northWrap.add(buildLichHomNayCard(lop));
        northWrap.add(Box.createVerticalStrut(15));
        northWrap.add(buildTongQuanLopCard(lop));

        add(northWrap, BorderLayout.NORTH);
    }

    // Bang tong quan toan bo lop dang day, lap khoang trong ben duoi cac card KPI/lich hom nay
    // - truoc day trang Tong quan chi co 2 khoi noi dung tren cung, phia duoi trong rong rat
    // mat can doi.
    private JPanel buildTongQuanLopCard(List<Object[]> lop) {
        JPanel card = UIUtils.createCardShell("Tổng quan các lớp đang dạy", UIUtils.MIT_RED);

        String[] columns = {"Mã LHP", "Môn học", "Học kỳ", "Sĩ số / Sức chứa", "Lịch học"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Object[] row : lop) {
            String tenHK = row[4] != null ? (String) row[4] : (String) row[3];
            int sucChua = (int) row[5];
            int siSo = (int) row[6];
            String thu = row[7] != null ? (String) row[7] : "Chưa xếp";
            String tiet = row[8] != null ? (String) row[8] : "";
            String lichHoc = tiet.isEmpty() ? thu : (thu + ", tiết " + tiet);
            model.addRow(new Object[]{ row[0], row[1], tenHK, siSo + " / " + sucChua, lichHoc });
        }

        JTable table = new JTable(model);
        UIUtils.styleTable(table);
        table.setRowHeight(36);
        int[] widths = {90, 220, 150, 130, 150};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        scroll.setPreferredSize(new Dimension(100, Math.min(36 * (lop.size() + 1) + 10, 320)));
        card.add(scroll, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(6000, 360));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    // Loc ra cac lop co Thu trung voi hom nay VA hom nay nam trong khoang
    // NgayBatDauHoc - NgayKetThucHoc cua lop (lop chua xep lich se tu dong bi loai).
    private JPanel buildLichHomNayCard(List<Object[]> lop) {
        String thuHomNay = getThuVietNam(new java.util.Date());
        long homNay = System.currentTimeMillis();

        List<Object[]> lopHomNay = new ArrayList<>();
        for (Object[] row : lop) {
            String thu = row[7] != null ? (String) row[7] : "";
            java.util.Date ngayBatDau = (java.util.Date) row[10];
            java.util.Date ngayKetThuc = (java.util.Date) row[11];
            if (thu.equals(thuHomNay) && ngayBatDau != null && ngayKetThuc != null
                && homNay >= ngayBatDau.getTime() && homNay <= ngayKetThuc.getTime()) {
                lopHomNay.add(row);
            }
        }

        JPanel card = UIUtils.createCardShell("Lịch dạy hôm nay (" + thuHomNay + ")", UIUtils.MIT_ORANGE);

        if (lopHomNay.isEmpty()) {
            JLabel lblEmpty = new JLabel("Không có lịch dạy hôm nay.");
            lblEmpty.setFont(UIUtils.FONT_NORMAL);
            lblEmpty.setForeground(UIUtils.TEXT_MUTED);
            lblEmpty.setBorder(new EmptyBorder(20, 20, 20, 20));
            card.add(lblEmpty, BorderLayout.CENTER);
            card.setMaximumSize(new Dimension(6000, 90));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            return card;
        }

        String[] columns = {"Tiết học", "Phòng", "Môn học", "Mã LHP"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Object[] row : lopHomNay) {
            String tiet = row[8] != null ? (String) row[8] : "-";
            String phong = row[9] != null ? (String) row[9] : "Chưa xếp";
            model.addRow(new Object[]{ tiet, phong, row[1], row[0] });
        }
        JTable table = new JTable(model);
        UIUtils.styleTable(table);
        table.setRowHeight(38);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        scroll.setPreferredSize(new Dimension(100, Math.min(38 * (lopHomNay.size() + 1) + 10, 220)));
        card.add(scroll, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(6000, 280));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    // Anh xa thu trong tuan sang dinh dang chuoi "Thu X" dung nhu dang luu trong LOP_HOC_PHAN.Thu
    // (da xac nhan qua man hinh Lich giang day: Thu 2..Thu 7, Chu nhat).
    private String getThuVietNam(java.util.Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        switch (cal.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:    return "Thứ 2";
            case Calendar.TUESDAY:   return "Thứ 3";
            case Calendar.WEDNESDAY: return "Thứ 4";
            case Calendar.THURSDAY:  return "Thứ 5";
            case Calendar.FRIDAY:    return "Thứ 6";
            case Calendar.SATURDAY:  return "Thứ 7";
            default:                 return "Chủ nhật";
        }
    }
}
