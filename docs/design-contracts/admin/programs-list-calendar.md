# 관리자 프로그램 캘린더 뷰 — 디자인 계약

> 산출: D-admin-calendar (2026-10-12). ym-spec 결과 + 사용자 Q0~Q10 결정 반영.
> 갱신: **FOLLOW-admin-calendar-panel-card-prototype-match (2026-10-14)** — 우측 패널 카드 구조를 admin prototype L1125~1157 매칭으로 전면 재작성.
> 계약 코드: `e2e/contracts/admin-programs-calendar.ts`

## FOLLOW 2026-10-14 — D5-Q3 와 결별

**admin 캘린더 패널 카드는 capacity bar 를 사용하지 않는다.** admin prototype L1125~1157 은 전혀 다른 디자인이며:

- 썸네일: 100% × 100px 보라 bg + 32px 보라 텍스트 (제목 첫 글자, Q1=b)
- 제목: 밑줄 (`#E3E1E8`) 처리된 anchor + 아래 센터명 11px (Q2=a — article wrapper 안의 제목·버튼만 anchor)
- 구분선 1px `#F0EFF3`
- 정보 3라인 dl: "진행 기간" / "신청 기간" / "신청 현황" (Q3=a 공용 `ProgramCardDto` 확장 + Q4=a `formatRange` 공용 유틸)
- "신청 현황" 값만 primary bold 12px (Q5=a — "{applied}/{capacity}명", capacity null → "제한 없음" Q5=b fallback)
- 상세보기 버튼: full-width primary bg + chevron SVG
- **chip / capacity bar 없음** — regression 방지용 negative check 2건 (`panel.card.capacity_bar.absent`, `panel.card.chip.absent`)

user 쪽 (`program/_calendar-fragment.html`) 은 여전히 `program-calendar-panel-card-{bar,chip,capacity,meta,thumb}` 를 사용하므로 공유 CSS 는 유지. admin 은 `admin-program-calendar-panel-card-*` 네임스페이스로 완전 분리.

## 범위

| 포함 | 제외 |
|---|---|
| 월간 42셀 캘린더 grid (POLICY P-6) | 주/리스트 뷰 (Q4 — 별 티켓) |
| 뷰 토글 세그먼트 (목록·카드·캘린더 3버튼) | 카드 뷰 실제 UI (Q8 — placeholder 만) |
| 월 이동 nav + [오늘] 버튼 | 캘린더 내 CRUD (Q5 — read-only) |
| 셀 클릭 → 우측 300px slide 패널 (Q3) | 모바일 하단 시트 (admin 특성상 데스크톱 위주) |
| URL filter pass-through (q·status) (Q7) | 지역/카테고리/정렬 (관리자 목록이 아직 미도입) |
| SUSPENDED 포함 (Q6) | — |
| 범례 3종 | — |

## 사용자 F0f 와의 차이

| 축 | 사용자 F0f | 관리자 D-admin-calendar |
|---|---|---|
| 조회 범위 | `isActive=true` 만 | **isActive 전체** (SUSPENDED 포함) |
| 필터 입력 | status·regions·centers·sort | **q·status** (관리자 목록 축 재사용) |
| 셀 클릭 대상 URL | `/programs/{id}` | **`/admin/programs/{id}`** |
| 모바일 하단 시트 | 포함 | 미포함 |
| 범례 `ended` 라벨 | "종료" | **"종료 · 운영중단"** |
| CSS | main.css `program-calendar-*` | **CSS 재사용 + admin 네임스페이스 추가** (`admin-program-calendar-*`) |

## 공용 유틸 (POLICY P-6 보증)

`src/main/java/.../common/calendar/CalendarGridBuilder.java` 가 42셀 산출을 담당.
사용자 `ProgramCalendarService` 와 admin `AdminProgramCalendarService` 모두 이 유틸을 통해 셀 산출 → **월별 행 수 불일치 발생 불가**.

## 상호작용 (JS)

| 트리거 | 결과 | 파일 |
|---|---|---|
| 셀 클릭 (`.program-calendar-cell[data-in-month="true"]`) | 우측 패널 slide-in · 해당 날짜 카드 그룹 노출 | `/js/admin-program-calendar.js` |
| pill 클릭 | 상세 이동 (`/admin/programs/{id}`) · 셀 선택 전파 차단 | 상동 |
| 패널 내부 카드 클릭 | 상세 이동 · 셀 선택 전파 차단 | 상동 |
| × (data-calendar-close) | 패널 close | 상동 |
| [오늘] (data-calendar-today) | viewMonth==todayMonth → 오늘 셀 선택 / else → `?year=&month=` 이동 | 상동 |
| 월 arrow (prev/next) | 서버 전체 리로드 (admin 은 HTMX partial swap 미사용) | fragment href |

## 공통 정책 참조

- POLICY P-6: 월간 캘린더 42셀 6행 고정 (`docs/design-contracts/POLICY.md#p-6`)
- POLICY P-3: 아이콘 SVG (fragment `~{fragments/icons :: calendar(...)}` 사용)

## 완료 기준

1. `./gradlew test --tests AdminProgramCalendarServiceTest --tests AdminProgramListRenderTest --tests CalendarGridBuilderTest --tests ProgramCalendarServiceTest` 전수 PASS
2. `cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts admin-programs-calendar` 갭 0
3. 기능 E2E `admin-program-calendar.spec.ts` chromium PASS (뷰 토글·월 이동·셀 클릭·패널 slide·상세 이동)

## 이월 (별 티켓)

- 카드 뷰 실제 UI (Q8)
- 주/리스트 뷰 (Q4)
- 캘린더 내 CRUD · 드래그-드롭 일정 조정 (Q5)
- admin 지역/카테고리 필터 (목록이 지원하는 시점에 pass-through 자동 확장)
