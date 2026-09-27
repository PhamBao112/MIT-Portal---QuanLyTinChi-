package ui;

import exception.BusinessLogicException;
import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * So dau bai dien tu: GV chon ngay hoc (gioi han trong khoang lop dang mo),
 * ghi noi dung da day + ghi chu cho buoi do. Ben duoi la lich su cac buoi da
 * ghi cua lop nay (moi nhat truoc) - double-click 1 dong de nap lai vao form sua.
 */
public class GVSoDauBaiDialog extends JDialog {

    private final StudentManagerService service;
    private final String maGV;
    private final String maLHP;
    private JSpinner dateSpinner;
    private JTextArea txtNoiDung;
    private JTextArea txtGhiChu;
    private DefaultTableModel lichSuModel;
    private static final SimpleDateFormat DF = new SimpleDateFormat("dd/MM/yyyy");

    public GVSoDauBaiDialog(Window owner, StudentManagerService service, String maGV, String maLHP,
                             String tenMonHienThi, java.util.Date ngayBatDau, java.util.Date ngayKetThuc) {
        super(owner, "Sổ đầu bài - " + tenMonHienThi, ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.maGV = maGV;
        this.maLHP = maLHP;

        setSize(760, 640);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIUtils.BG_APP);

        buildUI(tenMonHienThi, ngayBatDau, ngayKetThuc);
    }

    private void buildUI(String tenMonHienThi, java.util.Date ngayBatDau, java.util.Date ngayKetThuc) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtils.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIUtils.BORDER), new EmptyBorder(16, 22, 10, 22)));

        JLabel lblTitle = new JLabel("Sổ đầu bài lớp " + maLHP + " - " + tenMonHienThi);
        lblTitle.setFont(UIUtils.FONT_TITLE);
        header.add(lblTitle, BorderLayout.WEST);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        datePanel.setBackground(UIUtils.WHITE);
        datePanel.add(new JLabel("Ngày học:"));

        java.util.Date min = ngayBatDau != null ? ngayBatDau : new java.util.Date(0);
        java.util.Date max = ngayKetThuc != null ? ngayKetThuc : new java.util.Date();
        java.util.Date initial = new java.util.Date();
        if (initial.before(min)) initial = min;
        if (initial.after(max)) initial = max;

        SpinnerDateModel dateModel = new SpinnerDateModel(initial, min.before(max) ? min : max, min.before(max) ? max : min, java.util.Calendar.DAY_OF_MONTH);
        dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        dateSpinner.setPreferredSize(new Dimension(130, 30));
        dateSpinner.addChangeListener(e -> loadFormChoNgayDaChon());
        datePanel.add(dateSpinner);
        header.add(datePanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // --- Form ghi noi dung ---
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(UIUtils.WHITE);
        form.setBorder(new EmptyBorder(16, 22, 10, 22));

        JLabel lblNoiDung = new JLabel("Nội dung đã dạy");
        lblNoiDung.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblNoiDung.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblNoiDung.setBorder(new EmptyBorder(0, 0, 6, 0));

        txtNoiDung = new JTextArea(4, 20);
        txtNoiDung.setLineWrap(true);
        txtNoiDung.setWrapStyleWord(true);
        txtNoiDung.setFont(UIUtils.FONT_NORMAL);
        JScrollPane scrollNoiDung = new JScrollPane(txtNoiDung);
        scrollNoiDung.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollNoiDung.setMaximumSize(new Dimension(6000, 110));

        JLabel lblGhiChu = new JLabel("Ghi chú (tình hình lớp, vấn đề phát sinh...)");
        lblGhiChu.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblGhiChu.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblGhiChu.setBorder(new EmptyBorder(12, 0, 6, 0));

        txtGhiChu = new JTextArea(2, 20);
        txtGhiChu.setLineWrap(true);
        txtGhiChu.setWrapStyleWord(true);
        txtGhiChu.setFont(UIUtils.FONT_NORMAL);
        JScrollPane scrollGhiChu = new JScrollPane(txtGhiChu);
        scrollGhiChu.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollGhiChu.setMaximumSize(new Dimension(6000, 70));

        JButton btnLuu = UIUtils.createPrimaryBtn("check", "Lưu buổi học này");
        btnLuu.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLuu.addActionListener(e -> luu());

        form.add(lblNoiDung);
        form.add(scrollNoiDung);
        form.add(lblGhiChu);
        form.add(scrollGhiChu);
        form.add(Box.createVerticalStrut(12));
        form.add(btnLuu);

        // --- Lich su cac buoi ---
        JPanel lichSuWrap = new JPanel(new BorderLayout());
        lichSuWrap.setBackground(UIUtils.WHITE);
        lichSuWrap.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIUtils.BORDER), new EmptyBorder(10, 22, 10, 22)));

        JLabel lblLichSu = new JLabel("Lịch sử các buổi đã ghi (nhấp đúp để sửa lại)");
        lblLichSu.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblLichSu.setBorder(new EmptyBorder(0, 0, 8, 0));
        lichSuWrap.add(lblLichSu, BorderLayout.NORTH);

        String[] columns = {"Ngày học", "Nội dung đã dạy", "Ghi chú"};
        lichSuModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(lichSuModel);
        UIUtils.styleTable(table);
        table.setRowHeight(34);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(320);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row < 0) return;
                    String ngayStr = (String) lichSuModel.getValueAt(row, 0);
                    try {
                        dateSpinner.setValue(DF.parse(ngayStr));
                    } catch (Exception ignore) {}
                }
            }
        });

        JScrollPane scrollLichSu = new JScrollPane(table);
        scrollLichSu.setPreferredSize(new Dimension(100, 180));
        lichSuWrap.add(scrollLichSu, BorderLayout.CENTER);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(UIUtils.WHITE);
        centerWrap.add(form, BorderLayout.NORTH);
        centerWrap.add(lichSuWrap, BorderLayout.CENTER);
        add(centerWrap, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIUtils.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIUtils.BORDER), new EmptyBorder(12, 22, 12, 22)));
        JButton btnDong = UIUtils.createSecondaryBtn("Đóng");
        btnDong.addActionListener(e -> dispose());
        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnBox.setBackground(UIUtils.WHITE);
        btnBox.add(btnDong);
        footer.add(btnBox, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        loadFormChoNgayDaChon();
        loadLichSu();
    }

    private Date getSelectedSqlDate() {
        java.util.Date d = (java.util.Date) dateSpinner.getValue();
        return new Date(d.getTime());
    }

    private void loadFormChoNgayDaChon() {
        Object[] info = service.getSoDauBai(maLHP, getSelectedSqlDate());
        if (info != null) {
            txtNoiDung.setText(info[0] != null ? (String) info[0] : "");
            txtGhiChu.setText(info[1] != null ? (String) info[1] : "");
        } else {
            txtNoiDung.setText("");
            txtGhiChu.setText("");
        }
    }

    private void loadLichSu() {
        lichSuModel.setRowCount(0);
        List<Object[]> ds = service.getLichSuSoDauBai(maLHP);
        for (Object[] row : ds) {
            java.util.Date ngay = (java.util.Date) row[0];
            String noiDung = row[1] != null ? (String) row[1] : "";
            String ghiChu = row[2] != null ? (String) row[2] : "";
            lichSuModel.addRow(new Object[]{ DF.format(ngay), noiDung, ghiChu });
        }
    }

    private void luu() {
        try {
            service.luuSoDauBai(maGV, maLHP, getSelectedSqlDate(), txtNoiDung.getText(), txtGhiChu.getText());
            JOptionPane.showMessageDialog(this, "Đã lưu sổ đầu bài cho ngày " + DF.format((java.util.Date) dateSpinner.getValue()) + "!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            loadLichSu();
        } catch (BusinessLogicException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Có lỗi xảy ra", JOptionPane.WARNING_MESSAGE);
        }
    }
}
