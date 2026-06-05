@echo off
cd /d "%~dp0"
if exist ".env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
    if not "%%A"=="" set "%%A=%%B"
  )
)
"C:\Program Files\Java\jdk-21\bin\java.exe" -jar "build\libs\app.jar" > "backend-runtime.out.log" 2> "backend-runtime.err.log"
