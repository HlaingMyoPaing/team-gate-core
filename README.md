# 🛡️ team-gate-core

## 📌 Overview
`team-gate-core` is a **Spring Boot application** that secures and mediates access to the **Kong Admin API** using **Keycloak JWT authentication**.

It ensures that only users with a valid Keycloak-issued JWT can retrieve Kong data (services, routes, upstream health, etc.), without exposing Kong’s Admin API directly to the frontend.

---

## 🔄 Flow

1. **Login (Vue3 + Keycloak JS)**
    - User logs in via the Vue3 frontend.
    - Keycloak issues a **JWT access token**.

2. **Call team-gate-core**
    - The Vue app sends requests (with JWT) to the Spring Boot app.
    - `team-gate-core` validates the JWT using Keycloak’s public keys (via Spring Security OAuth2 resource server).

3. **Forward to Kong**
    - If the JWT is valid → the app securely calls **Kong Admin API**.
    - If the JWT is invalid/expired → returns `401 Unauthorized`.

---

## 🔐 Security Role
- Acts as a **secured proxy** for Kong Admin API.
- Hides Kong from direct external access.
- Enforces **Single Sign-On (SSO)**: once logged into Keycloak, no re-login is needed to use Kong GUI/API.

---

## 🚀 Running

### Prerequisites
- ✅ A running **Keycloak** (JWT provider)
- ✅ A running **Kong** (Admin API enabled)
- ✅ **Maven** installed
- ✅ **Java 17+**

### Build
```bash
mvn clean package
```

### Run
```bash
java -jar target/team-gate-core-0.0.1-SNAPSHOT.jar
```

---

## 📂 Project Structure
```
team-gate-core/
 ├── src/
 │   ├── main/java/...   # Application code
 │   └── main/resources/ # application.yml
 ├── pom.xml             # Maven configuration
 ├── README.md           # Project documentation
```


## Health Check
http://host.docker.internal:8085/actuator/health

## Endpoints
- `GET /api/inventory` — totals: services/routes/upstreams
- `GET /api/services` — per-service + route counts
- `GET /api/upstreams` — per-upstream target counts + health summary
- `GET /api/upstreams/{name}/targets` — list targets + health for one upstream
- `GET /api/traffic` — mock traffic for charts (minutes/step query params)
- `GET /api/admin/ping` — requires ADMIN role

All `/api/**` endpoints require a **Keycloak access token**.

