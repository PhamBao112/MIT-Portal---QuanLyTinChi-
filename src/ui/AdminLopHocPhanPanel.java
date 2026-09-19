package ui;

import utils.UIUtils;
import config.DBConnect;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminLopHocPhanPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private String currentMaHK = "";
    private DefaultTableModel model;
    private JTable table;

    private JTextField txtMaLHP, txtMonHoc, txtHK, txtPhong, txtSucChua, txtMaGV;
    private JComboBox<String> cbThu, cbTiet;
    private JCheckBox chkChuaXepLich;

    public AdminLopHocPhanPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(5, 0, 0, 0));

        // ========================================================
        // 1. BẢNG DANH SÁCH LỚP HỌC PHẦN (card shell dùng chung)
        // ========================================================
        JPanel tableWrapper = UIUtils.createCardShell("Bảng Danh Sách Lớp Học Phần Mở Trong Kỳ", UIUtils.MIT_ORANGE);

        String[] cols = {"Mã LHP", "Mã Môn", "Tên Môn", "Trạng thái", "Thứ", "Tiết", "Phòng", "Sức chứa", "Đã ĐK", "Mã GV"};
        model = new DefaultTableModel(cols, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIUtils.styleTable(table);
        table.setShowGrid(true);
        table.setGridColor(UIUtils.BORDER);
        table.setIntercellSpacing(new Dimension(1, 1));

        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);

        ZebraRenderer zebra = new ZebraRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        tableWrapper.add(scroll, BorderLayout.CENTER);

        // ========================================================
        // 2. KHUNG NHẬP LIỆU (card shell dùng chung)
        // ========================================================
        JPanel formWrapper = UIUtils.createCardShell("Thông Tin Chi Tiết & Điều Chỉnh Lịch Học", UIUtils.MIT_ORANGE);
        formWrapper.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel inputGrid = new JPanel(new GridLayout(3, 4, 15, 5));
        inputGrid.setOpaque(false);
        inputGrid.setBorder(new EmptyBorder(14, 20, 5, 20));

        txtMaLHP = UIUtils.createInput();
        txtMonHoc = UIUtils.createInput();
        txtHK = UIUtils.createInput();
        cbThu = new JComboBox<>(new String[]{"2", "3", "4", "5", "6", "7"}); cbThu.setFont(UIUtils.FONT_NORMAL); cbThu.setBackground(Color.WHITE); // Bo "Chu Nhat": LichHocPanel chi ve luoi Thu 2-7, chon Chu Nhat se bi an mat khoi TKB sinh vien
        cbTiet = new JComboBox<>(new String[]{"1-3", "1-4", "4-6", "7-9", "7-10", "10-12"}); cbTiet.setFont(UIUtils.FONT_NORMAL); cbTiet.setBackground(Color.WHITE);
        txtPhong = UIUtils.createInput();
        txtSucChua = UIUtils.createInput();
        txtMaGV = UIUtils.createInput();

        // FIX (yeu cau moi): cho phep mo lop TRUOC, xep lich SAU (~10 ngay) - dung nhu quy
        // trinh Bao mo ta. Khi tick, an cac o Thu/Tiet/Phong vi chua co gia tri.
        chkChuaXepLich = new JCheckBox("Lớp này CHƯA xếp lịch (chờ phân lớp sau)");
        chkChuaXepLich.setFont(UIUtils.FONT_NORMAL);
        chkChuaXepLich.setOpaque(false);
        chkChuaXepLich.addActionListener(e -> {
            boolean chuaXep = chkChuaXepLich.isSelected();
            cbThu.setEnabled(!chuaXep);
            cbTiet.setEnabled(!chuaXep);
            txtPhong.setEnabled(!chuaXep);
        });

        inputGrid.add(createCompactFormRow("Mã Lớp Học Phần:", txtMaLHP));
        inputGrid.add(createCompactFormRow("Mã Môn Học:", txtMonHoc));
        inputGrid.add(createCompactFormRow("Mã Học Kỳ:", txtHK));
        inputGrid.add(createCompactFormRow("Học Vào Thứ:", cbThu));

        inputGrid.add(createCompactFormRow("Ca / Tiết Học:", cbTiet));
        inputGrid.add(createCompactFormRow("Phòng Học:", txtPhong));
        inputGrid.add(createCompactFormRow("Sức Chứa Tối Đa:", txtSucChua));
        inputGrid.add(createCompactFormRow("Mã Giảng Viên (bắt buộc):", txtMaGV));

        inputGrid.add(chkChuaXepLich);
        inputGrid.add(new JLabel(""));
        inputGrid.add(new JLabel(""));
        inputGrid.add(new JLabel(""));

        // ========================================================
        // 3. NÚT HÀNH ĐỘNG (đồng bộ palette chung UIUtils)
        // ========================================================
        JPanel btnGrid = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnGrid.setOpaque(false);
        btnGrid.setBorder(new EmptyBorder(4, 20, 16, 20));

        JButton btnClear = UIUtils.createSecondaryBtn("Làm mới");
        JButton btnAdd = UIUtils.createPrimaryBtn("Mở lớp mới");
        JButton btnUpdate = UIUtils.createPrimaryBtn("Lưu lịch học");
        JButton btnDel = UIUtils.createDangerBtn("Hủy lớp");
        Dimension btnSize = new Dimension(130, 38);
        for (JButton b : new JButton[]{btnClear, btnAdd, btnUpdate, btnDel}) b.setPreferredSize(btnSize);

        btnGrid.add(btnClear); btnGrid.add(btnAdd); btnGrid.add(btnUpdate); btnGrid.add(btnDel);

        JPanel formContent = new JPanel(new BorderLayout());
        formContent.setOpaque(false);
        formContent.add(inputGrid, BorderLayout.CENTER);
        formContent.add(btnGrid, BorderLayout.SOUTH);
        formWrapper.add(formContent, BorderLayout.CENTER);

        // ========================================================
        // 4. GẮN SỰ KIỆN TƯƠNG TÁC
        // ========================================================
        table.getSelectionModel().addListSelectionListener(e -> {
            int r = table.getSelectedRow();
            if(r >= 0 && !e.getValueIsAdjusting()) {
                txtMaLHP.setText(model.getValueAt(r, 0) != null ? model.getValueAt(r, 0).toString() : "");
                txtMonHoc.setText(model.getValueAt(r, 1) != null ? model.getValueAt(r, 1).toString() : "");
                txtHK.setText(currentMaHK);
                boolean chuaXep = "Chưa xếp lịch".equals(model.getValueAt(r, 3));
                chkChuaXepLich.setSelected(chuaXep);
                cbThu.setEnabled(!chuaXep);
                cbTiet.setEnabled(!chuaXep);
                txtPhong.setEnabled(!chuaXep);
                cbThu.setSelectedItem(model.getValueAt(r, 4) != null ? model.getValueAt(r, 4).toString() : "2");
                cbTiet.setSelectedItem(model.getValueAt(r, 5) != null ? model.getValueAt(r, 5).toString() : "1-3");
                txtPhong.setText(model.getValueAt(r, 6) != null ? model.getValueAt(r, 6).toString() : "");
                txtSucChua.setText(model.getValueAt(r, 7) != null ? model.getValueAt(r, 7).toString() : "");
                txtMaGV.setText(model.getValueAt(r, 9) != null ? model.getValueAt(r, 9).toString() : "");
            }
        });

        btnClear.addActionListener(e -> clearForm());

        btnAdd.addActionListener(e -> {
            if (txtMaLHP.getText().isBlank() || txtMonHoc.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Mã Lớp học phần và Mã Môn học không được để trống!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (txtMaGV.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Bắt buộc phải nhập Mã Giảng Viên!\nNếu để trống, lớp học phần sẽ KHÔNG hiển thị trong trang Đăng Ký của sinh viên.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean chuaXep = chkChuaXepLich.isSelected();
            String maHK = txtHK.getText().trim();
            String sql;
            if (chuaXep) {
                sql = "INSERT INTO LOP_HOC_PHAN (MaLHP, MaMon, MaHK, Thu, TietHoc, PhongHoc, SucChua, MaGV, " +
                      "TrangThaiXepLich, NgayBatDauHoc, NgayKetThucHoc) VALUES (?, ?, ?, NULL, NULL, NULL, ?, ?, N'Chưa xếp lịch', NULL, NULL)";
                executeDB(sql, "Mở lớp học phần mới (chưa xếp lịch)", txtMaLHP.getText().trim(), txtMonHoc.getText().trim(), maHK, txtSucChua.getText().trim(), txtMaGV.getText().trim());
            } else {
                java.sql.Date[] cuaSo = tinhCuaSoHoc(maHK);
                sql = "INSERT INTO LOP_HOC_PHAN (MaLHP, MaMon, MaHK, Thu, TietHoc, PhongHoc, SucChua, MaGV, " +
                      "TrangThaiXepLich, NgayBatDauHoc, NgayKetThucHoc) VALUES (?, ?, ?, ?, ?, ?, ?, ?, N'Đã xếp lịch', ?, ?)";
                executeDB(sql, "Mở lớp học phần mới", txtMaLHP.getText().trim(), txtMonHoc.getText().trim(), maHK, cbThu.getSelectedItem(), cbTiet.getSelectedItem(), txtPhong.getText().trim(), txtSucChua.getText().trim(), txtMaGV.getText().trim(), cuaSo[0], cuaSo[1]);
            }
            updateData(currentMaHK);
        });

        btnUpdate.addActionListener(e -> {
            if (txtMaLHP.getText().isBlank()) return;
            if (txtMaGV.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Bắt buộc phải nhập Mã Giảng Viên!\nNếu để trống, lớp học phần sẽ KHÔNG hiển thị trong trang Đăng Ký của sinh viên.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean chuaXep = chkChuaXepLich.isSelected();
            String maHK = txtHK.getText().trim();
            String sql;
            if (chuaXep) {
                sql = "UPDATE LOP_HOC_PHAN SET MaMon=?, MaHK=?, Thu=NULL, TietHoc=NULL, PhongHoc=NULL, SucChua=?, MaGV=?, " +
                      "TrangThaiXepLich=N'Chưa xếp lịch', NgayBatDauHoc=NULL, NgayKetThucHoc=NULL WHERE MaLHP=?";
                executeDB(sql, "Cập nhật lớp (chuyển về chưa xếp lịch)", txtMonHoc.getText().trim(), maHK, txtSucChua.getText().trim(), txtMaGV.getText().trim(), txtMaLHP.getText().trim());
            } else {
                java.sql.Date[] cuaSo = tinhCuaSoHoc(maHK);
                sql = "UPDATE LOP_HOC_PHAN SET MaMon=?, MaHK=?, Thu=?, TietHoc=?, PhongHoc=?, SucChua=?, MaGV=?, " +
                      "TrangThaiXepLich=N'Đã xếp lịch', NgayBatDauHoc=?, NgayKetThucHoc=? WHERE MaLHP=?";
                executeDB(sql, "Cập nhật lịch học", txtMonHoc.getText().trim(), maHK, cbThu.getSelectedItem(), cbTiet.getSelectedItem(), txtPhong.getText().trim(), txtSucChua.getText().trim(), txtMaGV.getText().trim(), cuaSo[0], cuaSo[1], txtMaLHP.getText().trim());
            }
            updateData(currentMaHK);
        });

        btnDel.addActionListener(e -> {
            if (txtMaLHP.getText().isBlank()) return;
            if(JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn hủy Lớp Học Phần này?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                String sql = "DELETE FROM LOP_HOC_PHAN WHERE MaLHP=?";
                executeDB(sql, "Hủy lớp học phần", txtMaLHP.getText().trim());
                updateData(currentMaHK);
                clearForm();
            }
        });

        add(tableWrapper, BorderLayout.CENTER);
        add(formWrapper, BorderLayout.SOUTH);
    }

    private java.sql.Date[] tinhCuaSoHoc(String maHK) {
        java.sql.Date ngayBatDauHK = null;
        String sql = "SELECT NgayBatDau FROM HOC_KY WHERE MaHK = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maHK);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) ngayBatDauHK = rs.getDate("NgayBatDau");
        } catch (Exception e) { e.printStackTrace(); }
        if (ngayBatDauHK == null) ngayBatDauHK = java.sql.Date.valueOf(java.time.LocalDate.now());
        java.time.LocalDate ld = ngayBatDauHK.toLocalDate();
        java.sql.Date batDauHoc = java.sql.Date.valueOf(ld.plusDays(10));
        java.sql.Date ketThucHoc = java.sql.Date.valueOf(ld.plusDays(66));
        return new java.sql.Date[]{batDauHoc, ketThucHoc};
    }

    public void updateData(String maHK) {
        this.currentMaHK = maHK;
        txtHK.setText(maHK);
        model.setRowCount(0);

        String sql = "SELECT lhp.MaLHP, lhp.MaMon, m.TenMon, lhp.TrangThaiXepLich, lhp.Thu, lhp.TietHoc, lhp.PhongHoc, lhp.SucChua, lhp.MaGV, " +
                     "(SELECT COUNT(*) FROM KET_QUA_DANG_KY WHERE MaLHP = lhp.MaLHP) as DaDK " +
                     "FROM LOP_HOC_PHAN lhp JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE lhp.MaHK = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maHK);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("MaLHP"), rs.getString("MaMon"), rs.getString("TenMon"), rs.getString("TrangThaiXepLich"),
                    rs.getString("Thu"), rs.getString("TietHoc"), rs.getString("PhongHoc"), rs.getInt("SucChua"), rs.getInt("DaDK"),
                    rs.getString("MaGV")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void clearForm() {
        txtMaLHP.setText(""); txtMonHoc.setText(""); txtPhong.setText(""); txtSucChua.setText(""); txtMaGV.setText("");
        cbThu.setSelectedIndex(0); cbTiet.setSelectedIndex(0);
        chkChuaXepLich.setSelected(false);
        cbThu.setEnabled(true); cbTiet.setEnabled(true); txtPhong.setEnabled(true);
        table.clearSelection();
    }

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

    class ZebraRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (column == 3 && value != null) {
                String text = value.toString();
                int type = text.equals("Đã xếp lịch") ? UIUtils.BADGE_OK : UIUtils.BADGE_WARN;
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
