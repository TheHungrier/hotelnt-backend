/* =====================================================================
   HotelNT - tao co so du lieu SQL Server theo ERD (docs/ERD.png)
   Chay 1 lan tren DB trong. An toan de chay lai: se XOA va tao moi toan bo bang.
   ===================================================================== */
IF DB_ID(N'HotelNT') IS NULL CREATE DATABASE HotelNT COLLATE Vietnamese_CI_AS;
GO
USE HotelNT;
GO
SET QUOTED_IDENTIFIER ON;   -- bat buoc de tao chi muc loc (filtered index)
GO

/* ---- Xoa bang cu (thu tu: bang con truoc) ---- */
DROP TABLE IF EXISTS CauHinh;
DROP TABLE IF EXISTS OtpChallenge;
DROP TABLE IF EXISTS PhienDangNhap;
DROP TABLE IF EXISTS LienHe;
DROP TABLE IF EXISTS Media;
DROP TABLE IF EXISTS TienIchCSLT;
DROP TABLE IF EXISTS CoSoLuuTru;
DROP TABLE IF EXISTS TienIch;
DROP TABLE IF EXISTS LoaiCoSo;
DROP TABLE IF EXISTS Phuong;
DROP TABLE IF EXISTS TaiKhoan;
GO

/* ---- Tai khoan (Host va Admin) ---- */
CREATE TABLE TaiKhoan (
    id               INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_TaiKhoan PRIMARY KEY,
    vaiTro           NVARCHAR(20)  NOT NULL CONSTRAINT CK_TaiKhoan_vaiTro CHECK (vaiTro IN (N'HOST', N'ADMIN')),
    trangThai        NVARCHAR(20)  NOT NULL CONSTRAINT CK_TaiKhoan_trangThai CHECK (trangThai IN (N'ACTIVE', N'LOCKED')),
    hoTen            NVARCHAR(100) NOT NULL,
    soDienThoai      VARCHAR(15)   NOT NULL,
    email            VARCHAR(100)  NOT NULL,
    cccd             VARCHAR(12)   NULL,
    matKhau          VARCHAR(255)  NOT NULL,            -- bcrypt hash, KHONG luu mat khau goc
    ngayTao          DATETIME      NOT NULL CONSTRAINT DF_TaiKhoan_ngayTao DEFAULT GETDATE(),
    ngayCapNhat      DATETIME      NOT NULL CONSTRAINT DF_TaiKhoan_ngayCapNhat DEFAULT GETDATE(),
    lyDoKhoa         NVARCHAR(500) NULL,
    ngayKhoa         DATETIME      NULL,
    soLanDangNhapSai INT           NOT NULL CONSTRAINT DF_TaiKhoan_soLanSai DEFAULT 0,
    khoaDen          DATETIME      NULL                 -- khoa tam thoi 15 phut khi sai mat khau 5 lan
);
CREATE UNIQUE INDEX UX_TaiKhoan_email       ON TaiKhoan(email);
CREATE UNIQUE INDEX UX_TaiKhoan_soDienThoai ON TaiKhoan(soDienThoai);
CREATE UNIQUE INDEX UX_TaiKhoan_cccd        ON TaiKhoan(cccd) WHERE cccd IS NOT NULL;
GO

CREATE TABLE Phuong (
    maPhuong  INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Phuong PRIMARY KEY,
    tenPhuong NVARCHAR(100) NOT NULL
);
CREATE TABLE LoaiCoSo (
    maLoai  INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LoaiCoSo PRIMARY KEY,
    tenLoai NVARCHAR(100) NOT NULL
);
CREATE TABLE TienIch (
    maTienIch  INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_TienIch PRIMARY KEY,
    tenTienIch NVARCHAR(100) NOT NULL,
    nhom       VARCHAR(20)   NOT NULL CONSTRAINT CK_TienIch_nhom CHECK (nhom IN ('TIEN_NGHI', 'LAN_CAN')),
    icon       VARCHAR(100)  NULL
);
GO

/* ---- Co so luu tru ---- */
CREATE TABLE CoSoLuuTru (
    maCSLT         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CoSoLuuTru PRIMARY KEY,
    tenCSLT        NVARCHAR(150)  NOT NULL,
    diaChiChiTiet  NVARCHAR(200)  NOT NULL,
    moTa           NVARCHAR(500)  NULL,
    longitude      DECIMAL(10,7)  NULL,
    latitude       DECIMAL(10,7)  NULL,
    gia            DECIMAL(18,0)  NULL,
    trangThai      NVARCHAR(20)   NOT NULL
        CONSTRAINT CK_CSLT_trangThai CHECK (trangThai IN (N'DRAFT', N'PENDING', N'APPROVED', N'REJECTED', N'HIDDEN', N'REMOVED')),
    ngayTao        DATETIME       NOT NULL CONSTRAINT DF_CSLT_ngayTao DEFAULT GETDATE(),
    ngayCapNhat    DATETIME       NOT NULL CONSTRAINT DF_CSLT_ngayCapNhat DEFAULT GETDATE(),
    lyDoTuChoi     NVARCHAR(500)  NULL,
    lyDoViPham     NVARCHAR(500)  NULL,
    ngayGuiDuyet   DATETIME       NULL,
    ngayDuyet      DATETIME       NULL,
    ngayCapNhatGia DATETIME       NULL,
    maHost         INT NOT NULL CONSTRAINT FK_CSLT_Host   FOREIGN KEY REFERENCES TaiKhoan(id),
    maPhuong       INT NOT NULL CONSTRAINT FK_CSLT_Phuong FOREIGN KEY REFERENCES Phuong(maPhuong),
    maLoai         INT NOT NULL CONSTRAINT FK_CSLT_Loai   FOREIGN KEY REFERENCES LoaiCoSo(maLoai)
);
CREATE INDEX IX_CSLT_trangThai ON CoSoLuuTru(trangThai);
CREATE INDEX IX_CSLT_maHost    ON CoSoLuuTru(maHost);
CREATE INDEX IX_CSLT_maPhuong  ON CoSoLuuTru(maPhuong);
CREATE INDEX IX_CSLT_maLoai    ON CoSoLuuTru(maLoai);
GO

CREATE TABLE TienIchCSLT (
    maCSLT    INT NOT NULL CONSTRAINT FK_TICSLT_CSLT   FOREIGN KEY REFERENCES CoSoLuuTru(maCSLT) ON DELETE CASCADE,
    maTienIch INT NOT NULL CONSTRAINT FK_TICSLT_TienIch FOREIGN KEY REFERENCES TienIch(maTienIch),
    CONSTRAINT PK_TienIchCSLT PRIMARY KEY (maCSLT, maTienIch)
);

CREATE TABLE Media (
    mediaId   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Media PRIMARY KEY,
    duongDan  VARCHAR(500) NOT NULL,
    kichThuoc INT          NOT NULL,                    -- byte
    loai      VARCHAR(10)  NOT NULL CONSTRAINT CK_Media_loai CHECK (loai IN ('IMAGE', 'VIDEO')),
    mimeType  VARCHAR(50)  NOT NULL,
    thuTu     INT          NOT NULL,
    ngayTao   DATETIME     NOT NULL CONSTRAINT DF_Media_ngayTao DEFAULT GETDATE(),
    maCSLT    INT NOT NULL CONSTRAINT FK_Media_CSLT FOREIGN KEY REFERENCES CoSoLuuTru(maCSLT) ON DELETE CASCADE
);
CREATE INDEX IX_Media_maCSLT ON Media(maCSLT);

CREATE TABLE LienHe (
    maLienHe   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LienHe PRIMARY KEY,
    loaiLienHe NVARCHAR(20) NOT NULL CONSTRAINT CK_LienHe_loai CHECK (loaiLienHe IN (N'SO_DIEN_THOAI', N'ZALO', N'FACEBOOK')),
    giaTri     VARCHAR(255) NOT NULL,
    maCSLT     INT NOT NULL CONSTRAINT FK_LienHe_CSLT FOREIGN KEY REFERENCES CoSoLuuTru(maCSLT) ON DELETE CASCADE
);
CREATE INDEX IX_LienHe_maCSLT ON LienHe(maCSLT);
GO

/* ---- Xac thuc ---- */
CREATE TABLE PhienDangNhap (
    maPhien          VARCHAR(36)  NOT NULL CONSTRAINT PK_PhienDangNhap PRIMARY KEY,   -- UUID
    refreshTokenHash VARCHAR(255) NOT NULL,
    hetHanLuc        DATETIME     NOT NULL,
    daThuHoi         BIT          NOT NULL CONSTRAINT DF_Phien_daThuHoi DEFAULT 0,
    ngayTao          DATETIME     NOT NULL CONSTRAINT DF_Phien_ngayTao DEFAULT GETDATE(),
    maTaiKhoan       INT NOT NULL CONSTRAINT FK_Phien_TaiKhoan FOREIGN KEY REFERENCES TaiKhoan(id) ON DELETE CASCADE
);
CREATE INDEX IX_Phien_maTaiKhoan ON PhienDangNhap(maTaiKhoan);

CREATE TABLE OtpChallenge (
    challengeId   VARCHAR(36)   NOT NULL CONSTRAINT PK_OtpChallenge PRIMARY KEY,      -- UUID
    loai          NVARCHAR(20)  NOT NULL CONSTRAINT CK_Otp_loai CHECK (loai IN (N'SIGNUP', N'SIGNIN', N'FORGOT_PASSWORD')),
    email         VARCHAR(100)  NOT NULL,
    duLieuDangKy  NVARCHAR(MAX) NULL,                    -- payload dang ky cho OTP (mat khau da bcrypt); xoa khi het han
    otpHash       VARCHAR(255)  NOT NULL,
    soLanSai      INT           NOT NULL CONSTRAINT DF_Otp_soLanSai DEFAULT 0,
    hetHanLuc     DATETIME      NOT NULL,
    guiLaiSauLuc  DATETIME      NOT NULL,
    daDung        BIT           NOT NULL CONSTRAINT DF_Otp_daDung DEFAULT 0,
    maTaiKhoan    INT           NULL CONSTRAINT FK_Otp_TaiKhoan FOREIGN KEY REFERENCES TaiKhoan(id) ON DELETE CASCADE
);
CREATE INDEX IX_Otp_email ON OtpChallenge(email);

/* ---- Cau hinh he thong (SEO, AI...) dang khoa - gia tri ---- */
CREATE TABLE CauHinh (
    khoa        VARCHAR(100)  NOT NULL CONSTRAINT PK_CauHinh PRIMARY KEY,
    giaTri      NVARCHAR(MAX) NOT NULL,
    ngayCapNhat DATETIME      NOT NULL CONSTRAINT DF_CauHinh_ngayCapNhat DEFAULT GETDATE(),
    maTaiKhoan  INT NULL CONSTRAINT FK_CauHinh_TaiKhoan FOREIGN KEY REFERENCES TaiKhoan(id)
);
GO
PRINT N'Tao schema HotelNT xong.';
