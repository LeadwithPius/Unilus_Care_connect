
package com.unilus.careconnect.service;

import com.unilus.careconnect.entity.UserAccount;
import com.unilus.careconnect.entity.UserRole;
import com.unilus.careconnect.repository.UserAccountRepository;
import com.unilus.careconnect.repository.UserRoleRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;

    public CustomUserDetailsService(
            UserAccountRepository userAccountRepository,
            UserRoleRepository userRoleRepository) {

        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        UserAccount account = userAccountRepository
                .findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Invalid email or password."));

        List<UserRole> roles =
                userRoleRepository.findAllByUserId(account.getId());

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(
                        "ROLE_" + role.getRoleName()))
                .toList();

        boolean accountIsActive =
                "ACTIVE".equals(account.getAccountStatus());

        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!accountIsActive)
                .build();
    }
} 