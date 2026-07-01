#!/usr/bin/env bash
# 사용자용 헬퍼: 새 worktree 자동 생성 + cd + claude 실행
#
# 사용:
#   cw <branch>              # 기존 branch 로 worktree
#   cw -b <new-branch>       # 새 branch 생성 + worktree
#   cw                       # 인자 없이 대화형 (사용 가능한 branch 목록 표시)
#
# 설치:
#   ~/.bashrc 또는 ~/.zshrc 에 아래 alias 추가:
#     alias cw='bash C:/Users/User/IdeaProjects/youth-moa-java/.claude/scripts/claude-work.sh'
#   Mac 개인 PC 에선 경로를 ~/IdeaProjects/youth-moa-java/... 로 조정.

set -euo pipefail

# repo 루트 (이 스크립트 위치에서 3단계 상위)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$REPO_ROOT" || exit 1

# 인자 파싱
create_new=0
branch=""
if [ $# -eq 0 ]; then
  echo "사용 가능한 브랜치:"
  git branch --format='  %(refname:short)' | grep -v '^\* ' | head -20
  echo ""
  read -r -p "worktree 로 열 브랜치 (또는 -b 새브랜치명): " input
  set -- $input
fi

if [ "${1:-}" = "-b" ]; then
  create_new=1
  branch="${2:-}"
  if [ -z "$branch" ]; then
    echo "❌ -b 뒤에 브랜치명 필요"
    exit 1
  fi
else
  branch="${1:-}"
fi

if [ -z "$branch" ]; then
  echo "❌ 브랜치명 필요"
  exit 1
fi

# worktree 경로 결정: <repo>-<branch에서-슬래시-를-대시-로>
safe_name="$(echo "$branch" | sed 's|/|-|g' | sed 's|[^a-zA-Z0-9._-]|-|g')"
parent_dir="$(dirname "$REPO_ROOT")"
base_name="$(basename "$REPO_ROOT")"
wt_path="$parent_dir/${base_name}-${safe_name}"

# 이미 있으면 그리로 cd
if [ -d "$wt_path" ]; then
  echo "✅ worktree 이미 존재: $wt_path"
  echo "   cd 후 claude 실행..."
  cd "$wt_path"
  exec claude
fi

# fetch 로 원격 최신
echo "→ git fetch origin..."
git fetch origin --quiet || true

# worktree 생성
if [ "$create_new" = "1" ]; then
  echo "→ 새 브랜치 '$branch' 생성 + worktree add..."
  if git worktree add -b "$branch" "$wt_path" 2>&1; then
    echo "✅ worktree 생성 완료: $wt_path"
  else
    echo "❌ worktree 생성 실패"
    exit 1
  fi
else
  # 기존 브랜치 확인
  if git show-ref --verify --quiet "refs/heads/$branch" 2>/dev/null; then
    # 로컬 브랜치 존재
    echo "→ 기존 로컬 브랜치 '$branch' 로 worktree add..."
    if git worktree add "$wt_path" "$branch" 2>&1; then
      echo "✅ worktree 생성 완료: $wt_path"
    else
      echo "❌ worktree 생성 실패 (같은 브랜치 이미 다른 worktree 에서 checkout 됐을 수 있음)"
      exit 1
    fi
  elif git show-ref --verify --quiet "refs/remotes/origin/$branch" 2>/dev/null; then
    # 원격만 존재 → track 하여 checkout
    echo "→ 원격 브랜치 'origin/$branch' 를 track 하여 worktree add..."
    if git worktree add -b "$branch" "$wt_path" "origin/$branch" 2>&1; then
      echo "✅ worktree 생성 완료: $wt_path"
    else
      echo "❌ worktree 생성 실패"
      exit 1
    fi
  else
    echo "❌ 브랜치 '$branch' 로컬·원격 모두 없음. 새로 만들려면 -b 옵션 사용."
    exit 1
  fi
fi

# 이동 + claude 실행
echo ""
echo "→ cd $wt_path"
cd "$wt_path"
echo "→ claude 실행..."
echo ""
exec claude
