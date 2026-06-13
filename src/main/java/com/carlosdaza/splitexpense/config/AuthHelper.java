package com.carlosdaza.splitexpense.config;

import com.carlosdaza.splitexpense.domain.entity.User;
import com.carlosdaza.splitexpense.exception.ResourceNotFoundException;
import com.carlosdaza.splitexpense.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }
}
