# 2026-07-06 — `&&` 체인 파괴적 git 명령으로 로컬 커밋 소실

> CLAUDE.md 에서 분리 (2026-07-28). 규칙 요약은 CLAUDE.md 「Git 브랜치·커밋 컨벤션」에 남아 있다.

**배경**: 2026-07-06 F0i 세션에서 `git checkout main && git reset --hard origin/main` 을 F0i 브랜치에서 실행. `checkout main` 이 미스테이시 파일로 실패했으나 `&&` 다음 명령 (reset --hard) 이 여전히 F0i 브랜치에서 실행되어 **로컬 F0i 커밋이 origin/main 으로 초기화**됨. 원격에 push 된 상태라 `git reset --hard <sha>` 로 복구 가능했지만, 다음 재발 방지 규칙:

- ❌ **파괴적 명령을 `&&` 체인으로 실행 금지**:
  - `git reset --hard`, `git clean -f`, `git checkout -- .`, `git branch -D` 등
  - 체인 앞 명령이 fail 하면 뒤 명령이 **의도하지 않은 브랜치·상태에서 실행**됨
- ✅ **각 파괴적 명령 이전에 상태 검증**:
  ```bash
  git status --short           # 현재 브랜치·미커밋 파일 확인
  git branch --show-current    # 현재 브랜치 이름 재확인
  git reset --hard origin/main # 그 다음에만 실행
  ```
- ✅ **체이닝이 필요하면 `set -e` 또는 명시적 실패 감지**:
  ```bash
  git checkout main && git reset --hard origin/main
  # 이 형태는 체크아웃 실패 시 reset 이 뒤 브랜치에서 실행됨
  # → 대신 아래 형태:
  git checkout main || { echo "checkout failed"; exit 1; }
  git reset --hard origin/main
  ```
- ✅ **파괴적 명령 실행 전 로컬 커밋 원격 존재 확인**: `git log --oneline @{u}..HEAD` 로 unpushed 커밋 있는지. 있으면 push or 우회
- 사고 발생 시 첫 조치: **reflog 조회** (`git reflog -20`) — 최근 HEAD 이동 이력에서 사고 이전 SHA 확인 → `git reset --hard <sha>` 로 복구
