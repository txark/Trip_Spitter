package com.cp.party_trip.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

// ยืนยันตัวตนทำที่ AuthInterceptor + AuthGuard (token ใน header X-Auth-Token) ไม่ได้ใช้ระบบ login ของ Spring Security
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // ปิด CSRF เพื่อให้หน้าบ้านยิง POST API ได้
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // สิทธิ์ตรวจที่ AuthInterceptor/AuthGuard
                );
        return http.build();
    }
}
