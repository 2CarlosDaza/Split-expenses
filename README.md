# split-expense

A **Java 21 / Spring Boot 3** REST API for group expense splitting — similar to Splitwise. Users can create groups, record shared expenses, and see who owes what to whom.

Features **JWT authentication**, **event-driven notifications via Apache Kafka**, and automatic equal splitting across group members.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    split-expense                        │
│                                                         │
│  REST API (Spring MVC)                                  │
│       │                                                 │
│  AuthService   GroupService   ExpenseService            │
│       │              │              │                   │
│  UserRepository  GroupRepository  ExpenseRepository     │
│                        │                               │
│                   PostgreSQL (Flyway)                   │
│                                                         │
│  ExpenseEventProducer ──► Kafka ──► ExpenseEventConsumer│
│                    (expense-events topic)               │
└─────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security + JWT (jjwt 0.12) |
| Messaging | Apache Kafka |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Documentation | SpringDoc OpenAPI (Swagger UI) |
| Testing | JUnit 5 / Mockito |
| Build | Gradle |
| Infrastructure | Docker Compose |

---

## Getting Started

### Prerequisites
- Java 21+
- Docker Desktop

### Run infrastructure

```bash
docker-compose up -d
```

### Run the service

```bash
./gradlew bootRun
```

Service starts on `http://localhost:8080`

### Swagger UI

```
http://localhost:8080/swagger-ui.html
```

> All endpoints except `/api/v1/auth/**` require a Bearer token. Register first, copy the `accessToken`, and use it in the Swagger UI Authorize button.

---

## API Endpoints

### Auth (public)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user |
| `POST` | `/api/v1/auth/login` | Login and get JWT |

### Groups (authenticated)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/groups` | Create a group |
| `GET` | `/api/v1/groups/my` | Get my groups |
| `GET` | `/api/v1/groups/{id}` | Get group by ID |
| `POST` | `/api/v1/groups/{id}/members/{userId}` | Add member |
| `DELETE` | `/api/v1/groups/{id}/members/{userId}` | Remove member |

### Expenses (authenticated)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/groups/{groupId}/expenses` | Add expense to group |
| `GET` | `/api/v1/groups/{groupId}/expenses` | List group expenses |
| `GET` | `/api/v1/groups/{groupId}/expenses/balance` | Get balances and debts |

---

## Example Flow

### 1. Register

```json
POST /api/v1/auth/register
{
  "name": "Carlos",
  "email": "carlos@example.com",
  "password": "password123"
}
```

### 2. Create a group

```json
POST /api/v1/groups
Authorization: Bearer <token>
{
  "name": "Cartagena Trip 2026",
  "description": "Beach vacation with friends"
}
```

### 3. Add an expense

```json
POST /api/v1/groups/{groupId}/expenses
Authorization: Bearer <token>
{
  "description": "Hotel night 1",
  "amount": 300.00,
  "currency": "USD",
  "expenseDate": "2026-07-10"
}
```

The expense is split equally among all group members. The payer's split is marked as settled automatically.

### 4. Check balances

```json
GET /api/v1/groups/{groupId}/expenses/balance
Authorization: Bearer <token>

{
  "groupId": "...",
  "groupName": "Cartagena Trip 2026",
  "memberBalances": [
    { "userName": "Carlos", "totalPaid": 300.00, "totalOwed": 100.00, "netBalance": 200.00 },
    { "userName": "Ana",    "totalPaid": 0.00,   "totalOwed": 100.00, "netBalance": -100.00 }
  ],
  "debts": [
    { "fromUserName": "Ana", "toUserName": "Carlos", "amount": 100.00, "currency": "USD" }
  ]
}
```

---

## Kafka Events

Every new expense publishes an `EXPENSE_CREATED` event to the `expense-events` topic. In production this would trigger push notifications, email summaries, or activity feed updates for all group members.

---

## Data Model

```
users ──< group_members >── groups
                                │
                           expenses
                                │
                         expense_splits
                                │
                          settlements
```

---

## Roadmap

- [ ] Settlement recording between members
- [ ] Debt minimization algorithm (reduce number of transfers)
- [ ] Push notifications via Kafka consumers
- [ ] Monthly expense reports per group

---

## Author

**Carlos Daza** — Java Backend Engineer  
[LinkedIn](https://www.linkedin.com/in/carlos-alberto-daza-murgas-a4224a217/) · [GitHub](https://github.com/2CarlosDaza)
