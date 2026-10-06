package com.cp.party_trip.controller;

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

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // เข้าด้วยชื่อเล่น: เครื่องเดิมส่ง token มาใน header,
    // เครื่องใหม่ที่ชื่อมีเจ้าของแล้วต้องส่ง pin
    @PostMapping("/login")
    public ResponseEntity<User> loginOrRegister(@RequestParam String username) {
        User user = userService.saveOrUpdateUser(username);
        return ResponseEntity.ok(user);
    }
}
