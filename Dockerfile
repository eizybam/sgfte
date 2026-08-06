# --- WAR COMPILE ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY core/pom.xml core/pom.xml
COPY core/.mvn core/.mvn
COPY core/mvnw core/mvnw
RUN chmod +x core/mvnw
WORKDIR /build/core
RUN ./mvnw -B dependency:go-offline
WORKDIR /build
COPY core/src core/src
COPY docker/db.properties core/src/main/resources/db.properties
WORKDIR /build/core
RUN ./mvnw -B package -DskipTests

# --- TOMCAT ---
FROM tomcat:10.1-jdk21-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /build/core/target/core-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080


