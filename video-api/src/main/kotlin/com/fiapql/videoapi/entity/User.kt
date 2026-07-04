package com.fiapql.videoapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

/** Leitura da tabela users (criada pelo auth-api). Apenas leitura aqui. */
@Entity
@Table(name = "users")
class User(
    @Id
    var id: UUID? = null,

    @Column(unique = true)
    var email: String = "",

    @Column(name = "password_hash")
    var passwordHash: String = ""
) : UserDetails {

    override fun getUsername(): String = email
    override fun getPassword(): String = passwordHash
    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}
