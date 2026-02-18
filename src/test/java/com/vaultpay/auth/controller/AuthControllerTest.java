package com.vaultpay.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultpay.auth.dto.request.LoginRequest;
import com.vaultpay.auth.dto.request.RefreshTokenRequest;
import com.vaultpay.auth.dto.request.RegisterRequest;
import com.vaultpay.auth.dto.response.AuthResponse;
import com.vaultpay.auth.service.AuthService;
import com.vaultpay.common.exception.GlobalExceptionHandler;
import com.vaultpay.common.exception.DuplicateResourceException;
import com.vaultpay.common.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "SecurePass1!";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String PHONE = "+2348012345678";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("should return 201 and auth response when registration succeeds")
        void shouldReturn201WhenRegistrationSucceeds() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    FIRST_NAME, LAST_NAME, EMAIL, PHONE, PASSWORD);
            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(ACCESS_TOKEN)
                    .refreshToken(REFRESH_TOKEN)
                    .tokenType("Bearer")
                    .expiresIn(900L)
                    .build();
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.data.accessToken").value(ACCESS_TOKEN))
                    .andExpect(jsonPath("$.data.refreshToken").value(REFRESH_TOKEN))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(900));

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("should return 409 when email already exists")
        void shouldReturn409WhenEmailExists() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    FIRST_NAME, LAST_NAME, EMAIL, PHONE, PASSWORD);
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new DuplicateResourceException("User", EMAIL));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 400 when request body is invalid")
        void shouldReturn400WhenInvalidBody() throws Exception {
            String invalidBody = "{\"firstName\":\"\",\"email\":\"not-an-email\"}";

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("should return 200 and auth response when login succeeds")
        void shouldReturn200WhenLoginSucceeds() throws Exception {
            LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(ACCESS_TOKEN)
                    .refreshToken(REFRESH_TOKEN)
                    .tokenType("Bearer")
                    .expiresIn(900L)
                    .build();
            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value(ACCESS_TOKEN));

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("should return 401 when credentials are invalid")
        void shouldReturn401WhenInvalidCredentials() throws Exception {
            LoginRequest request = new LoginRequest(EMAIL, "wrong");
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("should return 200 and new tokens when refresh token is valid")
        void shouldReturn200WhenRefreshValid() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);
            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken("new-access")
                    .refreshToken("new-refresh")
                    .tokenType("Bearer")
                    .expiresIn(900L)
                    .build();
            when(authService.refreshToken(REFRESH_TOKEN)).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));

            verify(authService).refreshToken(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("should return 401 when refresh token is invalid")
        void shouldReturn401WhenRefreshInvalid() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("invalid");
            when(authService.refreshToken("invalid"))
                    .thenThrow(new UnauthorizedException("Invalid or expired refresh token"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("should return 200 when logout succeeds")
        void shouldReturn200WhenLogoutSucceeds() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));

            verify(authService).logout(REFRESH_TOKEN);
        }
    }
}
