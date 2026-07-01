# 세션 동시 작업 조율 가이드

Claude Code 세션 2개 이상을 병렬로 사용하는 상황(예: 같은 PC 에서 여러 터미널, 회사·개인 PC 동시)에서 **파일 편집 충돌·git checkout 뒤엉킴** 을 방지하는 규약과 자동화.

## 핵심 원칙

**같은 폴더에 두 세션을 열지 말 것.** 각 세션은 자기만의 worktree 폴더에서 실행되어야 한다.

## Git Worktree 개념 요약

- **브랜치** = git 이력의 이름표 (논리적)
- **worktree** = 어떤 브랜치의 파일들이 실제로 펼쳐진 폴더 (물리적)
- 일반적으로 repo 하나 = 폴더 하나 = 한 번에 한 브랜치 checkout
- **worktree add** 로 폴더를 여러 개 만들어 서로 다른 브랜치를 동시에 파일 시스템에 펼쳐 놓을 수 있음
- `.git/` 은 모든 worktree 가 공유 → fetch·push 는 한 번으로 반영

## 자동화 계층

### 1. SessionStart hook 자동 감지 (`.claude/hooks/session-start.sh`)

새 Claude Code 세션 시작 시 자동 실행. 다음을 수행:

1. 자기 세션 lock 파일 생성 (`$(git rev-parse --git-common-dir)/session-locks/<sid>.lock`)
2. 다른 활성 세션 스캔 (heartbeat 5분 이내)
3. 같은 pwd 에 다른 세션 발견 시:
   - 경고 배너 출력
   - **자동으로 worktree 생성** (`../<repo>-2/`, 다음 번호 자동)
   - 다음 명령 안내: `cd <새경로> && claude`
4. 다른 worktree 의 세션은 정보로만 표시 (충돌 아님)

**참고**: 이 세션 자체를 강제 종료하지 않음. 사용자가 read-only 확인 등 목적으로 계속 사용해도 됨. 파일 편집 시엔 반드시 새 worktree 로.

### 2. UserPromptSubmit hook 하트비트 (`.claude/hooks/user-prompt-heartbeat.sh`)

매 프롬프트 시 자동 실행. `last_heartbeat` 갱신. 다른 세션이 같은 pwd 에서 활동 중이면 프롬프트에 컨텍스트 힌트 주입.

### 3. Stop hook 정리 (`.claude/hooks/session-stop.sh`)

세션 종료 시 자기 lock 제거 + stale lock 청소 (5분+ 무응답).

### 4. 사용자용 헬퍼 (`.claude/scripts/claude-work.sh`)

**alias 설치**:

`.bashrc` (Windows Git Bash) 또는 `~/.zshrc` (Mac) 에 추가:
```bash
alias cw='bash /c/Users/User/IdeaProjects/youth-moa-java/.claude/scripts/claude-work.sh'
# Mac 개인 PC:
# alias cw='bash ~/IdeaProjects/youth-moa-java/.claude/scripts/claude-work.sh'
```

**사용법**:

```bash
cw feature/F0e-home-prototype      # 기존 브랜치 worktree 로 열기
cw -b feature/new-work             # 새 브랜치 만들면서 worktree
cw                                 # 대화형 (사용 가능 브랜치 목록 표시)
```

자동으로:
1. worktree 폴더 생성 (`../<repo>-<branch명>/`)
2. 원격 최신 fetch
3. 그 폴더로 cd
4. claude 실행

**전체 흐름 (사용자 개입 1 명령)**:
```
새 터미널 열기 → cw <브랜치>
```

## 락 파일 위치

- 경로: `.git/session-locks/<session_id>.lock`
- `.git/` 아래이므로 **자동으로 gitignore** (커밋 대상 아님)
- **모든 worktree 가 같은 `.git/` 을 공유** → lock 파일도 공유
  - 예: `youth-moa-java/.git/` 와 `youth-moa-java-2/.git` 은 하나의 물리적 저장소를 가리킴
  - 어느 worktree 에서 실행되든 같은 `session-locks/` 디렉토리를 참조

## Windows / Mac 호환

- 스크립트는 Bash 기반. Windows 는 Git Bash, Mac 은 기본 shell 에서 동작.
- `date -u -d 'X ago'` (GNU) 와 `date -u -j -f ...` (BSD/Mac) 양쪽 fallback 처리됨.

## Wrap-up 시 worktree 정리

브랜치 머지·삭제 후:
```bash
git worktree remove ../youth-moa-java-<safe-name>
git worktree prune
```

`/wrap-up` skill 은 자동으로 worktree 정리를 시도하도록 확장 예정.

## 트러블슈팅

| 증상 | 원인 | 해결 |
|---|---|---|
| `cw` 명령 인식 안 됨 | alias 미등록 | `.bashrc` / `.zshrc` 재로드 (`source ~/.bashrc`) |
| worktree 생성 실패 "already checked out" | 같은 브랜치를 다른 worktree 에서 이미 checkout | 그 worktree 로 이동해 사용하거나 다른 브랜치 지정 |
| 락 파일이 계속 남음 | Stop hook 실행 안 됨 (강제 종료 등) | stale 5분 후 다음 SessionStart 가 자동 청소 |
| lock 파일 경로가 없다는 에러 | git repo 아닌 곳에서 세션 시작 | 무해히 무시됨 (hook 이 자동 exit) |

## 미래 개선

- SessionEnd 강제 종료 대응: `.claude/session-locks/` 를 pid 별 flock 로 관리
- 락 파일에 `active_files` 필드 추가 → 어떤 파일 편집 중인지 세밀 감지
- 크로스 PC 감지: 락 파일에 `hostname` 추가하여 Win↔Mac 세션도 조율 (Notion 페이지 or GitHub Discussions 통해)
