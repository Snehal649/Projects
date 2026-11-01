# FinTrack: Smart Personal Finance Manager

Spring Boot 3 (Java 17) + React 18 (Vite, Tailwind, Recharts) + MySQL 8.

## Run it

1. Start MySQL (Docker): `docker compose up -d`
   or install MySQL locally and set DB_USER / DB_PASS env vars.
2. Backend: `cd backend && mvn spring-boot:run`  (http://localhost:8080, Swagger: /swagger-ui.html)
3. Frontend: `cd frontend && npm install && npm run dev`  (http://localhost:5173)
4. Register an account, then try the quick-add bar: `Swiggy 450`, `Paid 1200 for petrol yesterday`, `Salary 50000`.

## What is implemented

Auth (JWT, BCrypt) | accounts with live balances | transactions (CRUD, filters, paging, CSV export) |
budgets with 80%/100% alerts | goals with what-if simulator | dashboard charts |
natural-language + voice quick-add | safe-to-spend | month-end forecast | anomaly detection |
subscription detector | financial health score | privacy mode (blur amounts).

## Next steps (not included)
Recurring transactions (@Scheduled), split expenses, receipt OCR, SMS parser, PWA, refresh tokens,
Flyway migrations, Docker images for backend/frontend, tests.
