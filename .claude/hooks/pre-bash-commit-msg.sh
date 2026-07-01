#!/usr/bin/env bash
# PreToolUse hook — warn if `git commit -m "..."` message does not match convention
# Convention: `YYMMDD_identifier - description (#PR)` OR `[TAG] description`
# Exit: 0 allow, 2 block. Warning is soft — allows override with `--force` flag in message context.

set -euo pipefail

input="$(cat)"
cmd="$(printf '%s' "$input" | grep -oE '"command"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"command"[[:space:]]*:[[:space:]]*"//; s/"$//')"

# Only intercept `git commit -m ...`
if ! printf '%s' "$cmd" | grep -qE '(^|[[:space:];&|])git[[:space:]]+commit[[:space:]]+.*-m'; then
  exit 0
fi

# Extract message after -m (rough — handles most common forms)
msg="$(printf '%s' "$cmd" | sed -E 's/.*-m[[:space:]]+"([^"]+)".*/\1/;t;s/.*-m[[:space:]]+'"'"'([^'"'"']+)'"'"'.*/\1/;t;d')"

if [ -z "$msg" ]; then
  # Message uses HEREDOC or other form — skip check
  exit 0
fi

# Take first line only for pattern check
firstline="$(printf '%s' "$msg" | head -1)"

# Pattern A: YYMMDD_identifier - description
# Pattern B: [TAG] description (workflow-level convention)
if printf '%s' "$firstline" | grep -qE '^[0-9]{6}_[a-z0-9_]+[[:space:]]+-[[:space:]]+.+'; then
  exit 0
fi
if printf '%s' "$firstline" | grep -qE '^\[[A-Z가-힣]+\].+'; then
  exit 0
fi

cat >&2 <<EOF
WARN: commit message does not match convention.

Expected patterns:
  A) YYMMDD_identifier - 사용자 화면에서 보이는 변화 요약  (예: 260701_home_prototype - 홈 화면 Hero 검색바 추가)
  B) [워크플로우명] 요약                                     (예: [OOS] OPEN 단계 필수값 해제)

Received first line:
  $firstline

If intentional, proceed by re-running. This hook warns once and allows the commit.
EOF

# Soft warning — do not block. Return 0 so commit proceeds.
# (To make hard block, change to `exit 2`.)
exit 0
