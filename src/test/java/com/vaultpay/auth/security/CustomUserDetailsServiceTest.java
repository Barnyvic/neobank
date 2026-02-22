package com.vaultpay.auth.security;

import com.vaultpay.auth.entity.Role;
import com.vaultpay.auth.entity.UserRole;
import com.vaultpay.auth.repository.UserRoleRepository;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.enums.KycLevel;
import com.vaultpay.user.enums.UserStatus;
import com.vaultpay.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
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

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User buildUser(UserStatus status) {
        return User.builder()
                .id(1L)
                .email(EMAIL)
                .passwordHash("hash")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+2348012345678")
                .status(status)
                .kycLevel(KycLevel.TIER_1)
                .build();
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("should fall back to ROLE_USER when user has no roles assigned")
        void shouldFallBackToRoleUserWhenNoRolesAssigned() {
            User user = buildUser(UserStatus.ACTIVE);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userRoleRepository.findByUserIdWithRole(1L)).thenReturn(List.of());

            UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

            assertThat(details).isInstanceOf(UserPrincipal.class);
            assertThat(details.getUsername()).isEqualTo(EMAIL);
            assertThat(details.getPassword()).isEqualTo("hash");
            assertThat(details.getAuthorities()).hasSize(1);
            assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
            assertThat(details.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should load authorities from assigned database roles")
        void shouldLoadAuthoritiesFromDatabaseRoles() {
            User user = buildUser(UserStatus.ACTIVE);
            Role adminRole = Role.builder().id(1L).name("ROLE_ADMIN").build();
            UserRole userRole = UserRole.builder().id(1L).user(user).role(adminRole).build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userRoleRepository.findByUserIdWithRole(1L)).thenReturn(List.of(userRole));

            UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

            assertThat(details.getAuthorities()).hasSize(1);
            assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
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
        @DisplayName("should return isEnabled false for SUSPENDED user")
        void shouldReturnEnabledFalseForSuspendedUser() {
            User user = buildUser(UserStatus.SUSPENDED);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userRoleRepository.findByUserIdWithRole(1L)).thenReturn(List.of());

            UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

            assertThat(details.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should return isAccountNonLocked false for SUSPENDED user")
        void shouldReturnAccountLockedForSuspendedUser() {
            User user = buildUser(UserStatus.SUSPENDED);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userRoleRepository.findByUserIdWithRole(1L)).thenReturn(List.of());

            UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

            assertThat(details.isAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("should return isAccountNonLocked false for CLOSED user")
        void shouldReturnAccountLockedForClosedUser() {
            User user = buildUser(UserStatus.CLOSED);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userRoleRepository.findByUserIdWithRole(1L)).thenReturn(List.of());

            UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

            assertThat(details.isAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("should return isAccountNonLocked true for ACTIVE user")
        void shouldReturnAccountNonLockedForActiveUser() {
            User user = buildUser(UserStatus.ACTIVE);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userRoleRepository.findByUserIdWithRole(1L)).thenReturn(List.of());

            UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

            assertThat(details.isAccountNonLocked()).isTrue();
        }
    }
}
