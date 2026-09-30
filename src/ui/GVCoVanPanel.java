package ui;

import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Man hinh "Lop co van" - GV lam co van hoc tap cho 1 hay nhieu LOP SINH HOAT
 * (SINH_VIEN.MaLop, vd K2021A - khac voi lop hoc phan theo mon). Hien GPA va
 * tin chi tich luy tung SV, co bo loc "chi hien SV canh bao hoc vu".
 */
public class GVCoVanPanel extends JPanel {

    // Nguong canh bao hoc vu: GPA duoi 5.0/10 HOAC dang no tu 2 mon tro len.
    private static final double NGUONG_GPA_CANH_BAO = 5.0;
    private static final int NGUONG_SO_MON_NO_CANH_BAO = 2;

    private final StudentManagerService service;
    private final String maGV;
    private JComboBox<String> cbLop;
    private JCheckBox chkChiCanhBao;
    private DefaultTableModel tableModel;
    private List<Object[]> rawData = new ArrayList<>();

    public GVCoVanPanel(StudentManagerService service, String maGV) {
        this.service = service;
        this.maGV = maGV;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(10, 0, 0, 0));

        buildUI();
    }

    private void buildUI() {
        List<String> dsLop = service.getLopCoVanByGV(maGV);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UIUtils.BG_APP);
        JLabel lblTitle = new JLabel("Lớp Cố Vấn Học Tập");
        lblTitle.setFont(UIUtils.FONT_TITLE);
        topBar.add(lblTitle, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        if (dsLop.isEmpty()) {
            JPanel emptyCard = UIUtils.createCardShell("Thông báo", UIUtils.MIT_RED);
            JLabel lblEmpty = new JLabel("Bạn hiện không được phân công làm cố vấn học tập cho lớp nào.");
            lblEmpty.setForeground(UIUtils.TEXT_MUTED);
            lblEmpty.setBorder(new EmptyBorder(24, 20, 24, 20));
            emptyCard.add(lblEmpty, BorderLayout.CENTER);
            add(emptyCard, BorderLayout.CENTER);
            return;
        }

        JPanel toolBar = new JPanel(new BorderLayout());
        toolBar.setBackground(UIUtils.BG_APP);
        toolBar.setBorder(new EmptyBorder(10, 0, 10, 0));

        JPanel toolLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolLeft.setBackground(UIUtils.BG_APP);
        toolLeft.add(new JLabel("Lớp:"));
        cbLop = new JComboBox<>(dsLop.toArray(new String[0]));
        cbLop.addActionListener(e -> loadData());
        toolLeft.add(cbLop);
        toolBar.add(toolLeft, BorderLayout.WEST);

        chkChiCanhBao = new JCheckBox("Chỉ hiện sinh viên cảnh báo học vụ");
        chkChiCanhBao.setBackground(UIUtils.BG_APP);
        chkChiCanhBao.addActionListener(e -> renderTable());
        toolBar.add(chkChiCanhBao, BorderLayout.EAST);

        JPanel tableContainer = UIUtils.createCardShell("Danh sách sinh viên", UIUtils.MIT_RED);

        String[] columns = {"MSSV", "Họ tên", "Trạng thái học tập", "GPA (hệ 10)", "Tín chỉ tích lũy", "Số môn đang nợ", "Cảnh báo"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(tableModel);
        UIUtils.styleTable(table);
        table.setRowHeight(38);
        int[] widths = {90, 170, 140, 100, 120, 120, 140};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                Object canhBao = t.getValueAt(row, t.getColumnCount() - 1);
                boolean warn = "⚠ Cảnh báo".equals(canhBao);
                if (!isSelected) {
                    c.setBackground(warn ? new Color(254, 226, 226) : Color.WHITE);
                    c.setForeground(warn ? new Color(185, 28, 28) : UIUtils.TEXT_MAIN);
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        tableContainer.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIUtils.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIUtils.BORDER), new EmptyBorder(10, 20, 10, 20)));
        JLabel lblGhiChu = new JLabel("Cảnh báo học vụ: GPA dưới " + NGUONG_GPA_CANH_BAO + "/10 hoặc đang nợ từ " + NGUONG_SO_MON_NO_CANH_BAO + " môn trở lên.");
        lblGhiChu.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblGhiChu.setForeground(UIUtils.TEXT_MUTED);
        footer.add(lblGhiChu, BorderLayout.WEST);
        tableContainer.add(footer, BorderLayout.SOUTH);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(UIUtils.BG_APP);
        centerWrap.add(toolBar, BorderLayout.NORTH);
        centerWrap.add(tableContainer, BorderLayout.CENTER);
        add(centerWrap, BorderLayout.CENTER);

        loadData();
    }

    private void loadData() {
        String maLop = (String) cbLop.getSelectedItem();
        if (maLop == null) return;
        rawData = service.getSinhVienTrongLopCoVan(maLop);
        renderTable();
    }

    private void renderTable() {
        tableModel.setRowCount(0);
        boolean chiCanhBao = chkChiCanhBao.isSelected();

        for (Object[] row : rawData) {
            String maSV = (String) row[0];
            String hoTen = (String) row[1];
            String trangThaiHocTap = row[2] != null ? (String) row[2] : "";
            double gpa = (double) row[3];
            int tinChiTichLuy = (int) row[4];
            int soMonNo = (int) row[5];

            boolean canhBao = gpa > 0 && gpa < NGUONG_GPA_CANH_BAO || soMonNo >= NGUONG_SO_MON_NO_CANH_BAO;
            if (chiCanhBao && !canhBao) continue;

            String canhBaoText = canhBao ? "⚠ Cảnh báo" : "";
            tableModel.addRow(new Object[]{
                maSV, hoTen, trangThaiHocTap, String.format("%.2f", gpa), tinChiTichLuy, soMonNo, canhBaoText
            });
        }
    }
}
