package com.example.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "User_Profile")
public class UserProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    private String fullName;
    private LocalDate birthday;
    private String gender;
    private String phone;
    private String address;
    @Column(name = "image_url")
    private String imageUrl;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
}
