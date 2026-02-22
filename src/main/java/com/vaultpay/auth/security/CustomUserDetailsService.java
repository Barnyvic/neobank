package com.vaultpay.auth.security;

import com.vaultpay.auth.repository.UserRoleRepository;
import com.vaultpay.user.entity.User;
import com.vaultpay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = userRoleRepository.findByUserIdWithRole(user.getId())
                .stream()
                .map(ur -> (GrantedAuthority) new SimpleGrantedAuthority(ur.getRole().getName()))
                .toList();

        if (authorities.isEmpty()) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new UserPrincipal(user, authorities);
    }
}
