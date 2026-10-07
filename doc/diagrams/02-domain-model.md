# Domain Model (Conceptual Class Diagram)

แสดงแนวคิดของระบบและความสัมพันธ์ระหว่างกัน โดยไม่ลงรายละเอียดการเขียนโค้ด (ฐานข้อมูลจริงอยู่ที่ [data-dictionary.md](../data-dictionary.md))

```mermaid
classDiagram
    direction LR
    class User {
        ชื่อเล่น
        token ประจำเครื่อง
        PIN
    }
    class Trip {
        ชื่อทริป
        วันเริ่ม และวันสิ้นสุด
        รหัสเชิญ
    }
    class TripSettings {
        งบต่อคน
        สกุลเงินและเรท
        เขตเวลา
    }
    class Member {
        ชื่อในทริป
        บทบาท ADMIN หรือ MEMBER
    }
    class Activity {
        ประเภท: เดินทาง กิน พัก เที่ยว
        วันและเวลา
        ค่าใช้จ่ายโดยประมาณ
    }
    class Expense {
        ยอดบิล
        สกุลเงิน
        วิธีหาร
    }
    class Split {
        ยอดที่ต้องจ่าย
        จ่ายแล้วเท่าไร
    }
    class Repayment {
        ยอดที่รับคืน
        ผู้โอน และผู้รับ
    }
    class Poll {
        คำถาม
        สถานะเปิดหรือปิด
    }
    class PollOption {
        ข้อความตัวเลือก
    }
    class Vote {
        ผู้โหวตและตัวเลือก
    }
    class ChecklistItem {
        ชื่อสิ่งของ
        จำนวน และหน่วย
        เตรียมแล้วหรือยัง
    }
    class TripEvent {
        ข้อความความเคลื่อนไหว
    }

    User "1" -- "0..*" Member : เป็นสมาชิกในหลายทริป
    Trip "1" -- "1" TripSettings : มีการตั้งค่า
    Trip "1" *-- "1..*" Member : มีสมาชิก
    Trip "1" *-- "0..*" Activity : มีแพลน
    Trip "1" *-- "0..*" Expense : มีบิล
    Trip "1" *-- "0..*" Poll : มีโหวต
    Trip "1" *-- "0..*" ChecklistItem : มีสิ่งของ
    Trip "1" *-- "0..*" Repayment : มีการรับเงินคืน
    Trip "1" *-- "0..*" TripEvent : มีความเคลื่อนไหว
    Expense "1" *-- "1..*" Split : แบ่งเป็น
    Member "1" -- "0..*" Expense : จ่ายบิล
    Member "1" -- "0..*" Split : ต้องจ่าย
    Member "0..*" -- "0..*" Activity : ไปร่วม
    Member "0..*" -- "0..*" ChecklistItem : รับผิดชอบ
    Repayment "1" -- "1..*" Split : หักยอดจาก
    Poll "1" *-- "2..*" PollOption : มีตัวเลือก
    PollOption "1" -- "0..*" Vote : ได้คะแนน
    Member "1" -- "0..*" Vote : โหวต
    Activity "0..1" -- "0..*" Expense : ผูกกับบิล
    Poll "0..1" -- "0..*" Activity : เพิ่มเป็นกิจกรรม
```

**กติกาสำคัญของโดเมน**
- ผลรวมส่วนแบ่ง (`Split`) ของบิลหนึ่งต้องเท่ายอดบิลพอดี
- หนี้ของสมาชิก = ส่วนแบ่งที่ต้องจ่าย − ยอดที่จ่ายคืนแล้ว ระบบรวบยอดเป็นรายการโอนน้อยที่สุด
- สมาชิกหนึ่งคนโหวตได้ 1 ตัวเลือกต่อ 1 โหวต
- คนเดียวกันอยู่ในทริปเดียวกันได้แถวเดียว (ชื่อซ้ำในทริปไม่ได้)
