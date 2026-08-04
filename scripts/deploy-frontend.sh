#!/usr/bin/env bash

# 프론트 컨테이너 교체 중 일부 명령이 실패하면 즉시 중단한다.
set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
readonly COMPOSE_FILE="${PROJECT_ROOT}/compose.yml"
readonly ENV_FILE="${PROJECT_ROOT}/.env"
readonly RUNTIME_DIR="${PROJECT_ROOT}/runtime"
readonly DEPLOYMENT_ENV_FILE="${RUNTIME_DIR}/deployment.env"
readonly ACTIVE_BACKEND_FILE="${RUNTIME_DIR}/nginx/active-backend.conf"
readonly DEPLOYMENT_LOCK_FILE="${RUNTIME_DIR}/deploy.lock"

temporary_env_file=""

print_usage() {
  cat <<'EOF'
Usage: ./scripts/deploy-frontend.sh <frontend-image-tag>

Example:
  ./scripts/deploy-frontend.sh 0123456789abcdef0123456789abcdef01234567
EOF
}

fail() {
  printf 'Error: %s\n' "$1" >&2
  exit 1
}

warn() {
  printf 'Warning: %s\n' "$1" >&2
}

cleanup_temporary_file() {
  if [[ -n "${temporary_env_file}" && -e "${temporary_env_file}" ]]; then
    rm -f -- "${temporary_env_file}"
  fi
}

trap cleanup_temporary_file EXIT

if (( $# != 1 )); then
  print_usage
  exit 1
fi

readonly new_frontend_tag="$1"

# Docker tag 형식이 아닌 값이나 셸 명령 문자가 전달되는 것을 차단한다.
if [[ ! "${new_frontend_tag}" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
  fail "frontend-image-tag is not a valid Docker tag."
fi

[[ -f "${COMPOSE_FILE}" ]] || fail "compose.yml was not found at ${COMPOSE_FILE}."
[[ -f "${ENV_FILE}" ]] || fail ".env was not found at ${ENV_FILE}."
[[ -f "${DEPLOYMENT_ENV_FILE}" ]] || fail "A/B backend deployment state was not found."
[[ -f "${ACTIVE_BACKEND_FILE}" ]] || fail "the active Nginx backend configuration was not found."

command -v docker >/dev/null 2>&1 || fail "docker command was not found."
command -v flock >/dev/null 2>&1 || fail "flock command was not found."
docker compose version >/dev/null 2>&1 || fail "Docker Compose plugin was not found."

# 백엔드 A/B 전환과 프론트 컨테이너 교체가 동시에 Compose 상태를 변경하지 못하게 한다.
exec 9>"${DEPLOYMENT_LOCK_FILE}"
flock -n 9 || fail "another frontend or backend deployment is already running."

compose() {
  docker compose \
    --project-directory "${PROJECT_ROOT}" \
    --file "${COMPOSE_FILE}" \
    --env-file "${ENV_FILE}" \
    --env-file "${DEPLOYMENT_ENV_FILE}" \
    "$@"
}

read_frontend_tag() {
  local frontend_tag=""

  while IFS='=' read -r key value; do
    if [[ "${key}" == "FRONTEND_TAG" ]]; then
      frontend_tag="${value}"
      break
    fi
  done < "${ENV_FILE}"

  if [[ ! "${frontend_tag}" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
    fail ".env contains a missing or invalid FRONTEND_TAG."
  fi

  printf '%s\n' "${frontend_tag}"
}

write_frontend_tag() {
  local frontend_tag="$1"

  umask 077
  temporary_env_file="$(mktemp "${PROJECT_ROOT}/.env.XXXXXX")"

  awk -v frontend_tag="${frontend_tag}" '
    BEGIN { updated = 0 }
    /^FRONTEND_TAG=/ && updated == 0 {
      print "FRONTEND_TAG=" frontend_tag
      updated = 1
      next
    }
    { print }
    END {
      if (updated == 0) {
        print "FRONTEND_TAG=" frontend_tag
      }
    }
  ' "${ENV_FILE}" > "${temporary_env_file}"

  chmod 600 "${temporary_env_file}"
  mv -- "${temporary_env_file}" "${ENV_FILE}"
  temporary_env_file=""
}

test_frontend() {
  local attempt

  for attempt in {1..15}; do
    if compose exec -T frontend nginx -t >/dev/null 2>&1 \
      && compose exec -T frontend wget -q -O /dev/null -T 5 http://127.0.0.1/ \
      && compose exec -T frontend wget -q -O /dev/null -T 5 http://127.0.0.1/api/actuator/health; then
      return 0
    fi

    sleep 2
  done

  return 1
}

rollback_frontend() {
  local previous_tag="$1"

  warn "restoring the previous frontend image tag ${previous_tag}."
  write_frontend_tag "${previous_tag}"

  if ! compose pull frontend; then
    warn "the previous frontend image could not be pulled. Docker will try its local image."
  fi

  if ! compose up -d --no-deps --force-recreate frontend; then
    warn "the previous frontend container could not be restored automatically."
    return 1
  fi

  if ! test_frontend; then
    warn "the restored frontend did not pass its smoke test. Manual recovery is required."
    return 1
  fi
}

readonly previous_frontend_tag="$(read_frontend_tag)"

# Compose가 기존 운영 설정 전체를 정상적으로 해석할 수 있는지 먼저 확인한다.
compose config --quiet

if [[ "${previous_frontend_tag}" == "${new_frontend_tag}" ]] && test_frontend; then
  printf 'Frontend image tag %s is already deployed and healthy.\n' "${new_frontend_tag}"
  exit 0
fi

# .env를 바꾸기 전에 새 이미지를 먼저 받아 pull 실패가 현재 실행 상태에 영향을 주지 않게 한다.
printf 'Pulling frontend image tag %s...\n' "${new_frontend_tag}"
if ! FRONTEND_TAG="${new_frontend_tag}" compose pull frontend; then
  fail "failed to pull the new frontend image."
fi

write_frontend_tag "${new_frontend_tag}"

printf 'Recreating the frontend container...\n'
if ! compose up -d --no-deps --force-recreate frontend; then
  rollback_frontend "${previous_frontend_tag}" || true
  fail "failed to recreate the frontend container; rollback was attempted."
fi

if ! test_frontend; then
  rollback_frontend "${previous_frontend_tag}" || true
  fail "the new frontend failed its smoke test; rollback was attempted."
fi

printf 'Frontend deployment completed successfully. Image tag: %s\n' "${new_frontend_tag}"
