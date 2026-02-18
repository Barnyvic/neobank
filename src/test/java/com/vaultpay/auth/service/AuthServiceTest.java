package com.vaultpay.auth.service;

import com.vaultpay.auth.dto.request.LoginRequest;
import com.vaultpay.auth.dto.request.RegisterRequest;
import com.vaultpay.auth.dto.response.AuthResponse;
import com.vaultpay.auth.security.UserPrincipal;
import com.vaultpay.auth.service.impl.AuthServiceImpl;
import com.vaultpay.common.exception.DuplicateResourceException;
import com.vaultpay.common.exception.UnauthorizedException;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.enums.KycLevel;
import com.vaultpay.user.enums.UserStatus;
import com.vaultpay.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "SecurePass1!";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String PHONE = "+2348012345678";
    private static final Long USER_ID = 1L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", 900_000L);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register new user and return tokens")
        void shouldRegisterNewUser() {
            RegisterRequest request = new RegisterRequest(
                    FIRST_NAME, LAST_NAME, EMAIL, PHONE, PASSWORD);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByPhoneNumber(PHONE)).thenReturn(false);
            User savedUser = User.builder()
                    .id(USER_ID)
                    .email(EMAIL)
                    .phoneNumber(PHONE)
                    .firstName(FIRST_NAME)
                    .lastName(LAST_NAME)
                    .passwordHash("hashed")
                    .status(UserStatus.ACTIVE)
                    .kycLevel(KycLevel.TIER_1)
                    .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed");
            when(jwtService.generateAccessToken(any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenStore.createToken(USER_ID)).thenReturn(REFRESH_TOKEN);

            AuthResponse response = authService.register(request);

            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(900L);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User captured = userCaptor.getValue();
            assertThat(captured.getEmail()).isEqualTo(EMAIL);
            assertThat(captured.getPhoneNumber()).isEqualTo(PHONE);
            assertThat(captured.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(captured.getLastName()).isEqualTo(LAST_NAME);
            assertThat(captured.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(captured.getKycLevel()).isEqualTo(KycLevel.TIER_1);
        }

        @Test
        @DisplayName("should throw when email already exists")
        void shouldThrowWhenEmailExists() {
            RegisterRequest request = new RegisterRequest(
                    FIRST_NAME, LAST_NAME, EMAIL, PHONE, PASSWORD);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining(EMAIL);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when phone already exists")
        void shouldThrowWhenPhoneExists() {
            RegisterRequest request = new RegisterRequest(
                    FIRST_NAME, LAST_NAME, EMAIL, PHONE, PASSWORD);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.existsByPhoneNumber(PHONE)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining(PHONE);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should normalise email to lowercase")
        void shouldNormaliseEmail() {
            RegisterRequest request = new RegisterRequest(
                    FIRST_NAME, LAST_NAME, "User@Example.COM", PHONE, PASSWORD);
            when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
            when(userRepository.existsByPhoneNumber(PHONE)).thenReturn(false);
            User savedUser = User.builder().id(USER_ID).email("user@example.com").build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(jwtService.generateAccessToken(any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenStore.createToken(USER_ID)).thenReturn(REFRESH_TOKEN);

            authService.register(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should return tokens when credentials are valid")
        void shouldReturnTokensWhenValid() {
            LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
            User user = User.builder().id(USER_ID).email(EMAIL).passwordHash("hashed").build();
            UserPrincipal principal = new UserPrincipal(user);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);
            when(jwtService.generateAccessToken(principal)).thenReturn(ACCESS_TOKEN);
            when(refreshTokenStore.createToken(USER_ID)).thenReturn(REFRESH_TOKEN);

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.getTokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("should pass normalised email to authentication manager")
        void shouldPassNormalisedEmail() {
            LoginRequest request = new LoginRequest("  User@Example.COM  ", PASSWORD);
            User user = User.builder().id(USER_ID).email(EMAIL).passwordHash("hashed").build();
            UserPrincipal principal = new UserPrincipal(user);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);
            when(jwtService.generateAccessToken(any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenStore.createToken(USER_ID)).thenReturn(REFRESH_TOKEN);

            authService.login(request);

            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshToken {

        @Test
        @DisplayName("should return new tokens and revoke old refresh token")
        void shouldReturnNewTokensAndRevokeOld() {
            User user = User.builder().id(USER_ID).email(EMAIL).passwordHash("hashed").build();
            when(refreshTokenStore.getUserId(REFRESH_TOKEN)).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(refreshTokenStore.createToken(USER_ID)).thenReturn("new-refresh");
            when(jwtService.generateAccessToken(any())).thenReturn("new-access");

            AuthResponse response = authService.refreshToken(REFRESH_TOKEN);

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
            verify(refreshTokenStore).revoke(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("should throw when refresh token is invalid or expired")
        void shouldThrowWhenRefreshTokenInvalid() {
            when(refreshTokenStore.getUserId(REFRESH_TOKEN)).thenReturn(null);

            assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid or expired refresh token");
            verify(refreshTokenStore, never()).revoke(any());
        }

        @Test
        @DisplayName("should throw when user not found for refresh token userId")
        void shouldThrowWhenUserNotFound() {
            when(refreshTokenStore.getUserId(REFRESH_TOKEN)).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("should revoke refresh token")
        void shouldRevokeRefreshToken() {
            authService.logout(REFRESH_TOKEN);
            verify(refreshTokenStore).revoke(REFRESH_TOKEN);
        }
    }
}
