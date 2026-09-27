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

public class AdminCongNoPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private DefaultTableModel model;
    private JTable table;
    private JLabel lblHeroSub;
    private JPanel heroRingHolder;

    private JTextField txtMaPhieu, txtTienThu;

    public AdminCongNoPanel() {
        setLayout(new BorderLayout(0, 20));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(5, 0, 0, 0));

        // --- V9: banner gradient + vong tron ty le da thu hoc phi ---
        JPanel hero = UIUtils.createHeroBanner();
        hero.setLayout(new BorderLayout(20, 0));
        hero.setBorder(new EmptyBorder(20, 28, 20, 26));
        hero.setMaximumSize(new Dimension(6000, 108));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel heroLeft = new JPanel();
        heroLeft.setLayout(new BoxLayout(heroLeft, BoxLayout.Y_AXIS));
        heroLeft.setOpaque(false);
        JLabel lblHeroTitle = new JLabel("Công Nợ Học Phí Toàn Trường");
        lblHeroTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblHeroTitle.setForeground(Color.WHITE);
        lblHeroTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblHeroSub = new JLabel("Đang tải dữ liệu...");
        lblHeroSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblHeroSub.setForeground(new Color(255, 255, 255, 215));
        lblHeroSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblHeroSub.setBorder(new EmptyBorder(6, 0, 0, 0));
        heroLeft.add(Box.createVerticalGlue());
        heroLeft.add(lblHeroTitle);
        heroLeft.add(lblHeroSub);
        heroLeft.add(Box.createVerticalGlue());

        heroRingHolder = new JPanel(new BorderLayout());
        heroRingHolder.setOpaque(false);
        heroRingHolder.setPreferredSize(new Dimension(96, 96));
        heroRingHolder.add(UIUtils.createRadialProgress(0, "0%", "Đã thu",
            UIUtils.MIT_YELLOW, new Color(255, 255, 255, 55), Color.WHITE), BorderLayout.CENTER);

        hero.add(heroLeft, BorderLayout.WEST);
        hero.add(heroRingHolder, BorderLayout.EAST);

        JPanel tableWrapper = UIUtils.createCardShell("Bảng Danh Sách Công Nợ Học Phí", UIUtils.MIT_RED);

        String[] cols = {"Mã Phiếu", "Mã SV", "Họ Tên SV", "Học Kỳ", "Phải Đóng", "Đã Đóng", "Còn Nợ", "Trạng Thái"};
        model = new DefaultTableModel(cols, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIUtils.styleTable(table);
        table.setShowGrid(true);
        table.setGridColor(UIUtils.BORDER);
        table.setIntercellSpacing(new Dimension(1, 1));

        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(7).setPreferredWidth(110);

        ZebraRenderer zebra = new ZebraRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        tableWrapper.add(scroll, BorderLayout.CENTER);

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);
        topSection.add(hero);
        topSection.add(Box.createVerticalStrut(20));

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(topSection, BorderLayout.NORTH);
        centerWrapper.add(tableWrapper, BorderLayout.CENTER);

        JPanel formWrapper = UIUtils.createCardShell("Ghi Nhận Thanh Toán Học Phí", UIUtils.MIT_RED);
        formWrapper.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel inputGrid = new JPanel(new GridLayout(1, 3, 20, 5));
        inputGrid.setOpaque(false);
        inputGrid.setBorder(new EmptyBorder(15, 20, 10, 20));

        txtMaPhieu = UIUtils.createInput();
        txtMaPhieu.setEditable(false);
        txtMaPhieu.setBackground(UIUtils.BG_APP);

        txtTienThu = UIUtils.createInput();

        inputGrid.add(createCompactFormRow("Mã Phiếu Thu (Chọn từ danh sách):", txtMaPhieu));
        inputGrid.add(createCompactFormRow("Số Tiền Thu (VNĐ):", txtTienThu));
        inputGrid.add(new JLabel(""));

        JPanel btnGrid = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnGrid.setOpaque(false);
        btnGrid.setBorder(new EmptyBorder(4, 20, 16, 20));

        JButton btnThuTien = UIUtils.createPrimaryBtn("check", "Xác nhận thu tiền");
        btnThuTien.setPreferredSize(new Dimension(170, 38));

        btnGrid.add(btnThuTien);

        JPanel formContent = new JPanel(new BorderLayout());
        formContent.setOpaque(false);
        formContent.add(inputGrid, BorderLayout.CENTER);
        formContent.add(btnGrid, BorderLayout.SOUTH);
        formWrapper.add(formContent, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            int r = table.getSelectedRow();
            if(r >= 0 && !e.getValueIsAdjusting()) {
                txtMaPhieu.setText(model.getValueAt(r, 0).toString());
                String conNoStr = model.getValueAt(r, 6).toString().replaceAll("[^0-9]", "");
                txtTienThu.setText(conNoStr);
            }
        });

        btnThuTien.addActionListener(e -> {
            String maPhieu = txtMaPhieu.getText().trim();
            String tienThuStr = txtTienThu.getText().trim().replaceAll("[^0-9]", "");

            if (maPhieu.isEmpty() || tienThuStr.isEmpty() || tienThuStr.equals("0")) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn phiếu nợ và nhập số tiền hợp lệ!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double tienThu = Double.parseDouble(tienThuStr);

            int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận thu " + String.format("%,.0f", tienThu) + " VNĐ cho phiếu " + maPhieu + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String sql = "UPDATE CONG_NO_HOC_PHI SET SoTienDaDong = SoTienDaDong + ?, TrangThai = CASE WHEN TongTienPhaiDong <= SoTienDaDong + ? THEN N'Đã hoàn thành' ELSE N'Còn nợ' END WHERE MaPhieu = ?";
                try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setDouble(1, tienThu);
                    ps.setDouble(2, tienThu);
                    ps.setString(3, maPhieu);
                    ps.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Thu tiền thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    txtMaPhieu.setText("");
                    txtTienThu.setText("");
                    updateData("");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi Database", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        add(centerWrapper, BorderLayout.CENTER);
        add(formWrapper, BorderLayout.SOUTH);
    }

    public void updateData(String maHK) {
        model.setRowCount(0);
        double tongNo = 0;
        double tongPhaiDong = 0;
        double tongDaDong = 0;

        String sql = "SELECT c.MaPhieu, c.MaSV, s.HoTen, c.MaHK, c.TongTienPhaiDong, c.SoTienDaDong, c.TrangThai " +
                     "FROM CONG_NO_HOC_PHI c JOIN SINH_VIEN s ON c.MaSV = s.MaSV " +
                     "ORDER BY c.MaHK DESC, c.MaPhieu ASC";

        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double phaiDong = rs.getDouble("TongTienPhaiDong");
                double daDong = rs.getDouble("SoTienDaDong");
                double no = phaiDong - daDong;

                if (no > 0) tongNo += no;
                tongPhaiDong += phaiDong;
                tongDaDong += daDong;

                model.addRow(new Object[]{
                    rs.getString("MaPhieu"), rs.getString("MaSV"), rs.getString("HoTen"), rs.getString("MaHK"),
                    String.format("%,.0f đ", phaiDong), String.format("%,.0f đ", daDong), String.format("%,.0f đ", Math.max(no, 0)), rs.getString("TrangThai")
                });
            }

            lblHeroSub.setText("Tổng phải thu: " + String.format("%,.0f đ", tongPhaiDong)
                + "  ·  Còn nợ: " + String.format("%,.0f đ", tongNo));

            int pctDaThu = tongPhaiDong > 0 ? (int) Math.round(tongDaDong * 100.0 / tongPhaiDong) : 0;
            heroRingHolder.removeAll();
            heroRingHolder.add(UIUtils.createRadialProgress(pctDaThu, pctDaThu + "%", "Đã thu",
                UIUtils.MIT_YELLOW, new Color(255, 255, 255, 55), Color.WHITE), BorderLayout.CENTER);
            heroRingHolder.revalidate();
            heroRingHolder.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            if (column == 7 && value != null) {
                String text = value.toString();
                int type = text.equals("Đã hoàn thành") ? UIUtils.BADGE_OK : UIUtils.BADGE_BAD;
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
            c.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(0, 15, 0, 15)
            ));
            return c;
        }
    }
}
