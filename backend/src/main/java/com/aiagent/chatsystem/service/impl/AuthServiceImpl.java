package com.aiagent.chatsystem.service.impl;

import com.aiagent.chatsystem.dto.AuthResponse;
import com.aiagent.chatsystem.dto.LoginRequest;
import com.aiagent.chatsystem.dto.RegisterRequest;
import com.aiagent.chatsystem.exception.EmailAlreadyExistsException;
import com.aiagent.chatsystem.exception.InvalidCredentialsException;
import com.aiagent.chatsystem.model.User;
import com.aiagent.chatsystem.repository.UserRepository;
import com.aiagent.chatsystem.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String jwtSecret;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${jwt.secret}") String jwtSecret) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        String token = generateToken(user.getId().toString());
        return new AuthResponse(token, user.getId().toString());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = generateToken(user.getId().toString());
        return new AuthResponse(token, user.getId().toString());
    }

    private String generateToken(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        Instant expiration = now.plus(7, ChronoUnit.DAYS);
        return Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }
}
