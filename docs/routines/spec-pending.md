# Spec Pending 루틴 프롬프트

- **cron**: `40 23 * * *` UTC = 매일 KST 08:40
- **모델**: claude-sonnet-4-6
- **MCP**: Notion
- **allowed_tools**: Read, Bash, Glob, Grep, (Notion search / fetch / create-pages)

---

## 프롬프트

어제 PM Review 에서 나온 결정 항목이 오늘 spec 착수로 이어졌는지 확인하고 Notion 에 리포트를 생성하라.

### 1. KST 어제 날짜 계산

```bash
date -u
```

UTC + 9h = KST. 어제 KST 날짜 `MM-DD` 확정.

### 2. 어제 PM Review 페이지 조회

`mcp__claude_ai_Notion__notion-search` 로 검색:
- query: `MM-DD PM Review` (어제 날짜)
- 상위 페이지: `38fc33520d0e80b0bdddc3b2a430fb94`

결과에서 어제 PM Review 페이지 ID 확보. 없으면 `어제 PM Review 페이지 없음` 명시하고 종료 리포트만 생성.

### 3. 어제 페이지 정독

`mcp__claude_ai_Notion__notion-fetch` 로 어제 페이지 fetch. 아래 두 섹션 추출:
- **5. 권장 + 결정 요청** → 사용자가 결정 필요한 Q 목록
- **6. ym-spec 인계 메모** → 예상 브랜치명·범위

### 4. 오늘 spec 착수 상태 확인

각 결정 항목별로:

```bash
# 예상 브랜치가 원격에 있는지
git ls-remote origin refs/heads/<branch-name>

# 로컬 최근 커밋에서 관련 파일 변경 있는지 (24h)
git log --since="24 hours ago" --name-only --oneline
```

- 브랜치 존재 → **착수됨** ✅
- 브랜치 없음 + 결정도 미확정 → **결정 대기** 🟡
- 브랜치 없음 + 결정은 됨 → **spec 대기** 🔴 (사용자 액션 필요)

### 5. STATE.md 대조

`docs/STATE.md` 의 "다음 작업 큐" 를 로드해 어제 대상이 여전히 큐에 있는지 확인. 이미 제거됐으면 `완료 반영됨` 표시.

### 6. Notion 페이지 생성

- `parent`: `{ "type": "page_id", "page_id": "38fc33520d0e80b0bdddc3b2a430fb94" }`
- `title`: `MM-DD Spec Pending` (오늘 KST)
- `icon`: `📐`
- `content`:

```markdown
> 📌 **한눈에**
> 1. 어제 결정 N건 중 착수 M건 / 대기 K건
> 2. (가장 중요한 대기 항목 한 줄)
> 3. (오늘 사용자가 해야 할 액션 한 줄)

## 어제 PM Review 요약
- 페이지: [MM-DD PM Review — <대상>](url)
- 결정 요청 항목: N 개

## 착수 상태

| 항목 | 결정 | 브랜치 존재 | 상태 |
|---|---|---|---|
| Q5 (예: Hero 검색바) | ✅ 결정됨 | ✅ feature/F0e | ✅ 착수 |
| Q6 (...) | 🟡 대기 | ❌ | 🟡 결정 대기 |
| Q7 (...) | ✅ 결정됨 | ❌ | 🔴 spec 대기 |

## 오늘 사용자 액션
- 🔴 spec 대기 N건 → ym-spec 호출 권장
- 🟡 결정 대기 K건 → PM Review 페이지 재검토

## 참고
- 어제 페이지 링크
- 현재 브랜치 상태 (git branch -vv 요약)
```

### 7. 에러 처리

- 어제 페이지 없음 → 빈 리포트에 `어제 PM Review 없음 — 최초 실행이거나 루틴 실패` 명시
- Notion search 오류 → search 결과 없이 STATE.md 만으로 리포트

### 8. 결과 출력

Notion 페이지 URL 만 마지막 줄에 출력.
