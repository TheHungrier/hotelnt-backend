/* =====================================================================
   HotelNT - du lieu mau de phat trien / demo. Chay SAU schema.sql.
   Tai khoan mau:
     Admin : admin@hotelnt.local / Admin@123
     Host  : host1@hotelnt.local, host2@hotelnt.local, host3@hotelnt.local (bi KHOA) / Host@1234
   ===================================================================== */
USE HotelNT;
GO
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;

/* ---- Danh muc (insert theo thu tu de ma tu tang 1,2,3...) ---- */
INSERT INTO Phuong(tenPhuong) VALUES
 (N'Phước Hải'), (N'Lộc Thọ'), (N'Vĩnh Phước'), (N'Vĩnh Hải'), (N'Vĩnh Nguyên'), (N'Phước Long');

INSERT INTO LoaiCoSo(tenLoai) VALUES
 (N'Khách sạn'), (N'Villa'), (N'Homestay'), (N'Resort'), (N'Căn hộ');

INSERT INTO TienIch(tenTienIch, nhom, icon) VALUES
 (N'Hồ bơi',        'TIEN_NGHI', 'pool'),      -- 1
 (N'Wifi',          'TIEN_NGHI', 'wifi'),      -- 2
 (N'Gần biển',      'LAN_CAN',   'beach'),     -- 3
 (N'Bãi đậu xe',    'TIEN_NGHI', 'parking'),   -- 4
 (N'Điều hòa',      'TIEN_NGHI', 'ac'),        -- 5
 (N'Bếp riêng',     'TIEN_NGHI', 'kitchen'),   -- 6
 (N'Gần chợ đêm',   'LAN_CAN',   'market'),    -- 7
 (N'Gần trung tâm', 'LAN_CAN',   'center');    -- 8

/* ---- Tai khoan ---- */
DECLARE @hAdmin VARCHAR(255) = '$2a$10$8nX8cjcMW4hQw1OTMLfjvOOIRgnUzKB2Ke9Qs8sit9ntgTleaArba';  -- Admin@123
DECLARE @hHost  VARCHAR(255) = '$2a$10$UD9YMntbrrM5BahTH0z3wuUUI9ReJUkE/jIVFydWsKu9kODCO64fe';  -- Host@1234

INSERT INTO TaiKhoan(vaiTro, trangThai, hoTen, soDienThoai, email, cccd, matKhau, lyDoKhoa, ngayKhoa) VALUES
 (N'ADMIN', N'ACTIVE', N'Quản trị viên',  '0900000001', 'admin@hotelnt.local', NULL,           @hAdmin, NULL, NULL),  -- id 1
 (N'HOST',  N'ACTIVE', N'Nguyễn Văn A',   '0901234567', 'host1@hotelnt.local', '079201000001', @hHost,  NULL, NULL),  -- id 2
 (N'HOST',  N'ACTIVE', N'Trần Thị B',     '0902345678', 'host2@hotelnt.local', '079201000002', @hHost,  NULL, NULL),  -- id 3
 (N'HOST',  N'LOCKED', N'Lê Văn C',       '0903456789', 'host3@hotelnt.local', '079201000003', @hHost,  N'Vi phạm quy định đăng tin', GETDATE()); -- id 4

/* ---- Co so luu tru (toa do trong Nha Trang: vi do 12.1-12.45, kinh do 109.1-109.3) ---- */
INSERT INTO CoSoLuuTru(tenCSLT, diaChiChiTiet, moTa, longitude, latitude, gia, trangThai, lyDoTuChoi, lyDoViPham,
                       ngayGuiDuyet, ngayDuyet, ngayCapNhatGia, maHost, maPhuong, maLoai) VALUES
 -- maCSLT 1..4: da duyet, hien thi cho khach (Host ACTIVE)
 (N'Villa View Biển Nha Trang', N'12 Trần Phú, Nha Trang', N'Villa sát biển, có hồ bơi riêng.',
    109.1943000, 12.2451000, 1500000, N'APPROVED', NULL, NULL, GETDATE(), GETDATE(), GETDATE(), 2, 1, 2),
 (N'Khách sạn Sao Biển', N'45 Nguyễn Thiện Thuật, Nha Trang', N'Khách sạn 3 sao gần chợ đêm, có bữa sáng.',
    109.1950000, 12.2400000, 800000, N'APPROVED', NULL, NULL, GETDATE(), GETDATE(), GETDATE(), 2, 2, 1),
 (N'Homestay Nắng Mai', N'88 Hùng Vương, Nha Trang', N'Homestay ấm cúng, phù hợp nhóm bạn.',
    109.1900000, 12.2500000, 450000, N'APPROVED', NULL, NULL, GETDATE(), GETDATE(), GETDATE(), 3, 3, 3),
 (N'Căn hộ Biển Xanh', N'30 Phạm Văn Đồng, Nha Trang', N'Căn hộ có bếp, view biển, gần trung tâm.',
    109.2100000, 12.2700000, 1100000, N'APPROVED', NULL, NULL, GETDATE(), GETDATE(), GETDATE(), 3, 4, 5),
 -- maCSLT 5: cho duyet
 (N'Resort Hải Âu', N'1 Nguyễn Tất Thành, Nha Trang', N'Resort nghỉ dưỡng cao cấp.',
    109.2200000, 12.2900000, 2800000, N'PENDING', NULL, NULL, GETDATE(), NULL, GETDATE(), 2, 5, 4),
 -- maCSLT 6: ban nhap
 (N'Homestay Đang Soạn', N'5 Lê Hồng Phong, Nha Trang', NULL,
    NULL, NULL, NULL, N'DRAFT', NULL, NULL, NULL, NULL, NULL, 3, 6, 3),
 -- maCSLT 7: bi tu choi
 (N'Khách sạn Thiếu Ảnh', N'9 Yersin, Nha Trang', N'Khách sạn mini.',
    109.1920000, 12.2380000, 600000, N'REJECTED', N'Cần bổ sung thêm ảnh thực tế của phòng.', NULL, GETDATE(), NULL, GETDATE(), 2, 2, 1),
 -- maCSLT 8: da duyet nhung Host bi KHOA => khong duoc hien thi cho khach (de test loc)
 (N'Villa Của Host Bị Khóa', N'20 Trần Phú, Nha Trang', N'Cơ sở này không được hiển thị vì chủ bị khóa.',
    109.1960000, 12.2430000, 2000000, N'APPROVED', NULL, NULL, GETDATE(), GETDATE(), GETDATE(), 4, 1, 2);

/* ---- Tien ich cua tung co so ---- */
INSERT INTO TienIchCSLT(maCSLT, maTienIch) VALUES
 (1,1),(1,2),(1,3),(1,5),
 (2,2),(2,4),(2,5),(2,7),
 (3,2),(3,5),(3,6),
 (4,2),(4,5),(4,6),(4,8),
 (5,1),(5,2),(5,3),
 (7,2),
 (8,1),(8,2);

/* ---- Media: moi co so APPROVED/PENDING/REJECTED co >= 3 anh (duong dan mau) ---- */
INSERT INTO Media(duongDan, kichThuoc, loai, mimeType, thuTu, maCSLT) VALUES
 ('/uploads/mau/1-1.jpg', 500000, 'IMAGE', 'image/jpeg', 1, 1),
 ('/uploads/mau/1-2.jpg', 480000, 'IMAGE', 'image/jpeg', 2, 1),
 ('/uploads/mau/1-3.jpg', 510000, 'IMAGE', 'image/jpeg', 3, 1),
 ('/uploads/mau/2-1.jpg', 450000, 'IMAGE', 'image/jpeg', 1, 2),
 ('/uploads/mau/2-2.jpg', 460000, 'IMAGE', 'image/jpeg', 2, 2),
 ('/uploads/mau/2-3.jpg', 470000, 'IMAGE', 'image/jpeg', 3, 2),
 ('/uploads/mau/3-1.jpg', 430000, 'IMAGE', 'image/jpeg', 1, 3),
 ('/uploads/mau/3-2.jpg', 440000, 'IMAGE', 'image/jpeg', 2, 3),
 ('/uploads/mau/3-3.jpg', 420000, 'IMAGE', 'image/jpeg', 3, 3),
 ('/uploads/mau/4-1.jpg', 520000, 'IMAGE', 'image/jpeg', 1, 4),
 ('/uploads/mau/4-2.jpg', 530000, 'IMAGE', 'image/jpeg', 2, 4),
 ('/uploads/mau/4-3.jpg', 540000, 'IMAGE', 'image/jpeg', 3, 4),
 ('/uploads/mau/5-1.jpg', 550000, 'IMAGE', 'image/jpeg', 1, 5),
 ('/uploads/mau/5-2.jpg', 560000, 'IMAGE', 'image/jpeg', 2, 5),
 ('/uploads/mau/5-3.jpg', 570000, 'IMAGE', 'image/jpeg', 3, 5),
 ('/uploads/mau/7-1.jpg', 400000, 'IMAGE', 'image/jpeg', 1, 7),
 ('/uploads/mau/8-1.jpg', 400000, 'IMAGE', 'image/jpeg', 1, 8),
 ('/uploads/mau/8-2.jpg', 400000, 'IMAGE', 'image/jpeg', 2, 8),
 ('/uploads/mau/8-3.jpg', 400000, 'IMAGE', 'image/jpeg', 3, 8);

/* ---- Lien he cua co so (khong nhap thi khong co dong => API tra null) ---- */
INSERT INTO LienHe(loaiLienHe, giaTri, maCSLT) VALUES
 (N'SO_DIEN_THOAI', '0901234567', 1),
 (N'ZALO',          '0901234567', 1),
 (N'SO_DIEN_THOAI', '0901234567', 2),
 (N'SO_DIEN_THOAI', '0902345678', 3),
 (N'FACEBOOK',      'https://facebook.com/homestay.nangmai', 3),
 (N'SO_DIEN_THOAI', '0902345678', 4);

/* ---- Cau hinh mac dinh (khoa - gia tri JSON/text) ---- */
INSERT INTO CauHinh(khoa, giaTri, maTaiKhoan) VALUES
 ('seo.metaTitle',       N'Hotel Nha Trang - Tìm nơi lưu trú', 1),
 ('seo.metaDescription', N'Tìm kiếm khách sạn, villa, homestay tại Nha Trang với giá tốt.', 1),
 ('seo.slugUrl',         N'hotel-nha-trang', 1),
 ('seo.ogTitle',         N'Hotel Nha Trang', 1),
 ('seo.ogImageUrl',      N'', 1),
 ('ai.tinNhanMacDinh',   N'Xin chào! Mình có thể giúp bạn tìm nơi lưu trú tại Nha Trang.', 1);
GO
PRINT N'Nap du lieu mau xong.';
