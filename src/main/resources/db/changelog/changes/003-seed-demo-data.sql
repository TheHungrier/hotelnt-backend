--liquibase formatted sql

--changeset hotelnt:200-demo-accounts context:dev splitStatements:false
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM TaiKhoan WHERE email = 'admin@hotelnt.local'
DECLARE @hAdmin VARCHAR(255) = '$2a$10$8nX8cjcMW4hQw1OTMLfjvOOIRgnUzKB2Ke9Qs8sit9ntgTleaArba';
DECLARE @hHost VARCHAR(255) = '$2a$10$UD9YMntbrrM5BahTH0z3wuUUI9ReJUkE/jIVFydWsKu9kODCO64fe';
INSERT INTO TaiKhoan(vaiTro, trangThai, hoTen, soDienThoai, email, cccd, matKhau, lyDoKhoa, ngayKhoa) VALUES
 (N'ADMIN', N'ACTIVE', N'Quản trị viên', '0900000001', 'admin@hotelnt.local', NULL, @hAdmin, NULL, NULL),
 (N'HOST', N'ACTIVE', N'Nguyễn Văn A', '0901234567', 'host1@hotelnt.local', '079201000001', @hHost, NULL, NULL),
 (N'HOST', N'ACTIVE', N'Trần Thị B', '0902345678', 'host2@hotelnt.local', '079201000002', @hHost, NULL, NULL),
 (N'HOST', N'LOCKED', N'Lê Văn C', '0903456789', 'host3@hotelnt.local', '079201000003', @hHost, N'Vi phạm quy định đăng tin', GETDATE());

--changeset hotelnt:201-demo-listings context:dev
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM CoSoLuuTru
INSERT INTO CoSoLuuTru(tenCSLT, diaChiChiTiet, moTa, longitude, latitude, gia, trangThai, lyDoTuChoi, lyDoViPham,
                       ngayGuiDuyet, ngayDuyet, ngayCapNhatGia, maHost, maPhuong, maLoai) VALUES
 (N'Villa View Biển Nha Trang', N'12 Trần Phú, Nha Trang', N'Villa sát biển, có hồ bơi riêng.',
    109.1943000, 12.2451000, 1500000, N'APPROVED', NULL, NULL, GETDATE(), GETDATE(), GETDATE(), 2, 1, 2),
 (N'Khách sạn Sao Biển', N'45 Nguyễn Thiện Thuật, Nha Trang', N'Khách sạn 3 sao gần chợ đêm, có bữa sáng.',
    109.1950000, 12.2400000, 800000, N'APPROVED', NULL, NULL, GETDATE(), GETDATE(), GETDATE(), 2, 2, 1),
 (N'Homestay Nắng Mai', N'88 Hùng Vương, Nha Trang', N'Homestay ấm cúng, phù hợp nhóm bạn.',
    109.1900000, 12.2500000, 450000, N'APPROVED', NULL, NULL, GETDATE(), GETDATE(), GETDATE(), 3, 3, 3),
 (N'Căn hộ Biển Xanh', N'30 Phạm Văn Đồng, Nha Trang', N'Căn hộ có bếp, view biển, gần trung tâm.',
    109.2100000, 12.2700000, 1100000, N'APPROVED', NULL, NULL, GETDATE(), GETDATE(), GETDATE(), 3, 4, 5),
 (N'Resort Hải Âu', N'1 Nguyễn Tất Thành, Nha Trang', N'Resort nghỉ dưỡng cao cấp.',
    109.2200000, 12.2900000, 2800000, N'PENDING', NULL, NULL, GETDATE(), NULL, GETDATE(), 2, 5, 4),
 (N'Homestay Đang Soạn', N'5 Lê Hồng Phong, Nha Trang', NULL,
    NULL, NULL, NULL, N'DRAFT', NULL, NULL, NULL, NULL, NULL, 3, 6, 3),
 (N'Khách sạn Thiếu Ảnh', N'9 Yersin, Nha Trang', N'Khách sạn mini.',
    109.1920000, 12.2380000, 600000, N'REJECTED', N'Cần bổ sung thêm ảnh thực tế của phòng.', NULL,
    GETDATE(), NULL, GETDATE(), 2, 2, 1),
 (N'Villa Của Host Bị Khóa', N'20 Trần Phú, Nha Trang', N'Cơ sở này không được hiển thị vì chủ bị khóa.',
    109.1960000, 12.2430000, 2000000, N'APPROVED', NULL, NULL, GETDATE(), GETDATE(), GETDATE(), 4, 1, 2);

--changeset hotelnt:202-demo-amenities context:dev
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM TienIchCSLT
INSERT INTO TienIchCSLT(maCSLT, maTienIch) VALUES
 (1,1),(1,2),(1,3),(1,5),(2,2),(2,4),(2,5),(2,7),
 (3,2),(3,5),(3,6),(4,2),(4,5),(4,6),(4,8),(5,1),(5,2),(5,3),
 (7,2),(8,1),(8,2);

--changeset hotelnt:203-demo-media context:dev
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM Media
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

--changeset hotelnt:204-demo-contacts context:dev
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM LienHe
INSERT INTO LienHe(loaiLienHe, giaTri, maCSLT) VALUES
 (N'SO_DIEN_THOAI', '0901234567', 1),(N'ZALO', '0901234567', 1),
 (N'SO_DIEN_THOAI', '0901234567', 2),(N'SO_DIEN_THOAI', '0902345678', 3),
 (N'FACEBOOK', 'https://facebook.com/homestay.nangmai', 3),(N'SO_DIEN_THOAI', '0902345678', 4);

--changeset hotelnt:205-demo-config context:dev
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM CauHinh
INSERT INTO CauHinh(khoa, giaTri, maTaiKhoan) VALUES
 ('seo.metaTitle', N'Hotel Nha Trang - Tìm nơi lưu trú', 1),
 ('seo.metaDescription', N'Tìm kiếm khách sạn, villa, homestay tại Nha Trang với giá tốt.', 1),
 ('seo.slugUrl', N'hotel-nha-trang', 1),
 ('seo.ogTitle', N'Hotel Nha Trang', 1),
 ('seo.ogImageUrl', N'', 1),
 ('ai.tinNhanMacDinh', N'Xin chào! Mình có thể giúp bạn tìm nơi lưu trú tại Nha Trang.', 1);
