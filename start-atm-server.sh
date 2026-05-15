#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DEFAULT_PORT="2525"

read_with_default() {
    local prompt="$1"
    local default_value="$2"
    local value

    read -r -p "$prompt [$default_value]: " value
    printf '%s\n' "${value:-$default_value}"
}

is_valid_port() {
    local port="$1"
    [[ "$port" =~ ^[0-9]+$ ]] && ((port >= 1 && port <= 65535))
}

port="${1:-}"
if [[ -z "$port" ]]; then
    port="$(read_with_default "请输入ATM服务器监听端口" "$DEFAULT_PORT")"
fi

if ! is_valid_port "$port"; then
    echo "端口号无效，应为 1~65535。" >&2
    exit 1
fi

echo "正在编译 ATMServer.java 和 ATMClient.java..."
javac ATMServer.java ATMClient.java ATMGuiClient.java

echo "启动ATM服务器，监听端口 $port"
java ATMServer "$port"
