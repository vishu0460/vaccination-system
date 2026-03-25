package com.vaccine.security;

import com.vaccine.domain.Role;
import com.vaccine.domain.RoleName;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<String> authorities = new ArrayList<>();
        for (Role r : user.getRoles()) {
            String roleName = r.getName().name();
            authorities.add(roleName);
            authorities.add("ROLE_" + roleName);
        }
        String effectiveRole = user.getEffectiveRole();
        if (effectiveRole != null && !effectiveRole.isBlank()) {
            authorities.add(effectiveRole);
            authorities.add("ROLE_" + effectiveRole);
        }
        if (user.isSuperAdmin()) {
            authorities.add("SUPER_ADMIN");
            authorities.add("ROLE_SUPER_ADMIN");
        }
        if (user.isAdmin()) {
            authorities.add("ADMIN");
            authorities.add("ROLE_ADMIN");
        }
        if (!authorities.contains(RoleName.USER.name())) {
            authorities.add("USER"); // fallback
            authorities.add("ROLE_USER");
        }

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .disabled(!user.getEnabled())
            .authorities(authorities.toArray(new String[0]))
            .build();
    }
}
