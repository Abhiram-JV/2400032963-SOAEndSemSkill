# Banking Microservices - Advanced SOA Question 7

This is a deliberately small three-project demonstration of Eureka discovery,
JWT-protected account CRUD, and API Gateway path rewriting.

## Project layout

```text
banking-microservices/
├── eureka-server/
├── account-service/
├── api-gateway/
└── README.md
```

Each directory is an independent Maven project. There is no root Maven
project and no additional microservice.

## Technology and credentials

- Java 25 (latest LTS), Spring Boot 3.3.5, Spring Cloud 2023.0.3
- Eureka Server on `8761`
- Account Service on `8081` and `8082`
- API Gateway on `8080`
- H2 in-memory database and Spring Data JPA
- JWT login credentials: `admin` / `password`

The Account Service POM enables Byte Buddy experimental mode for Mockito
tests on Java 25. All services compile and run with Java 25.

## Question 7 path rewriting

The Gateway route uses:

```yaml
uri: lb://ACCOUNT-SERVICE
predicates:
  - Path=/accounts,/accounts/**
filters:
  - RewritePath=^/accounts(?<remaining>/.*)?$, /internal/accounts${remaining}
```

Therefore:

```text
/accounts       -> /internal/accounts
/accounts/101   -> /internal/accounts/101
```

The Gateway logs the original public path. The Account Service logs the
received rewritten backend path. Eureka load balancing selects either
registered Account Service instance.

## Run in VS Code or STS 4

Install Java 25 and Maven, then open the root folder in VS Code. In STS 4,
import each directory separately using **File > Import > Existing Maven
Projects**.

Start Eureka first:

```powershell
cd eureka-server
mvn spring-boot:run
```

Start Account Service twice in two terminals:

```powershell
cd account-service
mvn spring-boot:run
```

```powershell
cd account-service
$env:SERVER_PORT="8082"
$env:INSTANCE_ID="account-service-2"
mvn spring-boot:run
```

Start the Gateway:

```powershell
cd api-gateway
mvn spring-boot:run
```

Open `http://localhost:8761` to see registered services.

## Postman tests

### 1. Login

```text
POST http://localhost:8080/auth/login
Content-Type: application/json
```

Body:

```json
{
  "username": "admin",
  "password": "password"
}
```

Copy the returned `token` and use it as a Bearer token for the following
requests.

### 2. Create an account

```text
POST http://localhost:8080/internal/accounts
Authorization: Bearer <token>
Content-Type: application/json
```

Body:

```json
{
  "accountHolderName": "Asha",
  "balance": 1000.00
}
```

Expected response: HTTP `201 Created` with the generated account ID.

### 3. Question 7 public routes

```text
GET http://localhost:8080/accounts
GET http://localhost:8080/accounts/1
```

Expected responses are JSON account data or an empty JSON array when the
selected instance has no account. Add the Bearer token to both requests.

### 4. Other CRUD routes

```text
PUT    http://localhost:8080/internal/accounts/1
DELETE http://localhost:8080/internal/accounts/1
```

For a direct service check, use port `8081` or `8082` and the `/internal`
paths. The service console prints, for example:

```text
API Gateway original request path: /accounts/1
Account Service received backend request path: /internal/accounts/1
```

### Invalid JWT

Call a protected endpoint without a token or with `Bearer invalid`.
Expected response: HTTP `401 Unauthorized`.

## Build and test

Run these commands inside each project:

```powershell
mvn clean test
mvn package
```

The Account Service tests use JUnit 5, Mockito, and MockMvc for account
retrieval, creation, valid JWT access, and missing JWT rejection.

## Optional TLS demonstration

For a real deployment, configure a keystore instead of committing a
certificate:

```yaml
server:
  ssl:
    enabled: true
    key-store: ${KEYSTORE_PATH}
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

Keep keystore files and passwords outside source control.

## Brief Terraform workflow

Terraform is not required for this local practical. A normal workflow would
be: write provider and container/VM resources, run `terraform init`, inspect
with `terraform plan`, apply with `terraform apply`, and remove with
`terraform destroy`. No cloud infrastructure is provisioned for this practical.

## Troubleshooting

- **Maven not found:** install Maven and confirm `mvn -version`; select a
  Java 25 JDK in VS Code or STS.
- **Dependency download errors:** retry with `mvn clean package -U` and check
  Maven Central connectivity.
- **Port already in use:** stop the old process or change `SERVER_PORT`.
  For the second local Account Service, use `SERVER_PORT=8082`.
- **Gateway returns 401:** obtain a fresh token from `/auth/login` and send
  `Authorization: Bearer <token>`.
- **Gateway returns 503:** start Eureka and both Account Service instances,
  wait a few seconds for registration, then retry. Check `http://localhost:8761`.
- **No account after load balancing:** the sample uses an in-memory H2
  database per instance. Use the same instance for a CRUD sequence during
  the practical, or replace H2 with shared persistence for production.
