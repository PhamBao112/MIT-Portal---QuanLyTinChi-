package ui;

import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Man hinh lich day cua Giang vien - hien duoi dang bang (Thu/Tiet/Phong/Mon/Hoc ky)
 * thay vi luoi tuan nhu LichHocPanel cua SV, de don gian va de doc nhanh danh sach
 * nhieu lop cung luc.
 */
public class GVLichDayPanel extends JPanel {

    private final StudentManagerService service;
    private final String maGV;
    private DefaultTableModel tableModel;

    public GVLichDayPanel(StudentManagerService service, String maGV) {
        this.service = service;
        this.maGV = maGV;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(10, 0, 0, 0));

        buildUI();
    }

    private void buildUI() {
        JLabel lblTitle = new JLabel("Lịch Giảng Dạy");
        lblTitle.setFont(UIUtils.FONT_TITLE);

        JPanel tableContainer = UIUtils.createCardShell("Danh sách buổi dạy theo lớp học phần", UIUtils.MIT_RED);

        String[] columns = {"Thứ", "Tiết học", "Phòng", "Môn học", "Mã LHP", "Học kỳ"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(tableModel);
        UIUtils.styleTable(table);
        table.setRowHeight(42);

        loadData();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new MatteBorder(1, 0, 0, 0, UIUtils.BORDER));
        tableContainer.add(scroll, BorderLayout.CENTER);

        JPanel northWrap = new JPanel(new BorderLayout());
        northWrap.setBackground(UIUtils.BG_APP);
        northWrap.setBorder(new EmptyBorder(0, 0, 15, 0));
        northWrap.add(lblTitle, BorderLayout.WEST);

        add(northWrap, BorderLayout.NORTH);
        add(tableContainer, BorderLayout.CENTER);
    }

    private void loadData() {
        tableModel.setRowCount(0);
        List<Object[]> ds = service.getLopHocPhanByGV(maGV);
        // Sap xep theo Thu de de theo doi lich trong tuan
        ds.sort((a, b) -> {
            String thuA = a[7] != null ? a[7].toString() : "";
            String thuB = b[7] != null ? b[7].toString() : "";
            return thuA.compareTo(thuB);
        });
        for (Object[] row : ds) {
            // row: MaLHP, TenMon, SoTinChi, MaHK, TenHK, SucChua, SiSo, Thu, TietHoc, PhongHoc
            String maLHP = (String) row[0];
            String tenMon = (String) row[1];
            String tenHK = row[4] != null ? (String) row[4] : (String) row[3];
            String thu = row[7] != null ? (String) row[7] : "Chưa xếp lịch";
            String tiet = row[8] != null ? (String) row[8] : "-";
            String phong = row[9] != null ? (String) row[9] : "Chưa xếp";

            tableModel.addRow(new Object[]{ thu, tiet, phong, tenMon, maLHP, tenHK });
        }
    }
}
