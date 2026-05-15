@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

set "DEFAULT_HOST=127.0.0.1"
set "DEFAULT_PORT=2525"

if /I "%~1"=="server" goto server_args
if /I "%~1"=="client" goto client_args

:menu
echo.
echo ATM startup script
echo 1. Start ATM server
echo 2. Start ATM client
echo 0. Exit
set "choice="
set /p "choice=Choose mode [1]: "
if "%choice%"=="" set "choice=1"

if "%choice%"=="1" goto server_prompt
if "%choice%"=="2" goto client_prompt
if "%choice%"=="0" exit /b 0

echo Invalid choice.
goto menu

:server_args
set "PORT=%~2"
if "%PORT%"=="" set "PORT=%DEFAULT_PORT%"
goto start_server

:server_prompt
set "PORT="
set /p "PORT=Server listen port [%DEFAULT_PORT%]: "
if "%PORT%"=="" set "PORT=%DEFAULT_PORT%"
goto start_server

:start_server
call :validate_port "%PORT%" || goto fail
call :compile_sources || goto fail

echo Starting ATM server on port %PORT%
java ATMServer "%PORT%"
set "EXIT_CODE=%ERRORLEVEL%"
goto finish

:client_args
set "HOST=%~2"
set "PORT=%~3"
if "%HOST%"=="" set "HOST=%DEFAULT_HOST%"
if "%PORT%"=="" set "PORT=%DEFAULT_PORT%"
goto start_client

:client_prompt
set "HOST="
set "PORT="
set /p "HOST=ATM server IP or host [%DEFAULT_HOST%]: "
if "%HOST%"=="" set "HOST=%DEFAULT_HOST%"
set /p "PORT=ATM server port [%DEFAULT_PORT%]: "
if "%PORT%"=="" set "PORT=%DEFAULT_PORT%"
goto start_client

:start_client
call :validate_port "%PORT%" || goto fail
call :compile_sources || goto fail

echo Connecting to ATM server %HOST%:%PORT%
java ATMClient "%HOST%" "%PORT%"
set "EXIT_CODE=%ERRORLEVEL%"
goto finish

:fail
set "EXIT_CODE=1"
goto finish

:finish
echo.
echo Program finished with exit code %EXIT_CODE%.
pause
exit /b %EXIT_CODE%

:compile_sources
echo Compiling ATMServer.java, ATMClient.java and ATMGuiClient.java...
javac ATMServer.java ATMClient.java ATMGuiClient.java
if errorlevel 1 (
    echo Compile failed. Please check that JDK is installed and javac is available.
    exit /b 1
)
exit /b 0

:validate_port
set "CHECK_PORT=%~1"
echo(%CHECK_PORT%| findstr /R "^[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo Invalid port. It must be a number from 1 to 65535.
    exit /b 1
)

set /A CHECK_PORT_NUM=%CHECK_PORT% >nul 2>nul
if %CHECK_PORT_NUM% LSS 1 (
    echo Invalid port. It must be a number from 1 to 65535.
    exit /b 1
)
if %CHECK_PORT_NUM% GTR 65535 (
    echo Invalid port. It must be a number from 1 to 65535.
    exit /b 1
)
exit /b 0
