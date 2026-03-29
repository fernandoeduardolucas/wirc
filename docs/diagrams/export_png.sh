#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v mmdc >/dev/null 2>&1; then
  echo "Erro: comando 'mmdc' não encontrado."
  echo "Instale @mermaid-js/mermaid-cli e execute novamente."
  exit 1
fi

for src in "$ROOT_DIR"/*.mmd; do
  [ -e "$src" ] || continue
  out="${src%.mmd}.png"
  echo "Gerando $(basename "$out")..."
  mmdc -i "$src" -o "$out" -b transparent
  echo "OK: $out"
done
