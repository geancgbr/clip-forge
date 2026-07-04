package com.fiapql.authapi;

import com.fiapql.authapi.dto.*;
import com.fiapql.authapi.entity.User;
import com.fiapql.authapi.repository.UserRepository;
import com.fiapql.authapi.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository        userRepository;
    @Mock JwtService            jwtService;
    @Mock AuthenticationManager authManager;

    @Spy PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks AuthService authService;

    @Test
    void register_deveRetornarToken() {
        var req = new RegisterRequest("gean@fiapx.com", "senha123");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        var resp = authService.register(req);

        assertThat(resp.token()).isEqualTo("jwt-token");
        assertThat(resp.email()).isEqualTo(req.email());
    }

    @Test
    void register_emailDuplicado_deveLancarExcecao() {
        var req = new RegisterRequest("gean@fiapx.com", "senha123");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("já cadastrado");
    }

    @Test
    void login_credenciaisValidas_deveRetornarToken() {
        var req  = new LoginRequest("gean@fiapx.com", "senha123");
        var user = User.builder().email(req.email()).passwordHash("hash").build();
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        var resp = authService.login(req);

        assertThat(resp.token()).isEqualTo("jwt-token");
    }
}
