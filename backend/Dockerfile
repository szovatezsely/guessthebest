# ---- Build stage: compile + assemble a runnable distribution ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy the Gradle wrapper first to leverage layer caching for the distribution download.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
# Normalize line endings (the wrapper may be checked out with CRLF on Windows) and make executable.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Now the sources, then build the application distribution.
COPY src ./src
RUN ./gradlew --no-daemon installDist

# ---- Runtime stage: JRE only ----
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# The application plugin produces build/install/<rootProject.name>/{bin,lib}.
COPY --from=build /app/build/install/guessthebest-backend ./

# Run as a non-root user; keep the SQLite database in a dedicated, writable directory.
RUN useradd --system --uid 1001 appuser \
    && mkdir -p /data \
    && chown -R appuser:appuser /data
USER appuser

# The DB path in the app is relative (jdbc:sqlite:guessthebest.db), so it is created
# in the working directory — point that at the /data volume so it persists.
WORKDIR /data

EXPOSE 8080
CMD ["/app/bin/guessthebest-backend"]
