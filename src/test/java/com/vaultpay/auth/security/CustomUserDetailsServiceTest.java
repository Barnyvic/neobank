package com.vaultpay.auth.security;

import com.vaultpay.user.entity.User;
import com.vaultpay.user.enums.KycLevel;
import com.vaultpay.user.enums.UserStatus;
import com.vaultpay.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    private static final String EMAIL = "user@example.com";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("should load UserDetails when user exists by email")
    void shouldLoadUserDetailsWhenUserExists() {
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .passwordHash("hash")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+2348012345678")
                .status(UserStatus.ACTIVE)
                .kycLevel(KycLevel.TIER_1)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(details).isInstanceOf(UserPrincipal.class);
        assertThat(details.getUsername()).isEqualTo(EMAIL);
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found")
                .hasMessageContaining(EMAIL);
    }

    @Test
    @DisplayName("should set enabled false when user status is not ACTIVE")
    void shouldSetEnabledFalseWhenUserSuspended() {
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .passwordHash("hash")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+2348012345678")
                .status(UserStatus.SUSPENDED)
                .kycLevel(KycLevel.TIER_1)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(details.isEnabled()).isFalse();
    }
}
