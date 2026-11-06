package com.hotelnt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ListingApiTest {

    @Autowired MockMvc mvc;

    @Test void listOk() throws Exception {
        mvc.perform(get("/listings?maPhuong=1&maPhuong=2&limit=5"))
                .andExpect(status().isOk())
                // Du lieu mau: phuong 1,2 chi co co so 1 va 2 hien thi cho khach (7 bi tu choi, 8 thuoc Host bi khoa)
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.pagination.totalItems").value(2))
                .andExpect(jsonPath("$.pagination.limit").value(5));
    }

    @Test void notFound() throws Exception {
        mvc.perform(get("/listings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LISTING_NOT_FOUND"));
    }

    @Test void priceRangeInvalid() throws Exception {
        mvc.perform(get("/listings?giaMin=5&giaMax=1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test void limitTooBig() throws Exception {
        mvc.perform(get("/listings?limit=100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test void idNotNumber() throws Exception {
        mvc.perform(get("/listings/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
