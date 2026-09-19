package ui;

import config.DBConnect;
import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LichHocPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private StudentManagerService service;
    private String currentMaSV;
    private String currentMaHK = "";
    private String currentTenHK = "";
    private String lastLoadedHK = null;
    private JComboBox<String> cbHocKy;

    // Quản lý tuần học
    private int currentWeek = 1;
    private int maxWeek = 15; // Giả sử 1 học kỳ có 15 tuần
    private LocalDate ngayBatDauHK;
    private JLabel lblWeek;
    private JPanel gridContainer;

    // FIX: truoc day chi 1 ClassInfo / o (4 Ca x 6 Thu) -> 2 lop hoc khac nhau nhung cung buoi
    // (vd tiet 1-3 va tiet 4-6 deu roi vao "buoi sang") se GHI DE len nhau, mat du lieu / trung
    // lap tuy thu tu doc tu DB. Gio moi o la 1 DANH SACH, khong con gioi han so lop/buoi.
    private static final int SO_HANG = 2;      // 0 = Sang, 1 = Chieu
    private static final int SO_COT = 6;       // Thu 2 -> Thu 7
    private static final String[] TEN_BUOI = {"Sáng", "Chiều"};
    private static final String[] TEN_THU = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};

    public LichHocPanel(StudentManagerService service, String maSV) {
        this.service = service;
        this.currentMaSV = maSV;

        setLayout(new BorderLayout());
        setBackground(UIUtils.WHITE);
        setBorder(new LineBorder(UIUtils.BORDER, 1, true));

        ngayBatDauHK = LocalDate.now();

        buildUI();
    }

    // Combo Học kỳ riêng của trang này - chỉ liệt kê những học kỳ SV này thực sự có đăng ký.
    private void loadHocKyOptions(JComboBox<String> combo) {
        String[] result = UIUtils.loadHocKyOptionsForStudent(combo, currentMaSV, currentMaHK);
        currentMaHK = result[0];
        currentTenHK = result[1];
    }

    private void fetchNgayBatDauHK() {
        try (Connection conn = DBConnect.getConnection()) {
            String sql = "SELECT NgayBatDau FROM HOC_KY WHERE MaHK = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentMaHK);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getDate("NgayBatDau") != null) {
                ngayBatDauHK = rs.getDate("NgayBatDau").toLocalDate();
            } else {
                ngayBatDauHK = LocalDate.now();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildUI() {
        this.removeAll();

        // 0. COMBO HỌC KỲ RIÊNG CỦA TRANG NÀY
        JPanel comboRow = new JPanel(new BorderLayout());
        comboRow.setBackground(UIUtils.WHITE);
        comboRow.setBorder(new EmptyBorder(15, 20, 0, 20));

        JPanel hkBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hkBox.setBackground(UIUtils.WHITE);
        hkBox.add(new JLabel("Học kỳ:"));
        cbHocKy = new JComboBox<>();
        loadHocKyOptions(cbHocKy);
        if (!java.util.Objects.equals(currentMaHK, lastLoadedHK)) {
            fetchNgayBatDauHK();
            currentWeek = 1;
            lastLoadedHK = currentMaHK;
        }
        cbHocKy.setFont(UIUtils.FONT_BOLD);
        cbHocKy.setBackground(UIUtils.WHITE);
        cbHocKy.addActionListener(e -> buildUI());
        hkBox.add(cbHocKy);
        comboRow.add(hkBox, BorderLayout.EAST);

        // ==========================================
        // 1. HEADER & THANH ĐIỀU HƯỚNG TUẦN
        // ==========================================
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtils.MIT_RED_LIGHT);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIUtils.MIT_RED),
            new EmptyBorder(15, 20, 15, 20)
        ));

        // FIX: truoc day cat chuoi theo dau "-" HAI LAN lien tiep - lan dau bo ma hoc ky
        // ("HK2_2425 - Hoc ky 2 2024-2025" -> "Hoc ky 2 2024-2025"), nhung ham nay lai cat
        // THEM 1 lan nua va vo tinh cat ngay trong nam hoc "2024-2025", con lai mỗi "(2025)".
        // Gio KHONG cat lai - currentTenHK tu UIUtils da la ten rut gon dung roi.
        String tenHKHienThi = (currentTenHK == null || currentTenHK.isBlank()) ? "Chưa có dữ liệu" : currentTenHK;

        // FIX #2: bo lap chu "Lich Hoc Thoi Khoa Bieu" - chu nay da hien o thanh tieu de
        // dung chung tren cung (StudentPanel.lblHeaderTitle). O day chi con can hien HOC KY
        // dang xem, khong lap lai ten trang.
        JLabel lblTitle = new JLabel("Học kỳ: " + tenHKHienThi);
        lblTitle.setFont(UIUtils.FONT_TITLE);
        lblTitle.setForeground(UIUtils.MIT_RED);

        header.add(lblTitle, BorderLayout.WEST);

        JPanel weekNavPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        weekNavPanel.setBackground(UIUtils.MIT_RED_LIGHT);

        JButton btnPrev = new JButton("<");
        styleNavButton(btnPrev);

        lblWeek = new JLabel("Tuần " + currentWeek);
        lblWeek.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblWeek.setForeground(UIUtils.TEXT_MAIN);
        lblWeek.setPreferredSize(new Dimension(80, 30));
        lblWeek.setHorizontalAlignment(SwingConstants.CENTER);

        JButton btnNext = new JButton(">");
        styleNavButton(btnNext);

        btnPrev.addActionListener(e -> {
            if (currentWeek > 1) {
                currentWeek--;
                updateWeekView();
            }
        });

        btnNext.addActionListener(e -> {
            if (currentWeek < maxWeek) {
                currentWeek++;
                updateWeekView();
            }
        });

        weekNavPanel.add(btnPrev);
        weekNavPanel.add(lblWeek);
        weekNavPanel.add(btnNext);
        header.add(weekNavPanel, BorderLayout.EAST);

        // ==========================================
        // 2. VẼ LƯỚI LỊCH HỌC (GRID) - 2 buổi x 6 thứ
        // ==========================================
        gridContainer = new JPanel(new GridLayout(SO_HANG + 1, SO_COT + 1, 1, 1));
        gridContainer.setBackground(UIUtils.BORDER);
        gridContainer.setBorder(new MatteBorder(1, 1, 1, 1, UIUtils.BORDER));
        populateGrid();

        JPanel northWrapper = new JPanel();
        northWrapper.setLayout(new BoxLayout(northWrapper, BoxLayout.Y_AXIS));
        northWrapper.setBackground(UIUtils.WHITE);
        northWrapper.add(comboRow);
        northWrapper.add(header);

        // FIX 4: wrap gridContainer trong JScrollPane de xu ly tran vien khi nhieu mon hoc
        // GridLayout se tinh chieu cao dong tu theo cell co dai dai nhat trong dong do
        JScrollPane scrollGrid = new JScrollPane(gridContainer);
        scrollGrid.setBorder(null);
        scrollGrid.setBackground(UIUtils.WHITE);
        scrollGrid.getViewport().setBackground(UIUtils.WHITE);
        // Luon cho scrollbar hien thi khi can thiet
        scrollGrid.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollGrid.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(northWrapper, BorderLayout.NORTH);
        add(scrollGrid, BorderLayout.CENTER);

        this.revalidate();
        this.repaint();
    }

    private void populateGrid() {
        LocalDate startOfWeek = ngayBatDauHK.plusWeeks(currentWeek - 1);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM");

        gridContainer.add(createHeaderCell("Buổi Học", ""));
        for (int i = 0; i < SO_COT; i++) {
            LocalDate currentDate = startOfWeek.plusDays(i);
            boolean isToday = currentDate.equals(LocalDate.now());
            gridContainer.add(createHeaderCell(TEN_THU[i], currentDate.format(df), isToday));
        }

        List<ClassInfo>[][] schedule = fetchScheduleFromDB(startOfWeek);

        // FIX: thu hep bang - truoc day GridLayout keo gian 7 cot ra het chieu rong panel du
        // noi dung ("Thu 2", ngay thang, the mon hoc) khong can nhieu cho nhu vay. Ep kich
        // thuoc co dinh vua du: cot nhan "Buoi Hoc" 110px, moi cot Thu 160px.
        int colLabelWidth = 110;
        int colThuWidth = 160;
        int headerRowHeight = 40;
        int bodyRowHeight = 175;
        int tongRong = colLabelWidth + SO_COT * colThuWidth;
        int tongCao = headerRowHeight + SO_HANG * bodyRowHeight;
        gridContainer.setPreferredSize(new Dimension(tongRong, tongCao));

        // FIX 3: đảm bảo mỗi môn chỉ chiếm 1 buổi học (Sáng hoặc Chiều) trong ngày
        // nếu có nhiều môn cùng tiết, chỉ giữ lại môn sớm nhất (tiet bat dau nho nhat)
        for (int row = 0; row < SO_HANG; row++) {
            gridContainer.add(createTimeCell(TEN_BUOI[row]));
            for (int col = 0; col < SO_COT; col++) {
                List<ClassInfo> danhSach = schedule[row][col];
                if (danhSach != null && !danhSach.isEmpty()) {
                    // tìm môn có tiet bat dau nho nhat (nghĩa là bắt đầu sớm nhất trong chuỗi tiết)
                    ClassInfo monDauTien = danhSach.get(0);
                    for (ClassInfo info : danhSach) {
                        if (info.tietBatDau < monDauTien.tietBatDau) {
                            monDauTien = info;
                        }
                    }
                    // chỉ hiển thị môn đầu tiên
                    gridContainer.add(createCellWithSingleClass(monDauTien));
                } else {
                    gridContainer.add(createEmptyCell());
                }
            }
        }
    }

    private void updateWeekView() {
        gridContainer.removeAll();
        populateGrid();
        lblWeek.setText("Tuần " + currentWeek);
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    // ==========================================
    // LOGIC DATABASE & PARSER
    // ==========================================
    @SuppressWarnings("unchecked")
    private List<ClassInfo>[][] fetchScheduleFromDB(LocalDate startOfWeek) {
        List<ClassInfo>[][] sch = new List[SO_HANG][SO_COT];
        for (int i = 0; i < SO_HANG; i++)
            for (int j = 0; j < SO_COT; j++)
                sch[i][j] = new ArrayList<>();

        // FIX (yeu cau moi): chi lay lop DA XEP LICH, kem NgayBatDauHoc/NgayKetThucHoc de
        // kiem tra lop co con "hoat dong" trong tuan dang xem hay khong (lop chi hoc trong
        // 1 cua so ~8 tuan, KHONG lap lai suot ca hoc ky nhu truoc).
        String sql = "SELECT DISTINCT lhp.MaLHP, lhp.Thu, lhp.TietHoc, lhp.PhongHoc, " +
                     "lhp.NgayBatDauHoc, lhp.NgayKetThucHoc, " +
                     "mh.TenMon, mh.SoTinChi, " +
                     "gv.HoTen AS TenGV, gv.SoDienThoai AS SdtGV, gv.Email AS EmailGV, gv.HocVi AS HocViGV " +
                     "FROM KET_QUA_DANG_KY kq " +
                     "JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP " +
                     "JOIN MON_HOC mh ON lhp.MaMon = mh.MaMon " +
                     "LEFT JOIN GIANG_VIEN gv ON lhp.MaGV = gv.MaGV " +
                     "WHERE kq.MaSV = ? AND lhp.MaHK = ? AND lhp.TrangThaiXepLich = N'Đã xếp lịch'";

        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentMaSV);
            ps.setString(2, currentMaHK);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String thuStr = rs.getString("Thu");
                String tiet = rs.getString("TietHoc");
                String phong = rs.getString("PhongHoc");
                String mon = rs.getString("TenMon");
                String gv = rs.getString("TenGV");
                if (gv == null) gv = "Đang cập nhật";

                int col = parseThu(thuStr);
                int[] tietBDKT = parseTietRange(tiet);
                if (col == -1 || tietBDKT == null) continue;

                // FIX (yeu cau moi): kiem tra ngay thuc te cua "Thu" nay trong TUAN DANG XEM co
                // nam trong cua so [NgayBatDauHoc, NgayKetThucHoc] cua lop khong. Ngoai cua so
                // (chua toi ngay hoc, hoac da hoc xong 8 tuan) thi KHONG hien o tuan nay.
                java.sql.Date ngayBDSql = rs.getDate("NgayBatDauHoc");
                java.sql.Date ngayKTSql = rs.getDate("NgayKetThucHoc");
                if (ngayBDSql == null || ngayKTSql == null) continue; // an toan: thieu du lieu cua so hoc
                LocalDate ngayThucTe = startOfWeek.plusDays(col);
                if (ngayThucTe.isBefore(ngayBDSql.toLocalDate()) || ngayThucTe.isAfter(ngayKTSql.toLocalDate())) {
                    continue; // tuan nay nam ngoai cua so hoc cua lop
                }

                int row = tietBDKT[0] <= 6 ? 0 : 1; // FIX: gom theo Sang/Chieu thay vi 4 "Ca" cung nhac
                                                     // truoc day (chinh la nguon goc mat/de len du lieu).

                ClassInfo info = new ClassInfo();
                info.maLHP = rs.getString("MaLHP");
                info.subject = mon;
                info.room = phong;
                info.giangVien = gv;
                info.sdtGV = rs.getString("SdtGV");
                info.emailGV = rs.getString("EmailGV");
                info.hocViGV = rs.getString("HocViGV");
                info.soTinChi = rs.getObject("SoTinChi") == null ? 0 : rs.getInt("SoTinChi");
                info.thu = TEN_THU[col];
                info.tietHoc = tiet;
                info.gioHoc = tinhGioHoc(tietBDKT[0], tietBDKT[1]);
                info.tietBatDau = tietBDKT[0]; // lay tien de sort

                sch[row][col].add(info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // FIX 1: sort danh sach mon trong moi o theo tiet bat dau (nho -> lon)
        for (int i = 0; i < SO_HANG; i++) {
            for (int j = 0; j < SO_COT; j++) {
                sch[i][j].sort((a, b) -> Integer.compare(a.tietBatDau, b.tietBatDau));
            }
        }
        return sch;
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

    // Tra ve [tietBatDau, tietKetThuc] hoac null neu du lieu khong dung dinh dang "so-so"
    private int[] parseTietRange(String tietHoc) {
        if (tietHoc == null || tietHoc.isBlank() || !tietHoc.contains("-")) return null;
        try {
            String[] parts = tietHoc.trim().split("-");
            int bd = Integer.parseInt(parts[0].trim());
            int kt = Integer.parseInt(parts[1].trim());
            return new int[]{bd, kt};
        } catch (Exception e) {
            return null;
        }
    }

    // Uoc luong gio hoc tu so tiet: tiet 1 bat dau 07:00, moi tiet 45 phut, nghi trua sau tiet 6.
    private String tinhGioHoc(int tietBD, int tietKT) {
        java.time.LocalTime moc = tietBD <= 6
            ? java.time.LocalTime.of(7, 0).plusMinutes((long) (tietBD - 1) * 45)
            : java.time.LocalTime.of(13, 0).plusMinutes((long) (tietBD - 7) * 45);
        long soTiet = tietKT - tietBD + 1;
        java.time.LocalTime ketThuc = moc.plusMinutes(soTiet * 45);
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        return moc.format(tf) + " - " + ketThuc.format(tf);
    }

    // ==========================================
    // UI BUILDERS
    // ==========================================
    private void styleNavButton(JButton btn) {
        btn.setFont(new Font("Consolas", Font.BOLD, 16));
        btn.setBackground(Color.WHITE);
        btn.setForeground(UIUtils.TEXT_MAIN);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.BORDER, 1, true),
            new EmptyBorder(4, 12, 4, 12)
        ));
    }

    private JPanel createHeaderCell(String thu, String date) {
        return createHeaderCell(thu, date, false);
    }

    private JPanel createHeaderCell(String thu, String date, boolean isToday) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(isToday ? UIUtils.MIT_RED_LIGHT : UIUtils.BG_APP);
        // FIX 1: ép cứng chiều cao hàng header = 40px (tương đương table.setRowHeight(0, 40))
        p.setPreferredSize(new Dimension(0, 40));
        p.setBorder(new EmptyBorder(4, 0, 4, 0));

        JLabel lThu = new JLabel(thu, SwingConstants.CENTER);
        lThu.setFont(UIUtils.FONT_BOLD);
        lThu.setForeground(isToday ? UIUtils.MIT_RED : UIUtils.TEXT_MAIN);
        p.add(lThu, BorderLayout.CENTER);

        if (!date.isEmpty()) {
            JLabel lDate = new JLabel(date, SwingConstants.CENTER);
            lDate.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lDate.setForeground(isToday ? UIUtils.MIT_RED : UIUtils.TEXT_MUTED);
            p.add(lDate, BorderLayout.SOUTH);
        }
        return p;
    }

    private JPanel createTimeCell(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIUtils.WHITE);
        JLabel l = new JLabel("<html><center>" + text + "</center></html>", SwingConstants.CENTER);
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

    // Mot o chi hien thi 1 mon (dat lenh 1 mon = 1 buoi). Neu co xung dot, hien mon bat dau nhat.
    private JPanel createCellWithSingleClass(ClassInfo info) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UIUtils.WHITE);
        wrapper.setBorder(new EmptyBorder(4, 6, 4, 6));

        String[] themes = {"blue", "green", "amber", "purple", "rose"};
        String theme = themes[Math.abs(info.subject.hashCode()) % themes.length];
        wrapper.add(createClassCard(info, theme), BorderLayout.CENTER);
        return wrapper;
    }

    // The 1 lop hoc - co the click de xem chi tiet (GV, SDT, tin chi...)
    private JPanel createClassCard(ClassInfo info, String colorTheme) {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(borderColorForTheme(colorTheme), 1, true),
            // FIX 3: tang padding ngang de tranh chu dong mat
            new EmptyBorder(7, 10, 7, 10)
        ));
        inner.setCursor(new Cursor(Cursor.HAND_CURSOR));
        inner.setToolTipText("Nhấp để xem chi tiết");

        Color bg = UIUtils.WHITE, fg = UIUtils.TEXT_MAIN;
        if (colorTheme.equals("blue")) { bg = new Color(239,246,255); fg = new Color(30,64,175); }
        else if (colorTheme.equals("green")) { bg = new Color(240,253,244); fg = new Color(22,101,52); }
        else if (colorTheme.equals("amber")) { bg = new Color(255,251,235); fg = new Color(153,84,0); }
        else if (colorTheme.equals("purple")) { bg = new Color(250,245,255); fg = new Color(107,33,168); }
        else if (colorTheme.equals("rose")) { bg = new Color(255,241,242); fg = new Color(159,18,57); }

        inner.setBackground(bg);

        // FIX 2: HTML wrap de text tu xuong dong khi dai, tranh bi xén thanh "..."
// Dung width fixed + white-space: normal de dam bao wrap o Swing HTML renderer
        String widthStyle = "text-align:center; width:130px; white-space:normal;";
        JLabel lSubj = new JLabel("<html><div style='" + widthStyle + "'><b>" + info.subject + "</b></div></html>", SwingConstants.CENTER);
        lSubj.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lSubj.setForeground(fg);
        lSubj.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lTiet = new JLabel("<html><div style='" + widthStyle + "'>Tiết: " + info.tietHoc + " (" + info.gioHoc + ")</div></html>", SwingConstants.CENTER);
        lTiet.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lTiet.setForeground(fg);
        lTiet.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lRoom = new JLabel("<html><div style='" + widthStyle + "'>Phòng: " + info.room + "</div></html>", SwingConstants.CENTER);
        lRoom.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lRoom.setForeground(fg);
        lRoom.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lGV = new JLabel("<html><div style='" + widthStyle + "'>GV: " + info.giangVien + "</div></html>", SwingConstants.CENTER);
        lGV.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lGV.setForeground(fg);
        lGV.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(lSubj);
        inner.add(Box.createVerticalStrut(4));
        inner.add(lTiet);
        inner.add(Box.createVerticalStrut(3));
        inner.add(lRoom);
        inner.add(Box.createVerticalStrut(3));
        inner.add(lGV);

        inner.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showChiTietDialog(info);
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

    // FIX: chuc nang moi theo yeu cau - click vao 1 tiet hoc hien popup chi tiet
    // (GV, hoc vi, SDT, email, tin chi, phong, gio hoc).
    private void showChiTietDialog(ClassInfo info) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(6, 10, 6, 10));
        content.setBackground(UIUtils.WHITE);

        JLabel lblTen = new JLabel(info.subject);
        lblTen.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTen.setForeground(new Color(30, 64, 175));
        lblTen.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(lblTen);
        content.add(Box.createVerticalStrut(10));

        content.add(chiTietRow("Mã lớp học phần", info.maLHP));
        content.add(chiTietRow("Số tín chỉ", info.soTinChi > 0 ? String.valueOf(info.soTinChi) : "Chưa cập nhật"));
        content.add(chiTietRow("Thời gian", info.thu + ", tiết " + info.tietHoc + " (" + info.gioHoc + ")"));
        content.add(chiTietRow("Phòng học", info.room));
        content.add(Box.createVerticalStrut(8));
        content.add(new JSeparator());
        content.add(Box.createVerticalStrut(8));

        content.add(chiTietRow("Giảng viên", (info.hocViGV != null && !info.hocViGV.isBlank() ? info.hocViGV + " " : "") + info.giangVien));
        content.add(chiTietRow("Số điện thoại GV", info.sdtGV != null && !info.sdtGV.isBlank() ? info.sdtGV : "Chưa cập nhật"));
        content.add(chiTietRow("Email GV", info.emailGV != null && !info.emailGV.isBlank() ? info.emailGV : "Chưa cập nhật"));

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
        lLabel.setPreferredSize(new Dimension(140, 20));

        JLabel lValue = new JLabel(value);
        lValue.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lValue.setForeground(UIUtils.TEXT_MAIN);

        row.add(lLabel, BorderLayout.WEST);
        row.add(lValue, BorderLayout.CENTER);
        return row;
    }

    // Lớp chứa data nội bộ cho Panel
    static class ClassInfo {
        String maLHP, subject, room, giangVien, sdtGV, emailGV, hocViGV, thu, tietHoc, gioHoc;
        int soTinChi;
        int tietBatDau; // dùng để sort thứ tự tiết
    }
}
