package com.example.finance.service;

import com.example.finance.entity.User;
import com.example.finance.entity.UserProfile;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public User save(User user) {
        return userRepository.save(user);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public UserProfile saveProfile(UserProfile profile) {
        try {
            // Tìm profile hiện tại nếu có
            Optional<UserProfile> existingProfile = userProfileRepository.findByUserId(profile.getUserId());
            
            if (existingProfile.isPresent()) {
                // Cập nhật profile hiện tại
                UserProfile currentProfile = existingProfile.get();
                
                // Cập nhật các trường
                if (profile.getFullName() != null) {
                    currentProfile.setFullName(profile.getFullName());
                }
                if (profile.getBirthday() != null) {
                    currentProfile.setBirthday(profile.getBirthday());
                }
                if (profile.getGender() != null) {
                    currentProfile.setGender(profile.getGender());
                }
                if (profile.getPhone() != null) {
                    currentProfile.setPhone(profile.getPhone());
                }
                if (profile.getAddress() != null) {
                    currentProfile.setAddress(profile.getAddress());
                }
                if (profile.getImageUrl() != null) {
                    currentProfile.setImageUrl(profile.getImageUrl());
                }
                
                // Lưu profile đã cập nhật
                return userProfileRepository.save(currentProfile);
            } else {
                // Tạo profile mới nếu chưa có
                // Đảm bảo User entity được merge trước khi persist UserProfile
                User user = entityManager.find(User.class, profile.getUserId());
                if (user != null) {
                    // Merge User entity để đảm bảo nó trong persistence context
                    user = entityManager.merge(user);
                    profile.setUser(user);
                }
                
                // Tạo profile mới
                return userProfileRepository.save(profile);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu profile: " + e.getMessage(), e);
        }
    }

    public Optional<UserProfile> findProfileByUserId(Long userId) {
        return userProfileRepository.findByUserId(userId);
    }
}
