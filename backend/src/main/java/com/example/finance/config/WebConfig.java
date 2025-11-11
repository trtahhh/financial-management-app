package com.example.finance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
 @Override
 public void configurePathMatch(@NonNull PathMatchConfigurer configurer) {
 configurer.setPatternParser(null); 
 }

 // CORS configuration moved to CorsConfig.java to avoid conflicts
 // @Override
 // public void addCorsMappings(CorsRegistry registry) {
 // registry.addMapping("/api/**")
 // .allowedOriginPatterns("http://localhost:3000")
 // .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
 // .allowedHeaders("*")
 // .allowCredentials(true);
 // }
}
