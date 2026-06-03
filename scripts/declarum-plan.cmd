@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
set "MANIFEST_PATH=%~f1"

if "%MANIFEST_PATH%"=="" goto usage
if "%~2" neq "" goto usage
if not exist "%MANIFEST_PATH%" (
    >&2 echo Manifest file not found: "%MANIFEST_PATH%"
    exit /b 2
)

if not defined DOCUMENTUM_DOCBASE (
    >&2 echo DOCUMENTUM_DOCBASE environment variable is required.
    exit /b 2
)
if not defined DOCUMENTUM_USER (
    >&2 echo DOCUMENTUM_USER environment variable is required.
    exit /b 2
)
if not defined DOCUMENTUM_PASSWORD (
    >&2 echo DOCUMENTUM_PASSWORD environment variable is required.
    exit /b 2
)
if not defined DOCUMENTUM_DFC_JAR (
    >&2 echo DOCUMENTUM_DFC_JAR environment variable is required and must point to dfc.jar.
    exit /b 2
)
if not exist "%DOCUMENTUM_DFC_JAR%" (
    >&2 echo DOCUMENTUM_DFC_JAR does not point to an existing file: "%DOCUMENTUM_DFC_JAR%"
    exit /b 2
)
if defined DOCUMENTUM_DFC_CONFIG_DIR if not exist "%DOCUMENTUM_DFC_CONFIG_DIR%" (
    >&2 echo DOCUMENTUM_DFC_CONFIG_DIR does not point to an existing directory: "%DOCUMENTUM_DFC_CONFIG_DIR%"
    exit /b 2
)

pushd "%PROJECT_DIR%" || exit /b 1

call "%PROJECT_DIR%\mvnw.cmd" -q -DskipTests compile dependency:build-classpath -Dmdep.outputFile=target\declarum-classpath.txt
if errorlevel 1 (
    popd
    exit /b 1
)

set "DECLARUM_DEPENDENCY_CLASSPATH="
if exist "target\declarum-classpath.txt" set /p DECLARUM_DEPENDENCY_CLASSPATH=<"target\declarum-classpath.txt"

set "DECLARUM_RUNTIME_CLASSPATH=target\classes"
if defined DECLARUM_DEPENDENCY_CLASSPATH set "DECLARUM_RUNTIME_CLASSPATH=%DECLARUM_RUNTIME_CLASSPATH%;%DECLARUM_DEPENDENCY_CLASSPATH%"
set "DECLARUM_RUNTIME_CLASSPATH=%DECLARUM_RUNTIME_CLASSPATH%;%DOCUMENTUM_DFC_JAR%"
if defined DOCUMENTUM_DFC_CONFIG_DIR set "DECLARUM_RUNTIME_CLASSPATH=%DOCUMENTUM_DFC_CONFIG_DIR%;%DECLARUM_RUNTIME_CLASSPATH%"
if defined CLASSPATH set "DECLARUM_RUNTIME_CLASSPATH=%DECLARUM_RUNTIME_CLASSPATH%;%CLASSPATH%"

java -cp "%DECLARUM_RUNTIME_CLASSPATH%" com.f0xrge.declarum.cli.DeclarumCli "%MANIFEST_PATH%"
set "EXIT_CODE=%ERRORLEVEL%"

popd
exit /b %EXIT_CODE%

:usage
>&2 echo Usage: scripts\declarum-plan.cmd path\to\manifest.yaml
>&2 echo Required environment variables: DOCUMENTUM_DOCBASE, DOCUMENTUM_USER, DOCUMENTUM_PASSWORD, DOCUMENTUM_DFC_JAR
>&2 echo Optional environment variables: DOCUMENTUM_DOMAIN, DOCUMENTUM_DFC_CONFIG_DIR
exit /b 2
