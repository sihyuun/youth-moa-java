# D-admin-calendar — 관리자 프로그램 캘린더 뷰

> 상태: `spec_confirmed`  
> ID: a70601edd02b47cba  
> 산출: ym-spec (2026-10-12). 사용자 Q0~Q10 결정 전부 권장안 채택.

## 1. 결정 요약 (Q0~Q10)

| Q | 결정 |
|---|---|
| Q0 | **A — admin 트랙 캘린더 신설** (사용자 F0f 는 이미 완결, admin 미구현) |
| Q1 | **C — 공용 `CalendarGridBuilder` 유틸 추출**, admin / user 각자 Service |
| Q2 | **6행 42셀 고정** (POLICY P-6 신설) |
| Q3 | **우측 300px 슬라이드 패널** (HANDOFF L232 Google Calendar 패턴, 사용자용 CSS 재사용) |
| Q4 | **월간만 (MVP)** — 주/리스트 뷰는 별 티켓 |
| Q5 | **read-only** — 셀 또는 pill 클릭 시 상세 이동만. 캘린더 내 CRUD 금지 |
| Q6 | SUSPENDED **포함** (관리자 운영 특성) |
| Q7 | **URL query pass-through** — 지역·카테고리·상태·검색 그대로 유지 |
| Q8 | **카드 뷰는 별 티켓** — view 토글 UI 는 신설하되 card 선택 시 placeholder 처리 |
| Q9 | **F0f PR-2 CSS 재사용** — 모바일 하단 시트까지 공용 |
| Q10 | POLICY **P-6 "월간 캘린더 42셀 6행 고정"** 신설 |

## 2. 변경 범위 (파일 단위)

### PR-1 리팩터 (사용자 F0f TC 유지 전제)

| 파일 | 변경 |
|---|---|
| `src/main/java/.../common/calendar/CalendarGridBuilder.java` | **신설** — 42셀 산출·prev/next 월 계산·nearestMonth 공통 유틸 |
| `src/main/java/.../program/ProgramCalendarService.java` | **리팩터** — 42셀 산출을 CalendarGridBuilder 로 위임 (동작 불변) |
| `src/test/java/.../common/calendar/CalendarGridBuilderTest.java` | **신설** — 42셀 산출·prev/next·edge case 단위 테스트 |

### PR-2 admin 캘린더

| 파일 | 변경 |
|---|---|
| `src/main/java/.../admin/program/AdminProgramCalendarService.java` | **신설** — SUSPENDED 포함, URL filter pass-through, CalendarGridBuilder 재사용 |
| `src/main/java/.../admin/AdminProgramController.java` | **수정** — `view` 파라미터 추가, `view=calendar` 분기 (`card` 는 placeholder flash) |
| `src/main/resources/templates/admin/program/_calendar-fragment.html` | **신설** — admin DTO 바인딩, 우측 300px slide 패널 |
| `src/main/resources/templates/admin/program/list.html` | **수정** — 뷰 토글 세그먼트 (list/card/calendar) + view=calendar 시 fragment include |
| `src/main/resources/static/css/admin.css` | **수정** — admin-program-calendar-* 네임스페이스 + user 공용 토큰 import 선언 |
| `src/main/resources/static/js/admin-program-calendar.js` | **신설** — 셀 클릭 slide-in 패널 토글 (사용자 program-calendar.js 패턴 포팅) |
| `src/test/java/.../admin/AdminProgramCalendarServiceTest.java` | **신설** — SUSPENDED 포함·필터 pass-through·42셀 검증 |
| `src/test/java/.../admin/AdminProgramListRenderTest.java` | **수정** — view 토글·calendar 분기 렌더 검증 |
| `docs/design-contracts/admin/programs-list-calendar.md` | **신설** — admin 캘린더 디자인 계약 |
| `e2e/contracts/admin-programs-calendar.ts` | **신설** — admin 캘린더 계약 코드 |
| `e2e/tests/admin-program-calendar.spec.ts` | **신설** — 뷰 토글·월 이동·셀 클릭·패널 slide·상세 이동 E2E |
| `docs/design-contracts/POLICY.md` | **수정** — P-6 신설 |

## 3. 데이터 모델

변경 없음. 기존 `Program`, `ProgramStatus`, `ProgramCardDto`, `CalendarViewDto` 를 그대로 재사용한다.
단, admin 은 `ProgramStatus.SUSPENDED` 포함 조회(= `isActive=false` 포함) 가 핵심 차이.

## 4. 서비스 설계

### 4-1 `CalendarGridBuilder` (공용)

- 입력: `YearMonth ym`, `LocalDate today`, `Map<Integer, List<ProgramCardDto>> byDay`, `int maxPills`
- 출력: `GridResult { List<CalendarCell> cells, int prevYear, int prevMonth, int nextYear, int nextMonth }`
- cells 는 항상 42개 (6행 × 7열). `firstDow = firstOfMonth.getDayOfWeek().getValue() % 7`
- pill 산출·colorKind·moreCount 계산은 호출자에서 완성한 `ProgramCardDto` 를 받아서 pill 로 변환
- nearestMonth 계산 유틸도 공용 (`nearestMonth(List<Program> candidates, YearMonth pivot) → Optional<YearMonth>`)

### 4-2 `AdminProgramCalendarService`

- SUSPENDED 포함: `isActive=true` 조건 **제외**. 대신 `statusFilter` 가 "SUSPENDED" 면 `isActive=false AND status=SUSPENDED`
  (ProgramStatus enum 기준으로 분기)
- URL filter pass-through: `q`, `statusFilter`, `regions`, `centers` 전부 받아 Specification 결합
- CalendarGridBuilder 로 42셀 산출
- `panelData` LinkedHashMap, nearestMonth 공용 유틸 재사용

## 5. 컨트롤러 분기

`AdminProgramController#list` 에 `@RequestParam(defaultValue="list") String view` 추가:

| view | 처리 |
|---|---|
| `list` (기본) | 기존 list 렌더 그대로 |
| `calendar` | `AdminProgramCalendarService.calendar(...)` 호출 → `calendarView`, `filterStatus`, `filterQ` 모델 주입, `admin/program/list` 뷰 리턴 (fragment include) |
| `card` | placeholder — `flashInfo="카드 뷰는 별 티켓에서 제공돼요."` 후 `list` 와 동일 |

## 6. 템플릿 설계

`admin/program/list.html` 상단 액션바 안에 뷰 토글 세그먼트 추가:

```html
<nav class="admin-program-view-toggle" aria-label="뷰 전환">
  <a th:href="@{/admin/programs(q=${q}, status=${statusFilter}, view='list')}"
     th:class="${viewMode == 'list'} ? 'active' : ''">목록</a>
  <a th:href="@{/admin/programs(q=${q}, status=${statusFilter}, view='card')}"
     th:class="${viewMode == 'card'} ? 'active' : ''">카드</a>
  <a th:href="@{/admin/programs(q=${q}, status=${statusFilter}, view='calendar')}"
     th:class="${viewMode == 'calendar'} ? 'active' : ''">캘린더</a>
</nav>
```

view=calendar 일 때는 `admin-program-list-card` 섹션을 숨기고 fragment include:

```html
<div th:if="${viewMode == 'calendar'}"
     th:replace="~{admin/program/_calendar-fragment :: calendar-region}"></div>
```

## 7. 디자인 계약

`docs/design-contracts/admin/programs-list-calendar.md` + `e2e/contracts/admin-programs-calendar.ts` 신설:

- 42셀 grid 존재
- 뷰 토글 3버튼 (list/card/calendar)
- 우측 300px slide 패널 (초기 hidden)
- 월 이동 arrow, [오늘] 버튼
- 범례 3색 (upcoming/open/ended)

## 8. 테스트 매핑

- `CalendarGridBuilderTest` — 42셀, prev/next, nearestMonth
- `ProgramCalendarServiceTest` (기존 10 TC 유지) — 리팩터 회귀 방어
- `AdminProgramCalendarServiceTest` — SUSPENDED 포함, filter pass-through, 42셀, nearestMonth
- `AdminProgramListRenderTest` — view 토글 3버튼 렌더, calendar fragment include 확인
- `admin-program-calendar.spec.ts` — 뷰 토글·월 이동·셀 클릭·패널 slide·상세 이동

## 9. 금지 사항

- 카드 뷰 구현 금지 (Q8 — placeholder 만)
- 주/리스트 뷰 금지 (Q4)
- 캘린더 내 CRUD 금지 (Q5)
- 사용자 F0f 코드 침입 금지 (CalendarGridBuilder 추출만, `ProgramCalendarService` 공용 함수 변경 최소화)
- Q 결정 범위 외 리팩토링 금지

## 10. 상태

- `spec_confirmed` (ym-spec 완료, 사용자 결정 반영)
- 다음 단계: ym-impl 구현 → ym-qa → ym-verify
