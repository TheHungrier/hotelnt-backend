package com.hotelnt.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm tra migration Liquibase chạy trên DB trống:
 * - Kiểm tra đủ bảng trong schema.
 * - ddl-auto=validate không báo lệch entity (SpringBoot context khởi tạo thành công với hibernate.ddl-auto=validate).
 * - Seed tham chiếu đủ dòng (Phuong >= 6, LoaiCoSo >= 5, TienIch >= 8).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class LiquibaseMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Kiểm tra đầy đủ các bảng được tạo từ Liquibase changelog")
    void verifyTablesExist() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'",
                String.class
        );

        List<String> expectedTables = List.of(
                "TaiKhoan",
                "LoaiCoSo",
                "Phuong",
                "CoSoLuuTru",
                "TienIch",
                "CoSo_TienIch",
                "Media",
                "CauHinh",
                "PhienDangNhap"
        );

        for (String table : expectedTables) {
            assertTrue(
                    tables.stream().anyMatch(t -> t.equalsIgnoreCase(table)),
                    "Bảng " + table + " phải tồn tại sau khi chạy migration Liquibase"
            );
        }
    }

    @Test
    @DisplayName("Kiểm tra dữ liệu tham chiếu (seed reference data) đã được nạp đủ dòng")
    void verifySeedReferenceData() {
        Integer phuongCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Phuong", Integer.class);
        assertNotNull(phuongCount);
        assertTrue(phuongCount >= 6, "Bảng Phuong phải có ít nhất 6 dòng tham chiếu");

        Integer loaiCoSoCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM LoaiCoSo", Integer.class);
        assertNotNull(loaiCoSoCount);
        assertTrue(loaiCoSoCount >= 5, "Bảng LoaiCoSo phải có ít nhất 5 dòng tham chiếu");

        Integer tienIchCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TienIch", Integer.class);
        assertNotNull(tienIchCount);
        assertTrue(tienIchCount >= 8, "Bảng TienIch phải có ít nhất 8 dòng tham chiếu");
    }
}
