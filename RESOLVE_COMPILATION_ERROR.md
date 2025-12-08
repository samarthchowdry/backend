# How to Resolve Spring Security Compilation Error

## Error Message
```
java: package org.springframework.security.core does not exist
```

## Root Cause
Maven hasn't downloaded the Spring Security dependency yet. The `pom.xml` is correct, but the IDE needs to download the JAR files.

## Solution: IntelliJ IDEA (Easiest Method)

### Step 1: Reload Maven Project
1. **Right-click** on `pom.xml` in the Project tree (left sidebar)
2. Select **"Maven"** → **"Reload Project"**
3. Wait for the download to complete (watch bottom status bar for "Maven: downloading...")

### Step 2: If Step 1 Doesn't Work
1. Open **Maven** tool window:
   - View → Tool Windows → Maven
   - OR click the Maven icon in the right sidebar
2. Click the **"Reload All Maven Projects"** button (circular arrow icon at the top)
3. Wait for dependencies to download

### Step 3: Invalidate Caches (If Still Not Working)
1. Go to **File** → **Invalidate Caches...**
2. Check **"Clear file system cache and Local History"**
3. Click **"Invalidate and Restart"**
4. After restart, right-click `pom.xml` → **Maven** → **Reload Project**

## Alternative: Use IntelliJ's Maven

If Maven command line isn't available:

1. Go to **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven**
2. Make sure **"Maven home path"** is set (IntelliJ bundles Maven)
3. Click **"Apply"** and **"OK"**
4. Right-click `pom.xml` → **Maven** → **Reload Project**

## Verify It Worked

After reloading, check:
1. The compilation error should disappear
2. In Project Structure → Libraries, you should see `spring-boot-starter-security`
3. You can import Spring Security classes without errors

## Quick Checklist

- [ ] Right-clicked `pom.xml` → Maven → Reload Project
- [ ] Waited for Maven to finish downloading (status bar)
- [ ] Checked that error is gone
- [ ] If error persists, invalidated caches and restarted IDE

## The pom.xml is Correct

Your `pom.xml` already has the correct dependency (lines 117-121):
```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

No changes needed to `pom.xml` - just need to download dependencies!

## Still Having Issues?

If the error persists:
1. Check your internet connection (Maven needs to download from repositories)
2. Verify Maven settings in IntelliJ (File → Settings → Maven)
3. Try building the project: **Build** → **Rebuild Project**






