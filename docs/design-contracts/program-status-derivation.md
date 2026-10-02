# 프로그램 상태(status) · D-day 파생 계약

> 확정일 2026-10-02. 분류: **서술 계약** (갭 계측 자동화 대상 아님 — 도메인 파생 규칙). 티켓 D5-Q1a.

## 1. 결정

Program 의 `ProgramStatus` 와 D-day 레이블은 **신청기간(`applyStartDate`/`applyEndDate`)** 기준으로 파생된다. 운영기간(`startDate`/`endDate`) 은 "진행 월 뷰" (캘린더 그룹핑) 와 상세 운영기간 표기용으로만 쓰인다.

```
if (!isActive)                                        → SUSPENDED
if (applyStartDate > today)                           → UPCOMING
if (applyStartDate <= today <= applyEndDate)          → OPEN
if (applyEndDate < today)                             → ENDED

D-day = applyEndDate - today
  < 0  → "종료"
  == 0 → "D-DAY"
  > 0  → "D-N"
```

**최우선 조건**: `isActive=false` 는 어떤 기간이든 즉시 `SUSPENDED`.

## 2. 근거 (prototype + HANDOFF 인용)

- `docs/00_assets/admin/prototype.tsx` **L132~133**
  ```
  const getProgramStatus = (p) => {
    if (!p.isActive) return 'suspended'
    const today = new Date().toISOString().split('T')[0]
    if (p.applyEndDate && p.applyEndDate < today) return 'ended'
    ...
  }
  ```
- `docs/00_assets/admin/HANDOFF.md` **L273**
  > status는 신청기간·정원으로 파생

운영기간은 "언제 진행되는지" (information), 신청기간은 "언제까지 신청할 수 있는지" (action). 사용자가 카드를 보고 결정해야 하는 축은 후자이므로 status 뱃지는 신청기간을 반영해야 한다.

## 3. 사용자 확정 결정 (Set-A · 2026-10-02)

| Q | 결정 | 적용 |
|---|---|---|
| Q5 | **A** — `applyEndDate < today → ENDED`. 운영기간은 조건에 반영하지 않는다 | `getStatus()` 분기 |
| Q6 | **A** — `ProgramCardDto.dateLabel` 은 **운영기간** 유지 (별도 축) | ProgramCardDto 수정 없음 (D5-Q1b) |
| Q4 | **B** — AdminStatsService fallback 제거, `applyEndDate` 전용 (D5-Q1e PR 분리) | 이 PR 범위 아님 |
| Q3 | **A** — 캘린더 그룹핑 키는 운영 `startDate` 유지 ("진행 월별 뷰") | 이 PR 범위 아님 |

## 4. nullable 폴백 (임시)

V12 (2026-09-10) 는 `applyStartDate`/`applyEndDate` 를 **nullable** 로 추가했다. 레거시 row 는 null 이 남아 있다.

이 PR 은 다음 폴백을 둔다 — **D5-Q1d (V27 backfill + NOT NULL 승격) 머지 후 반드시 제거**한다:

```java
LocalDate effectiveStart = applyStartDate != null ? applyStartDate : startDate;
LocalDate effectiveEnd   = applyEndDate   != null ? applyEndDate   : endDate;
```

Program.java 에 `TODO(D5-Q1d)` 주석으로 명시했다.

## 5. 구현 매핑 (코드 ↔ 계약)

| 계약 행 | 구현 |
|---|---|
| isActive=false → SUSPENDED | `Program.java:getStatus()` L1 분기 |
| applyStart > today → UPCOMING | `Program.java:getStatus()` effectiveStart 비교 |
| applyEnd < today → ENDED | `Program.java:getStatus()` effectiveEnd 비교 |
| D-day = applyEnd - today | `Program.java:getDaysUntilDeadline()` |
| applyEnd 당일 → D-DAY | `Program.java:getDdayLabel()` days==0 |
| applyEnd 지남 → "종료" | `Program.java:getDdayLabel()` days<0 |
| null 폴백 → startDate/endDate | `Program.java:getStatus()` + `getDaysUntilDeadline()` effectiveX 변수 |

## 6. 테스트 커버리지

`ProgramStatusDerivationTest` — 6 case 매트릭스:

1. applyStart 미래 → UPCOMING
2. applyStart ≤ today ≤ applyEnd → OPEN + D-N
3. applyEnd 지남 (운영기간 미래여도) → ENDED + "종료" (Q5=A 검증)
4. applyEnd 당일 (경계) → OPEN + D-DAY
5. isActive=false → SUSPENDED (최우선)
6. applyStart/End 둘 다 null → startDate/endDate 폴백 (UPCOMING/OPEN/ENDED 세 서브 케이스)

## 7. 소비 지점

### 7-1. Q1a 머지 즉시 자동 교체되는 지점 (Q5=A 의도된 결과)

다음 6개 지점은 `program.getStatus()` / `getDdayLabel()` / `getDaysUntilDeadline()` 을 **직접 호출**한다. 엔티티 파생 메서드만 교체돼도 이 지점의 출력이 함께 신청기간 기준으로 바뀐다. **별도 Q1b 작업 불필요**:

| 파일 | 라인 | 호출 | 즉시 영향 |
|---|---|---|---|
| `ProgramCardDto.java` | L78, 150-186, 256-265, 279-284, 298-303 | `program.getStatus()` / `getDdayLabel()` / `getDaysUntilDeadline()` / `getDaysUntilApplyStart()` | 목록·상세·검색 결과 뱃지·D-day·상세 "마감까지 N일"/"신청 오픈까지 N일" 변경 |
| `ApplicationService.java` | L127 | `program.getStatus() != OPEN` 신청 가드 | applyEnd 지난 프로그램 신청 불가 |
| `AdminDashboardService.java` | L43-45 | status 별 `.filter(...)` 카운트 | 관리자 대시보드 OPEN/ENDED/UPCOMING 집계 변경 |
| `AdminStatsService.java` | L337 | 통계 레코드 `.status(...)` | 통계 표시값 변경 (Q4=B D5-Q1e 로 fallback 제거는 별도) |
| `AdminCsvController.java` | L166 | CSV 내보내기 status 셀 | CSV 출력값 변경 |
| `ProgramCalendarService.java` | L228 | `card.getStatus()` | 캘린더 뱃지 변경 (Q3=A 로 그룹핑 키 = 운영 startDate 는 유지) |

→ dateLabel 은 Q6=A 로 운영기간 유지, 캘린더 그룹핑 키는 Q3=A 로 운영 startDate 유지. 그 외 모든 "status 라벨" 과 "D-day" 는 신청기간 기준.

**각주 (Q1a → Q1b 상충 구간)**: `AdminDashboardService.load()` L72-81 "마감 임박 Top 5" 블록은 §7-2 에 따라 **Q1b 머지까지 운영 endDate 유지** → 홈 뱃지(applyEnd 기준) 와 상충 가능. 같은 프로그램이 뱃지는 "종료", 대시보드 Top 5 는 "D-5" 로 노출될 수 있음. Q1b 머지 즉시 해소.

### 7-2. D5-Q1b 로 이월 — DB 쿼리 수준에서 startDate/endDate 를 비교하는 지점

엔티티 파생이 아니라 **JPA Criteria / @Query 로 DB 에 직접 날짜 비교**를 보내는 지점은 Q1a 범위를 벗어난다. Q1b 에서 applyStart/applyEnd 로 교체 예정:

| 파일 | 라인 | 현재 비교 컬럼 | Q1b 교체 방향 |
|---|---|---|---|
| `ProgramSpec.withDateStatus()` | L52-64 | `startDate` / `endDate` (open/upcoming/ended 3분기) | `applyStartDate` / `applyEndDate` (effective 폴백은 D5-Q1d 백필 전까지만) |
| `ProgramSpec.notEnded()` | L74-80 | `endDate` (전체 탭 종료 제외) | `applyEndDate` |
| `ProgramRepository.findTop4ByIsActiveTrueOrderByEndDateAsc()` | L22 | `endDate ASC` method naming | `findTop4...OrderByApplyEndDateAsc()` 로 교체 — 홈 Top 4 "마감임박" 정렬 축이 신청기간 기준이 되어야 함 |
| `ProgramRepository.countActiveGroupByCenterId()` | L35-40 | `@Query ... p.endDate >= CURRENT_DATE` | `p.applyEndDate >= CURRENT_DATE` |
| `AdminProgramService.statusSpec()` | L465-493 | isActive + startDate/endDate (SUSPENDED/ENDED/UPCOMING/OPEN 4분기) | applyStartDate/applyEndDate (effective 폴백 D5-Q1d 백필 전까지) |
| `AdminDashboardService.load()` | L72-81 | `p.getEndDate()` 직접 산술 — "마감 임박 Top 5" (오늘~+7 일 endDate 범위) | `applyEndDate` (effective 폴백 D5-Q1d 백필 전까지). 코멘트에 "A3 에서 applyEndDate 도입 시 교체 · deferred" 로 이미 Q1 시리즈 대상 명시 |
| `AdminStatsService.deadlineOf()` | L241-243 | `applyEndDate ?? endDate` fallback 블록 — "마감 임박" 집계 | Q4=B 결정에 따라 D5-Q1e 에서 fallback 제거 (applyEndDate 전용) |

**사용 경로 (영향 범위)**:

- `ProgramSpec.withDateStatus` / `notEnded` → `ProgramService.search()` (L44-57), `ProgramCalendarService.load()` (L63-71) — 목록/캘린더 **DB 필터 결과와 뱃지 상태 불일치** 가능 (QA 리포트 §2-4 재현 시나리오: "applyStart 미래 + startDate 과거")
- `ProgramRepository.findTop4...` → `HomeService` L80, L143 — 홈 Top 4 선정 축
- `ProgramRepository.countActiveGroupByCenterId` → `CenterService` L76, L133 — 센터 카드 "진행중 프로그램 N건" 배지
- `AdminProgramService.statusSpec` → admin 목록 `/admin/programs?status=...` 필터 — Q1a 머지 후 admin 목록 필터와 뱃지 상태 상충 가능 (ProgramSpec 과 동일한 "DB 필터 vs 뱃지" 불일치 유형, admin 쪽에서도 동일 재현)
- `AdminDashboardService.load` 마감 임박 Top 5 → `/admin` 대시보드 "마감 임박" 섹션 — Q1b 머지 전까지 홈 뱃지(applyEnd 기준) 와 상충 가능

### 7-3. 후속 이월 (별도 PR)

- **D5-Q1c (UI)** — list.html / detail.html / dashboard.html / stats.html 등 뷰 레이어에서 "신청기간/운영기간" 라벨 명확화
- **D5-Q1d** — V27 backfill (startDate/endDate → applyStart/End) + applyStart/End NOT NULL 승격 + §4 폴백 (`effectiveStart` / `effectiveEnd`) 제거
- **D5-Q1e** — AdminStatsService fallback 제거 (Q4=B)

## 8. 관련 PR / 이월

- **D5-Q1a** (이 PR) — Program 엔티티 파생 메서드 교체 + 단위 테스트 + 서술 계약 + DataInitializer 역산 시드. 뱃지·CSV·대시보드·캘린더·신청 가드가 **머지 즉시 신청기간 기준으로 전환됨** (§7-1, Q5=A 의도된 결과)
- **D5-Q1b** — DB 쿼리 날짜 비교 지점 교체 (§7-2: ProgramSpec withDateStatus/notEnded + ProgramRepository findTop4/countActiveGroupByCenterId)
- **D5-Q1c** — UI 노출 (list.html / detail.html / dashboard.html / stats.html)
- **D5-Q1d** — V27 backfill (startDate/endDate → applyStart/End) + NOT NULL 승격 + 본 폴백 제거
- **D5-Q1e** — AdminStatsService fallback 제거 (Q4=B)
