package com.vaultpay.user.service;

import com.vaultpay.common.exception.BusinessException;
import com.vaultpay.common.exception.DuplicateResourceException;
import com.vaultpay.common.exception.ErrorCode;
import com.vaultpay.common.exception.ResourceNotFoundException;
import com.vaultpay.user.dto.request.SetTransactionPinRequest;
import com.vaultpay.user.dto.request.UpdateProfileRequest;
import com.vaultpay.user.dto.response.UserResponse;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.enums.KycLevel;
import com.vaultpay.user.enums.UserStatus;
import com.vaultpay.user.repository.UserRepository;
import com.vaultpay.user.service.PinAttemptService;
import com.vaultpay.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "user@example.com";
    private static final String PHONE = "+2348012345678";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final LocalDateTime CREATED_AT = LocalDateTime.now();

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PinAttemptService pinAttemptService;

    @InjectMocks
    private UserServiceImpl userService;

    private static User defaultUser() {
        User user = User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .phoneNumber(PHONE)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .kycLevel(KycLevel.TIER_1)
                .build();
        user.setCreatedAt(CREATED_AT);
        return user;
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return user response when user exists")
        void shouldReturnUserWhenExists() {
            User user = defaultUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            UserResponse response = userService.getUserById(USER_ID);

            assertThat(response.getId()).isEqualTo(USER_ID);
            assertThat(response.getEmail()).isEqualTo(EMAIL);
            assertThat(response.getPhoneNumber()).isEqualTo(PHONE);
            assertThat(response.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(response.getLastName()).isEqualTo(LAST_NAME);
            assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE.name());
            assertThat(response.getKycLevel()).isEqualTo(KycLevel.TIER_1.name());
            assertThat(response.getCreatedAt()).isEqualTo(CREATED_AT);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining(USER_ID.toString());
        }
    }

    @Nested
    @DisplayName("getUserByEmail")
    class GetUserByEmail {

        @Test
        @DisplayName("should return user response when user exists")
        void shouldReturnUserWhenExists() {
            User user = defaultUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            UserResponse response = userService.getUserByEmail(EMAIL);

            assertThat(response.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByEmail(EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(EMAIL);
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("should update firstName and lastName when provided")
        void shouldUpdateFirstNameAndLastName() {
            User user = defaultUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith", null);

            UserResponse response = userService.updateProfile(USER_ID, request);

            assertThat(response.getFirstName()).isEqualTo("Jane");
            assertThat(response.getLastName()).isEqualTo("Smith");
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getFirstName()).isEqualTo("Jane");
            assertThat(captor.getValue().getLastName()).isEqualTo("Smith");
        }

        @Test
        @DisplayName("should update phone when provided and not taken by another user")
        void shouldUpdatePhoneWhenNotTaken() {
            User user = defaultUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.findByPhoneNumber("+2348098765432")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            UpdateProfileRequest request = new UpdateProfileRequest(null, null, "+2348098765432");

            userService.updateProfile(USER_ID, request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPhoneNumber()).isEqualTo("+2348098765432");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when phone taken by another user")
        void shouldThrowWhenPhoneTaken() {
            User user = defaultUser();
            User otherUser = User.builder().id(2L).phoneNumber("+2348098765432").build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.findByPhoneNumber("+2348098765432")).thenReturn(Optional.of(otherUser));
            UpdateProfileRequest request = new UpdateProfileRequest(null, null, "+2348098765432");

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("+2348098765432");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow same user to keep their existing phone")
        void shouldAllowSameUserToKeepPhone() {
            User user = defaultUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            UpdateProfileRequest request = new UpdateProfileRequest(null, null, PHONE);

            userService.updateProfile(USER_ID, request);

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            UpdateProfileRequest request = new UpdateProfileRequest("Jane", null, null);

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("setTransactionPin")
    class SetTransactionPin {

        @Test
        @DisplayName("should set transaction pin when pin and confirm match")
        void shouldSetPinWhenMatch() {
            User user = defaultUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(passwordEncoder.encode("1234")).thenReturn("encoded-pin");
            SetTransactionPinRequest request = new SetTransactionPinRequest("1234", "1234");

            userService.setTransactionPin(USER_ID, request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getTransactionPin()).isEqualTo("encoded-pin");
        }

        @Test
        @DisplayName("should throw BusinessException when pin and confirm do not match")
        void shouldThrowWhenPinMismatch() {
            SetTransactionPinRequest request = new SetTransactionPinRequest("1234", "5678");

            assertThatThrownBy(() -> userService.setTransactionPin(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PIN and confirmation do not match");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            SetTransactionPinRequest request = new SetTransactionPinRequest("1234", "1234");

            assertThatThrownBy(() -> userService.setTransactionPin(USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("verifyTransactionPin")
    class VerifyTransactionPin {

        @Test
        @DisplayName("should return true and reset attempt counter when pin matches")
        void shouldReturnTrueWhenMatch() {
            User user = defaultUser();
            user.setTransactionPin("encoded-pin");
            when(pinAttemptService.isLocked(USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("1234", "encoded-pin")).thenReturn(true);

            boolean result = userService.verifyTransactionPin(USER_ID, "1234");

            assertThat(result).isTrue();
            verify(pinAttemptService).recordSuccess(USER_ID);
            verify(pinAttemptService, never()).recordFailure(any());
        }

        @Test
        @DisplayName("should return false and record failure when pin does not match")
        void shouldReturnFalseWhenNoMatch() {
            User user = defaultUser();
            user.setTransactionPin("encoded-pin");
            when(pinAttemptService.isLocked(USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "encoded-pin")).thenReturn(false);

            boolean result = userService.verifyTransactionPin(USER_ID, "wrong");

            assertThat(result).isFalse();
            verify(pinAttemptService).recordFailure(USER_ID);
            verify(pinAttemptService, never()).recordSuccess(any());
        }

        @Test
        @DisplayName("should return false when user has no transaction pin set")
        void shouldReturnFalseWhenNoPinSet() {
            User user = defaultUser();
            user.setTransactionPin(null);
            when(pinAttemptService.isLocked(USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            boolean result = userService.verifyTransactionPin(USER_ID, "1234");

            assertThat(result).isFalse();
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("should throw BusinessException with PIN_LOCKED when PIN is locked")
        void shouldThrowWhenPinLocked() {
            when(pinAttemptService.isLocked(USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> userService.verifyTransactionPin(USER_ID, "1234"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.PIN_LOCKED);
                    });
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(pinAttemptService.isLocked(USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.verifyTransactionPin(USER_ID, "1234"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
