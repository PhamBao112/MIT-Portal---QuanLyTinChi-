package ui;

import config.DBConnect;
import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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

    public LichHocPanel(StudentManagerService service, String maSV) {
        this.service = service;
        this.currentMaSV = maSV;
        
        setLayout(new BorderLayout());
        setBackground(UIUtils.WHITE);
        setBorder(new LineBorder(UIUtils.BORDER, 1, true));

        // Khởi tạo ngày bắt đầu mặc định nếu DB chưa có
        ngayBatDauHK = LocalDate.now(); 

        buildUI();
    }

    // Combo Học kỳ riêng của trang này - chỉ liệt kê những học kỳ SV này thực sự có đăng ký
    private void loadHocKyOptions(JComboBox<String> combo) {
        combo.removeAllItems();
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT DISTINCT h.MaHK, h.TenHK FROM HOC_KY h " +
                 "JOIN LOP_HOC_PHAN lhp ON lhp.MaHK = h.MaHK " +
                 "JOIN KET_QUA_DANG_KY kq ON kq.MaLHP = lhp.MaLHP " +
                 "WHERE kq.MaSV = ? ORDER BY h.NamHoc DESC, h.MaHK DESC")) {
            ps.setString(1, currentMaSV);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) combo.addItem(rs.getString("MaHK") + " - " + rs.getString("TenHK"));
        } catch (Exception e) { e.printStackTrace(); }

        if (combo.getItemCount() == 0) {
            currentMaHK = "";
            currentTenHK = "Chua co du lieu";
            return;
        }
        String target = null;
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i);
            if (item.startsWith(currentMaHK + " ")) { target = item; break; }
        }
        if (target == null) target = combo.getItemAt(0);
        combo.setSelectedItem(target); // set truoc khi gan listener nen khong ban su kien
        currentMaHK = target.split("-")[0].trim();
        currentTenHK = target.substring(target.indexOf("-") + 1).trim();
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
                ngayBatDauHK = LocalDate.now(); // Fallback nếu DB rỗng
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildUI() {
        this.removeAll();

        // 0. COMBO HỌC KỲ RIÊNG CỦA TRANG NÀY (chỉ liệt kê học kỳ SV này thực sự có đăng ký)
        JPanel comboRow = new JPanel(new BorderLayout());
        comboRow.setBackground(UIUtils.WHITE);
        comboRow.setBorder(new EmptyBorder(15, 20, 0, 20));
        JLabel lblPageTitle = new JLabel("Lịch Học Thời Khóa Biểu");
        lblPageTitle.setFont(UIUtils.FONT_TITLE);
        comboRow.add(lblPageTitle, BorderLayout.WEST);

        JPanel hkBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        hkBox.setBackground(UIUtils.WHITE);
        hkBox.add(new JLabel("Học kỳ:"));
        cbHocKy = new JComboBox<>();
        loadHocKyOptions(cbHocKy); // populate + resolve currentMaHK/currentTenHK, chưa gắn listener nên không bắn sự kiện
        if (!java.util.Objects.equals(currentMaHK, lastLoadedHK)) {
            // Chỉ fetch lại ngày bắt đầu + reset tuần khi THẬT SỰ đổi học kỳ (không fetch lại mỗi lần bấm tuần)
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
        // 1. HEADER & THANH ĐIỀU HƯỚNG TUẦN ( ĐÃ CẮT MÃ HK )
        // ==========================================
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(239, 246, 255)); // Nền Xanh dương nhạt
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(191, 219, 254)), // Viền dưới nổi nhẹ
            new EmptyBorder(15, 20, 15, 20)
        ));
        
        // --- THUẬT TOÁN TỰ ĐỘNG LỌC BỎ MÃ HỌC KỲ ---
        String tenHKRutGọn = currentTenHK;
        if (currentTenHK != null && currentTenHK.contains("-")) {
            // Cắt từ sau dấu gạch ngang (-) trở đi để lấy nguyên cái tên học kỳ thôi
            tenHKRutGọn = currentTenHK.substring(currentTenHK.indexOf("-") + 1).trim();
        }
        
        JLabel lblTitle = new JLabel("Lịch Học Thời Khóa Biểu (" + tenHKRutGọn + ")");
        lblTitle.setFont(UIUtils.FONT_TITLE); 
        lblTitle.setForeground(new Color(30, 64, 175)); // Chữ Xanh dương đậm
        
        header.add(lblTitle, BorderLayout.WEST);

        // Thanh điều hướng tuần
        JPanel weekNavPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        weekNavPanel.setBackground(UIUtils.BG_APP);

        JButton btnPrev = new JButton("<");
        styleNavButton(btnPrev);
        
        lblWeek = new JLabel("Tuần " + currentWeek);
        lblWeek.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblWeek.setForeground(UIUtils.TEXT_MAIN);
        lblWeek.setPreferredSize(new Dimension(80, 30));
        lblWeek.setHorizontalAlignment(SwingConstants.CENTER);
        
        JButton btnNext = new JButton(">");
        styleNavButton(btnNext);

        // Sự kiện tiến lùi tuần
        btnPrev.addActionListener(e -> {
            if (currentWeek > 1) {
                currentWeek--;
                buildUI();
            }
        });

        btnNext.addActionListener(e -> {
            if (currentWeek < maxWeek) { 
                currentWeek++;
                buildUI(); 
            }
        });

        weekNavPanel.add(btnPrev);
        weekNavPanel.add(lblWeek);
        weekNavPanel.add(btnNext);
        header.add(weekNavPanel, BorderLayout.EAST);

        // ==========================================
        // 2. VẼ LƯỚI LỊCH HỌC (GRID)
        // ==========================================
        gridContainer = new JPanel(new GridLayout(5, 7, 1, 1)); 
        gridContainer.setBackground(UIUtils.BORDER); 
        gridContainer.setBorder(new MatteBorder(1, 1, 1, 1, UIUtils.BORDER));

        // Tính ngày thứ 2 của tuần hiện tại
        LocalDate startOfWeek = ngayBatDauHK.plusWeeks(currentWeek - 1);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM");

        // Vẽ Header Cột (Thứ + Ngày tháng)
        gridContainer.add(createHeaderCell("Ca Học", ""));
        String[] thuNames = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};
        
        for (int i = 0; i < 6; i++) {
            LocalDate currentDate = startOfWeek.plusDays(i);
            boolean isToday = currentDate.equals(LocalDate.now());
            gridContainer.add(createHeaderCell(thuNames[i], currentDate.format(df), isToday));
        }

        // Lấy dữ liệu từ CSDL cho học kỳ này
        ClassInfo[][] schedule = fetchScheduleFromDB();

        // Vẽ các ô thời khóa biểu
        String[] timeLabels = { 
            "Ca 1<br><span style='font-size:9px; color:#64748b'>(07:00 - 09:30)</span>", 
            "Ca 2<br><span style='font-size:9px; color:#64748b'>(09:40 - 12:10)</span>", 
            "Ca 3<br><span style='font-size:9px; color:#64748b'>(13:00 - 15:30)</span>", 
            "Ca 4<br><span style='font-size:9px; color:#64748b'>(15:40 - 18:10)</span>" 
        };
        String[] themes = {"blue", "green", "amber", "purple", "rose"}; 

        for (int row = 0; row < 4; row++) {
            gridContainer.add(createTimeCell(timeLabels[row])); 
            
            for (int col = 0; col < 6; col++) {
                ClassInfo info = schedule[row][col];
                if (info != null) {
                    String theme = themes[Math.abs(info.subject.hashCode()) % themes.length];
                    gridContainer.add(createClassCell(info.subject, info.room, info.giangVien, theme));
                } else {
                    gridContainer.add(createEmptyCell());
                }
            }
        }

        JPanel northWrapper = new JPanel();
        northWrapper.setLayout(new BoxLayout(northWrapper, BoxLayout.Y_AXIS));
        northWrapper.setBackground(UIUtils.WHITE);
        northWrapper.add(comboRow);
        northWrapper.add(header);

        add(northWrapper, BorderLayout.NORTH); 
        add(gridContainer, BorderLayout.CENTER);
        
        this.revalidate();
        this.repaint();
    }

    // ==========================================
    // LOGIC DATABASE & PARSER
    // ==========================================
    private ClassInfo[][] fetchScheduleFromDB() {
        ClassInfo[][] sch = new ClassInfo[4][6];
        
        String sql = "SELECT lhp.Thu, lhp.TietHoc, lhp.PhongHoc, mh.TenMon, gv.HoTen AS TenGV " +
                     "FROM KET_QUA_DANG_KY kq " +
                     "JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP " +
                     "JOIN MON_HOC mh ON lhp.MaMon = mh.MaMon " +
                     "LEFT JOIN GIANG_VIEN gv ON lhp.MaGV = gv.MaGV " + // JOIN lấy tên GV
                     "WHERE kq.MaSV = ? AND lhp.MaHK = ?";
                     
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
                if (gv == null) gv = "Khoa CM"; // Nếu chưa phân công
                
                int col = parseThu(thuStr);
                int row = parseCa(tiet);
                
                if (col != -1 && row != -1) {
                    sch[row][col] = new ClassInfo(mon, "Phòng: " + phong, "GV: " + gv);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    private int parseCa(String tietHoc) {
        if (tietHoc == null || tietHoc.isBlank()) return -1;
        try {
            int start = Integer.parseInt(tietHoc.trim().split("-")[0].trim());
            if (start <= 3) return 0;      // Ca 1 (1-3)
            if (start <= 6) return 1;      // Ca 2 (4-6)
            if (start <= 9) return 2;      // Ca 3 (7-9)
            return 3;                      // Ca 4 (10-12)
        } catch (Exception e) { return -1; }
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
        p.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        JLabel lThu = new JLabel(thu, SwingConstants.CENTER); 
        lThu.setFont(UIUtils.FONT_BOLD); 
        lThu.setForeground(isToday ? UIUtils.MIT_RED : UIUtils.TEXT_MAIN); 
        p.add(lThu, BorderLayout.CENTER);
        
        if (!date.isEmpty()) {
            JLabel lDate = new JLabel(date, SwingConstants.CENTER);
            lDate.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lDate.setForeground(isToday ? UIUtils.MIT_RED : UIUtils.TEXT_MUTED);
            p.add(lDate, BorderLayout.SOUTH);
        }
        return p;
    }

    private JPanel createTimeCell(String text) {
        JPanel p = new JPanel(new BorderLayout()); 
        p.setBackground(UIUtils.WHITE);
        JLabel l = new JLabel("<html><center>" + text + "</center></html>", SwingConstants.CENTER); 
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(UIUtils.TEXT_MAIN); 
        p.add(l, BorderLayout.CENTER); 
        return p;
    }

    private JPanel createEmptyCell() { 
        JPanel p = new JPanel(); 
        p.setBackground(UIUtils.WHITE); 
        return p; 
    }
    
    // Tích hợp tên Giảng viên vào ô Môn học
    private JPanel createClassCell(String subject, String room, String gv, String colorTheme) {
        JPanel wrapper = new JPanel(new BorderLayout()); 
        wrapper.setBackground(UIUtils.WHITE); 
        wrapper.setBorder(new EmptyBorder(4, 4, 4, 4));
        
        JPanel inner = new JPanel(); 
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS)); 
        inner.setBorder(new EmptyBorder(8, 8, 8, 8));
        
        Color bg = UIUtils.WHITE, fg = UIUtils.TEXT_MAIN;
        if (colorTheme.equals("blue")) { bg = new Color(239,246,255); fg = new Color(30,64,175); } 
        else if (colorTheme.equals("green")) { bg = new Color(240,253,244); fg = new Color(22,101,52); } 
        else if (colorTheme.equals("amber")) { bg = new Color(255,251,235); fg = new Color(153,84,0); }
        else if (colorTheme.equals("purple")) { bg = new Color(250,245,255); fg = new Color(107,33,168); }
        else if (colorTheme.equals("rose")) { bg = new Color(255,241,242); fg = new Color(159,18,57); }
        
        inner.setBackground(bg);
        
        JLabel lSubj = new JLabel("<html><div style='text-align:center'><b>" + subject + "</b></div></html>", SwingConstants.CENTER); 
        lSubj.setFont(new Font("Segoe UI", Font.PLAIN, 12)); 
        lSubj.setForeground(fg); 
        lSubj.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lRoom = new JLabel(room, SwingConstants.CENTER); 
        lRoom.setFont(new Font("Segoe UI", Font.PLAIN, 11)); 
        lRoom.setForeground(fg); 
        lRoom.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Thêm label tên Giảng Viên
        JLabel lGV = new JLabel(gv, SwingConstants.CENTER); 
        lGV.setFont(new Font("Segoe UI", Font.ITALIC, 11)); 
        lGV.setForeground(fg); 
        lGV.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        inner.add(Box.createVerticalGlue()); 
        inner.add(lSubj); 
        inner.add(Box.createVerticalStrut(4)); 
        inner.add(lRoom); 
        inner.add(Box.createVerticalStrut(2)); 
        inner.add(lGV);
        inner.add(Box.createVerticalGlue()); 
        
        wrapper.add(inner, BorderLayout.CENTER); 
        return wrapper;
    }

    // Lớp chứa data nội bộ cho Panel
    static class ClassInfo {
        String subject, room, giangVien;
        ClassInfo(String s, String r, String g) { subject = s; room = r; giangVien = g; }
    }
}