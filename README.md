# Java App2 – Spring Boot (depends on java-app1)

A Java 21 Spring Boot 3 web application that depends on **java-app1** from GitHub Package Registry. No OCI bucket flow; config is from in-repo `application.properties`.

## About Project

This project is a reference microservice that consumes another artifact (java-app1) from GitHub Package. It demonstrates:

- **Application:** A Spring Boot 3.2 web app on Java 21 with REST endpoints, health check, and OpenAPI/Swagger UI.
- **Dependency:** Maven dependency on `com.example:java-app1` resolved from GitHub Package (no OCI M2 bucket).
- **Build & image:** **Single-stage Dockerfile**; JAR is built by GitHub Actions in the **Maven-Docker-Build** job (Maven runs in Actions, then Docker build uses `target/` in the same job). No Maven inside the image.
- **Configuration:** `application.properties` lives in `src/main/resources/` (no OCI config bucket).
- **CI/CD:** Single job **Maven-Docker-Build** runs Maven (build + test), then builds and pushes the Docker image when `DOCKERHUB_USERNAME` is set; can notify Microsoft Teams.

Use it as a template for microservices that depend on internal packages from GitHub Package and optionally push to Docker Hub, without OCI.

## Features

- **Java 21** with **Spring Boot 3.2**
- **Sample REST API**
  - `GET /` – Welcome message
  - `GET /api/hello?name=...` – Greeting (default: "World")
  - `GET /api/health` – Health check
  - `GET /api/items` – List items
  - `GET /api/items/{id}` – Get item by ID
  - `POST /api/items` – Create item (JSON: `{"name":"...","description":"..."}`)
  - `DELETE /api/items/{id}` – Delete item
- **Swagger UI** at [/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3** spec at [/v3/api-docs](http://localhost:8080/v3/api-docs)

## Prerequisites

- **java-app1** must be published to GitHub Package first (same org or an org your token can read). The `pom.xml` declares a repository with URL `https://maven.pkg.github.com/<owner>/java-app1` and dependency `com.example:java-app1` (version from property `java-app1.version`).

## Build & run locally

Requires **Java 21** and **Maven 3.8+**. Maven must be able to resolve java-app1 from GitHub Package.

1. Configure GitHub Package authentication in `~/.m2/settings.xml` with server `id=github` and credentials (username + PAT):

```xml
<servers>
  <server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_GITHUB_PAT_WITH_READ_PACKAGES</password>
  </server>
</servers>
```

2. Replace `REPO_OWNER` in `pom.xml` in the `<repositories>` URL with your GitHub org or username (e.g. `myorg` in `https://maven.pkg.github.com/myorg/java-app1`).

3. Build and run:

```bash
mvn clean package
java -jar target/java-app2-1.0.0.jar
```

Then open:

- http://localhost:8080
- http://localhost:8080/swagger-ui.html

(Default port may be 8090 if set in `application.properties`.)

## Run with Docker

The Dockerfile is **runtime-only**: it expects the JAR to exist in `target/` (built by CI or locally with Maven). Build the JAR first (see **Build & run locally** for Maven + GitHub Package auth), then build the image:

```bash
# After mvn package (with ~/.m2/settings.xml for GitHub Package)
JAR_FILE=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout).jar
docker build --build-arg JAR_FILE="$JAR_FILE" --build-arg REPO_NAME=java-app2 -t java-app2 .
docker run -p 8080:8080 java-app2
```

Then open http://localhost:8080 and http://localhost:8080/swagger-ui.html

## GitHub Actions: Workflow overview

The **Build and Push** workflow (`.github/workflows/build-and-push.yaml`) uses a single job to run Maven in Actions and build the Docker image from the resulting JAR.

- **Triggers:** Push to branches `main` or `dev`; release `created`; or manual run via **Actions → Workflow dispatch**.
- **Jobs:**
  1. **Maven-Docker-Build:** (Runs when variable `DOCKERHUB_USERNAME` is set.) Checkout, set `REPO_OWNER` in `pom.xml`, configure Maven with `ci/settings.xml` (GitHub Package auth). Run `mvn package`, get project coordinates, then build and push the image (context has `target/` with the JAR). Tag format: `<ref>-<YYMMDDHH>-<short-sha>-<run_number>` (or tag-based when triggered by release).
  2. **Notify-Teams:** Runs after Maven-Docker-Build (always). If secret `TEAMS_WEBHOOK_URL` is set, posts a Microsoft Teams message card with build status and image tag.
  3. **Details:** Writes a run summary to the workflow run page (event, ref, actor, commit link, job status table).

## GitHub Actions: Variables and Secrets

Configure these in the repo **Settings → Secrets and variables → Actions**.

### Variables

| Variable             | Required | Description                                                                                                                                      |
| -------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `DOCKERHUB_USERNAME` | No       | Docker Hub username. When set, the **Maven-Docker-Build** job runs and pushes the image as `DOCKERHUB_USERNAME/java-app2:<tag>`. Omit to skip the build job. |

### Secrets

| Secret                 | Required        | Description                                                                                                                                                                                                                                                                                             |
| ---------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `GH_PACKAGES_PAT`      | Yes             | **Classic** personal access token so Maven can resolve java-app1 from GitHub Package. Create at GitHub → Settings → Developer settings → Personal access tokens → **Tokens (classic)**. Required scopes: **`read:packages`**; add **`repo`** if the repository or java-app1 package is private.        |
| `GH_PACKAGES_USERNAME` | Recommended      | GitHub **username (login)** of the account that owns `GH_PACKAGES_PAT`. Maven uses this for auth. If unset, the workflow uses `github.actor`. Set explicitly if the actor is not the token owner.                                                                                                          |
| `DOCKERHUB_TOKEN`      | When Docker push | Docker Hub personal access token (Read & Write). Required only when `DOCKERHUB_USERNAME` is set and you want to push the image. Create at [Docker Hub → Security](https://hub.docker.com/settings/security).                                                                                            |
| `TEAMS_WEBHOOK_URL`    | No               | Microsoft Teams incoming webhook URL. When set, the **Notify-Teams** job posts a card with build status and image tag. If unset, notification is skipped. Create in Teams: channel → Connectors → Incoming Webhook.                                                                |

No OCI variables or secrets are used.

## Cross-repo notes

- **Same org:** If java-app1 and java-app2 are in the same GitHub org, use a **Classic** PAT (`GH_PACKAGES_PAT`) with **Packages: Read** (and **repo** if repos are private) so the workflow can read java-app1’s package.
- **Different org:** If java-app1 is in another org, create the Classic PAT for a user with access to that package, with **read:packages** (and **repo** if needed), and store it as `GH_PACKAGES_PAT` in java-app2’s repo secrets. Set `GH_PACKAGES_USERNAME` to that user’s GitHub login.
