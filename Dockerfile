# 1. Build the App
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the entire repo into the container
COPY . .

# Run Maven pointing explicitly to the backend folder
RUN mvn -f backend/pom.xml clean package -DskipTests

# 2. Run the App
FROM eclipse-temurin:17-jdk-jammy

# Find the built JAR file inside the backend/target folder
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
