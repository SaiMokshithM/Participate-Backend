package com.participatehere.security;

import com.participatehere.entity.Admin;
import com.participatehere.entity.User;
import com.participatehere.repository.AdminRepository;
import com.participatehere.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public CustomUserDetailsService(UserRepository userRepository, AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // First, check if it's an Admin trying to log in via username
        Optional<Admin> adminOpt = adminRepository.findByUsername(identifier);
        if (adminOpt.isPresent()) {
            return new org.springframework.security.core.userdetails.User(
                    adminOpt.get().getUsername(),
                    adminOpt.get().getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        }

        // If not an Admin, fallback to checking if it's a User via email
        User user = userRepository.findByEmail(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User/Admin not found: " + identifier));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
