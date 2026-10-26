# Backend (Spring Boot, Java 21). Bir xil fayl ARM64 (Oracle Ampere) va AMD64 da ishlaydi:
# eclipse-temurin rasmlari ikkala arxitekturaga ham chiqariladi.
# Yig'ish: deploy/docker-compose.yml shuni ishlatadi (docker compose build backend).

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
# avval faqat Gradle fayllari - kutubxonalar keshi kod o'zgarganda qayta yuklanmaydi
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon -q dependencies > /dev/null
COPY src/main src/main
RUN ./gradlew --no-daemon -q bootJar \
    && cp "$(ls build/libs/*.jar | grep -v -- '-plain.jar')" /app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --home /app app \
    && mkdir -p /app /data/storage \
    && chown -R app /app /data/storage
WORKDIR /app
COPY --from=build /app.jar /app/app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=server \
    TZ=Asia/Tashkent \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError -Duser.timezone=Asia/Tashkent"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
