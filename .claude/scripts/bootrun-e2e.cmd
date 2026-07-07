@echo off
rem Claude Preview bootRun wrapper (Windows office PC).
rem System JAVA_HOME is JDK 8, which the Gradle 9 launcher rejects -> force JDK 17 here.
rem e2e profile (H2 in-memory + seed) on port 8090 -> never conflicts with IntelliJ's 8080 server.
rem NOTE: bare "gradlew.bat" fails on this PC (cwd not searched for executables) -> explicit path.
set "JAVA_HOME=C:\Program Files\Java\jdk-17.0.14"
set "REPO=%~dp0..\.."
cd /d "%REPO%"
call "%REPO%\gradlew.bat" -p "%REPO%" bootRun "--args=--spring.profiles.active=e2e --server.port=8090"
