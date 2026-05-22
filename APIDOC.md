# Buy-01 API Documentation & Tracker

Our platform utilizes **Springdoc OpenAPI** to provide live, interactive documentation for all microservices.

## 📑 Table of Contents
1. [Development Tracker](#1-development-tracker)
2. [General Information](#2-general-information)
3. [Data Transfer Objects (DTOs)](#3-data-transfer-objects-dtos)
4. [API Endpoints](#4-api-endpoints)
    - 4.1 Auth API
    - 4.2 User Management API
    - 4.3 Seller Profile API
    - 4.4 Product Management API

---

## 1. Development Tracker

| Schema Name | Description | Status |
| :--- | :--- | :--- |
| `Role` | Enum defining access levels (CLIENT, SELLER, ADMIN). | ✅ Validated |
| `InfoUserDTO` | Core user profile data. | ✅ Validated |
| `SellerProfileDTO` | Comprehensive analytics, metrics, and profile data for sellers. | ✅ Validated |
| `RegisterUserDTO` | Payload for registering a new account. | ✅ Validated |
| `LoginUserDTO` | Payload for authentication. | ✅ Validated |
| `UpdateUserDTO` | Payload for partially updating a user profile. | ✅ Validated |
| `MediaUploadResponseDTO`| Returns `fileId` and `fileUrl` upon successful upload. | ✅ Validated |
| `ClientProductDTO` | Standard product mapping for general client usage. | ✅ Validated |
| `CreateProductDTO` | Payload for initializing a new product listing. | ✅ Validated |
| `UpdateProductDTO` | Payload for modifying existing product details. | ✅ Validated |
| `ProductCardDTO` | Lightweight product schema tailored for catalog and home pages. | ✅ Validated |
| `ProductDTO` | Full product schema including media arrays and seller contact info. | ✅ Validated |
| `ProductSimpleDTO` | Lean schema specifically for internal service-to-service calls. | ✅ Validated |
| `StockAdjustmentRequest`| Payload for inventory decrement/increment operations. | ✅ Validated |
| `SellerOrderDTO` | Order details aggregated and optimized for seller dashboards. | ✅ Validated |

---

## 2. General Information

### Accessing the API
The API documentation is automatically generated and available at the following endpoints:
* **JSON Format:** `https://localhost:8443/api-docs` 
* **Interactive UI:** `https://localhost:8443/swagger-ui.html` 

### 🔒 Security & Authentication
All APIs are protected by our API Gateway. 
- **Authentication:** Most endpoints require a valid JWT token.
- **JWT Handling:** The Gateway expects a `jwt` cookie. It validates the signature and automatically propagates the following trusted headers to backend services:
    - `X-User-ID`: The unique identifier of the authenticated user.
    - `X-User-Email`: The principal email address.
    - `X-User-Role`: The user's assigned role (`CLIENT`, `SELLER`, or `ADMIN`).

### 📝 Error Contract
To ensure a consistent experience for our frontend, all microservices return standardized error responses:
```json
{
  "timestamp": "2026-05-19T18:00:00.000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid JWT token",
  "path": "/api/users/me"
}

```

---

## 3. Data Transfer Objects (DTOs)

This section defines the standard JSON structures used across the platform. Click on any DTO name from the API tables below to jump here.

### InfoUserDTO

Core user profile data returned to the frontend (passwords and sensitive data are stripped).

```json
{
  "id": "60d5ec49c54f4b238a4d2e9c",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "role": "CLIENT",
  "avatarUrl": "https://storage/..."
}

```

### SellerProfileDTO

Comprehensive analytics, metrics, and profile data for sellers.

```json
{
  "sellerId": "60d5ec49c54f4b238a4d2e9c",
  "sellerName": "Nordic Electronics",
  "shopLogoUrl": "https://storage/.../logo.png",
  "shopDescription": "High-quality hardware and components.",
  "totalRevenue": 14500.50,
  "totalSales": 145,
  "totalOrders": 150,
  "totalCustomers": 120,
  "bestSellingProductId": "prod_8832",
  "bestSellingProductName": "Mechanical Keyboard",
  "bestSellingProductCount": 42,
  "averageRating": 4.8,
  "totalReviews": 89,
  "totalFiveStarReviews": 75,
  "isVerified": true,
  "isActive": true,
  "deliveryRating": 4.9,
  "communicationRating": 4.7,
  "returnRate": 2,
  "cancellationRate": 1,
  "joinDate": "2025-01-15T10:00:00Z",
  "lastSaleDate": "2026-05-18T14:30:00Z",
  "categories": ["Electronics", "Computers", "Accessories"],
  "followerCount": 350
}

```

### RegisterUserDTO

Payload for registering a new account.

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "SecurePassword123!",
  "role": "CLIENT" 
}

```

### LoginUserDTO

Payload for authentication.

```json
{
  "email": "john.doe@example.com",
  "password": "SecurePassword123!"
}

```

### UpdateUserDTO

Payload for partially updating a user profile. All fields are optional.

```json
{
  "email": "new.email@example.com",
  "firstName": "Johnny",
  "lastName": "Doe",
  "currentPassword": "OldPassword123!", 
  "newPassword": "NewPassword456!"
}

```

### ClientProductDTO

Standard payload for representing product information to the client.

```json
{
  "name": "Mechanical Keyboard",
  "description": "High-profile mechanical keyboard with RGB.",
  "price": 129.99,
  "quantity": 50
}

```

### CreateProductDTO

Payload for initializing a new product listing.

```json
{
  "name": "Mechanical Keyboard",
  "description": "High-profile mechanical keyboard with RGB.",
  "price": 129.99,
  "quantity": 50
}

```

### ProductCardDTO

Lightweight product schema tailored for catalog and home pages, including a subset of image URLs.

```json
{
  "id": "prod_8832",
  "name": "Mechanical Keyboard",
  "description": "High-profile mechanical keyboard with RGB.",
  "price": 129.99,
  "quantity": 50,
  "createdByMe": false,
  "imageUrls": [
    "[https://storage.example.com/media/img1.png](https://storage.example.com/media/img1.png)",
    "[https://storage.example.com/media/img2.png](https://storage.example.com/media/img2.png)"
  ]
}

```

### ProductDTO

Full product schema including media arrays, ownership flags, and seller contact info.

```json
{
  "productId": "prod_8832",
  "name": "Mechanical Keyboard",
  "description": "High-profile mechanical keyboard with RGB.",
  "price": 129.99,
  "quantity": 50,
  "sellerFirstName": "Jane",
  "sellerLastName": "Smith",
  "sellerEmail": "jane.smith@example.com",
  "createdByMe": true,
  "media": [
    {
      "fileId": "media_774",
      "fileUrl": "[https://storage.example.com/media/img1.png](https://storage.example.com/media/img1.png)"
    }
  ]
}

```

### ProductSimpleDTO

Lean schema specifically for internal service-to-service calls, containing essential data without heavy media objects.

```json
{
  "productId": "prod_8832",
  "name": "Mechanical Keyboard",
  "description": "High-profile mechanical keyboard with RGB.",
  "price": 129.99,
  "quantity": 50,
  "sellerID": "60d5ec49c54f4b238a4d2e9c"
}

```

### StockAdjustmentRequest

Payload for inventory decrement/increment operations.

```json
{
  "productId": "prod_8832",
  "quantity": 2
}

```

### UpdateProductDTO

Payload for modifying existing product details. All fields are optional depending on what needs updating.

```json
{
  "name": "Mechanical Keyboard Pro",
  "description": "Updated model with silent switches.",
  "price": 139.99,
  "quantity": 45
}

```

---

## 4. API Endpoints

*(Note: All endpoints below automatically route through the API Gateway on port `8443`. Do not hit the internal service ports directly.)*

### 4.1 Authentication API

Handles user registration, login, and secure session management. Base path: `/api/auth`

| Method | Endpoint | Description | Auth Required | Request Payload | Response Type |
| --- | --- | --- | --- | --- | --- |
| **POST** | `/register` | Registers a new user. | No | `multipart/form-data` | `{"message": "string"}` |
| **POST** | `/login` | Authenticates user and issues JWT. | No | [LoginUserDTO](https://www.google.com/search?q=%23loginuserdto) | `{"message": "string"}` |
| **POST** | `/logout` | Clears the JWT session cookie. | Yes | None | `{"message": "string"}` |

### 4.2 User Management API

Endpoints for managing core user identities and account lifecycles. Base path: `/api/users`

| Method | Endpoint | Description | Auth | Role Required | Request Payload | Response Type |
| --- | --- | --- | --- | --- | --- | --- |
| **GET** | `/me` | Gets current user's profile info. | Yes | ANY | None | **[InfoUserDTO](https://www.google.com/search?q=%23infouserdto)** |
| **PUT** | `/me` | Partially updates user profile. | Yes | ANY | [UpdateUserDTO](https://www.google.com/search?q=%23updateuserdto) | `{"message": "string"}` |
| **DELETE** | `/` | Deletes account (requires `?password=`). | Yes | ANY | URL Query Params | `{"message": "string"}` |
| **DELETE** | `/avatar` | Removes the user's avatar image. | Yes | ANY | None | `{"message": "string"}` |
| **GET** | `/email` | Looks up a user by `?email=`. | Yes | **ADMIN** | URL Query Params | **[InfoUserDTO](https://www.google.com/search?q=%23infouserdto)** |

### 4.3 Seller Profile API

Endpoints dedicated to managing seller-specific metadata and shop statistics. Base path: `/api/sellers`

| Method | Endpoint | Description | Auth | Role Required | Request Payload | Response Type |
| --- | --- | --- | --- | --- | --- | --- |
| **GET** | `/profile` | Gets the authenticated seller's shop. | Yes | **SELLER** | None | **[SellerProfileDTO](https://www.google.com/search?q=%23sellerprofiledto)** |
| **PUT** | `/profile` | Updates the seller's shop data. | Yes | **SELLER** | [SellerProfileDTO](https://www.google.com/search?q=%23sellerprofiledto) | **[SellerProfileDTO](https://www.google.com/search?q=%23sellerprofiledto)** |
| **GET** | `/{id}/statistics` | Gets public stats for a specific seller. | No | None | None | **[SellerProfileDTO](https://www.google.com/search?q=%23sellerprofiledto)** |

#### 4.4 Product Management API

Endpoints dedicated to managing products, inventory, and product media.
**Base path:** `/api/products`

| Method | Endpoint | Description | Auth | Role Required | Request Payload | Response Type |
| --- | --- | --- | --- | --- | --- | --- |
| **GET** | `/all` | Retrieves a paginated list of all products in the catalog. | Optional | ANY | Query Params (Page, Size) | `Page<ProductCardDTO>` |
| **GET** | `/search` | Dynamic search & filter endpoint (keyword, price, quantity, date ranges). | Optional | ANY | Query Params (`q`, `minPrice`, `maxPrice`, etc.) | `Page<ProductCardDTO>` |
| **GET** | `/my-products` | Retrieves a paginated list of products created by the current seller. | Yes | **SELLER / ADMIN** | Query Params (Page, Size) | `Page<ProductCardDTO>` |
| **POST** | `/` | Creates a new product listing. | Yes | **SELLER / ADMIN** | `CreateProductDTO` | `Product` |
| **PUT** | `/{productId}` | Updates an existing product's details. | Yes | **SELLER / ADMIN** | `UpdateProductDTO` | `UpdateProductDTO` |
| **DELETE** | `/{productId}` | Permanently deletes a product and triggers media deletion. | Yes | **SELLER / ADMIN** | None | `String` |
| **GET** | `/{productId}` | Retrieves detailed product information (Authenticated). | Yes | ANY | None | `ProductDTO` |
| **GET** | `/public/{productId}` | Retrieves detailed product information without user context. | No | ANY | None | `ProductDTO` |
| **GET** | `/seller/{email}` | Retrieves all products associated with a specific seller email. | No | ANY | None | `List<ProductDTO>` |

### 4.4.1 Internal & Media Product APIs

These endpoints handle specific internal logic, cross-service interactions, and media proxying.

| Method | Endpoint | Description | Auth | Role Required | Request Payload | Response Type |
| --- | --- | --- | --- | --- | --- | --- |
| **POST** | `/create/images` | Uploads a single image for a specific product. | Yes | **SELLER / ADMIN** | `MultipartFile` (`file`) | `Map<String, String>` |
| **DELETE** | `/deleteMedia/{productId}/{mediaId}` | Removes a specific media asset from a product. | Yes | **SELLER / ADMIN** | None | `Map<String, String>` |
| **POST** | `/adjust-stock` | Decrements stock during order processing (Internal use). | No | INTERNAL | `List<StockAdjustmentRequest>` | `Void` (200 OK) |
| **POST** | `/restock` | Increments stock upon order cancellation (Internal use). | No | INTERNAL | `List<StockAdjustmentRequest>` | `Void` (200 OK) |
| **GET** | `/simple/{productId}` | Extremely lightweight fetch for `orders-service` verification. | No | INTERNAL | None | `ProductSimpleDTO` |
