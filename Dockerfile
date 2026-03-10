# Runtime only - JAR is built in CI (Maven-Docker-Build job) and copied from context.
# JAR_FILE and REPO_NAME are passed at build time.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
EXPOSE 8080

ARG OTEL_AGENT_VERSION=2.7.0

ARG JAR_FILE
ARG REPO_NAME
ENV REPO_NAME=${REPO_NAME}
ENV JAR_FILE=${JAR_FILE}
LABEL repo.name=${REPO_NAME}

# Default OpenTelemetry / Tempo configuration.
# These can be overridden from Kubernetes (pod env vars) when needed.
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://tempo.monitoring.svc.cluster.local:4317" \
    OTEL_EXPORTER_OTLP_PROTOCOL="grpc" \
    OTEL_SERVICE_NAME="java-app2" \
    OTEL_RESOURCE_ATTRIBUTES="service.namespace=acute-dev,deployment.environment=dev" \
    OTEL_TRACES_SAMPLER="parentbased_always_on" \
    OTEL_METRICS_EXPORTER="none" \
    OTEL_LOGS_EXPORTER="none"

RUN apk add --no-cache curl && \
    curl -L "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar" \
      -o /app/opentelemetry-javaagent.jar

COPY target/${JAR_FILE} /app/${REPO_NAME}/${JAR_FILE}
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
