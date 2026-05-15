@echo off
setlocal EnableExtensions

cd /d "%~dp0"

echo Compiling ATM GUI client...
javac ATMServer.java ATMClient.java ATMGuiClient.java
if errorlevel 1 (
    echo Compile failed. Please check that JDK is installed and javac is available.
    goto finish
)

echo Starting ATM GUI client...
java ATMGuiClient

:finish
echo.
echo Program finished with exit code %ERRORLEVEL%.
pause
exit /b %ERRORLEVEL%
