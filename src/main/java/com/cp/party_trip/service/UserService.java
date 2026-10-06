package com.cp.party_trip.service;

import com.cp.party_trip.model.User;
import com.cp.party_trip.repository.UserRepo;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public User saveOrUpdateUser(String username) {
        Optional<User> existingUser = userRepo.findByUsername(username);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        User newUser = new User();
        newUser.setUsername(username);
        return userRepo.save(newUser);
    }
}