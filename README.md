# Java App2 – Spring Boot (depends on java-app1)

A Java 21 Spring Boot 3 web application that depends on **java-app1** from GitHub Package Registry. No OCI bucket flow; config is from in-repo `application.properties`.

## About Project

This project is a reference microservice that consumes another artifact (java-app1) from GitHub Package. It demonstrates:

- **Application:** A Spring Boot 3.2 web app on Java 21 with REST endpoints, health check, and OpenAPI/Swagger UI.
- **Dependency:** Maven dependency on `com.example:java-app1` resolved from GitHub Package (no OCI M2 bucket).
- **Build & image:** Maven build in CI; the Docker image is built from the JAR artifact (single-stage Dockerfile, no Maven inside the image). For local Docker build, run `mvn package` first so the JAR exists at `target/java-app2-1.0.0.jar`.
- **Configuration:** `application.properties` lives in `src/main/resources/` (no OCI config bucket).
- **CI/CD:** GitHub Actions workflow that resolves java-app1 from GitHub Package, builds the JAR, optionally builds and pushes a Docker image to Docker Hub, and can notify Microsoft Teams.

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

The Dockerfile expects a pre-built JAR (no Maven stage in the image). Build the JAR first, then build the image. For local builds, pass `JAR_FILE` and `REPO_NAME` as build-args to match the artifact name:

```bash
mvn package
docker build --build-arg JAR_FILE=java-app2-1.0.0.jar --build-arg REPO_NAME=java-app2 -t java-app2 .
docker run -p 8080:8080 java-app2
```

Then open http://localhost:8080 and http://localhost:8080/swagger-ui.html

## GitHub Actions: Workflow overview

The **Build and Push** workflow (`.github/workflows/build-and-push.yaml`) builds the app, optionally builds and pushes a Docker image from the JAR artifact, and can notify Microsoft Teams.

- **Triggers:** Push to branches `main` or `dev`; release `created`; or manual run via **Actions → Workflow dispatch**.
- **Jobs:**
  1. **publish:** Checkout, set up JDK 21, replace `REPO_OWNER` in `pom.xml`, configure Maven from `ci/settings.xml` (credentials: `GH_PACKAGES_USERNAME`, `GH_PACKAGES_PAT`), run `mvn package` (build + test). App2 does not publish to GitHub Packages; the JAR is uploaded as an artifact for the Docker job. Job summary is written to the Actions run page.
  2. **docker:** (Optional) If variable `DOCKERHUB_USERNAME` is set: download the JAR artifact, prepare build context, set Docker tag, log in to Docker Hub, build and push the image (single-stage, with `JAR_FILE` and `REPO_NAME` build-args). Tag format: `<ref>-<YYMMDDHH>-<short-sha>-<run_number>` (or tag-based when triggered by release). Job summary is written to the Actions run page.
  3. **notify-teams:** Runs after publish (and docker when applicable). If secret `TEAMS_WEBHOOK_URL` is set, posts a Microsoft Teams message card with build status and image tag (when Docker push ran). If the secret is unset, notification is skipped.
  4. **summary:** Writes a run summary to the workflow run page (event, ref, actor, commit link, job status table).

## GitHub Actions: Variables and Secrets

Configure these in the repo **Settings → Secrets and variables → Actions**.

### Variables

| Variable             | Required | Description                                                                                                                                      |
| -------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `DOCKERHUB_USERNAME` | No       | Docker Hub username. When set, the **docker** job builds and pushes the image as `DOCKERHUB_USERNAME/java-app2:<tag>`. Omit to skip Docker push. |

### Secrets

| Secret                 | Required        | Description                                                                                                                                                                                                                                                                                             |
| ---------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `GH_PACKAGES_PAT`      | Yes             | **Classic** personal access token so Maven can resolve java-app1 from GitHub Package. Create at GitHub → Settings → Developer settings → Personal access tokens → **Tokens (classic)**. Required scopes: **`read:packages`**; add **`repo`** if the repository or java-app1 package is private.        |
| `GH_PACKAGES_USERNAME` | Recommended      | GitHub **username (login)** of the account that owns `GH_PACKAGES_PAT`. Maven uses this for auth. If unset, the workflow uses `github.actor`. Set explicitly if the actor is not the token owner.                                                                                                          |
| `DOCKERHUB_TOKEN`      | When Docker push | Docker Hub personal access token (Read & Write). Required only when `DOCKERHUB_USERNAME` is set and you want to push the image. Create at [Docker Hub → Security](https://hub.docker.com/settings/security).                                                                                            |
| `TEAMS_WEBHOOK_URL`    | No               | Microsoft Teams incoming webhook URL. When set, the **notify-teams** job posts a card with build status and image tag (if Docker push ran). If unset, notification is skipped. Create in Teams: channel → Connectors → Incoming Webhook.                                                                |

No OCI variables or secrets are used.

## Cross-repo notes

- **Same org:** If java-app1 and java-app2 are in the same GitHub org, use a **Classic** PAT (`GH_PACKAGES_PAT`) with **Packages: Read** (and **repo** if repos are private) so the workflow can read java-app1’s package.
- **Different org:** If java-app1 is in another org, create the Classic PAT for a user with access to that package, with **read:packages** (and **repo** if needed), and store it as `GH_PACKAGES_PAT` in java-app2’s repo secrets. Set `GH_PACKAGES_USERNAME` to that user’s GitHub login.
