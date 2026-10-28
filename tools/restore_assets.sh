#!/usr/bin/env bash
# 还原 Base64 保存的二进制资源（贴图 / logo）。
# 仓库里 .png 用 assets-b64/*.b64 存（纯文本，便于在任何地方 review / diff），
# 首次构建前跑一次这个脚本即可。
set -euo pipefail
cd "$(dirname "$0")/.."

restore() {  # $1 = b64 相对路径, $2 = 目标相对路径
  local src="$1" dst="$2"
  [ -f "$src" ] || { echo "缺少 $src"; return 0; }
  mkdir -p "$(dirname "$dst")"
  base64 -d "$src" > "$dst"
  echo "还原 $dst ($(wc -c < "$dst") 字节)"
}

restore assets-b64/seven_curses_ring.png.b64  src/main/resources/assets/sevencurses/textures/item/seven_curses_ring.png
restore assets-b64/artifact_noise.png.b64     src/main/resources/assets/sevencurses/textures/glitch/artifact_noise.png
restore assets-b64/logo.png.b64               src/main/resources/logo.png

echo "完成。现在可以 ./gradlew build 了。"
