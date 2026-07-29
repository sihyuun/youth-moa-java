# 디자인 계약 — 청년센터 `/centers`

> **추출 기준**: `docs/00_assets/prototype.tsx` L2161~2425 (`CentersScreen`) / 2026-07-28
> **검증 상태**: 테스트 연결됨 (`e2e/tests/visual-centers.spec.ts`) · 스크린샷 baseline 미등록
> **기계 계약**: `e2e/contracts/centers.ts` — px·색·폰트·개수는 전부 그쪽에서 자동 검사된다. 이 문서는 **판단이 필요한 구조**만 담는다.

이 화면은 2026-07-07 에 **prototype 3-column 인데 2-column + 별도 라우트로 구현되어 재작업**한 이력이 있다 (CLAUDE.md "prototype 3자산 병렬 정독" 규칙이 신설된 계기). 2026-07-09 F0h-c2 리팩터로 3-column 정합을 맞췄다고 기록돼 있고, 2026-07-27 갭 스캔은 "이미 정합 완료" 로 결론냈다. **본 계약이 그 결론을 정량으로 검증한 자리이며, 판정은 아래 §1 마지막에 있다.**

---

## 1. 3-column 레이아웃 축

콘텐츠 영역은 `display:flex` 한 줄에 컬럼 3개를 나란히 둔다 (proto L2269). 좌우 80px 패딩 · 컬럼 간격 16 · `align-items: flex-start`.

| # | 컬럼 | 폭 (상세 닫힘) | 폭 (상세 열림) | flex | 높이 | proto |
|---|---|---|---|---|---|---|
| 1 | 센터 리스트 | **360** | **240** (250ms ease 전환) | `flex-shrink:0` | 스크롤 `max-height 600` | L2271, L2282 |
| 2 | 인라인 상세 패널 | 렌더 안 함 | **320** 고정 | `flex-shrink:0` | 콘텐츠 높이 | L2320~2321 |
| 3 | 지도 | 남는 폭 전부 | 남는 폭 전부 | `flex:1` | **640 고정** | L2353 |

핵심은 2번이 **1·3번의 형제 컬럼**이라는 점이다. 상세는 별도 화면이 아니라 리스트와 지도 사이에 끼어드는 패널이고, 열리면 리스트가 360→240 으로 줄면서 자리를 내준다. 따라서:

- 상세를 별도 라우트 전용 템플릿으로 만들면 **아키텍처 위반** (2026-07-07 사고)
- URL 은 `/centers/{id}` 로 바뀌지만 **렌더되는 템플릿은 `/centers` 와 동일** (`center/list.html`). `centers-detail.js` 가 `history.pushState` + fragment fetch 로 처리하므로 풀 리로드도 없다
- 리스트 카드도 폭에 맞춰 **full ↔ compact 두 모드**로 전환된다 (proto L2288)

| 카드 모드 | 조건 | 패딩 | 구성 | proto |
|---|---|---|---|---|
| full | 상세 닫힘 | 14 | 센터명(15/700) + 운영배지 · 주소(12) · 구분선 · `프로그램 N건` + `상세보기 →` | L2300~2312 |
| compact | 상세 열림 | 10px 12px | 30x30 pin 아이콘 박스 · 센터명(13/600) + 지역(11) · 운영배지 | L2288~2298 |

정렬 pill(`이름순`/`프로그램많은순`) 은 **상세가 열리면 숨긴다** (`!detailId` — proto L2274). 240px 폭에 pill 2개가 들어가지 않기 때문.

### 3-column 정합 판정 (2026-07-28)

기계 계약의 **P0 8건 전부 통과**했다. 2026-07-27 스캔의 "이미 정합 완료" 결론은 **정량으로 확인된다.**

| P0 항목 | 기대 | 실제 | 판정 |
|---|---|---|---|
| `layout.display` | `flex` | `flex` | ✅ |
| `layout.columns` (컬럼 수) | 3 | 3 | ✅ |
| `layout.detail.isInline` (`.centers-layout > .centers-detail-col`) | 존재 | 존재 | ✅ |
| `layout.list.width` | 360 | 360 | ✅ |
| `layout.detail.width` | 320 | 320 | ✅ |
| `layout.map.flexGrow` | 1 | 1 | ✅ |
| `detail.list.width` (상세 열림) | 240 | 240 | ✅ |
| `detail.panel.width` (상세 열림) | 320 | 320 | ✅ |
| `detail.map.flexGrow` (상세 열림) | 1 | 1 | ✅ |

남은 갭 18건은 전부 P1(3) / P2(15) 스타일 편차이며 아키텍처 항목은 없다.

---

## 2. list ↔ map 양방향 연동 상태머신

prototype 은 상태 변수 2개로 전부 제어한다 (proto L2162~2163).

- `selectedId` — **지도 선택** (마커 확장 + 인포윈도우)
- `detailId` — **상세 패널 열림**

두 값은 독립이 아니라 아래 전이 규칙으로 묶여 있다 (proto L2180~2187, L2325, L2398, L2408).

| # | 트리거 | prototype 전이 | 화면 결과 | proto | 구현 상태 |
|---|---|---|---|---|---|
| T1 | 마커 클릭 | `selectedId = (같은 id ? null : id)` · **`detailId = null`** | 인포윈도우 열림, 상세 패널 **닫힘** | L2180~2183 | ⚠️ 상세를 닫지 않음 (§2-1) |
| T2 | 리스트 카드 클릭 | `detailId = (같은 id ? null : id)` · `selectedId = id` | 상세 열림 + 리스트 240 + 해당 마커 선택 | L2184~2187 | ⚠️ 재클릭 토글 없음 (§2-1) |
| T3 | 인포윈도우 `상세보기` | `detailId = infoCenter.id` · `selectedId = null` | 인포윈도우 닫히고 상세 패널로 승격 | L2408 | ✅ `centers:request-detail` 이벤트 |
| T4 | 인포윈도우 `×` | `selectedId = null` | 인포윈도우만 닫힘 | L2398 | ✅ `clearSelection()` |
| T5 | 상세 패널 `×` | `detailId = null` · `selectedId = null` | 상세 닫힘 + 리스트 360 + 마커 선택 해제 | L2325 | ✅ `closeDetail()` → `centers:detail-close` |
| T6 | 인포윈도우 노출 조건 | `infoCenter && !detailId` | **상세가 열려 있으면 인포윈도우를 띄우지 않는다** | L2393 | ⚠️ 상세 열림 중에도 열림 (§2-1) |
| T7 | 필터 변경 (검색·지역·운영중·정렬) | `filtered` 재계산 → 리스트·마커 동시 갱신 | 지도 마커도 같은 집합으로 필터 | L2171~2175, L2366 | ✅ `centers:filter-changed` partial swap |

카드↔마커 결합의 시각 표현:

| 상태 | 카드 | 마커 |
|---|---|---|
| `id === selectedId` 또는 `id === detailId` | 배경 `primaryBg` + 보더 `primary` | 원형→pill 확장 + 센터명 라벨 + primary 배경 | 
| 그 외 | 배경 `surface` + 보더 `borderLight` | 30px 원형 (운영중 흰 배경 / 종료 회색) |

proto L2287 (카드) · L2372~2376 (마커).

### 2-1. 구현이 prototype 과 다른 전이 3건 (기계 검사 불가 — 사람 판단 필요)

| 항목 | prototype | 현재 구현 | 영향 |
|---|---|---|---|
| T1 마커 클릭 시 상세 처리 | `detailId = null` 로 상세를 **닫는다** | `selectMarker()` 가 하이라이트 + 인포윈도우만 수행, 상세는 그대로 (`center-map.js` `selectMarker`) | 상세 패널과 인포윈도우가 **동시에** 뜬다 |
| T6 인포윈도우 노출 조건 | `!detailId` 일 때만 | 조건 없음. `centers:detail-open` → `_selectMarker()` 가 인포윈도우까지 연다 (`center-map.js` L20~24) | 위와 같은 건. 320px 패널 + 300px 인포윈도우 중복 노출 |
| T2 카드 재클릭 | 같은 카드 재클릭 시 상세 **닫힘** (토글) | `openDetail()` 만 호출 — 항상 열림 유지 (`centers-detail.js` L116~123) | 닫으려면 상세 패널 `×` 를 눌러야 함 |

세 건 모두 "T1·T6 을 prototype 대로 맞출지" 하나의 결정으로 묶인다 → §6 Q1.

---

## 3. 필터 바 구성

전폭 `surface` 바 + 하단 1px `border`. 높이 56, 항목 간격 12, 좌우 80px (proto L2198~2199).

| 순서 | 요소 | 규격 | proto |
|---|---|---|---|
| 1 | 센터명 검색 | 250x38 · radius 8 · **좌측 search SVG** + input(13) + 입력 시 clear SVG | L2201~2204 |
| 2 | 지역 드롭다운 | 38 높이 · min-width 140 · radius 8 · **보더 1.5px** · 미선택 시 `지역 선택` + chevD SVG | L2208~2224 |
| 2a | └ 드롭다운 패널 | 260 폭 · radius 12 · 상단 시·군 검색 input + 220 스크롤 목록 | L2226~2234 |
| 2b | └ 목록 행 | 17px 체크박스 + 시·군명(14). 검색어는 `#FEF08A` 하이라이트 | L2241~2250 |
| 3 | (spacer) | `flex:1` | L2258 |
| 4 | 운영중 토글 | 40x22 트랙 · 18px 손잡이 · 라벨 `운영중만 보기`(13) | L2260~2265 |

선택된 지역은 트리거가 `primary` 보더 + `primaryBg` 배경으로 바뀌고, pin SVG + 18px 원형 clear 버튼이 붙는다 (proto L2210~2217). **현재 구현은 pin 을 📍 이모지, chevron 을 ▾ 문자, clear 를 × 문자로 대체**해 CLAUDE.md "prototype 이 SVG 인 곳에 이모지로 대체 금지" 규칙을 위반한다 (§5 갭 참조).

---

## 4. CTA·링크 라우팅

| 요소 | prototype 목적지 | 현재 구현 |
|---|---|---|
| 리스트 카드 | 인라인 상세 패널 열기 (라우팅 없음) | `href="/centers/{id}"` 를 JS 가 가로채 fragment fetch + `pushState` — 동작은 동일, JS 미실행 시 SSR fallback |
| 인포윈도우 `상세보기` | 인라인 상세 패널 열기 | `centers:request-detail` 이벤트 → 동일 |
| 상세 패널 `프로그램 전체보기` | `programs` (필터 없음) | `/programs?region={센터 지역}` — **지역 필터를 얹었다** |
| 상세 패널 `×` | 상세 닫기 | `/centers?{필터}` anchor 를 JS 가 가로채 닫기 |
| `이 지역에서 검색` | 토스트만 (목업) | 실제 지도 bounds 재조회 |

`프로그램 전체보기` 의 지역 필터 부가는 prototype 보다 나은 동작으로 보이나 명시적 결정 기록이 없다 → §6 Q2.

---

## 5. 기계 계약이 커버하지 않는 항목

자동 검사 대상이 아니므로 화면 작업 시 **사람이 확인**한다.

### 5-1. 카카오맵 SDK 의존 (E2E 에서 원천 검사 불가)

E2E 헬퍼 `abortExternal(page)` 가 `dapi.kakao.com` 을 포함한 모든 외부 도메인을 차단한다. 따라서 지도 **내부**는 계약에 넣지 않았고, 컨테이너의 존재·크기(640)·radius 만 검사한다.

- 마커 렌더·좌표 정확도·선택 시 pill 확장 + 센터명 라벨 (proto L2366~2377)
- 20개 이상일 때 클러스터링 (`MarkerClusterer`) — prototype 에는 없는 구현 확장
- 인포윈도우 (300 폭 · 이미지 110 · 상태배지 · 상세보기/공유 버튼) — proto L2394~2417
- `fitAndClamp()` 초기 줌 레벨, 마커 선택 시 `setLevel(4)` + `panBy` 중앙 정렬
- `내 위치` geolocation 버튼 — prototype 에 없는 구현 확장

### 5-2. 그 외 기계 검사 불가

- **카드 보더 1.5px** — Chrome 이 computed `border-width` 를 디바이스 픽셀로 반올림해 dpr=1 에서 항상 `1px` 로 보고한다. prototype 도 동일하게 렌더되므로 이 값으로는 갭을 판별할 수 없어 계약에서 뺐다 (proto L2287)
- 리스트 폭 360↔240 **전환 애니메이션의 부드러움** (250ms 값 자체는 계약에 있음)
- 카드 hover ↔ 마커 zIndex 999 승격 연동 (`center-map.js` mouseenter/mouseleave)
- 지역 드롭다운 검색어 하이라이트(`#FEF08A`) 및 외부 클릭 시 닫힘
- 운영 배지 판정식 — 구현은 `hasSchedule` 인 센터에만 배지를 렌더한다 (`isActive && isOpenNow` 조합). prototype 은 mock `open` 불리언이라 전 센터에 배지가 있다. 시드 48개 중 3개가 배지 없음 → **데이터 성격 차이이므로 갭 아님**

---

## 6. 현재 갭 (2026-07-28 계약 검사 결과)

`e2e/gap-reports/gap-centers.md` 참조. **80/98 통과 · 갭 18건** (P0 **0** / P1 3 / P2 15) · 의도적 이탈 2건.

무거운 순:

1. **P1 — 상세 CTA `프로그램 전체보기` 가 35px** (계약 42px, `Btn size="m"`). 폰트도 13 (계약 14). 상세 패널의 유일한 액션이 prototype 대비 눈에 띄게 작다
2. **P1 — 센터명 검색 박스에 search 아이콘이 없다.** prototype 은 250x38 박스 안 좌측에 lucide search SVG (proto L2202). 현재는 placeholder 텍스트만
3. **P1 — 지역 드롭다운 chevron 이 `▾` 문자.** prototype 은 `<Icon n="chevD">` SVG. CLAUDE.md "prototype 이 SVG 인 곳에 이모지로 대체 금지" 위반이 남아 있다 (선택 상태의 pin `📍`·clear `×` 도 같은 건이나 기본 상태에서 렌더되지 않아 기계 검사 대상 외)
4. **P2 — 상세 패널 메타 아이콘 박스가 `primaryBg`** (계약 `primaryLight`). radius 도 8 (계약 6). prototype 은 아이콘 배경만 한 단계 진한 톤을 쓴다
5. **P2 — 콘텐츠 상단 여백이 32** (계약 16). `.centers-container` 16 + `.centers-layout` 16 이 중복 적용됐다
6. **P2 — 정렬 pill 컨테이너에 `--color-bg` 배경**이 깔려 있다. prototype 은 컨테이너 배경 없이 active pill 만 `primaryBg` (proto L2275)

나머지 P2 는 폰트 13→14(검색·지역 라벨), 간격 6→8 / 7→8, 상세 본문 좌우 패딩 18→16, line-height 1.6→1.5 등 1~2px 급 편차다.

---

## 7. 사용자 결정 필요

계약과 구현이 다르지만 **의도적 이탈일 수 있는** 항목. 확정되면 계약의 `deviation` 필드에 사유를 넣어 검사에서 제외하거나, 구현 수정 티켓으로 넘긴다.

| # | 항목 | prototype | 현재 | 논점 |
|---|---|---|---|---|
| Q1 | 상세 패널 ↔ 인포윈도우 배타 (§2-1 T1·T6) | 상세가 열리면 인포윈도우를 띄우지 않고, 마커 클릭은 상세를 닫는다 | 둘이 동시에 뜬다 | 화면 폭 1280 에서 리스트 240 + 상세 320 + 인포윈도우 300 이 겹친다. prototype 배타 규칙을 복원할지, 현행 동시 노출을 정식 사양으로 확정할지 |
| Q2 | 상세 CTA 목적지 | `/programs` (필터 없음) | `/programs?region={센터 지역}` | 센터 상세에서 넘어가는 맥락상 지역 필터가 자연스러움. 이탈로 확정할지 |
| Q3 | 카드 재클릭 토글 (§2-1 T2) | 같은 카드 재클릭 → 상세 닫힘 | 항상 열림 유지 | 닫는 수단이 상세 `×` 하나뿐이어도 되는지 |
| Q4 | 운영 배지 노출 정책 (§5-2) | 전 센터 노출 (mock `open`) | `hasSchedule` 인 센터만 (48개 중 45개) | 운영시간 데이터가 없는 센터에 "운영종료" 를 찍지 않으려는 F0h 결정. 계약 제외로 확정할지 |
| Q5 | 지도 확장 기능 | 없음 | 클러스터링 · `내 위치` 버튼 · 공유 버튼 | prototype 에 없는 추가 기능. 정식 사양으로 승격해 별도 계약 항목을 만들지 |

Q1 을 "prototype 배타 규칙 복원" 으로 결정하시면 `center-map.js` 의 `centers:detail-open` 리스너에서 인포윈도우 open 을 분리하는 수정이 필요하고, "현행 유지" 로 결정하시면 이 문서 §2 표의 ⚠️ 를 정식 사양으로 갱신합니다.
