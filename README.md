# PicPay Simplificado

<p>
  <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen?logo=springboot&logoColor=white" alt="Spring Boot 3.2.4"/>
  <img src="https://img.shields.io/badge/Maven-build-C71A36?logo=apachemaven&logoColor=white" alt="Maven"/>
  <img src="https://img.shields.io/badge/H2-database-blue?logo=h2&logoColor=white" alt="H2 Database"/>
  <img src="https://img.shields.io/badge/JUnit%205-tests-25A162?logo=junit5&logoColor=white" alt="JUnit 5"/>
  <img src="https://img.shields.io/badge/status-learning%20project-yellow" alt="Status"/>
</p>

A REST API that simulates the backend of a peer-to-peer payment service in the style of PicPay: user registration (common users and merchants), money transfers between accounts, and business validations such as balance checks, user type restrictions, and external transaction authorization.

## About the project

This repository was built as a learning project focused on deepening knowledge of **Spring Boot** and common practices for a backend API in Java: layered architecture, dependency injection, persistence with JPA, centralized exception handling, integration with external services via `RestTemplate`, and a full test suite covering both isolated unit tests and end-to-end integration tests.

More than just "making it work," the goal was to understand the *why* behind each decision — how to separate responsibilities between `controller`, `service`, and `repository`, how to model business rules in the domain layer, and how to cover that behavior with reliable, well-isolated tests.

## Features

- User registration, with two distinct profiles: `COMMON` (can send and receive money) and `MERCHANT` (can only receive)
- CPF validation on registration, using the [Caelum Stella](https://github.com/caelum/caelum-stella) library to check digit correctness (not just format)
- Listing of all registered users
- Money transfer between two users, with:
  - Sender balance validation
  - Merchants blocked from sending money
  - External transaction authorization via a mock service (simulating an anti-fraud service)
  - Asynchronous notification to both sender and receiver upon completion
- Centralized error handling, with standardized HTTP responses and messages for each type of failure

## Tech stack

| Category              | Technology                                                      |
| --------------------- | --------------------------------------------------------------- |
| Language              | Java 17                                                         |
| Framework             | Spring Boot 3.2.4 (Web, Data JPA)                               |
| Persistence           | Spring Data JPA + H2 (in-memory database)                       |
| Build                 | Maven (with Maven Wrapper)                                      |
| Boilerplate reduction | Lombok                                                          |
| HTTP integration      | RestTemplate (external authorization and notification services) |
| CPF validation        | Caelum Stella (`caelum-stella-core`)                            |
| Testing               | JUnit 5, Mockito, AssertJ, MockMvc, MockRestServiceServer       |

## Architecture

The project follows a layered architecture, organized by responsibility:

```
src/main/java/com/picpaysimplificado
├── controllers/     # REST endpoints (UserController, TransactionController)
├── services/        # Business rules (UserService, TransactionService, NotificationService, CpfValidationService)
├── repositories/     # Data access via Spring Data JPA
├── domain/           # JPA entities (User, Transaction, UserType)
├── DTOs/              # Records used as the API's input/output contract
├── exception/          # Custom business exceptions
└── infra/                # Configuration and global exception handling
```

This separation keeps controllers thin (only orchestrating request/response), concentrates business rules in the services, and isolates data access in the repositories — making it easier to both maintain the code and write tests isolated by layer.

## Business rules

1. A user's `document` must be a valid CPF (correct check digits, not just 11 digits) — registration is rejected otherwise.
2. A `MERCHANT` user cannot send money, only receive it.
3. The sender must have a balance equal to or greater than the transaction value.
4. Every transaction goes through an external authorization service; if it denies the request, the transaction is blocked.
5. After an authorized transaction, both the sender's and receiver's balances are updated and both receive a notification. The whole operation runs inside a transaction (`@Transactional`), guaranteeing atomicity: any failure partway through the process rolls back the changes already made.
6. Failures at any step (user not found, insufficient balance, unauthorized, invalid CPF, notification service unavailable) interrupt the operation and return a clear error message.

## API endpoints

### Users

| Method | Route    | Description                |
| ------ | -------- | -------------------------- |
| POST   | `/users` | Creates a new user         |
| GET    | `/users` | Lists all registered users |

**Example request — `POST /users`**
```json
{
  "firstName": "Joanne",
  "lastName": "Silva",
  "document": "12345678909",
  "balance": 500.00,
  "email": "joanne@email.com",
  "password": "senha123",
  "userType": "COMMON"
}
```
> `document` must be a valid CPF — the API rejects registrations with mistyped or fake numbers.

### Transactions

| Method | Route           | Description                           |
| ------ | --------------- | ------------------------------------- |
| POST   | `/transactions` | Performs a transfer between two users |

**Example request — `POST /transactions`**
```json
{
  "value": 100.00,
  "senderId": 1,
  "receiverId": 2
}
```

## How to run the project

**Prerequisites:** Java 17+ installed. Maven doesn't need to be installed globally — the project already includes the Maven Wrapper.

```bash
# clone the repository
git clone https://github.com/joannegs/picpay-simplificado.git
cd picpay-simplificado

# run the application (Windows)
.\mvnw.cmd spring-boot:run

# run the application (Linux/macOS)
./mvnw spring-boot:run
```

The API runs by default on `http://localhost:8080`.

The project uses an in-memory H2 database, with no additional configuration required. The H2 console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testbd`, user: `sa`, no password).

## How to run the tests

```bash
# Windows
.\mvnw.cmd test

# Linux/macOS
./mvnw test
```

To run only a subset:

```bash
# unit tests only
./mvnw test -Dtest='*Test,!*IntegrationTest'

# integration tests only
./mvnw test -Dtest='*IntegrationTest'
```

The project has two layers of automated tests:

- **Unit tests** — isolate each class with Mockito, covering the business rules in the services, the controllers' behavior, and the global exception handling, without touching a real database or making real HTTP calls.
- **Integration tests** — exercise the real Spring context end-to-end: controllers are called through `MockMvc` against a real H2 database (with automatic transaction rollback after each test), while external HTTP calls (authorization and notification services) are intercepted with `MockRestServiceServer` bound to the actual `RestTemplate` bean. They also cover the repository layer with `@DataJpaTest`, and verify that a failure mid-transaction (e.g., the notification service being unavailable) correctly rolls back balance changes and the persisted transaction, thanks to `@Transactional(rollbackFor = Exception.class)`.

```
src/test/java/com/picpaysimplificado
├── controllers/
│   ├── UserControllerTest.java                    # unit
│   ├── UserControllerIntegrationTest.java          # integration
│   ├── TransactionControllerTest.java              # unit
│   └── TransactionControllerIntegrationTest.java   # integration
├── services/
│   ├── UserServiceTest.java                        # unit
│   ├── TransactionServiceTest.java                 # unit
│   ├── NotificationServiceTest.java                # unit
│   └── CpfValidationServiceTest.java               # unit
├── repositories/
│   ├── UserRepositoryIntegrationTest.java          # integration
│   └── TransactionRepositoryIntegrationTest.java   # integration
└── infra/
    └── ControllerExceptionHandlerTest.java          # unit
```

## Possible future improvements

Ideas to keep evolving the project as a learning exercise:

- Authentication and authorization with Spring Security + JWT
- Interactive API documentation with Swagger/OpenAPI
- Migrating from H2 to a persistent relational database (PostgreSQL) in production
- Containerization with Docker and a CI/CD pipeline

## Author

Developed by **Joanne Silva** as a Spring Boot learning project.

- GitHub: [@joannegs](https://github.com/joannegs)
- Email: joanneegabriela@gmail.com
