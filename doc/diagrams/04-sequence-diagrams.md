# Sequence Diagram

5 scenario หลัก ทุกลำดับเขียนตามโค้ดจริง (ชื่อคลาส/เมธอดตรงกับ `src/main/java/com/cp/party_trip/`)

## 1) เข้าสู่ระบบด้วยชื่อเล่น (รวมกรณีเครื่องอื่น)

```mermaid
sequenceDiagram
    actor U as ผู้ใช้
    participant UI as หน้าเว็บ
    participant C as UserController
    participant S as UserServiceImpl
    participant R as UserRepo
    U->>UI: กรอกชื่อเล่น (และ PIN ถ้ามี)
    UI->>C: POST /api/v1/users/login
    C->>S: login(ชื่อ, token เดิม, pin)
    S->>R: findByUsername(ชื่อ)
    alt ยังไม่มีผู้ใช้ชื่อนี้
        S->>R: save(ผู้ใช้ใหม่ + token ใหม่)
        S-->>C: Login(ผู้ใช้, token)
    else token ตรงกับของเดิม (เครื่องเดิม)
        S-->>C: Login(ผู้ใช้, token เดิม)
    else เครื่องอื่น
        alt ไม่ได้ส่ง PIN
            S-->>C: 401 PIN_REQUIRED
        else PIN ถูกต้อง หรือรหัสกู้คืนถูกต้อง
            S-->>C: Login(ผู้ใช้, token)
        else PIN ผิด
            S-->>C: 401 PIN ไม่ถูกต้อง
        end
    end
    C-->>UI: LoginResponse (หรือข้อความ error)
    UI->>UI: เก็บ token ไว้ ส่งใน X-Auth-Token ทุกครั้ง
```

## 2) เข้าร่วมทริปด้วยรหัสเชิญ

```mermaid
sequenceDiagram
    actor U as สมาชิกใหม่
    participant I as AuthInterceptor
    participant C as TripController
    participant S as TripServiceImpl
    participant TR as TripRepo
    participant MR as TripMemberRepo
    U->>I: POST /api/v1/trips/join/{รหัส} + X-Auth-Token
    I->>I: หาผู้ใช้จาก token (ไม่เจอ = 401)
    I->>C: ส่งต่อพร้อมผู้ใช้
    C->>S: joinTrip(รหัส, ชื่อเล่นของเจ้าของ token)
    S->>TR: findByInviteCodeIgnoreCase(รหัส)
    alt ไม่พบทริป
        S-->>C: 404
    else พบทริป
        S->>MR: หาสมาชิกชื่อนี้ในทริป
        alt ยังไม่เป็นสมาชิก
            S->>MR: save(สมาชิกใหม่ role MEMBER)
        end
        S->>S: บันทึกทริปลงประวัติผู้ใช้
        S-->>C: TripMember
    end
    C-->>U: ข้อมูลสมาชิก (หรือ error)
```

## 3) เพิ่มบิลและแบ่งส่วนแบ่ง (Strategy + Observer)

```mermaid
sequenceDiagram
    actor U as สมาชิก
    participant G as AuthGuard
    participant C as ExpenseController
    participant S as ExpenseServiceImpl
    participant F as SplitStrategyFactory
    participant ST as SplitStrategy
    participant R as ExpenseRepo
    participant L as TripEventListener
    U->>C: POST /api/v1/expenses/add/{tripId} (JSON)
    C->>C: Bean Validation (@Valid)
    C->>G: self(tripId, recordedBy) ตรวจว่าเป็นสมาชิกและไม่ทำแทนคนอื่น
    C->>S: createExpense(...)
    S->>F: forType(splitType)
    F-->>S: Equal หรือ Custom strategy
    S->>ST: split(SplitContext)
    ST-->>S: รายการ SplitShare รวมเท่ายอดบิล
    S->>R: save(expense + splits)
    S-)L: publishEvent(ExpenseAddedEvent)
    L->>L: บันทึก trip_events
    S-->>C: Expense
    C-->>U: 201 ExpenseResponse
```

## 4) บันทึกการรับเงินคืน

```mermaid
sequenceDiagram
    actor U as ผู้รับเงิน
    participant C as ExpenseController
    participant S as RepaymentServiceImpl
    participant MR as TripMemberRepo
    participant ER as ExpenseRepo
    participant SR as ExpenseSplitRepo
    participant RR as RepaymentRepo
    U->>C: POST /api/v1/expenses/repay/{tripId}
    C->>S: receive(tripId, ผู้รับ, ผู้โอน, คำขอ)
    S->>MR: lockById(ผู้โอน) กันบันทึกซ้อนกัน
    S->>ER: findByTripId แล้วคัดบิลที่ผู้รับจ่าย
    S->>SR: หาส่วนแบ่งที่ผู้โอนยังค้าง (บิลเก่าสุดก่อน)
    alt ไม่มียอดค้าง หรือยอดเกินยอดค้าง
        S-->>C: 400
    else ยอดถูกต้อง
        S->>S: หักยอดทีละบิล (ครบ = จ่ายแล้ว ไม่ครบ = เก็บยอดที่จ่ายมา)
        S->>SR: saveAll(ส่วนแบ่งที่เปลี่ยน)
        S->>RR: save(repayment + รายการย่อย)
        S-)S: publishEvent(RepaymentRecordedEvent)
        S-->>C: Repayment
    end
    C-->>U: 201 RepaymentResponse (หรือ error)
```

## 5) ลงคะแนนโหวต

```mermaid
sequenceDiagram
    actor U as สมาชิก
    participant C as PollController
    participant G as AuthGuard
    participant S as PollServiceImpl
    participant VR as PollVoteRepo
    U->>C: POST /api/v1/polls/{pollId}/vote?optionId=...
    C->>G: self(ทริปของโหวต, memberId)
    C->>S: castVote(pollId, optionId, memberId)
    S->>S: ตรวจโหวตยังเปิด (ไม่ปิด และไม่เลยเวลา) และเป็นสมาชิกทริป
    S->>VR: findByPollIdAndMemberId
    alt เคยโหวตตัวเลือกเดิม
        S->>VR: delete (ยกเลิกคะแนน)
    else เคยโหวตตัวเลือกอื่น
        S->>VR: save (เปลี่ยนตัวเลือก)
    else ยังไม่เคยโหวต
        S->>VR: save (คะแนนใหม่)
    end
    S-->>C: ผลการโหวต
    C-->>U: ข้อความผลลัพธ์ (ถ้าชนกันพร้อมกัน ตอบ 409)
```
