---
name: wrap-up
description: youth-moa-java 의 미커밋 변경을 한 흐름으로 commit·push·PR 생성·squash merge·main pull·prune 까지 자동 처리. 매 작업 사이클 종료 시 호출. 메모리 daily_commit 규칙의 표준 wrap-up 진입점.
disable-model-invocation: true
---

사용자가 `/wrap-up <식별자>` 또는 `/wrap-up` 만 입력하면 아래 절차를 순차 실행한다.

- `/wrap-up <식별자>` — commit 메시지 식별자 명시 (예: `/wrap-up F0f_filter_redesign`)
- `/wrap-up` — 현재 브랜치 이름에서 자동 추출 (예: `feature/F0f-list-filter` → `F0f_list_filter`)

---

## Step 1 — 사전 점검

```powershell
cd C:\Users\User\IdeaProjects\youth-moa-java
git branch --show-current
git status -s
git log --oneline -3
```

- 현재 브랜치 main 이면 즉시 중단·보고 ("main 직접 변경 금지")
- 변경 없음 (`git status` 빈 결과) → 중단
- 변경 파일 사용자에게 리스트 보고 → 확인 받음 ("이대로 wrap-up 진행할까요?")

## Step 2 — 정적 + E2E 검증 (선택, `--no-verify` 옵션으로 스킵 가능)

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.14"
.\gradlew.bat compileJava 2>&1 | Select-String -Pattern "BUILD|error:|FAILED"
```

```bash
# bootRun 동작 중이면 Playwright 회귀 (5초 대기 후)
sleep 5
cd e2e && npx playwright test --reporter=line 2>&1 | tail -5
```

- 실패 시 중단·보고. wrap-up 강제 진행은 사용자가 `--no-verify` 옵션 명시 시만.

## Step 3 — 명시적 staging

❌ `git add -A` / `git add .` 금지 (메모리 규칙)
✅ Step 1 의 변경 파일을 명시적으로 add. 파일 목록 사용자에게 보여줌.

```powershell
git add <file1> <file2> ...
git status -s
```

## Step 4 — Commit (CLAUDE.md 컨벤션)

자동 생성 메시지 형식:
```
YYMMDD_<식별자> - <요약 한 줄>

- <변경 항목 1>
- <변경 항목 2>
- ...

## 정적 검증
- compileJava SUCCESS [+ test N TC PASS]

## 동적 검증
- <bootRun 결과 또는 "bootRun 미기동">

## E2E 검증
- Playwright N/M PASS [or "스킵"]

Co-Authored-By: Claude <noreply@anthropic.com>
```

- 요약 한 줄: 사용자에게 받거나, Step 1 의 변경 파일 패턴에서 자동 유추 (예: signup.html 변경 다수 → "회원가입 폼 ...")
- 항목 리스트: 변경 파일별 1줄 요약 (max 5~7개)
- HEREDOC 으로 전달:

```bash
git commit -m "$(cat <<'EOF'
<위 형식>
EOF
)"
```

## Step 5 — Push

```bash
git push -u origin <현재 브랜치>
```

첫 push 시 `-u` 자동. 이후 push 면 `-u` 생략 OK.

## Step 6 — PR 생성

```bash
gh pr create --base main --title "<PR 제목>" --body "$(cat <<'EOF'
<PR 본문>
EOF
)"
```

- PR 제목: commit 의 "요약 한 줄" 재사용 (또는 `[<식별자>] <요약>`)
- PR 본문: commit 본문에서 검증 섹션 + 변경 요약 발췌

## Step 7 — Squash merge + branch 삭제

```bash
gh pr merge <번호> --squash --delete-branch
```

PR 번호는 Step 6 의 출력에서 추출.

## Step 8 — main 동기화 + prune

```bash
git checkout main
git pull origin main
git fetch --prune origin
```

## Step 9 — 보고

```markdown
## /wrap-up 완료

- PR #N merged → <commit hash>
- 변경 N 파일
- main 최신 동기화
- prune 완료

## 다음 단계
- 작업 큐의 다음 항목: <메모리 참조>
```

---

## 옵션

| 옵션 | 동작 |
|---|---|
| `/wrap-up <id>` | 식별자 명시 + 검증 포함 |
| `/wrap-up` | 브랜치명에서 식별자 자동 |
| `/wrap-up --no-verify` | Step 2 (compileJava + Playwright) 스킵 — 학습용 비추 |
| `/wrap-up --dry-run` | Step 4 까지만 (commit 만 만들고 push X) — 검토 용도 |

## 충돌·실패 시

- 컴파일 실패 → 중단 + 오류 라인 보고
- Playwright 실패 → 중단 + 실패 spec 보고
- Push 실패 (다른 PC 변경 conflict) → `git fetch origin && git rebase origin/<branch>` 안내
- gh pr merge 실패 (충돌·CI 실패) → 사용자에게 PR URL 전달 + 수동 해결 요청

## 메모리 규칙 준수

- ❌ `git add -A` 금지
- ✅ 사용자가 wrap-up 키워드 명시한 시점만 호출 (자동 호출 X)
- ✅ commit 전 파일 목록 사용자 확인
- ✅ 정적·동적·E2E 검증 분리 표기

## 다른 Skill / Agent 연계

- 매 PR 머지 후 호출 권장 → `/memory-sync` 로 작업 큐 자동 갱신
- ym-qa 가 검증 통과 후 추천 호출
- 사이클 종료 마커 — 이 Skill 호출 시점부터 다음 cycle (ym-pm or pm-review) 진입 가능
