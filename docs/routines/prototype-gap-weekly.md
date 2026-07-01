# Prototype Gap 주간 루틴 프롬프트

- **cron**: `55 23 * * 0` UTC = 매주 KST 월요일 08:55
- **모델**: claude-sonnet-4-6
- **MCP**: Notion
- **allowed_tools**: Read, Bash, Glob, Grep, (Notion create-pages / search / fetch)

---

## 프롬프트

youth-moa-java 저장소의 prototype 대비 Thymeleaf 구현 일치도를 화면별로 재평가하고, 저번 주 대비 증감을 계산해 주간 리포트를 Notion 에 생성한다.

### 1. KST 이번 주 월요일 날짜

```bash
date -u
```

UTC 일요일 23:55 = KST 월요일 08:55. 오늘 KST 날짜(월요일) 를 `YYYY-MM-DD` 로 확정. 저번 주 월요일도 계산 (7일 전).

### 2. 자산 정독

- `docs/00_assets/prototype.html` — 각 화면별 섹션 (HomeScreen / ProgramList / ProgramDetail / SignUp / Login / Header / Footer 등)
- `docs/00_assets/HANDOFF.md`
- `docs/STATE.md` "prototype 일치도" 표 (있으면 현재 기준값)
- `src/main/resources/templates/**` 실제 구현

### 3. 화면 목록

STATE.md 표에 있는 화면 기준. 최소 다음 화면들 평가:

- 홈 `/`
- 프로그램 목록 `/programs`
- 프로그램 상세 `/programs/{id}`
- 신청 폼 `/programs/{id}/apply`
- 로그인 `/login`
- 회원가입 `/signup`
- 헤더 (fragment)
- 푸터 (fragment)

### 4. 화면별 일치도 판정

각 화면마다 4개 축으로 5점 척도 점수화:

| 축 | 5점 | 3점 | 1점 |
|---|---|---|---|
| **레이아웃·구조** | prototype 섹션 순서·계층 완전 일치 | 일부 섹션 누락·순서 다름 | 완전히 다른 구조 |
| **CSS 토큰 사용** | `--color-*` `--radius-*` `--shadow-*` 변수만 사용 | 부분적으로 하드코딩 색·크기 | 대부분 하드코딩 |
| **인터랙션·CTA** | HTMX·라우팅·버튼 액션 prototype 대응 | 일부 버튼 dead-link 또는 미구현 | 인터랙션 대부분 없음 |
| **콘텐츠·라벨** | UX 라이팅·라벨 prototype 일치 | 일부 라벨 lorem 또는 placeholder | 대부분 placeholder |

**일치도 %** = (4개 축 합계 / 20) × 100

라운딩: 5% 단위로 반올림 (예: 87% → 85%).

### 5. 저번 주 대비 계산

`mcp__claude_ai_Notion__notion-search` 로 저번 주 리포트 조회:
- query: `YYYY-MM-DD Prototype Gap` (저번 주 월요일 날짜) 또는 `Prototype Gap` 최근 정렬
- 상위 페이지: `38fc33520d0e80b0bdddc3b2a430fb94`

찾으면 fetch 해서 화면별 저번 주 점수 추출. 증감 컬럼에 `▲+5` `▼-3` `─` 표시.

저번 주 리포트 없음 (최초 실행) → 증감 컬럼 `—` 표시하고 baseline 안내 명시.

### 6. Notion 페이지 생성

- `title`: `YYYY-MM-DD Prototype Gap`  (예: `2026-07-06 Prototype Gap`)
- `icon`: `📊`
- `content`:

```markdown
> 📌 **한눈에**
> 1. 전체 평균 일치도 XX% (저번 주 대비 ▲/▼N%)
> 2. 최대 상승 화면 + 최대 하락 화면
> 3. 이번 주 우선 개선 후보 (일치도 최하 화면)

## 화면별 일치도

| 화면 | 이번 주 | 저번 주 | 증감 | 우선순위 |
|---|---|---|---|---|
| 홈 `/` | 70% | 65% | ▲+5 | 중 |
| 프로그램 목록 | 85% | 85% | ─ | 낮음 |
| 로그인 | 95% | 95% | ─ | 낮음 |
| 회원가입 | 30% | — | 신규 평가 | **높음** |
| ... | ... | ... | ... | ... |

**평균**: XX% (저번 주 YY% 대비 ▲/▼N%)

## 상세 평가 (하락 or 우선 개선 화면만)

### 홈 `/` — 70% (▲+5)
- **레이아웃**: 5/5 (Q1 결정 반영 완료)
- **CSS 토큰**: 4/5 (text-tri 자기참조 이슈 잔존)
- **인터랙션**: 3/5 (Quick Stats 링크 미구현)
- **콘텐츠**: 3/5 (프로그램 4건 하드코딩)

**개선 제안**: 
- Quick Stats 링크 활성화 (안 A 채택 시)
- fix/text-tri-token 머지

### 회원가입 — 30% 신규
- prototype F0b 미착수 상태
- HANDOFF 5.7 spec 존재, 구현 대기

## 이번 주 권장 작업 순서

1. (일치도 최하 화면)
2. (증감이 크게 하락한 화면)
3. (사용자 결정 이미 완료됐지만 착수 안 된 항목)

## 참고

- 이전 주 리포트: (링크)
- 평가 기준: 이 페이지 프롬프트의 4축 5점 척도
- STATE.md "prototype 일치도" 표 갱신 권장 (다음 wrap-up 시 미러 반영)
```

### 7. 특수 케이스

- 최초 실행 (저번 주 리포트 없음) → 페이지 상단에 `⚡ 최초 실행 — baseline 수립. 다음 주부터 증감 표시` 명시
- 화면 순회 중 templates 파일 미존재 → 일치도 0%, 우선순위 최고

### 8. 결과 출력

Notion 페이지 URL 만 출력.

### 9. 코드 변경 금지

Read/Bash/Glob/Grep/Notion MCP 만 사용. STATE.md 자동 갱신 금지 (사용자가 wrap-up 때 수동 반영).
