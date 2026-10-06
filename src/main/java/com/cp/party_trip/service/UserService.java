package com.cp.party_trip.service;

import com.cp.party_trip.model.User;
import java.time.Duration;

// บัญชีผู้ใช้: เข้าสู่ระบบด้วยชื่อเล่น + PIN, เปลี่ยนชื่อ, รหัสกู้คืน
// ตัวจริงอยู่ที่ service/impl/UserServiceImpl (Controller ขึ้นกับ interface นี้ ไม่ใช่คลาสจริง)
public interface UserService {
    // ผลการเข้าสู่ระบบ: บัญชี + token ที่หน้าเว็บต้องเก็บไว้
    record Login(User user, String token) {
    }

    // รหัสกู้คืนใช้ได้นานเท่านี้
    Duration RECOVERY_TTL = Duration.ofMinutes(30);

    Login login(String username, String token, String pin);

    // เปลี่ยนชื่อเล่นของบัญชีนี้ (token/PIN เดิม) + ชื่อในทุกทริปที่อยู่ บิล/หนี้/ประวัติเดิมยังเป็นของเรา
    // ชื่อใหม่เป็นของบัญชีอื่นอยู่แล้ว = 409 NAME_TAKEN (หน้าเว็บจะถามว่าจะเข้าชื่อนั้นด้วย PIN แทนไหม)
    User rename(User me, String newName);

    // คนสร้างทริปออกรหัสกู้คืน 6 หลักให้เพื่อน (ใช้แทน PIN ได้ 1 ครั้ง ภายใน 30 นาที)
    String issueRecoveryCode(String username);

    // ตั้ง/เปลี่ยน PIN ของตัวเอง (null/ว่าง = ลบ PIN)
    User setPin(User user, String pin);

    // เดิม: เข้าด้วยชื่ออย่างเดียว (ยังใช้ในส่วนที่ไม่ต้องยืนยันตัวตน)
    User saveOrUpdateUser(String username);
}
