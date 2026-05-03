package com.example.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;




@Entity
@Table(name="users")
@Getter
@Setter
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(unique = true, nullable = false)
    private String email;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false)
    private String password;
    private boolean enabled;
    @Column(name="verification_code")
    private String verificationCode;
    @Column(name="verification_expiration")
    private LocalDateTime verificationCodeExpiresAt;

    private String bio;
    private String headline;
    private String location;
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    public User(){

    }
    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return java.util.Collections.emptyList();
        }
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getUsername() {
        return email;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("username")
    public String getRealUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired(){
        return true;
    }
    @Override
    public boolean isAccountNonLocked(){
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }
    @Override
    public boolean isEnabled(){
        return enabled;
    }





}
