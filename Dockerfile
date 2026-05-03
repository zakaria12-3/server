# Use official Java 21 runtime
FROM eclipse-temurin:21-jdk

# Set working directory inside container
WORKDIR /app

# Copy project files
COPY . .

# Build with Gradle (skip tests for faster deploy)
RUN ./gradlew build -x test

# Run the JAR (adjust name if different)
CMD ["java", "-jar", "build/libs/your-app.jar"]
