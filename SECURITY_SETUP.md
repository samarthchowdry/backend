# Role-Based Authentication & Security Configuration

This document explains the role-based authentication system implemented using Spring Security and JWT tokens. The system is designed to be flexible for both client and organization deployments.

## Features

- **JWT Token-Based Authentication**: Stateless authentication using JSON Web Tokens
- **Role-Based Access Control (RBAC)**: Three roles - ADMIN, TEACHER, STUDENT
- **Google OAuth Integration**: Seamless integration with existing Google OAuth flow
- **Flexible Configuration**: Environment-based configuration for different deployment scenarios
- **CORS Support**: Configurable CORS settings for client/organization use

## Architecture

### Components

1. **JwtTokenProvider**: Generates and validates JWT tokens
2. **JwtAuthenticationFilter**: Filters incoming requests and extracts JWT tokens
3. **SecurityConfiguration**: Main security configuration with role-based access rules
4. **CustomUserDetailsService**: Loads user details from database for authentication
5. **SecurityUtils**: Utility class for accessing current user information

## Configuration

### Application Properties

Add these to your `application.yml` or environment variables:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}  # Change in production!
    expiration: ${JWT_EXPIRATION:86400000}  # 24 hours
    issuer: ${JWT_ISSUER:student-management-system}
  security:
    allowed-origins: ${ALLOWED_ORIGINS:http://localhost:4200}
    enable-csrf: ${ENABLE_CSRF:false}
    permit-all-paths: ${PERMIT_ALL_PATHS:/api/auth/**,/api/public/**}
```

### Environment Variables

For production deployment:

```bash
JWT_SECRET=your-secret-key-minimum-64-characters-long
JWT_EXPIRATION=86400000
JWT_ISSUER=your-organization-name
ALLOWED_ORIGINS=https://your-client-domain.com,https://your-org-domain.com
ENABLE_CSRF=true
```

## Authentication Flow

### 1. Google OAuth Login

```http
POST /api/auth/google
Content-Type: application/json

{
  "idToken": "google-id-token-here"
}
```

**Response:**
```json
{
  "token": "jwt-token-here",
  "email": "user@example.com",
  "name": "User Name",
  "role": "STUDENT",
  "userId": 123,
  "pictureUrl": "https://...",
  "emailVerified": true,
  "googleSub": "google-sub-id",
  "lastLoginAt": "2024-01-01T12:00:00"
}
```

### 2. Using JWT Token

Include the JWT token in requests using one of these methods:

**Option 1: Authorization Header (Standard)**
```http
Authorization: Bearer <jwt-token>
```

**Option 2: X-Auth-Token Header (Flexible)**
```http
X-Auth-Token: <jwt-token>
```

## Role-Based Access Control

### Endpoint Protection

- **Public Endpoints** (No authentication required):
  - `/api/auth/**` - Authentication endpoints
  - `/api/public/**` - Public API endpoints
  - `/error` - Error handling
  - `/actuator/health` - Health check

- **Admin Only** (`ROLE_ADMIN` required):
  - All `/api/admin/**` endpoints

- **Student/Teacher/Admin** (`ROLE_STUDENT`, `ROLE_TEACHER`, or `ROLE_ADMIN`):
  - `/api/students/**` - Student management
  - `/api/marks/**` - Marks management
  - `/api/courses/**` - Course management

- **Teacher/Admin** (`ROLE_TEACHER` or `ROLE_ADMIN`):
  - `/api/reports/**` - Report generation

### Method-Level Security

Use `@PreAuthorize` annotation for fine-grained control:

```java
@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() {
    // Only admins can access
}

@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public void adminOrTeacherMethod() {
    // Admins or teachers can access
}
```

## Using SecurityUtils

Access current user information in your services:

```java
import com.studentdetails.details.Security.SecurityUtils;

// Get current user email
Optional<String> email = SecurityUtils.getCurrentUserEmail();

// Get current user role
Optional<UserRole> role = SecurityUtils.getCurrentUserRole();

// Check if user is admin
if (SecurityUtils.isAdmin()) {
    // Admin-only logic
}

// Check if user has specific role
if (SecurityUtils.hasRole(UserRole.TEACHER)) {
    // Teacher-specific logic
}
```

## Client/Organization Deployment

### For Client Deployment

1. Set `ALLOWED_ORIGINS` to your client domain(s)
2. Set `ENABLE_CSRF=false` for API-only deployments
3. Configure JWT secret and expiration
4. Use `Authorization: Bearer <token>` header

### For Organization Deployment

1. Set `ALLOWED_ORIGINS` to organization domains
2. Set `ENABLE_CSRF=true` for web applications
3. Use strong JWT secret (minimum 64 characters)
4. Configure shorter token expiration for security
5. Consider implementing token refresh mechanism

## Security Best Practices

1. **JWT Secret**: Use a strong, randomly generated secret (minimum 64 characters)
2. **Token Expiration**: Set appropriate expiration based on your security requirements
3. **HTTPS**: Always use HTTPS in production
4. **CORS**: Restrict allowed origins to known domains
5. **CSRF**: Enable CSRF protection for web applications
6. **Token Storage**: Store tokens securely on client side (httpOnly cookies or secure storage)

## Migration from Header-Based Auth

The system maintains backward compatibility with the existing `X-Role` header approach while adding JWT support. Existing endpoints will continue to work, but new requests should use JWT tokens for better security.

## Troubleshooting

### Token Expired
- Check token expiration time
- Implement token refresh mechanism
- Re-authenticate user

### Access Denied
- Verify user has required role
- Check endpoint security configuration
- Ensure token is valid and not expired

### CORS Issues
- Verify `ALLOWED_ORIGINS` includes your domain
- Check CORS configuration in `SecurityConfiguration`
- Ensure credentials are allowed if needed

## Example Frontend Integration

```typescript
// Store token after login
localStorage.setItem('jwt_token', response.token);

// Include in requests
const headers = {
  'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`,
  'Content-Type': 'application/json'
};

// Or use X-Auth-Token header
const headers = {
  'X-Auth-Token': localStorage.getItem('jwt_token'),
  'Content-Type': 'application/json'
};
```






