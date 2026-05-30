#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(dirname "$0")
source "$SCRIPT_DIR/colors.sh"

# ---------------------------------------------------------------------------
# mise
# ---------------------------------------------------------------------------

if ! command -v mise >/dev/null 2>&1; then
  echo -e "${RED}mise not found — install it first: https://mise.jdx.dev${NC}"
  exit 1
fi

echo -e "${YELLOW}Installing tools from mise.toml${NC}"
mise install

