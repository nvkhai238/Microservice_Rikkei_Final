# =============================================================================
# Một Dockerfile cho MỌI service (NFR-MNT-007: cùng một kiểu image, khác nhau ở cấu hình).
# Build jar trước:   ./mvnw clean package -DskipTests
# Build image:       docker build --build-arg MODULE=order-service -t ecommerce/order-service .
# Hoặc cả hệ thống:  docker compose --profile apps up -d --build
# =============================================================================
FROM eclipse-temurin:17-jre-alpine

ARG MODULE
# Chạy bằng user thường, không dùng root (NFR-SEC-004: least privilege)
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/data/media && chown -R app:app /app && chmod -R a+rwX /app/data
WORKDIR /app
COPY --chown=app:app ${MODULE}/target/${MODULE}-0.0.1-SNAPSHOT.jar app.jar
USER app

# Container giới hạn RAM => để JVM tự chia heap theo giới hạn đó
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Duser.timezone=UTC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
