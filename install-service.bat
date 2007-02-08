@echo off

REM !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
REM !!!!!!!!!! Set this to the root directory of your Java installation. !!!!!!!
REM !!!!!!!!!! Typical value: C:\Program Files\Java\jdk_1.5.0_09         !!!!!!!
REM !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
set JAVA_HOME=

REM Check if at least serviceName and mappingFileName are present on the command line
if "%1%2"=="" (
  echo usage: install-service serviceName [-p port] [-b serverBaseURI] mappingFileName
  exit /B
)

REM Make sure we are in the D2R directory
if NOT EXIST .\d2r-server.bat (
  echo Please cd into the D2R Server directory to install the service
  exit /B
)

REM Make sure we have JAVA_HOME
if "%JAVA_HOME%"=="" (
  echo Could not determine the location of the Java installation.
  echo Please set the JAVA_HOME environment variable or edit install-service.bat
  exit /B
)

REM Check various possible locations for jvm.dll. I don't really know what
REM I'm doing here ...
set JVM_DLL=
IF EXIST "%JAVA_HOME%\bin\client\jvm.dll" (
  set JVM_DLL=%JAVA_HOME%\bin\client\jvm.dll
)
IF EXIST "%JAVA_HOME%\bin\hotspot\jvm.dll" (
  set JVM_DLL=%JAVA_HOME%\bin\hotspot\jvm.dll
)
IF EXIST "%JAVA_HOME%\bin\server\jvm.dll" (
  set JVM_DLL=%JAVA_HOME%\bin\server\jvm.dll
)
IF EXIST "%JAVA_HOME%\jre\bin\client\jvm.dll" (
  set JVM_DLL=%JAVA_HOME%\jre\bin\client\jvm.dll
)
IF EXIST "%JAVA_HOME%\jre\bin\hotspot\jvm.dll" (
  set JVM_DLL=%JAVA_HOME%\jre\bin\hotspot\jvm.dll
)
IF EXIST "%JAVA_HOME%\jre\bin\server\jvm.dll" (
  set JVM_DLL=%JAVA_HOME%\jre\bin\server\jvm.dll
)
if "%JVM_DLL%"=="" (
  IF NOT EXIST "%JAVA_HOME%" (
    echo JAVA_HOME is not set correctly. The directory does not exist.
  ) ELSE (
    echo Failed to locate jvm.dll. Please check if JAVA_HOME is set correctly.
  )
  echo Current value of JAVA_HOME: %JAVA_HOME%
  exit /B
)

set D2RQ_ROOT=%CD%
set CP="%D2RQ_ROOT%\build"
call :findjars "%D2RQ_ROOT%\lib"
set LOGCONFIG=%D2RQ_ROOT%/etc/log4j.properties
rem "-Dlog4j.configuration=%LOGCONFIG%" 
set JAVA_OPTIONS=-Djava.class.path=%CP% -Xms64M -Xmx256M
"%D2RQ_ROOT%\etc\D2Rservice.exe" -install %1 "%JVM_DLL%" %JAVA_OPTIONS% -start d2r.server -params %2 %3 %4 %5 %6 %7 %8 %9 -out "%D2RQ_ROOT%\stdout.log" -err "%D2RQ_ROOT%\stderr.log" -current "%D2RQ_ROOT%" -append -description "D2R Server"
echo.
IF ERRORLEVEL 1 EXIT /B

echo You can start and stop the service from the Services pane in the
echo Management Console.
echo The service logs to stdout.log and stderr.log in the current directory.
echo To uninstall the service, run 'uninstall-service %1'.
exit /B

:findjars
for %%j in (%1\*.jar) do call :addjar "%%j"
for /D %%d in (%1\*) do call :findjars "%%d"
exit /B

:addjar
set CP=%CP%;%1
