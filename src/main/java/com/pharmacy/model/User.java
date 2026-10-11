package com.pharmacy.model;

import java.time.LocalDateTime;

/** An application account. The password hash never leaves the server and is never logged. */
public class User extends Person {
    private static final long serialVersionUID = 1L;

    private String username;
    private transient String passwordHash;
    private Role role;
    private LocalDateTime lastLoginAt;

    @Override
    public String getCode() {
        return username;
    }

    @Override
    public String getAuditType() {
        return "USER";
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean can(Permission permission) {
        return isActive() && permission.isGrantedTo(role);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
