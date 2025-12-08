# User Management System - Admin Feature

## Overview

A secure admin-only feature has been added to allow administrators to create new teachers and administrators directly from the application interface.

## Features

✅ **Create Users**: Admins can create new ADMIN or TEACHER users
✅ **Secure Password Storage**: Passwords are hashed using BCrypt
✅ **Role-Based Access**: Only ADMIN users can access this feature
✅ **Form Validation**: Client-side and server-side validation
✅ **User-Friendly Interface**: Clean, intuitive form design

## Access

### URL
- **Frontend**: `http://localhost:4200/user-management`
- **Backend API**: `POST http://localhost:8080/api/auth/admin/users`

### How to Access
1. Login as an ADMIN user
2. Navigate to the Overview/Dashboard page
3. Click on "Manage Users" in the Quick Access section
4. Or directly visit `/user-management`

## Backend Implementation

### Endpoint
```
POST /api/auth/admin/users
```

### Security
- Protected with `@PreAuthorize("hasRole('ADMIN')")`
- Requires valid JWT token in Authorization header
- Password is automatically hashed using BCrypt

### Request Body
```json
{
  "email": "teacher@example.com",
  "password": "securePassword123",
  "role": "TEACHER",
  "fullName": "John Doe"
}
```

### Response (Success - 201 Created)
```json
{
  "email": "teacher@example.com",
  "fullName": "John Doe",
  "role": "TEACHER",
  "userId": 123,
  "message": "User created successfully"
}
```

### Response (Error - 400 Bad Request)
```json
{
  "message": "Email is required"
}
```

### Response (Error - 409 Conflict)
```json
{
  "message": "User with this email already exists"
}
```

## Frontend Implementation

### Component Location
- `loginpage/loginpageapp/src/app/user-management/`

### Files
- `user-management.component.ts` - Component logic
- `user-management.component.html` - Template
- `user-management.component.css` - Styles

### Features
- Form validation (email, password length, role selection)
- Success/error message display
- Loading states during submission
- Admin-only access check
- Responsive design

## Usage

### Creating a New Teacher

1. Fill in the form:
   - **Email**: `teacher@example.com` (will be used as username)
   - **Password**: `securePassword123` (minimum 6 characters)
   - **Role**: Select "Teacher"
   - **Full Name**: `John Doe` (optional)

2. Click "Create User"

3. The new teacher can now login using:
   - Email: `teacher@example.com`
   - Password: `securePassword123`

### Creating a New Administrator

1. Fill in the form:
   - **Email**: `admin2@example.com`
   - **Password**: `adminPassword123`
   - **Role**: Select "Administrator"
   - **Full Name**: `Jane Admin` (optional)

2. Click "Create User"

3. The new admin can now login using:
   - Email: `admin2@example.com`
   - Password: `adminPassword123`

## Security Features

### Password Security
- Passwords are hashed using BCrypt (industry standard)
- Minimum 6 characters required
- Passwords are never stored in plain text

### Access Control
- Only ADMIN role can access this feature
- Frontend checks admin status before showing form
- Backend validates admin role via JWT token
- Method-level security with `@PreAuthorize`

### Validation
- Email format validation
- Password length validation (minimum 6 characters)
- Role validation (only ADMIN or TEACHER allowed)
- Duplicate email check

## Database

### Table: `login_info`

The created users are stored in the `login_info` table with:
- `email`: User's email (unique, used as username)
- `password`: BCrypt hashed password
- `role`: ADMIN or TEACHER
- `full_name`: User's full name (optional)
- `google_sub`: Auto-generated unique identifier
- `last_login_at`: Current timestamp
- `is_project_admin`: true for ADMIN, false for TEACHER

## Important Notes

⚠️ **Security Considerations**:
- Only create users you trust
- Use strong passwords (recommended: 8+ characters, mixed case, numbers, symbols)
- Regularly review created users
- Remove unnecessary admin accounts

⚠️ **Limitations**:
- Cannot create STUDENT users via this form (students sign up via Google)
- Email addresses must be unique
- Cannot edit or delete users via this interface (use database directly)

⚠️ **Password Requirements**:
- Minimum 6 characters
- No maximum length
- Recommended: 8+ characters with complexity

## Troubleshooting

### "Access denied" Error
- Ensure you're logged in as an ADMIN user
- Check that your JWT token is valid
- Verify your role in the database

### "User with this email already exists"
- The email is already registered
- Use a different email address
- Or update the existing user's password/role in the database

### "Password must be at least 6 characters"
- Increase password length
- Ensure password field is not empty

### "Failed to create user"
- Check backend logs for detailed error
- Verify database connection
- Ensure all required fields are provided

## API Integration

### Frontend Service Call
```typescript
const headers = authRoleService.createRoleHeaders({
  'Content-Type': 'application/json'
});

const response = await fetch('http://localhost:8080/api/auth/admin/users', {
  method: 'POST',
  headers: headers,
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'password123',
    role: 'TEACHER',
    fullName: 'User Name'
  })
});
```

## Future Enhancements

Potential improvements:
- Edit existing users
- Delete users
- List all users
- Reset passwords
- Bulk user creation
- User activity logs

## Related Files

### Backend
- `details/src/main/java/com/studentdetails/details/Resources/AuthController.java`
- `details/src/main/java/com/studentdetails/details/Service/AuthService.java`
- `details/src/main/java/com/studentdetails/details/Service/ServiceImpl/AuthServiceImpl.java`
- `details/src/main/java/com/studentdetails/details/Security/SecurityConfiguration.java`

### Frontend
- `loginpage/loginpageapp/src/app/user-management/user-management.component.ts`
- `loginpage/loginpageapp/src/app/user-management/user-management.component.html`
- `loginpage/loginpageapp/src/app/user-management/user-management.component.css`
- `loginpage/loginpageapp/src/app/overview/overview.component.ts`
- `loginpage/loginpageapp/src/app/app.routes.ts`





