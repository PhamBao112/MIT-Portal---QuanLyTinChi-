package dao;

import config.DBConnect;
import entity.LopHocPhan;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TruongHocDAO {
    
    // ==========================================================
    // 1. NGHIỆP VỤ ĐĂNG KÝ HỌC PHẦN
    // ==========================================================
    
    public LopHocPhan findLopHocPhanById(String maLHP) {
        // Cập nhật câu lệnh SQL: SELECT thêm cột lhp.MaHK
        String sql = "SELECT lhp.MaLHP, lhp.MaMon, lhp.SucChua, m.SoTinChi, lhp.MaHK FROM LOP_HOC_PHAN lhp JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE lhp.MaLHP = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLHP); ResultSet rs = ps.executeQuery();
            if (rs.next()) return new LopHocPhan(rs.getString("MaLHP"), rs.getString("MaMon"), rs.getInt("SucChua"), rs.getInt("SoTinChi"), rs.getString("MaHK")); // Truyền thêm MaHK
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public int countSinhVienDaDangKy(String maLHP) {
        String sql = "SELECT COUNT(*) FROM KET_QUA_DANG_KY WHERE MaLHP = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLHP); ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public String getMonTienQuyet(String maMon) {
        String sql = "SELECT MaMonTQ FROM MON_TIEN_QUYET WHERE MaMon = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maMon); ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("MaMonTQ");
        } catch (SQLException e) { e.printStackTrace(); }
        return null; 
    }

    public boolean checkSinhVienDaDatMon(String maSV, String maMon) {
        String sql = "SELECT COUNT(*) FROM KET_QUA_DANG_KY kq JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP WHERE kq.MaSV = ? AND lhp.MaMon = ? AND kq.TrangThai = N'Đạt'";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV); ps.setString(2, maMon); ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // FIX: ham moi ho tro checkDaDangKyLop() trong registerCourse() - ngan chan dang ky trung
    // (goc re cua loi lich hoc hien trung 3 lan 1 lop).
    public boolean checkDaDangKyLop(String maSV, String maLHP) {
        String sql = "SELECT COUNT(*) FROM KET_QUA_DANG_KY WHERE MaSV = ? AND MaLHP = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV); ps.setString(2, maLHP); ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // FIX (yeu cau 3): lay Thu + TietHoc cua 1 lop hoc phan, phuc vu kiem tra trung buoi
    // truoc khi cho dang ky (LopHocPhan entity hien khong co field Thu/TietHoc).
    public String[] getThuVaTietHoc(String maLHP) {
        String sql = "SELECT Thu, TietHoc FROM LOP_HOC_PHAN WHERE MaLHP = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLHP); ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{rs.getString("Thu"), rs.getString("TietHoc")};
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // FIX (yeu cau 3): RANG BUOC THAT O TANG DU LIEU - 1 mon phai chiem tron 1 buoi (Sang/Chieu),
    // khong duoc nhoi 2 mon KHAC NHAU vao chung 1 buoi cung ngay. Truoc day chi co logic UI
    // (LichHocPanel tu chon mon co tiet som nhat de hien) - kieu vá đó CHỈ ẨN mon con lai khoi
    // man hinh chu KHONG XOA khoi KET_QUA_DANG_KY, khien tin chi/hoc phi van tinh SAI (dem ca
    // mon bi an). Gio chan NGAY TU LUC DANG KY, khong cho phat sinh du lieu xung dot nua.
    public boolean checkTrungBuoi(String maSV, String maHK, String thuMoi, int tietBatDauMoi) {
        if (thuMoi == null) return false;
        boolean buoiSangMoi = tietBatDauMoi <= 6;
        String sql = "SELECT lhp.TietHoc FROM KET_QUA_DANG_KY kq " +
                     "JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP " +
                     "WHERE kq.MaSV = ? AND lhp.MaHK = ? AND lhp.Thu = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV); ps.setString(2, maHK); ps.setString(3, thuMoi);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String tiet = rs.getString("TietHoc");
                if (tiet == null || !tiet.contains("-")) continue;
                try {
                    int bd = Integer.parseInt(tiet.split("-")[0].trim());
                    boolean buoiSangCu = bd <= 6;
                    if (buoiSangCu == buoiSangMoi) return true; // da co 1 mon khac trung buoi nay roi
                } catch (NumberFormatException ignore) {}
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public void insertKetQuaDangKy(String maSV, String maLHP, String trangThai) {
        String sql = "INSERT INTO KET_QUA_DANG_KY (MaSV, MaLHP, TrangThai) VALUES (?, ?, ?)";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV); ps.setString(2, maLHP); ps.setString(3, trangThai); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Lỗi Insert: " + e.getMessage()); }
    }
    
    // HÀM MỚI: XÓA KẾT QUẢ ĐĂNG KÝ KHI HỦY MÔN
    public void deleteKetQuaDangKy(String maSV, String maLHP) throws Exception {
        String sql = "DELETE FROM KET_QUA_DANG_KY WHERE MaSV = ? AND MaLHP = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV);
            ps.setString(2, maLHP);
            ps.executeUpdate();
        }
    }

    // ==========================================================
    // 2. NGHIỆP VỤ CÔNG NỢ HỌC PHÍ (Bao gồm chức năng cho CongNoPanel)
    // ==========================================================
    
    // Lấy tổng số tín chỉ đã đăng ký trong 1 học kỳ để tính ra tiền
    public int sumTinChiTrongKy(String maSV, String maHK) {
        String sql = "SELECT SUM(CAST(m.SoTinChi AS INT)) FROM KET_QUA_DANG_KY kq JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE kq.MaSV = ? AND lhp.MaHK = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV); ps.setString(2, maHK); ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // Tạo phiếu thu nợ mới
    public void insertCongNo(String maPhieu, String maSV, String maHK, double tongTien) {
        // Dùng IF EXISTS của SQL Server để kiểm tra: Có rồi thì UPDATE, chưa có thì INSERT
        String sql = "IF EXISTS (SELECT * FROM CONG_NO_HOC_PHI WHERE MaPhieu = ?) " +
                     "BEGIN " +
                     "   UPDATE CONG_NO_HOC_PHI SET TongTienPhaiDong = ? WHERE MaPhieu = ? " +
                     "END " +
                     "ELSE " +
                     "BEGIN " +
                     "   INSERT INTO CONG_NO_HOC_PHI (MaPhieu, MaSV, MaHK, TongTienPhaiDong, SoTienDaDong, TrangThai) " +
                     "   VALUES (?, ?, ?, ?, 0, N'Chưa đóng') " +
                     "END";
                     
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            // Tham số cho lệnh UPDATE
            ps.setString(1, maPhieu);
            ps.setDouble(2, tongTien);
            ps.setString(3, maPhieu);
            
            // Tham số cho lệnh INSERT
            ps.setString(4, maPhieu);
            ps.setString(5, maSV);
            ps.setString(6, maHK);
            ps.setDouble(7, tongTien);
            
            ps.executeUpdate();
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
    }

    // Lấy danh sách Học Kỳ từ Database để nạp vào ComboBox
    public List<String> getAllHocKy() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT MaHK, TenHK FROM HOC_KY ORDER BY NamHoc DESC, MaHK ASC";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("MaHK") + " - " + rs.getString("TenHK"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // Lấy danh sách học kỳ MÀ SINH VIÊN NÀY THỰC SỰ CÓ ĐĂNG KÝ (không hiện các kỳ trước khi SV nhập học)
    public List<String> getHocKyCuaSinhVien(String maSV) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT h.MaHK, h.TenHK, h.NamHoc " +
                     "FROM HOC_KY h " +
                     "JOIN LOP_HOC_PHAN lhp ON lhp.MaHK = h.MaHK " +
                     "JOIN KET_QUA_DANG_KY kq ON kq.MaLHP = lhp.MaLHP " +
                     "WHERE kq.MaSV = ? " +
                     "ORDER BY h.NamHoc DESC, h.MaHK DESC";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("MaHK") + " - " + rs.getString("TenHK"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // Lấy MaHK gần đây nhất mà sinh viên có đăng ký (dùng cho Trang Chủ Tổng Quan - không cần combo chọn)
    public String getHocKyGanNhatCuaSinhVien(String maSV) {
        List<String> list = getHocKyCuaSinhVien(maSV);
        if (list.isEmpty()) return null;
        return list.get(0).split("-")[0].trim();
    }

    // Lấy thông tin phiếu thu (Tổng tiền, đã đóng) của SV theo Học kỳ
    public double[] getThongTinCongNo(String maSV, String maHK) {
        double[] info = new double[]{-1.0, 0.0}; 
        String sql = "SELECT TongTienPhaiDong, SoTienDaDong FROM CONG_NO_HOC_PHI WHERE MaSV = ? AND MaHK = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV); ps.setString(2, maHK);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                info[0] = rs.getDouble("TongTienPhaiDong");
                info[1] = rs.getDouble("SoTienDaDong");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return info; 
    }

    // NGHIỆP VỤ MỚI: Lấy toàn bộ lịch sử công nợ của Sinh viên để hiển thị lên Bảng
    // FIX #3: trả về List thay vì ResultSet sống - trước đây Connection không bao giời được đóng (connection leak)
    public List<Object[]> getLichSuCongNo(String maSV) {
        List<Object[]> ket = new ArrayList<>();
        String sql = "SELECT c.MaHK, h.TenHK, c.TongTienPhaiDong, c.SoTienDaDong, c.TrangThai " +
                     "FROM CONG_NO_HOC_PHI c " +
                     "LEFT JOIN HOC_KY h ON c.MaHK = h.MaHK " +
                     "WHERE c.MaSV = ? ORDER BY c.MaHK DESC";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ket.add(new Object[]{
                        rs.getString("MaHK"), rs.getString("TenHK"),
                        rs.getDouble("TongTienPhaiDong"), rs.getDouble("SoTienDaDong"), rs.getString("TrangThai")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ket;
    }

    // Lấy chi tiết các môn học đã đăng ký trong kỳ đó để đổ vào JTable
    // FIX #3: trả về List thay vì ResultSet sống - trước đây Connection không bao giời được đóng (connection leak)
    public List<Object[]> getChiTietDangKyTrongKy(String maSV, String maHK) {
        List<Object[]> ket = new ArrayList<>();
        String sql = "SELECT m.MaMon, m.TenMon, m.SoTinChi " +
                     "FROM KET_QUA_DANG_KY kq " +
                     "JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP " +
                     "JOIN MON_HOC m ON lhp.MaMon = m.MaMon " +
                     "WHERE kq.MaSV = ? AND lhp.MaHK = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV);
            ps.setString(2, maHK);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ket.add(new Object[]{ rs.getString("MaMon"), rs.getString("TenMon"), rs.getInt("SoTinChi") });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ket;
    }

    // NGHIỆP VỤ MỚI: Cập nhật CSDL khi sinh viên bấm Thanh Toán
    public void updateThanhToanCongNo(String maSV, String maHK, double tongTien) {
        String sql = "UPDATE CONG_NO_HOC_PHI SET SoTienDaDong = ?, TrangThai = N'Đã hoàn thành' WHERE MaSV = ? AND MaHK = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, tongTien);
            ps.setString(2, maSV);
            ps.setString(3, maHK);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ==========================================================
    // 3. NGHIỆP VỤ XÉT TỐT NGHIỆP
    // ==========================================================
    
    // Tính tổng tín chỉ thực tế (Chỉ cộng những môn có trạng thái 'Đạt')
    public int getTongTinChiTichLuy(String maSV) { 
        String sql = "SELECT SUM(CAST(m.SoTinChi AS INT)) FROM KET_QUA_DANG_KY kq JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP JOIN MON_HOC m ON lhp.MaMon = m.MaMon WHERE kq.MaSV = ? AND kq.TrangThai = N'Đạt'";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV); ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0; 
    }

    public boolean checkNoHocPhi(String maSV) {
        // FIX: truoc day chi dem theo CHUOI TrangThai != N'Da hoan thanh', trong khi TotNghiepPanel
        // lai xet theo SO TIEN thuc te con no -> mau thuan ngay tren cung 1 man hinh (the hien
        // "Da hoan tat hoc phi" mau xanh nhung ket luan lai bao "Con no hoc phi" mau do).
        // Vi du sinh ra mau thuan: calculateTuition() tao phieu 0d voi TrangThai = N'Chua dong'.
        // Gio thong nhat: chi tinh la no khi SO TIEN con thieu > 0.
        String sql = "SELECT COUNT(*) FROM CONG_NO_HOC_PHI WHERE MaSV = ? " +
                     "AND CAST(TongTienPhaiDong AS FLOAT) - CAST(SoTienDaDong AS FLOAT) > 0";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV); ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean checkChuanNgoaiNgu(String maSV) {
        String sql = "SELECT DatChuanNgoaiNgu FROM SINH_VIEN WHERE MaSV = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV); ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // ==========================================================
    // 4. NGHIỆP VỤ IMPORT DATA TỪ FILE EXCEL
    // ==========================================================
    
    public void insertSinhVien(String maSV, String hoTen, String gioiTinh, String ngaySinh, String sdt, String email, String trangThai, String maCTDT, String maLop) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM SINH_VIEN WHERE MaSV = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setString(1, maSV); ResultSet rs = psCheck.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return; 
        }
        String sql = "INSERT INTO SINH_VIEN (MaSV, HoTen, GioiTinh, NgaySinh, SoDienThoai, Email, TrangThaiHocTap, DatChuanNgoaiNgu, MaCTDT, MaLop) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV); ps.setString(2, hoTen); ps.setString(3, gioiTinh); ps.setString(4, ngaySinh); ps.setString(5, sdt); ps.setString(6, email); ps.setString(7, trangThai); ps.setString(8, maCTDT); ps.setString(9, maLop); ps.executeUpdate();
        }
    }

    public void insertGiangVien(String maGV, String hoTen, String gioiTinh, String hocVi, String sdt, String email, String maKhoa) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM GIANG_VIEN WHERE MaGV = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setString(1, maGV); ResultSet rs = psCheck.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return; 
        }
        String sql = "INSERT INTO GIANG_VIEN (MaGV, HoTen, GioiTinh, HocVi, SoDienThoai, Email, MaKhoa) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maGV); ps.setString(2, hoTen); ps.setString(3, gioiTinh); ps.setString(4, hocVi); ps.setString(5, sdt); ps.setString(6, email); ps.setString(7, maKhoa); ps.executeUpdate();
        }
    }

    // ==========================================================
    // 5. NGHIỆP VỤ GIẢNG VIÊN (đăng nhập bằng Email + xem lớp + nhập điểm)
    // ==========================================================

    // Dùng để đăng nhập GV: tìm MaGV tương ứng với Email đã nhập (App.authenticateDB)
    public String getMaGVByEmail(String email) {
        String sql = "SELECT MaGV FROM GIANG_VIEN WHERE Email = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email); ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("MaGV");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // Thông tin hiển thị ở sidebar/profile card của GiangVienPanel
    public Object[] getThongTinGV(String maGV) {
        String sql = "SELECT HoTen, Email, HocVi FROM GIANG_VIEN WHERE MaGV = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maGV); ResultSet rs = ps.executeQuery();
            if (rs.next()) return new Object[]{ rs.getString("HoTen"), rs.getString("Email"), rs.getString("HocVi") };
        } catch (SQLException e) { e.printStackTrace(); }
        return new Object[]{ "Giảng viên", "", "" };
    }

    // Danh sách lớp học phần GV này phụ trách (kèm sĩ số thực tế qua subquery đếm KET_QUA_DANG_KY)
    public List<Object[]> getLopHocPhanByGV(String maGV) {
        List<Object[]> ket = new ArrayList<>();
        String sql = "SELECT lhp.MaLHP, m.TenMon, m.SoTinChi, lhp.MaHK, h.TenHK, lhp.SucChua, " +
                     "(SELECT COUNT(*) FROM KET_QUA_DANG_KY kq WHERE kq.MaLHP = lhp.MaLHP) AS SiSo, " +
                     "lhp.Thu, lhp.TietHoc, lhp.PhongHoc, lhp.NgayBatDauHoc, lhp.NgayKetThucHoc " +
                     "FROM LOP_HOC_PHAN lhp " +
                     "JOIN MON_HOC m ON lhp.MaMon = m.MaMon " +
                     "LEFT JOIN HOC_KY h ON lhp.MaHK = h.MaHK " +
                     "WHERE lhp.MaGV = ? " +
                     "ORDER BY lhp.MaHK DESC, lhp.MaLHP";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maGV);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ket.add(new Object[]{
                        rs.getString("MaLHP"), rs.getString("TenMon"), rs.getInt("SoTinChi"),
                        rs.getString("MaHK"), rs.getString("TenHK"), rs.getInt("SucChua"), rs.getInt("SiSo"),
                        rs.getString("Thu"), rs.getString("TietHoc"), rs.getString("PhongHoc"),
                        rs.getDate("NgayBatDauHoc"), rs.getDate("NgayKetThucHoc")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ket;
    }

    // Danh sách SV (kèm điểm hiện có) trong 1 lớp học phần - phục vụ màn hình Nhập điểm
    public List<Object[]> getSinhVienTrongLopHP(String maLHP) {
        List<Object[]> ket = new ArrayList<>();
        String sql = "SELECT sv.MaSV, sv.HoTen, kq.DiemChuyenCan, kq.DiemGiuaKy, kq.DiemCuoiKy, kq.DiemTongKet, kq.TrangThai " +
                     "FROM KET_QUA_DANG_KY kq " +
                     "JOIN SINH_VIEN sv ON kq.MaSV = sv.MaSV " +
                     "WHERE kq.MaLHP = ? ORDER BY sv.HoTen";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLHP);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ket.add(new Object[]{
                        rs.getString("MaSV"), rs.getString("HoTen"),
                        rs.getDouble("DiemChuyenCan"), rs.getDouble("DiemGiuaKy"), rs.getDouble("DiemCuoiKy"),
                        rs.getDouble("DiemTongKet"), rs.getString("TrangThai")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ket;
    }

    // CHẶN Ở TẦNG DỮ LIỆU: xác nhận GV này thực sự phụ trách lớp trước khi cho phép sửa điểm
    // (không tin tưởng vào UI - phòng trường hợp sau này có API/URL truyền MaLHP tùy ý).
    public boolean isGVPhuTrachLop(String maGV, String maLHP) {
        String sql = "SELECT COUNT(*) FROM LOP_HOC_PHAN WHERE MaLHP = ? AND MaGV = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLHP); ps.setString(2, maGV);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // Cập nhật điểm thành phần cho 1 SV trong 1 lớp học phần.
    // Tự tính DiemTongKet (CC 10% + GK 30% + CK 60% - có thể điều chỉnh hệ số tại đây)
    // và TrangThai (ngưỡng đạt = 5.0/10, theo quy chế phổ biến).
    public void capNhatDiemSinhVien(String maSV, String maLHP, double chuyenCan, double giuaKy, double cuoiKy) {
        double tongKet = chuyenCan * 0.1 + giuaKy * 0.3 + cuoiKy * 0.6;
        tongKet = Math.round(tongKet * 100.0) / 100.0;
        String trangThai = tongKet >= 5.0 ? "Đạt" : "Không đạt";

        String sql = "UPDATE KET_QUA_DANG_KY SET DiemChuyenCan=?, DiemGiuaKy=?, DiemCuoiKy=?, DiemTongKet=?, TrangThai=? " +
                     "WHERE MaSV=? AND MaLHP=?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, chuyenCan);
            ps.setDouble(2, giuaKy);
            ps.setDouble(3, cuoiKy);
            ps.setDouble(4, tongKet);
            ps.setString(5, trangThai);
            ps.setString(6, maSV);
            ps.setString(7, maLHP);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ==========================================================
    // 6. NGHIỆP VỤ ĐIỂM DANH (Giảng viên điểm danh SV theo từng buổi học)
    // ==========================================================

    // Cap nhat / them moi 1 dong diem danh (upsert theo khoa chinh MaLHP+MaSV+NgayHoc)
    public void upsertDiemDanh(String maLHP, String maSV, Date ngayHoc, String trangThai, String ghiChu) {
        String sql = "IF EXISTS (SELECT 1 FROM DIEM_DANH WHERE MaLHP=? AND MaSV=? AND NgayHoc=?) " +
                     "UPDATE DIEM_DANH SET TrangThai=?, GhiChu=? WHERE MaLHP=? AND MaSV=? AND NgayHoc=? " +
                     "ELSE INSERT INTO DIEM_DANH (MaLHP, MaSV, NgayHoc, TrangThai, GhiChu) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLHP);  ps.setString(2, maSV);  ps.setDate(3, ngayHoc);
            ps.setString(4, trangThai); ps.setString(5, ghiChu);
            ps.setString(6, maLHP);  ps.setString(7, maSV);  ps.setDate(8, ngayHoc);
            ps.setString(9, maLHP);  ps.setString(10, maSV); ps.setDate(11, ngayHoc);
            ps.setString(12, trangThai); ps.setString(13, ghiChu);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // Lay diem danh da luu cho 1 lop trong 1 ngay cu the (de hien thi lai khi GV mo lai form diem danh)
    // Key cua Map la MaSV, value la {TrangThai, GhiChu}
    public Map<String, Object[]> getDiemDanhTheoNgay(String maLHP, Date ngayHoc) {
        Map<String, Object[]> ket = new HashMap<>();
        String sql = "SELECT MaSV, TrangThai, GhiChu FROM DIEM_DANH WHERE MaLHP = ? AND NgayHoc = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLHP); ps.setDate(2, ngayHoc);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ket.put(rs.getString("MaSV"), new Object[]{ rs.getString("TrangThai"), rs.getString("GhiChu") });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ket;
    }

    // Thong ke diem danh theo tung SV trong 1 lop: tong so buoi da diem danh, so vang, so vang co phep.
    // Dung LEFT JOIN tu KET_QUA_DANG_KY (danh sach SV cua lop) sang DIEM_DANH, nen SV chua duoc
    // diem danh buoi nao van hien trong ket qua voi TongBuoi = 0 (khong bi mat khoi danh sach).
    public List<Object[]> getThongKeDiemDanhTheoLop(String maLHP) {
        List<Object[]> ket = new ArrayList<>();
        String sql = "SELECT sv.MaSV, sv.HoTen, " +
                     "COUNT(dd.NgayHoc) AS TongBuoi, " +
                     "SUM(CASE WHEN dd.TrangThai = N'Vắng' THEN 1 ELSE 0 END) AS SoVang, " +
                     "SUM(CASE WHEN dd.TrangThai = N'Vắng có phép' THEN 1 ELSE 0 END) AS SoVangCoPhep " +
                     "FROM KET_QUA_DANG_KY kq " +
                     "JOIN SINH_VIEN sv ON kq.MaSV = sv.MaSV " +
                     "LEFT JOIN DIEM_DANH dd ON dd.MaLHP = kq.MaLHP AND dd.MaSV = kq.MaSV " +
                     "WHERE kq.MaLHP = ? " +
                     "GROUP BY sv.MaSV, sv.HoTen " +
                     "ORDER BY sv.HoTen";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLHP);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ket.add(new Object[]{
                        rs.getString("MaSV"), rs.getString("HoTen"),
                        rs.getInt("TongBuoi"), rs.getInt("SoVang"), rs.getInt("SoVangCoPhep")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ket;
    }

    // ==========================================================
    // 7. NGHIỆP VỤ SỒ ĐẦU BÀI (Giảng viên ghi nội dung đã dạy theo từng buổi học)
    // ==========================================================

    // Upsert 1 dong so dau bai theo khoa chinh MaLHP+NgayHoc
    public void upsertSoDauBai(String maLHP, Date ngayHoc, String noiDungDay, String ghiChu) {
        String sql = "IF EXISTS (SELECT 1 FROM SO_DAU_BAI WHERE MaLHP=? AND NgayHoc=?) " +
                     "UPDATE SO_DAU_BAI SET NoiDungDay=?, GhiChu=? WHERE MaLHP=? AND NgayHoc=? " +
                     "ELSE INSERT INTO SO_DAU_BAI (MaLHP, NgayHoc, NoiDungDay, GhiChu) VALUES (?,?,?,?)";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLHP);  ps.setDate(2, ngayHoc);
            ps.setString(3, noiDungDay); ps.setString(4, ghiChu);
            ps.setString(5, maLHP);  ps.setDate(6, ngayHoc);
            ps.setString(7, maLHP);  ps.setDate(8, ngayHoc);
            ps.setString(9, noiDungDay); ps.setString(10, ghiChu);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // Lay so dau bai da ghi cho 1 lop trong 1 ngay cu the -> {NoiDungDay, GhiChu}, hoac null neu chua ghi
    public Object[] getSoDauBai(String maLHP, Date ngayHoc) {
        String sql = "SELECT NoiDungDay, GhiChu FROM SO_DAU_BAI WHERE MaLHP = ? AND NgayHoc = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLHP); ps.setDate(2, ngayHoc);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new Object[]{ rs.getString("NoiDungDay"), rs.getString("GhiChu") };
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // Toan bo lich su so dau bai cua 1 lop (moi nhat truoc), phuc vu xem lai / chon sua
    public List<Object[]> getLichSuSoDauBai(String maLHP) {
        List<Object[]> ket = new ArrayList<>();
        String sql = "SELECT NgayHoc, NoiDungDay, GhiChu FROM SO_DAU_BAI WHERE MaLHP = ? ORDER BY NgayHoc DESC";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLHP);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ket.add(new Object[]{ rs.getDate("NgayHoc"), rs.getString("NoiDungDay"), rs.getString("GhiChu") });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ket;
    }

    // ==========================================================
    // 8. NGHIỆP VỤ CỐ VẤN HỌC TẬP (Giảng viên làm cố vấn cho 1 hay nhiều lớp sinh hoạt)
    // ==========================================================

    // Danh sach MaLop (lop sinh hoat, vd 'K2021A') ma GV nay dang lam co van - 1 GV co the
    // phu trach nhieu lop tuy vao cach gan trong bang CO_VAN_HOC_TAP.
    public List<String> getLopCoVanByGV(String maGV) {
        List<String> ket = new ArrayList<>();
        String sql = "SELECT MaLop FROM CO_VAN_HOC_TAP WHERE MaGV = ? ORDER BY MaLop";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maGV);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ket.add(rs.getString("MaLop"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ket;
    }

    // Danh sach SV trong 1 lop sinh hoat, kem GPA (tinh theo trong so tin chi, dung cong thuc
    // da thong nhat trong toan bo project: SUM(diem*tinchi)/SUM(tinchi), CHI tinh tren cac mon
    // da co diem - N'Chua co diem' khong tinh vao), tin chi tich luy (chi cong mon N'Dat') va
    // so mon dang no (N'Khong dat').
    public List<Object[]> getSinhVienTrongLopCoVan(String maLop) {
        List<Object[]> ket = new ArrayList<>();
        String sql = "SELECT sv.MaSV, sv.HoTen, sv.TrangThaiHocTap, " +
                     "ISNULL(SUM(CASE WHEN kq.TrangThai <> N'Chưa có điểm' THEN kq.DiemTongKet * mh.SoTinChi ELSE 0 END) / " +
                     "      NULLIF(SUM(CASE WHEN kq.TrangThai <> N'Chưa có điểm' THEN mh.SoTinChi ELSE 0 END), 0), 0) AS GPA, " +
                     "ISNULL(SUM(CASE WHEN kq.TrangThai = N'Đạt' THEN mh.SoTinChi ELSE 0 END), 0) AS TinChiTichLuy, " +
                     "ISNULL(SUM(CASE WHEN kq.TrangThai = N'Không đạt' THEN 1 ELSE 0 END), 0) AS SoMonNo " +
                     "FROM SINH_VIEN sv " +
                     "LEFT JOIN KET_QUA_DANG_KY kq ON kq.MaSV = sv.MaSV " +
                     "LEFT JOIN LOP_HOC_PHAN lhp ON kq.MaLHP = lhp.MaLHP " +
                     "LEFT JOIN MON_HOC mh ON lhp.MaMon = mh.MaMon " +
                     "WHERE sv.MaLop = ? " +
                     "GROUP BY sv.MaSV, sv.HoTen, sv.TrangThaiHocTap " +
                     "ORDER BY sv.HoTen";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maLop);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ket.add(new Object[]{
                        rs.getString("MaSV"), rs.getString("HoTen"), rs.getString("TrangThaiHocTap"),
                        rs.getDouble("GPA"), rs.getInt("TinChiTichLuy"), rs.getInt("SoMonNo")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ket;
    }
}