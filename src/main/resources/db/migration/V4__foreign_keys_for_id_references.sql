-- V4: เพิ่ม Foreign Key ให้คอลัมน์ที่เดิมอ้างอิงกันด้วยเลข id เฉยๆ (polls, repayments, ประวัติ ฯลฯ)
-- กติกาเลือก ON DELETE:
--   CASCADE   = ข้อมูลลูกไม่มีความหมายถ้าไม่มีแม่ (ตัวเลือก/คะแนนโหวต, ประวัติการเปิดทริป)
--   SET NULL  = เป็นแค่ "ใครทำ/เกี่ยวกับอะไร" ลบต้นทางแล้วข้อมูลหลักยังอยู่ (ผู้สร้างโหวต, บิลที่ผูกกับกิจกรรม)
--   (ค่าเริ่มต้น = ห้ามลบ) = ข้อมูลเงิน (การรับเงินคืน) ห้ามหายเงียบๆ ต้องจัดการให้ชัดก่อนลบ

-- ---------- 1) เก็บกวาดแถวที่ชี้ไปหาของที่ไม่มีแล้ว (ไม่งั้น FK จะสร้างไม่ได้) ----------
-- ตาราง "ลูก" ที่ไม่มีความหมายถ้าไม่มีแม่: ลบทิ้ง (ลบจากลูกไปหาแม่)
DELETE FROM poll_votes WHERE poll_id IS NOT NULL AND poll_id NOT IN (SELECT id FROM polls);
DELETE FROM poll_votes WHERE member_id IS NOT NULL AND member_id NOT IN (SELECT id FROM trip_members);
DELETE FROM poll_votes WHERE option_id IS NOT NULL AND option_id NOT IN (SELECT id FROM poll_options);
DELETE FROM poll_options WHERE poll_id IS NOT NULL AND poll_id NOT IN (SELECT id FROM polls);
DELETE FROM polls WHERE trip_id IS NOT NULL AND trip_id NOT IN (SELECT id FROM trips);
DELETE FROM user_trip_history WHERE trip_id IS NOT NULL AND trip_id NOT IN (SELECT id FROM trips);
DELETE FROM user_trip_history WHERE user_id IS NOT NULL AND user_id NOT IN (SELECT id FROM users);

-- ข้อมูลเงิน: ลบแถวที่อ้างอิงทริป/สมาชิกที่ไม่มีแล้ว (ไม่มีทริปให้ตามกลับ จึงไม่มีความหมาย)
DELETE FROM repayment_items WHERE repayment_id IN (
    SELECT id FROM repayments
    WHERE (trip_id IS NOT NULL AND trip_id NOT IN (SELECT id FROM trips))
       OR (from_member_id IS NOT NULL AND from_member_id NOT IN (SELECT id FROM trip_members))
       OR (to_member_id IS NOT NULL AND to_member_id NOT IN (SELECT id FROM trip_members)));
DELETE FROM repayments
WHERE (trip_id IS NOT NULL AND trip_id NOT IN (SELECT id FROM trips))
   OR (from_member_id IS NOT NULL AND from_member_id NOT IN (SELECT id FROM trip_members))
   OR (to_member_id IS NOT NULL AND to_member_id NOT IN (SELECT id FROM trip_members));
DELETE FROM repayment_items WHERE expense_id IS NOT NULL AND expense_id NOT IN (SELECT id FROM expenses);

-- คอลัมน์ "ใครทำ/ผูกกับอะไร": เปลี่ยนเป็น NULL แทนการลบแถว
UPDATE polls SET created_by_member_id = NULL
WHERE created_by_member_id IS NOT NULL AND created_by_member_id NOT IN (SELECT id FROM trip_members);
UPDATE activities SET created_by_member_id = NULL
WHERE created_by_member_id IS NOT NULL AND created_by_member_id NOT IN (SELECT id FROM trip_members);
UPDATE activities SET poll_id = NULL
WHERE poll_id IS NOT NULL AND poll_id NOT IN (SELECT id FROM polls);
UPDATE expenses SET activity_id = NULL
WHERE activity_id IS NOT NULL AND activity_id NOT IN (SELECT id FROM activities);
UPDATE expenses SET recorded_by_member_id = NULL
WHERE recorded_by_member_id IS NOT NULL AND recorded_by_member_id NOT IN (SELECT id FROM trip_members);
UPDATE checklist_items SET assigned_to_member_id = NULL
WHERE assigned_to_member_id IS NOT NULL AND assigned_to_member_id NOT IN (SELECT id FROM trip_members);
UPDATE checklist_items SET updated_by_member_id = NULL
WHERE updated_by_member_id IS NOT NULL AND updated_by_member_id NOT IN (SELECT id FROM trip_members);

-- ---------- 2) polls / poll_options / poll_votes ----------
ALTER TABLE polls
    ADD CONSTRAINT fk_polls_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    ADD CONSTRAINT fk_polls_created_by FOREIGN KEY (created_by_member_id) REFERENCES trip_members (id) ON DELETE SET NULL;
ALTER TABLE poll_options
    ADD CONSTRAINT fk_poll_options_poll FOREIGN KEY (poll_id) REFERENCES polls (id) ON DELETE CASCADE;
ALTER TABLE poll_votes
    ADD CONSTRAINT fk_poll_votes_poll FOREIGN KEY (poll_id) REFERENCES polls (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_poll_votes_option FOREIGN KEY (option_id) REFERENCES poll_options (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_poll_votes_member FOREIGN KEY (member_id) REFERENCES trip_members (id) ON DELETE CASCADE;

-- ---------- 3) repayments / repayment_items (ข้อมูลเงิน: ใช้ค่าเริ่มต้น ห้ามลบต้นทาง) ----------
ALTER TABLE repayments
    ADD CONSTRAINT fk_repayments_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    ADD CONSTRAINT fk_repayments_from FOREIGN KEY (from_member_id) REFERENCES trip_members (id),
    ADD CONSTRAINT fk_repayments_to FOREIGN KEY (to_member_id) REFERENCES trip_members (id);
ALTER TABLE repayment_items
    ADD CONSTRAINT fk_repayment_items_expense FOREIGN KEY (expense_id) REFERENCES expenses (id);

-- ---------- 4) ความสัมพันธ์อื่นที่ยังเป็นแค่เลข ----------
ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_activity FOREIGN KEY (activity_id) REFERENCES activities (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_expenses_recorded_by FOREIGN KEY (recorded_by_member_id) REFERENCES trip_members (id) ON DELETE SET NULL;
ALTER TABLE activities
    ADD CONSTRAINT fk_activities_created_by FOREIGN KEY (created_by_member_id) REFERENCES trip_members (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_activities_poll FOREIGN KEY (poll_id) REFERENCES polls (id) ON DELETE SET NULL;
ALTER TABLE checklist_items
    ADD CONSTRAINT fk_checklist_items_assigned FOREIGN KEY (assigned_to_member_id) REFERENCES trip_members (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_checklist_items_updated_by FOREIGN KEY (updated_by_member_id) REFERENCES trip_members (id) ON DELETE SET NULL;
ALTER TABLE user_trip_history
    ADD CONSTRAINT fk_user_trip_history_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_trip_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
