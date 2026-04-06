FROM eclipse-temurin:25.0.2_10-jre-alpine

RUN addgroup -g 1000 stablepay && \
    adduser -u 1000 -G stablepay -s /bin/sh -D stablepay

WORKDIR /app

COPY build/libs/*.jar app.jar

RUN chown -R stablepay:stablepay /app

USER stablepay

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]
