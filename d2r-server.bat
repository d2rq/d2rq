@echo off
@REM $Id$

if EXIST .\lib\d2rq.jar (
  set D2R_ROOT=.
  goto :ok
)

echo Please run the D2R Server from its root directory
goto theEnd

:ok
REM Do this to put the developement .class files first
REM NB no space before the ")"
if EXIST %D2R_ROOT%\classes (
  if "%CP%" == "" (set CP=%D2R_ROOT%\classes) ELSE (set CP=%CP%;%D2R_ROOT%\classes)
)

pushd %D2R_ROOT%
for %%f in (lib\*.jar) do call :oneStep %%f
popd
goto noMore

:oneStep
if "%CP%" == "" (set CP=%D2R_ROOT%\%1) ELSE (set CP=%CP%;%D2R_ROOT%\%1)
exit /B

:noMore

REM set LOGCONFIG=file:%D2R_ROOT%\etc\log4j-detail.properties
set LOGCONFIG=file:%D2R_ROOT%\etc\log4j.properties
set LOG=-Dlog4j.configuration=%LOGCONFIG%

java -cp %CP% %LOG% d2r.server %1 %2 %3 %4 %5 %6 %7 %8 %9

:theEnd
