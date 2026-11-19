package com.hotelnt.integration;

import com.hotelnt.domain.AccountStatus;
import com.hotelnt.domain.CoSoLuuTru;
import com.hotelnt.domain.ListingStatus;
import com.hotelnt.domain.TaiKhoan;
import com.hotelnt.repository.CoSoLuuTruRepository;
import com.hotelnt.repository.LoaiCoSoRepository;
import com.hotelnt.repository.PhuongRepository;
import com.hotelnt.repository.TaiKhoanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(ListingPessimisticLockIntegrationTest.PessimisticLockTestService.class)
class ListingPessimisticLockIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PessimisticLockTestService lockTestService;

    @Autowired
    private CoSoLuuTruRepository coSoRepo;

    @Autowired
    private TaiKhoanRepository taiKhoanRepo;

    @Autowired
    private PhuongRepository phuongRepo;

    @Autowired
    private LoaiCoSoRepository loaiRepo;

    @Service
    public static class PessimisticLockTestService {

        @Autowired
        private CoSoLuuTruRepository repo;

        @Transactional
        public void incrementPriceWithDelay(int listingId, long delta, long delayMs) {
            CoSoLuuTru coSo = repo.findByIdForUpdate(listingId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy CSLT ID: " + listingId));

            long currentPrice = coSo.getGia().longValue();

            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            coSo.setGia(BigDecimal.valueOf(currentPrice + delta));
            repo.saveAndFlush(coSo);
        }
    }

    @Test
    @DisplayName("Kiểm tra findByIdForUpdate: nhiều luồng đồng thời thao tác một cơ sở, khóa bi quan tuần tự hóa chính xác")
    void testConcurrentUpdatesAreSerializedCorrectly() throws Exception {
        // Tạo CSLT ban đầu với giá 1,000,000 VND
        TaiKhoan host = taiKhoanRepo.findByEmail("host1@hotelnt.local").orElseGet(() -> {
            TaiKhoan h = new TaiKhoan();
            h.setHoTen("Host Concurrency");
            h.setEmail("host_lock_test@hotelnt.local");
            h.setMatKhau("secret");
            h.setVaiTro("HOST");
            h.setTrangThai(AccountStatus.ACTIVE);
            return taiKhoanRepo.save(h);
        });

        CoSoLuuTru coSo = new CoSoLuuTru();
        coSo.setTenCSLT("Lock Concurrency Listing");
        coSo.setDiaChiChiTiet("123 Tran Phu");
        coSo.setGia(BigDecimal.valueOf(1_000_000L));
        coSo.setTrangThai(ListingStatus.APPROVED);
        coSo.setHost(host);
        coSo.setPhuong(phuongRepo.findAll().get(0));
        coSo.setLoai(loaiRepo.findAll().get(0));
        coSo.setNgayCapNhat(LocalDateTime.now());
        CoSoLuuTru saved = coSoRepo.saveAndFlush(coSo);
        int listingId = saved.getMaCSLT();

        int threadCount = 5;
        long deltaPerThread = 100_000L;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Đợi tất cả thread sẵn sàng cùng xuất phát
                    lockTestService.incrementPriceWithDelay(listingId, deltaPerThread, 50);
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Kích hoạt chạy đồng thời
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Tất cả các luồng phải hoàn thành trong 30 giây");
        assertTrue(errors.isEmpty(), "Không được có lỗi lock conflict hoặc exception: " + errors);
        assertEquals(threadCount, successCount.get(), "Cả 5 luồng phải cập nhật thành công");

        // Kiểm tra giá cuối cùng: 1,000,000 + 5 * 100,000 = 1,500,000
        CoSoLuuTru finalCoSo = coSoRepo.findById(listingId).orElseThrow();
        long expectedPrice = 1_000_000L + (threadCount * deltaPerThread);
        assertEquals(expectedPrice, finalCoSo.getGia().longValue(),
                "Pessimistic lock phải bảo toàn tính tuần tự, giá cuối cùng phải là " + expectedPrice);
    }
}
