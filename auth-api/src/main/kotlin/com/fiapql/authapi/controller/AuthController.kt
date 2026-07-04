package com.fiapql.authapi.controller

import com.fiapql.authapi.dto.AuthResponse
import com.fiapql.authapi.dto.LoginRequest
import com.fiapql.authapi.dto.RegisterRequest
import com.fiapql.authapi.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    /** POST /auth/register  →  201 + { token, email } */
    @PostMapping("/register")
    fun register(@Valid @RequestBody req: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req))

    /** POST /auth/login  →  200 + { token, email } */
    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(req))
}
