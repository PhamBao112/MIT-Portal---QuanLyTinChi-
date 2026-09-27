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

/**
 * Thong ke diem danh cho 1 lop hoc phan: voi tung SV, hien tong so buoi da diem danh,
 * so buoi vang (khong phep), so buoi vang co phep, ty le vang khong phep (%).
 * SV vuot nguong CANH_BAO_TY_LE_VANG se duoc to mau hong canh bao trong bang.
 */
public class GVThongKeDiemDanhDialog extends JDialog {

    // Nguong canh bao: vang KHONG PHEP qua 20% tong so buoi da diem danh cua lop
    // (muc pho bien nhieu truong ap dung de canh bao SV co nguy co bi cam thi).
    private static final double NGUONG_CANH_BAO = 20.0;

    public GVThongKeDiemDanhDialog(Window owner, StudentManagerService service, String maLHP, String tenMonHienThi) {
        super(owner, "Thống kê điểm danh - " + tenMonHienThi, ModalityType.APPLICATION_MODAL);
        setSize(720, 540);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIUtils.BG_APP);

        buildUI(service, maLHP, tenMonHienThi);
    }

    private void buildUI(StudentManagerService service, String maLHP, String tenMonHienThi) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtils.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIUtils.BORDER), new EmptyBorder(16, 22, 16, 22)));
        JLabel lblTitle = new JLabel("Thống kê điểm danh lớp " + maLHP + " - " + tenMonHienThi);
        lblTitle.setFont(UIUtils.FONT_TITLE);
        header.add(lblTitle, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        String[] columns = {"MSSV", "Họ tên", "Tổng buổi", "Vắng", "Vắng có phép", "Tỷ lệ vắng", "Cảnh báo"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        List<Object[]> ds = service.getThongKeDiemDanhTheoLop(maLHP);
        for (Object[] row : ds) {
            String maSV = (String) row[0];
            String hoTen = (String) row[1];
            int tongBuoi = (int) row[2];
            int soVang = (int) row[3];
            int soVangCoPhep = (int) row[4];

            String tyLeStr = "Chưa điểm danh";
            boolean canhBao = false;
            if (tongBuoi > 0) {
                double tyLe = soVang * 100.0 / tongBuoi;
                tyLeStr = String.format("%.1f%%", tyLe);
                canhBao = tyLe > NGUONG_CANH_BAO;
            }
            String canhBaoText = canhBao ? "⚠ Vắng nhiều" : "";

            tableModel.addRow(new Object[]{ maSV, hoTen, tongBuoi, soVang, soVangCoPhep, tyLeStr, canhBaoText });
        }

        JTable table = new JTable(tableModel);
        UIUtils.styleTable(table);
        table.setRowHeight(38);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                Object canhBao = t.getValueAt(row, t.getColumnCount() - 1);
                boolean warn = "⚠ Vắng nhiều".equals(canhBao);
                if (!isSelected) {
                    c.setBackground(warn ? new Color(254, 226, 226) : Color.WHITE);
                    c.setForeground(warn ? new Color(185, 28, 28) : UIUtils.TEXT_MAIN);
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIUtils.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIUtils.BORDER), new EmptyBorder(14, 22, 14, 22)));
        JLabel lblHint = new JLabel("SV vắng (không phép) trên " + (int) NGUONG_CANH_BAO + "% số buổi đã điểm danh sẽ được cảnh báo.");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblHint.setForeground(UIUtils.TEXT_MUTED);
        footer.add(lblHint, BorderLayout.WEST);

        JButton btnDong = UIUtils.createSecondaryBtn("Đóng");
        btnDong.addActionListener(e -> dispose());
        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnBox.setBackground(UIUtils.WHITE);
        btnBox.add(btnDong);
        footer.add(btnBox, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);
    }
}
