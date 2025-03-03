@echo off
rem -------------------------------------------------------------------------
rem JBoss Server Migration Tool for Windows
rem -------------------------------------------------------------------------
rem
rem A simple tool for migrating servers.

rem $Id$

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "BASE_DIR=%~dp0%"
) else (
  set BASE_DIR=.\
)

rem Setup JBoss specific properties
if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  set "JAVA=%JAVA_HOME%\bin\java"
)

rem set default modular jvm parameters
setlocal EnableDelayedExpansion
"%JAVA%" --add-modules=java.se -version >nul 2>&1 && (set MODULAR_JDK=true) || (set MODULAR_JDK=false)
if "!MODULAR_JDK!" == "true" (
    echo "%JAVA_OPTS%" | findstr /I "\-\-add\-modules" > nul
    if errorlevel == 1 (
      rem Set default modular jdk options
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-exports=java.base/sun.nio.ch=ALL-UNNAMED"
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED"
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-exports=jdk.unsupported/sun.reflect=ALL-UNNAMED"
      set "DEFAULT_MODULAR_JVM_OPTIONS=!DEFAULT_MODULAR_JVM_OPTIONS! --add-modules=java.se"
    ) else (
      set "DEFAULT_MODULAR_JVM_OPTIONS="
    )
)
set "JAVA_OPTS=!JAVA_OPTS! !DEFAULT_MODULAR_JVM_OPTIONS!"
setlocal DisableDelayedExpansion

set "JAVA_OPTS=%JAVA_OPTS% -Djboss.server.migration.baseDir=%BASE_DIR%"

echo "%JAVA_OPTS%" | findstr /I "logging.configuration" > nul
if errorlevel == 1 (
  set "JAVA_OPTS=%JAVA_OPTS% -Djava.util.logging.manager=org.jboss.logmanager.LogManager -Dlogging.configuration=file:%BASE_DIR%\configuration\logging.properties -Djboss.server.migration.logfile=%BASE_DIR%\logs\migration.log"
) else (
  echo logging.configuration already set in JAVA_OPTS
)

rem Sort arguments into JAVA_OPTS and TOOL_OPTS
set TOOL_OPTS=

:argsLoop
set "arg=%~1%"
if "%arg:~0,2%"=="-D" (
  set "JAVA_OPTS=%JAVA_OPTS% %arg%"
) else (
  set "TOOL_OPTS=%TOOL_OPTS% %arg%"
)
shift
if not "%~1%"=="" goto argsLoop


"%JAVA%" %JAVA_OPTS% ^
    -cp "%BASE_DIR%\lib\*" ^
    org.jboss.migration.cli.CommandLineServerMigration ^
    %TOOL_OPTS%

set /A RC=%errorlevel%
:END
if "x%NOPAUSE%" == "x" pause

if "x%RC%" == "x" (
  set /A RC=0
)
exit /B %RC%