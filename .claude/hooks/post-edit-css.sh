#!/usr/bin/env bash
# PostToolUse hook — static resource guards after Edit/Write
# 1) src/main/resources/static/** 변경 시 build/resources/main 으로 즉시 미러 복사.
#    실행 중인 bootRun 은 build 산출물을 우선 서빙하므로, 미러 없이는 옛 CSS/JS/이미지가
#    계속 서빙됨 (2026-07-02 D1b 사고). processResources 전체 실행 없이 파일 1개만 복사.
# 2) .css 파일의 self-referencing custom property 감지 (`--x: var(--x)` → 값 unset 버그).
#    Historical incident: --color-text-tri self-reference broke text-tri color (STATE.md).

set -euo pipefail

input="$(cat)"
# Extract file_path from tool_input
file="$(printf '%s' "$input" | grep -oE '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"file_path"[[:space:]]*:[[:space:]]*"//; s/"$//')"

if [ -z "$file" ]; then
  exit 0
fi

# JSON 이스케이프(\\)·Windows 역슬래시 경로를 forward slash 로 정규화
fslash="$(printf '%s' "$file" | sed 's/\\\\/\//g; s/\\/\//g')"

# --- 1) static resource mirror (css/js/images 모두) ---
case "$fslash" in
  */src/main/resources/static/*)
    if [ -f "$fslash" ]; then
      rel="${fslash#*/src/main/resources/}"
      root="${fslash%/src/main/resources/*}"
      dest="$root/build/resources/main/$rel"
      if [ -d "$root/build/resources/main" ]; then
        mkdir -p "$(dirname "$dest")"
        cp -f "$fslash" "$dest"
        echo "INFO: static resource mirrored → build/resources/main/$rel (실행 중인 bootRun 에 즉시 반영. 브라우저는 Ctrl+Shift+R)" >&2
      fi
    fi
    ;;
esac

# --- 2) CSS self-reference check ---
case "$fslash" in
  *.css) ;;
  *) exit 0 ;;
esac

file="$fslash"
if [ ! -f "$file" ]; then
  exit 0
fi

# Detect `--x: var(--x)` — CSS custom property that references itself.
# Regex: capture --NAME on LHS, then look for var(--NAME) on RHS on same line.
selfref="$(grep -nE '^\s*(--[a-zA-Z0-9_-]+)\s*:[^;]*\bvar\(\1\)' "$file" || true)"

if [ -n "$selfref" ]; then
  cat >&2 <<EOF
WARN: self-referencing CSS custom property detected in $file

$selfref

This produces an invalid CSS value and the property will be unset (transparent / initial).
Historical incident: --color-text-tri self-reference broke text-tri color (see STATE.md).

Fix by referencing a base value (e.g. oklch() literal or another token):
  --color-text-tri: oklch(0.7 0.02 280);  /* not var(--color-text-tri) */
EOF
  # Soft warning — allow save. Change to `exit 2` to hard block.
fi

exit 0
