# ===================================================================
# 多阶段构建 Dockerfile - RAG 网关服务
# 阶段一：用 Maven 镜像编译打包，阶段二：仅拷贝产物到精简 JRE 运行，
# 这样最终镜像不含 Maven 与源码，体积更小、攻击面更少。
# ===================================================================

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# 先拷贝 pom.xml 单独下载依赖，利用 Docker 层缓存加速后续构建
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
# 再拷贝源码并打包（跳过测试，测试在 CI 阶段单独执行）
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# 从构建阶段拷贝可执行 jar
COPY --from=build /build/target/*.jar app.jar
# 网关对外端口
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
