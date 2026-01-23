# --- Stage 1: Build ứng dụng (Dùng Maven) ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy file cấu hình maven trước để tận dụng cache
COPY pom.xml .
# Copy toàn bộ source code
COPY src ./src

# Build ra file .jar (Bỏ qua test để build nhanh hơn khi setup ban đầu)
RUN mvn clean package -DskipTests

# --- Stage 2: Chạy ứng dụng (Dùng JRE nhẹ) ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy file .jar từ Stage 1 sang Stage 2
COPY --from=build /app/target/*.jar app.jar

# Mở port 8080
EXPOSE 8386

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]