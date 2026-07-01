#!/usr/bin/env bash
# Stop hook — 세션 종료 시
#   1. 자기 lock 파일 제거
#   2. stale lock (5분+) 청소
#
# Best-effort. 실패해도 세션 종료는 계속.

set -euo pipefail

input="$(cat)"
session_id="$(printf '%s' "$input" | grep -oE '"session_id"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"session_id"[[:space:]]*:[[:space:]]*"//; s/"$//')"
cwd="$(printf '%s' "$input" | grep -oE '"cwd"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"cwd"[[:space:]]*:[[:space:]]*"//; s/"$//')"

[ -z "${cwd:-}" ] && cwd="$(pwd)"

if ! git_common_dir="$(git -C "$cwd" rev-parse --git-common-dir 2>/dev/null)"; then
  exit 0
fi
case "$git_common_dir" in
  /*) ;;
  *)  git_common_dir="$cwd/$git_common_dir" ;;
esac

locks_dir="$git_common_dir/session-locks"
[ -d "$locks_dir" ] || exit 0

# 자기 락 제거
if [ -n "${session_id:-}" ]; then
  rm -f "$locks_dir/${session_id}.lock"
fi

# stale 락 청소
five_min_ago_epoch="$(date -u -d '5 minutes ago' +%s 2>/dev/null || echo 0)"
if [ "$five_min_ago_epoch" = "0" ]; then
  five_min_ago_epoch="$(( $(date -u +%s) - 300 ))"
fi

for f in "$locks_dir"/*.lock; do
  [ -f "$f" ] || continue

  other_hb="$(grep -m1 '^last_heartbeat=' "$f" | cut -d= -f2- || true)"
  [ -z "$other_hb" ] && { rm -f "$f"; continue; }

  other_epoch="$(date -u -d "$other_hb" +%s 2>/dev/null || echo 0)"
  if [ "$other_epoch" = "0" ]; then
    other_epoch="$(date -u -j -f '%Y-%m-%dT%H:%M:%SZ' "$other_hb" +%s 2>/dev/null || echo 0)"
  fi
  if [ "$other_epoch" -lt "$five_min_ago_epoch" ]; then
    rm -f "$f"
  fi
done

exit 0
