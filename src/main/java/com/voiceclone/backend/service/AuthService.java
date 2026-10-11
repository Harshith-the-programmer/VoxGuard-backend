package com.voiceclone.backend.service;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.voiceclone.backend.dto.LoginRequest;
import com.voiceclone.backend.dto.RegisterRequest;
import com.voiceclone.backend.entity.User;
import com.voiceclone.backend.repository.UserRepository;
import com.voiceclone.backend.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String register(RegisterRequest request) {
        String email = request.getEmail().trim();

        if (userRepository.existsByEmail(email)) {
            return "Email already registered";
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
        return "Registration successful";
    }

    /**
     * Authenticates a user and returns the token plus non-sensitive profile
     * fields needed by the frontend. Returns null for invalid credentials.
     */
    public Map<String, String> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            return null;
        }

        String token = jwtService.generateToken(user.getEmail());
        return Map.of(
                "token", token,
                "name", user.getName(),
                "email", user.getEmail()
        );
    }

    /** Returns only profile fields; the password hash is never exposed. */
    public Map<String, String> getUserDetails(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return Map.of(
                "name", user.getName(),
                "email", user.getEmail()
        );
    }
}
