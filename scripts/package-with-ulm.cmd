@echo off
rem Builds MailBox and creates a ULM sidecar distribution in target\ulm-dist.
rem Requires: JDK 17+ on PATH for the final launchers; ULM 1.1.0 built/installed locally.
setlocal
set "ROOT=%~dp0.."
set "ULM=%ROOT%\..\unifiedlicensemanager"
set "KEYS=%ROOT%\src\main\ulm\trusted-keys.properties"
set "AGENT=%ULM%\license-agent\target\ulm-agent.jar"
set "CLI=%ULM%\license-cli\target\ulm-cli.jar"

if not exist "%AGENT%" (
  echo Missing %AGENT%
  echo Build ULM first: cd "%ULM%" ^&^& mvnw.cmd -B install
  exit /b 1
)
if not exist "%KEYS%" (
  echo Missing public key file: %KEYS%
  echo Export trusted-keys.properties from the ULM generator into src\main\ulm\
  exit /b 1
)

pushd "%ROOT%" || exit /b 1
call mvn -B package -DskipTests
if errorlevel 1 (
  popd
  exit /b 1
)

java -jar "%CLI%" integrate --jar "%ROOT%\target\MailBox.jar" --product MAILBOX --trusted-keys "%KEYS%" --agent-jar "%AGENT%" --out "%ROOT%\target\ulm-dist" --force
set "RC=%ERRORLEVEL%"
popd
if not "%RC%"=="0" exit /b %RC%
echo.
echo ULM distribution ready: %ROOT%\target\ulm-dist
echo Start with run.bat (Java 17+). Place MAILBOX.lic under %%APPDATA%%\UnifiedLicensing\licenses\
exit /b 0
