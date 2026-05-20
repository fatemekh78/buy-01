# Buy-01 API Documentation & Tracker

Our platform utilizes **Springdoc OpenAPI** to provide live, interactive documentation for all microservices.

## 📑 Table of Contents
1. [Development Tracker](#1-development-tracker)
2. [General Information](#2-general-information)
3. [Data Transfer Objects (DTOs)](#3-data-transfer-objects-dtos)
4. [API Endpoints](#4-api-endpoints)

---

## 1. Development Tracker

### Defined Schemas (Models)
| Schema Name | Description | Status |
| :--- | :--- | :--- |
| `Role` | Enum defining access levels (CLIENT, SELLER, ADMIN). | ✅ Validated |
| `InfoUserDTO` | Core user profile data. | ✅ Validated |
| `SellerProfileDTO` | Comprehensive analytics, metrics, and profile data for sellers. | ✅ Validated |
| `RegisterUserDTO` | Payload for registering a new account. | ✅ Validated |
| `LoginUserDTO` | Payload for authentication. | ✅ Validated |
| `UpdateUserDTO` | Payload for partially updating a user profile. | ✅ Validated |
| `MediaUploadResponseDTO`| Returns `fileId` and `fileUrl` upon successful upload. | ⏳ Pending |

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
