-- V5: ลบ UNIQUE (poll_id, member_id) ที่ซ้ำกันของ poll_votes
-- ฐานข้อมูลที่เคยสร้างด้วย Hibernate (ddl-auto) มีกฎนี้ 2 ตัว คือ poll_votes_poll_id_member_id_key
-- (จาก V1) กับ uk5aui3ahbiud7lch9cr4blftis (ชื่อที่ Hibernate ตั้งเอง) ทำงานซ้ำกันและเปลืองดัชนี
-- เก็บตัวชื่อชัดเจนไว้ตัวเดียว ถ้าฐานข้อมูลไม่มีตัวซ้ำ (เช่น สร้างใหม่จาก V1) คำสั่งนี้ไม่ทำอะไร
ALTER TABLE poll_votes DROP CONSTRAINT IF EXISTS uk5aui3ahbiud7lch9cr4blftis;
