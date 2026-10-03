package com.cp.party_trip.controller;

import com.cp.party_trip.config.AuthGuard;
import com.cp.party_trip.model.User;
import com.cp.party_trip.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final AuthGuard guard;

    public UserController(UserService userService, AuthGuard guard) {
        this.userService = userService;
        this.guard = guard;
    }

    // เข้าด้วยชื่อเล่น: เครื่องเดิมส่ง token มาใน header, เครื่องใหม่ที่ชื่อมีเจ้าของแล้วต้องส่ง pin
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginOrRegister(
            @RequestParam String username,
            @RequestParam(required = false) String pin,
            @RequestHeader(value = AuthGuard.HEADER, required = false) String token) {
        UserService.Login login = userService.login(username, token, pin);
        Map<String, Object> body = userJson(login.user());
        body.put("token", login.token());
        return ResponseEntity.ok(body);
    }

    // ตั้ง/เปลี่ยน/ลบ PIN ของตัวเอง (ต้องมี token)
    @PostMapping("/pin")
    public ResponseEntity<Map<String, Object>> setPin(@RequestParam(required = false) String pin) {
        return ResponseEntity.ok(userJson(userService.setPin(guard.user(), pin)));
    }

    // ชื่อปัจจุบันของเจ้าของ token (เครื่องอื่นของเราใช้เช็กว่าชื่อถูกเปลี่ยนไปแล้วหรือยัง)
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        return ResponseEntity.ok(userJson(guard.user()));
    }

    // เปลี่ยนชื่อเล่นของตัวเอง (ต้องมี token) ทริปเดิมตามไปด้วย
    @PostMapping("/rename")
    public ResponseEntity<Map<String, Object>> rename(@RequestParam String username) {
        User user = userService.rename(guard.user(), username);
        Map<String, Object> body = userJson(user);
        body.put("token", user.getAuthToken());
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> userJson(User user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", user.getId());
        body.put("username", user.getUsername());
        body.put("pinSet", user.getPinHash() != null);
        return body;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("message", e.getReason() == null ? "" : e.getReason()));
    }
}
