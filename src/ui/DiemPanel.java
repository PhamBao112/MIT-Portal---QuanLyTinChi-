package ui;

import config.DBConnect;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DiemPanel extends JPanel {

    private String currentMaSV;

    private String currentMaHK = "";
    private String currentTenHK = "";
    private JComboBox<String> cbHocKy;

    private int tcKyNay = 0;
    private double gpa10 = 0.0;
    private double gpa4 = 0.0;
    private String xepLoai = "Chưa xét";

    private DefaultTableModel tableModel;
    private JPanel topStatsPanel;
    private JPanel tableContainer;

    public DiemPanel(String maSV) {
        this.currentMaSV = maSV;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(10, 0, 0, 0));

        buildUI();
    }

    private void loadHocKyOptions(JComboBox<String> combo) {
        String[] result = UIUtils.loadHocKyOptionsForStudent(combo, currentMaSV, currentMaHK);
        currentMaHK = result[0];
        currentTenHK = result[1];
    }

    private void buildUI() {
        removeAll();

        // 0. COMBO HỌC KỲ RIÊNG CỦA TRANG NÀY
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UIUtils.BG_APP);
        topBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblPageTitle = new JLabel("Bảng Kết Quả Học Tập");
        lblPageTitle.setFont(UIUtils.FONT_TITLE);
        topBar.add(lblPageTitle, BorderLayout.WEST);

        JPanel hkBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hkBox.setBackground(UIUtils.BG_APP);
        hkBox.add(new JLabel("Học kỳ:"));
        cbHocKy = new JComboBox<>();
        loadHocKyOptions(cbHocKy);
        cbHocKy.setFont(UIUtils.FONT_BOLD);
        cbHocKy.setBackground(UIUtils.WHITE);
        cbHocKy.addActionListener(e -> {
            String sel = (String) cbHocKy.getSelectedItem();
            if (sel != null && sel.contains("-")) {
                currentMaHK = sel.split("-")[0].trim();
                currentTenHK = sel.substring(sel.indexOf("-") + 1).trim();
                buildUI();
            }
        });
        hkBox.add(cbHocKy);
        topBar.add(hkBox, BorderLayout.EAST);

        // 1. TẢI DỮ LIỆU THỐNG KÊ
        loadStatsData();

        // 2. KPI CARD DÙNG CHUNG (UIUtils.createKpiCard) - bo GPA Hệ 4 vì đã thể hiện
        // qua vong tron tren banner ben duoi, tranh trung lap thong tin.
        topStatsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        topStatsPanel.setBackground(UIUtils.BG_APP);
        topStatsPanel.setMaximumSize(new Dimension(6000, 140));
        topStatsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        topStatsPanel.add(UIUtils.createKpiCard("book", tcKyNay + " TC", "Tín chỉ đạt kỳ này", new Color(37, 99, 235)));
        topStatsPanel.add(UIUtils.createKpiCard("target", String.format("%.2f", gpa10), "Điểm TB Học kỳ (Hệ 10)", UIUtils.MIT_ORANGE));
        topStatsPanel.add(UIUtils.createKpiCard("medal", xepLoai, "Xếp loại Học kỳ", UIUtils.MIT_RED));

        // V9: banner gradient + vong tron GPA Hệ 4 - dong bo phong cach voi Dashboard
        JPanel hero = UIUtils.createHeroBanner();
        hero.setLayout(new BorderLayout(20, 0));
        hero.setBorder(new EmptyBorder(18, 28, 18, 26));
        hero.setMaximumSize(new Dimension(6000, 100));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel heroLeft = new JPanel();
        heroLeft.setLayout(new BoxLayout(heroLeft, BoxLayout.Y_AXIS));
        heroLeft.setOpaque(false);
        JLabel lblHeroTitle = new JLabel("Kết quả học tập – " + currentTenHK);
        lblHeroTitle.setFont(new Font("Segoe UI", Font.BOLD, 19));
        lblHeroTitle.setForeground(Color.WHITE);
        lblHeroTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblHeroSub = new JLabel(gpa4 > 0
            ? "Xếp loại: " + xepLoai + "  ·  " + tcKyNay + " tín chỉ đạt kỳ này"
            : "Chưa có điểm tổng kết trong học kỳ này");
        lblHeroSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblHeroSub.setForeground(new Color(255, 255, 255, 215));
        lblHeroSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblHeroSub.setBorder(new EmptyBorder(6, 0, 0, 0));
        heroLeft.add(Box.createVerticalGlue());
        heroLeft.add(lblHeroTitle);
        heroLeft.add(lblHeroSub);
        heroLeft.add(Box.createVerticalGlue());

        int pctGpa4 = (int) Math.round(Math.min(100.0, gpa4 * 100.0 / 4.0));
        JComponent ring = UIUtils.createRadialProgress(pctGpa4, String.format("%.2f", gpa4), "GPA / 4.0",
            UIUtils.MIT_YELLOW, new Color(255, 255, 255, 55), Color.WHITE);
        ring.setPreferredSize(new Dimension(88, 88));
        ring.setOpaque(false);

        hero.add(heroLeft, BorderLayout.WEST);
        hero.add(ring, BorderLayout.EAST);

        // 3. BẢNG ĐIỂM CHI TIẾT (card shell dùng chung)
        tableContainer = UIUtils.createCardShell("Bảng Điểm Chi Tiết - " + currentTenHK, UIUtils.MIT_RED);

        String[] columns = {"STT", "Mã HP", "Tên môn học", "Số TC", "Điểm QT", "Điểm thường", "Được dự thi", "Điểm cuối", "Điểm TK H10", "Hệ 4", "Điểm chữ", "Xếp loại", "Đạt"};
        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = new JTable(tableModel);
        UIUtils.styleTable(table);

        int lastCol = table.getColumnCount() - 1;
        ZebraRenderer zebra = new ZebraRenderer();
        for (int i = 0; i < lastCol; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        table.getColumnModel().getColumn(lastCol).setCellRenderer(new BadgeRenderer());

        int[] colWidths = {45, 90, 220, 70, 90, 105, 115, 95, 115, 65, 90, 95, 95};
        for (int i = 0; i < colWidths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(colWidths[i]);
        }
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader headerTable = table.getTableHeader();
        headerTable.setPreferredSize(new Dimension(headerTable.getPreferredSize().width, 40));

        loadTableData();

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));

        tableContainer.add(scrollTable, BorderLayout.CENTER);

        JPanel northWrapper = new JPanel();
        northWrapper.setLayout(new BoxLayout(northWrapper, BoxLayout.Y_AXIS));
        northWrapper.setBackground(UIUtils.BG_APP);
        northWrapper.add(topBar);
        northWrapper.add(Box.createVerticalStrut(15));
        northWrapper.add(hero);
        northWrapper.add(Box.createVerticalStrut(15));
        northWrapper.add(topStatsPanel);

        add(northWrapper, BorderLayout.NORTH);
        add(tableContainer, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void loadStatsData() {
        tcKyNay = 0; gpa10 = 0.0; gpa4 = 0.0; xepLoai = "Chưa xét";

        try (Connection conn = DBConnect.getConnection()) {
            if (conn == null) return;

            String sqlDiem = "SELECT " +
                             "ISNULL(SUM(CASE WHEN kq.TrangThai = N'Đạt' THEN CAST(m.SoTinChi AS INT) ELSE 0 END), 0) as TCDat, " +
                             "SUM(CAST(kq.DiemTongKet AS FLOAT) * CAST(m.SoTinChi AS FLOAT)) as TongDiemNhanTC, " +
                             "SUM(CAST(m.SoTinChi AS FLOAT)) as TongTCCoDiem " +
                             "FROM KET_QUA_DANG_KY kq " +
                             "JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP " +
                             "JOIN MON_HOC m ON lhp.MaMon = m.MaMon " +
                             "WHERE kq.MaSV = ? AND lhp.MaHK = ? AND kq.DiemTongKet IS NOT NULL";

            try (PreparedStatement ps = conn.prepareStatement(sqlDiem)) {
                ps.setString(1, currentMaSV);
                ps.setString(2, currentMaHK);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    tcKyNay = rs.getInt("TCDat");
                    double tongTCCoDiem = rs.getDouble("TongTCCoDiem");
                    gpa10 = tongTCCoDiem > 0 ? rs.getDouble("TongDiemNhanTC") / tongTCCoDiem : 0.0;

                    if (gpa10 > 0) {
                        gpa4 = (gpa10 / 10.0) * 4.0;
                        if (gpa4 >= 3.6) xepLoai = "Xuất sắc";
                        else if (gpa4 >= 3.2) xepLoai = "Giỏi";
                        else if (gpa4 >= 2.5) xepLoai = "Khá";
                        else if (gpa4 >= 2.0) xepLoai = "Trung bình";
                        else xepLoai = "Yếu";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTableData() {
        try (Connection conn = DBConnect.getConnection()) {
            String sql = "SELECT lhp.MaMon, m.TenMon, m.SoTinChi, kq.DiemChuyenCan, kq.DiemGiuaKy, kq.DiemCuoiKy, kq.DiemTongKet, kq.TrangThai " +
                         "FROM KET_QUA_DANG_KY kq " +
                         "JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP " +
                         "JOIN MON_HOC m ON lhp.MaMon = m.MaMon " +
                         "WHERE kq.MaSV = ? AND lhp.MaHK = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentMaSV);
            ps.setString(2, currentMaHK);
            ResultSet rs = ps.executeQuery();

            int stt = 1;
            while(rs.next()) {
                double chuyenCan = rs.getDouble("DiemChuyenCan");
                double giuaKy = rs.getDouble("DiemGiuaKy");
                double cuoiKy = rs.getDouble("DiemCuoiKy");
                double tongKet = rs.getDouble("DiemTongKet");

                double he4 = tongKet > 0 ? Math.round((tongKet / 10.0 * 4.0) * 10.0) / 10.0 : 0.0;

                String diemChu;
                if (he4 >= 3.6) diemChu = "A";
                else if (he4 >= 3.2) diemChu = "B+";
                else if (he4 >= 2.5) diemChu = "B";
                else if (he4 >= 2.0) diemChu = "C+";
                else if (he4 >= 1.5) diemChu = "C";
                else if (he4 >= 1.0) diemChu = "D+";
                else if (he4 >= 0.5) diemChu = "D";
                else diemChu = "F";

                String xepLoaiMon;
                if (he4 >= 3.6) xepLoaiMon = "Xuất sắc";
                else if (he4 >= 3.2) xepLoaiMon = "Giỏi";
                else if (he4 >= 2.5) xepLoaiMon = "Khá";
                else if (he4 >= 2.0) xepLoaiMon = "Trung bình";
                else xepLoaiMon = "Yếu";

                String trangThaiRaw = rs.getString("TrangThai");
                String dat;
                if (trangThaiRaw == null || trangThaiRaw.contains("Chưa có điểm")) {
                    dat = "Đang học";
                } else if (trangThaiRaw.contains("Không đạt")) {
                    dat = "Không đạt";
                } else if (trangThaiRaw.contains("Đạt")) {
                    dat = "Đạt";
                } else {
                    dat = trangThaiRaw;
                }

                String duocDuThi = tongKet > 0 ? "Đã thi" : "Chưa thi";

                String chuyenCanStr = chuyenCan > 0 ? formatScore(chuyenCan) : "";
                String giuaKyStr = giuaKy > 0 ? formatScore(giuaKy) : "";
                String cuoiKyStr = cuoiKy > 0 ? formatScore(cuoiKy) : "";
                String tongKetStr = tongKet > 0 ? formatScore(tongKet) : "";
                String he4Str = he4 > 0 ? formatScore(he4) : "";

                tableModel.addRow(new Object[]{
                    stt++, rs.getString("MaMon"), rs.getString("TenMon"), rs.getInt("SoTinChi"),
                    chuyenCanStr, giuaKyStr, duocDuThi, cuoiKyStr, tongKetStr, he4Str, diemChu, xepLoaiMon, dat
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            tableModel.addRow(new Object[]{"---", "Lỗi tải dữ liệu", "", "", "", "", "", "", "", "", "", "", ""});
        }
    }

    private String formatScore(double score) {
        if (score <= 0) return "";
        if (score == Math.floor(score)) {
            return String.format("%.0f", score);
        }
        return String.format("%.1f", score);
    }

    class ZebraRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
            } else {
                c.setBackground(UIUtils.MIT_RED_LIGHT);
            }

            if (value != null) {
                String text = value.toString();
                if (column >= 4 && column <= 9 && !text.isEmpty()) {
                    c.setForeground(new Color(15, 23, 42));
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                } else {
                    c.setForeground(UIUtils.TEXT_MAIN);
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                }
            }
            setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(0, 15, 0, 15)));
            return c;
        }
    }

    /** Cột "Đạt" -> badge tròn màu dùng chung UIUtils. */
    class BadgeRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            wrap.setBackground(isSelected ? UIUtils.MIT_RED_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
            wrap.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(6, 15, 6, 15)));
            if (value != null) {
                String text = value.toString();
                int type = text.equals("Đạt") ? UIUtils.BADGE_OK
                         : text.equals("Không đạt") ? UIUtils.BADGE_BAD
                         : UIUtils.BADGE_PENDING; // "Đang học"
                wrap.add(UIUtils.createBadge(text, type));
            }
            return wrap;
        }
    }
}
