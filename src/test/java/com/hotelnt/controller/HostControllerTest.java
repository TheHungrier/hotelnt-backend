package com.hotelnt.controller;

import com.hotelnt.dto.HostDtos.*;
import com.hotelnt.helper.HostIdentityResolver.Identity;
import com.hotelnt.common.error.*;
import com.hotelnt.config.*;
import com.hotelnt.domain.*;
import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.hotelnt.repository.*;
import com.hotelnt.service.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.hotelnt.common.error.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HostController.class)
class HostControllerTest {
    @Autowired MockMvc mvc;
    @MockBean HostService service;
    @MockBean HostIdentityResolver identities;
    void authenticated(){when(identities.resolve(any())).thenReturn(new Identity(1,"session"));}
    @Test void missingAuthRejected()throws Exception {when(identities.resolve(any())).thenThrow(new AppException(ErrorCode.TOKEN_INVALID));mvc.perform(get("/host/profile")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));verifyNoInteractions(service);}
    @Test void cannotEditEmailEvenNull()throws Exception{authenticated();mvc.perform(patch("/host/profile").contentType("application/json").content("{\"email\":null,\"hoTen\":\"Dat\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("FIELD_NOT_EDITABLE"));verifyNoInteractions(service);}
    @Test void rejectsInvalidPhone()throws Exception{authenticated();mvc.perform(patch("/host/profile").contentType("application/json").content("{\"soDienThoai\":\"123\"}")).andExpect(status().isBadRequest());verifyNoInteractions(service);}
    @Test void rejectsMalformedJson()throws Exception{mvc.perform(post("/host/listings").contentType("application/json").content("{")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));}
    @Test void rejectsMissingRequiredListingFields()throws Exception{mvc.perform(post("/host/listings").contentType("application/json").content("{}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details").isArray());verifyNoInteractions(service);}
    @Test void rejectsFractionalPrice()throws Exception{mvc.perform(post("/host/listings").contentType("application/json").content("{\"tenCSLT\":\"Villa\",\"diaChiChiTiet\":\"12 Tran Phu\",\"maLoai\":1,\"maPhuong\":1,\"gia\":650000.5}")).andExpect(status().isBadRequest());}
    @Test void rejectsInvalidPagination()throws Exception{mvc.perform(get("/host/listings?page=0&limit=100")).andExpect(status().isBadRequest());}
    @Test void ownerFailureUsesCommonErrorEnvelope()throws Exception{authenticated();when(service.detail(any(),eq(10))).thenThrow(new AppException(ErrorCode.NOT_OWNER));mvc.perform(get("/host/listings/10")).andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.error.code").value("NOT_OWNER"));}
}