# ============================================================
# Stage 1: Build — Maven + JDK 17
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 先复制依赖配置，利用 Docker 层缓存加速
COPY pom.xml ./
COPY lombok.config ./

# 下载依赖（不编译，层缓存）
RUN mvn dependency:go-offline -B || true

# 复制源码
COPY src/ src/

# 编译打包，跳过测试
RUN mvn package -DskipTests -B

# ============================================================
# Stage 2: Runtime — JRE 17
# ============================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# 从 build 阶段复制构建产物
COPY --from=build /app/target/*.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -sf http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
