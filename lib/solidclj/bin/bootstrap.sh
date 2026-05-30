#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(dirname "$0")
source "$SCRIPT_DIR/colors.sh"

echo -e "${YELLOW}Installing JS dependencies (bun install)${NC}"
bun install

echo -e "${YELLOW}Fetching Clojure dependencies (clojure -P)${NC}"
clojure -P -A:cljs:test

echo -e "${GREEN}Bootstrap complete — run: task test${NC}"
