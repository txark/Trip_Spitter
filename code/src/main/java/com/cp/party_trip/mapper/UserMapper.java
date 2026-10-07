package com.cp.party_trip.mapper;

import com.cp.party_trip.dto.response.LoginResponse;
import com.cp.party_trip.dto.response.UserResponse;
import com.cp.party_trip.model.User;
import org.springframework.stereotype.Component;

// User -> DTO (ไม่ส่ง PIN/รหัสกู้คืนออกไป, token ส่งเฉพาะตอนเข้าสู่ระบบ/เปลี่ยนชื่อ)
@Component
public class UserMapper {
    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getPinHash() != null);
    }

    public LoginResponse toLogin(User user, String token) {
        return new LoginResponse(user.getId(), user.getUsername(), user.getPinHash() != null, token);
    }
}
