--liquibase formatted sql

--changeset hotelnt:100-reference-phuong
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM Phuong
INSERT INTO Phuong(tenPhuong) VALUES
 (N'Phước Hải'), (N'Lộc Thọ'), (N'Vĩnh Phước'), (N'Vĩnh Hải'), (N'Vĩnh Nguyên'), (N'Phước Long');

--changeset hotelnt:101-reference-loai-co-so
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM LoaiCoSo
INSERT INTO LoaiCoSo(tenLoai) VALUES
 (N'Khách sạn'), (N'Villa'), (N'Homestay'), (N'Resort'), (N'Căn hộ');

--changeset hotelnt:102-reference-tien-ich
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM TienIch
INSERT INTO TienIch(tenTienIch, nhom, icon) VALUES
 (N'Hồ bơi', 'TIEN_NGHI', 'pool'),
 (N'Wifi', 'TIEN_NGHI', 'wifi'),
 (N'Gần biển', 'LAN_CAN', 'beach'),
 (N'Bãi đậu xe', 'TIEN_NGHI', 'parking'),
 (N'Điều hòa', 'TIEN_NGHI', 'ac'),
 (N'Bếp riêng', 'TIEN_NGHI', 'kitchen'),
 (N'Gần chợ đêm', 'LAN_CAN', 'market'),
 (N'Gần trung tâm', 'LAN_CAN', 'center');
