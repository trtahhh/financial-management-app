package com.example.finance.service;
import com.example.finance.entity.User;
import com.example.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
    @Autowired UserRepository repo;
    @Autowired PasswordEncoder encoder;
    
    public User register(String username, String email, String password, String role) {
        System.out.println("UserService.register called with role: " + role); // Debug log
        
        if (repo.findByEmail(email).isPresent()) throw new RuntimeException("Email exists");
        if (repo.findByUsername(username).isPresent()) throw new RuntimeException("Username exists");
        
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(role != null ? role : "user"); // Sử dụng role từ parameter
        u.setCreatedAt(new java.util.Date());
        
        System.out.println("Saving user with role: " + u.getRole()); // Debug log
        User savedUser = repo.save(u);
        System.out.println("Saved user role: " + savedUser.getRole()); // Debug log
        return savedUser;
    }
    
    public Optional<User> findByEmail(String email) {
        return repo.findByEmail(email);
    }
}