#!/usr/bin/env bash

source "$(dirname "$0")/_common.sh"

if [[ ! -x "$PROJECT_ROOT/.venv/bin/python" ]]; then
  fail "Python environment is missing. Run scripts/dev/setup.sh first."
fi

cd "$PROJECT_ROOT"
exec .venv/bin/python -m uvicorn api.main:app --host 0.0.0.0 --port 8000 --reload
