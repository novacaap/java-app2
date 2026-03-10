#!/bin/sh
set -e

# Allow extra JVM options via JAVA_OPTS
JAVA_OPTS="${JAVA_OPTS:-}"

exec java -javaagent:/app/opentelemetry-javaagent.jar ${JAVA_OPTS} -jar /app/${REPO_NAME}/${JAR_FILE} "$@"
