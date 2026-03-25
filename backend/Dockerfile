FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && addgroup --system spring \
    && adduser --system --ingroup spring spring
ENV JAVA_OPTS="-XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"
COPY --from=build /app/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring
EXPOSE 8080
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
