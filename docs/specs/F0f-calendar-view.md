# F0f — 프로그램 목록 캘린더 뷰

- 상태: `spec_confirmed`
- 우선순위: 1 (미구현 3건 중 최우선)
- 브랜치 후보: `feature/F0f-calendar-view`
- 작성일: 2026-07-28
- 확정일: 2026-08-27 (Q1~Q8 사용자 컨펌 + `docs/00_assets/Program Calendar.dc.html` 정본 반영)

---

## 1. 배경

`/programs` 화면에 `[목록 | 캘린더]` view toggle 이 이미 노출되어 있으나 (`templates/program/list.html:143~148`) 캘린더 버튼은 `disabled + title="캘린더 보기는 곧 제공돼요"` placeholder 상태. 사용자가 캘린더 컨텍스트로 프로그램 시작일을 훑어보는 UX 를 제공하기 위해 실제 구현이 필요.

**정본 자산**: `docs/00_assets/Program Calendar.dc.html` (7 섹션 / 1183줄). 캘린더뷰의 규정·색 매핑·빈 달 배너·모바일 규격은 이 문서가 최종 결정. prototype.tsx 는 7×5 mock 기준이므로 dc.html 확정안과 충돌 시 dc.html 을 채택한다.

---

## 2. 디자인 출처 (3자산 + dc.html)

| 자산 | 위치 | 내용 |
|---|---|---|
| **dc.html §1a** | `docs/00_assets/Program Calendar.dc.html` L699~800 | 캘린더뷰 정본. 6행 42셀 grid · 툴바 · 셀 pill · 우측 패널 |
| **dc.html §1c** | 같은 파일 L872~ | 상태 → 색 매핑 매트릭스 (확정) |
| **dc.html §4a** | 결정 정리 | 신규 임계값 만들지 않음 · 마감임박 셀 색 제외 · SUSPENDED 제외 |
| **dc.html §5a** | 색 체계 확정 | 진행예정 secondary / 모집중 primary / 종료 textTri (3색) |
| **dc.html §6a** | grouping 확정 | startDate 고정, 예외 없음 |
| **dc.html §7a** | 빈 달 배너 규격 | 격자 위·월 네비 아래 · 문구·버튼 규칙 |
| **dc.html §7b** | 모바일 규격 | 정사각 · 점 최대 3 · 하단 시트 (**PR-2 이월**) |
| prototype.tsx | `ProgramCalendar` L728~814 | 캘린더 구현체(참고). 셀 클릭 시 320px 우측 패널. **grid rows 는 dc.html §1a 로 override (5→6)** |
| prototype.tsx | `ProgramList` L815~942 (L870~880) | view toggle · filtered prop 그대로 전달 |
| HANDOFF.md | 언급 없음 | — |
| 현재 구현 | `templates/program/list.html:143~148` | disabled placeholder |

### 2-A. tsx 상태 다이어그램 (L728~814 인용, dc.html 확정값 반영)

```
ProgramCalendar({ filtered, year, month, today })
├─ 로컬 state: selDay (Number|null) — 우측 패널 표시 여부 (client JS)
├─ 서버 파생: cells[42] (6행 고정, dc.html §1a), byDay = {day → [pg,...]}
│              (grouping key = pg.startDate, dc.html §6a)
├─ 서버 파생: nearestMonth (필터 결과 있는 가장 가까운 달, 없으면 null) — 빈 달 배너용
└─ 렌더:
    ┌─ 좌측 메인 (flex:1)
    │  ├─ 상단 툴바: [오늘] · ‹ YYYY년 M월 › (grid 1fr auto 1fr 로 nav 정중앙 정렬. 2026-08-31 사용자 시각 fix 로 "시작일 기준" 라벨 제거)
    │  ├─ (조건) 빈 달 배너 — 격자 위·월 네비 아래 (dc.html §7a)
    │  ├─ 요일 헤더 (일=error, 토=primary)
    │  └─ 7×6=42 셀 grid (셀 높이 104px)
    │     ├─ 오늘: 원형 primary 배경 + "오늘" 뱃지
    │     ├─ 선택셀: primary 테두리 + primaryBg 배경
    │     └─ 셀 내부: 프로그램 pill 최대 2건 (색=매트릭스 §1c) + "+N건 더"
    └─ 우측 패널 (width:320, selDay 있을 때만, client JS toggle)
       ├─ 헤더: "M월 D일 · 시작 N건" + × 닫기
       └─ 카드 리스트: 썸네일 60×60 + center + DdayChip(마감/종료) + title
                       + CapacityBar(showLabel=false)
```

**핵심 상호작용:**
- 셀 클릭 → client JS 로 `selDay` 상태 갱신 · 우측 패널 open (**Q4 → client only**)
- pill 클릭 → `stopPropagation` + `/programs/{id}` 이동
- 우측 카드 클릭 → `/programs/{id}` 이동
- [오늘] → `selDay = today.day` (LocalDate.now() 기준)
- ‹ › → 서버 URL query 재렌더 (**Q3 → `?view=calendar&year=&month=&status=&regions=&centers=`**)
- 빈 달 배너 [N월 보기] → 같은 필터 유지, `year/month` 만 갱신

---

## 3. 자산 간 갭 표

| 항목 | prototype.tsx | dc.html 확정 | 채택 |
|---|---|---|---|
| grid rows | 5 (35 cells) | **6 (42 cells)** dc.html §1a L799 | **6행 고정** (Q1) — 2026-08 재현 시 5행이면 8/30·8/31 잘림 |
| grouping key | mock (`dayMap[pg.id]`) | `startDate` (§6a) | **startDate 고정** (Q2) |
| 셀 색 체계 | capInfo 다색 | **3색: 진행예정·모집중·종료** (§1c, §5a) | 3색 채택 |
| 마감임박 셀 색 | 있음 | **없음** (§4a #3) | 셀 색 제외, 시급성은 우측 패널 담당 |
| 신규 임계값 (70/90% · D-3/D-14) | 캘린더 별도 | **캘린더 임계값 없음** (§4a, §5a 각주) | 캘린더 밖에서만 사용 |
| 오픈 표시 | D-N | **날짜 텍스트 "M/D 오픈"** (§4a #2, §5a) | `ProgramCardDto.secondaryLabel` 재사용 |
| 정원 마감(`isFull`) | 별도 색 | **종료 회색 편입** (§5a) | 사유는 패널 DdayChip (마감/종료) |
| SUSPENDED | — | ProgramService.search() `isActive()` 필터로 미도달 | 매핑 불필요 |
| pill 초과 | "+N건 더" | 동일 | 유지 (Q6) |
| 여러 날 걸침 | startDate 셀 1개만 | dc.html §1c 각주 확정 | startDate 셀 1개만 pill (Q8) |
| 종료 프로그램 표시 | filter 그대로 | filter 그대로 · 시작일 축 예외 없음 · 빈 달 배너로 이동 (§6a) | 채택 (Q7) |
| 반응형 <767px | 미규정 | 정사각 · 점 최대 3 · 하단 시트 (§7b) | **PR-2 이월** (Q5) |
| 월 이동 방식 | UI only | URL query 재렌더 | Q3 URL query 채택 |
| 셀 클릭 우측 패널 | client state | client JS toggle | Q4 client only 채택 |
| 툴바 우측 "시작일 기준" 라벨 | 없음 | 있음 (§1a L723) | **초회 채택 → 2026-08-31 사용자 시각 fix 로 제거**. 툴바는 grid `1fr auto 1fr` 로 [오늘] 좌·nav 정중앙 정렬 |
| calendarSource | filtered prop | filtered (`activeFilters`: status·regions·centers 재사용) | 채택 |

---

## 3-A. dc.html 확정 신규 규정 (10건 · 필수 반영)

각 규정 옆 링크는 `docs/00_assets/Program Calendar.dc.html` 내부 앵커.

1. **셀 색 체계 = 3색** (§1c, §5a)
   - 진행예정 `secondary` `#F97316` — UPCOMING = `today < startDate`
   - 모집중 `primary` `#3F30E9` — OPEN = `startDate ≤ today ≤ endDate` **AND** `!isFull`
   - 종료 `textTri` — ENDED = `endDate < today` **OR** (OPEN AND `applied ≥ capacity`)
2. ~~**"시작일 기준" 라벨** — 툴바 우측에 항상 표시 (§1a L723)~~ **2026-08-31 사용자 시각 fix 로 제거**. 툴바는 grid `1fr auto 1fr` 로 [오늘] 좌측 · nav 정중앙 정렬
3. **오픈 표시** — 진행예정은 D-N 만들지 않고 "M/D 오픈" 날짜 텍스트. `ProgramCardDto.secondaryLabel` 재사용 (§4a #2, §5a)
4. **정원 마감(`isFull`) → 종료 회색 편입** — 사유는 우측 패널 `DdayChip` ("마감" / "종료") 로만 노출 (§5a)

#### 4-A. 우측 패널 카드 chip 4종 매핑 표 (dc.html §5a `chipOf()` 원본 반영, 2026-08-31 verify 6차 후 명문화)

| 상태 조건 | chip modifier | 텍스트 | 배경 | 구현 위치 |
|---|---|---|---|---|
| UPCOMING | `--upcoming` | `"M/D 오픈"` (예: `8/28 오픈`, padding 없음) | `--color-secondary` `#F97316` (오렌지) | `ProgramCardDto.getCalendarChipKind` = `upcoming` |
| OPEN + `days > 3` | `--open` | `"D-N"` (예: `D-12`) | `rgba(0, 0, 0, 0.55)` (dark 반투명) | `ProgramCardDto.getCalendarChipKind` = `open` |
| OPEN + `0 ≤ days ≤ 3` (D-DAY 포함) | `--urgent` | `"D-N"` / `"D-DAY"` | `--color-error` (빨강) | `ProgramCardDto.getCalendarChipKind` = `urgent` |
| OPEN + `isFull` (`applied ≥ capacity`) | `--ended` | `"마감"` | `rgba(120, 124, 130, 0.85)` (회색) | `ProgramCardDto.getCalendarChipLabel` 하드코딩 |
| ENDED | `--ended` | `"종료"` | 동일 (회색) | `ProgramCardDto.getCalendarChipLabel` 하드코딩 |
| SUSPENDED | 캘린더 미도달 | — | — | `ProgramService.search()` L44 `isActive()` 필터 |

**정본 근거**: `docs/00_assets/Program Calendar.dc.html` `chipOf()` (L953-960) + §5a 매핑 표 (L241-280).

5. **SUSPENDED** — `ProgramService.search()` L44 `isActive()` 필터로 캘린더 미도달. 매핑 불필요 (§4a #6)
6. **캘린더 임계값 만들지 않음** — 셀 색은 `getStatus()` + `isFull` 만 사용. 70/90% · D-3 · D-14 는 캘린더 밖에서만 (§5a 각주, §4a)
7. **마감임박 셀 색 제외** — 시급성은 우측 패널 `DdayChip` (D-3 빨강) + `CapacityBar` (≥90% error) 담당 (§3a 채택, §4a #3)
8. **빈 달 배너** (§7a)
   - 위치: 캘린더 격자 **위**, 월 네비 **아래**
   - 문구: "{현재월}에는 {탭 이름} 프로그램이 없어요. **{nearestMonth}월**에 N건 있어요." + `[N월 보기]`
   - null 케이스 (`nearestMonth = null`): 문구만 "조건에 맞는 프로그램이 없어요.", 버튼 없음
   - 이동 시 필터는 그대로 유지, 표시 월만 변경. 이동 후 배너 사라짐
   - 격자는 배너가 떠도 그대로 렌더 (빈 달이라도 날짜 표시)
9. **서버 신규 조회** — 필터 조건으로 결과가 있는 **가장 가까운 달** (표시 월 기준 절대 거리 최소) 1개 조회 (빈 달 배너용). `ProgramCalendarService` 에 신설. 동거리 tie-break 은 **미래 우선** (예: 8월 기준 6월/10월 각 1건 → 10월 선택)
10. **calendarSource = filtered** — 목록 뷰의 `activeFilters` (status·regions·centers) 그대로 재사용. view 만 스위치

---

## 4. 데이터 모델 gap 표

| prototype 필드 | 현재 엔티티 (`Program`) | 조치 |
|---|---|---|
| `pg.id` | `Program.id` | ✅ |
| `pg.title` | `title` | ✅ |
| `pg.center` | `organization` | ✅ |
| `pg.date` | `startDate` / `endDate` | ✅ |
| `pg.status` | `getStatus()` (OPEN/UPCOMING/ENDED/SUSPENDED) | ✅ |
| grouping 기준일 | `startDate` (§6a 확정) | ✅ 별도 컬럼 불요 (`applyStartDate` 신설은 admin 트랙 이월) |
| `isFull` | `applied ≥ capacity` 파생 | ✅ `ProgramCardDto` 재사용 |
| 이미지 | `imageUrl` | ✅ |
| `secondaryLabel` ("M/D 오픈") | `ProgramCardDto.secondaryLabel` | ✅ 재사용 |

**추가 컬럼 없음** — 캘린더 뷰는 기존 필드로 렌더 가능.

**admin 트랙 이월**: `applyStartDate` / `applyEndDate` 컬럼 도입 시 `getStatus()` 파생 규칙과 D-day 기준일 재검토 (ADMIN-00 §3-1).

---

## 5. 데이터 소비 지점

| 소비 지점 | prototype 참조 | 현재 상태 | 갭 |
|---|---|---|---|
| `/programs?view=calendar` 캘린더 grid | dc.html §1a | 미구현 (disabled 버튼) | 신설 |
| 캘린더 셀 pill → detail | tsx L766 | 기존 detail 재사용 | — |
| 우측 패널 카드 → detail | tsx L796 | 기존 detail 재사용 | — |
| 빈 달 배너 [N월 보기] → 이동 | dc.html §7a | 신설 | 서버 nearestMonth 조회 |
| 목록 뷰 필터 pill / 정렬 / status 탭 | list.html:83~150 | 유지 (뷰만 스위치) | view=calendar 시 정렬 영역만 조건부 숨김 |

---

## 6. 변경 범위

**신설**
- [ ] `program/ProgramController.java` — `list()` 에 `@RequestParam view`, `year`, `month` 추가. `view=calendar` 이면 캘린더 모델 세팅
- [ ] `program/ProgramCalendarService.java` (신규)
  - 월 단위 grouping (`Map<Integer, List<ProgramCardDto>>`, key = `startDate.getDayOfMonth()`)
  - 42셀 배열 산출 (6행 고정, 선행 공백 + 다음 달 채우기)
  - prev/next 월 파라미터
  - **nearestMonth 조회** — 필터 적용 결과에서 표시 월과 절대 거리 최소인 달 1개 (빈 달 배너용, 결과 없으면 null). 동거리 tie-break 은 미래 우선 (§3-A #9)
- [ ] `templates/program/list.html`
  - view toggle 활성화 (`disabled` 제거, `?view=calendar&...` link)
  - 조건부 renderer 분기 (view=calendar → fragment include)
- [ ] `templates/program/_calendar-fragment.html` (신규)
  - 툴바 (grid `1fr auto 1fr`, nav 정중앙) · 빈 달 배너 · 42셀 grid · 우측 패널 (hidden 데이터 포함)
- [ ] `templates/program/_calendar-day-panel-fragment.html` (선택) — 우측 패널 카드 리스트를 셀별로 pre-render 후 hidden 처리
- [ ] `static/css/main.css` — 캘린더 grid · 셀 · pill · 우측 패널 · 빈 달 배너 (dc.html 인라인 style → CSS 변수 기반)
- [ ] `static/js/program-calendar.js` (신규) — 셀 클릭 · × 닫기 · [오늘] 이동 · hidden 카드 데이터 렌더

**수정 없음**
- Program 엔티티 (신규 컬럼 불필요)
- Flyway 마이그레이션 (스키마 변경 없음)
- 계약 문서 `docs/design-contracts/programs.md` — PR-3 (선택) 로 분리, 3색 체계·"신청 마감 = 진행 종료" 정책 명시 (§4a #7 "문서화 필요")

**스코프 밖 (명시)**
- 모바일 진입점: `main.css` L6954 `@media (max-width: 767px) { .view-toggle { display: none; } }` 제거 — **PR-2 (모바일 트랙) 선행 항목**
- 모바일 캘린더 규격 (정사각 · 점 3개 · 하단 시트 `.filter-mobile-sheet` 재활용) — **PR-2**

---

## 7. PR 분할

- **PR-1 (본 스코프)** — 데스크톱 · view toggle 활성화 · 서버 rendering 캘린더 grid (6행 42셀) · 우측 패널 (client JS) · 월 이동 URL query · 빈 달 배너
- **PR-2 (선택 · 별도)** — 모바일 정사각 + 하단 시트 + `view-toggle` 진입점 정리 (dc.html §7b)
- **PR-3 (선택)** — 계약 문서 `docs/design-contracts/programs.md` 갱신 (3색 체계 · "신청 마감 = 진행 종료" 명시)

---

## 8. 검증 시나리오

### 정적
- `./gradlew compileJava test --tests ProgramCalendarServiceTest`
  - 42셀 배열 산출 (6행 고정, 2026-08 = 선행 공백 6 + 31일 + 후행 채움 5)
  - grouping key = startDate
  - nearestMonth 계산 (결과 있음 / 없음)
- `./gradlew test --tests ProgramListRenderTest` (view=calendar 분기 추가)

### 동적 (curl)
- `GET /programs?view=calendar` → 200, `<div class="program-calendar-grid"` 존재
- `GET /programs?view=calendar&status=ENDED` → 빈 달 배너 or nearestMonth 이동 링크 검증
- `GET /programs?view=calendar&year=2026&month=9` → 다른 달 렌더
- `GET /programs?view=calendar&status=OPEN&regions=수원시` → 필터 유지 확인
- `GET /programs` → 기본 grid 뷰 유지 (회귀)
- 정적 리소스 200 (main.css / program-calendar.js) 확인

### 인터랙션 (기능 E2E — CLAUDE.md 2026-08-13 필수 조항)
- 셀 클릭 → 우측 패널 slide-in + 카드 리스트 렌더
- × 클릭 → 우측 패널 닫힘
- [오늘] 클릭 → 오늘 셀 선택 (`LocalDate.now()` 기준)
- pill 클릭 → detail 이동, event bubbling 차단 (셀 패널 안 열림)
- 빈 달 배너 [N월 보기] 클릭 → 해당 달 이동 + 필터 유지

### 시각 (사용자)
- 3색 셀 렌더 · 오늘 원형 primary 배경 · 선택 셀 primary 테두리 + primaryBg 배경
- 툴바 grid 정렬: [오늘] 좌측 · `‹ YYYY년 M월 ›` 정중앙 (`1fr auto 1fr`)
- 빈 달 배너 문구·버튼 규격 (dc.html §7a)

### write→read 왕복
- 캘린더는 read-only 뷰 (신규 저장 없음) — 해당 없음

---

## 9. Q 리스트 (결정 완료)

| # | 질문 | 결정 | 근거 |
|---|---|---|---|
| Q1 | grid 행 수 | **6행 고정 (42셀)** | dc.html §1a L799 — 2026-08-01 은 토요일, 선행 공백 6 + 31일 = 37칸 필요. 5행이면 8/30·8/31 잘림 |
| Q2 | grouping 기준일 | **`startDate` 고정, 예외 없음** | dc.html §6a |
| Q3 | 월 이동 방식 | **URL query 서버 재렌더** (`?view=calendar&year=&month=&status=&regions=&centers=`) | 학습 프로젝트 SSR 우선, URL 공유 가능 |
| Q4 | 셀 클릭 → 우측 패널 | **client JS only** — hidden 데이터 렌더 후 display toggle | 왕복 오버헤드 없음 · HTMX 사고 표면 회피 |
| Q5 | 반응형 (<767px) | **PR-2 이월** — 모바일 별도 스펙 (dc.html §7b, 정사각 · 점 최대 3 · 하단 시트) | PR-1 스코프 최소화 |
| Q6 | pill 초과 문구 | **"+N건 더" 유지** | dc.html §1a `{{ c.more }}` |
| Q7 | 종료 프로그램 표시 | **필터 그대로 · 시작일 축 예외 없음 · 빈 달 배너로 이동** | dc.html §6a |
| Q8 | 여러 날 걸침 | **startDate 셀 1개만 pill** (bar span 미채택) | dc.html §1c 하단 각주 |

---

## 10. 위험 / 주의

- **월 이동 시 필터 상태 유지 필수** — `activeFilters` 모델 재활용, `th:href` 에 `status/regions/centers/year/month` 재바인딩
- **`th:fragment` 는 별도 파일로** — `_calendar-fragment.html` 필수 (CLAUDE.md F0h-c2 사고 재발 방지)
- **6행 고정 필수** — 5행 하드코딩 시 특정 월 (2026-08 등) 잘림. Q1 결정 이유가 이것
- **calendarSource = filtered** — 목록 뷰의 필터 파이프라인 재사용. 캘린더 전용 별도 쿼리 만들지 말 것
- **client JS 우측 패널** — HTMX 왕복 금지 (Q4 결정). 셀별 카드 데이터를 초기 렌더에 hidden 으로 포함해 JS 로 display toggle
- **nearestMonth 조회 성능** — 현재 구현은 필터 결과 전체를 메모리 로드 후 stream `.min()`. 시드 규모(<100건)에서 무영향. 프로덕션 대량 데이터 시 SQL 집계 (`GROUP BY date_trunc('month', start_date) ORDER BY ABS(...) LIMIT 1`) 로 최적화 여지 있음 (백로그, 별도 인덱스 검토 불요)
- **admin 이월** — `applyStartDate` / `applyEndDate` 컬럼 도입 시 D-day 기준일·`getStatus()` 파생 규칙 재검토 (ADMIN-00 §3-1)
- **계약 문서 갱신** — PR-3 로 분리. `docs/design-contracts/programs.md` 3색 체계 반영 및 "신청 마감 = 진행 종료" 현재 정책 명시 (§4a #7)
