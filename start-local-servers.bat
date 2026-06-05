@echo off
setlocal

set "ROOT=%~dp0"
set "MAVEN_CMD=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd"

if not exist "%MAVEN_CMD%" (
  set "MAVEN_CMD=%ROOT%backend\mvnw.cmd"
)

if not exist "%MAVEN_CMD%" (
  echo Maven command was not found.
  echo Expected:
  echo   %USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd
  echo or:
  echo   %ROOT%backend\mvnw.cmd
  pause
  exit /b 1
)

echo Starting Pet Record backend on http://localhost:8080 ...
start "pet-record backend" /D "%ROOT%backend" cmd /k ""%MAVEN_CMD%" -nsu spring-boot:run"

echo Starting Pet Record frontend on http://127.0.0.1:5173 ...
start "pet-record frontend" /D "%ROOT%frontend" cmd /k "npm.cmd run dev -- --host localhost --port 5173"

echo.
echo Started local servers in separate windows.
echo Frontend: http://127.0.0.1:5173/
echo Backend:  http://localhost:8080
echo.
echo Close the two opened command windows to stop the servers.
pause
