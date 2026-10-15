@echo off
REM RemoteForge Windows Agent Startup Installer
REM Copies a startup batch runner into shell:startup for automatic launch on boot

set STARTUP_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
set SCRIPT_DIR=%~dp0

echo [RemoteForge] Installing Windows Agent Auto-Start...
echo Target Directory: %STARTUP_DIR%

(
echo @echo off
echo cd /d "%SCRIPT_DIR%"
echo call setup_acer_windows.bat
) > "%STARTUP_DIR%\RemoteForgeAgent.bat"

echo [RemoteForge] Windows Auto-Start script installed to "%STARTUP_DIR%\RemoteForgeAgent.bat".
echo [RemoteForge] The Agent will now start automatically upon Windows user login.
pause
