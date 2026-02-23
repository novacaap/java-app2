# Runtime only - JAR is built in CI (publish job) and copied from context.
# JAR_FILE and REPO_NAME are passed at build time.
FROM amazoncorretto:21-alpine-jdk

EXPOSE 8080

ARG JAR_FILE
ARG REPO_NAME
LABEL repo.name="${REPO_NAME}"

COPY target/${JAR_FILE} /app/${JAR_FILE}

ENTRYPOINT ["java", "-jar", "/app/${JAR_FILE}"]
