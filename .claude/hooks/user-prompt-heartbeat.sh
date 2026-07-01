#!/usr/bin/env bash
# UserPromptSubmit hook — 매 프롬프트 시
#   1. 자기 lock 파일 last_heartbeat 갱신
#   2. 다른 세션 활성 상태를 Claude 컨텍스트에 힌트 주입 (stdout — 프롬프트에 추가됨)
#
# 입력: {"hook_event_name":"UserPromptSubmit","session_id":"...","cwd":"...","prompt":"..."}
# stdout 은 Claude 프롬프트에 컨텍스트로 붙음. 성공 exit 0.

set -euo pipefail

input="$(cat)"
session_id="$(printf '%s' "$input" | grep -oE '"session_id"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"session_id"[[:space:]]*:[[:space:]]*"//; s/"$//')"
cwd="$(printf '%s' "$input" | grep -oE '"cwd"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"cwd"[[:space:]]*:[[:space:]]*"//; s/"$//')"

[ -z "${session_id:-}" ] && exit 0
[ -z "${cwd:-}" ] && cwd="$(pwd)"

if ! git_common_dir="$(git -C "$cwd" rev-parse --git-common-dir 2>/dev/null)"; then
  exit 0
fi
case "$git_common_dir" in
  /*) ;;
  *)  git_common_dir="$cwd/$git_common_dir" ;;
esac

locks_dir="$git_common_dir/session-locks"
my_lock="$locks_dir/${session_id}.lock"
now="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

# 자기 lock 이 없으면 (SessionStart hook 이 못 뛰었을 수 있음) 생성만
if [ ! -f "$my_lock" ]; then
  mkdir -p "$locks_dir"
  branch="$(git -C "$cwd" branch --show-current 2>/dev/null || echo 'unknown')"
  {
    echo "session_id=$session_id"
    echo "pwd=$cwd"
    echo "branch=$branch"
    echo "started_at=$now"
    echo "last_heartbeat=$now"
  } > "$my_lock"
  exit 0
fi

# heartbeat 갱신
sed -i.bak "s|^last_heartbeat=.*|last_heartbeat=$now|" "$my_lock" 2>/dev/null && rm -f "${my_lock}.bak"

# 다른 활성 세션 (같은 pwd 만 경고 대상)
five_min_ago_epoch="$(date -u -d '5 minutes ago' +%s 2>/dev/null || echo 0)"
if [ "$five_min_ago_epoch" = "0" ]; then
  five_min_ago_epoch="$(( $(date -u +%s) - 300 ))"
fi

for f in "$locks_dir"/*.lock; do
  [ -f "$f" ] || continue
  [ "$f" = "$my_lock" ] && continue

  other_pwd="$(grep -m1 '^pwd=' "$f" | cut -d= -f2- || true)"
  other_hb="$(grep -m1 '^last_heartbeat=' "$f" | cut -d= -f2- || true)"
  other_branch="$(grep -m1 '^branch=' "$f" | cut -d= -f2- || true)"

  [ -z "$other_hb" ] && continue

  other_epoch="$(date -u -d "$other_hb" +%s 2>/dev/null || echo 0)"
  if [ "$other_epoch" = "0" ]; then
    other_epoch="$(date -u -j -f '%Y-%m-%dT%H:%M:%SZ' "$other_hb" +%s 2>/dev/null || echo 0)"
  fi
  [ "$other_epoch" -lt "$five_min_ago_epoch" ] && continue

  # 같은 pwd 인 경우만 힌트 주입 (다른 worktree 는 조용히)
  if [ "$other_pwd" = "$cwd" ]; then
    echo "⚠️ 병렬 세션 경고 — 같은 pwd 에 다른 세션 활성: branch=$other_branch, 마지막 활동=$other_hb. 파일 편집 시 충돌 위험. worktree 사용 권장 (cw <branch>)."
  fi
done

exit 0
