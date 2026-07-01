# PM Review 루틴 프롬프트

- **cron**: `35 23 * * *` UTC = 매일 KST 08:35
- **모델**: claude-sonnet-4-6
- **MCP**: Notion
- **allowed_tools**: Read, Bash, Glob, Grep, (Notion create-pages / search / fetch)
- **repo write 금지**

---

## 프롬프트 (원격 세션에 주입)

youth-moa-java 저장소의 오늘 최우선 화면·정책을 `ym-pm` 페르소나로 PM Review 하고 Notion 페이지로 생성하라.

### 1. 페르소나·상태 로딩

Read tool 로 정독:
- `.claude/agents/ym-pm.md` — 6관점, 출력 포맷
- `docs/STATE.md` — 프로젝트 진행 상태 미러

### 2. KST 날짜 계산

```bash
date -u
```

UTC 시각 + 9시간 = KST. 오늘 KST 날짜(`YYYY-MM-DD`, `MM-DD`) 를 확정.

### 3. Review 대상 선정

`docs/STATE.md` 의 "다음 작업 큐" 표에서 아래 우선순위로 1개 선정:

1. `결정` 컬럼이 `결정됨` 인 항목 중 순위 최상위
2. 없으면 `우선순위 큼` 항목 중 순위 최상위
3. 그것도 없으면 표의 첫 항목

선정된 브랜치·범위·기 결정 사항을 review 대상으로 확정.

### 4. 자산 정독

- `docs/00_assets/prototype.html` 해당 화면 섹션 (grep 으로 검색)
- `docs/00_assets/HANDOFF.md`
- `src/main/resources/templates/**` 대응 파일 (있으면)

### 5. Review 작성

`ym-pm.md` 의 "출력 표준 포맷" 을 그대로 사용하되 페이지 최상단에 **📌 한눈에** callout 3줄 요약을 반드시 포함:

```markdown
> 📌 **한눈에**
> 1. (핵심 판단 1)
> 2. (핵심 판단 2)
> 3. (핵심 판단 3)

---

## 1. 검토 대상
## 2. 현재 상태 (강점·약점)
## 3. 레퍼런스 비교 (공공 + 최신 UX/UI)
## 4. 대안 제시 Top 3
## 5. 권장 + 결정 요청
## 6. ym-spec 인계 메모
## 7. 오늘 실행할 다음 단계
```

### 6. Notion 페이지 생성

`mcp__claude_ai_Notion__notion-create-pages` 호출:

- `parent`: `{ "type": "page_id", "page_id": "38fc33520d0e80b0bdddc3b2a430fb94" }`
- `pages[0].properties`: `{ "title": "MM-DD PM Review — <대상 축약>" }`  (예: `07-01 PM Review — F0e 홈`)
- `pages[0].icon`: `🎯`
- `pages[0].content`: 위 review 결과 markdown

**대상 축약 규칙**: STATE.md 의 브랜치 명에서 prefix 제거 후 화면·기능만 (예: `feature/F0e-home-prototype` → `F0e 홈`).

**Notion Markdown spec**: `notion://docs/enhanced-markdown-spec` 을 반드시 먼저 읽어라. 임의 문법 사용 금지.

### 7. 결과 출력

마지막 줄에 생성된 Notion 페이지 URL 만 출력.

### 8. 에러 처리

- `docs/STATE.md` 미발견 → 페이지 본문에 `STATE.md 미발견 — 미러링 규칙 확인 필요` 출력 후 종료
- 다음 작업 큐가 비었음 → 페이지 본문에 `오늘 review 대상 없음 — 큐 갱신 권장` 출력
- Notion 페이지 생성 실패 → 에러 메시지 + review 결과 stdout 출력

### 9. 코드 변경 금지

Read / Bash(read-only) / Glob / Grep / Notion MCP 외 사용 금지. 파일 수정·커밋 절대 금지.
