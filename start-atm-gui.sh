#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "正在编译 ATM 图形客户端..."
javac ATMServer.java ATMClient.java ATMGuiClient.java

echo "启动ATM图形客户端"
java ATMGuiClient
