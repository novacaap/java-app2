# Java App2 – Spring Boot (uses java-app1 as library)

A Java 21 Spring Boot 3 web application that **uses java-app1 as a library** (shared models `Item`, `Message`) from GitHub Package Registry. No OCI bucket; configuration is from in-repo `application.properties`. CI runs Maven and builds a single-stage Docker image in one job (**Maven-Docker-Build**).

---

## Use case

**java-app2** is a Spring Boot application that depends on **java-app1** for shared types. It declares the Maven dependency `com.example:java-app1`, imports `com.example.javaapp1.model.Item` and `Message`, and implements the REST layer (controllers, Swagger, health). The library (java-app1) is a plain JAR with no Spring or web server; this app is the runnable service and optionally produces a Docker image.

---

## How the system works

1. **java-app1** is a plain Java library that publishes `Item` and `Message` (package `com.example.javaapp1.model`) to GitHub Package.
2. **java-app2** (this repo) adds `com.example:java-app1` as a dependency, imports those types, and implements the REST API and Docker image.
3. Flow: Publish java-app1 → any app (e.g. java-app2) adds the dependency and uses the library; CI builds this app and optionally pushes `DOCKERHUB_USERNAME/<DOCKERHUB_IMAGE>:<tag>` (image name from variable **DOCKERHUB_IMAGE**).

```
java-app1 (library) → GitHub Package → java-app2 (Maven dep) → mvn package → JAR in target/ → Docker image (optional)
```

---

## About this project

| Aspect | Details |
|--------|---------|
| **Application** | Spring Boot 3.2.5 on Java 21 – REST API, health check, OpenAPI/Swagger UI |
| **Group/Artifact** | `com.example:java-app2` (see `pom.xml`) |
| **Version** | Defined in `pom.xml` (e.g. `1.0.0`) |
| **Library dependency** | `com.example:java-app1` (version from property `java-app1.version`, e.g. `1.0.2`) – provides `Item`, `Message` from `com.example.javaapp1.model` |
| **Build & image** | Single job **Maven-Docker-Build**: Maven runs in Actions (resolves java-app1 from GitHub Package, produces JAR), then Docker build uses `target/<jar>` and `entrypoint.sh` in the same job. Single-stage Dockerfile; no Maven in the image; container starts via **`/app/entrypoint.sh`** (runs `java -jar /app/app.jar`). |
| **Configuration** | `application.properties` in `src/main/resources/` (no OCI config bucket). |
| **CI/CD** | GitHub Actions: **Sonar** (optional; tests with JaCoCo coverage, Sonar scan, quality gate fails = build reject), **Maven-Docker-Build** (after Sonar when configured; build, test, Docker build/push), **Notify-Teams**, **Details**. |

Use this repo as a template for applications that consume the java-app1 library and optionally push a Docker image to Docker Hub.

---

## How Docker build works in CI

Docker is built **in the same CI job** as the Maven build (**Maven-Docker-Build**). There is no separate job that downloads an artifact.

1. **Checkout** → **Configure Maven** (`ci/settings.xml` for GitHub Package auth).
2. **`mvn package`** → JAR is produced in `target/<artifactId>-<version>.jar`. Maven resolves java-app1 from GitHub Package during this step.
3. **Get project coordinates** → `mvn help:evaluate` for `jar_file` (e.g. `java-app2-1.0.0.jar`).
4. **Set Docker tag** → e.g. `main-YYMMDDHH-<short-sha>-<run_number>`.
5. **Docker login** → **Build and push** with build context `.` (so `target/` and `entrypoint.sh` are available), build-args `JAR_FILE` and `REPO_NAME`. The Dockerfile is **single-stage, runtime-only**: base image is JRE (`eclipse-temurin:21-jre-alpine`), no Maven in the image; it copies `target/${JAR_FILE}` to `/app/app.jar`, copies and sets executable `entrypoint.sh`, and uses **`/app/entrypoint.sh`** as `ENTRYPOINT` (the script runs `java -jar /app/app.jar`).

**Summary:** The same job runs Maven (which resolves java-app1 from GitHub Package and produces the app JAR) and then builds the Docker image from that JAR; the image is runtime-only (no build tools).

---

## Project structure

```
java-app2/
├── .github/workflows/
│   └── build-and-push.yaml      # Maven-Docker-Build, Sonar, Notify-Teams, Details
├── ci/
│   └── settings.xml             # Maven server "github" – uses env GH_PACKAGES_USERNAME, GH_PACKAGES_PAT
├── src/main/java/...            # Spring Boot app (controllers, Application); Item/Message from java-app1
├── src/main/resources/
│   └── application.properties   # Server port, app name, etc.
├── src/test/...                 # Unit/integration tests
├── pom.xml                      # groupId com.example, artifactId java-app2, repositories + dependency on java-app1
├── Dockerfile                   # Runtime-only: COPY target/${JAR_FILE} → /app/app.jar, entrypoint.sh; ENTRYPOINT /app/entrypoint.sh
├── entrypoint.sh                # Container entrypoint: exec java -jar /app/app.jar (extend for JVM opts, etc.)
└── README.md
```

**Key files**

- **`pom.xml`** – `<repositories>` points to `https://maven.pkg.github.com/<owner>/java-app1`. Dependency `com.example:java-app1` with version from property `java-app1.version`. Replace `REPO_OWNER` (or the full owner in the URL) for your org.
- **`ci/settings.xml`** – Server `id=github`; credentials from env `GH_PACKAGES_USERNAME` and `GH_PACKAGES_PAT` (Classic PAT with `read:packages`).
- **`Dockerfile`** – Expects build-args `JAR_FILE` and `REPO_NAME`. Copies `target/${JAR_FILE}` to `/app/app.jar` and `entrypoint.sh` to `/app/entrypoint.sh`; `ENTRYPOINT` is `/app/entrypoint.sh`. Base: `eclipse-temurin:21-jre-alpine`.
- **`entrypoint.sh`** – Shell script that runs `exec java -jar /app/app.jar "$@"`. You can extend it for JVM options, pre-run checks, or wrapping the Java process.

---

## Prerequisites

- **java-app1** must be published to GitHub Package (same org or an org your PAT can read). `pom.xml` declares the repository URL and dependency `com.example:java-app1` (version from `java-app1.version`).

---

## Features

- **Java 21** with **Spring Boot 3.2**
- **REST API**
  - `GET /` – Welcome
  - `GET /api/hello?name=...` – Greeting (default: "World")
  - `GET /api/health` – Health check
  - `GET /api/items`, `GET /api/items/{id}`, `POST /api/items`, `DELETE /api/items/{id}`
- **Swagger UI** – [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3** – [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## Distributed tracing with Tempo (OpenTelemetry)

This service is preconfigured to emit **OpenTelemetry traces** via the OpenTelemetry Java Agent and export them to **Grafana Tempo**.

### How it works

- The Docker image includes the **OpenTelemetry Java Agent** (downloaded at build time).
- The container entrypoint runs:

```bash
java -javaagent:/app/opentelemetry-javaagent.jar ${JAVA_OPTS} -jar /app/${REPO_NAME}/${JAR_FILE}
```

- The agent auto-instruments:
  - Incoming HTTP requests (Spring MVC / Tomcat)
  - JDBC / HTTP clients (if present)
  - JVM runtime
- Traces are exported via **OTLP** to your Tempo endpoint, configured with environment variables.

### Required environment variables for Tempo

Set these environment variables on the container (Kubernetes `env`, Docker `-e`, etc.):

```bash
# Where Tempo OTLP gRPC endpoint is reachable from the app
OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo.monitoring.svc.cluster.local:4317
OTEL_EXPORTER_OTLP_PROTOCOL=grpc

# Service identity
OTEL_SERVICE_NAME=java-app2
OTEL_RESOURCE_ATTRIBUTES=service.namespace=acute-dev,service.instance.id=${HOSTNAME},deployment.environment=dev

# Tracing sampler (always on at the edge, but respects upstream parent sampling)
OTEL_TRACES_SAMPLER=parentbased_always_on

# We only use traces; logs/metrics are disabled here
OTEL_METRICS_EXPORTER=none
OTEL_LOGS_EXPORTER=none
```

Adjust the `OTEL_EXPORTER_OTLP_ENDPOINT` to match your Tempo Service name/namespace.

### Logs with TraceID / SpanID

The application uses **Logback** with a pattern that includes the OpenTelemetry correlation IDs:

- Pattern (see `src/main/resources/logback-spring.xml`):

```text
%d{ISO8601} %-5level [%thread] trace_id=%X{trace_id} span_id=%X{span_id} %logger - %msg%n
```

When the Java Agent is active, each log line within a trace context will contain:

- `trace_id` – the trace identifier (matches Tempo / Grafana “Trace ID”)
- `span_id` – the current span identifier

If your logs are shipped to a centralized system (e.g. Loki), you can:

1. **Filter** logs by `trace_id=<value>` copied from Tempo.
2. Or click from a log line (if supported by your tooling) to open the corresponding trace in Grafana Tempo.

---

## Using the application

- **As a dependency:** Other projects that need **shared types** (Item, Message) should depend on **java-app1**. This repo (java-app2) is the **runnable application** that uses that library and exposes a REST API and Docker image.
- **Running locally:** Build with Maven (requires GitHub Package auth to resolve java-app1), then run the JAR (see below).
- **Running with Docker:** Build the JAR locally (or use CI-built artifact), then build the image with `JAR_FILE` and `REPO_NAME` build-args and run the container.
- **Using the Docker image:** The image is the runnable Spring Boot app (REST API, Swagger). Image name/tag: `DOCKERHUB_USERNAME/<DOCKERHUB_IMAGE>:<tag>` (e.g. `myuser/java-app2:main-26022314-a1b2c3d-42`). Set variable **DOCKERHUB_IMAGE** in the repo (e.g. `java-app2`). Exposes port 8080; use for deployment, local testing, or as a base for further customization.

---

## Build and run locally

Requires **Java 21** and **Maven 3.8+**. Maven must be able to resolve **java-app1** from GitHub Package.

1. **Configure GitHub Package in `~/.m2/settings.xml`** (server `id=github`, username + PAT with `read:packages`).

2. **Set repository URL in `pom.xml`** – Replace the owner in the `<repositories>` URL with your GitHub org/user (e.g. `https://maven.pkg.github.com/myorg/java-app1`).

3. **Build and run:**

```bash
mvn clean package
java -jar target/java-app2-1.0.0.jar
```

(Use the version from your `pom.xml` if different.) Then open http://localhost:8080 and http://localhost:8080/swagger-ui.html (port may be overridden in `application.properties`).

---

## Run with Docker

The Dockerfile is **runtime-only**: it expects the JAR in `target/` and `entrypoint.sh` in the project root. Build the JAR first (see above), then build the image with the correct `JAR_FILE` and `REPO_NAME`:

```bash
# After mvn package (with ~/.m2/settings.xml for GitHub Package)
JAR_FILE=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout).jar
docker build --build-arg JAR_FILE="$JAR_FILE" --build-arg REPO_NAME=java-app2 -t java-app2 .
docker run -p 8080:8080 java-app2
```

---

## GitHub Actions – workflow overview

**Workflow file:** `.github/workflows/build-and-push.yaml`  
**Name:** Build and Push

| Trigger | Branches `main`, `dev`; release `created`; or **workflow_dispatch** |
|---------|----------------------------------------------------------------------|
| Permissions | `contents: read`, `packages: read` |

### Jobs

1. **Sonar** (runs only when variables **SONAR_HOST_URL** and **SONAR_PROJECT_KEY** are set)
   - Checkout (full depth) → Set up JDK 21 → Configure Maven.
   - Run `mvn verify` with **JaCoCo** (test coverage) and Sonar scan via `sonar-maven-plugin`. Supports **SonarCloud** or **self-hosted SonarQube** (we use self-hosted). Requires secret **SONAR_TOKEN** and variables **SONAR_HOST_URL**, **SONAR_PROJECT_KEY**; for SonarCloud only, also set **SONAR_ORGANIZATION**.
   - Uses **`sonar.qualitygate.wait=true`**: the job **fails the workflow** (build reject) if the Sonar quality gate fails on the server. Coverage is reported in Sonar from the JaCoCo report. Quality gate rules (e.g. coverage thresholds) are configured in the Sonar server, not in the repo.

2. **Maven-Docker-Build** (runs when **DOCKERHUB_USERNAME** is set; depends on **Sonar**)
   - Runs only after **Sonar** has succeeded or was skipped (no Sonar vars). If Sonar runs and the quality gate fails, this job is skipped and no image is pushed.
   - Checkout → Set up JDK 21 (Temurin) → Copy `ci/settings.xml` to `~/.m2/settings.xml`.
   - Run `mvn --batch-mode package` (build + test).
   - **Get project coordinates:** `mvn help:evaluate` for `project.version` and `project.artifactId`; output `jar_file` = `artifactId-version.jar`.
   - **Set Docker tag:** e.g. `main-YYMMDDHH-<short-sha>-<run_number>` or for release `ref-name-<short-sha>`.
   - Log in to Docker Hub → Set up Buildx → **Build and push** with `context: .`, build-args `JAR_FILE` and `REPO_NAME`, tag `DOCKERHUB_USERNAME/<DOCKERHUB_IMAGE>:<tag>` (image name from variable **DOCKERHUB_IMAGE**).
   - Write job summary (image, tag, JAR name).

3. **Notify-Teams**
   - `needs: Maven-Docker-Build`, `if: always()`.
   - If secret `TEAMS_WEBHOOK_URL` is set, posts a Microsoft Teams message card (build status, image tag when build ran).

4. **Details**
   - `needs: Maven-Docker-Build`, `if: always()`.
   - Writes run summary to the Actions run page (event, ref, actor, commit, job status table).

### Docker tag format

- Branch/tag: `<ref>-<YYMMDDHH>-<short-sha>-<run_number>` (e.g. `main-26022314-a1b2c3d-42`).
- Release: `<ref>-<short-sha>` (e.g. `v1.0.0-a1b2c3d`).

---

## Variables and secrets

Configure in **Settings → Secrets and variables → Actions**.

### Variables

| Variable | Required | Description |
|----------|----------|-------------|
| **DOCKERHUB_USERNAME** | No | Docker Hub username. When set, **Maven-Docker-Build** runs and pushes `DOCKERHUB_USERNAME/<DOCKERHUB_IMAGE>:<tag>`. Omit to skip the build job (Notify-Teams and Details still run with skipped build). |
| **DOCKERHUB_IMAGE** | When pushing image | Docker image name (e.g. `java-app2`). Used as the image name when pushing to Docker Hub; the full image is `DOCKERHUB_USERNAME/DOCKERHUB_IMAGE:<tag>`. Set in **Settings → Secrets and variables → Actions → Variables**. |
| **SONAR_HOST_URL** | When using Sonar | Sonar server URL. For **self-hosted SonarQube** use your server URL (e.g. `https://sonar.company.com`). For **SonarCloud** use `https://sonarcloud.io`. |
| **SONAR_PROJECT_KEY** | When using Sonar | Sonar project key (e.g. `java-app2` or `myorg_java-app2`). Must match the key in your Sonar server or SonarCloud project settings. |
| **SONAR_ORGANIZATION** | SonarCloud only | SonarCloud organization key (e.g. `myorg`). Set only when using **SonarCloud**; leave empty for self-hosted SonarQube. Find it in [SonarCloud → My Account → Organizations](https://sonarcloud.io/account/organizations). |

### Secrets

| Secret | Required | Description |
|--------|----------|-------------|
| **GH_PACKAGES_PAT** | Yes | **Classic** personal access token so Maven can resolve java-app1 from GitHub Package. Create at GitHub → Settings → Developer settings → Personal access tokens → **Tokens (classic)**. Scopes: **`read:packages`**; add **`repo`** if the repo or java-app1 package is private. |
| **GH_PACKAGES_USERNAME** | Recommended | GitHub username that owns the PAT. If unset, workflow uses `github.actor`. |
| **DOCKERHUB_TOKEN** | When pushing image | Docker Hub personal access token (Read & Write). Required when `DOCKERHUB_USERNAME` is set. Create at [Docker Hub → Security](https://hub.docker.com/settings/security). |
| **SONAR_TOKEN** | When using Sonar | Sonar authentication token for CI. For **self-hosted**: create in SonarQube → My Account → Security. For **SonarCloud**: create at [SonarCloud → My Account → Security](https://sonarcloud.io/account/security). Required when **SONAR_HOST_URL** and **SONAR_PROJECT_KEY** are set. |
| **TEAMS_WEBHOOK_URL** | No | Microsoft Teams incoming webhook URL. If set, **Notify-Teams** posts a card. Create in Teams: channel → Connectors → Incoming Webhook. |

No OCI variables or secrets are used.

### Sonar: SonarCloud vs self-hosted

The **Sonar** job works with both SonarCloud and self-hosted SonarQube. Set **SONAR_HOST_URL** and **SONAR_PROJECT_KEY** (and **SONAR_TOKEN** as a secret); the job runs when those are set. It runs tests with **JaCoCo** coverage and fails the workflow if the Sonar **quality gate** fails (build reject). The quality gate (e.g. coverage thresholds, no new critical issues) is configured in your Sonar server (SonarCloud or self-hosted), not in this repo.

| Use case | **SONAR_HOST_URL** | **SONAR_PROJECT_KEY** | **SONAR_ORGANIZATION** | **SONAR_TOKEN** |
|----------|--------------------|------------------------|-------------------------|-----------------|
| **Self-hosted SonarQube** (current setup) | Your server URL (e.g. `https://sonar.company.com`) | Project key from your Sonar server | Leave empty | Token from SonarQube → My Account → Security |
| **SonarCloud** | `https://sonarcloud.io` | Project key (e.g. `myorg_java-app2`) | Your SonarCloud org key | Token from [SonarCloud → Security](https://sonarcloud.io/account/security) |

---

## Cross-repo notes

- **Same org:** If java-app1 and java-app2 are in the same GitHub org, use a **Classic** PAT with **Packages: Read** (and **repo** if repos are private).
- **Different org:** If java-app1 is in another org, create a Classic PAT for a user with access to that package (`read:packages`, and **repo** if needed); store as `GH_PACKAGES_PAT` in java-app2 and set `GH_PACKAGES_USERNAME` to that user’s GitHub login.
