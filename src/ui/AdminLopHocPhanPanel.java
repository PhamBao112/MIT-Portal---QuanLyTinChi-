package ui;

import utils.UIUtils;
import config.DBConnect;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    public AdminLopHocPhanPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(5, 0, 0, 0));

        // ========================================================
        // 1. BẢNG DANH SÁCH LỚP HỌC PHẦN
        // ========================================================
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBackground(Color.WHITE);
        tableWrapper.setBorder(new LineBorder(UIUtils.BORDER, 1, true));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setBackground(Color.WHITE);
        tableHeader.setBorder(new EmptyBorder(12, 20, 12, 20));
        JLabel lblTblTitle = new JLabel("Bảng Danh Sách Lớp Học Phần Mở Trong Kỳ");
        lblTblTitle.setFont(UIUtils.FONT_TITLE);
        lblTblTitle.setForeground(UIUtils.TEXT_MAIN);
        tableHeader.add(lblTblTitle, BorderLayout.WEST);

        String[] cols = {"Mã LHP", "Mã Môn", "Tên Môn", "Thứ", "Tiết", "Phòng", "Sức chứa", "Đã ĐK", "Mã GV"};
        model = new DefaultTableModel(cols, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIUtils.styleTable(table);
        table.setShowGrid(true);
        table.setGridColor(UIUtils.BORDER);
        table.setIntercellSpacing(new Dimension(1, 1));
        
        // Căn chỉnh độ rộng cột cho đẹp
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        
        ZebraRenderer zebra = new ZebraRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));

        tableWrapper.add(tableHeader, BorderLayout.NORTH);
        tableWrapper.add(scroll, BorderLayout.CENTER);

        // ========================================================
        // 2. KHUNG NHẬP LIỆU THU GỌN (2 DÒNG)
        // ========================================================
        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setBackground(Color.WHITE);
        formWrapper.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.BORDER, 1, true), new EmptyBorder(10, 15, 10, 15)
        ));

        // Tiêu đề form với gạch chân màu Cam đậm
        JPanel formHeader = new JPanel(new BorderLayout());
        formHeader.setBackground(Color.WHITE);
        formHeader.setBorder(new MatteBorder(0, 0, 2, 0, new Color(230, 81, 0))); 
        JLabel lblFormTitle = new JLabel("THÔNG TIN CHI TIẾT & ĐIỀU CHỈNH LỊCH HỌC");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblFormTitle.setForeground(new Color(230, 81, 0));
        lblFormTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        formHeader.add(lblFormTitle, BorderLayout.WEST);

        // Lưới nhập liệu: 2 dòng x 4 cột = 8 ô
        JPanel inputGrid = new JPanel(new GridLayout(2, 4, 15, 5));
        inputGrid.setBackground(Color.WHITE);
        inputGrid.setBorder(new EmptyBorder(10, 0, 5, 0));
        
        txtMaLHP = UIUtils.createInput();
        txtMonHoc = UIUtils.createInput();
        txtHK = UIUtils.createInput();
        cbThu = new JComboBox<>(new String[]{"2", "3", "4", "5", "6", "7"}); cbThu.setFont(UIUtils.FONT_NORMAL); cbThu.setBackground(Color.WHITE); // Bo "Chu Nhat": LichHocPanel chi ve luoi Thu 2-7, chon Chu Nhat se bi an mat khoi TKB sinh vien
        cbTiet = new JComboBox<>(new String[]{"1-3", "1-4", "4-6", "7-9", "7-10", "10-12"}); cbTiet.setFont(UIUtils.FONT_NORMAL); cbTiet.setBackground(Color.WHITE);
        txtPhong = UIUtils.createInput();
        txtSucChua = UIUtils.createInput();
        txtMaGV = UIUtils.createInput();

        inputGrid.add(createCompactFormRow("Mã Lớp Học Phần:", txtMaLHP));
        inputGrid.add(createCompactFormRow("Mã Môn Học:", txtMonHoc));
        inputGrid.add(createCompactFormRow("Mã Học Kỳ:", txtHK));
        inputGrid.add(createCompactFormRow("Học Vào Thứ:", cbThu));

        inputGrid.add(createCompactFormRow("Ca / Tiết Học:", cbTiet));
        inputGrid.add(createCompactFormRow("Phòng Học:", txtPhong));
        inputGrid.add(createCompactFormRow("Sức Chứa Tối Đa:", txtSucChua));
        inputGrid.add(createCompactFormRow("Mã Giảng Viên (bắt buộc):", txtMaGV));

        // ========================================================
        // 3. CÁC NÚT HÀNH ĐỘNG MÀU SẮC ĐẬM ĐÀ
        // ========================================================
        JPanel btnGrid = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        btnGrid.setBackground(Color.WHITE);

        JButton btnClear = createActionButton("LÀM MỚI", new Color(108, 117, 125));
        JButton btnAdd = createActionButton("MỞ LỚP MỚI", new Color(25, 135, 84));
        JButton btnUpdate = createActionButton("LƯU LỊCH HỌC", new Color(13, 110, 253));
        JButton btnDel = createActionButton("HỦY LỚP", new Color(220, 53, 69));

        btnGrid.add(btnClear); btnGrid.add(btnAdd); btnGrid.add(btnUpdate); btnGrid.add(btnDel);

        JPanel formContent = new JPanel(new BorderLayout());
        formContent.setBackground(Color.WHITE);
        formContent.add(inputGrid, BorderLayout.CENTER);
        formContent.add(btnGrid, BorderLayout.SOUTH);

        formWrapper.add(formHeader, BorderLayout.NORTH);
        formWrapper.add(formContent, BorderLayout.CENTER);

        // ========================================================
        // 4. GẮN SỰ KIỆN TƯƠNG TÁC CHUẨN XÁC
        // ========================================================
        table.getSelectionModel().addListSelectionListener(e -> {
            int r = table.getSelectedRow();
            if(r >= 0 && !e.getValueIsAdjusting()) {
                txtMaLHP.setText(model.getValueAt(r, 0) != null ? model.getValueAt(r, 0).toString() : "");
                txtMonHoc.setText(model.getValueAt(r, 1) != null ? model.getValueAt(r, 1).toString() : "");
                txtHK.setText(currentMaHK); // Lấy từ biến hiện tại
                cbThu.setSelectedItem(model.getValueAt(r, 3) != null ? model.getValueAt(r, 3).toString() : "2");
                cbTiet.setSelectedItem(model.getValueAt(r, 4) != null ? model.getValueAt(r, 4).toString() : "1-3");
                txtPhong.setText(model.getValueAt(r, 5) != null ? model.getValueAt(r, 5).toString() : "");
                txtSucChua.setText(model.getValueAt(r, 6) != null ? model.getValueAt(r, 6).toString() : "");
                txtMaGV.setText(model.getValueAt(r, 8) != null ? model.getValueAt(r, 8).toString() : "");
            }
        });

        btnClear.addActionListener(e -> clearForm());

        btnAdd.addActionListener(e -> {
            if (txtMaLHP.getText().isBlank() || txtMonHoc.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Mã Lớp học phần và Mã Môn học không được để trống!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // FIX #1: bat buoc phai co MaGV, neu khong lop se KHONG hien trong tab Dang Ky cua sinh vien
            // (DangKyPanel dung INNER JOIN GIANG_VIEN nen MaGV = NULL se bi loai khoi ket qua)
            if (txtMaGV.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Bắt buộc phải nhập Mã Giảng Viên!\nNếu để trống, lớp học phần sẽ KHÔNG hiển thị trong trang Đăng Ký của sinh viên.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String sql = "INSERT INTO LOP_HOC_PHAN (MaLHP, MaMon, MaHK, Thu, TietHoc, PhongHoc, SucChua, MaGV) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            executeDB(sql, "Mở lớp học phần mới", txtMaLHP.getText().trim(), txtMonHoc.getText().trim(), txtHK.getText().trim(), cbThu.getSelectedItem(), cbTiet.getSelectedItem(), txtPhong.getText().trim(), txtSucChua.getText().trim(), txtMaGV.getText().trim());
            updateData(currentMaHK);
        });

        btnUpdate.addActionListener(e -> {
            if (txtMaLHP.getText().isBlank()) return;
            if (txtMaGV.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Bắt buộc phải nhập Mã Giảng Viên!\nNếu để trống, lớp học phần sẽ KHÔNG hiển thị trong trang Đăng Ký của sinh viên.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String sql = "UPDATE LOP_HOC_PHAN SET MaMon=?, MaHK=?, Thu=?, TietHoc=?, PhongHoc=?, SucChua=?, MaGV=? WHERE MaLHP=?";
            executeDB(sql, "Cập nhật lịch học", txtMonHoc.getText().trim(), txtHK.getText().trim(), cbThu.getSelectedItem(), cbTiet.getSelectedItem(), txtPhong.getText().trim(), txtSucChua.getText().trim(), txtMaGV.getText().trim(), txtMaLHP.getText().trim());
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

    // ==========================================
    // LOGIC DATABASE
    // ==========================================
    public void updateData(String maHK) {
        this.currentMaHK = maHK;
        txtHK.setText(maHK); // Cập nhật luôn ô text
        model.setRowCount(0);
        
        String sql = "SELECT lhp.MaLHP, lhp.MaMon, m.TenMon, lhp.Thu, lhp.TietHoc, lhp.PhongHoc, lhp.SucChua, lhp.MaGV, " +
                     "(SELECT COUNT(*) FROM KET_QUA_DANG_KY WHERE MaLHP = lhp.MaLHP) as DaDK " +
                     "FROM LOP_HOC_PHAN lhp JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE lhp.MaHK = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maHK);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("MaLHP"), rs.getString("MaMon"), rs.getString("TenMon"), rs.getString("Thu"),
                    rs.getString("TietHoc"), rs.getString("PhongHoc"), rs.getInt("SucChua"), rs.getInt("DaDK"),
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
        table.clearSelection();
    }

    // ==========================================
    // UI UTILS: THIẾT KẾ NÚT & Ô NHẬP LIỆU
    // ==========================================
    private JPanel createCompactFormRow(String labelText, JComponent input) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(0, 0, 5, 0)); 
        
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(71, 85, 105)); 
        lbl.setBorder(new EmptyBorder(0, 0, 3, 0)); 
        
        wrapper.add(lbl, BorderLayout.NORTH);
        wrapper.add(input, BorderLayout.CENTER);
        return wrapper;
    }

    private JButton createActionButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI()); 
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(bgColor.darker(), 1, true),
            new EmptyBorder(8, 20, 8, 20)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bgColor.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bgColor); }
        });
        return btn;
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
            c.setForeground(UIUtils.TEXT_MAIN);
            setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(0, 15, 0, 15)
            ));
            return c;
        }
    }
}