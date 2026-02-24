#!/bin/sh
set -e
exec java -jar /app/${REPO_NAME}/${JAR_FILE} "$@"
