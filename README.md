# RevPay

[![Java](https://img.shields.io/badge/Java-17-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-6DB33F)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-Frontend-DD0031)](https://angular.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8-4479A1)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-Internal-lightgrey)](#license)

RevPay is a full-stack digital payments platform for personal and business users.  
It supports wallet transfers, money requests, transaction PIN authorization, business workflows, and notification management.

---

## Features

- JWT-based authentication and role-based access
- Wallet operations: balance, add funds, withdraw, send money
- Money request lifecycle: create, receive, accept, reject
- Transaction PIN verification (4-digit) for sensitive actions
- Payment method management (cards, business methods)
- Notifications:
  - Requests
  - Transactions
  - Alerts
  - Unread count
  - Mark read/all read
  - User preferences
- Business modules: profile verification, invoices, loans

---

## Screenshots

Add screenshots in a `/docs/images` folder and link them here.

- Login  
  `![Login](docs/images/login.png)`
- Dashboard  
  `![Dashboard](docs/images/dashboard.png)`
- Money Requests  
  `![Money Requests](docs/images/money-requests.png)`
- Notifications  
  `![Notifications](docs/images/notifications.png)`

---

## Tech Stack

- Frontend: Angular, TypeScript, RxJS
- Backend: Spring Boot 3, Spring Security, Spring Data JPA
- Database: MySQL
- Build: Maven, npm

---

## Repository Structure

```text
revpay-ph2/
├── backend/   # Spring Boot API
└── frontend/  # Angular app
```

---

## Getting Started

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- npm 9+
- MySQL 8+

## Backend Config

File: `backend/src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/revpay_dev?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
server.port=8080
```

## Frontend Config

File: `frontend/src/environments/environment.ts`

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'
};
```

---

## Run Locally

## 1) Start backend

```bash
cd backend
mvn spring-boot:run
```

Backend URL: `http://localhost:8080`

## 2) Start frontend

```bash
cd frontend
npm install
npm start
```

Frontend URL: `http://localhost:4200`

---

## API Overview

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/recovery/questions`
- `POST /api/auth/recovery/reset`

### Security

- `POST /api/v1/security/pin/setup`
- `POST /api/v1/security/pin/verify`
- `POST /api/v1/security/pin/reset`

### Money Requests

- `POST /api/v1/requests/create`
- `GET /api/v1/requests`
- `POST /api/v1/requests/{requestId}/respond`

### Notifications

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/unread-count`
- `PATCH /api/v1/notifications/{notificationId}/read`
- `PATCH /api/v1/notifications/read-all`
- `GET /api/v1/notifications/preferences`
- `PUT /api/v1/notifications/preferences`

---

## Sample API Payloads

### Login

`POST /api/auth/login`

Request:
```json
{
  "email": "abc@gmail.com",
  "password": "Secret@123"
}
```

Response:
```json
{
  "token": "jwt-token-value",
  "username": "abc",
  "role": "PERSONAL"
}
```

### Create Money Request

`POST /api/v1/requests/create`

Request:
```json
{
  "payerUsername": "xyz@gmail.com",
  "amount": 234.00,
  "note": "Books"
}
```

Response:
```json
{
  "message": "Money request sent successfully",
  "requestId": 42
}
```

### Respond to Money Request

`POST /api/v1/requests/42/respond`

Request:
```json
{
  "accept": true
}
```

Response:
```json
{
  "message": "Request accepted"
}
```

### Verify PIN

`POST /api/v1/security/pin/verify`

Request:
```json
{
  "pin": "1234"
}
```

Response:
```json
{
  "valid": true
}
```

### Notifications List

`GET /api/v1/notifications?category=REQUESTS&unreadOnly=false`

Response:
```json
[
  {
    "id": 101,
    "category": "REQUESTS",
    "type": "MONEY_REQUEST_RECEIVED",
    "title": "Money requested from you",
    "message": "Money requested from abc@gmail.com for $234.00",
    "amount": 234.00,
    "counterparty": "abc@gmail.com",
    "status": "PENDING",
    "navigationTarget": "/requests/42",
    "eventTime": "2026-02-27T10:30:00",
    "read": false,
    "createdAt": "2026-02-27T10:30:00"
  }
]
```

---

## Build and Checks

Backend compile:
```bash
cd backend
mvn -DskipTests compile
```

Frontend type check:
```bash
cd frontend
npx tsc -p tsconfig.app.json --noEmit
```

Frontend build:
```bash
cd frontend
npm run build
```

---

## Troubleshooting

- PIN always incorrect:
  - Use exactly 4 digits
  - Reset PIN from Security Settings if needed
- Notifications not visible:
  - Ensure backend is running
  - Confirm `notifications` and `notification_preferences` tables exist
- Auth issues:
  - Verify JWT token is present in browser storage
  - Check backend logs for 401/403 paths

---

## Roadmap

- Backend-side mandatory PIN enforcement in money request acceptance API
- Automated integration tests for request/notification flows
- Docker setup for one-command local startup

---

## License

Internal / educational project.
