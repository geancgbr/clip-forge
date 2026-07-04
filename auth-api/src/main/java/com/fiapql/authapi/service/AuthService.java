package com.fiapql.authapi.service;

import com.fiapql.authapi.dto.*;
import com.fiapql.authapi.entity.User;
import com.fiapql.authapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        var user = User.builder()
            .email(req.email())
            .passwordHash(passwordEncoder.encode(req.password()))
            .build();
        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user), user.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        // lança BadCredentialsException se inválido
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
        var user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
        return new AuthResponse(jwtService.generateToken(user), user.getEmail());
    }
}
