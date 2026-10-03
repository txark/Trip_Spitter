package com.cp.party_trip.service;

import com.cp.party_trip.model.User;
import com.cp.party_trip.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepo userRepo;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepo.class);
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findByUsername(any())).thenReturn(Optional.empty());
        service = new UserService(userRepo, org.mockito.Mockito.mock(com.cp.party_trip.repository.TripMemberRepo.class));
    }

    private User existing(String name, String token, String pin) {
        User u = new User();
        u.setId(1L);
        u.setUsername(name);
        u.setAuthToken(token);
        u.setPinHash(pin == null ? null : UserService.hashPin(pin));
        when(userRepo.findByUsername(name)).thenReturn(Optional.of(u));
        return u;
    }

    private void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable call) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, call);
        assertEquals(status, e.getStatusCode());
    }

    @Test
    void newNameCreatesAccountWithToken() {
        UserService.Login login = service.login("  Joa ", null, null);
        assertEquals("Joa", login.user().getUsername());
        assertNotNull(login.token());
        assertEquals(48, login.token().length());
        assertNull(login.user().getPinHash());
    }

    @Test
    void sameDeviceTokenLogsIn() {
        existing("Joa", "tok-1", null);
        assertEquals("tok-1", service.login("Joa", "tok-1", null).token());
    }

    @Test
    void legacyAccountWithoutTokenIsClaimedOnce() {
        User u = existing("Old", null, null);
        String token = service.login("Old", null, null).token();
        assertNotNull(token);
        assertEquals(token, u.getAuthToken());
        // เครื่องอื่นพิมพ์ชื่อเดียวกันทีหลัง = เข้าไม่ได้ (ยังไม่ตั้ง PIN)
        assertStatus(HttpStatus.CONFLICT, () -> service.login("Old", null, null));
        assertStatus(HttpStatus.CONFLICT, () -> service.login("Old", "wrong", null));
    }

    @Test
    void otherDeviceNeedsCorrectPin() {
        existing("Joa", "tok-1", "1234");
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.login("Joa", null, null));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        assertEquals("PIN_REQUIRED", e.getReason());
        assertStatus(HttpStatus.UNAUTHORIZED, () -> service.login("Joa", null, "9999"));
        assertEquals("tok-1", service.login("Joa", null, "1234").token()); // ได้ token เดิม ใช้ได้หลายเครื่อง
    }

    @Test
    void tooManyWrongPinsLocksTheName() {
        existing("Joa", "tok-1", "1234");
        for (int i = 0; i < UserService.MAX_PIN_ATTEMPTS; i++) {
            assertStatus(HttpStatus.UNAUTHORIZED, () -> service.login("Joa", null, "0000"));
        }
        assertStatus(HttpStatus.TOO_MANY_REQUESTS, () -> service.login("Joa", null, "1234"));
        assertEquals("tok-1", service.login("Joa", "tok-1", null).token()); // เครื่องเดิมยังเข้าได้
    }

    @Test
    void pinMustBeFourToSixDigitsAndCanBeRemoved() {
        User u = existing("Joa", "tok-1", null);
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.setPin(u, "12"));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.setPin(u, "abcd"));
        service.setPin(u, "123456");
        assertTrue(UserService.matchesPin("123456", u.getPinHash()));
        assertFalse(UserService.matchesPin("123457", u.getPinHash()));
        service.setPin(u, "");
        assertNull(u.getPinHash());
    }

    @Test
    void rejectsBlankOrLongNames() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.login(" ", null, null));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.login("x".repeat(41), null, null));
    }
}
