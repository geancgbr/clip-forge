package com.fiapql.authapi

import com.fiapql.authapi.dto.LoginRequest
import com.fiapql.authapi.dto.RegisterRequest
import com.fiapql.authapi.entity.User
import com.fiapql.authapi.repository.UserRepository
import com.fiapql.authapi.service.AuthService
import com.fiapql.authapi.service.JwtService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var jwtService: JwtService
    @Mock lateinit var authManager: AuthenticationManager

    @Spy var passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    @InjectMocks lateinit var authService: AuthService

    @Test
    fun `register deve retornar token`() {
        val req = RegisterRequest("gean@fiapx.com", "senha123")
        whenever(userRepository.existsByEmail(req.email)).thenReturn(false)
        whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
        whenever(jwtService.generateToken(any())).thenReturn("jwt-token")

        val resp = authService.register(req)

        assertThat(resp.token).isEqualTo("jwt-token")
        assertThat(resp.email).isEqualTo(req.email)
    }

    @Test
    fun `register com email duplicado deve lancar excecao`() {
        val req = RegisterRequest("gean@fiapx.com", "senha123")
        whenever(userRepository.existsByEmail(req.email)).thenReturn(true)

        assertThatThrownBy { authService.register(req) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("já cadastrado")
    }

    @Test
    fun `login com credenciais validas deve retornar token`() {
        val req = LoginRequest("gean@fiapx.com", "senha123")
        val user = User(email = req.email, passwordHash = "hash")
        whenever(userRepository.findByEmail(req.email)).thenReturn(Optional.of(user))
        whenever(jwtService.generateToken(user)).thenReturn("jwt-token")

        val resp = authService.login(req)

        assertThat(resp.token).isEqualTo("jwt-token")
    }
}
