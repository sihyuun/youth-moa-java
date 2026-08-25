# PR Health 루틴 프롬프트

- **cron**: `45 23 * * *` UTC = 매일 KST 08:45
- **모델**: claude-sonnet-4-6
- **MCP**: Notion
- **allowed_tools**: Read, Bash, Glob, Grep, WebFetch, (Notion create-pages)

**GitHub 인증**: claude.ai 의 "GitHub 연동" (1st-party integration) 이 활성화되어 있으므로 CCR 세션에서 `gh` CLI 가 사전 인증되어 있어야 정상. 미인증 시 아래 3단 fallback 수행.

---

## 프롬프트

youth-moa-java 저장소의 열린 PR·최근 머지·CI 상태를 스캔해 매일 아침 요약 리포트를 Notion 에 생성하라.

### 1. KST 오늘 날짜

```bash
date -u
```
+9h = KST.

### 2. PR·CI 상태 수집

**Priority 1 — gh CLI 사용 가능하면**:

```bash
gh --version 2>&1

# 가능하면
gh pr list --state open --json number,title,headRefName,statusCheckRollup,updatedAt
gh pr list --state merged --limit 5 --json number,title,mergedAt
```

**Priority 2 — gh 인증 실패 시 GITHUB_TOKEN env var 확인 후 REST API**:

```bash
curl -H "Authorization: token $GITHUB_TOKEN" \
  https://api.github.com/repos/sihyuun/youth-moa-java/pulls?state=open

curl -H "Authorization: token $GITHUB_TOKEN" \
  https://api.github.com/repos/sihyuun/youth-moa-java/pulls?state=closed&per_page=5
```

**Fallback — 인증 실패**:
- git log main 최근 10 first-parent 로 최근 머지 이력만 정리
- 열린 PR 정보는 `조회 실패 — GITHUB_TOKEN 미설정` 명시

### 3. STATE.md 의 "진행 중 브랜치 / 열린 PR" 섹션과 대조

`docs/STATE.md` 로드 → 메모리에 기록된 PR 상태 vs 실제 상태 비교. 불일치 항목 표시 (메모리 stale).

### 4. 리포트 구성

각 열린 PR 마다:
- 번호·제목·브랜치
- 마지막 업데이트 (몇 시간/일 전)
- CI 상태 (SUCCESS / FAILURE / PENDING)
- STATE.md 에서 언급된 "시각 확인 필요" 여부

### 5. Notion 페이지 생성

- `parent`: `{ "type": "page_id", "page_id": "38fc33520d0e80b0bdddc3b2a430fb94" }`
- `title`: `MM-DD PR Health`
- `icon`: `🚦`
- `content`:

```markdown
> 📌 **한눈에**
> 1. 열린 PR N건 (신선 M / 정체 K 3일↑)
> 2. (가장 시급한 액션 항목 한 줄 — 예: CI 실패 or 시각 확인 대기)
> 3. 어제 대비 (신규 X / 머지 Y)

## 열린 PR

| # | 제목 | 브랜치 | 업데이트 | CI | 시각확인 |
|---|---|---|---|---|---|
| ... | ... | ... | 2h ago | ✅ | 대기 |

## 최근 머지 (지난 5건)
- #NN (제목) — MM-DD

## STATE.md 불일치
- 메모리엔 PR #7 열림, 실제는 머지됨 → STATE.md 갱신 필요

## 오늘 사용자 액션
- CI 실패 PR: (있으면 목록)
- 시각 확인 대기: (개인 PC 이동 시 처리)
- 정체 PR (3일+): 리마인드
```

### 6. 인증 fallback 리포트

GITHUB_TOKEN 없을 시:

```markdown
> 📌 **한눈에**
> 1. GITHUB_TOKEN 미설정 — API 조회 불가
> 2. git log 기반 최근 머지 정보만 표시
> 3. 이 루틴 정식 활성화 위해 CCR 환경 변수 등록 필요

## 최근 머지 (git log main 첫 부모 10건)
...

## 설정 안내
CCR 환경 변수에 GITHUB_TOKEN 추가 (repo scope) 후 이 루틴 재시도.
```

### 7. 결과 출력

Notion 페이지 URL 만 출력.

### 8. 코드 변경 금지

Read/Bash/Glob/Grep/WebFetch/Notion MCP 만 사용. 파일 수정·커밋 금지.
