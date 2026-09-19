package ui;

import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CongNoPanel extends JPanel {
    private StudentManagerService service;
    private String maSV;

    private DefaultTableModel tableModel;
    private JTable table;
    private JButton btnThanhToan;
    private JLabel lblTongNo;

    private final Color COLOR_PAY_ACTIVE = UIUtils.MIT_RED;
    private final Color COLOR_PAY_DONE = new Color(25, 135, 84);
    private final Color COLOR_PAY_DISABLED = new Color(156, 163, 175);

    public CongNoPanel(StudentManagerService service, String maSV) {
        this.service = service;
        this.maSV = maSV;

        setLayout(new BorderLayout(0, 20));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // ==========================================
        // KHÚC GIỮA: BẢNG DANH SÁCH CÔNG NỢ TỪNG KỲ (card shell dùng chung)
        // ==========================================
        JPanel tablePanel = UIUtils.createCardShell("Bảng theo dõi Thanh toán Học phí", UIUtils.MIT_RED);

        JPanel tableHeader = (JPanel) tablePanel.getComponent(0);
        lblTongNo = new JLabel("Tổng nợ hiện tại: 0 VNĐ");
        lblTongNo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTongNo.setForeground(UIUtils.RED_500);
        tableHeader.add(lblTongNo, BorderLayout.EAST);

        tableModel = new DefaultTableModel(new String[]{
                "Mã HK", "Học Kỳ", "Tổng phải đóng (VNĐ)", "Đã đóng (VNĐ)", "Còn nợ (VNĐ)", "Trạng Thái"
        }, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UIUtils.styleTable(table);

        table.setShowGrid(true);
        table.setGridColor(UIUtils.BORDER);
        table.setIntercellSpacing(new Dimension(1, 1));

        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(5).setPreferredWidth(140);

        RowColorRenderer rowColorRenderer = new RowColorRenderer();
        for (int i = 0; i < table.getColumnCount() - 1; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rowColorRenderer);
        }
        table.getColumnModel().getColumn(5).setCellRenderer(new BadgeRenderer());

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));

        tablePanel.add(scrollTable, BorderLayout.CENTER);

        // ==========================================
        // KHÚC DƯỚI: NÚT THANH TOÁN
        // ==========================================
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 15));
        bottomPanel.setBackground(UIUtils.WHITE);
        bottomPanel.setBorder(new EmptyBorder(0, 20, 10, 20));

        btnThanhToan = new JButton("CHỌN HỌC KỲ ĐỂ THANH TOÁN");
        btnThanhToan.setIcon(new CreditCardIcon());
        btnThanhToan.setIconTextGap(12);
        btnThanhToan.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnThanhToan.setBackground(COLOR_PAY_DISABLED);
        btnThanhToan.setForeground(UIUtils.WHITE);
        btnThanhToan.setPreferredSize(new Dimension(280, 45));

        btnThanhToan.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btnThanhToan.setBorder(new EmptyBorder(0, 0, 0, 0));
        btnThanhToan.setFocusPainted(false);

        btnThanhToan.setEnabled(false);
        btnThanhToan.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnThanhToan.addActionListener(e -> actionThanhToan());

        bottomPanel.add(btnThanhToan);
        tablePanel.add(bottomPanel, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int row = table.getSelectedRow();
                String conNoStr = table.getValueAt(row, 4).toString().replaceAll("[^0-9]", "");
                double conNo = conNoStr.isEmpty() ? 0 : Double.parseDouble(conNoStr);

                if (conNo <= 0) {
                    btnThanhToan.setEnabled(false);
                    btnThanhToan.setBackground(COLOR_PAY_DONE);
                    btnThanhToan.setIcon(new CheckIcon());
                    btnThanhToan.setText("ĐÃ HOÀN THÀNH KỲ NÀY");
                } else {
                    btnThanhToan.setEnabled(true);
                    btnThanhToan.setBackground(COLOR_PAY_ACTIVE);
                    btnThanhToan.setIcon(new CreditCardIcon());
                    btnThanhToan.setText("THANH TOÁN KỲ NÀY");
                }
            }
        });

        add(tablePanel, BorderLayout.CENTER);

        loadTableData();
    }

    private void loadTableData() {
        tableModel.setRowCount(0);
        double tongTienNoToanKhoa = 0;

        try {
            List<Object[]> ds = service.getDanhSachCongNo(maSV);
            for (Object[] row : ds) {
                String maHK = (String) row[0];
                String tenHK = row[1] != null ? (String) row[1] : maHK;
                double tongPhaiDong = (double) row[2];
                double daDong = (double) row[3];
                double conNo = tongPhaiDong - daDong;
                String trangThai = (String) row[4];

                tongTienNoToanKhoa += conNo;

                tableModel.addRow(new Object[]{
                    maHK, tenHK, String.format("%,.0f", tongPhaiDong),
                    String.format("%,.0f", daDong), String.format("%,.0f", conNo), trangThai
                });
            }

            if (tongTienNoToanKhoa > 0) {
                lblTongNo.setText("Tổng nợ hiện tại: " + String.format("%,.0f VNĐ", tongTienNoToanKhoa));
                lblTongNo.setForeground(UIUtils.RED_500);
            } else {
                lblTongNo.setText("Bạn không có khoản nợ nào.");
                lblTongNo.setForeground(UIUtils.GREEN_500);
            }

            btnThanhToan.setEnabled(false);
            btnThanhToan.setBackground(COLOR_PAY_DISABLED);
            btnThanhToan.setIcon(new CreditCardIcon());
            btnThanhToan.setText("CHỌN HỌC KỲ ĐỂ THANH TOÁN");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void actionThanhToan() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) return;

        String maHK = table.getValueAt(selectedRow, 0).toString();
        String tenHK = table.getValueAt(selectedRow, 1).toString();
        String tienNoStr = table.getValueAt(selectedRow, 4).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
            "Xác nhận thanh toán công nợ cho [" + tenHK + "] với số tiền " + tienNoStr + " VNĐ?",
            "Xác nhận thanh toán", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String msg = service.thanhToanCongNo(maSV, maHK);
                JOptionPane.showMessageDialog(this, msg, "Giao dịch thành công", JOptionPane.INFORMATION_MESSAGE);
                loadTableData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi thanh toán", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class CreditCardIcon implements Icon {
        public int getIconWidth() { return 22; }
        public int getIconHeight() { return 22; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRoundRect(x + 1, y + 4, 20, 14, 4, 4);
            g2.fillRect(x + 1, y + 8, 20, 3);
            g2.fillRect(x + 4, y + 13, 4, 2);
            g2.dispose();
        }
    }

    class CheckIcon implements Icon {
        public int getIconWidth() { return 22; }
        public int getIconHeight() { return 22; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 4, y + 11, x + 9, y + 16);
            g2.drawLine(x + 9, y + 16, x + 18, y + 6);
            g2.dispose();
        }
    }

    class RowColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                c.setForeground(UIUtils.TEXT_MAIN);
            } else {
                c.setBackground(UIUtils.MIT_RED_LIGHT);
                c.setForeground(UIUtils.MIT_RED);
            }

            setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 1, UIUtils.BORDER),
                new EmptyBorder(0, 15, 0, 15)
            ));
            return c;
        }
    }

    /** Cột "Trạng Thái" -> badge tròn màu dùng chung UIUtils. */
    class BadgeRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            wrap.setBackground(isSelected ? UIUtils.MIT_RED_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
            wrap.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 1, UIUtils.BORDER), new EmptyBorder(6, 15, 6, 15)));
            if (value != null) {
                String status = value.toString();
                int type = status.equalsIgnoreCase("Đã hoàn thành") ? UIUtils.BADGE_OK
                         : (status.equalsIgnoreCase("Chưa đóng") || status.toLowerCase().contains("nợ")) ? UIUtils.BADGE_BAD
                         : UIUtils.BADGE_PENDING;
                wrap.add(UIUtils.createBadge(status, type));
            }
            return wrap;
        }
    }
}
