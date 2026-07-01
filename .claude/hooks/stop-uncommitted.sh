#!/usr/bin/env bash
# Stop hook — remind user if there are uncommitted changes at session end
# Non-blocking: prints reminder to stderr (visible in Claude session) but never blocks.

set -euo pipefail

# Repo root — this hook runs from wherever Claude invoked it. Try to cd.
repo_root="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [ -z "$repo_root" ]; then
  exit 0
fi
cd "$repo_root"

changes="$(git status --porcelain 2>/dev/null | head -30)"

if [ -z "$changes" ]; then
  exit 0
fi

changed_count="$(printf '%s' "$changes" | wc -l | tr -d ' ')"

cat >&2 <<EOF
REMINDER: 세션 종료 전, 미커밋 변경 $changed_count 개가 남아 있습니다.

$changes

권장:
  - 저장할 변경이면: /wrap-up 실행 → 정적/E2E 검증 후 명시적 stage → 커밋 → PR
  - 임시 저장: git stash push -m "WIP" -- <files>
  - 폐기 확실: git restore <files>

다중 세션 병행 중이면 다른 세션의 파일까지 sweeping 안 되도록 명시적 add 필수.
EOF

exit 0
