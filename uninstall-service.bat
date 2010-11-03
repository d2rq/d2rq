@echo off

REM Check if serviceName is present on the command line
if "%1"=="" (
  echo usage: uninstall-service serviceName
  exit /B
)

REM Make sure we are in the D2R directory
if NOT EXIST .\d2r-server.bat (
  echo Please cd into the D2R Server directory to uninstall the service
  exit /B
)

set D2RQ_ROOT=%~p0
"%D2RQ_ROOT%etc\D2Rservice.exe" -uninstall %1
