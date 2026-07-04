package com.fiapql.authapi.service

import com.fiapql.authapi.dto.AuthResponse
import com.fiapql.authapi.dto.LoginRequest
import com.fiapql.authapi.dto.RegisterRequest
import com.fiapql.authapi.entity.User
import com.fiapql.authapi.repository.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager
) {
    fun register(req: RegisterRequest): AuthResponse {
        require(!userRepository.existsByEmail(req.email)) { "E-mail já cadastrado" }

        val user = User(
            email = req.email,
            passwordHash = passwordEncoder.encode(req.password)
        )
        userRepository.save(user)
        return AuthResponse(jwtService.generateToken(user), user.email)
    }

    fun login(req: LoginRequest): AuthResponse {
        // lança BadCredentialsException se inválido
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(req.email, req.password)
        )
        val user = userRepository.findByEmail(req.email)
            .orElseThrow { BadCredentialsException("Usuário não encontrado") }
        return AuthResponse(jwtService.generateToken(user), user.email)
    }
}
