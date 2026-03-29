# Product Catalog Manager

REST API for managing a product catalog with support for multiple producers, dynamic product attributes, and flexible filtering.

## Tech Stack

- Java 21
- Spring Boot 3.5
- Spring Data JPA + Hibernate
- PostgreSQL
- H2 (for tests)
- Liquibase
- MapStruct
- Lombok
- Springdoc OpenAPI (Swagger)

---

## Requirements

- Java 21+
- Docker (for PostgreSQL)
- Maven 3.9+

---

## Quick Start

### 1. Start PostgreSQL

```bash
docker run -d \
  --name catalog_postgres \
  -e POSTGRES_DB=catalog \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16
```

### 2. Run the application
```bash
mvn package -DskipTests
java -jar target/catalog_manager-0.0.1-SNAPSHOT.jar
```

### 3. Open Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

---

## Bootstrap Data

The application loads sample data automatically on startup (via Liquibase `dev` context).

Sample producers: **Samsung**, **LG**, **Bosch**, **Apple**

Sample categories: **TV**, **Smartphone**, **Laptop**, **Washing Machine**

Sample products: Samsung QLED 55, Samsung Neo QLED 65, Samsung Galaxy S24, LG OLED C3 55, iPhone 15 Pro, MacBook Pro 14, Bosch Serie 6

---

## API Endpoints

### Producers

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/producers` | Create producer |
| GET | `/api/producers` | List producers (pagination, filtering, sorting) |
| GET | `/api/producers/{id}` | Get producer by ID |
| PATCH | `/api/producers/{id}` | Update producer |
| DELETE | `/api/producers/{id}` | Delete producer |

**GET /api/producers — query params:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `country` | String | - | Filter by country (case-insensitive) |
| `page` | int | 0 | Page number (0-based) |
| `size` | int | 20 | Page size (max 100) |
| `sortBy` | NAME / COUNTRY / CREATED_AT | NAME | Sort field |
| `sortDir` | ASC / DESC | ASC | Sort direction |

**DELETE /api/producers/{id} — query params:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `force` | boolean | false | If true, soft-deletes all products of this producer |

---

### Products

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/products` | Create product |
| GET | `/api/products` | List products (pagination, filtering, sorting) |
| GET | `/api/products/{id}` | Get product by ID |
| PATCH | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |

**GET /api/products — query params:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `name` | String | - | Filter by name (case-insensitive, partial match) |
| `producerId` | UUID | - | Filter by producer |
| `categoryId` | UUID | - | Filter by category |
| `minPrice` | BigDecimal | - | Minimum price |
| `maxPrice` | BigDecimal | - | Maximum price |
| `currency` | PLN / EUR / USD / GBP | - | Filter by currency |
| `attributes` | String | - | Filter by attribute value (e.g. `{"color" : "white", "capacity" : "9kg"}) |
| `page` | int | 0 | Page number (0-based) |
| `size` | int | 20 | Page size (max 100) |
| `sortBy` | NAME / PRICE / CREATED_AT / UPDATED_AT | NAME | Sort field |
| `sortDir` | ASC / DESC | ASC | Sort direction |

**Example requests:**

```
# All Samsung TVs sorted by price ascending
GET /api/products?producerId=a0000000-0000-0000-0000-000000000001&categoryId=b0000000-0000-0000-0000-000000000001&sortBy=PRICE&sortDir=ASC

# Products with black color
GET /api/products?attributes={"color":"black"}

# Products in price range
GET /api/products?minPrice=1000&maxPrice=5000&currency=PLN
```

---

### Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/categories` | Create category |
| GET | `/api/categories` | List categories |
| GET | `/api/categories/{id}` | Get category by ID |
| PATCH | `/api/categories/{id}` | Update category |
| DELETE | `/api/categories/{id}` | Delete category |

**GET /api/categories — query params:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | int | 0 | Page number (0-based) |
| `size` | int | 20 | Page size (max 100) |
| `sortBy` | NAME / CREATED_AT / UPDATED_AT | NAME | Sort field |
| `sortDir` | ASC / DESC | ASC | Sort direction |

**DELETE /api/categories/{id} — query params:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `force` | boolean | false | If true, soft-deletes all products in this category |

---

## Design Decisions

### Dynamic Product Attributes

Products have varying numbers of attributes depending on their type (a TV has `screen_size`, `resolution`; a smartphone has `battery_capacity`, `storage`). This is handled with a hybrid schema:

- `products` table stores fixed, always-present fields: `name`, `price`, `currency`, `producer_id`, `category_id`
- `product_attributes` table stores a `JSONB` column with all dynamic attributes

This allows flexible, schema-less attributes while keeping common fields indexed and queryable. A GIN index on the `attributes` column enables efficient JSON containment queries (`@>`).

### Attribute Validation

Each category defines `required_attributes` (e.g. TV requires `screen_size` and `resolution`). When creating or updating a product, the API validates that all required attributes are present and non-null. This enforces data quality without a rigid schema.

### Soft Delete

Producers, products, and categories are never physically deleted. Instead, `deleted_at` is set to the current timestamp. All JPA queries automatically filter out soft-deleted records via Hibernate's `@SQLRestriction("deleted_at IS NULL")`.

When deleting a producer or category:
- Without `force=true` — returns `409 Conflict` if there are active products
- With `force=true` — soft-deletes all related products first, then the producer/category

### Pagination

All list endpoints return a `PageResponse<T>` with:

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "last": false
}
```

Pages are 0-based (page=0 is the first page).

### Currency

Stored as a `VARCHAR(3)` enum (`PLN`, `EUR`, `USD`, `GBP`, `CHF`). Prices use `DECIMAL(19,4)` with `BigDecimal` in Java to avoid floating-point precision issues.

---

## Known Limitations

- Product name search uses `LIKE '%query%'` which performs a Sequential Scan. For production use with large datasets, consider replacing with PostgreSQL full-text search using a GIN index on `tsvector`.
- Attribute filtering uses PostgreSQL's `@>` JSONB operator and is not compatible with H2 in-memory database. Therefore manual tests were conducted.

---

## Database Schema

```
producers
├── id (UUID, PK)
├── name (VARCHAR 100, unique)
├── country (VARCHAR 50)
├── email (VARCHAR 100, unique)
├── created_at, updated_at, deleted_at

product_categories
├── id (UUID, PK)
├── name (VARCHAR 50, unique)
├── required_attributes (JSONB)
├── created_at, updated_at, deleted_at

products
├── id (UUID, PK)
├── producer_id (FK → producers)
├── category_id (FK → product_categories)
├── name (VARCHAR 255)
├── description (VARCHAR 255)
├── price (DECIMAL 19,4)
├── currency (VARCHAR 3)
├── created_at, updated_at, deleted_at

product_attributes
├── id (UUID, PK)
├── product_id (FK → products, unique)
├── attributes (JSONB)
└── updated_at
```

**Indexes:**
- `idx_product_producer_id` — fast filtering by producer
- `idx_product_category_id` — fast filtering by category
- `idx_product_attributes_gin` — GIN index for JSONB attribute queries
