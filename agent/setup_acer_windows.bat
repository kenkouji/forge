@echo off
REM ==============================================================================
REM RemoteForge Windows Agent Setup & Launch Script
REM Target Machine: Acer Aspire 7 A715-76G-548J (NVIDIA RTX 3050)
REM ==============================================================================

echo [RemoteForge] Initializing PC Agent on Windows...
echo [RemoteForge] Target Computer: Acer Aspire 7 (A715-76G-548J)

where python >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Python 3.10+ was not found on PATH. Please install Python from python.org
    pause
    exit /b 1
)

if not exist venv (
    echo [RemoteForge] Creating virtual environment...
    python -m venv venv
)

echo [RemoteForge] Activating virtual environment and installing dependencies...
call venv\Scripts\activate.bat
pip install --upgrade pip
pip install websockets psutil requests

REM Configure target server URL:
REM Set to your Cloudflare Tunnel public URL or NetBird VPN IP
set SERVER_URL=https://dream-utilize-offline-chen.trycloudflare.com
set SERVER_WS_URL=wss://dream-utilize-offline-chen.trycloudflare.com
set AGENT_SECRET=rf_agent_secret_token_default_replace_me

echo.
echo ==============================================================================
echo [RemoteForge] Connecting outbound to: %SERVER_WS_URL%
echo [RemoteForge] Outbound architecture: No inbound ports required on Windows Firewall!
echo ==============================================================================
echo.

set PYTHONPATH=.;backend
python agent\remote_forge_agent\main.py --server %SERVER_WS_URL% --token %AGENT_SECRET%

pause
