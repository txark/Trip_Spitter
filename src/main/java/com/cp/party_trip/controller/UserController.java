package com.cp.party_trip.controller;

import com.cp.party_trip.model.User;
import com.cp.party_trip.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = { "http://127.0.0.1:5500", "http://localhost:5500" })
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<User> loginOrRegister(@RequestParam String username) {
        User user = userService.saveOrUpdateUser(username);
        return ResponseEntity.ok(user);
    }
}