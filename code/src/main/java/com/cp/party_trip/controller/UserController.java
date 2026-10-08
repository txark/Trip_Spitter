package com.cp.party_trip.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.dto.response.LoginResponse;
import com.cp.party_trip.dto.response.UserResponse;
import com.cp.party_trip.mapper.UserMapper;
import com.cp.party_trip.model.User;
import com.cp.party_trip.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users - ผู้ใช้และการเข้าสู่ระบบ")
@RestController
@RequestMapping("/api/v1")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthGuard guard;

    public UserController(UserService userService, UserMapper userMapper, AuthGuard guard) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.guard = guard;
    }

    // เข้าด้วยชื่อเล่น: เครื่องเดิมส่ง token มาใน header, เครื่องใหม่ที่ชื่อมีเจ้าของแล้วต้องส่ง pin
    @PostMapping("/sessions")
    public ResponseEntity<LoginResponse> loginOrRegister(
            @RequestParam String username,
            @RequestParam(required = false) String pin,
            @RequestHeader(value = AuthGuard.HEADER, required = false) String token) {
        UserService.Login login = userService.login(username, token, pin);
        return ResponseEntity.ok(userMapper.toLogin(login.user(), login.token()));
    }

    // ตั้ง/เปลี่ยน/ลบ PIN ของตัวเอง (ต้องมี token)
    @PutMapping("/users/me/pin")
    public ResponseEntity<UserResponse> setPin(@RequestParam(required = false) String pin) {
        return ResponseEntity.ok(userMapper.toResponse(userService.setPin(guard.user(), pin)));
    }

    // ชื่อปัจจุบันของเจ้าของ token (เครื่องอื่นของเราใช้เช็กว่าชื่อถูกเปลี่ยนไปแล้วหรือยัง)
    @GetMapping("/users/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(userMapper.toResponse(guard.user()));
    }

    // เปลี่ยนชื่อเล่นของตัวเอง (ต้องมี token) ทริปเดิมตามไปด้วย
    @PutMapping("/users/me/username")
    public ResponseEntity<LoginResponse> rename(@RequestParam String username) {
        User user = userService.rename(guard.user(), username);
        return ResponseEntity.ok(userMapper.toLogin(user, user.getAuthToken()));
    }
}
