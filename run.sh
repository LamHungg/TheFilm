#!/usr/bin/env bash
# Chay Keycloak (docker compose) roi chay BE Spring Boot voi profile oracle (xac thuc bang Keycloak that).
# Cach dung: ./run.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ORACLE_CONTAINER="${ORACLE_CONTAINER:-oracle26ai}"
KC_URL="http://localhost:8081/realms/thefilm/.well-known/openid-configuration"

mkdir -p "$ROOT/.run"

# 1. Oracle: neu co container oracle26ai dang tat thi bat len
if command -v docker >/dev/null 2>&1; then
  if docker ps -a --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER"; then
    if ! docker ps --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER"; then
      echo "Dang bat container Oracle ($ORACLE_CONTAINER)..."
      docker start "$ORACLE_CONTAINER" >/dev/null
    fi
  else
    echo "Khong thay container $ORACLE_CONTAINER, bo qua buoc bat Oracle (dung Oracle co san tren may)."
  fi
else
  echo "Chua cai docker, khong the chay Keycloak." >&2
  exit 1
fi

# 2. Keycloak
echo "Dang khoi dong Keycloak..."
docker compose -f "$ROOT/docker-compose.yml" up -d

echo -n "Cho Keycloak san sang"
for _ in $(seq 1 90); do
  if curl -fsS "$KC_URL" >/dev/null 2>&1; then
    echo " - xong."
    break
  fi
  echo -n "."
  sleep 2
done
if ! curl -fsS "$KC_URL" >/dev/null 2>&1; then
  echo
  echo "Keycloak chua san sang sau 3 phut. Xem log: docker logs thefilm-keycloak" >&2
  exit 1
fi

# 3. Bien moi truong cho BE (doc tu BE/.env neu co)
cd "$ROOT/BE"
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI="${SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI:-http://localhost:8081/realms/thefilm}"

# 4. BE (khong dung profile devauth, token that lay tu Keycloak)
echo $$ > "$ROOT/.run/be.pid"
echo "Dang chay BE. Dung bang Ctrl+C hoac ./stop.sh"
exec mvn spring-boot:run "-Dspring-boot.run.profiles=oracle"
