# 디자인 계약 — 프로그램 목록 `/programs`

> **추출 기준**: `docs/00_assets/prototype.tsx` L815~944 (`ProgramList`) / 2026-07-28
> 보조 참조 — `FilterPopChip` L697~727 · `ProgramCalendar` L728~814 · `CapacityBar` L214~249 · `DdayChip` L250~258 · 토큰 `T` L10~21
> **검증 상태**: 테스트 연결됨 (`e2e/tests/visual-programs.spec.ts`) · 스크린샷 baseline 미등록
> **기계 계약**: `e2e/contracts/programs.ts` — px·색·폰트·개수는 전부 그쪽에서 자동 검사된다. 이 문서는 **판단이 필요한 구조**만 담는다.

## 1. 레이아웃 축과 폭 정책

단일 컬럼이다. 좌측 사이드바 필터는 없고, 필터가 전부 **상단 인라인 바** 한 줄에 들어간다.

| # | 영역 | 폭 정책 | 배경 | proto |
|---|---|---|---|---|
| 1 | 페이지 타이틀 바 | 전폭, 텍스트 중앙 | `surface` + 하단 1px `borderLight` | L838~840 |
| 2 | 본문 컨테이너 | **maxWidth 1160** + 좌우 80px (홈·헤더의 1440 과 다름) | `bg` | L841 |
| 3 | 필터 바 | 컨테이너 폭, `space-between` 1행 (wrap 허용) | — | L844~879 |
| 4 | 결과 영역 | 3열 그리드 (gap 16) 또는 캘린더 | — | L880~920 |
| 5 | 페이지네이션 | 중앙 정렬, marginTop 28 | — | L921~935 |
| 6 | Footer | — | — | L939 |

필터 바 내부 배치는 다음 2그룹으로 고정된다.

```
[좌] 상태탭 4 | 세로구분선 | 지역칩 | 청년센터칩 | (선택시) 초기화
[우] 전체 N건 · 기본순·마감임박순·인기순 · [목록|캘린더]
```

**주의**: 결과 카운트(`전체 N건`)와 정렬은 **필터 바 우측 그룹 안**에 있다 (L858~868). 결과 목록 위 별도 줄이 아니다. 또한 이 둘은 `view==='grid'` 일 때만 렌더된다 — 캘린더 뷰에서는 카운트·정렬이 사라지고 뷰 전환 토글만 남는다.

## 2. 화면 상태 머신

`ProgramList` 의 `useState` 8개 (L816~823).

| 상태 | 초기값 | UI 변화 |
|---|---|---|
| `sel` | `new Set()` | 지역·청년센터 **공용 선택 집합**. 크기>0 이면 칩이 primary 로 활성화되고 초기화 링크가 노출됨 |
| `active` | `'전체'` | 상태 탭. 목록 필터 기준 |
| `sort` | `'기본'` | 정렬. 활성 항목만 primary·600 |
| `fav` | `new Set([1])` | 카드 우상단 별 채움 (amber `#F59E0B`) |
| `waitlist` | `null` | 값이 있으면 `WaitlistModal` 오픈 (`__mode`: `open`/`waitlist`) |
| `notified` | `new Set()` | 알림 신청된 프로그램의 CTA 라벨이 `알림 신청됨 · 해제` 로 바뀜 |
| `view` | `'grid'` | `grid` ↔ `calendar` 전면 교체. 카운트·정렬·페이지네이션은 grid 에서만 |
| `loading` | `true` (700ms) | 스켈레톤 카드 6장 → 실제 카드 (L883~884) |

## 3. 필터 동작

- **즉시 반영**. 드롭다운 안의 체크박스를 누르는 순간 `setSel` 이 실행되고 목록이 갱신된다 (L715). 적용/취소 버튼도, 확인 단계도 없다. 드롭다운 헤더도 없다 — 옵션이 8개를 넘으면 상단에 검색 입력만 붙는다 (L709).
- **지역·청년센터는 같은 Set 을 공유**한다 (L835 `centerSel = sel`). 필터 판정은 `sel.size===0 || sel.has(p.region) || sel.has(p.center)` — 즉 두 축이 **OR** 로 합쳐진다 (L825). "부천시 + 상상대로" 를 고르면 둘 중 하나만 맞아도 노출된다.
- **초기화 링크**는 `sel.size>0` 일 때만 나타나고 `sel` 만 비운다 (L856). 상태 탭·정렬은 유지.
- 빈 상태의 `필터 초기화` 버튼은 범위가 더 넓다 — `active`·`sort`·`sel` 을 **전부** 초기화한다 (L890).

## 4. 상태 탭과 정렬

**탭 4개** (L847). `마감` 은 탭이 아니라 카드 뱃지로만 표현된다 (파일 헤더 L3 주석).

| 탭 | 필터식 |
|---|---|
| 전체 | `status !== '종료'` ← **종료를 제외한다** |
| 모집중 / 진행예정 / 종료 | `status === 탭명` |

**정렬 3종** (2026-07-29 wireframe WF-5-001-01 반영). 마감임박순은 wireframe 에 없어 제거.

| 값 | 라벨 | 규칙 |
|---|---|---|
| `default` | 기본순 | 로그인 유저의 즐겨찾기 프로그램 먼저 → 나머지는 최신 등록순 (createdAt DESC). 비로그인·즐겨찾기 없는 경우 최신순만 |
| `newest` | 최신순 | 최신 등록순 (createdAt DESC) |
| `popular` | 인기순 | 신청률(`applied/cap`) 내림차순 |

"전체" 탭은 종료 프로그램을 완전히 제외한다 (계약 `grid.excludeEnded`). 종료는 별도 "종료" 탭 진입 시에만 노출.

## 5. 뷰 전환 — 목록 / 캘린더

두 뷰 모두 즉시 전환 가능한 동등한 축이다. **PR-1 (2026-08-31 · #195)** 로 캘린더 뷰가 구현되며 정본 결정 사항은 `docs/00_assets/Program Calendar.dc.html` (7 섹션 · 1183 라인) 및 `docs/specs/F0f-calendar-view.md` 에 있다. 아래는 계약 요약.

### 5-A. 툴바 (dc.html §1a)
- grid `1fr auto 1fr` 로 [오늘] 좌측 · `‹ YYYY년 M월 ›` 정중앙 (E2E ±2px 정량 assert)
- "시작일 기준" 라벨 없음 (초회 설계에서 제외 확정, 2026-08-31)
- prev/next 링크에 status·regions·centers·sort 파라미터 전 보존

### 5-B. 요일·격자 (dc.html §1a)
- 요일 행 7칸 (일=error 빨강, 토=primary)
- **6행 42칸 고정** (Q1). 2026-08 등 첫 요일이 토·일일 때 5행이면 마지막 날짜 잘림 (`docs/00_assets/Program Calendar.dc.html:799` 각주)
- 셀 높이 104, gap 4, radius 8. 선택 셀은 primary 테두리 + `primaryBg`
- 셀당 프로그램 pill 최대 2건 (점+제목), 초과 시 `+N건 더`
- 오늘 셀 원형 primary 배경 + "오늘" 뱃지

### 5-C. 셀 pill 3색 (dc.html §5a) — **셀에는 임계값 없음**
- 진행예정 (UPCOMING) — secondary `#F97316`
- 모집중 (OPEN, `!isFull`) — primary `#3F30E9`
- 종료 (ENDED, `endDate < today`) **또는** OPEN + isFull (`applied ≥ capacity`) — textTri
- 마감임박 (D-3↓ · ≥90%) 은 셀 색에 반영하지 않음. 시급성은 우측 패널 chip 이 담당

### 5-D. 하단 범례 3종
- 진행예정 · 모집중 · 종료 (셀 pill 색과 1:1)

### 5-E. 빈 달 배너 (dc.html §7a)
- 위치: 격자 위, 월 네비게이션 아래
- 문구: `"{현재월}에는 {탭 이름} 프로그램이 없어요. **{nearestMonth}월**에 N건 있어요."` + `[N월 보기]` 버튼
- `nearestMonth == null` 케이스: `"조건에 맞는 프로그램이 없어요."`, 버튼 없음
- 배너 있어도 격자는 그대로 렌더 (빈 달이라도 날짜 표시)
- **nearestMonth 조회**: 필터 조건으로 절대 거리 최소인 달. 동거리 tie-break 은 **미래 우선** (스펙 §3-A #9)

### 5-F. 우측 패널 (dc.html §1a)
- 폭 **320px** 고정 (`.program-calendar-panel`)
- 초기 hidden 속성 (`display: none` — CSS `:not([hidden])` 로 방어)
- 날짜 클릭 시 slide-in. 헤더 `M월 D일` + `시작 N건` + 닫기(×)
- 프로그램 없는 날짜 클릭 시: `#program-calendar-panel-empty` 노출 (`이 날에는 프로그램이 없어요`)
- **`.program-calendar-panel-group`** 는 초기 `hidden`, JS 로 선택 날짜만 노출 (CSS `:not([hidden])` 로 `display: flex` 스코핑 — 2026-08-31 사고 방지)

### 5-G. 우측 패널 카드 chip 4종 매핑 (dc.html §5a `chipOf()`)

| 상태 조건 | chip modifier | 텍스트 | 배경 |
|---|---|---|---|
| UPCOMING | `--upcoming` | `"M/D 오픈"` (예: `8/28 오픈`, padding 없음) | secondary `#F97316` (오렌지) |
| OPEN + `days > 3` | `--open` | `"D-N"` (예: `D-12`) | `rgba(0, 0, 0, 0.55)` (dark) |
| OPEN + `0 ≤ days ≤ 3` (D-DAY 포함) | `--urgent` | `"D-N"` / `"D-DAY"` | `--color-error` (빨강) |
| OPEN + `isFull` | `--ended` | `"마감"` | `rgba(120, 124, 130, 0.85)` (grey) |
| ENDED | `--ended` | `"종료"` | 동일 (grey) |
| SUSPENDED | 캘린더 미도달 | — | — |

구현 위치: `ProgramCardDto.getCalendarChipLabel/Kind()`, fragment `_calendar-fragment.html`, CSS `main.css` `.program-calendar-panel-card-chip--*`.

### 5-H. 인터랙션 (dc.html §1a + client JS)
- 셀 클릭 → 선택 셀 하이라이트 + 우측 패널 표시
- pill 클릭 → `event.stopPropagation()` + `/programs/{id}` 이동 (셀 선택 안 됨)
- × 클릭 → 우측 패널 hidden
- [오늘] 클릭 → 현재 월이면 오늘 셀 선택, 다른 월이면 오늘 월로 이동 (필터 유지)
- **HTMX innerHTML swap 후에도 리스너 유지** — `document.body` 레벨 이벤트 위임 (`program-calendar.js`)

### 5-I. HTMX 라우팅 (2026-08-31 verify N2-2)
- `HX-Request` + `view=calendar` 요청 시 컨트롤러가 `_calendar-fragment :: calendar-region` 반환 (list fragment 아님)
- 필터 팝오버 적용 시 `applyFiltersFromPopovers()` → `window.location.search` 로 view=calendar 자동 유지

## 6. 카드 CTA 5분기

`capInfo(pg)` 결과와 `notified` 로 결정된다 (L906~915). 높이 34, radius 20(tagR), 폰트 13/600, 아이콘 15.

| 조건 | 라벨 | 아이콘 | 테두리 / 배경 / 색 | 클릭 동작 |
|---|---|---|---|---|
| 종료 (`ended`) | 지난 프로그램 | 없음 | `borderLight` / `borderLight` / `textTri` | 없음 (버튼 아님) |
| 중단 (`inactive`) | 운영이 중단되었어요 | 없음 | `border` / `borderLight` / `textTri` | 없음 |
| 진행예정 (`upcoming`) | 오픈 알림 받기 | bell | `secondary` / 투명 / `secondary` | 로그인 → 모달(`open`), 비로그인 → 로그인 유도 |
| 정원 마감 (`full`) | 빈자리 알림 받기 | bell | `border` / `borderLight` / `textSec` | 로그인 → 모달(`waitlist`), 비로그인 → 로그인 유도 |
| 그 외 (모집중) | 신청하기 | check | `primary` / 투명 / `primary` | **상세로 이동** |
| 위 2·3행 + `notified` | 알림 신청됨 · 해제 | bell | 동일 | 재클릭 시 알림 해제 |

## 7. CTA·링크 라우팅

| 요소 | prototype 목적지 | proto |
|---|---|---|
| 카드 본체 | `program-detail` | L893 |
| 카드 CTA `신청하기` | **`program-detail`** (신청 폼 직행 아님) | L911 |
| 카드 CTA 알림 계열 | `WaitlistModal` (화면 이동 없음) | L911 |
| 카드 별 | 로그인 → `fav` 토글 / 비로그인 → `onLoginClick()` | L897 |
| 캘린더 셀 프로그램 · 우측 패널 카드 | `program-detail` | L766 · L796 |
| 상태 탭 · 정렬 · 필터 칩 | 화면 이동 없음 (클라이언트 상태 전환) | L848 · L865 · L715 |

**현재 구현 갭**:
- `신청하기` 가 `/programs/{id}/apply` 로 바로 간다. prototype 은 상세 화면을 거친다.
- 상태 탭·정렬·페이지네이션이 전부 `<a href>` 풀 페이지 이동이다 (필터 칩만 HTMX 부분 갱신). prototype 은 전부 클라이언트 상태 전환이라 스크롤 위치가 유지된다.
- 비로그인 별이 `/login` 링크다. prototype 의 `onLoginClick()` 과 의도는 같다.

## 8. 기계 계약이 커버하지 않는 항목

자동 검사 대상이 아니므로 화면 작업 시 **사람이 확인**한다.

- **로딩 스켈레톤** — 700ms 동안 `ProgramCardSkeleton` 6장 (L118, L883~884). SSR 구현에는 개념 자체가 없다
- **목록·홈·마이페이지 D-day 칩 상태별 배경** — 종료 `rgba(120,124,130,0.85)` / 중단·마감 `rgba(0,0,0,0.55)` / 진행예정 `secondary` + `오픈 D-N` / 임박(D-3 이하) `error` (L250~257). 목록 뷰 구현은 임박(`--urgent`) 외 분기 클래스가 없어 셀렉터로 특정할 수 없다. **캘린더 우측 패널 카드는 별도** — §5-G chip 4종 매핑 참조 (기계 검사 가능)
- **카드 이미지 필터** — 종료 `grayscale(1) brightness(0.82)` / 마감 `grayscale(0.5)` (L895)
- 카드 hover `translateY(-2px)` + shadow 상승 (`card-hover`)
- 드롭다운 열림 시 `position:fixed inset:0` 백드롭으로 외부 클릭 감지 (L707) + `dropdown-enter` 애니메이션
- 알림 신청 모달(`WaitlistModal`) 열림 → 완료 토스트 → 1.1초 후 자동 닫힘 → CTA 라벨 전환 (L265~269)
- 캘린더 `오늘` 버튼 / 월 이동 `‹ ›` / 날짜 선택 시 우측 패널 슬라이드인
- 빈 상태의 `필터 초기화` 가 상태 탭·정렬까지 되돌리는지 (§3)

## 9. 현재 갭 (2026-07-29 계약 검사 결과)

`e2e/gap-reports/gap-programs.md` 참조. **62/74 통과 · 갭 12건** (P0 0 / P1 0 / P2 12) · 의도적 이탈 2건 · 이월 1건.

**PR1 (2026-07-29) — P1 5건 전부 해결**
- `grid.excludeEnded` — "전체" 탭에서 종료 프로그램 제외 (`ProgramSpec.notEnded()`)
- `resultcount.placement` — `.filter-right` 안 정렬 왼쪽으로 이동 + htmx OOB swap
- `sort.first.text` — 첫 정렬 라벨 `기본순` + 즐겨찾기 우선 정렬 로직 (wireframe WF-5-001-01)
- `viewtoggle.icon` — grid/calendar SVG 아이콘 추가
- `cta.radius` — 카드 CTA pill radius 20

**남은 P2 12건** — 시각 미세 조정만 (padding·margin·fontSize·bookmark 색·pagination). PR2 별도 진행.
- 카드 내부 리듬: 본문 상단 패딩 14(→12), 제목 하단 6(→3), 센터명 하단 4(→2), 기간 하단 8(→10)
- 뷰 토글 패딩 12(→14) / 폰트 12(→12.5)
- 페이지네이션 상단 여백 36(→28), 별 색 알파 0.9(→0.85), CTA 아이콘 14(→15), 정렬 gap 4(→2)

## 10. 사용자 결정 필요

계약과 구현이 다르지만 **의도적 이탈일 수 있는** 항목. 확정되면 계약의 `deviation` 필드에 사유를 넣어 검사에서 제외한다.

| # | 항목 | prototype | 현재 | 논점 |
|---|---|---|---|---|
| ~~Q1~~ | 첫 정렬 옵션 | `기본순` (정렬 미적용) | `기본순` (즐겨찾기 우선 + 최신순 fallback) | **✅ 확정 (2026-07-29): wireframe WF-5-001-01 정책 채택.** 즐겨찾기 우선 + fallback 최신 등록순으로 구현. 마감임박순은 wireframe 에 없어 제거 |
| Q2 | 지역 · 청년센터 필터 결합 | 하나의 Set, **OR** 결합 | 별도 파라미터, 교차 결합 추정 | prototype 쪽이 데모 편의를 위한 단순화일 가능성이 큼. AND 유지가 맞다면 계약이 아니라 이 문서에 확정 기록 |
| ~~Q3~~ | 드롭다운 적용 방식 | 체크 즉시 반영, 헤더·푸터 없음 | 헤더(제목·×) + 취소/적용 푸터 | **✅ 확정 (2026-07-28): prototype 대로 맞춘다.** 아래 §10-A 참조 |
| Q4 | 활성 필터 칩 줄 | 없음 | `.active-filter-bar` 에 선택값 칩 + 전체 초기화 | prototype 에 없는 **구현 추가 요소**. 유지하기로 하면 결과 카운트만 필터 바로 올리고 칩 줄은 남기는 절충안 가능 |
| Q5 | `신청하기` CTA 목적지 | 상세 화면 | 신청 폼 직행 | 클릭 수는 줄지만 상세를 안 보고 신청하게 됨 |
| Q6 | 빈 상태 카피 | 조건에 맞는 프로그램이 없어요 / 필터를 바꾸거나 초기화해 보세요 | 아직 없어요 / 선택하신 필터를 줄여보거나… | 2026-07-28 "구현 카피 유지" 결정에 따라 이미 `deviation` 처리함. 되돌릴지 여부만 확인 |

Q1 을 "현행 유지" 로 결정하시면 검사 대상 갭이 1건 줄어듭니다.

## 10-A. 확정 결정 — 드롭다운은 prototype 대로 (2026-07-28)

**결정**: 필터 드롭다운의 **헤더(제목·× 닫기)와 취소/적용 푸터를 제거하고, 체크 즉시 반영**으로 맞춘다. `deviation` 처리하지 않고 **갭으로 유지**한다.

**prototype 실제 구조** (`prototype.tsx` L708~723) — 컨테이너(`width 260`, `radius 12`, `padding 14`) 직계 자식이 **2개뿐**이다.

1. 검색 행 — **옵션이 8개를 넘을 때만** 노출 (L709)
2. 옵션 목록 — `gap 2`, `maxHeight 220`, `overflow-y auto` (L713)

헤더도 푸터도 없다. 옵션 `<label>` 의 `onClick` 이 곧바로 `setSel` 을 호출하므로(L715) 확인 단계 자체가 존재하지 않는다. 드롭다운 밖 클릭은 `position:fixed` 오버레이(L707)가 받아 닫는다 — 그래서 × 버튼이 필요 없다.

**계약 항목 2개로 표현된다**

| 항목 | 기대 | 의미 |
|---|---|---|
| `popover.noHeader` | `.filter-popover-header` 없음 | 제목·× 제거 |
| `popover.noApplyFooter` | `.filter-popover-footer` 없음 | 취소/적용 제거 = 즉시 반영 구조 |

**왜 `deviation` 이 아닌가**: 이 두 요소는 prototype 에 없는 것을 **더한** 게 아니라 prototype 의 동작(즉시 반영)을 **대체**했다. `POLICY.md` P-5 의 주의 조항이 다루는 경우이고, 그 조항은 대체형은 화면 Q 로 올려 결정하라고 정한다 — 결정 결과가 "맞춘다" 이므로 갭으로 남는다.

**구현 시 주의 — 요청 수**: 취소/적용 푸터를 둔 원래 의도는 다중 선택 시 HTMX 요청이 매 클릭마다 나가는 것을 막는 것으로 보인다. 즉시 반영으로 바꿀 때 이 문제가 되살아나므로 **debounce(예: 300ms) 후 1회 요청**으로 처리한다. prototype 은 클라이언트 상태라 공짜였지만 우리는 서버 왕복이 있다.

**기계 검사 불가 (구현 시 사람이 확인)**

- 체크 → debounce → URL·목록 갱신이 실제로 일어나는가 (E2E 시나리오로 별도 커버)
- 드롭다운 밖 클릭으로 닫히는가 (× 제거 후 유일한 닫기 경로가 되므로 반드시 확인)
- 검색 행이 **옵션 8개 초과일 때만** 노출되는가 (지역 18개 → 노출 / 청년센터 9개 → 노출). 참고로 prototype 의 검색 행은 `<span>` 짜리 비기능 목업이고 구현은 실제 `<input>` 이다 — 이건 P-5 개선이라 계약에 넣지 않았다
