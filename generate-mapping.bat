@echo off
@REM $Id: generate-mapping.bat,v 1.1 2006/05/30 07:29:00 cyganiak Exp $

set D2R_ROOT=%0
pushd %D2R_ROOT%
for %%f in (lib\*.jar lib\*\*.jar) do call :oneStep %%f
popd
goto noMore

:oneStep
if "%CP%" == "" (set CP=%D2R_ROOT%\%1) ELSE (set CP=%CP%;%D2R_ROOT%\%1)
exit /B

:noMore
java -cp %CP% d2rq.generate_mapping %1 %2 %3 %4 %5 %6 %7 %8 %9
