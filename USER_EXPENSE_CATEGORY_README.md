# UserExpenseCategory Service

## Overview
This service manages per-user expense categories, allowing each user to have their own customizable set of expense categories (up to 20 categories per user).

## Features
- **User-specific categories**: Each user has their own set of expense categories
- **Auto-initialization**: When a new user is created, master expense categories are automatically copied to their account
- **20 category limit**: Each user can have a maximum of 20 expense categories
- **Caching**: Uses Spring Cache for optimized performance
- **Transaction support**: All write operations are transactional

## Database Table
The `user_expense_category` table must be created manually in your MySQL database. Use the provided SQL script:

```bash
mysql -u pi -p expense_tracker < src/main/resources/db/create_user_expense_category_table.sql
```

### Table Schema
```sql
CREATE TABLE user_expense_category (
  user_expense_category_id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) NOT NULL,
  user_expense_category_name VARCHAR(255) NOT NULL,
  last_update_tmstp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(1) NOT NULL DEFAULT 'A',
  CONSTRAINT uq_user_category UNIQUE (username, user_expense_category_name),
  INDEX idx_username (username)
);
```

## API Endpoints

### 1. Get All Categories for a User
**GET** `/api/user-expense-category/{username}`

**Response:**
```json
[
  {
    "userExpenseCategoryId": 1,
    "username": "john_doe",
    "userExpenseCategoryName": "Food",
    "lastUpdateTmstp": "2025-11-12T17:00:00",
    "status": "A"
  }
]
```

### 2. Add a New Category
**POST** `/api/user-expense-category/{username}`

**Request Body:**
```json
{
  "userExpenseCategoryName": "Transportation",
  "status": "A"
}
```

**Response:**
```json
{
  "userExpenseCategoryId": 2,
  "username": "john_doe",
  "userExpenseCategoryName": "Transportation",
  "lastUpdateTmstp": "2025-11-12T17:00:00",
  "status": "A"
}
```

**Error Response (when limit reached):**
```json
{
  "error": "User may have at most 20 categories"
}
```

### 3. Update a Category
**PUT** `/api/user-expense-category/{username}/{id}`

**Request Body:**
```json
{
  "userExpenseCategoryName": "Public Transport",
  "status": "A"
}
```

**Response:**
```json
{
  "userExpenseCategoryId": 2,
  "username": "john_doe",
  "userExpenseCategoryName": "Public Transport",
  "lastUpdateTmstp": "2025-11-12T17:05:00",
  "status": "A"
}
```

### 4. Delete a Category
**DELETE** `/api/user-expense-category/{username}/{id}`

**Response:**
```json
{
  "status": "success"
}
```

### 5. Delete All Categories for a User
**DELETE** `/api/user-expense-category/{username}`

**Response:**
```json
{
  "status": "success"
}
```

### 6. Copy Master Categories
**POST** `/api/user-expense-category/{username}/copy-master`

This endpoint:
1. Deletes all existing categories for the user
2. Copies up to 20 master expense categories from the `expense_category` table
3. Sets status to 'A' (active) for all copied categories

**Response:**
```json
{
  "status": "success"
}
```

## Automatic Behaviors

### 1. New User Creation
When a new user is created via `POST /api/user`, the system automatically:
- Copies all master expense categories to the new user's account
- Limits to 20 categories if there are more master categories
- Sets status to 'A' for all categories

### 2. User Deletion
When a user is deleted via `DELETE /api/user/{userId}`, the system automatically:
- Deletes all user expense categories
- Deletes all expenses
- Deletes all incomes
- Finally deletes the user record

## Caching
The service uses Spring Cache with the following behavior:
- **Cache Name**: `userExpenseCategories`
- **Cache Key**: username
- **Cached Operation**: `findAll(username)`
- **Cache Eviction**: Automatic on all write operations (add, update, delete, deleteAll, copy-master)

## Business Rules
1. **Maximum 20 categories per user**: Enforced on add and copy-master operations
2. **Unique category names per user**: Database constraint ensures no duplicate category names for the same user
3. **Status field**: Single character field where 'A' means active
4. **Timestamps**: Automatically updated on create and update operations

## Integration Points

### With UserController
- `createOrUpdateUser()`: Calls `onUserCreated(username)` for new users
- `deleteUser()`: Calls `deleteAll(username)` before deleting the user

### With ExpenseCategoryService
- Retrieves master expense categories for copying to user accounts

## Error Handling
- **400 Bad Request**: Invalid input, missing required fields, or limit violations
- **404 Not Found**: Category not found for the given username and ID
- **500 Internal Server Error**: Unexpected server errors

## Status Codes
- `A` - Active (default)
- Other status codes can be defined as needed

## Example Usage Flow

### Creating a New User
```bash
# 1. Create a user (automatically copies master categories)
curl -X POST http://localhost:8080/api/user \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","email":"john@example.com","password":"Password123!"}'

# 2. View auto-copied categories
curl http://localhost:8080/api/user-expense-category/john_doe
```

### Managing Categories
```bash
# Add a custom category
curl -X POST http://localhost:8080/api/user-expense-category/john_doe \
  -H "Content-Type: application/json" \
  -d '{"userExpenseCategoryName":"Entertainment","status":"A"}'

# Update a category
curl -X PUT http://localhost:8080/api/user-expense-category/john_doe/5 \
  -H "Content-Type: application/json" \
  -d '{"userExpenseCategoryName":"Entertainment & Fun","status":"A"}'

# Delete a category
curl -X DELETE http://localhost:8080/api/user-expense-category/john_doe/5

# Reset to master categories
curl -X POST http://localhost:8080/api/user-expense-category/john_doe/copy-master
```

## Files Created
- `model/UserExpenseCategory.java` - Entity model
- `repository/UserExpenseCategoryRepository.java` - JPA repository
- `dto/UserExpenseCategoryRequest.java` - Request DTO
- `dto/UserExpenseCategoryResponse.java` - Response DTO
- `service/UserExpenseCategoryService.java` - Business logic
- `controller/UserExpenseCategoryController.java` - REST API
- `resources/db/create_user_expense_category_table.sql` - Database schema

## Files Modified
- `config/CacheConfig.java` - Added "userExpenseCategories" cache
- `controller/UserController.java` - Integrated user expense category lifecycle
