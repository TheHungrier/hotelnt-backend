package com.hotelnt.controller;

import com.hotelnt.dto.SeoDtos.*;
import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** Test tang web (khong can SQL Server): service duoc gia lap. */
@WebMvcTest(SeoController.class)
class SeoControllerTest {

    @Autowired MockMvc mvc;
    @MockBean SeoService service;

    @Test void getOk() throws Exception {
        when(service.get()).thenReturn(new SeoResponse("Tieu de", "Mo ta", "khach-san-nha-trang", "OG", "https://x/og.jpg"));
        mvc.perform(get("/configurations/seo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metaTitle").value("Tieu de"))
                .andExpect(jsonPath("$.metaDescription").value("Mo ta"))
                .andExpect(jsonPath("$.slugUrl").value("khach-san-nha-trang"))
                .andExpect(jsonPath("$.ogTitle").value("OG"))
                .andExpect(jsonPath("$.ogImageUrl").value("https://x/og.jpg"));
    }
}