# QueueLess – Smart Queue Management System

A backend application that digitizes traditional queue management by allowing customers to generate service tokens, track their queue positions, and receive service in an organized manner.

## Overview

QueueLess is a RESTful backend application designed to simplify queue management for organizations such as hospitals, clinics, colleges, banks, and service centers.

Instead of waiting in physical queues without knowing their position, customers can generate digital tokens and track their progress. Administrators can manage services, call the next customer, and update token statuses.

The system focuses on reliable queue processing, database consistency, and concurrency-safe token generation.

## Key Features

- **Service Management:** Create services, retrieve service details, and activate or deactivate services.
- **Digital Token Generation:** Generate sequential tokens for customers within each service.
- **Queue Position Tracking:** Calculate how many customers are ahead of a particular token.
- **Queue Processing:** Call the next waiting customer in the correct order.
- **Token Lifecycle Management:** Mark tokens as SERVING, COMPLETED, or SKIPPED.
- **Concurrency Control:** Use database locking and transaction management to prevent duplicate token numbers and conflicting queue operations.
- **Input Validation:** Validate incoming API requests using Jakarta Bean Validation.
- **Centralized Exception Handling:** Provide consistent error responses.
- **Automated Testing:** Include unit, integration, and concurrency tests.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 25 | Programming language |
| Spring Boot 4.1.2 | Backend application framework |
| Spring WebMVC | REST API development |
| Spring Data JPA | Database operations |
| Hibernate | ORM |
| MySQL 8 | Relational database |
| Maven | Build and dependency management |
| JUnit 5 | Unit and integration testing |
| Mockito | Mocking dependencies in unit tests |
| Postman | API testing |
| Git & GitHub | Version control |

## Application Architecture

The application follows a layered architecture to maintain separation of concerns.

```text
Client (Postman)
      |
      v
REST Controllers
      |
      v
Service Layer
      |
      v
Repository Layer
      |
      v
MySQL Database
```

### Project Structure

```text
src/
├── main/
│   ├── java/com/queueless/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── repository/
│   │   └── service/
│   │
│   └── resources/
│       └── application.properties/
│
└── test/
    └── java/com/queueless/
        ├── service/
        ├── controller/
        └── concurrency/
```

## Database Design

The application uses two primary entities:

### 1. Service

| Column | Description |
|---|---|
| id | Primary key |
| name | Service name |
| active | Service availability |
| last_token_number | Last generated token number |

### 2. QueueToken

| Column | Description |
|---|---|
| id | Primary key |
| token_number | Sequential token number |
| customer_name | Customer name |
| status | Current token status |
| created_at | Token creation timestamp |
| service_id | Foreign key referencing Service |

**Token Statuses:**

- `WAITING`
- `SERVING`
- `COMPLETED`
- `SKIPPED`

## REST API Endpoints

Base URL: `http://localhost:8081`

### Service Management

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/services` | Create a service |
| GET | `/api/services` | Retrieve all services |
| GET | `/api/services/{id}` | Retrieve a service by ID |
| PATCH | `/api/services/{id}/status?active=true` | Activate or deactivate a service |

### Token Management

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/tokens` | Generate a customer token |
| GET | `/api/tokens/waiting/{serviceId}` | Retrieve waiting tokens |
| GET | `/api/tokens/{tokenId}/position` | Check queue position |

### Queue Management

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/queues/{serviceId}/next` | Call the next waiting customer |
| GET | `/api/queues/{serviceId}/current` | Get the currently serving token |
| PUT | `/api/queues/tokens/{tokenId}/complete` | Complete a token |
| PUT | `/api/queues/tokens/{tokenId}/skip` | Skip a token |

## Sample API Requests

### 1. Create a Service

**POST** `/api/services`

Request body:

```json
{
  "name": "General Consultation"
}
```

### 2. Generate a Token

**POST** `/api/tokens`

Request body:

```json
{
  "customerName": "Ahmed",
  "serviceId": 1
}
```

Sample response:

```json
{
  "id": 1,
  "tokenNumber": 1,
  "customerName": "Ahmed",
  "status": "WAITING",
  "createdAt": "2026-11-19T10:30:00",
  "serviceId": 1,
  "serviceName": "General Consultation"
}
```

### 3. Check Queue Position

**GET** `/api/tokens/1/position`

Sample response:

```json
{
  "tokenId": 1,
  "tokenNumber": 1,
  "customerName": "Ahmed",
  "status": "WAITING",
  "peopleAhead": 0
}
```

### 4. Call the Next Customer

**POST** `/api/queues/1/next`

This endpoint selects the earliest waiting token and changes its status to `SERVING`.

## Concurrency and Transaction Management

QueueLess addresses concurrency challenges that can occur when multiple customers generate tokens or multiple operators attempt to process the same queue simultaneously.

The application uses:

- **Pessimistic Database Locking:** Prevents concurrent transactions from modifying the same service or token records simultaneously.
- **Transactional Operations:** Uses Spring's `@Transactional` to maintain consistency during queue operations.
- **Sequential Token Generation:** Maintains a separate token counter for each service.
- **Queue Validation:** Prevents another token from being called while a customer is already being served.

These mechanisms help maintain consistent queue processing under concurrent requests.

## Automated Testing

The project includes 25 automated tests:

| Test Type | Number of Tests |
|---|---:|
| Service Unit Tests | 12 |
| Controller Integration Tests | 10 |
| Concurrency Integration Tests | 3 |
| **Total** | **25** |

Testing technologies:

- JUnit 5
- Mockito
- Spring Boot Test
- MockMvc
- MySQL

The tests cover token generation, queue processing, validation, exception handling, REST API behaviour, and concurrent operations.

## Getting Started

### Prerequisites

Make sure you have installed:

- JDK 25
- MySQL 8 or compatible version
- Maven
- IntelliJ IDEA or another Java IDE
- Postman (optional, for API testing)

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/QueueLess.git
```

```bash
cd QueueLess
```

### 2. Create the Database

Log in to MySQL and execute:

```sql
CREATE DATABASE queueless_db;
```

### 3. Configure Database Credentials

Open `src/main/resources/application.properties`.

Configure your MySQL connection:

```properties
spring.application.name=QueueLess

server.port=8081

spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/queueless_db}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Set your database password using the `DB_PASSWORD` environment variable. Do not commit real database credentials to GitHub.

### 4. Run the Application

Using Maven:

```bash
./mvnw spring-boot:run
```

Or run `QueuelessApplication.java` from your IDE.

The application starts at:

```text
http://localhost:8081
```

### 5. Run Automated Tests

Execute all tests using:

```bash
./mvnw test
```

## Future Improvements

- JWT-based authentication and role-based authorization.
- WebSocket-based real-time queue updates.
- Customer-facing frontend application.
- Admin dashboard for service monitoring.
- Queue analytics and reporting.
- Docker containerization and deployment.

## Author

**YOUR NAME**

Java Backend Developer | Spring Boot | MySQL

GitHub: https://github.com/YOUR_USERNAME

---

*QueueLess – Making queues smarter, simpler, and more efficient.*
