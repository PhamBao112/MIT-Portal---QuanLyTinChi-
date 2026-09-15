package dao;

import config.DBConnect;
import entity.LopHocPhan;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        String sql = "SELECT COUNT(*) FROM CONG_NO_HOC_PHI WHERE MaSV = ? AND TrangThai != N'Đã hoàn thành'";
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
}