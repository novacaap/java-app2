# Runtime only - JAR is built in CI (Maven-Docker-Build job) and copied from context.
# JAR_FILE and REPO_NAME are passed at build time.
FROM eclipse-temurin:21-jre-alpine

EXPOSE 8080

ARG JAR_FILE
ARG REPO_NAME
ENV REPO_NAME=${REPO_NAME}
ENV JAR_FILE=${JAR_FILE}
LABEL repo.name=${REPO_NAME}

COPY target/${JAR_FILE} /app/${JAR_FILE}
ENTRYPOINT ["java", "-jar", "/app/${JAR_FILE}"]
