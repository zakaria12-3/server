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
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name="verification_code")
    private String verificationCode;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name="verification_expiration")
    private LocalDateTime verificationCodeExpiresAt;
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    private String approvalStatus;

    private String bio;
    private String headline;
    private String location;
    private String avatarUrl;
    private String phone;
    private String jobTitle;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    private Integer riskScore = 0;
    private Boolean reported = false;
    private Boolean suspended = false;
    private String suspensionReason;

    public int getRiskScore() { return riskScore == null ? 0 : riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public boolean isReported() { return reported != null && reported; }
    public void setReported(Boolean reported) { this.reported = reported; }
    public boolean isSuspended() { return suspended != null && suspended; }
    public void setSuspended(Boolean suspended) { this.suspended = suspended; }

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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
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

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified) || (enabled && verificationCode == null);
    }

    public boolean getEmailVerified() {
        return isEmailVerified();
    }





}
