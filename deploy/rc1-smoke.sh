#!/usr/bin/env bash
set -Eeuo pipefail

: "${BSO_BASE_URL:?BSO_BASE_URL, z. B. http://127.0.0.1:8081}"
: "${BSO_TEST_USER:?fiktives Testkonto erforderlich}"
: "${BSO_TEST_PASSWORD:?fiktives Testpasswort erforderlich}"
cookie_file="$(mktemp)"
trap 'rm -f "$cookie_file"' EXIT
health="$(curl -fsS "$BSO_BASE_URL/api/health")"
grep -q '"status":"ok"' <<<"$health"
token="$(curl -fsS -c "$cookie_file" "$BSO_BASE_URL/api/csrf" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
curl -fsS -o /dev/null -c "$cookie_file" -b "$cookie_file" -X POST -H 'Content-Type: application/x-www-form-urlencoded' -H "X-XSRF-TOKEN: $token" --data-urlencode "username=$BSO_TEST_USER" --data-urlencode "password=$BSO_TEST_PASSWORD" --data-urlencode "_csrf=$token" "$BSO_BASE_URL/login"
token="$(curl -fsS -b "$cookie_file" -c "$cookie_file" "$BSO_BASE_URL/api/csrf" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
curl -fsS -b "$cookie_file" "$BSO_BASE_URL/api/me" >/dev/null
printf 'RC1 HTTP smoke bestanden: health, CSRF, Login, Session\n'

if [[ -n "${BSO_STUDENT_PK:-}" && -n "${BSO_EVENT_ID:-}" ]]; then
  results="$( { curl -s -o /dev/null -w '%{http_code} ' -b "$cookie_file" -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $token" -d "{\"studentPk\":$BSO_STUDENT_PK,\"eventId\":$BSO_EVENT_ID,\"coins\":${BSO_COINS:-0.5}}" "$BSO_BASE_URL/api/grants" & curl -s -o /dev/null -w '%{http_code}\n' -b "$cookie_file" -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $token" -d "{\"studentPk\":$BSO_STUDENT_PK,\"eventId\":$BSO_EVENT_ID,\"coins\":${BSO_COINS:-0.5}}" "$BSO_BASE_URL/api/grants"; wait; } )"
  grep -Eq '201.*400|400.*201' <<<"$results"
  printf 'RC1 Parallelbuchung bestanden: genau eine Annahme, eine Doppelbuchung abgewiesen\n'
fi
