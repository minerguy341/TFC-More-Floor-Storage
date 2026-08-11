#!/usr/bin/env bash
# Local static server for the TFC Lean Mesh Visualizer.
cd "$(dirname "$0")"
PORT=8765
if command -v python3 >/dev/null; then
  (sleep 0.4; command -v xdg-open >/dev/null && xdg-open "http://127.0.0.1:${PORT}/" || true) &
  exec python3 -m http.server "$PORT"
fi
echo "Python 3 required to serve ES modules."
exit 1
