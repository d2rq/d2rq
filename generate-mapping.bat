@echo off
@REM $Id: generate-mapping.bat,v 1.1 2006/09/07 16:57:50 cyganiak Exp $

set D2RQ_ROOT=%0
set CP=
pushd %D2RQ_ROOT%
for %%f in (lib\*.jar lib\*\*.jar) do call :oneStep %%f
popd
goto noMore

:oneStep
if "%CP%" == "" (set CP=%D2RQ_ROOT%\%1) ELSE (set CP=%CP%;%D2RQ_ROOT%\%1)
exit /B

:noMore
set LOGCONFIG=file:%D2RQ_ROOT%\etc\log4j.properties
java -cp "%CP%" "-Dlog4j.configuration=%LOGCONFIG%" d2rq.generate_mapping %1 %2 %3 %4 %5 %6 %7 %8 %9