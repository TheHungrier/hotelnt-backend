--liquibase formatted sql

--changeset hotelnt:001-tai-khoan
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables WHERE name = 'TaiKhoan' AND schema_id = SCHEMA_ID('dbo')
CREATE TABLE TaiKhoan (
    id               INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_TaiKhoan PRIMARY KEY,
    vaiTro           NVARCHAR(20)  NOT NULL CONSTRAINT CK_TaiKhoan_vaiTro CHECK (vaiTro IN (N'HOST', N'ADMIN')),
    trangThai        NVARCHAR(20)  NOT NULL CONSTRAINT CK_TaiKhoan_trangThai CHECK (trangThai IN (N'ACTIVE', N'LOCKED')),
    hoTen            NVARCHAR(100) NOT NULL,
    soDienThoai      VARCHAR(15)   NOT NULL,
    email            VARCHAR(100)  NOT NULL,
    cccd             VARCHAR(12)   NULL,
    matKhau          VARCHAR(255)  NOT NULL,
    ngayTao          DATETIME      NOT NULL CONSTRAINT DF_TaiKhoan_ngayTao DEFAULT GETDATE(),
    ngayCapNhat      DATETIME      NOT NULL CONSTRAINT DF_TaiKhoan_ngayCapNhat DEFAULT GETDATE(),
    lyDoKhoa         NVARCHAR(500) NULL,
    ngayKhoa         DATETIME      NULL,
    soLanDangNhapSai INT           NOT NULL CONSTRAINT DF_TaiKhoan_soLanSai DEFAULT 0,
    khoaDen          DATETIME      NULL
);
CREATE UNIQUE INDEX UX_TaiKhoan_email ON TaiKhoan(email);
CREATE UNIQUE INDEX UX_TaiKhoan_soDienThoai ON TaiKhoan(soDienThoai);
CREATE UNIQUE INDEX UX_TaiKhoan_cccd ON TaiKhoan(cccd) WHERE cccd IS NOT NULL;
--rollback DROP TABLE TaiKhoan;

--changeset hotelnt:002-phuong
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables WHERE name = 'Phuong' AND schema_id = SCHEMA_ID('dbo')
CREATE TABLE Phuong (
    maPhuong INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Phuong PRIMARY KEY,
    tenPhuong NVARCHAR(100) NOT NULL
);
--rollback DROP TABLE Phuong;

--changeset hotelnt:003-loai-co-so
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables WHERE name = 'LoaiCoSo' AND schema_id = SCHEMA_ID('dbo')
CREATE TABLE LoaiCoSo (
    maLoai INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LoaiCoSo PRIMARY KEY,
    tenLoai NVARCHAR(100) NOT NULL
);
--rollback DROP TABLE LoaiCoSo;

--changeset hotelnt:004-tien-ich
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables WHERE name = 'TienIch' AND schema_id = SCHEMA_ID('dbo')
CREATE TABLE TienIch (
    maTienIch INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_TienIch PRIMARY KEY,
    tenTienIch NVARCHAR(100) NOT NULL,
    nhom VARCHAR(20) NOT NULL CONSTRAINT CK_TienIch_nhom CHECK (nhom IN ('TIEN_NGHI', 'LAN_CAN')),
    icon VARCHAR(100) NULL
);
--rollback DROP TABLE TienIch;

--changeset hotelnt:005-co-so-luu-tru
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables WHERE name = 'CoSoLuuTru' AND schema_id = SCHEMA_ID('dbo')
CREATE TABLE CoSoLuuTru (
    maCSLT INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CoSoLuuTru PRIMARY KEY,
    tenCSLT NVARCHAR(150) NOT NULL,
    diaChiChiTiet NVARCHAR(200) NOT NULL,
    moTa NVARCHAR(500) NULL,
    longitude DECIMAL(10,7) NULL,
    latitude DECIMAL(10,7) NULL,
    gia DECIMAL(18,0) NULL,
    trangThai NVARCHAR(20) NOT NULL
        CONSTRAINT CK_CSLT_trangThai CHECK (trangThai IN (N'DRAFT', N'PENDING', N'APPROVED', N'REJECTED', N'HIDDEN', N'REMOVED')),
    ngayTao DATETIME NOT NULL CONSTRAINT DF_CSLT_ngayTao DEFAULT GETDATE(),
    ngayCapNhat DATETIME NOT NULL CONSTRAINT DF_CSLT_ngayCapNhat DEFAULT GETDATE(),
    lyDoTuChoi NVARCHAR(500) NULL,
    lyDoViPham NVARCHAR(500) NULL,
    ngayGuiDuyet DATETIME NULL,
    ngayDuyet DATETIME NULL,
    ngayCapNhatGia DATETIME NULL,
    maHost INT NOT NULL CONSTRAINT FK_CSLT_Host FOREIGN KEY REFERENCES TaiKhoan(id),
    maPhuong INT NOT NULL CONSTRAINT FK_CSLT_Phuong FOREIGN KEY REFERENCES Phuong(maPhuong),
    maLoai INT NOT NULL CONSTRAINT FK_CSLT_Loai FOREIGN KEY REFERENCES LoaiCoSo(maLoai)
);
CREATE INDEX IX_CSLT_trangThai ON CoSoLuuTru(trangThai);
CREATE INDEX IX_CSLT_maHost ON CoSoLuuTru(maHost);
CREATE INDEX IX_CSLT_maPhuong ON CoSoLuuTru(maPhuong);
CREATE INDEX IX_CSLT_maLoai ON CoSoLuuTru(maLoai);
--rollback DROP TABLE CoSoLuuTru;

--changeset hotelnt:006-tien-ich-cslt
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables WHERE name = 'TienIchCSLT' AND schema_id = SCHEMA_ID('dbo')
CREATE TABLE TienIchCSLT (
    maCSLT INT NOT NULL CONSTRAINT FK_TICSLT_CSLT FOREIGN KEY REFERENCES CoSoLuuTru(maCSLT) ON DELETE CASCADE,
    maTienIch INT NOT NULL CONSTRAINT FK_TICSLT_TienIch FOREIGN KEY REFERENCES TienIch(maTienIch),
    CONSTRAINT PK_TienIchCSLT PRIMARY KEY (maCSLT, maTienIch)
);
--rollback DROP TABLE TienIchCSLT;

--changeset hotelnt:007-media
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables WHERE name = 'Media' AND schema_id = SCHEMA_ID('dbo')
CREATE TABLE Media (
    mediaId INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Media PRIMARY KEY,
    duongDan VARCHAR(500) NOT NULL,
    kichThuoc INT NOT NULL,
    loai VARCHAR(10) NOT NULL CONSTRAINT CK_Media_loai CHECK (loai IN ('IMAGE', 'VIDEO')),
    mimeType VARCHAR(50) NOT NULL,
    thuTu INT NOT NULL,
    ngayTao DATETIME NOT NULL CONSTRAINT DF_Media_ngayTao DEFAULT GETDATE(),
    maCSLT INT NOT NULL CONSTRAINT FK_Media_CSLT FOREIGN KEY REFERENCES CoSoLuuTru(maCSLT) ON DELETE CASCADE
);
CREATE INDEX IX_Media_maCSLT ON Media(maCSLT);
--rollback DROP TABLE Media;

--changeset hotelnt:008-lien-he
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables WHERE name = 'LienHe' AND schema_id = SCHEMA_ID('dbo')
CREATE TABLE LienHe (
    maLienHe INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LienHe PRIMARY KEY,
    loaiLienHe NVARCHAR(20) NOT NULL CONSTRAINT CK_LienHe_loai CHECK (loaiLienHe IN (N'SO_DIEN_THOAI', N'ZALO', N'FACEBOOK')),
    giaTri VARCHAR(255) NOT NULL,
    maCSLT INT NOT NULL CONSTRAINT FK_LienHe_CSLT FOREIGN KEY REFERENCES CoSoLuuTru(maCSLT) ON DELETE CASCADE
);
CREATE INDEX IX_LienHe_maCSLT ON LienHe(maCSLT);
--rollback DROP TABLE LienHe;

--changeset hotelnt:009-phien-dang-nhap
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables WHERE name = 'PhienDangNhap' AND schema_id = SCHEMA_ID('dbo')
CREATE TABLE PhienDangNhap (
    maPhien VARCHAR(36) NOT NULL CONSTRAINT PK_PhienDangNhap PRIMARY KEY,
    refreshTokenHash VARCHAR(255) NOT NULL,
    hetHanLuc DATETIME NOT NULL,
    daThuHoi BIT NOT NULL CONSTRAINT DF_Phien_daThuHoi DEFAULT 0,
    ngayTao DATETIME NOT NULL CONSTRAINT DF_Phien_ngayTao DEFAULT GETDATE(),
    maTaiKhoan INT NOT NULL CONSTRAINT FK_Phien_TaiKhoan FOREIGN KEY REFERENCES TaiKhoan(id) ON DELETE CASCADE
);
CREATE INDEX IX_Phien_maTaiKhoan ON PhienDangNhap(maTaiKhoan);
--rollback DROP TABLE PhienDangNhap;

--changeset hotelnt:010-otp-challenge
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables WHERE name = 'OtpChallenge' AND schema_id = SCHEMA_ID('dbo')
CREATE TABLE OtpChallenge (
    challengeId VARCHAR(36) NOT NULL CONSTRAINT PK_OtpChallenge PRIMARY KEY,
    loai NVARCHAR(20) NOT NULL CONSTRAINT CK_Otp_loai CHECK (loai IN (N'SIGNUP', N'SIGNIN', N'FORGOT_PASSWORD')),
    email VARCHAR(100) NOT NULL,
    duLieuDangKy NVARCHAR(MAX) NULL,
    otpHash VARCHAR(255) NOT NULL,
    soLanSai INT NOT NULL CONSTRAINT DF_Otp_soLanSai DEFAULT 0,
    hetHanLuc DATETIME NOT NULL,
    guiLaiSauLuc DATETIME NOT NULL,
    daDung BIT NOT NULL CONSTRAINT DF_Otp_daDung DEFAULT 0,
    maTaiKhoan INT NULL CONSTRAINT FK_Otp_TaiKhoan FOREIGN KEY REFERENCES TaiKhoan(id) ON DELETE CASCADE
);
CREATE INDEX IX_Otp_email ON OtpChallenge(email);
--rollback DROP TABLE OtpChallenge;

--changeset hotelnt:011-cau-hinh
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM sys.tables WHERE name = 'CauHinh' AND schema_id = SCHEMA_ID('dbo')
CREATE TABLE CauHinh (
    khoa VARCHAR(100) NOT NULL CONSTRAINT PK_CauHinh PRIMARY KEY,
    giaTri NVARCHAR(MAX) NOT NULL,
    ngayCapNhat DATETIME NOT NULL CONSTRAINT DF_CauHinh_ngayCapNhat DEFAULT GETDATE(),
    maTaiKhoan INT NULL CONSTRAINT FK_CauHinh_TaiKhoan FOREIGN KEY REFERENCES TaiKhoan(id)
);
--rollback DROP TABLE CauHinh;
