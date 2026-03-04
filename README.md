# ExpenseAPI

A simple expense-tracking application built with **Spring Boot** (Java 17) and a **Vaadin** UI.

Users can add/edit/delete expenses, upload receipt images, and have fields auto-filled via a receipt-parsing service (Anthropic Claude).  
Data is stored in a PostgreSQL database; receipts are saved on disk.

---

## Features

- Vaadin-based single-page UI for managing expenses.
- REST API under `/api/expenses` for programmatic access.
- Receipt upload/download support.
- Receipt scanning via external Claude API.
- JPA/Hibernate persistence with PostgreSQL.
- Environment configuration via `.env` and `spring-dotenv`.

---

## Technologies

- Java 17, Maven
- Spring Boot 3.5, Spring Data JPA
- Vaadin 24 (frontend assets generated under `frontend/generated` – now gitignored)
- PostgreSQL driver (`org.postgresql:postgresql`)
- HikariCP connection pool
- Lombok, commons-io, jackson-databind
- Vaadin dev mode for live frontend updates

---

## Getting Started

### Prerequisites

- JDK 17+
- Maven (included via `./mvnw`)
- PostgreSQL running locally (or adjust `DB_URL` accordingly)

### Clone & Prepare

```bash
git clone <repo-url>
cd expenseAPI
```

### Environment

Create a `.env` file at project root (tracked by `spring-dotenv`):

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/expense_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# where receipts are stored (must be writable)
RECEIPT_UPLOAD_DIR=./receipts

ANTHROPIC_API_KEY=your_api_key_here
```

> **Note:** `.gitignore` includes `frontend/generated/` and should also include `.env`  
> to keep credentials out of version control.

### Database

Create the database (example with `psql`):

```sql
CREATE DATABASE expense_db;
```

Spring Boot will auto-create/update tables (`spring.jpa.hibernate.ddl-auto=update`).

### Build

```bash
./mvnw clean install
```

### Run

```bash
./mvnw spring-boot:run
```

Application starts on port `8080` (change with `server.port` in `application.properties` or via env).

Visit **http://localhost:8080** to open the Vaadin UI.

API endpoints:

- `GET /api/expenses` – list
- `POST /api/expenses` – create
- `PUT /api/expenses/{id}` – update
- `DELETE /api/expenses/{id}` – delete
- `POST /api/expenses/{id}/receipt` – upload receipt file
- `GET /api/expenses/{id}/receipt` – download receipt

### Frontend Files

Vaadin generates client-side JS/TS under `frontend/generated/`.  
These files are now **ignored** by Git; they are built automatically when you run the app.

If you ever need to regenerate them:

```bash
./mvnw vaadin:build
```

### Testing

You can run tests with:

```bash
./mvnw test
```

(Current examples include basic repository tests; the project currently skips tests by default when running.)

---

## Project Structure

```
src/
  main/
    java/com/example/expenseapi
      controller/        REST endpoints
      model/             JPA entities
      service/           business logic, storage & scanning
      ui/                Vaadin view
    resources/application.properties
frontend/               Vaadin frontend (generated code ignored)
.env                    environment variables
pom.xml
```

### Receipt Storage

`ReceiptStorageService` constructs a directory from `${app.receipt.upload-dir}` and provides `store`, `load`, `delete`.  
Default path is `./receipts`.

### Receipt Scanning

`ReceiptExtractorService` calls the Anthropic Claude API with an image to extract title/amount/category.

---

## Deployment Notes

- Ensure `RECEIPT_UPLOAD_DIR` is writable by the container.
- Provide real `ANTHROPIC_API_KEY` via environment or Kubernetes secret.
- PostgreSQL connection details must be set in environment before startup.

---

## Troubleshooting

- **Port in use**: change `server.port` or kill the existing process.
- **Driver class not found**: make sure PostgreSQL dependency is present in `pom.xml`.
- **Uploads failing**: ensure `RECEIPT_UPLOAD_DIR` points to a writable location.

---

Happy coding!  
_Feedback and contributions are welcome._
