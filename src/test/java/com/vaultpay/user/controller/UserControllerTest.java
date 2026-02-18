package com.vaultpay.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultpay.auth.security.UserPrincipal;
import com.vaultpay.common.exception.GlobalExceptionHandler;
import com.vaultpay.user.dto.request.SetTransactionPinRequest;
import com.vaultpay.user.dto.request.UpdateProfileRequest;
import com.vaultpay.user.dto.response.UserResponse;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.enums.KycLevel;
import com.vaultpay.user.enums.UserStatus;
import com.vaultpay.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "user@example.com";
    private static final UserResponse USER_RESPONSE = UserResponse.builder()
            .id(USER_ID)
            .email(EMAIL)
            .phoneNumber("+2348012345678")
            .firstName("John")
            .lastName("Doe")
            .status(UserStatus.ACTIVE.name())
            .kycLevel(KycLevel.TIER_1.name())
            .createdAt(LocalDateTime.now())
            .build();

    private static RequestPostProcessor withUserPrincipal() {
        User user = User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .passwordHash("hash")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+2348012345678")
                .status(UserStatus.ACTIVE)
                .kycLevel(KycLevel.TIER_1)
                .build();
        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(auth);
            return request;
        };
    }

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("should return 200 and user response when authenticated")
        void shouldReturn200WhenAuthenticated() throws Exception {
            when(userService.getUserById(USER_ID)).thenReturn(USER_RESPONSE);

            mockMvc.perform(get("/api/v1/users/me").with(withUserPrincipal()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(USER_ID))
                    .andExpect(jsonPath("$.data.email").value(EMAIL))
                    .andExpect(jsonPath("$.data.firstName").value("John"));

            verify(userService).getUserById(USER_ID);
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateProfile {

        @Test
        @DisplayName("should return 200 and updated user when request is valid")
        void shouldReturn200WhenValid() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith", null);
            UserResponse updated = UserResponse.builder()
                    .id(USER_ID)
                    .email(EMAIL)
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();
            when(userService.updateProfile(eq(USER_ID), any(UpdateProfileRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/v1/users/me")
                            .with(withUserPrincipal())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Profile updated"))
                    .andExpect(jsonPath("$.data.firstName").value("Jane"))
                    .andExpect(jsonPath("$.data.lastName").value("Smith"));

            verify(userService).updateProfile(eq(USER_ID), any(UpdateProfileRequest.class));
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest("Jane", null, null);
            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/me/pin")
    class SetTransactionPin {

        @Test
        @DisplayName("should return 200 when pin and confirm match")
        void shouldReturn200WhenMatch() throws Exception {
            SetTransactionPinRequest request = new SetTransactionPinRequest("1234", "1234");

            mockMvc.perform(post("/api/v1/users/me/pin")
                            .with(withUserPrincipal())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Transaction PIN set successfully"));

            verify(userService).setTransactionPin(eq(USER_ID), any(SetTransactionPinRequest.class));
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            SetTransactionPinRequest request = new SetTransactionPinRequest("1234", "1234");
            mockMvc.perform(post("/api/v1/users/me/pin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
