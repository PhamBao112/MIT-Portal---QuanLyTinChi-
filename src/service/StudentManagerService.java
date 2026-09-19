package service;

import dao.TruongHocDAO;
import entity.LopHocPhan;
import exception.BusinessLogicException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class StudentManagerService {
    private TruongHocDAO dao;

    public StudentManagerService() { 
        this.dao = new TruongHocDAO(); 
    }

    // ==========================================================
    // CÁC HÀM PHỤC VỤ TRANG CÔNG NỢ (HÌNH 1 - CÓ BẢNG CHI TIẾT)
    // ==========================================================
    public List<String> getDanhSachHocKy() {
        return dao.getAllHocKy();
    }

    // Danh sách học kỳ riêng cho 1 sinh viên (chỉ những kỳ SV đó thực sự có đăng ký)
    public List<String> getDanhSachHocKyCuaSV(String maSV) {
        return dao.getHocKyCuaSinhVien(maSV);
    }

    public String getHocKyGanNhatCuaSV(String maSV) {
        return dao.getHocKyGanNhatCuaSinhVien(maSV);
    }

    public double[] getChiTietCongNo(String maSV, String maHK) {
        return dao.getThongTinCongNo(maSV, maHK);
    }

    // NGHIỆP VỤ MỚI: Lấy danh sách công nợ cho Bảng tổng hợp
    // FIX #3: trả về List thay vì ResultSet - tránh rò rỉ connection
    public List<Object[]> getDanhSachCongNo(String maSV) {
        return dao.getLichSuCongNo(maSV);
    }

    public List<Object[]> getBangDiemChiTiet(String maSV, String maHK) {
        return dao.getChiTietDangKyTrongKy(maSV, maHK);
    }

    // NGHIỆP VỤ MỚI: Thanh toán công nợ
    public String thanhToanCongNo(String maSV, String maHK) throws BusinessLogicException {
        double[] info = dao.getThongTinCongNo(maSV, maHK);
        if (info[0] == -1.0) {
            throw new BusinessLogicException("Chưa có phiếu báo công nợ cho học kỳ này. Vui lòng liên hệ Phòng Đào Tạo!");
        }
        double tongTien = info[0];
        double daDong = info[1];
        
        if (tongTien <= daDong) {
            throw new BusinessLogicException("Học phí học kỳ này đã được thanh toán hoàn tất!");
        }
        
        // Gọi DAO cập nhật Database: SoTienDaDong = TongTienPhaiDong
        dao.updateThanhToanCongNo(maSV, maHK, tongTien);
        return "Thanh toán thành công số tiền: " + String.format("%,.0f VNĐ", (tongTien - daDong));
    }

    // ==========================================================
    // CÁC HÀM CŨ XỬ LÝ ĐĂNG KÝ, TÍNH TIỀN, TỐT NGHIỆP, IMPORT
    // ==========================================================
    public String registerCourse(String maSV, String maLHP) throws BusinessLogicException {
        LopHocPhan lhp = dao.findLopHocPhanById(maLHP);
        if (lhp == null) throw new BusinessLogicException("Lớp học phần không tồn tại!");
        
        if (dao.countSinhVienDaDangKy(maLHP) >= lhp.getSucChua()) {
            throw new BusinessLogicException("Lớp học phần đã đầy sức chứa!");
        }
        
        String maMonTQ = dao.getMonTienQuyet(lhp.getMaMon());
        if (maMonTQ != null && !dao.checkSinhVienDaDatMon(maSV, maMonTQ)) {
            throw new BusinessLogicException("Bạn chưa đạt môn tiên quyết: " + maMonTQ);
        }
        
        // FIX: goc re cua loi "dang ky trung 3 lan" tren TKB - insertKetQuaDangKy() truoc day
        // KHONG kiem tra da dang ky chua ma insert thang, chi can bam nut/double-click 1 lan lo
        // la co the tao 2-3 dong trung het cho cung 1 lop -> hien lap lai nhieu lan tren lich hoc.
        if (dao.checkDaDangKyLop(maSV, maLHP)) {
            throw new BusinessLogicException("Bạn đã đăng ký lớp học phần này rồi!");
        }
        
        // FIX (yêu cầu 3): ràng buộc THẬT ở tầng dữ liệu - 1 buổi (Sáng/Chiều) trong ngày
        // chỉ được học 1 môn, chặn ngay lúc đăng ký thay vì để lọt vào DB rồi mới ẩn ở UI.
        String[] thuTiet = dao.getThuVaTietHoc(maLHP);
        if (thuTiet != null && thuTiet[1] != null && thuTiet[1].contains("-")) {
            try {
                int tietBD = Integer.parseInt(thuTiet[1].split("-")[0].trim());
                if (dao.checkTrungBuoi(maSV, lhp.getMaHK(), thuTiet[0], tietBD)) {
                    String buoi = tietBD <= 6 ? "Sáng" : "Chiều";
                    throw new BusinessLogicException("Bạn đã có môn học khác vào buổi " + buoi + " " + thuTiet[0] + " rồi! Mỗi buổi chỉ được đăng ký 1 môn.");
                }
            } catch (NumberFormatException ignore) {}
        }
        
        dao.insertKetQuaDangKy(maSV, maLHP, "Chưa có điểm");
        
        // Tự động cập nhật lại hóa đơn công nợ khi đăng ký môn mới
        try { calculateTuition(maSV, lhp.getMaHK()); } catch (Exception e) {}
        
        return "Đăng ký thành công lớp " + maLHP + " cho sinh viên " + maSV;
    }

    // HÀM MỚI: HỦY ĐĂNG KÝ HỌC PHẦN
    public String cancelRegistration(String maSV, String maLHP) throws BusinessLogicException {
        try {
            LopHocPhan lhp = dao.findLopHocPhanById(maLHP);
            dao.deleteKetQuaDangKy(maSV, maLHP);
            
            // Tự động trừ tiền hóa đơn công nợ khi hủy môn
            if (lhp != null) {
                calculateTuition(maSV, lhp.getMaHK());
            }
            
            return "Đã hủy đăng ký môn học thành công!";
        } catch (Exception e) {
            throw new BusinessLogicException("Lỗi hệ thống khi hủy đăng ký: " + e.getMessage());
        }
    }

    public String calculateTuition(String maSV, String maHK) throws BusinessLogicException {
        int tongTinChi = dao.sumTinChiTrongKy(maSV, maHK);
        String maPhieu = "HP_" + maSV + "_" + maHK;
        
        if (tongTinChi == 0) {
            // Nếu không còn tín chỉ nào (do chưa đăng ký hoặc đã hủy sạch) -> Update học phí về 0
            dao.insertCongNo(maPhieu, maSV, maHK, 0.0);
            return "Học phí đã được cập nhật về 0 do không còn môn học nào.";
        }
        
        // FIX: don gia tin chi truoc day hardcode SAI 450.000d, trong khi TOAN BO du lieu mau
        // trong CONG_NO_HOC_PHI (QuanLyTinChi.sql) deu tinh theo 850.000d/TC (vd 8TC=6.800.000d).
        // Neu de 450.000d, chi can 1 SV dang ky/huy 1 mon la calculateTuition() se tinh lai
        // hoc phi SAI, ghi de len du lieu dung da co.
        double donGia = 850000.0;
        double tongTien = tongTinChi * donGia;
        
        dao.insertCongNo(maPhieu, maSV, maHK, tongTien);
        return "Đã lập/cập nhật phiếu thu: " + maPhieu + "\nTổng tiền phải đóng: " + String.format("%,.0f VNĐ", tongTien);
    }

    public String checkGraduation(String maSV) {
        StringBuilder sb = new StringBuilder("KẾT QUẢ XÉT DUYỆT TỐT NGHIỆP:\n");
        boolean pass = true;
        
        int tcTichLuy = dao.getTongTinChiTichLuy(maSV);
        if (tcTichLuy < 150) { 
            sb.append("- [X] Chưa đủ tín chỉ (").append(tcTichLuy).append("/150)\n"); 
            pass = false; 
        } else {
            sb.append("- [OK] Đã đủ tín chỉ\n");
        }
        
        if (dao.checkNoHocPhi(maSV)) { 
            sb.append("- [X] Còn nợ học phí\n"); 
            pass = false; 
        } else {
            sb.append("- [OK] Đã hoàn thành học phí\n");
        }
        
        if (!dao.checkChuanNgoaiNgu(maSV)) { 
            sb.append("- [X] Chưa đạt chuẩn Ngoại ngữ\n"); 
            pass = false; 
        } else {
            sb.append("- [OK] Đã đạt chuẩn Ngoại ngữ\n");
        }
        
        sb.append("\nKẾT LUẬN: ").append(pass ? "ĐỦ ĐIỀU KIỆN TỐT NGHIỆP" : "CHƯA ĐỦ ĐIỀU KIỆN TỐT NGHIỆP");
        return sb.toString();
    }

    public String importSinhVienFromExcel(String filePath) throws BusinessLogicException {
        int countSuccess = 0, countFail = 0;
        try (FileInputStream fis = new FileInputStream(new File(filePath)); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0); DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                try {
                    // FIX #4: thu tu cot Excel phai la MaSV(0), HoTen(1), GioiTinh(2), NgaySinh(3),
                    // SDT(4), Email(5), TrangThai(6), MaCTDT(7), MaLop(8).
                    // Ban cu doc nham cell(9) lam MaLop va bo qua han cell(8) -> du lieu MaLop bi sai/rong.
                    String maSV = formatter.formatCellValue(row.getCell(0));
                    if (maSV.isEmpty()) continue;
                    dao.insertSinhVien(
                        maSV, formatter.formatCellValue(row.getCell(1)), 
                        formatter.formatCellValue(row.getCell(2)), formatter.formatCellValue(row.getCell(3)), 
                        formatter.formatCellValue(row.getCell(4)), formatter.formatCellValue(row.getCell(5)), 
                        formatter.formatCellValue(row.getCell(6)), formatter.formatCellValue(row.getCell(7)), 
                        formatter.formatCellValue(row.getCell(8))
                    );
                    countSuccess++;
                } catch (Exception e) { countFail++; }
            }
        } catch (Exception e) { throw new BusinessLogicException("Lỗi đọc file Excel (.xlsx)"); }
        return "Import Excel Sinh Viên hoàn tất!\n- Thành công: " + countSuccess + "\n- Lỗi/Bỏ qua: " + countFail;
    }

    public String importGiangVienFromExcel(String filePath) throws BusinessLogicException {
        int countSuccess = 0, countFail = 0;
        try (FileInputStream fis = new FileInputStream(new File(filePath)); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0); DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                try {
                    String maGV = formatter.formatCellValue(row.getCell(0));
                    if (maGV.isEmpty()) continue;
                    dao.insertGiangVien(
                        maGV, formatter.formatCellValue(row.getCell(1)), 
                        formatter.formatCellValue(row.getCell(2)), formatter.formatCellValue(row.getCell(3)), 
                        formatter.formatCellValue(row.getCell(4)), formatter.formatCellValue(row.getCell(5)), 
                        formatter.formatCellValue(row.getCell(6))
                    );
                    countSuccess++;
                } catch (Exception e) { countFail++; }
            }
        } catch (Exception e) { throw new BusinessLogicException("Lỗi đọc file Excel (.xlsx)"); }
        return "Import Excel Giảng Viên hoàn tất!\n- Thành công: " + countSuccess + "\n- Lỗi/Bỏ qua: " + countFail;
    }
}