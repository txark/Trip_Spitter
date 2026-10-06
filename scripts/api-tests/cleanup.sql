-- ลบข้อมูลที่ชุดทดสอบสร้าง: ทริปชื่อ [api-test] และผู้ใช้ที่ชื่อขึ้นต้นด้วย zt_ (ดู common.py)
-- ข้อมูลจริงไม่โดน เพราะลบเฉพาะสองเงื่อนไขนี้
begin;
create temp table tt as select id from trips where title = '[api-test]';
create temp table uu as select id from users where username like 'zt\_%';
delete from repayment_items where repayment_id in (select id from repayments where trip_id in (select id from tt));
delete from repayments where trip_id in (select id from tt);
delete from expense_splits where expense_id in (select id from expenses where trip_id in (select id from tt));
delete from expenses where trip_id in (select id from tt);
delete from poll_votes where poll_id in (select id from polls where trip_id in (select id from tt));
delete from poll_options where poll_id in (select id from polls where trip_id in (select id from tt));
delete from polls where trip_id in (select id from tt);
delete from checklist_item_assignees where item_id in (select id from checklist_items where trip_id in (select id from tt));
delete from checklist_items where trip_id in (select id from tt);
delete from activity_participants where activity_id in (select id from activities where trip_id in (select id from tt));
delete from activity_stops where activity_id in (select id from activities where trip_id in (select id from tt));
delete from activities where trip_id in (select id from tt);
delete from settlements where trip_id in (select id from tt);
delete from trip_members where trip_id in (select id from tt);
delete from user_trip_history where trip_id in (select id from tt) or user_id in (select id from uu);
delete from trips where id in (select id from tt);
delete from users where id in (select id from uu);
commit;
select (select count(*) from trips) trips, (select count(*) from users) users;
