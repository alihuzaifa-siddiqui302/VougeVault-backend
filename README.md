# VougeVault

A modular, enterprise-grade e-commerce backend built with Spring Boot 3. VougeVault features automated order lifecycle state machines, Stripe payment processing with webhook verification, real-time OTP-verified delivery dispatch, customer wishlist and cart flows, business analytics, and multi-tenant role-based access control.

---

## Key Features

* **Authentication & RBAC:** Stateless JWT security with role hierarchies (`ROLE_CUSTOMER`, `ROLE_BRAND`, `ROLE_DELIVERY_PERSON`, `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`).
* **Catalog & Dynamic Inventory:** Multi-variant SKU management, category hierarchies, product media handling, and atomic inventory reservations.
* **Cart & Wishlist Engine:** Cart persistence, item quantity adjustments, personalized wishlists for saved items, and seamless cart-to-checkout transitions.
* **Deterministic State Machines:** Strict lifecycle orchestration for orders (`PENDING` → `PAID` → `SHIPPED` → `DELIVERED`) and customer return requests.
* **Payment Processing:** Stripe PaymentIntents with asynchronous webhook handling and cryptographic HMAC signature validation.
* **Last-Mile Delivery:** Geolocation tracking, automated agent assignment, and secure customer handoff via 6-digit OTP verification.
* **Business Intelligence & Analytics:** Real-time revenue reporting, platform gross merchandise value (GMV) metrics, brand-specific sales tracking, order conversion rates, and inventory velocity trends.
* **Asynchronous Notifications:** Event-driven email dispatch for order confirmations, delivery updates, and returns via SendGrid (with local Mailpit support).
* **Caching & Persistence:** High-performance Redis caching paired with PostgreSQL, versioned across 23 sequential Flyway schema migrations.

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language & Core** | Java 17+, Spring Boot 3.x |
| **Database & Caching** | PostgreSQL 16, Redis 7 (Lettuce), Spring Data JPA, Hibernate |
| **Database Migrations** | Flyway (23 Versioned Migrations) |
| **Security & Auth** | Spring Security 6, JJWT (HMAC-SHA256) |
| **Third-Party APIs** | Stripe SDK (Java), SendGrid API |
| **Local Infrastructure** | Docker, Docker Compose, Mailpit |
| **API Documentation** | SpringDoc OpenAPI 3 (Swagger UI) |
| **Testing** | JUnit 5, Mockito, Spring Boot Test (290+ unit and integration tests) |

---

## Project Structure

```text
src/main/java/com/ecommerce/VougeVault/
├── admin/               # SuperAdmin analytics and platform-wide oversight
├── analytics/           # Business intelligence, revenue metrics, and trends
├── auth/                # JWT filters, UserDetails, and authentication endpoints
├── brand/               # Brand onboarding, brand-level catalog management
├── cart/                # Customer cart management and cart item operations
├── catalog/             # Products, variants, categories, and image storage
├── delivery/            # Agent assignment, live GPS tracking, and delivery OTP handoff
├── email/               # SendGrid integration and asynchronous event listeners
├── inventory/           # Stock levels, reservations, and atomic restock flows
├── order/               # Order placement, checkout, and state machine transitions
├── payment/             # Stripe checkout and webhook signature validation
├── returnmanagement/   # Return requests, inspection, and refund state machine
├── review/              # Product ratings and customer reviews
├── shared/              # Global exceptions, Redis config, security filter chains
├── user/                # User entities, profiles, and role definitions
└── wishlist/            # Customer wishlist management and saved-item collections