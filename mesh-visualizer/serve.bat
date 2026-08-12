@echo off
REM Local static server for the TFC Lean Mesh Visualizer (needs ES modules).
cd /d "%~dp0"
where py >nul 2>nul && (
  start "" http://127.0.0.1:8765/
  py -3 -m http.server 8765
  goto :eof
)
where python >nul 2>nul && (
  start "" http://127.0.0.1:8765/
  python -m http.server 8765
  goto :eof
)
echo Python not found. Install Python 3, or open index.html via any static file server.
pause
