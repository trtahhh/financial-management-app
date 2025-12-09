package com.example.finance.service;

import com.example.finance.entity.User;
import com.example.finance.repository.UserRepository;
import com.example.finance.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

 @Autowired
 UserRepository userRepository;

 @Override
 public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
 User user = userRepository.findByUsername(username)
 .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

 return new CustomUserDetails(
 user.getId(),
 user.getUsername(),
 user.getPasswordHash()
 );
 }
}
