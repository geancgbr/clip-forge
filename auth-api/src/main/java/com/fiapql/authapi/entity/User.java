package com.fiapql.authapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    // ── UserDetails ────────────────────────────────────────────────────────────

    @Override public String getUsername()                                          { return email; }
    @Override public String getPassword()                                          { return passwordHash; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities()      { return List.of(); }
    @Override public boolean isAccountNonExpired()                                 { return true; }
    @Override public boolean isAccountNonLocked()                                  { return true; }
    @Override public boolean isCredentialsNonExpired()                             { return true; }
    @Override public boolean isEnabled()                                           { return true; }
}
