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

    private LocalDate birthday;
    private String gender;
    private String phone;
    private String address;
    private String imageUrl;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
}
