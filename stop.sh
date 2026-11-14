#!/usr/bin/env bash
# Dung BE va Keycloak. Them --all de dung ca container Oracle.
# Cach dung: ./stop.sh   hoac   ./stop.sh --all
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ORACLE_CONTAINER="${ORACLE_CONTAINER:-oracle26ai}"
PID_FILE="$ROOT/.run/be.pid"

# 1. Dung BE
if [ -f "$PID_FILE" ]; then
  PID="$(cat "$PID_FILE")"
  if kill -0 "$PID" 2>/dev/null; then
    echo "Dang dung BE (pid $PID)..."
    pkill -P "$PID" 2>/dev/null || true
    kill "$PID" 2>/dev/null || true
  fi
  rm -f "$PID_FILE"
fi
# Phong truong hop java con chay sau khi mvn thoat
pkill -f "vn.thefilm.backend.TheFilmApplication" 2>/dev/null || true

# 2. Dung Keycloak (giu nguyen du lieu realm trong file, khong xoa volume)
echo "Dang dung Keycloak..."
docker compose -f "$ROOT/docker-compose.yml" down

# 3. Tuy chon: dung Oracle
if [ "${1:-}" = "--all" ]; then
  if docker ps --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER"; then
    echo "Dang dung Oracle ($ORACLE_CONTAINER)..."
    docker stop "$ORACLE_CONTAINER" >/dev/null
  fi
fi

echo "Da dung."
