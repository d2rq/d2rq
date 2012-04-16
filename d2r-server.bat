@echo off
if NOT EXIST .\d2r-server.bat (
  echo Please cd into the D2R Server directory to run the server
  exit /B
)
set D2RQ_ROOT=%~p0
set CP="%D2RQ_ROOT%build"
call :findjars "%D2RQ_ROOT%lib"
set LOGCONFIG=file:%D2RQ_ROOT%etc/log4j.properties
java -cp %CP% -Xmx1G "-Dlog4j.configuration=%LOGCONFIG%" d2rq.server %*
exit /B

:findjars
for %%j in (%1\*.jar) do call :addjar "%%j"
for /D %%d in (%1\*) do call :findjars "%%d"
exit /B

:addjar
set CP=%CP%;%1
