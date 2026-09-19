package ui;

import service.StudentManagerService;
import utils.UIUtils;
import config.DBConnect;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

public class AdminUserPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JButton btnTabSV, btnTabGV;

    // --- COMPONENTS SINH VIÊN ---
    private DefaultTableModel modelSV;
    private JTable tblSV;
    private TableRowSorter<DefaultTableModel> sorterSV;
    private JComboBox<String> cbLopFilter;
    private JTextField txtMaSV, txtHoTenSV, txtNgaySinhSV, txtSdtSV, txtEmailSV, txtLopSV, txtNganhSV;
    private JComboBox<String> cbGioiTinhSV, cbTrangThaiSV;

    // --- COMPONENTS GIẢNG VIÊN ---
    private DefaultTableModel modelGV;
    private JTable tblGV;
    private TableRowSorter<DefaultTableModel> sorterGV;
    private JComboBox<String> cbKhoaFilter;
    private JTextField txtMaGV, txtHoTenGV, txtSdtGV, txtEmailGV, txtKhoaGV;
    private JComboBox<String> cbGioiTinhGV, cbHocViGV;

    public AdminUserPanel(StudentManagerService service) {
        setLayout(new BorderLayout(0, 15));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(5, 0, 0, 0));

        // ==========================================
        // 1. THANH ĐIỀU HƯỚNG (TOGGLE TABS)
        // ==========================================
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        togglePanel.setBackground(UIUtils.BG_APP);
        togglePanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        btnTabSV = createToggleBtn("DANH SÁCH SINH VIÊN", true);
        btnTabGV = createToggleBtn("DANH SÁCH GIẢNG VIÊN", false);

        togglePanel.add(btnTabSV);
        togglePanel.add(btnTabGV);

        // ==========================================
        // 2. KHU VỰC NỘI DUNG (CARD LAYOUT)
        // ==========================================
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(UIUtils.BG_APP);

        contentPanel.add(createSinhVienView(), "SV");
        contentPanel.add(createGiangVienView(), "GV");

        btnTabSV.addActionListener(e -> {
            setToggleStyle(btnTabSV, true);
            setToggleStyle(btnTabGV, false);
            cardLayout.show(contentPanel, "SV");
            loadDataSV();
        });
        btnTabGV.addActionListener(e -> {
            setToggleStyle(btnTabGV, true);
            setToggleStyle(btnTabSV, false);
            cardLayout.show(contentPanel, "GV");
            loadDataGV();
        });

        add(togglePanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        loadDataSV();
    }

    // ========================================================
    // VIEW 1: QUẢN LÝ SINH VIÊN
    // ========================================================
    private JPanel createSinhVienView() {
        JPanel pnl = new JPanel(new BorderLayout(0, 15));
        pnl.setBackground(UIUtils.BG_APP);

        // --- 1. BẢNG DỮ LIỆU (card shell dùng chung — bo góc thật, tiêu đề + gạch chân đỏ) ---
        JPanel tableWrapper = UIUtils.createCardShell("Bảng Danh Sách Sinh Viên Toàn Trường", UIUtils.MIT_RED);

        // Bộ lọc theo Lớp — gắn vào góc phải của header card (do createCardShell tạo)
        JPanel tableHeader = (JPanel) tableWrapper.getComponent(0);
        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterBox.setOpaque(false);
        JLabel lblFilter = new JLabel("Lọc theo lớp:");
        lblFilter.setFont(UIUtils.FONT_NORMAL);
        lblFilter.setForeground(UIUtils.TEXT_MUTED);
        cbLopFilter = new JComboBox<>(new String[]{"Tất cả"});
        cbLopFilter.setFont(UIUtils.FONT_NORMAL);
        cbLopFilter.setBackground(Color.WHITE);
        cbLopFilter.setPreferredSize(new Dimension(130, 30));
        filterBox.add(lblFilter);
        filterBox.add(cbLopFilter);
        tableHeader.add(filterBox, BorderLayout.EAST);

        String[] cols = {"Mã SV", "Họ Tên", "Giới Tính", "Ngày Sinh", "SĐT", "Email", "Lớp", "Ngành", "Trạng Thái"};
        modelSV = new DefaultTableModel(cols, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblSV = new JTable(modelSV);
        UIUtils.styleTable(tblSV);
        tblSV.setShowGrid(true);
        tblSV.setGridColor(UIUtils.BORDER);
        tblSV.setIntercellSpacing(new Dimension(1, 1));
        sorterSV = new TableRowSorter<>(modelSV);
        tblSV.setRowSorter(sorterSV);

        ZebraRenderer zebra = new ZebraRenderer();
        for (int i = 0; i < tblSV.getColumnCount(); i++) tblSV.getColumnModel().getColumn(i).setCellRenderer(zebra);

        JScrollPane scroll = new JScrollPane(tblSV);
        scroll.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        tableWrapper.add(scroll, BorderLayout.CENTER);

        cbLopFilter.addActionListener(e -> {
            String chon = (String) cbLopFilter.getSelectedItem();
            if (chon == null || chon.equals("Tất cả")) sorterSV.setRowFilter(null);
            else sorterSV.setRowFilter(RowFilter.regexFilter("^" + Pattern.quote(chon) + "$", 6));
        });

        // --- 2. KHUNG NHẬP LIỆU (card shell dùng chung) ---
        JPanel formWrapper = UIUtils.createCardShell("Thông Tin Chi Tiết Sinh Viên", UIUtils.MIT_RED);
        formWrapper.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel inputGrid = new JPanel(new GridLayout(2, 5, 15, 5));
        inputGrid.setOpaque(false);
        inputGrid.setBorder(new EmptyBorder(14, 20, 5, 20));

        txtMaSV = UIUtils.createInput();
        txtHoTenSV = UIUtils.createInput();
        cbGioiTinhSV = new JComboBox<>(new String[]{"Nam", "Nữ"}); cbGioiTinhSV.setFont(UIUtils.FONT_NORMAL); cbGioiTinhSV.setBackground(Color.WHITE);
        txtNgaySinhSV = UIUtils.createInput();
        txtSdtSV = UIUtils.createInput();
        txtEmailSV = UIUtils.createInput();
        cbTrangThaiSV = new JComboBox<>(new String[]{"Đang học", "Bảo lưu", "Đã tốt nghiệp", "Thôi học"}); cbTrangThaiSV.setFont(UIUtils.FONT_NORMAL); cbTrangThaiSV.setBackground(Color.WHITE);
        txtLopSV = UIUtils.createInput();
        txtNganhSV = UIUtils.createInput();

        inputGrid.add(createCompactFormRow("Mã Sinh Viên:", txtMaSV));
        inputGrid.add(createCompactFormRow("Họ và Tên:", txtHoTenSV));
        inputGrid.add(createCompactFormRow("Giới Tính:", cbGioiTinhSV));
        inputGrid.add(createCompactFormRow("Ngày Sinh:", txtNgaySinhSV));
        inputGrid.add(createCompactFormRow("Số Điện Thoại:", txtSdtSV));

        inputGrid.add(createCompactFormRow("Email:", txtEmailSV));
        inputGrid.add(createCompactFormRow("Trạng Thái:", cbTrangThaiSV));
        inputGrid.add(createCompactFormRow("Mã Lớp:", txtLopSV));
        inputGrid.add(createCompactFormRow("Mã CTĐT:", txtNganhSV));
        inputGrid.add(new JLabel(""));

        // --- 3. NÚT HÀNH ĐỘNG (đồng bộ palette chung UIUtils, không tự chế màu riêng) ---
        JPanel btnGrid = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnGrid.setOpaque(false);
        btnGrid.setBorder(new EmptyBorder(4, 20, 16, 20));

        JButton btnClear = UIUtils.createSecondaryBtn("Làm mới");
        JButton btnAdd = UIUtils.createPrimaryBtn("Thêm mới");
        JButton btnUpdate = UIUtils.createPrimaryBtn("Cập nhật");
        JButton btnDel = UIUtils.createDangerBtn("Xóa bỏ");
        Dimension btnSize = new Dimension(120, 38);
        for (JButton b : new JButton[]{btnClear, btnAdd, btnUpdate, btnDel}) b.setPreferredSize(btnSize);

        btnGrid.add(btnClear); btnGrid.add(btnAdd); btnGrid.add(btnUpdate); btnGrid.add(btnDel);

        JPanel formContent = new JPanel(new BorderLayout());
        formContent.setOpaque(false);
        formContent.add(inputGrid, BorderLayout.CENTER);
        formContent.add(btnGrid, BorderLayout.SOUTH);
        formWrapper.add(formContent, BorderLayout.CENTER);

        // --- GẮN SỰ KIỆN CỦA BẢNG VÀ NÚT BẤM ---
        tblSV.getSelectionModel().addListSelectionListener(e -> {
            int viewRow = tblSV.getSelectedRow();
            if (viewRow >= 0 && !e.getValueIsAdjusting()) {
                int r = tblSV.convertRowIndexToModel(viewRow);
                txtMaSV.setText(modelSV.getValueAt(r, 0) != null ? modelSV.getValueAt(r, 0).toString() : "");
                txtHoTenSV.setText(modelSV.getValueAt(r, 1) != null ? modelSV.getValueAt(r, 1).toString() : "");
                cbGioiTinhSV.setSelectedItem(modelSV.getValueAt(r, 2) != null ? modelSV.getValueAt(r, 2).toString() : "Nam");
                txtNgaySinhSV.setText(modelSV.getValueAt(r, 3) != null ? modelSV.getValueAt(r, 3).toString() : "");
                txtSdtSV.setText(modelSV.getValueAt(r, 4) != null ? modelSV.getValueAt(r, 4).toString() : "");
                txtEmailSV.setText(modelSV.getValueAt(r, 5) != null ? modelSV.getValueAt(r, 5).toString() : "");
                txtLopSV.setText(modelSV.getValueAt(r, 6) != null ? modelSV.getValueAt(r, 6).toString() : "");
                txtNganhSV.setText(modelSV.getValueAt(r, 7) != null ? modelSV.getValueAt(r, 7).toString() : "");
                cbTrangThaiSV.setSelectedItem(modelSV.getValueAt(r, 8) != null ? modelSV.getValueAt(r, 8).toString() : "Đang học");
            }
        });

        btnClear.addActionListener(e -> clearFormSV());

        btnAdd.addActionListener(e -> {
            if (!validateFormSV(true)) return;
            java.sql.Date ngaySinh = parseNgaySinh(txtNgaySinhSV.getText());
            if (ngaySinh == null) return;
            String sql = "INSERT INTO SINH_VIEN (MaSV, HoTen, GioiTinh, NgaySinh, SoDienThoai, Email, TrangThaiHocTap, MaCTDT, MaLop, DatChuanNgoaiNgu) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
            executeDB(sql, "Thêm sinh viên", txtMaSV.getText().trim(), txtHoTenSV.getText().trim(), cbGioiTinhSV.getSelectedItem(), ngaySinh, txtSdtSV.getText().trim(), txtEmailSV.getText().trim(), cbTrangThaiSV.getSelectedItem(), txtNganhSV.getText().trim(), txtLopSV.getText().trim());
            loadDataSV();
        });

        btnUpdate.addActionListener(e -> {
            if (!validateFormSV(false)) return;
            java.sql.Date ngaySinh = parseNgaySinh(txtNgaySinhSV.getText());
            if (ngaySinh == null) return;
            String sql = "UPDATE SINH_VIEN SET HoTen=?, GioiTinh=?, NgaySinh=?, SoDienThoai=?, Email=?, TrangThaiHocTap=?, MaCTDT=?, MaLop=? WHERE MaSV=?";
            executeDB(sql, "Cập nhật sinh viên", txtHoTenSV.getText().trim(), cbGioiTinhSV.getSelectedItem(), ngaySinh, txtSdtSV.getText().trim(), txtEmailSV.getText().trim(), cbTrangThaiSV.getSelectedItem(), txtNganhSV.getText().trim(), txtLopSV.getText().trim(), txtMaSV.getText().trim());
            loadDataSV();
        });

        btnDel.addActionListener(e -> {
            String maSV = txtMaSV.getText().trim();
            if (maSV.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng chọn Sinh viên cần xóa.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE); return; }

            int soDangKy = demSoBanGhiLienQuan("KET_QUA_DANG_KY", "MaSV", maSV);
            int soCongNo = demSoBanGhiLienQuan("CONG_NO_HOC_PHI", "MaSV", maSV);
            if (soDangKy > 0 || soCongNo > 0) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "Sinh viên này đang có " + soDangKy + " bản ghi đăng ký học phần và " + soCongNo +
                    " phiếu công nợ.\nXóa sẽ mất toàn bộ lịch sử học tập/tài chính của SV này.\n" +
                    "Nên đổi Trạng Thái sang \"Thôi học\" thay vì xóa.\n\nBạn vẫn muốn XÓA VĨNH VIỄN chứ?",
                    "Cảnh báo - Có dữ liệu liên quan", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) return;
            } else if (JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa Sinh viên này?", "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            String sql = "DELETE FROM SINH_VIEN WHERE MaSV=?";
            executeDB(sql, "Xóa sinh viên", maSV);
            loadDataSV();
            clearFormSV();
        });

        pnl.add(tableWrapper, BorderLayout.CENTER);
        pnl.add(formWrapper, BorderLayout.SOUTH);
        return pnl;
    }

    // ========================================================
    // VIEW 2: QUẢN LÝ GIẢNG VIÊN
    // ========================================================
    private JPanel createGiangVienView() {
        JPanel pnl = new JPanel(new BorderLayout(0, 15));
        pnl.setBackground(UIUtils.BG_APP);

        JPanel tableWrapper = UIUtils.createCardShell("Bảng Danh Sách Giảng Viên Khoa", UIUtils.MIT_ORANGE);

        JPanel tableHeader = (JPanel) tableWrapper.getComponent(0);
        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterBox.setOpaque(false);
        JLabel lblFilter = new JLabel("Lọc theo khoa:");
        lblFilter.setFont(UIUtils.FONT_NORMAL);
        lblFilter.setForeground(UIUtils.TEXT_MUTED);
        cbKhoaFilter = new JComboBox<>(new String[]{"Tất cả"});
        cbKhoaFilter.setFont(UIUtils.FONT_NORMAL);
        cbKhoaFilter.setBackground(Color.WHITE);
        cbKhoaFilter.setPreferredSize(new Dimension(130, 30));
        filterBox.add(lblFilter);
        filterBox.add(cbKhoaFilter);
        tableHeader.add(filterBox, BorderLayout.EAST);

        String[] cols = {"Mã GV", "Họ Tên", "Giới Tính", "Học Vị", "SĐT", "Email", "Mã Khoa"};
        modelGV = new DefaultTableModel(cols, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblGV = new JTable(modelGV);
        UIUtils.styleTable(tblGV);
        tblGV.setShowGrid(true);
        tblGV.setGridColor(UIUtils.BORDER);
        tblGV.setIntercellSpacing(new Dimension(1, 1));
        sorterGV = new TableRowSorter<>(modelGV);
        tblGV.setRowSorter(sorterGV);

        ZebraRenderer zebra = new ZebraRenderer();
        for (int i = 0; i < tblGV.getColumnCount(); i++) tblGV.getColumnModel().getColumn(i).setCellRenderer(zebra);

        JScrollPane scroll = new JScrollPane(tblGV);
        scroll.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        tableWrapper.add(scroll, BorderLayout.CENTER);

        cbKhoaFilter.addActionListener(e -> {
            String chon = (String) cbKhoaFilter.getSelectedItem();
            if (chon == null || chon.equals("Tất cả")) sorterGV.setRowFilter(null);
            else sorterGV.setRowFilter(RowFilter.regexFilter("^" + Pattern.quote(chon) + "$", 6));
        });

        JPanel formWrapper = UIUtils.createCardShell("Thông Tin Chi Tiết Giảng Viên", UIUtils.MIT_ORANGE);
        formWrapper.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel inputGrid = new JPanel(new GridLayout(2, 4, 15, 5));
        inputGrid.setOpaque(false);
        inputGrid.setBorder(new EmptyBorder(14, 20, 5, 20));

        txtMaGV = UIUtils.createInput();
        txtHoTenGV = UIUtils.createInput();
        cbGioiTinhGV = new JComboBox<>(new String[]{"Nam", "Nữ"}); cbGioiTinhGV.setFont(UIUtils.FONT_NORMAL); cbGioiTinhGV.setBackground(Color.WHITE);
        cbHocViGV = new JComboBox<>(new String[]{"Cử nhân", "Thạc sĩ", "Tiến sĩ", "PGS.TS", "GS.TS"}); cbHocViGV.setFont(UIUtils.FONT_NORMAL); cbHocViGV.setBackground(Color.WHITE);
        txtSdtGV = UIUtils.createInput();
        txtEmailGV = UIUtils.createInput();
        txtKhoaGV = UIUtils.createInput();

        inputGrid.add(createCompactFormRow("Mã Giảng Viên:", txtMaGV));
        inputGrid.add(createCompactFormRow("Họ và Tên:", txtHoTenGV));
        inputGrid.add(createCompactFormRow("Giới Tính:", cbGioiTinhGV));
        inputGrid.add(createCompactFormRow("Học Vị:", cbHocViGV));

        inputGrid.add(createCompactFormRow("Số Điện Thoại:", txtSdtGV));
        inputGrid.add(createCompactFormRow("Email:", txtEmailGV));
        inputGrid.add(createCompactFormRow("Mã Khoa:", txtKhoaGV));
        inputGrid.add(new JLabel(""));

        JPanel btnGrid = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnGrid.setOpaque(false);
        btnGrid.setBorder(new EmptyBorder(4, 20, 16, 20));

        JButton btnClear = UIUtils.createSecondaryBtn("Làm mới");
        JButton btnAdd = UIUtils.createPrimaryBtn("Thêm mới");
        JButton btnUpdate = UIUtils.createPrimaryBtn("Cập nhật");
        JButton btnDel = UIUtils.createDangerBtn("Xóa bỏ");
        Dimension btnSize = new Dimension(120, 38);
        for (JButton b : new JButton[]{btnClear, btnAdd, btnUpdate, btnDel}) b.setPreferredSize(btnSize);

        btnGrid.add(btnClear); btnGrid.add(btnAdd); btnGrid.add(btnUpdate); btnGrid.add(btnDel);

        JPanel formContent = new JPanel(new BorderLayout());
        formContent.setOpaque(false);
        formContent.add(inputGrid, BorderLayout.CENTER);
        formContent.add(btnGrid, BorderLayout.SOUTH);
        formWrapper.add(formContent, BorderLayout.CENTER);

        tblGV.getSelectionModel().addListSelectionListener(e -> {
            int viewRow = tblGV.getSelectedRow();
            if (viewRow >= 0 && !e.getValueIsAdjusting()) {
                int r = tblGV.convertRowIndexToModel(viewRow);
                txtMaGV.setText(modelGV.getValueAt(r, 0) != null ? modelGV.getValueAt(r, 0).toString() : "");
                txtHoTenGV.setText(modelGV.getValueAt(r, 1) != null ? modelGV.getValueAt(r, 1).toString() : "");
                cbGioiTinhGV.setSelectedItem(modelGV.getValueAt(r, 2) != null ? modelGV.getValueAt(r, 2).toString() : "Nam");
                cbHocViGV.setSelectedItem(modelGV.getValueAt(r, 3) != null ? modelGV.getValueAt(r, 3).toString() : "Thạc sĩ");
                txtSdtGV.setText(modelGV.getValueAt(r, 4) != null ? modelGV.getValueAt(r, 4).toString() : "");
                txtEmailGV.setText(modelGV.getValueAt(r, 5) != null ? modelGV.getValueAt(r, 5).toString() : "");
                txtKhoaGV.setText(modelGV.getValueAt(r, 6) != null ? modelGV.getValueAt(r, 6).toString() : "");
            }
        });

        btnClear.addActionListener(e -> clearFormGV());

        btnAdd.addActionListener(e -> {
            if (!validateFormGV()) return;
            String sql = "INSERT INTO GIANG_VIEN (MaGV, HoTen, GioiTinh, HocVi, SoDienThoai, Email, MaKhoa) VALUES (?, ?, ?, ?, ?, ?, ?)";
            executeDB(sql, "Thêm giảng viên", txtMaGV.getText().trim(), txtHoTenGV.getText().trim(), cbGioiTinhGV.getSelectedItem(), cbHocViGV.getSelectedItem(), txtSdtGV.getText().trim(), txtEmailGV.getText().trim(), txtKhoaGV.getText().trim());
            loadDataGV();
        });

        btnUpdate.addActionListener(e -> {
            if (!validateFormGV()) return;
            String sql = "UPDATE GIANG_VIEN SET HoTen=?, GioiTinh=?, HocVi=?, SoDienThoai=?, Email=?, MaKhoa=? WHERE MaGV=?";
            executeDB(sql, "Cập nhật giảng viên", txtHoTenGV.getText().trim(), cbGioiTinhGV.getSelectedItem(), cbHocViGV.getSelectedItem(), txtSdtGV.getText().trim(), txtEmailGV.getText().trim(), txtKhoaGV.getText().trim(), txtMaGV.getText().trim());
            loadDataGV();
        });

        btnDel.addActionListener(e -> {
            String maGV = txtMaGV.getText().trim();
            if (maGV.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng chọn Giảng viên cần xóa.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE); return; }

            int soLop = demSoBanGhiLienQuan("LOP_HOC_PHAN", "MaGV", maGV);
            if (soLop > 0) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "Giảng viên này đang được phân công dạy " + soLop + " lớp học phần.\n" +
                    "Xóa sẽ làm các lớp đó mất thông tin giảng viên trên Thời Khóa Biểu của sinh viên.\n\n" +
                    "Bạn vẫn muốn XÓA chứ?",
                    "Cảnh báo - GV đang dạy", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) return;
            } else if (JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa Giảng viên này?", "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            String sql = "DELETE FROM GIANG_VIEN WHERE MaGV=?";
            executeDB(sql, "Xóa giảng viên", maGV);
            loadDataGV();
            clearFormGV();
        });

        pnl.add(tableWrapper, BorderLayout.CENTER);
        pnl.add(formWrapper, BorderLayout.SOUTH);
        return pnl;
    }

    // ==========================================
    // LOGIC DATABASE
    // ==========================================
    private void loadDataSV() {
        modelSV.setRowCount(0);
        String sql = "SELECT MaSV, HoTen, GioiTinh, NgaySinh, SoDienThoai, Email, MaLop, MaCTDT, TrangThaiHocTap FROM SINH_VIEN";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            LinkedHashSet<String> lopList = new LinkedHashSet<>();
            while (rs.next()) {
                String maLop = rs.getString("MaLop");
                modelSV.addRow(new Object[]{
                    rs.getString("MaSV"), rs.getString("HoTen"), rs.getString("GioiTinh"), rs.getString("NgaySinh"),
                    rs.getString("SoDienThoai"), rs.getString("Email"), maLop, rs.getString("MaCTDT"), rs.getString("TrangThaiHocTap")
                });
                if (maLop != null && !maLop.isEmpty()) lopList.add(maLop);
            }
            capNhatDanhSachLoc(cbLopFilter, lopList);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadDataGV() {
        modelGV.setRowCount(0);
        String sql = "SELECT MaGV, HoTen, GioiTinh, HocVi, SoDienThoai, Email, MaKhoa FROM GIANG_VIEN";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            LinkedHashSet<String> khoaList = new LinkedHashSet<>();
            while (rs.next()) {
                String maKhoa = rs.getString("MaKhoa");
                modelGV.addRow(new Object[]{
                    rs.getString("MaGV"), rs.getString("HoTen"), rs.getString("GioiTinh"), rs.getString("HocVi"),
                    rs.getString("SoDienThoai"), rs.getString("Email"), maKhoa
                });
                if (maKhoa != null && !maKhoa.isEmpty()) khoaList.add(maKhoa);
            }
            capNhatDanhSachLoc(cbKhoaFilter, khoaList);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Nap lai danh sach gia tri cho combo loc (Lop/Khoa) tu chinh du lieu vua tai, giu nguyen
    // lua chon dang loc neu no van con ton tai, tranh giat combo ve "Tat ca" moi lan F5.
    private void capNhatDanhSachLoc(JComboBox<String> combo, LinkedHashSet<String> values) {
        if (combo == null) return;
        Object dangChon = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem("Tất cả");
        for (String v : values) combo.addItem(v);
        if (dangChon != null) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (combo.getItemAt(i).equals(dangChon)) { combo.setSelectedIndex(i); return; }
            }
        }
        combo.setSelectedIndex(0);
    }

    private void executeDB(String sql, String actionName, Object... params) {
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, actionName + " thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi " + actionName + ": " + e.getMessage(), "Lỗi Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateFormSV(boolean laThemMoi) {
        if (txtMaSV.getText().trim().isEmpty()) {
            canhBao("Vui lòng nhập Mã Sinh Viên."); return false;
        }
        if (txtHoTenSV.getText().trim().isEmpty()) {
            canhBao("Vui lòng nhập Họ và Tên."); return false;
        }
        String email = txtEmailSV.getText().trim();
        if (!email.isEmpty() && !Pattern.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$", email)) {
            canhBao("Email không đúng định dạng (vd: ten@gmail.com)."); return false;
        }
        String sdt = txtSdtSV.getText().trim();
        if (!sdt.isEmpty() && !Pattern.matches("^[0-9]{9,11}$", sdt)) {
            canhBao("Số điện thoại phải gồm 9-11 chữ số."); return false;
        }
        return true;
    }

    private boolean validateFormGV() {
        if (txtMaGV.getText().trim().isEmpty()) {
            canhBao("Vui lòng nhập Mã Giảng Viên."); return false;
        }
        if (txtHoTenGV.getText().trim().isEmpty()) {
            canhBao("Vui lòng nhập Họ và Tên."); return false;
        }
        String email = txtEmailGV.getText().trim();
        if (!email.isEmpty() && !Pattern.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$", email)) {
            canhBao("Email không đúng định dạng (vd: ten@gmail.com)."); return false;
        }
        String sdt = txtSdtGV.getText().trim();
        if (!sdt.isEmpty() && !Pattern.matches("^[0-9]{9,11}$", sdt)) {
            canhBao("Số điện thoại phải gồm 9-11 chữ số."); return false;
        }
        return true;
    }

    private void canhBao(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    private java.sql.Date parseNgaySinh(String text) {
        String s = text == null ? "" : text.trim();
        if (s.isEmpty()) { canhBao("Vui lòng nhập Ngày Sinh (dd/MM/yyyy)."); return null; }
        try {
            LocalDate ld;
            if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
                ld = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                ld = LocalDate.parse(s, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            if (ld.isAfter(LocalDate.now())) { canhBao("Ngày sinh không thể ở tương lai."); return null; }
            return java.sql.Date.valueOf(ld);
        } catch (Exception ex) {
            canhBao("Ngày Sinh sai định dạng. Nhập theo dạng dd/MM/yyyy (vd: 12/07/2006).");
            return null;
        }
    }

    private int demSoBanGhiLienQuan(String bang, String cotKhoa, String giaTri) {
        String sql = "SELECT COUNT(*) FROM " + bang + " WHERE " + cotKhoa + " = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, giaTri);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private void clearFormSV() {
        txtMaSV.setText(""); txtHoTenSV.setText(""); txtNgaySinhSV.setText(""); txtSdtSV.setText("");
        txtEmailSV.setText(""); txtLopSV.setText(""); txtNganhSV.setText("");
        cbGioiTinhSV.setSelectedIndex(0); cbTrangThaiSV.setSelectedIndex(0);
        tblSV.clearSelection();
    }

    private void clearFormGV() {
        txtMaGV.setText(""); txtHoTenGV.setText(""); txtSdtGV.setText(""); txtEmailGV.setText(""); txtKhoaGV.setText("");
        cbGioiTinhGV.setSelectedIndex(0); cbHocViGV.setSelectedIndex(0);
        tblGV.clearSelection();
    }

    // ==========================================
    // UI HELPERS RIÊNG CHO PANEL NÀY (toggle tab, dòng form gọn)
    // ==========================================
    private JPanel createCompactFormRow(String labelText, JComponent input) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 5, 0));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(71, 85, 105));
        lbl.setBorder(new EmptyBorder(0, 0, 3, 0));

        wrapper.add(lbl, BorderLayout.NORTH);
        wrapper.add(input, BorderLayout.CENTER);
        return wrapper;
    }

    private JButton createToggleBtn(String text, boolean isActive) {
        JButton btn = new JButton(text);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        setToggleStyle(btn, isActive);
        return btn;
    }

    private void setToggleStyle(JButton btn, boolean isActive) {
        if (isActive) {
            btn.setBackground(UIUtils.MIT_RED);
            btn.setForeground(Color.WHITE);
            btn.setBorder(new EmptyBorder(12, 30, 12, 30));
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(UIUtils.TEXT_MUTED);
            btn.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, UIUtils.BORDER), new EmptyBorder(11, 29, 11, 29)
            ));
        }
    }

    class ZebraRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (column == 8 && value != null) {
                String text = value.toString();
                int type = text.equals("Đang học") ? UIUtils.BADGE_OK
                         : text.equals("Bảo lưu") ? UIUtils.BADGE_WARN
                         : text.equals("Đã tốt nghiệp") ? UIUtils.BADGE_PENDING
                         : UIUtils.BADGE_BAD;
                JLabel badge = UIUtils.createBadge(text, type);
                JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                wrap.setBackground(isSelected ? UIUtils.MIT_RED_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
                wrap.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(6, 15, 6, 15)
                ));
                wrap.add(badge);
                return wrap;
            }

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
            } else {
                c.setBackground(UIUtils.MIT_RED_LIGHT);
            }
            c.setForeground(UIUtils.TEXT_MAIN);
            setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(0, 15, 0, 15)
            ));
            return c;
        }
    }
}
