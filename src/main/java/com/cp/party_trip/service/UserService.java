package com.cp.party_trip.service;

import com.cp.party_trip.model.User;
import com.cp.party_trip.repository.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

// ตัวตนของผู้ใช้: ชื่อเล่น + token ต่อเครื่อง (+ PIN ไว้เข้าชื่อเดิมจากเครื่องอื่น)
// - ชื่อใหม่ = สร้างบัญชี ได้ token ทันที
// - ชื่อที่มีคนใช้แล้ว = ต้องมี token ของชื่อนั้น หรือกรอก PIN ถูก (กันพิมพ์ชื่อเพื่อนแล้วสวมรอย)
// - บัญชีเก่าก่อนมีระบบนี้ (ยังไม่มี token) = เครื่องแรกที่เข้าชื่อนั้นได้ไป
@Service
public class UserService {
    static final int MAX_PIN_ATTEMPTS = 5;
    static final Duration PIN_LOCK = Duration.ofMinutes(10);
    private static final Pattern PIN = Pattern.compile("\\d{4,6}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepo userRepo;
    // กันเดา PIN: ผิดครบ 5 ครั้ง ล็อกชื่อนั้น 10 นาที (เก็บในหน่วยความจำ รีสตาร์ตแล้วเริ่มนับใหม่)
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    private record Attempts(int count, Instant lockedUntil) {
    }

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    // ผลการเข้าสู่ระบบ: บัญชี + token ที่หน้าเว็บต้องเก็บไว้
    public record Login(User user, String token) {
    }

    public Login login(String username, String token, String pin) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty() || name.length() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ชื่อเล่นต้องมี 1–40 ตัวอักษร");
        }
        Optional<User> existing = userRepo.findByUsername(name);
        if (existing.isEmpty()) {
            User user = new User();
            user.setUsername(name);
            user.setAuthToken(newToken());
            if (pin != null && !pin.isBlank()) {
                user.setPinHash(hashPin(requireValidPin(pin)));
            }
            return new Login(userRepo.save(user), user.getAuthToken());
        }

        User user = existing.get();
        // เครื่องเดิม (มี token ของชื่อนี้อยู่แล้ว)
        if (token != null && !token.isBlank() && token.equals(user.getAuthToken())) {
            return new Login(user, user.getAuthToken());
        }
        // บัญชีเก่าที่ยังไม่เคยมี token: เครื่องแรกที่เข้าได้ไป
        if (user.getAuthToken() == null) {
            user.setAuthToken(newToken());
            return new Login(userRepo.save(user), user.getAuthToken());
        }
        // เครื่องอื่น: ต้องใช้ PIN
        if (user.getPinHash() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ชื่อนี้มีคนใช้แล้ว ใช้ชื่ออื่น หรือให้เจ้าของชื่อตั้ง PIN ก่อนแล้วเข้าด้วย PIN");
        }
        if (pin == null || pin.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PIN_REQUIRED");
        }
        checkNotLocked(name);
        if (!matchesPin(pin.trim(), user.getPinHash())) {
            recordFailure(name);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PIN ไม่ถูกต้อง");
        }
        attempts.remove(name);
        return new Login(user, user.getAuthToken());
    }

    // ตั้ง/เปลี่ยน PIN ของตัวเอง (null/ว่าง = ลบ PIN)
    public User setPin(User user, String pin) {
        user.setPinHash(pin == null || pin.isBlank() ? null : hashPin(requireValidPin(pin)));
        return userRepo.save(user);
    }

    public Optional<User> findByToken(String token) {
        if (token == null || token.isBlank() || token.length() > 64) {
            return Optional.empty();
        }
        return userRepo.findByAuthToken(token);
    }

    // เดิม: เข้าด้วยชื่ออย่างเดียว (ยังใช้ในส่วนที่ไม่ต้องยืนยันตัวตน)
    public User saveOrUpdateUser(String username) {
        Optional<User> existingUser = userRepo.findByUsername(username);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        User newUser = new User();
        newUser.setUsername(username);
        return userRepo.save(newUser);
    }

    private void checkNotLocked(String name) {
        Attempts a = attempts.get(name);
        if (a != null && a.lockedUntil() != null && Instant.now().isBefore(a.lockedUntil())) {
            long minutes = Math.max(1, Duration.between(Instant.now(), a.lockedUntil()).toMinutes() + 1);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "ใส่ PIN ผิดหลายครั้ง ลองใหม่ในอีก " + minutes + " นาที");
        }
    }

    private void recordFailure(String name) {
        attempts.compute(name, (k, a) -> {
            int count = (a == null || (a.lockedUntil() != null && Instant.now().isAfter(a.lockedUntil())))
                    ? 1
                    : a.count() + 1;
            return new Attempts(count, count >= MAX_PIN_ATTEMPTS ? Instant.now().plus(PIN_LOCK) : null);
        });
    }

    private static String requireValidPin(String pin) {
        String p = pin.trim();
        if (!PIN.matcher(p).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PIN ต้องเป็นตัวเลข 4–6 หลัก");
        }
        return p;
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes); // 48 ตัวอักษร
    }

    // PBKDF2 + salt สุ่ม: "salt:hash" (base64)
    static String hashPin(String pin) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(pbkdf2(pin, salt));
    }

    static boolean matchesPin(String pin, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        return MessageDigest.isEqual(expected, pbkdf2(pin, salt));
    }

    private static byte[] pbkdf2(String pin, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 60_000, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("hash PIN ไม่ได้", e);
        }
    }
}
