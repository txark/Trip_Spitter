package com.cp.party_trip.service;

import com.cp.party_trip.model.User;

import java.util.Optional;

// หาผู้ใช้จาก token ใน header (AuthInterceptor ใช้แค่นี้ จึงแยกออกจาก UserService ตาม Interface Segregation)
public interface AuthTokenService {
    Optional<User> findByToken(String token);
}
