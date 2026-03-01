#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

(
  cd "$ROOT_DIR/backend"
  ./mvnw spring-boot:run
) &
BACK_PID=$!

(
  cd "$ROOT_DIR/frontend"
  npm install
  npm start
) &
FRONT_PID=$!

trap 'kill $BACK_PID $FRONT_PID' EXIT
wait
