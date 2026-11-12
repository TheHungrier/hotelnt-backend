package com.hotelnt.service;

import com.hotelnt.dto.*;
import com.hotelnt.helper.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.auth.jwt-secret=01234567890123456789012345678901")
class SignupFlowIntegrationTest {
        @Autowired
        MockMvc mvc;
        @Autowired
        ObjectMapper json;
        @MockBean
        OtpEmailSender email;

        @Test
        void signupVerifyRotateAndLogout() throws Exception {
                String id = UUID.randomUUID().toString();
                long number = Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 10_000_000L);
                String phone = String.format("099%07d", number);
                String cccd = String.format("98765%07d", number);
                String body = json.writeValueAsString(
                                new SignupRequest("Test Host", phone, id + "@example.com", cccd, "Password123!"));
                String signup = mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
                verify(email).send(anyString(), code.capture());
                String registrationId = json.readTree(signup).get("registrationId").asText();
                String verifyBody = json.writeValueAsString(new SignupVerifyRequest(registrationId, code.getValue()));
                String verified = mvc
                                .perform(post("/auth/signup/verify").contentType(MediaType.APPLICATION_JSON)
                                                .content(verifyBody))
                                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                JsonNode verifiedJson = json.readTree(verified);
                String refresh = verifiedJson.get("refreshToken").asText();
                String refreshed = mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(new RefreshTokenRequest(refresh))))
                                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                JsonNode refreshJson = json.readTree(refreshed);
                String newRefresh = refreshJson.get("refreshToken").asText();
                assertNotEquals(refresh, newRefresh);
                mvc.perform(post("/auth/logout")
                                .header("Authorization", "Bearer " + refreshJson.get("accessToken").asText())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(new RefreshTokenRequest(newRefresh))))
                                .andExpect(status().isOk());
                mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(new RefreshTokenRequest(newRefresh))))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void hostSigninAndPasswordRecovery() throws Exception {
                String id = UUID.randomUUID().toString();
                long number = Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 10_000_000L);
                String phone = String.format("098%07d", number);
                String cccd = String.format("87654%07d", number);
                String address = id + "@example.com";
                when(email.isConfigured()).thenReturn(true);
                String signup = mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(
                                                new SignupRequest("Test Host", phone, address, cccd, "Password123!"))))
                                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                ArgumentCaptor<String> codes = ArgumentCaptor.forClass(String.class);
                verify(email).send(anyString(), codes.capture());
                String registrationId = json.readTree(signup).get("registrationId").asText();
                String verifyBody = json.writeValueAsString(new SignupVerifyRequest(registrationId, codes.getValue()));
                String verified = mvc
                                .perform(post("/auth/signup/verify").contentType(MediaType.APPLICATION_JSON)
                                                .content(verifyBody))
                                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                String initialRefresh = json.readTree(verified).get("refreshToken").asText();
                String signin = mvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(new HostSigninRequest(address, "Password123!"))))
                                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                verify(email, times(2)).send(anyString(), codes.capture());
                String challengeId = json.readTree(signin).get("challengeId").asText();
                mvc.perform(post("/auth/signin/verify").contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(
                                                new ChallengeVerifyRequest(challengeId, codes.getValue()))))
                                .andExpect(status().isOk());
                String forgot = mvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(new ForgotPasswordRequest(address))))
                                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                verify(email, times(3)).send(anyString(), codes.capture());
                String forgotId = json.readTree(forgot).get("challengeId").asText();
                String verifiedForgot = mvc
                                .perform(post("/auth/forgot-password/verify").contentType(MediaType.APPLICATION_JSON)
                                                .content(json.writeValueAsString(new ChallengeVerifyRequest(forgotId,
                                                                codes.getValue()))))
                                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                String reset = json.readTree(verifiedForgot).get("resetToken").asText();
                String resetBody = json.writeValueAsString(new ResetPasswordRequest(reset, "NewPass123!"));
                mvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(resetBody))
                                .andExpect(status().isOk());
                mvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(resetBody))
                                .andExpect(status().isBadRequest());
                mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(new RefreshTokenRequest(initialRefresh))))
                                .andExpect(status().isUnauthorized());
                mvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(new HostSigninRequest(address, "NewPass123!"))))
                                .andExpect(status().isOk());
        }

        @Test
        void adminCanSignInWithSeedAccount() throws Exception {
                mvc.perform(post("/auth/admin/signin").contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(
                                                new AdminSigninRequest("admin@hotelnt.local", "Admin@123"))))
                                .andExpect(status().isOk());
        }
}