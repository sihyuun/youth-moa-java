---
name: memory-sync
description: youth-moa-java 의 main 브랜치 최근 git log + 열린 PR + 다음 작업 큐 + 사용자 결정 이력을 종합해 `project_youth_moa_java.md` 메모리를 자동 갱신. 매 wrap-up 직후 호출 권장.
disable-model-invocation: true
---

사용자가 `/memory-sync` 를 입력하면 아래 절차를 실행한다.

- `/memory-sync` — 자동 갱신 (직전 PR 머지 후)
- `/memory-sync --preview` — 변경안 미리보기만 (실제 write X)

---

## Step 1 — 사실 수집

```powershell
cd C:\Users\User\IdeaProjects\youth-moa-java

# main 의 최근 머지 PR 10개
git log main --oneline -10 --first-parent

# 열린 PR
gh pr list --base main --state open

# 작업 트리 / 진행 중 브랜치
git status -s
git branch --show-current
git branch -vv | grep -v "main"
```

## Step 2 — 메모리 읽기

```
~/.claude/projects/C--Users-User/memory/project_youth_moa_java.md
~/.claude/projects/C--Users-User/memory/MEMORY.md
```

핵심 섹션:
- "마지막 갱신" 날짜
- "🟢 N 일자 세션 진행 완료" 블록
- "🔴 진행 중 브랜치 / 열린 PR" 블록
- "다음 작업 큐"
- "사용자 결정 이력 (Q 형식)"

## Step 3 — 갱신 대상 식별

다음을 비교해 변경 항목 찾음:
1. **마지막 갱신 날짜 vs 오늘** → 다르면 새 세션 블록 추가
2. **최근 머지된 PR (Step 1) vs 메모리 기록** → 누락 PR 추가
3. **현재 작업 큐 우선순위 vs 진행된 작업** → 완료 항목 제거 / 다음 항목 자동 승격
4. **사용자 결정 이력** → 이번 세션에서 새 결정 (Q-X) 있으면 추가

## Step 4 — 사용자 확인

```markdown
## /memory-sync 갱신안

### 추가
- [날짜] 세션 블록 — 머지 PR #N1, #N2, #N3 + 인프라 변경 X·Y
- 사용자 결정 Q-X-1, Q-X-2 ...

### 제거 (완료/스테일)
- 진행 중 브랜치 `<...>` (이미 머지됨)
- 작업 큐 ① `<...>` (PR #N 머지로 완료)

### 작업 큐 재정렬
- 다음 순서: ① F0e → ② F0g → ...

이대로 메모리 적용할까요? (Y/n / 수정)
```

## Step 5 — 실제 write (사용자 Y 시)

`Edit` 도구로 `project_youth_moa_java.md` 갱신:
- "마지막 갱신" 한 줄
- 세션 블록 (최상단에 prepend)
- 진행 중 브랜치 / 열린 PR 블록
- 다음 작업 큐 표 재작성
- 사용자 결정 이력 (Q 형식 모음)

`MEMORY.md` 인덱스 라인도 동기화 (한 줄 요약).

## Step 6 — 보고

```markdown
## /memory-sync 완료

- 갱신 파일: project_youth_moa_java.md / MEMORY.md
- 추가 N 줄 / 제거 M 줄
- 마지막 갱신 → YYYY-MM-DD

## 다음 작업 큐 (top 3)
1. ...
2. ...
3. ...
```

---

## 자동 호출 시나리오

- `/wrap-up` 완료 직후 사용자가 `/memory-sync` 호출 권장 (한 단계 추가, ~30초)
- 또는 세션 종료 직전 한 번
- ❌ ym-impl / ym-spec 중 자동 호출 X (작업 흐름 깨짐)

## 갱신 정책

### 추가하는 것
- 새 머지 PR 정보 (번호 + commit hash + 짧은 제목)
- 사용자가 명시한 결정 (Q-X 형식)
- 새 발견된 기술 부채 (작업 큐에 등재)
- 인프라 / 패턴 변경 (예: ym-* 에이전트 추가, Skill 신설)

### 추가 안 하는 것
- 코드 패턴 (CLAUDE.md 영역)
- 일반적 git 흐름 (이미 컨벤션 문서화)
- 임시 디버그 정보

### 제거 대상
- 머지 완료된 진행 중 브랜치
- 완료된 작업 큐 항목
- 옛 결정 (Q-X) 중 prototype 변경 등으로 무효화된 것 — 사용자 확인 후

## 메모리 규칙 준수

- ✅ 한 번에 큰 변경 X — 사용자 확인 게이트
- ✅ 사실 기반 (git log·gh pr list 가 원천)
- ❌ 추측 X
