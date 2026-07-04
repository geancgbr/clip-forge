package com.fiapql.authapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, unique = true)
    var email: String = "",

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
) : UserDetails {

    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
    }

    // ── UserDetails ────────────────────────────────────────────────────────────

    override fun getUsername(): String = email
    override fun getPassword(): String = passwordHash
    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}
