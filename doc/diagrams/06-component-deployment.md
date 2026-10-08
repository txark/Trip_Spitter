# Component Diagram และ Deployment Diagram

## Component Diagram

```mermaid
flowchart LR
    subgraph Client["ฝั่งผู้ใช้"]
        WEB["หน้าเว็บ HTML/CSS/JS<br/>home, dashboard, plan, expenses,<br/>debts, polls, checklist"]
    end

    subgraph App["Spring Boot Application"]
        direction TB
        SEC["Security<br/>AuthInterceptor + AuthGuard + CORS"]
        API["REST Controllers<br/>/api/v1/*"]
        DOC["Swagger UI<br/>springdoc-openapi"]
        SVC["Services<br/>Trip, Expense, Debt, Repayment, Activity,<br/>Checklist, Poll, User"]
        SPL["Split Strategies<br/>Equal, Custom"]
        EVT["Event Listener<br/>TripEventListener"]
        MAP["Mappers + DTO"]
        REPO["Repositories<br/>Spring Data JPA"]
        MIG["Flyway<br/>db/migration V1-V5"]
        SEC --> API
        API --> MAP
        API --> SVC
        SVC --> SPL
        SVC --> EVT
        SVC --> REPO
        EVT --> REPO
        MIG -. สร้างและปรับ schema ตอนเริ่มแอป .-> DB
    end

    DB[("PostgreSQL")]
    WEB -- "HTTPS JSON + X-Auth-Token" --> SEC
    WEB -. "เปิดดูเอกสาร API" .-> DOC
    REPO -- JDBC --> DB
```

## Deployment Diagram

```mermaid
flowchart TB
    DEV["เครื่องนักพัฒนา<br/>git push"]
    subgraph GH["GitHub"]
        REPO["Repository<br/>branch main / develop / ส่วนตัว"]
        CI["GitHub Actions<br/>build, unit test, docker build"]
    end
    subgraph RENDER["Render (แผนฟรี, region ค่าเริ่มต้น Oregon)"]
        CONT["Docker container<br/>Spring Boot 4.1.1 บน Java 25<br/>พอร์ตจากตัวแปร PORT"]
    end
    subgraph NEON["Neon (Singapore)"]
        PG[("PostgreSQL")]
    end
    USER["ผู้ใช้ (เบราว์เซอร์)"]

    DEV --> REPO
    REPO --> CI
    REPO -- "main เปลี่ยน: build จาก Dockerfile และ deploy อัตโนมัติ" --> CONT
    USER -- "HTTPS trip-spitter.onrender.com" --> CONT
    CONT -- "JDBC (SSL) DB_URL / DB_USERNAME / DB_PASSWORD" --> PG
```

**หมายเหตุ**
- ตัวแปรลับของฐานข้อมูลตั้งในหน้า Environment ของ Render ไม่เก็บใน Git
- แผนฟรีของ Render ให้แอป "หลับ" เมื่อไม่มีคนใช้นาน ~15 นาที และจะตื่นเมื่อมี request (ช้าครั้งแรก 30-60 วินาที)
- แอปอยู่ region ค่าเริ่มต้นของ Render (Oregon) ส่วนฐานข้อมูลอยู่ Singapore เป็นความต่างของ region ที่รู้และยอมรับไว้ (Render เปลี่ยน region ของ service เดิมไม่ได้)
- รันบนเครื่องตัวเองได้ด้วย `docker compose up --build` (app + PostgreSQL ใน container) ดู README
