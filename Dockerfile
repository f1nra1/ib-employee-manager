# Этап 1: Сборка приложения
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Установка JavaFX и зависимостей для GUI
RUN apt-get update && apt-get install -y \
    libgtk-3-0 \
    libglu1-mesa \
    libx11-6 \
    libxxf86vm1 \
    libgl1-mesa-glx \
    libgl1-mesa-dri \
    libxtst6 \
    libxi6 \
    && rm -rf /var/lib/apt/lists/*

# Копируем собранный JAR
COPY --from=builder /app/target/*-shaded.jar app.jar

# Переменные окружения для подключения к БД
ENV DB_HOST=postgres
ENV DB_PORT=5432
ENV DB_NAME=ib_employees
ENV DB_USER=ib_admin
ENV DB_PASSWORD=ib_secure_pass_2024

CMD ["java", "-jar", "app.jar"]