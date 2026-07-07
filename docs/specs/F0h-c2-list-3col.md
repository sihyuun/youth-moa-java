# F0h-c2 — 청년센터 목록 3-column 재구성

- **상태**: `spec_confirmed`
- **브랜치 후보**: `feature/F0h-c2-list-3col`
- **선행**: F0f (Region 엔티티) — 완료. 현재 코드 `region/Region.java` 존재
- **후속**: `F0h-c3` (인라인 상세 패널 내용 채우기 · 인포윈도우 · 마커 클러스터링)
- **관련 사고**: 2026-07-07 F0h 사고 — prototype 3-column 을 2-column 으로 잘못 옮김

---

## 1. 디자인 출처

| 자산 | 위치 | 용도 |
|---|---|---|
| **prototype.tsx** | `docs/00_assets/prototype.tsx` L1897~2161 (`CentersScreen`) | 컴포넌트 구조·상태머신·조건부 폭 (**최우선**) |
| **HANDOFF.md** | `docs/00_assets/HANDOFF.md` §5.15 L533~554 (⚠️ 필수반영 블록 포함) | 정책 원본 |
| **prototype.html** | `docs/00_assets/prototype.html` (centers 섹션) | 시각·토큰 확인 (별도 인용 없음; tsx 와 동일 구조) |
| 비교 대상 (현재 코드) | `src/main/resources/templates/center/list.html`, `center/CenterController.java`, `center/CenterListItem.java`, `js/center-map.js` | 2-column 잘못된 구현 |

---

## 1-A. 아키텍처 레벨 대조표

| 항목 | 현재 코드 (`list.html`) | prototype.tsx L2005~2156 | HANDOFF §5.15 | 채택 |
|---|---|---|---|---|
| 컬럼 수 | **2** (aside + map-panel) | **3** (list 360 + detail 320 + map flex) | 3 (좌 360 + 우 flex, 상세 패널은 §5.15 시안에 렌더) | **3-column** |
| 리스트 폭 | `aside` (CSS 미확인, 400 근처) | `detailId ? 240 : 360` (조건부) | 360px (좌측 고정) | **360 → 240 (compact)** |
| 상세 패널 | **없음** (`/centers/{id}` 별도 페이지만) | 인라인 패널 320px, `detailId` 존재 시만 표시 | 시안에 인라인 패널 렌더 (§5.15 도식) | **인라인 320px + `/centers/{id}` 병존** |
| 지도 폭 | `.centers-map-panel` (flex:1) | `flex:1`, `height:640, minHeight:640` | flex 우측 | **flex:1 (좌측 확장 시 축소)** |
| 지역 필터 | native `<select>` | 커스텀 드롭다운 (검색+체크리스트+하이라이트) L1942~1993 | "지역 더보기" 팝오버 UX 언급 (line 302), §5.15 필수반영 항목 | **커스텀 드롭다운** |
| 정렬 | 없음 (Controller `sort=name` param 만 존재) | pill 토글 `[이름순 | 프로그램많은순]` L2011~2016 | ⚠️ "정렬: 이름순 / 프로그램많은순" 필수반영 | **pill 토글** |
| 운영중 필터 | `<input type="checkbox">` + label | 토글 스위치 (width:40 height:22 rounded pill) L1996~2001 | "운영중" 상태 뱃지 강조 | **토글 스위치** |
| 카드 구조 | name+badge / region / address / phone | name+badge / addr / 구분선 / `프로그램 N건` + `상세보기 →` L2036~2049 | "이름 + 운영중/종료 뱃지 + 주소 + 운영시간/전화 + 진행중 프로그램 수" | **prototype 카드 (phone 제거, programCount 추가)** |
| 카드 compact | 없음 | `detailId` 존재 시 아이콘+이름+region+뱃지 한 줄 L2024~2034 | — (prototype 만) | **compact 모드 채택** |
| URL bookmark | `/centers/{id}` = 별도 상세 페이지 | React 상태 (URL 미반영) | — | **`/centers/{id}` 접근 시 목록+상세 패널 자동 open** |
| 카운트 표시 | `총 N개 센터` (좌측 상단) | `총 <strong>N</strong>개 센터` L2009 | — | 유지 |
| "이 지역에서 검색" 버튼 | 없음 | 지도 상단 중앙 L2117~2123 | ⚠️ 필수반영 (지도 idle bounds 재검색) | **c3 로 분리** (버튼 자리만 확보) |
| 마커 클러스터링 | 없음 (개별 마커) | 개별 마커 L2102~2116 | ⚠️ 필수반영 (`MarkerClusterer`) | **c3 로 분리** |

**결론**: c2 스코프는 **레이아웃 3-column 재구성 + 필터바 UX (드롭다운·정렬·토글) + 카드 리렌더 + URL bookmark**. 지도 SDK·클러스터링·인포윈도우·"이 지역에서 검색" 실동작은 c3.

---

## 1-B. HANDOFF §5.15 ⚠️ 필수반영 항목 매핑

| HANDOFF 요구 | c2 반영 | c3 이월 |
|---|---|---|
| 마커 클러스터링 (`MarkerClusterer`) | | ✔ |
| "이 지역에서 검색" (지도 idle → bounds 재검색) | 버튼 UI 자리만 (비활성) | ✔ 동작 |
| 목록 무한 스크롤 (독립 스크롤 영역) | ✔ 독립 스크롤 max-height 600, `총 N개` 고정 | 무한스크롤 페치는 c3 |
| 지도↔목록 동기화 (카드 hover / 마커 클릭) | 카드 클릭 → 상세 패널 open (지도 마커 강조는 기존 center-map.js 그대로) | ✔ hover 강조·양방향 |
| 정렬 (이름순 / 프로그램많은순) | ✔ pill 토글 |  |
| 내 주변순 |  | ✔ |

---

## 2. 8개 상태 머신 (prototype.tsx L1898~1905)

React 상태 8종을 Thymeleaf + URL query + 소량 client JS 로 매핑.

| React state | 초기값 | c2 매핑 (서버/클라이언트) |
|---|---|---|
| `selectedId` | null | **client only**. 지도 마커 클릭 시 인포윈도우 (c3). c2 에선 미사용. |
| `detailId` | null | **URL path**. `/centers/{id}` 진입 시 서버가 `detailCenter` 모델 세팅. 목록 카드 클릭은 `hx-get /centers/{id}` (c3) — c2 는 anchor `href` 로 페이지 전환도 허용 |
| `regionOpen` | false | **client only** (드롭다운 open/close) |
| `regionQuery` | '' | **client only** (드롭다운 내부 검색 입력) |
| `selectedRegion` | null | **URL query `region=`**. 서버가 filter |
| `onlyOpen` | false | **URL query `onlyActive=true`**. 서버 filter |
| `centerSearch` | '' | **URL query `q=`**. 서버 filter |
| `sortBy` | 'name' | **URL query `sort=name|programs`**. 서버 sort |

→ 필터 변경 시 form submit (GET `/centers?q=&region=&onlyActive=&sort=`). 초기 c2 는 full reload. HTMX 부분 리렌더는 후속 티켓 (`F0h-c4-htmx`) 로 분리.

---

## 3. 변경 범위 (파일 단위)

### 수정
- [ ] `src/main/resources/templates/center/list.html` — 2-col → 3-col 전면 재구성
- [ ] `src/main/resources/static/css/main.css` — `.centers-filter-*`, `.centers-layout`, `.center-card-*` 신규/조정. custom-dropdown / toggle-switch / sort-pill 클래스 추가
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/center/CenterController.java`
  - `/centers` 에 optional `detailId` 처리 추가 **불필요** — 대신 `/centers/{id}` 를 list.html 재사용 방식으로 렌더
  - `list()` 는 그대로. sort/region 리스트 소스만 `RegionRepository` 로 교체
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/center/CenterService.java`
  - `distinctActiveRegions()` → `RegionRepository.findAll()` 기반으로 교체 (F0f 일관성). 기존 로직 fallback 유지 여부 = **교체**
  - `CenterListItem` 에 `programCount` 필드 추가 → Service 에서 계산해서 채움
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/center/CenterListItem.java` — `int programCount` 필드 + 생성자 확장
- [ ] `src/main/resources/static/js/center-map.js` — 마커 클릭 시 카드 하이라이트 정도만 유지 (실 인포윈도우는 c3)

### 신규
- [ ] `src/test/java/io/github/sihyuuun/youthmoa/center/CenterControllerTest.java` — 확장 (혹은 신규)
- [ ] E2E: `e2e/tests/centers-list-3col.spec.ts` (별도 저장소 `youth-moa-java-e2e`)

### 변경 없음
- `Center.java`, `CenterRepository.java`, `Region.java`, `RegionRepository.java`
- `templates/center/detail.html` (별도 상세 페이지는 그대로 존재. c2 는 인라인 패널만 추가)

---

## 4. 레이아웃 스펙 (상세 수치)

### 4.1 필터바 (surface bg, border-bottom, padding `0 80px`, height 56)

```
[검색 250×38][지역 드롭다운 140×38][flex spacer][운영중 토글 40×22 + 라벨]
gap: 12px, align-items: center
```

- **검색 input**: `width:250 height:38 radius:8 border:1px var(--color-border)`. 좌측 search 아이콘 15px, 우측 close 아이콘 (값 있을 때만)
- **지역 드롭다운 트리거**: `min-width:140 height:38 radius:8 border:1.5px`. 선택 없음: 회색 텍스트 + chevron. 선택 있음: primary 테두리 + primaryBg 배경 + pin 아이콘 + primary 텍스트 + × 원형 버튼 (18×18)
- **지역 드롭다운 패널**: `position:absolute top:44 left:0 width:260`, `radius:12`, `box-shadow:0 8px 32px rgba(0,0,0,0.14)`, `z-index:300`
  - 상단: 검색 input (34 height, primary 테두리, autofocus)
  - 리스트: `max-height:220 overflow-y:auto`. 각 row `padding:9px 14px`, 체크박스 17×17 + region 이름
  - 선택 상태: `background: primaryBg`, 체크박스 primary fill, 텍스트 primary + fontWeight 600
  - **하이라이트**: 검색어 매치 부분을 `background:#FEF08A` + fontWeight 700 로 highlight
- **정렬 pill (리스트 컬럼 상단)**:
  - `[이름순][프로그램많은순]` — 각 pill `padding:4px 10px radius:pill fontSize:12`
  - 활성: `background:primaryBg color:primary fontWeight:600`
  - 비활성: `color:textTri`
  - `detailId` 있을 때는 **숨김** (compact 리스트에선 정렬 UI 숨김 — tsx L2010 `{!detailId && ...}`)
- **운영중 토글 스위치**:
  - 컨테이너 `width:40 height:22 radius:11 background: onlyOpen?primary:border`
  - 핸들 `width:18 height:18 border-radius:50% background:#fff position:absolute top:2 left:onlyOpen?20:2 transition:left 200ms`
  - 라벨 텍스트 "운영중만 보기" fontSize:13

### 4.2 콘텐츠 (`padding:16px 80px 40px, gap:16, align-items:flex-start`)

- **리스트 컬럼** `width: ${detailId ? 240 : 360}, flex-shrink:0, transition: width 250ms ease`
  - 상단: `총 N개 센터` (fontSize:13 textSec, strong text) + 정렬 pill (detailId 없을 때만)
  - 스크롤 컨테이너: `max-height:600 overflow-y:auto padding-right:4 flex-direction:column gap:8`
  - 빈 상태: "조건에 맞는 센터가 없습니다" (fontSize:14 textTri padding:40px 0)

- **상세 패널** (`detailId` 있을 때만 렌더) `width:320 flex-shrink:0 background:surface radius:var(--radius-lg) border:1px solid border box-shadow:0 4px 16px rgba(63,48,233,0.08) overflow:hidden`
  - c2 에선 **껍데기만** — 상단 이미지 자리 (160 height 회색 placeholder), 이름, 주소, 운영시간, 전화, "진행중 프로그램 N건", "프로그램 전체보기" CTA
  - 내용 상세 스타일은 c3 에서 완성 (본 c2 는 3-col 뼈대 확립이 목표)
  - **닫기 버튼**: 우측 상단 × — 클릭 시 `/centers` 로 이동 (URL 상태 반영)

- **지도 컬럼** `flex:1 border-radius:var(--radius-lg) overflow:hidden border:1px solid border height:640 min-height:640`
  - 기존 `#center-map` + `center-map.js` 유지. 폭이 flex 로 재계산되도록 css 만 조정

### 4.3 카드 (`.center-card`)

**Full 모드** (`detailId` 없음, width:360):
```
padding:14 radius:var(--radius-lg) background:surface border:1.5px solid borderLight
─ 헤더: [이름 700 15px] [운영뱃지 padding:2 7 radius:pill fontSize:11]
─ 주소: 12px textSec (한 줄 ellipsis)
─ 구분선 (border-top:1px solid borderLight, padding-top:8)
  · 좌: "프로그램 N건" (primary 600 12px)
  · 우: "상세보기 →" (textTri 12px)
```
활성 (`c.id === detailId` 또는 hover): `background: primaryBg, border-color: primary`

**Compact 모드** (`detailId` 있음, width:240):
```
padding:10 12 gap:8 flex row align-items:center
─ 아이콘 박스 30×30 radius:6 (활성:primary bg + 흰 pin / 비활성:borderLight + textTri pin)
─ 중앙: [이름 600 13px ellipsis] / [region 11px textTri]
─ 우측: 운영뱃지 (padding:1 6 radius:pill fontSize:10)
```

- 카드 클릭 → 링크 이동 `/centers/{id}?q=...&region=...&onlyActive=...&sort=...` (필터 상태 preserve)
- 활성 카드 = `detailId` 와 일치하는 카드 → primaryBg 배경 + primary 테두리

---

## 5. Controller / Service 명세

### 5.1 `CenterController.list()`
```java
@GetMapping({"/centers", "/centers/{detailId}"})
public String list(
    @PathVariable(required = false) Long detailId,
    @RequestParam(required = false) String q,
    @RequestParam(required = false) String region,
    @RequestParam(defaultValue = "false") boolean onlyActive,
    @RequestParam(defaultValue = "name") String sort,
    Model model) {

  List<CenterListItem> centers = centerService.list(q, region, onlyActive, sort);
  List<String> regions = regionService.findAllNames(); // Region 엔티티 기반

  Center detailCenter = null;
  if (detailId != null) {
    detailCenter = centerService.findById(detailId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "..."));
  }

  model.addAttribute("currentPage", "centers");
  model.addAttribute("centers", centers);
  model.addAttribute("regions", regions);
  model.addAttribute("detailCenter", detailCenter);   // null 이면 상세 패널 미렌더
  model.addAttribute("filterQ", ...);
  model.addAttribute("filterRegion", ...);
  model.addAttribute("filterOnlyActive", onlyActive);
  model.addAttribute("filterSort", sort);
  model.addAttribute("kakaoMapAppKey", kakaoMapAppKey);
  return "center/list";
}
```
→ **한 컨트롤러 메서드가 `/centers` 와 `/centers/{id}` 를 둘 다 처리** (Q1 B 결정 반영). 기존 `detail(@PathVariable Long id)` 는 삭제.

### 5.2 `CenterService`
- **신규**: `RegionRepository` 주입 (F0f 일관성).
- `distinctActiveRegions()` **삭제** → controller 에서 `regionRepository.findAll(Sort.by("name"))` 로 직접 호출 (혹은 얇은 `RegionService.findAllNames()` 신설 — 후자 권장)
- `list()` 내 `CenterListItem.from(c)` 호출을 `CenterListItem.of(c, programCount)` 로 교체. programCount 는 `ProgramRepository.countByCenterIdAndStatus(...)` 배치 조회 (N+1 방지)
  - **`Q4` 결정 확정: Region 엔티티 조회**. 하드코딩 리스트·`Center` 테이블 distinct 는 사용 안 함
  - programCount 소스는 `Program` 엔티티 — `status = RECRUITING` 인 것만 카운트 (HANDOFF "진행중 프로그램")

### 5.3 `CenterListItem`
```java
public record CenterListItem(
    Long id, String name, String region, String address, String phone,
    BigDecimal latitude, BigDecimal longitude, boolean isActive,
    int programCount) {  // ★ 추가
  public static CenterListItem of(Center c, int programCount) { ... }
  public boolean hasCoordinates() { ... }
}
```

---

## 6. 갭 리스트 (구현 시 삭제/치환)

| # | 항목 | 현재 | 명세 |
|---|---|---|---|
| G1 | `<select name="region">` native | 회색 select | 커스텀 드롭다운 (검색+체크리스트+하이라이트) |
| G2 | `<input type="checkbox">` 운영중 | 네이티브 체크박스 | 토글 스위치 |
| G3 | 정렬 UI | 없음 | pill 토글 (`sort=name|programs`) |
| G4 | 카드 phone 필드 | 렌더 | **제거** (prototype 카드에 없음. 상세 패널에서만 노출) |
| G5 | 카드 "프로그램 N건" | 없음 | **추가** (primary 색상, 하단 좌측) |
| G6 | 카드 "상세보기 →" | 없음 | **추가** (textTri, 하단 우측) |
| G7 | 상세 접근 | `/centers/{id}` 별도 페이지만 | **인라인 패널 + `/centers/{id}` 접근 시 목록에 오버레이** |
| G8 | 리스트 폭 조건부 | 고정 (400 근처) | 360 ↔ 240 transition |
| G9 | 지역 리스트 소스 | `centerRepository` distinct | `regionRepository.findAll()` |
| G10 | `CenterListItem.programCount` | 필드 없음 | 추가 |

---

## 7. 검증 시나리오 (ym-qa)

### 정적 검증
- `./gradlew compileJava` 통과
- `./gradlew test --tests CenterControllerTest` — `/centers`, `/centers/{id}` 응답 view name + model attribute (`centers`, `regions`, `detailCenter`)
- `./gradlew test --tests CenterServiceTest` — sort=programs 정렬 검증, region filter (Region 엔티티 기반) 검증

### 동적 검증 (Claude Preview 또는 curl)
- `GET /centers` → 200. HTML 에 `.centers-layout` 3 컬럼 컨테이너 존재 확인. 지역 드롭다운 트리거 요소 (`.centers-filter-region-trigger`) 확인. `.center-card` 다수 확인. **`detailCenter` null 이므로 `.centers-detail-panel` 미렌더**
- `GET /centers/1` → 200. 위 요소 + `.centers-detail-panel` 존재. `total N개 센터` 그대로.
- `GET /centers?sort=programs` → 카드 순서가 programCount 내림차순
- `GET /centers?onlyActive=true` → 운영종료 카드 미노출. 토글 스위치가 active 상태 렌더 (`checked` 또는 `data-on="true"`)
- `GET /centers?region=수원시` → 수원시 카드만
- `GET /centers?q=상상` → 이름/지역 include 카드만
- `GET /css/main.css | grep centers-filter-region-trigger` → 신규 스타일 서빙 확인
- 리스트 카드 anchor href 에 `?q=&region=&onlyActive=&sort=` 필터 파라미터 preserve 확인

### 회귀 검증
- `/centers/{id}` 직접 접근 → 200 (기존 `/centers/1` E2E 케이스 계속 통과해야 함). 단 렌더 소스가 `center/detail` → `center/list` 로 변경됨을 QA 노트에 명시
- 지역 필터 form submit (기존 native select 기반 QA 케이스) → 커스텀 드롭다운으로 대체됐으므로 E2E spec 갱신 필요

### E2E 시나리오 (Playwright — `centers-list-3col.spec.ts`)
1. **초기 렌더**: `/centers` 방문. 리스트 컬럼 폭 360, 상세 패널 미노출, 지도 flex 확장
2. **커스텀 지역 드롭다운**:
   - 트리거 클릭 → 패널 open
   - 검색 input 에 "수원" 입력 → 해당 region 만 필터, 하이라이트 span 렌더
   - 항목 클릭 → 트리거에 pin+이름+× 표시, URL 에 `region=수원시`
   - × 클릭 → region param 제거, `/centers` 로 복귀
3. **정렬 토글**: `[프로그램많은순]` pill 클릭 → URL `sort=programs`, 첫 카드 programCount 최대
4. **운영중 토글 스위치**: 클릭 → URL `onlyActive=true`, 종료 센터 카드 미노출, 스위치 핸들 오른쪽 이동
5. **카드 클릭 → 상세 패널 open**: 첫 카드 클릭 → URL `/centers/{id}?...(filters)`, 리스트 폭 240 로 축소 (compact 카드), 상세 패널 320 노출, 정렬 pill 숨김
6. **URL 직접 접근**: 새 탭 `/centers/3?onlyActive=true` → 목록 필터 유지 + 3번 상세 패널 자동 open
7. **닫기 버튼**: 상세 패널 × 클릭 → `/centers?...(filters)` 로 이동, 리스트 폭 360 복귀

### 시각 검증 (사용자)
- 3 컬럼 정렬 · 카드 hover 상태 · 드롭다운 open 애니메이션 · 토글 스위치 slide · 리스트 폭 transition (250ms)

---

## 8. 의존성 / 선행 작업

- **완료**: F0f (Region 엔티티) — `Region.java`, `RegionRepository.java` 존재 확인됨
- **선행 필요**: 없음 (Program 카운트 소스는 이미 존재)
- **후속 (c3)**: 카카오맵 마커 클러스터링, "이 지역에서 검색" bounds 재검색, 인포윈도우, 지도↔목록 hover 동기화

---

## 9. 작업 큐 메타

| key | value |
|---|---|
| 작업 ID | F0h-c2 |
| 우선순위 | 상 (F0h 사고 후속) |
| 추정 단위 | 1 PR (Java 3 파일 + template 1 + css + test) |
| 상태 | `spec_confirmed` |
| 결정 확정 | Q1=B (인라인+route 병존), Q4=Region 엔티티 조회 |
| PR 예상 라벨 | `layout`, `centers`, `f0h-followup` |
