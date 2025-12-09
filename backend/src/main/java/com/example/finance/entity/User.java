package com.example.finance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "Users")
public class User {
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(unique = true, nullable = false)
 private String username;

 private String email;

 @Column(name = "password_hash", nullable = false)
 @JsonIgnore
 private String passwordHash;

 @Column(name = "role")
 private String role = "USER";

 @Column(name = "is_active")
 private Boolean isActive = true;

 @Column(name = "email_verified")
 private Boolean emailVerified = false;

 @Column(name = "email_verification_token")
 private String emailVerificationToken;

 @Column(name = "email_verification_expires")
 private LocalDateTime emailVerificationExpires;

 @Column(name = "password_reset_token")
 private String passwordResetToken;

 @Column(name = "password_reset_expires")
 private LocalDateTime passwordResetExpires;

 @Column(name = "created_at")
 private LocalDateTime createdAt;

 @Column(name = "updated_at")
 private LocalDateTime updatedAt;

 @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
 @JsonIgnore
 private Set<Goal> goals = new HashSet<>();

 // Getters and Setters
 public Long getId() {
 return id;
 }

 public void setId(Long id) {
 this.id = id;
 }

 public String getUsername() {
 return username;
 }

 public void setUsername(String username) {
 this.username = username;
 }

 public String getEmail() {
 return email;
 }

 public void setEmail(String email) {
 this.email = email;
 }

 public String getPasswordHash() {
 return passwordHash;
 }

 public void setPasswordHash(String passwordHash) {
 this.passwordHash = passwordHash;
 }

 public String getRole() {
 return role;
 }

 public void setRole(String role) {
 this.role = role;
 }

 public Boolean getIsActive() {
 return isActive;
 }

 public void setIsActive(Boolean isActive) {
 this.isActive = isActive;
 }

 public LocalDateTime getCreatedAt() {
 return createdAt;
 }

 public void setCreatedAt(LocalDateTime createdAt) {
 this.createdAt = createdAt;
 }

 public LocalDateTime getUpdatedAt() {
 return updatedAt;
 }

 public void setUpdatedAt(LocalDateTime updatedAt) {
 this.updatedAt = updatedAt;
 }

 public Set<Goal> getGoals() {
 return goals;
 }

 public void setGoals(Set<Goal> goals) {
 this.goals = goals;
 }

 public Boolean getEmailVerified() {
 return emailVerified;
 }

 public void setEmailVerified(Boolean emailVerified) {
 this.emailVerified = emailVerified;
 }

 public String getEmailVerificationToken() {
 return emailVerificationToken;
 }

 public void setEmailVerificationToken(String emailVerificationToken) {
 this.emailVerificationToken = emailVerificationToken;
 }

 public LocalDateTime getEmailVerificationExpires() {
 return emailVerificationExpires;
 }

 public void setEmailVerificationExpires(LocalDateTime emailVerificationExpires) {
 this.emailVerificationExpires = emailVerificationExpires;
 }

 public String getPasswordResetToken() {
 return passwordResetToken;
 }

 public void setPasswordResetToken(String passwordResetToken) {
 this.passwordResetToken = passwordResetToken;
 }

 public LocalDateTime getPasswordResetExpires() {
 return passwordResetExpires;
 }

 public void setPasswordResetExpires(LocalDateTime passwordResetExpires) {
 this.passwordResetExpires = passwordResetExpires;
 }

 @Override
 public boolean equals(Object o) {
 if (this == o) return true;
 if (o == null || getClass() != o.getClass()) return false;
 User user = (User) o;
 return Objects.equals(id, user.id);
 }

 @Override
 public int hashCode() {
 return Objects.hash(id);
 }
}
