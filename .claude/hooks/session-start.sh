#!/usr/bin/env bash
# SessionStart hook — 세션 시작 시 자동 실행
# 목적:
#   1. 같은 working directory 에서 이미 다른 세션 활성 중인지 감지
#   2. 감지 시 별도 worktree 를 자동 생성하고 사용자에게 이동 명령 안내
#   3. 자기 lock 파일 생성 (heartbeat 방식)
#
# 락 파일 위치: $(git rev-parse --git-common-dir)/session-locks/${session_id}.lock
#   - .git/ 아래이므로 worktree 간 공유되고 gitignore 자동
#
# 입력 (stdin): {"hook_event_name":"SessionStart","session_id":"...","cwd":"...","transcript_path":"..."}

set -euo pipefail

# 하드 종료 안 함. 정보만 stderr 로 (Claude UI 에 표시). 성공 exit 0.

input="$(cat)"
session_id="$(printf '%s' "$input" | grep -oE '"session_id"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"session_id"[[:space:]]*:[[:space:]]*"//; s/"$//')"
cwd="$(printf '%s' "$input" | grep -oE '"cwd"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"cwd"[[:space:]]*:[[:space:]]*"//; s/"$//')"

# session_id 없으면 무해히 종료
if [ -z "${session_id:-}" ]; then
  exit 0
fi

# cwd 미제공 시 현재 pwd 사용
if [ -z "${cwd:-}" ]; then
  cwd="$(pwd)"
fi

# git repo 아니면 무해히 종료
if ! git_common_dir="$(git -C "$cwd" rev-parse --git-common-dir 2>/dev/null)"; then
  exit 0
fi

# 상대경로 반환 시 절대경로로 변환
case "$git_common_dir" in
  /*) ;;
  *)  git_common_dir="$cwd/$git_common_dir" ;;
esac

locks_dir="$git_common_dir/session-locks"
mkdir -p "$locks_dir"

now="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
branch="$(git -C "$cwd" branch --show-current 2>/dev/null || echo 'unknown')"
my_lock="$locks_dir/${session_id}.lock"

# ── 1. 다른 활성 세션 스캔 ─────────────────────────────────────────────
# fresh 기준: 마지막 heartbeat 가 5분(300초) 이내
five_min_ago_epoch="$(date -u -d '5 minutes ago' +%s 2>/dev/null || echo 0)"
if [ "$five_min_ago_epoch" = "0" ]; then
  # BSD date (Mac) 호환
  five_min_ago_epoch="$(( $(date -u +%s) - 300 ))"
fi

conflict_lock=""
active_others=()

for f in "$locks_dir"/*.lock; do
  [ -f "$f" ] || continue
  [ "$f" = "$my_lock" ] && continue

  other_pwd="$(grep -m1 '^pwd=' "$f" | cut -d= -f2- || true)"
  other_hb="$(grep -m1 '^last_heartbeat=' "$f" | cut -d= -f2- || true)"
  other_branch="$(grep -m1 '^branch=' "$f" | cut -d= -f2- || true)"
  other_sid="$(grep -m1 '^session_id=' "$f" | cut -d= -f2- || true)"

  [ -z "$other_hb" ] && continue

  # ISO → epoch (Linux: date -d, Mac: date -jf 필요. Portable 하게 파싱 시도)
  other_epoch="$(date -u -d "$other_hb" +%s 2>/dev/null || echo 0)"
  if [ "$other_epoch" = "0" ]; then
    # Mac fallback
    other_epoch="$(date -u -j -f '%Y-%m-%dT%H:%M:%SZ' "$other_hb" +%s 2>/dev/null || echo 0)"
  fi

  # stale 이면 정리
  if [ "$other_epoch" -lt "$five_min_ago_epoch" ]; then
    rm -f "$f"
    continue
  fi

  active_others+=("$other_sid|$other_pwd|$other_branch|$other_hb")

  # 같은 pwd → 충돌
  if [ "$other_pwd" = "$cwd" ]; then
    conflict_lock="$f"
  fi
done

# ── 2. 자기 lock 파일 생성 ─────────────────────────────────────────────
{
  echo "session_id=$session_id"
  echo "pwd=$cwd"
  echo "branch=$branch"
  echo "started_at=$now"
  echo "last_heartbeat=$now"
} > "$my_lock"

# ── 3. 충돌 감지 시 자동 worktree 생성 시도 ────────────────────────────
if [ -n "$conflict_lock" ]; then
  # 다음 사용 가능한 worktree 번호
  base_name="$(basename "$cwd")"
  parent_dir="$(dirname "$cwd")"
  n=2
  while [ -e "$parent_dir/${base_name}-${n}" ]; do
    n=$((n+1))
  done
  new_wt_path="$parent_dir/${base_name}-${n}"

  # worktree 자동 생성: 현재 branch 를 그대로 checkout 하는 게 아니라 임시 브랜치
  # (같은 브랜치 두 worktree 는 불가). 사용자가 나중에 브랜치 지정 원하면 수동.
  temp_branch="worktree-session-${session_id:0:8}"

  cat >&2 <<EOF

╔══════════════════════════════════════════════════════════════════════╗
║  ⚠️  DUAL SESSION DETECTED — 같은 작업 폴더에 세션 2개 시작 감지     ║
╠══════════════════════════════════════════════════════════════════════╣
║  기존 세션 pwd:    $cwd
║  기존 세션 branch: (다른 lock 파일 참조)
║
║  💡 권장: 별도 worktree 에서 작업하시면 파일 충돌 zero.
║
║  자동 worktree 생성 시도 중...
╚══════════════════════════════════════════════════════════════════════╝

EOF

  # worktree 자동 생성 (임시 브랜치 사용 — 사용자가 원하면 checkout 변경)
  if git -C "$cwd" worktree add -b "$temp_branch" "$new_wt_path" 2>/dev/null; then
    cat >&2 <<EOF
✅ Worktree 자동 생성 완료:
   경로: $new_wt_path
   임시 branch: $temp_branch

💡 다음 명령으로 새 worktree 에서 Claude Code 재시작:

   cd "$new_wt_path" && claude

원하시는 작업 브랜치가 따로 있다면 새 worktree 안에서:
   git checkout <branch-name>

⚠️  이 세션은 그대로 유지됩니다 (read-only 확인 등 목적이면 계속 사용 OK).
    파일 수정 시엔 새 worktree 로 이동 강력 권장.

EOF
  else
    cat >&2 <<EOF
❌ Worktree 자동 생성 실패 (이미 존재하거나 권한 문제).
   수동 처리: git worktree add ../${base_name}-alt <branch-name>
EOF
  fi
else
  # 충돌 없음. 다른 세션 정보 참고용으로 표시 (있으면)
  if [ "${#active_others[@]}" -gt 0 ]; then
    echo "ℹ️  활성 세션 ${#active_others[@]}개 (다른 worktree — 파일 충돌 없음):" >&2
    for entry in "${active_others[@]}"; do
      IFS='|' read -r o_sid o_pwd o_branch o_hb <<< "$entry"
      echo "   • ${o_sid:0:8}... pwd=$o_pwd branch=$o_branch" >&2
    done
  fi
fi

exit 0
