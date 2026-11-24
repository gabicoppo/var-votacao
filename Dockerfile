# VAR: imagem da aplicação web (Spring Boot). Usada pelo Render (render.yaml) e pelo job docker do CI.

# ---- build: compila e empacota o jar executável ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Primeiro só o pom: a camada de dependências fica em cache enquanto o pom não muda.
COPY pom.xml .
RUN mvn -B -ntp -q dependency:go-offline || true
COPY src ./src
COPY config ./config
RUN mvn -B -ntp -DskipTests package && cp target/var-*.jar /build/app.jar

# ---- runtime: só o JRE e o jar, com usuário sem privilégios ----
FROM eclipse-temurin:21-jre
RUN groupadd --system var && useradd --system --gid var --home-dir /app var
WORKDIR /app
COPY --from=build --chown=var:var /build/app.jar app.jar
USER var
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
