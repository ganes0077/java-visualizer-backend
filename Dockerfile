# 1. Build the App using Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the entire repository
COPY . .

# Run Maven pointing explicitly to the backend folder
RUN mvn -f backend/pom.xml clean package -DskipTests

# 2. Run the App using Java 21
FROM eclipse-temurin:21-jdk-jammy

# Find the built JAR file
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
