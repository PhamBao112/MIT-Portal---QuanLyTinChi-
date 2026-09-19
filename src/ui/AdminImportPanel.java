package ui;

import service.StudentManagerService;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AdminImportPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private StudentManagerService service;
    private JTextArea txtLogs;

    public AdminImportPanel(StudentManagerService service) {
        this.service = service;
        setLayout(new BorderLayout(20, 0));
        setBackground(UIUtils.BG_APP);
        setBorder(new EmptyBorder(20, 0, 20, 0));

        JPanel importBox = UIUtils.createCardShell("Nạp Dữ Liệu Excel (.xlsx)", UIUtils.MIT_RED);
        importBox.setPreferredSize(new Dimension(400, 0));

        JPanel importContent = new JPanel();
        importContent.setLayout(new BoxLayout(importContent, BoxLayout.Y_AXIS));
        importContent.setOpaque(false);
        importContent.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel lblSub = new JLabel("<html>Hệ thống cho phép nạp hàng loạt dữ liệu<br>từ tệp Microsoft Excel. Hãy đảm bảo file<br>đúng định dạng cột.</html>");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSub.setForeground(UIUtils.TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnSV = UIUtils.createPrimaryBtn("1. Nạp file Sinh viên");
        JButton btnGV = UIUtils.createSecondaryBtn("2. Nạp file Giảng viên");

        btnSV.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnGV.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSV.setMaximumSize(new Dimension(350, 46));
        btnGV.setMaximumSize(new Dimension(350, 46));

        btnSV.addActionListener(e -> processImport(true));
        btnGV.addActionListener(e -> processImport(false));

        importContent.add(lblSub);
        importContent.add(Box.createVerticalStrut(30));
        importContent.add(btnSV);
        importContent.add(Box.createVerticalStrut(15));
        importContent.add(btnGV);
        importBox.add(importContent, BorderLayout.CENTER);

        JPanel logBox = UIUtils.createCardShell("Hệ Thống Ghi Nhận (Logs)", UIUtils.MIT_ORANGE);

        txtLogs = new JTextArea("Chưa có tiến trình nào đang chạy...\n");
        txtLogs.setEditable(false);
        txtLogs.setFont(new Font("Consolas", Font.PLAIN, 15));
        txtLogs.setBackground(new Color(15, 23, 42));
        txtLogs.setForeground(new Color(52, 211, 153));
        txtLogs.setBorder(new EmptyBorder(15, 20, 15, 20));

        JScrollPane logScroll = new JScrollPane(txtLogs);
        logScroll.setBorder(BorderFactory.createEmptyBorder());

        logBox.add(logScroll, BorderLayout.CENTER);

        add(importBox, BorderLayout.WEST);
        add(logBox, BorderLayout.CENTER);
    }

    private void processImport(boolean isSinhVien) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(isSinhVien ? "Chọn file Excel Danh sách Sinh viên" : "Chọn file Excel Danh sách Giảng viên");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getAbsolutePath();
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());

            txtLogs.append("\n[" + time + "] Đang phân tích file: " + fc.getSelectedFile().getName() + "...\n");
            try {
                String msg = isSinhVien ? service.importSinhVienFromExcel(path) : service.importGiangVienFromExcel(path);
                txtLogs.append("[" + time + "] HOÀN TẤT:\n" + msg + "\n");
            } catch (Exception ex) {
                txtLogs.append("[" + time + "] LỖI NGHIÊM TRỌNG: " + ex.getMessage() + "\n");
            }
        }
    }
}
