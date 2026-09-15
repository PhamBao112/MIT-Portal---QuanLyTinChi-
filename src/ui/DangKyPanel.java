package ui;

import service.StudentManagerService;
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

public class DangKyPanel extends JPanel {
    
    private String currentMaHK = "";
    private String currentTenHK = "Đang tải dữ liệu...";
    private JComboBox<String> cbHocKy;
    
    private StudentManagerService service;
    private String currentMaSV;
    
    private DefaultTableModel tableModel;
    private JTable table;
    
    private JPanel cartContentPanel;
    private JLabel lblTotalCredits;
    private JLabel lblCreditInfo; 
    private JLabel lblBannerTitle; 

    private final Color INDIGO_600 = new Color(79, 70, 229);
    private final Color INDIGO_50 = new Color(238, 242, 255);
    private final Color SLATE_50 = new Color(248, 250, 252);
    private final Color GREEN_100 = new Color(220, 252, 231);
    private final Color GREEN_800 = new Color(22, 101, 52);
    private final Color RED_100 = new Color(254, 226, 226);
    private final Color RED_800 = new Color(153, 27, 27);

    public DangKyPanel(StudentManagerService service, String maSV) {
        this.service = service;
        this.currentMaSV = maSV;

        setLayout(new BorderLayout(0, 20));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(10, 0, 0, 0));

        cbHocKy = new JComboBox<>();
        loadHocKyOptions(cbHocKy); // populate + resolve currentMaHK/currentTenHK, chưa gắn listener nên không bắn sự kiện

        // ==========================================
        // 0. THANH THÔNG BÁO (BANNER) Ở TRÊN CÙNG (Giữ nguyên)
        // ==========================================
        JPanel topInfoPanel = new JPanel(new BorderLayout());
        topInfoPanel.setBackground(new Color(40, 40, 40));
        topInfoPanel.setBorder(new EmptyBorder(12, 20, 12, 20));

        lblBannerTitle = new JLabel("Đăng ký học phần - " + currentTenHK);
        lblBannerTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBannerTitle.setForeground(Color.WHITE);

        lblCreditInfo = new JLabel("TC đã đăng ký: 0/24    Tối thiểu: 14 TC - Tối đa: 24 TC");
        lblCreditInfo.setForeground(new Color(180,180,180));
        lblCreditInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel leftInfo = new JPanel();
        leftInfo.setOpaque(false);
        leftInfo.setLayout(new BoxLayout(leftInfo, BoxLayout.Y_AXIS));
        leftInfo.add(lblBannerTitle);
        leftInfo.add(Box.createVerticalStrut(5));
        leftInfo.add(lblCreditInfo);

        JLabel lblStatus = new JLabel("Đang mở đăng ký");
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(255, 243, 205));
        lblStatus.setForeground(new Color(146, 64, 14));
        lblStatus.setBorder(new EmptyBorder(6, 12, 6, 12));
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel hkBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        hkBox.setOpaque(false);
        JLabel lblHkTag = new JLabel("Học kỳ:");
        lblHkTag.setForeground(Color.WHITE);
        hkBox.add(lblHkTag);
        cbHocKy.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbHocKy.addActionListener(e -> {
            String sel = (String) cbHocKy.getSelectedItem();
            if (sel != null && sel.contains("-")) {
                currentMaHK = sel.split("-")[0].trim();
                currentTenHK = sel.substring(sel.indexOf("-") + 1).trim();
                lblBannerTitle.setText("Đăng ký học phần - " + currentTenHK);
                loadAvailableClasses();
                loadRegisteredClasses();
                revalidate();
                repaint();
            }
        });
        hkBox.add(cbHocKy);

        JPanel eastWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        eastWrap.setOpaque(false);
        eastWrap.add(hkBox);
        eastWrap.add(lblStatus);

        topInfoPanel.add(leftInfo, BorderLayout.WEST);
        topInfoPanel.add(eastWrap, BorderLayout.EAST);

        // ==========================================
        // 1. GRID CHÍNH (TRÁI: DANH SÁCH MÔN - PHẢI: GIỎ HÀNG)
        // ==========================================
        JPanel gridContainer = new JPanel(new GridBagLayout());
        gridContainer.setBackground(UIUtils.BG_APP);
        GridBagConstraints gbc = new GridBagConstraints();

        // --- PANEL TRÁI ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(new LineBorder(UIUtils.BORDER, 1, true));

        JPanel tableHeader = new JPanel(new BorderLayout());
        
        // 👉 1. ĐỔI MÀU NỀN TIÊU ĐỀ BẢNG THÀNH XANH NHẠT
        tableHeader.setBackground(new Color(239, 246, 255));
        tableHeader.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(191, 219, 254)), // Viền dưới nổi nhẹ
            new EmptyBorder(15, 20, 15, 20)
        ));
        
        JLabel lblLeftTitle = new JLabel("Danh sách lớp học phần mở đăng ký");
        lblLeftTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        // 👉 2. ĐỔI MÀU CHỮ TIÊU ĐỀ THÀNH XANH ĐẬM
        lblLeftTitle.setForeground(new Color(30, 64, 175));
        
        tableHeader.add(lblLeftTitle, BorderLayout.WEST);

        String[] columns = {"Mã lớp HP", "Tên môn học", "Số TC", "Giảng viên", "Thứ - Tiết", "Sĩ số", "Hành động"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UIUtils.styleTable(table);

        // =========================================================
        // 👉 3. ÉP MÀU HEADER JTABLE THÀNH XANH ĐẬM
        // =========================================================
        DefaultTableCellRenderer customHeaderRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setBackground(new Color(30, 64, 175)); // Nền Xanh dương đậm
                label.setForeground(Color.WHITE);            // Chữ Trắng
                label.setFont(new Font("Segoe UI", Font.BOLD, 14));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 0, 1, 1, new Color(40, 74, 185)), // Vạch chia cột
                    new EmptyBorder(10, 15, 10, 15)
                ));
                return label;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(customHeaderRenderer);
        }
        // =========================================================

        table.setShowGrid(true);
        table.setGridColor(UIUtils.BORDER);
        table.setIntercellSpacing(new Dimension(1, 1));

        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(190);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(6).setPreferredWidth(95);

        RowBackgroundRenderer rowBgRenderer = new RowBackgroundRenderer();
        for (int i = 0; i < table.getColumnCount() - 1; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rowBgRenderer);
        }
        table.getColumnModel().getColumn(6).setCellRenderer(new ActionButtonRenderer());

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        scrollTable.getViewport().setBackground(Color.WHITE);

        leftPanel.add(tableHeader, BorderLayout.NORTH);
        leftPanel.add(scrollTable, BorderLayout.CENTER);
        // --- PANEL PHẢI (GIỎ HÀNG) ---
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(new LineBorder(UIUtils.BORDER, 1, true));
        rightPanel.setPreferredSize(new Dimension(320, 0));

        JPanel cartHeader = new JPanel(new BorderLayout());
        
        // 👉 1. ĐỔI MÀU NỀN TIÊU ĐỀ GIỎ HÀNG THÀNH XANH NHẠT
        cartHeader.setBackground(new Color(239, 246, 255));
        cartHeader.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(191, 219, 254)), // Viền dưới nổi nhẹ
            new EmptyBorder(15, 20, 15, 20)
        ));
        
        JLabel lblRightTitle = new JLabel("Học phần đã chọn");
        lblRightTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        // 👉 2. ĐỔI MÀU CHỮ THÀNH XANH ĐẬM
        lblRightTitle.setForeground(new Color(30, 64, 175));
        
        cartHeader.add(lblRightTitle, BorderLayout.WEST);

        cartContentPanel = new JPanel();
        cartContentPanel.setLayout(new BoxLayout(cartContentPanel, BoxLayout.Y_AXIS));
        cartContentPanel.setBackground(Color.WHITE);

        JScrollPane scrollCart = new JScrollPane(cartContentPanel);
        scrollCart.setBorder(new MatteBorder(1, 0, 1, 0, UIUtils.BORDER));
        scrollCart.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel cartFooter = new JPanel(new BorderLayout());
        cartFooter.setBackground(SLATE_50);
        cartFooter.setBorder(new EmptyBorder(15, 20, 15, 20));
        lblTotalCredits = new JLabel("Tổng số tín chỉ: 0 TC");
        lblTotalCredits.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalCredits.setForeground(INDIGO_600);
        cartFooter.add(lblTotalCredits, BorderLayout.WEST);

        rightPanel.add(cartHeader, BorderLayout.NORTH);
        rightPanel.add(scrollCart, BorderLayout.CENTER);
        rightPanel.add(cartFooter, BorderLayout.SOUTH);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        gbc.gridx = 0; gbc.weightx = 0.7; gbc.insets = new Insets(0, 0, 0, 15);
        gridContainer.add(leftPanel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.3; gbc.insets = new Insets(0, 0, 0, 0);
        gridContainer.add(rightPanel, gbc);

        add(topInfoPanel, BorderLayout.NORTH);
        add(gridContainer, BorderLayout.CENTER);
        // LẮNG NGHE SỰ KIỆN CLICK VÀ KIỂM TRA MÔN TIÊN QUYẾT
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = table.getColumnModel().getColumnIndexAtX(e.getX());
                int row    = e.getY() / table.getRowHeight();

                if (row < table.getRowCount() && row >= 0 && column == 6) {
                    ActionState state = (ActionState) table.getValueAt(row, 6);
                    if (state != null) {
                        if (state.isRegistered) {
                            cancelClass(state.maLHP, state.tenMonDisplay);
                        } else if (!state.isEligible) {
                            // HIỆN THÔNG BÁO CHẶN NẾU THIẾU MÔN TIÊN QUYẾT
                            String msg = "Bạn không thể đăng ký môn học này vì đã thiếu môn tiên quyết";
                            if (state.maMonTQ != null && !state.maMonTQ.isEmpty()) {
                                msg += " (" + state.maMonTQ + ")";
                            }
                            JOptionPane.showMessageDialog(DangKyPanel.this, 
                                msg + "!", 
                                "Không thể đăng ký", JOptionPane.WARNING_MESSAGE);
                        } else if (!state.isFull) {
                            registerClass(state.maLHP, state.tenMonDisplay);
                        }
                    }
                }
            }
        });

        loadAvailableClasses();
        loadRegisteredClasses();
    }

    // Combo Hoc ky rieng cua trang Dang Ky: gom nhung ky SV da dang ky TRUOC DAY + luon co ky hien tai (moi nhat toan he thong)
    // de SV moi chua dang ky gi cung thay duoc ky dang mo dang ky, khac voi Diem/Lich chi loc thuan theo KET_QUA_DANG_KY
    private void loadHocKyOptions(JComboBox<String> combo) {
        combo.removeAllItems();
        String sql = "SELECT MaHK, TenHK FROM HOC_KY WHERE " +
                     "MaHK IN (SELECT DISTINCT lhp.MaHK FROM LOP_HOC_PHAN lhp JOIN KET_QUA_DANG_KY kq ON kq.MaLHP = lhp.MaLHP WHERE kq.MaSV = ?) " +
                     "OR MaHK = (SELECT TOP 1 MaHK FROM HOC_KY ORDER BY NamHoc DESC, MaHK DESC) " +
                     "ORDER BY NamHoc DESC, MaHK DESC";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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

    // =========================================================
    // TRUY VẤN MÔN HỌC KẾT HỢP KIỂM TRA MÔN TIÊN QUYẾT 
    // =========================================================
    private void loadAvailableClasses() {
        if (currentMaHK.isEmpty()) return;
        tableModel.setRowCount(0);

        String sql = "SELECT lhp.MaLHP, m.TenMon, m.SoTinChi, gv.HoTen as TenGV, " +
                     "lhp.Thu, lhp.TietHoc, lhp.PhongHoc, lhp.SucChua, " +
                     "(SELECT COUNT(*) FROM KET_QUA_DANG_KY WHERE MaLHP = lhp.MaLHP) as SiSo, " +
                     "(SELECT COUNT(*) FROM KET_QUA_DANG_KY WHERE MaLHP = lhp.MaLHP AND MaSV = ?) as DaDangKy, " +
                     "tq.MaMonTQ, " +
                     "(SELECT COUNT(*) FROM KET_QUA_DANG_KY kq2 " +
                     " JOIN LOP_HOC_PHAN lhp2 ON kq2.MaLHP = lhp2.MaLHP " +
                     " WHERE kq2.MaSV = ? AND lhp2.MaMon = tq.MaMonTQ AND kq2.TrangThai = N'Đạt') as PassTQ " +
                     "FROM LOP_HOC_PHAN lhp " +
                     "JOIN MON_HOC m ON lhp.MaMon = m.MaMon " +
                     // FIX #1: doi thanh LEFT JOIN - lop chua duoc gan GV van phai hien ra cho SV thay,
                     // truoc day dung INNER JOIN nen lop MaGV = NULL bi am tham bien mat khoi danh sach dang ky
                     "LEFT JOIN GIANG_VIEN gv ON lhp.MaGV = gv.MaGV " +
                     "LEFT JOIN MON_TIEN_QUYET tq ON m.MaMon = tq.MaMon " +
                     "WHERE lhp.MaHK = ?";

        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentMaSV); // Param cho DaDangKy
            ps.setString(2, currentMaSV); // Param cho PassTQ
            ps.setString(3, currentMaHK); // Param cho MaHK
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String maLHP = rs.getString("MaLHP");
                String tenMon = rs.getString("TenMon");
                int tc = rs.getInt("SoTinChi");
                String gv = rs.getString("TenGV");
                if (gv == null || gv.isBlank()) gv = "Chưa phân công"; // FIX #1: LEFT JOIN co the tra ve NULL
                String thoiGian = rs.getString("Thu") + " (" + rs.getString("TietHoc") + ") - " + rs.getString("PhongHoc");
                int sucChua = rs.getInt("SucChua");
                int siSo = rs.getInt("SiSo");
                
                boolean isReg = rs.getInt("DaDangKy") > 0;
                boolean isFull = siSo >= sucChua;
                
                String maMonTQ = rs.getString("MaMonTQ");
                int passTQ = rs.getInt("PassTQ");
                boolean isEligible = (maMonTQ == null || maMonTQ.trim().isEmpty() || passTQ > 0);

                String tenMonHienThi = tenMon;
                if (maMonTQ != null && !maMonTQ.trim().isEmpty()) {
                    tenMonHienThi += " (TQ: " + maMonTQ + ")";
                }

                tableModel.addRow(new Object[]{
                    maLHP, tenMonHienThi, tc, gv, thoiGian, siSo + " / " + sucChua,
                    new ActionState(maLHP, tenMon, tenMonHienThi, isReg, isFull, isEligible, maMonTQ)
                });
            }
        } catch (Exception e) {}
    }

    private void loadRegisteredClasses() {
        if (currentMaHK.isEmpty()) return;
        
        cartContentPanel.removeAll();
        int tongTC = 0;

        String sql = "SELECT lhp.MaLHP, m.TenMon, m.SoTinChi, lhp.Thu, lhp.TietHoc " +
                     "FROM KET_QUA_DANG_KY kq " +
                     "JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP " +
                     "JOIN MON_HOC m ON lhp.MaMon = m.MaMon " +
                     "WHERE kq.MaSV = ? AND lhp.MaHK = ?"; 

        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentMaSV);
            ps.setString(2, currentMaHK); 
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String maLHP = rs.getString("MaLHP");
                String tenMon = rs.getString("TenMon");
                int tc = rs.getInt("SoTinChi");
                String thu = rs.getString("Thu");
                String tiet = rs.getString("TietHoc");

                tongTC += tc;
                cartContentPanel.add(createCartItem(maLHP, tenMon, tc, thu, tiet));
                cartContentPanel.add(Box.createVerticalStrut(10));
            }
        } catch (Exception e) {}

        lblTotalCredits.setText("Tổng số tín chỉ: " + tongTC + " TC");
        lblCreditInfo.setText("TC đã đăng ký: " + tongTC + "/24    Tối thiểu: 14 TC - Tối đa: 24 TC");

        cartContentPanel.revalidate();
        cartContentPanel.repaint();
    }

    private JPanel createCartItem(String maLHP, String tenMon, int tc, String thu, String tiet) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(SLATE_50);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIUtils.BORDER, 1, true), new EmptyBorder(12, 15, 12, 15)
        ));
        card.setMaximumSize(new Dimension(300, 85));

        JPanel textP = new JPanel();
        textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS));
        textP.setOpaque(false);

        JLabel lTitle = new JLabel("<html><b>" + tenMon + "</b></html>");
        lTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lTitle.setForeground(UIUtils.TEXT_MAIN);

        JLabel lSub = new JLabel(tc + " TC • " + thu + " (Tiết " + tiet + ")");
        lSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lSub.setForeground(UIUtils.TEXT_MUTED);

        textP.add(lTitle);
        textP.add(Box.createVerticalStrut(4));
        textP.add(lSub);

        JLabel btnDelete = new JLabel("HỦY TC");
        btnDelete.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDelete.setForeground(new Color(220, 38, 38)); 
        btnDelete.setBackground(new Color(254, 226, 226)); 
        btnDelete.setOpaque(true);
        btnDelete.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(252, 165, 165), 1, true),
            new EmptyBorder(6, 12, 6, 12)
        ));

        btnDelete.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btnDelete.setBackground(new Color(252, 165, 165)); }
            @Override
            public void mouseExited(MouseEvent e) { btnDelete.setBackground(new Color(254, 226, 226)); }
            @Override
            public void mouseClicked(MouseEvent e) { cancelClass(maLHP, tenMon); }
        });

        card.add(textP, BorderLayout.CENTER);
        card.add(btnDelete, BorderLayout.EAST);
        return card;
    }

    private void registerClass(String maLHP, String tenMonDisplay) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xác nhận đăng ký học phần [" + tenMonDisplay + "]?",
                "Đăng ký học phần", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String result = service.registerCourse(currentMaSV, maLHP);
                JOptionPane.showMessageDialog(this, result, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadAvailableClasses();
                loadRegisteredClasses();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi đăng ký", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cancelClass(String maLHP, String tenMon) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn hủy đăng ký học phần [" + tenMon + "]?",
                "Hủy đăng ký học phần", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String result = service.cancelRegistration(currentMaSV, maLHP);
                JOptionPane.showMessageDialog(this, result, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadAvailableClasses();
                loadRegisteredClasses();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi hủy đăng ký", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class ActionState {
        String maLHP;
        String tenMon;
        String tenMonDisplay;
        boolean isRegistered;
        boolean isFull;
        boolean isEligible;
        String maMonTQ;

        public ActionState(String maLHP, String tenMon, String tenMonDisplay, boolean isReg, boolean isFull, boolean isEligible, String maMonTQ) {
            this.maLHP = maLHP;
            this.tenMon = tenMon;
            this.tenMonDisplay = tenMonDisplay;
            this.isRegistered = isReg;
            this.isFull = isFull;
            this.isEligible = isEligible;
            this.maMonTQ = maMonTQ;
        }
    }

    class RowBackgroundRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            ActionState state = (ActionState) table.getValueAt(row, 6);
            if (!isSelected) {
                if (state != null && state.isRegistered) {
                    c.setBackground(INDIGO_50); 
                } else {
                    // Đã bỏ màu nền vàng của dòng, trả lại giao diện sọc trắng/xám bình thường
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                c.setForeground(UIUtils.TEXT_MAIN);
            } else {
                c.setBackground(UIUtils.MIT_RED_LIGHT);
                c.setForeground(UIUtils.MIT_RED);
            }

            setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(0, 15, 0, 15)
            ));
            return c;
        }
    }

    class ActionButtonRenderer extends DefaultTableCellRenderer {
        private JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        private JLabel btn = new JLabel();

        public ActionButtonRenderer() {
            p.setOpaque(true); 
            btn.setOpaque(true);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setBorder(new EmptyBorder(6, 12, 6, 12));
            p.add(btn);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            ActionState state = (ActionState) value;
            if (state != null) {
                if (state.isRegistered) {
                    p.setBackground(INDIGO_50);
                    btn.setText("Hủy ĐK");
                    btn.setBackground(RED_100); 
                    btn.setForeground(RED_800);
                } else if (state.isFull) {
                    p.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                    btn.setText("Đã đầy");
                    btn.setBackground(RED_100); 
                    btn.setForeground(RED_800);
                } else {
                    // Dù có đủ điều kiện hay không (Thiếu TQ) thì vẫn hiển thị nút Đăng ký Xanh lá
                    p.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                    btn.setText("Đăng ký");
                    btn.setBackground(GREEN_100); 
                    btn.setForeground(GREEN_800);
                }
            }
            p.setBorder(new MatteBorder(0, 0, 1, 1, UIUtils.BORDER));
            return p;
        }
    }
}