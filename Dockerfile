# ---------- Stage 1: build jar ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# ดึง dependency ก่อน (layer นี้ถูก cache ตราบใดที่ pom.xml ไม่เปลี่ยน build รอบต่อไปเร็วขึ้นมาก)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# ข้ามเทสต์ใน image: เทสต์ต้องใช้ฐานข้อมูล ให้รันใน CI (GitHub Actions) แทน
RUN ./mvnw -B -q -DskipTests package

# ---------- Stage 2: run ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# ไม่รันด้วย root
RUN useradd --system --no-create-home appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

# จำกัดหน่วยความจำให้เหมาะกับ container เล็ก (เช่น Render ฟรี 512 MB)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
# พอร์ตจริงมาจากตัวแปร PORT (Cloud ส่งมาให้) ไม่ตั้ง = 8090
EXPOSE 8090
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
