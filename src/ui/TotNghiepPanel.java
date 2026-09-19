package ui;

import service.StudentManagerService;
import utils.UIUtils;
import config.DBConnect;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TotNghiepPanel extends JPanel {
    private String currentMaSV;

    // --- CÁC BIẾN LƯU TRỮ DỮ LIỆU ĐỂ XÉT ĐIỀU KIỆN ---
    private String hoTen = "Đang tải...";
    private int tinChiTichLuy = 0;
    private int tinChiYeuCau = 150;
    private double tongNo = 0.0;
    private boolean hasNoHocPhi = false;
    private boolean isDatNgoaiNgu = false;

    public TotNghiepPanel(StudentManagerService service, String maSV) {
        this.currentMaSV = maSV;

        // 1. Tải dữ liệu thật từ DB lên trước khi vẽ UI
        loadData();

        // 2. Vẽ giao diện dựa trên đoạn code của bạn
        setLayout(new BorderLayout());
        setBackground( Color.WHITE);
        setBorder(new LineBorder(UIUtils.BORDER, 1, true));
        add(buildSectionHeader("Thẩm Định Điều Kiện Tốt Nghiệp"), BorderLayout.NORTH);

        boolean tcOk  = (tinChiTichLuy >= tinChiYeuCau);
        boolean noOk  = !hasNoHocPhi;
        boolean nnOk  = isDatNgoaiNgu;
        boolean allOk = tcOk && noOk && nnOk;
        int metCount  = (tcOk ? 1 : 0) + (noOk ? 1 : 0) + (nnOk ? 1 : 0);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UIUtils.BG_APP);
        body.setBorder(new EmptyBorder(25, 30, 30, 30));

        // ─── Banner trang thai tong ────────────────────────────────
        Color sBg  = allOk ? new Color(240,253,244) : new Color(254,242,242);
        Color sAcc = allOk ? new Color(22,101,52)   : new Color(185,28,28);
        Color sBdr = allOk ? new Color(134,239,172) : new Color(252,165,165);

        JPanel banner = new JPanel(new BorderLayout(16, 0));
        banner.setBackground(sBg);
        banner.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(sBdr, 1, true), new EmptyBorder(14, 20, 14, 20)));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icoLbl = new JLabel(allOk ? "[OK]" : "[!!]");
        icoLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        icoLbl.setForeground(sAcc);

        JPanel bannerTxt = new JPanel();
        bannerTxt.setLayout(new BoxLayout(bannerTxt, BoxLayout.Y_AXIS));
        bannerTxt.setBackground(sBg);

        JLabel lblBT = new JLabel(allOk ? "Đủ điều kiện tốt nghiệp (Sơ bộ)"
                                        : "Chưa đủ điều kiện - cần bổ sung thêm");
        lblBT.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblBT.setForeground(sAcc);

        JLabel lblBS = new JLabel("Đã đáp ứng " + metCount + "/3 điều kiện  -  "
                                  + hoTen + "  (" + currentMaSV + ")");
        lblBS.setFont(UIUtils.FONT_NORMAL);
        lblBS.setForeground(UIUtils.TEXT_MUTED);

        bannerTxt.add(lblBT);
        bannerTxt.add(Box.createVerticalStrut(3));
        bannerTxt.add(lblBS);
        banner.add(icoLbl,    BorderLayout.WEST);
        banner.add(bannerTxt, BorderLayout.CENTER);
        body.add(banner);
        body.add(Box.createVerticalStrut(20));

        // ─── 3 the dieu kien ──────────────────────────────────────
        JPanel condRow = new JPanel(new GridLayout(1, 3, 15, 0));
        condRow.setBackground(UIUtils.BG_APP);
        condRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 175));
        condRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        condRow.add(buildCondCard(
            "1. Tín chỉ tích lũy",
            "Yêu cầu >= " + tinChiYeuCau + " tín chỉ",
            tinChiTichLuy + " / " + tinChiYeuCau + " TC",
            tcOk, tinChiTichLuy, tinChiYeuCau));

        condRow.add(buildCondCard(
            "2. Nghĩa vụ tài chính",
            "Không còn nợ học phí",
            noOk ? "Đã hoàn tất học phí" : String.format("Còn nợ %,.0f đ", tongNo),
            noOk, -1, -1));

        condRow.add(buildCondCard(
            "3. Chuẩn đầu ra ngoại ngữ",
            "Đạt chứng chỉ theo quy định",
            nnOk ? "Đã đạt chuẩn" : "Chưa đạt chuẩn",
            nnOk, -1, -1));

        body.add(condRow);
        body.add(Box.createVerticalStrut(25));

        // ─── Nut gui yeu cau ──────────────────────────────────────
        JButton btnXet = UIUtils.createPrimaryBtn("  GỬI YÊU CẦU XÉT DUYỆT CHÍNH THỨC");
        btnXet.setMaximumSize(new Dimension(440, 46));
        btnXet.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(btnXet);
        body.add(Box.createVerticalStrut(18));

        // ─── Panel ket qua (an, hien sau khi bam nut) ────────────
        Color rBg  = allOk ? new Color(240,253,244) : new Color(254,242,242);
        Color rBdr = allOk ? new Color(134,239,172) : new Color(252,165,165);

        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBackground(rBg);
        resultPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(rBdr, 1, true), new EmptyBorder(18, 22, 18, 22)));
        resultPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        resultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultPanel.setVisible(false);

        btnXet.addActionListener(e -> {
            btnXet.setEnabled(false);
            btnXet.setText("Đang xét duyệt...");

            new Thread(() -> {
                // Goi service de lay ket qua day du tu DB
                String rawResult = service.checkGraduation(currentMaSV);
                String ts = new java.text.SimpleDateFormat("dd/MM/yyyy  HH:mm:ss")
                                  .format(new java.util.Date());

                // Giả lập delay 1 xíu cho đẹp mắt
                try { Thread.sleep(600); } catch (Exception ex) {}

                SwingUtilities.invokeLater(() -> {
                    // FIX: truoc day dung bien allOk tinh 1 lan luc khoi tao panel, trong khi rawResult
                    // lay tu DB ngay luc bam nut -> neu SV vua thanh toan hoc phi o tab Cong No roi
                    // quay sang day, tieu de van bao "CHUA DU DIEU KIEN" du chi tiet ben duoi noi da du.
                    // Gio lay ket luan TRUC TIEP tu ket qua service vua tra ve.
                    boolean resultOk = rawResult.contains("KẾT LUẬN: ĐỦ ĐIỀU KIỆN");
                    Color rcBg  = resultOk ? new Color(240,253,244) : new Color(254,242,242);
                    Color rcBdr = resultOk ? new Color(134,239,172) : new Color(252,165,165);
                    resultPanel.setBackground(rcBg);
                    resultPanel.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(rcBdr, 1, true), new EmptyBorder(18, 22, 18, 22)));

                    JPanel rc = new JPanel();
                    rc.setLayout(new BoxLayout(rc, BoxLayout.Y_AXIS));
                    rc.setBackground(rcBg);

                    // Tieu de ket qua
                    JLabel lblRT = new JLabel(
                        (resultOk ? "[OK]  ĐỦ ĐIỀU KIỆN TỐT NGHIỆP"
                                  : "[XX]  CHƯA ĐỦ ĐIỀU KIỆN TỐT NGHIỆP"));
                    lblRT.setFont(new Font("Segoe UI", Font.BOLD, 15));
                    lblRT.setForeground(resultOk ? new Color(22,101,52) : new Color(185,28,28));
                    lblRT.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JSeparator sep2 = new JSeparator();
                    sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    sep2.setForeground(rcBdr);
                    sep2.setAlignmentX(Component.LEFT_ALIGNMENT);

                    // Chi tiet ket qua dang text (tu service tra ve)
                    JTextArea txtDetail = new JTextArea(rawResult);
                    txtDetail.setFont(new Font("Consolas", Font.PLAIN, 14));
                    txtDetail.setEditable(false);
                    txtDetail.setBackground(rcBg);
                    txtDetail.setForeground(UIUtils.TEXT_MAIN);
                    txtDetail.setBorder(BorderFactory.createEmptyBorder());
                    txtDetail.setLineWrap(true);
                    txtDetail.setWrapStyleWord(true);
                    txtDetail.setAlignmentX(Component.LEFT_ALIGNMENT);

                    // Timestamp + Ma SV
                    JLabel lblTs = new JLabel(
                        "Xét duyệt lúc: " + ts + "  |  Mã SV: " + currentMaSV);
                    lblTs.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                    lblTs.setForeground(UIUtils.TEXT_MUTED);
                    lblTs.setAlignmentX(Component.LEFT_ALIGNMENT);

                    rc.add(lblRT);
                    rc.add(Box.createVerticalStrut(10));
                    rc.add(sep2);
                    rc.add(Box.createVerticalStrut(10));
                    rc.add(txtDetail);
                    rc.add(Box.createVerticalStrut(10));
                    rc.add(lblTs);

                    resultPanel.removeAll();
                    resultPanel.add(rc, BorderLayout.CENTER);
                    resultPanel.setVisible(true);
                    resultPanel.revalidate();
                    resultPanel.repaint();

                    // FIX: cho phep bam xet lai nhieu lan (truoc day nut bi khoa vinh vien sau 1 lan bam,
                    // SV thanh toan hoc phi xong khong the xet lai ma phai dang xuat vao lai)
                    btnXet.setEnabled(true);
                    btnXet.setText(resultOk
                        ? "[OK]  Đã đủ điều kiện - xem kết quả bên dưới"
                        : "[!!]  Chưa đủ - xem chi tiết bên dưới");
                });
            }).start();
        });

        body.add(resultPanel);
        body.add(Box.createVerticalGlue());

        add(body, BorderLayout.CENTER);
    }

    // ============================================================
    // LẤY DỮ LIỆU TỪ DATABASE
    // ============================================================
    private void loadData() {
        try (Connection conn = DBConnect.getConnection()) {
            if (conn == null) return;
            
            // 1. Lấy Họ Tên & Chuẩn đầu ra ngoại ngữ
            String sql1 = "SELECT HoTen, DatChuanNgoaiNgu FROM SINH_VIEN WHERE MaSV = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setString(1, currentMaSV);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    hoTen = rs.getString("HoTen");
                    isDatNgoaiNgu = rs.getBoolean("DatChuanNgoaiNgu");
                }
            }

            // 2. Lấy Tín chỉ tích lũy
            String sql2 = "SELECT SUM(CAST(m.SoTinChi AS INT)) FROM KET_QUA_DANG_KY kq JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE kq.MaSV = ? AND kq.TrangThai = N'Đạt'";
            try (PreparedStatement ps = conn.prepareStatement(sql2)) {
                ps.setString(1, currentMaSV);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) tinChiTichLuy = rs.getInt(1);
            }

            // 3. Lay Thong tin No hoc phi
            // FIX: bo dieu kien loc theo chuoi TrangThai, chi cong nhung phieu thuc su con thieu tien,
            // de khop voi cach TruongHocDAO.checkNoHocPhi() xet (tranh the xanh "Da hoan tat" nhung
            // ket luan lai do "Con no hoc phi").
            String sql3 = "SELECT ISNULL(SUM(CAST(TongTienPhaiDong AS FLOAT) - CAST(SoTienDaDong AS FLOAT)), 0) " +
                          "FROM CONG_NO_HOC_PHI WHERE MaSV = ? " +
                          "AND CAST(TongTienPhaiDong AS FLOAT) - CAST(SoTienDaDong AS FLOAT) > 0";
            try (PreparedStatement ps = conn.prepareStatement(sql3)) {
                ps.setString(1, currentMaSV);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    tongNo = rs.getDouble(1);
                    hasNoHocPhi = (tongNo > 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // HELPER: THE DIEU KIEN TOT NGHIEP
    // passed = true  → xanh la
    // passed = false → do
    // current/max   >= 0 thi ve progress bar (chi cho dieu kien TC)
    // ============================================================
    private JPanel buildCondCard(String title, String requirement,
                                 String statusText, boolean passed,
                                 int current, int max) {
        Color bg  = passed ? new Color(240,253,244) : new Color(254,242,242);
        Color acc = passed ? new Color(22,101,52)   : new Color(185,28,28);
        Color bdr = passed ? new Color(134,239,172) : new Color(252,165,165);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(bdr, 1, true), new EmptyBorder(16, 18, 16, 18)));

        // Header: ten + icon [OK]/[X]
        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setBackground(bg);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(UIUtils.TEXT_MAIN);

        JLabel lblIco = new JLabel(passed ? "[OK]" : "[ X]");
        lblIco.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblIco.setForeground(acc);

        topRow.add(lblTitle, BorderLayout.CENTER);
        topRow.add(lblIco,   BorderLayout.EAST);

        // Body
        JPanel cardBody = new JPanel();
        cardBody.setLayout(new BoxLayout(cardBody, BoxLayout.Y_AXIS));
        cardBody.setBackground(bg);

        JLabel lblReq = new JLabel(requirement);
        lblReq.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblReq.setForeground(UIUtils.TEXT_MUTED);
        lblReq.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblStat = new JLabel(statusText);
        lblStat.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblStat.setForeground(acc);
        lblStat.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardBody.add(Box.createVerticalStrut(10));
        cardBody.add(lblReq);

        // Progress bar chi danh cho dieu kien tin chi
        if (current >= 0 && max > 0) {
            cardBody.add(Box.createVerticalStrut(8));
            JProgressBar bar = new JProgressBar(0, max);
            bar.setValue(Math.min(current, max));
            bar.setStringPainted(false);
            bar.setForeground(passed ? new Color(34,197,94) : new Color(239,68,68));
            bar.setBackground(passed ? new Color(220,252,231) : new Color(254,226,226));
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
            bar.setBorderPainted(false);
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardBody.add(bar);
        }

        cardBody.add(Box.createVerticalStrut(8));
        cardBody.add(lblStat);

        card.add(topRow,   BorderLayout.NORTH);
        card.add(cardBody, BorderLayout.CENTER);
        return card;
    }
    private JPanel buildSectionHeader(String title) {
        JPanel h = new JPanel(new BorderLayout());
        
        // =========================================================
        // ĐÃ SỬA TÊN BIẾN THÀNH CHỮ 'h' CHO KHỚP VỚI FILE CỦA BẠN
        // =========================================================
        h.setBackground(UIUtils.MIT_RED_LIGHT);
        h.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIUtils.MIT_RED),
            new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel l = new JLabel(title);
        l.setFont(UIUtils.FONT_TITLE);
        
        // =========================================================
        // ĐÃ SỬA TÊN BIẾN THÀNH CHỮ 'l' CHO KHỚP VỚI FILE CỦA BẠN
        // =========================================================
        l.setForeground(UIUtils.MIT_RED);
        
        h.add(l, BorderLayout.WEST);
        return h;
    }
}