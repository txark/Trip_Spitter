# SOLID Analysis

วิเคราะห์การใช้หลัก SOLID ในโปรเจกต์ Party & Trip Expense Splitter ว่าแต่ละหลักการปรากฏที่ไฟล์ไหน บรรทัดไหน และเหตุผลที่ออกแบบแบบนั้น
ตอนท้ายของแต่ละหัวข้อมี "ข้อจำกัด" ที่ยังไม่สมบูรณ์ เขียนไว้ตรงๆ เพื่อให้ตรวจสอบกับโค้ดได้

- **ที่มาของเลขบรรทัด:** นับจากโค้ด ณ commit `f99b086` (branch `artitaya_6430212592_01`) ถ้าแก้โค้ดภายหลัง เลขบรรทัดอาจเลื่อน ให้ค้นด้วยชื่อคลาส/เมธอดที่ระบุ
- **path ย่อ:** ทุกไฟล์อยู่ใต้ `src/main/java/com/cp/party_trip/` เช่น `service/impl/ExpenseServiceImpl.java`

## สรุป

| หลักการ | สถานะ | หลักฐานหลัก |
|---|---|---|
| **S** Single Responsibility | ผ่าน (มีจุดที่ยังรวมหลายหน้าที่ 1 จุด) | แยก Controller / Service / Mapper / Validation / Error / Authorization / Strategy เป็นคลาสต่างหน้าที่ |
| **O** Open/Closed | ผ่าน | วิธีหารบิลเป็น `SplitStrategy` เพิ่มวิธีใหม่ด้วยการเพิ่มคลาส ไม่แก้ `ExpenseServiceImpl` |
| **L** Liskov Substitution | ผ่าน (ขอบเขตจำกัด) | `EqualSplitStrategy` / `CustomSplitStrategy` ใช้แทนกันได้ ไม่มี `UnsupportedOperationException` |
| **I** Interface Segregation | ผ่าน (มี interface ใหญ่ปานกลาง 3 ตัว) | `AuthTokenService` 1 เมธอด, `TripEventService` 2, `TripAccessService` 6 แยกตามผู้ใช้ |
| **D** Dependency Inversion | ผ่าน (Mapper/Factory ยังเป็นคลาสจริง) | Controller และ `AuthGuard` ขึ้นกับ interface, ใช้ Constructor Injection อย่างเดียว (ไม่มี `@Autowired`) |

---

## S — Single Responsibility Principle

> แต่ละ Class มีหน้าที่เดียว ไม่รวม Business + Validation + Persistence ในคลาสเดียว

| หน้าที่ | คลาส | ตำแหน่ง | เหตุผลสั้นๆ |
|---|---|---|---|
| รับ/ส่ง HTTP | `ExpenseController` | `controller/ExpenseController.java:28` | ทำแค่ตรวจสิทธิ์ (บรรทัด 98 `guard.me(tripId)`) เรียก Service แล้วแปลงเป็น DTO ไม่มี Business Logic ไม่เรียก Repository |
| Business Logic ของบิล | `ExpenseServiceImpl` | `service/impl/ExpenseServiceImpl.java:41` | ตรวจกฎธุรกิจ (ยอด สกุลเงิน วันที่ สิทธิ์แก้บิล) และจัดการ transaction (`@Transactional` บรรทัด 67) |
| Persistence | `ExpenseRepo` | `repository/ExpenseRepo.java:14` | เป็น Spring Data JPA interface ดึง/บันทึกข้อมูลเท่านั้น ไม่มีตรรกะธุรกิจ |
| แปลง Entity ⇄ DTO | `ExpenseMapper` | `mapper/ExpenseMapper.java:12` (`toResponse` บรรทัด 20, `toView` บรรทัด 34) | Service/Controller ไม่ต้องรู้รูปแบบ JSON |
| Validation ของ input | `ExpenseRequest` | `dto/request/ExpenseRequest.java:15` | ใช้ Bean Validation (`@NotBlank`, `@DecimalMin` บรรทัด 17 เป็นต้นไป) ตรวจที่ขอบ API ก่อนเข้า Service |
| แปลง error เป็น JSON | `ApiErrorHandler` | `config/ApiErrorHandler.java:14` (handler บรรทัด 18, 25, 34) | จัดรูปแบบ `{"message": ...}` ที่เดียว Controller ไม่มี try/catch |
| ตรวจสิทธิ์ | `AuthGuard` | `config/AuthGuard.java:16` (`me` บรรทัด 36) | ตอบคำถามเดียว: "ผู้ใช้นี้เป็นสมาชิกทริปไหม" |
| ยืนยันตัวตนจาก token | `AuthInterceptor` | `config/AuthInterceptor.java:15` (`preHandle` บรรทัด 23) | อ่านหัว `X-Auth-Token` แล้วผูกผู้ใช้กับ request |
| ค้นหาทริปของข้อมูล (สำหรับตรวจสิทธิ์) | `TripAccessServiceImpl` | `service/impl/TripAccessServiceImpl.java:18` | แยกการเข้าถึงข้อมูลที่ `AuthGuard` ต้องใช้ ออกจาก `AuthGuard` เอง |
| คำนวณการหาร | `EqualSplitStrategy`, `CustomSplitStrategy` | `service/split/EqualSplitStrategy.java:16`, `service/split/CustomSplitStrategy.java:19` | คลาสละ 1 วิธีหาร ไม่ปนกัน |
| บันทึกประวัติทริป | `TripEventListener` | `event/TripEventListener.java:11` | แยกออกจากการบันทึกบิล (Service แค่ประกาศ event บรรทัด 91) |

**ข้อจำกัด**
- `ExpenseServiceImpl` ยาว 342 บรรทัด และเมธอด `apply` (เริ่มบรรทัด 175) ยังรวมหลายอย่างไว้ด้วยกัน: ตรวจชื่อ/ยอดบิล, แปลงสกุลเงิน, ตรวจวันที่, ตรวจกิจกรรมที่ผูก และสร้างส่วนแบ่ง ถ้าแยกต่อ ควรดึงส่วนตรวจกฎออกเป็นคลาสเช่น `ExpenseValidator` (ยังไม่ได้ทำ)

---

## O — Open/Closed Principle

> เพิ่มฟีเจอร์ใหม่ด้วยการเพิ่มคลาส ไม่ใช่แก้ if-else เดิม

**ตัวอย่างหลัก: วิธีหารบิล (Strategy)**

| ส่วน | ตำแหน่ง | บทบาท |
|---|---|---|
| สัญญา | `service/split/SplitStrategy.java:7` (เมธอด `split` บรรทัด 12) | interface ที่ทุกวิธีหารต้องทำตาม |
| วิธีหารแต่ละแบบ | `EqualSplitStrategy.java:14` (`@Component`), `CustomSplitStrategy.java:17` (`@Component`) | แต่ละวิธีเป็นคลาสแยก ถูก Spring ค้นเจอเอง |
| ตัวเลือก | `service/split/SplitStrategyFactory.java:17-18` | รับ `List<SplitStrategy>` จาก Spring แล้วเก็บเป็น Map ตามรหัส ไม่มี if-else/switch ตามชนิด (เมธอด `forType` บรรทัด 22) |
| ผู้ใช้ | `service/impl/ExpenseServiceImpl.java:190` และ `:244` | เรียก `splitStrategyFactory.forType(...)` แล้วเรียก `split(...)` ไม่รู้ว่าเป็นวิธีไหน |

**เหตุผล:** ก่อนปรับปรุง `ExpenseServiceImpl` แยก `EQUAL`/`CUSTOM` ด้วย if-else ถ้าเพิ่ม "หารตามเปอร์เซ็นต์" ต้องแก้คลาสที่ทดสอบแล้ว
ตอนนี้เพิ่มคลาส `PercentSplitStrategy implements SplitStrategy` + `@Component` ที่เดียวก็ใช้ได้ทันที (ข้อความ error 400 แสดงรายชื่อวิธีที่รองรับให้เองจาก Map)

**ตัวอย่างรอง: Observer ผ่าน Event**
`event/TripEventListener.java:18` (`@EventListener`) ฟัง `ExpenseAddedEvent` และ `RepaymentRecordedEvent`
เพิ่มผู้ฟังใหม่ (เช่น ส่งแจ้งเตือน) = เพิ่ม `@EventListener` ใหม่ โดยไม่แก้ `ExpenseServiceImpl` ที่ประกาศ event (`:91`) หรือ `RepaymentServiceImpl` (`:117`)

**ข้อจำกัด:** ฟิลด์/เงื่อนไขอื่นใน `apply` (เช่น กติกาสกุลเงิน) ยังแก้ในคลาสเดิม ไม่ได้ทำเป็น strategy

---

## L — Liskov Substitution Principle

> Subclass ใช้แทน Superclass ได้โดยไม่พังตรรกะ ไม่ throw `UnsupportedOperationException`

**หลักฐาน**
1. **ทุกวิธีหารทำตามสัญญาเดียวกัน** ผลลัพธ์ของ `split(...)` ต้องรวมได้เท่ากับยอดบิลพอดี ไม่เช่นนั้นตอบ 400
   - `EqualSplitStrategy.java:35-39` เศษจากการปัดทศนิยมไปที่คนแรก ผลรวมจึงเท่ายอดบิลเสมอ
   - `CustomSplitStrategy.java:47` ผลรวมไม่เท่ายอดบิลแล้วตอบ 400 ด้วยชนิด error เดียวกัน (`ResponseStatusException`)
2. **ผู้เรียกไม่ต้องรู้ว่าเป็นตัวไหน** `ExpenseServiceImpl.java:244-247` วนผลลัพธ์ `SplitShare` แบบเดียวกันไม่ว่าจะเป็นวิธีไหน และใช้ `SplitContext` / `SplitShare` (`service/split/SplitContext.java:9`, `SplitShare.java:6`) ที่เป็น record ชนิดเดียวกัน
3. **ไม่มี `UnsupportedOperationException`** ค้นทั้ง `src/main` ได้ 0 รายการ ไม่มีคลาสที่ implement interface แล้วทิ้งเมธอดว่าง
4. **ทดสอบการแทนที่ได้จริง** `ExpenseServiceImplTest` (บรรทัด 45) สร้าง `SplitStrategyFactory` จากรายการ strategy และ mock Repository แทนของจริง Service ทำงานถูกต้องโดยไม่แก้โค้ด

**ข้อจำกัด:** โปรเจกต์เลือกใช้ composition และ interface มากกว่าการสืบทอดคลาส (inheritance) จึงมีลำดับชั้นให้ตรวจ LSP น้อย หลักฐานหลักอยู่ที่ implementation ของ interface ไม่ใช่ subclass

---

## I — Interface Segregation Principle

> แยก Interface ย่อยตามการใช้งาน ไม่มี Fat Interface

| Interface | จำนวนเมธอด | ตำแหน่ง | ใช้โดย |
|---|---|---|---|
| `AuthTokenService` | 1 (`findByToken`) | `service/AuthTokenService.java:8` | `AuthInterceptor` (`config/AuthInterceptor.java:16`) |
| `TripEventService` | 2 | `service/TripEventService.java:7` | `TripEventListener`, `TripEventController` |
| `TripAccessService` | 6 | `service/TripAccessService.java:7` | `AuthGuard` (`config/AuthGuard.java:21`) |
| `DebtService` | 2 | `service/DebtService.java` | `DebtController` |
| `UserTripHistoryService` | 2 | `service/UserTripHistoryService.java` | `UserTripHistoryController` |
| `RepaymentService` | 3 | `service/RepaymentService.java` | `ExpenseController` |

**ตัวอย่างที่ชัดที่สุด:** `AuthInterceptor` ต้องการแค่ "หาผู้ใช้จาก token" จึงขึ้นกับ `AuthTokenService` (1 เมธอด) ไม่ใช่ `UserService` (6 เมธอด: login, PIN, rename ฯลฯ)
ทั้งสองเป็นคลาสเดียวกันจริง `UserServiceImpl implements UserService, AuthTokenService` (`service/impl/UserServiceImpl.java:32`) แต่ผู้ใช้แต่ละฝั่งเห็นเฉพาะส่วนที่ตัวเองต้องใช้

อีกตัวอย่าง: ตอนแรก `AuthGuard` เรียก Repository 6 ตัวโดยตรง จึงแยกเป็น `TripAccessService` ที่มีเฉพาะเมธอดที่ `AuthGuard` ต้องใช้ ไม่ต้องเห็นเมธอดธุรกิจของ Service อื่น

**ข้อจำกัด:** มี interface ที่ใหญ่ปานกลางที่รวมตามทรัพยากร ไม่ได้แยกตามผู้ใช้ต่อ: `TripService` 9 เมธอด (`service/TripService.java:10`), `ExpenseService` 8 (`service/ExpenseService.java:13`), `ChecklistService` 8, `PollService` 7 ที่ยังใหญ่เพราะ Controller ของแต่ละทรัพยากรใช้เกือบครบทุกเมธอด ถ้าจะแยกต่อ ควรแยกอ่าน/เขียน (query/command)

---

## D — Dependency Inversion Principle

> Service ขึ้นกับ Interface ไม่ใช่ Concrete Class + ใช้ Constructor Injection เท่านั้น

**1) ขึ้นกับ interface**

| ผู้ใช้ | ขึ้นกับ | ตำแหน่ง |
|---|---|---|
| `ExpenseController` | `ExpenseService`, `RepaymentService` (interface) | `controller/ExpenseController.java:30-31` |
| `AuthGuard` | `TripAccessService` (interface) | `config/AuthGuard.java:21` |
| `AuthInterceptor` | `AuthTokenService` (interface) | `config/AuthInterceptor.java:16` |
| `ExpenseServiceImpl` | `ExpenseRepo`, `TripRepo`, `TripMemberRepo`, `ActivityRepo`, `ExpenseSplitRepo` (interface ของ Spring Data) | `service/impl/ExpenseServiceImpl.java:43-47` |

Controller ไม่มีการอ้างถึงคลาส `*ServiceImpl` เลย (ค้น `new ...ServiceImpl(` ใน `src/main` ได้ 0 รายการ) คลาสจริงถูก Spring เลือกให้

**2) Constructor Injection เท่านั้น**
- ไม่มี `@Autowired` ในโค้ดหลักเลย (ค้นแล้วได้ 0 รายการ) ทุกคลาสรับ dependency ผ่าน constructor และเก็บเป็น `private final`
- ตัวอย่าง: `ExpenseController.java:36`, `ExpenseServiceImpl.java:52`, `AuthGuard.java:23`
- ผลที่ได้: ทดสอบง่าย `ExpenseServiceImplTest` ส่ง mock เข้า constructor ได้ตรงๆ โดยไม่ต้องเปิด Spring

**ข้อจำกัด**
- **Mapper เป็นคลาสจริง ไม่มี interface:** `ExpenseController.java:32-33` และ `ExpenseServiceImpl.java:48` รับ `ExpenseMapper` / `DebtMapper` เป็นคลาสตรงๆ ใช้ได้เพราะเป็นคลาส stateless ที่ไม่มีหลายแบบให้สลับ แต่ไม่เข้มงวดตาม DIP
- **`SplitStrategyFactory` เป็นคลาสจริง:** `ExpenseServiceImpl.java:49` (ตัว strategy จริงขึ้นกับ interface `SplitStrategy` แล้ว แต่ตัวเลือกเป็นคลาสตรงๆ)
- ถ้าจะให้เข้มงวดขึ้น ควรสร้าง interface `ExpenseMapping` และ `SplitStrategyResolver` (ยังไม่ได้ทำ)

---

## สรุปการปรับปรุงที่ทำเพื่อให้เป็นไปตาม SOLID

| ปัญหาเดิม | หลักการ | วิธีแก้ | ไฟล์ |
|---|---|---|---|
| `AuthGuard` (ชั้น config) เรียก Repository 6 ตัวโดยตรง | D, I, S | แยก `TripAccessService` | `service/TripAccessService.java`, `service/impl/TripAccessServiceImpl.java` |
| วิธีหารบิลแยกด้วย if-else ใน `ExpenseServiceImpl` | O | Strategy + Factory | `service/split/*` |
| บันทึกประวัติต้องยัดโค้ดในบริการบิล | O, S | Observer ผ่าน event | `event/*`, `service/impl/TripEventServiceImpl.java` |
| Controller คืน Entity/Map และมี handler error ของตัวเอง | S | DTO + Mapper + `ApiErrorHandler` | `dto/*`, `mapper/*`, `config/ApiErrorHandler.java` |
