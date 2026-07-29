# F0f — 프로그램 목록 캘린더 뷰

- 상태: `spec_draft`
- 우선순위: 1 (미구현 3건 중 최우선)
- 브랜치 후보: `feature/F0f-calendar-view`
- 작성일: 2026-07-28

---

## 1. 배경

`/programs` 화면에 `[목록 | 캘린더]` view toggle 이 이미 노출되어 있으나 (`templates/program/list.html:143~148`) 캘린더 버튼은 `disabled + title="캘린더 보기는 곧 제공돼요"` placeholder 상태. 사용자가 캘린더 컨텍스트로 프로그램 시작일을 훑어보는 UX 를 제공하기 위해 실제 구현이 필요.

---

## 2. 디자인 출처 (3자산)

| 자산 | 위치 | 내용 |
|---|---|---|
| prototype.tsx | `ProgramCalendar` 컴포넌트 L728~814 | 캘린더 실 컴포넌트. 7×5 grid, 셀 클릭 시 우측 320px 패널 slide-in, 셀당 프로그램 2건 pill + `+N건 더` |
| prototype.tsx | `ProgramList` L815~942 (특히 L870~880) | view toggle → `view==='calendar'` 시 `<ProgramCalendar filtered={filtered} go={go}/>` 렌더. **filtered (status·region·center 필터 적용된 리스트) 그대로 전달** |
| prototype.html | (동일 UI) | prototype.html 의 캘린더 섹션. tsx 와 동일 렌더 결과 |
| HANDOFF.md | 캘린더 관련 언급 없음 | 별도 ⚠️ 블록 없음 — tsx 를 primary source 로 사용 |
| 현재 구현 | `templates/program/list.html:143~148` | disabled placeholder. `?view=calendar` 파라미터 없음 |

### 2-A. tsx 상태 다이어그램 (L728~814 인용)

```
ProgramCalendar({ filtered, go })
├─ 로컬 state: selDay (Number|null) — 우측 패널 표시 여부
├─ 상수: firstDow=4, days=31, TODAY=19 (프로토는 2026-08 하드코딩)
├─ 파생: byDay = {day → [pg,...]} (프로토는 dayMap[pg.id] 로 mock 분산)
└─ 렌더:
    ┌─ 좌측 메인 (flex:1)
    │  ├─ 상단 툴바: [오늘] 버튼 · ‹ 2026년 8월 › · (여백)
    │  ├─ 요일 헤더 (일~토, 일=error, 토=primary)
    │  └─ 7×5=35 셀 grid (셀 높이 104px)
    │     ├─ 오늘: 원형 배경 primary + "오늘" 뱃지
    │     ├─ 선택셀: primary 테두리 + primaryBg 배경
    │     └─ 셀 내부: 프로그램 pill 최대 2건 (dot color=capInfo(pg).color + title 말줄임)
    │                +2 초과 시 "+N건 더"
    └─ 우측 패널 (width:320, selDay 있을 때만)
       ├─ 헤더: "8월 {selDay}일 · 시작 {daySel.length}건" + × 닫기
       └─ 카드 리스트: 썸네일 60×60 + center + DdayChip + title + CapacityBar(showLabel=false)
```

**핵심 상호작용:**
- 셀 클릭 → `setSelDay(d)` (우측 패널 open)
- pill 클릭 → `e.stopPropagation()` + `go('program-detail',{pg})`
- 우측 카드 클릭 → `go('program-detail',{pg})`
- [오늘] 클릭 → `setSelDay(TODAY)`
- ‹ › 클릭 → prototype 은 미구현 (visual only)

---

## 3. 자산 간 갭 표

| 항목 | prototype.tsx | prototype.html | HANDOFF | 채택 |
|---|---|---|---|---|
| grid rows | 5 (fixed 35 cells) | 동일 | — | **5 rows 고정** (Q1) |
| 셀 프로그램 표시 | pill (dot + title) 최대 2건 + "+N건 더" | 동일 | — | 동일 채택 |
| 월 이동 ‹ › | UI 있으나 handler 없음 | 동일 | — | **결정 필요 (Q3)** — 서버 URL query vs client-only |
| 오늘 위치 | 하드코딩 TODAY=19 | 동일 | — | 실제 `LocalDate.now()` 로 대체 |
| grouping key | tsx: `dayMap[pg.id]` mock. 실 데이터에서는 `pg.startDate` | — | — | **startDate 기준** (Q2 확인) |
| 우측 패널 폭 | 320px fixed | 동일 | — | 320px 채택 (반응형 축소는 Q5) |
| 필터 유지 | filtered prop 그대로 → view 만 변경 | 동일 | — | 서버 사이드에서 status/regions/centers 파라미터 유지 |

---

## 4. 데이터 모델 gap 표

| prototype 필드 | 현재 엔티티 (`Program`) | 조치 |
|---|---|---|
| `pg.id` | `Program.id` | ✅ 매핑됨 |
| `pg.title` | `title` | ✅ |
| `pg.center` | `organization` (렌더 시 표시) | ✅ 기존 재사용 |
| `pg.date` (진행기간) | `startDate` / `endDate` | ✅ |
| `pg.status` | `getStatus()` (OPEN/UPCOMING/ENDED/SUSPENDED) | ✅ |
| grouping 기준일 | `startDate` (**Q2**: 신청기간 시작 vs 진행기간 시작) | ⚠️ 확인 필요. Program 엔티티에는 별도 `applyStartDate` 없음 (D5-card-capacity-bar deferred 참조) |
| `capInfo(pg)` | `ProgramCardDto` 재사용 가능 | ✅ N+1 방지 IN 쿼리 재사용 |
| 이미지 | `imageUrl` | ✅ |

**추가 필요 컬럼 없음** — 캘린더 뷰는 기존 필드로 렌더 가능.

---

## 5. 데이터 소비 지점

| 소비 지점 | prototype 참조 | 현재 상태 | 갭 |
|---|---|---|---|
| `/programs?view=calendar` 캘린더 grid | tsx L728~781 | 미구현 (disabled 버튼) | 신설 |
| 캘린더 셀 pill → program-detail 링크 | tsx L766 | 기존 detail 페이지 재사용 | — |
| 우측 패널 카드 → program-detail 링크 | tsx L796 | 기존 detail 페이지 재사용 | — |
| 목록 뷰의 필터 pill / 정렬 / status 탭 | list.html:83~150 | 유지 (뷰만 스위치) | view 파라미터에 따라 정렬 영역만 조건부 숨김 |

---

## 6. 변경 범위

**신설**
- [ ] `program/ProgramController.java` — `list()` 메서드에 `@RequestParam view` 추가. `calendar` 이면 캘린더용 모델 attribute 세팅
- [ ] `program/ProgramCalendarService.java` (신규) — 월 단위 grouping 계산 (`Map<Integer, List<Program>>`), grid cell 배열 산출, prev/next 월 파라미터 산출
- [ ] `templates/program/list.html` — view toggle 버튼 활성화 (`disabled` 제거, `?view=calendar` link), 조건부 renderer 분기
- [ ] `templates/program/_calendar-fragment.html` (신규) — 캘린더 grid + 우측 패널 fragment. HTMX 부분 갱신 지원
- [ ] `static/css/main.css` — 캘린더 grid, 셀, pill, 우측 패널 스타일 (prototype 인라인 style 을 CSS 변수 기반으로 이식)

**수정 없음**
- Program 엔티티 (신규 컬럼 불필요)
- Flyway 마이그레이션 (스키마 변경 없음)

**JS**
- [ ] `templates/program/list.html` 하단 인라인 스크립트 또는 `static/js/program-calendar.js` 신규 — 셀 클릭 → 우측 패널 open, × 닫기. HTMX 로 서버 왕복 vs client-only 는 Q4 결정

---

## 7. PR 분할 제안

- **PR-1 (본 명세 스코프)**: view toggle 활성화 + 서버 rendering 캘린더 grid + 우측 패널 + 월 이동 (URL query). 셀·pill 클릭은 기존 detail 링크
- **PR-2 (선택)**: 월 이동을 HTMX 부분 갱신으로 전환 (URL query 유지, `#calendar-region` 만 swap)
- **PR-3 (선택)**: 반응형 (모바일에서 우측 패널을 bottom sheet 로 전환)

---

## 8. 검증 시나리오

### 정적
- `./gradlew compileJava test --tests ProgramCalendarServiceTest`
- `./gradlew test --tests ProgramListRenderTest` (view=calendar 분기 추가)

### 동적 (curl)
- `GET /programs?view=calendar` → 200, HTML 에 `<div class="program-calendar-grid"` 존재
- `GET /programs?view=calendar&status=OPEN&regions=수원시` → 필터 유지 확인
- `GET /programs?view=calendar&year=2026&month=9` → 다른 달 렌더
- `GET /programs` → 기본 grid 뷰 유지 (회귀)
- 정적 리소스 200 확인

### 시각 (Playwright / 사용자)
- 오늘 셀에 원형 primary 배경 + "오늘" 뱃지
- 셀 클릭 → 우측 패널 slide-in
- pill 클릭 → detail 페이지 이동 (event bubbling 차단 확인)
- 좌우 화살표 클릭 → 이전/다음 달

### write→read 왕복
- 캘린더는 read-only 뷰 (신규 저장 없음) → 왕복 시나리오 해당 없음

---

## 9. Q 리스트 (사용자 결정 필요)

| # | 질문 | 옵션 | 기본 제안 |
|---|---|---|---|
| Q1 | grid 행 수 | (a) 항상 5행 고정 tsx 동일 / (b) 월별 6행 가변 | (a) — tsx 그대로 |
| Q2 | grouping 기준일 | (a) `startDate` (진행 시작) / (b) 신청 마감 `endDate` / (c) 별도 `applyStartDate` 도입 | (a) — 프로토는 진행일 표시로 보임 |
| Q3 | 월 이동 방식 | (a) URL query `?year=&month=` 서버 렌더 / (b) HTMX 부분 갱신 / (c) client JS only | (a) — 학습 프로젝트 SSR 우선, 필터 조합 시 URL 공유 가능 |
| Q4 | 셀 클릭 → 우측 패널 | (a) client JS only / (b) HTMX `?day=` 왕복 | (a) — 이미 filtered 데이터가 클라에 있음, 왕복 오버헤드 없음 |
| Q5 | 반응형 축소 (< 768px) | (a) 우측 패널 → bottom sheet / (b) grid 만 유지, 셀 클릭 시 셀 내부 확장 / (c) 이번 PR 미포함 | (c) — PR 스코프 최소화 |
| Q6 | pill 초과 표시 문구 | tsx: "+N건 더" 유지 | 유지 |
| Q7 | 종료(ENDED) 프로그램 표시 여부 | 목록은 status 탭에서 필터. 캘린더는? (a) 목록 필터 그대로 / (b) 항상 종료 회색 표시 | (a) — filtered 동일 소스 |
| Q8 | 프로그램이 startDate~endDate 여러 날 걸칠 때 | (a) startDate 셀 1개만 pill / (b) 기간 전체를 bar 로 span | (a) — tsx 동일. bar span 은 CSS 복잡도 대비 이득 적음 |

---

## 10. 위험 / 주의

- **월 이동 시 필터 상태 유지 필수** — `activeFilters` 모델 재활용, `th:href` 에 status/regions/centers 모두 재바인딩
- **HTMX 부분 갱신 시 스타일 파라미터 왕복 (`hx-vals`) 패턴** — CLAUDE.md 참조. PR-2 진행 시 주의
- **`th:fragment` 는 별도 파일로** — `_calendar-fragment.html` 필수 (CLAUDE.md F0h-c2 사고 재발 방지)
- 프로토는 2026-08 하드코딩이라 "오늘" 이 19일로 고정 — 실 구현은 `LocalDate.now()` 로 대체하며 tsx 좌표는 참고만
