package com.example.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "Categories")
public class Category {
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(nullable = false)
 private String name;

 @Column(nullable = false)
 private String type; // expense, income

 private String color; // Hex color code

 private String icon; // Font Awesome icon class

 @Column(name = "created_at")
 private LocalDateTime createdAt = LocalDateTime.now();

 @Column(name = "is_active")
 private Boolean isActive = true;

 @Override
 public boolean equals(Object o) {
 if (this == o) return true;
 if (o == null || getClass() != o.getClass()) return false;
 Category category = (Category) o;
 return Objects.equals(id, category.id);
 }

 @Override
 public int hashCode() {
 return Objects.hash(id);
 }
}
