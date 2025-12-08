@echo off
echo ========================================
echo Refreshing Maven Dependencies
echo ========================================
echo.
cd /d "%~dp0"
echo Current directory: %CD%
echo.
echo Running: mvn clean compile -U
echo This will download Spring Security and other dependencies...
echo.
call mvn clean compile -U
echo.
echo ========================================
if %ERRORLEVEL% EQU 0 (
    echo SUCCESS! Dependencies downloaded.
    echo Please refresh your IDE project now.
) else (
    echo ERROR occurred. Please check the error messages above.
)
echo ========================================
pause






