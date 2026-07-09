# 작업 명세: F0h-c4 — 청년센터 지도 인터랙션 고도화

> 상태: `spec_confirmed`
> 실행 순서: **F0h-c2 (좌측 리스트 필터/정렬) 머지 후 착수**. bounds 필터가 c2 필터 파이프라인과 연동됨
> 브랜치명: `feature/F0h-c4-map-interactions`
>
> **개정 (2026-07-09 두번째)**: list↔map 양방향 연동 정합·CustomEvent 채널 도입·인포윈도우 CTA client-state 화·zoom clamp 확정
>
> ## 📌 2026-07-09 c2 개정 반영
> ym-verify 결과 FAIL 4건·spec 결함 3건 확인 후 반영. 핵심 3항목:
> 1. **인포윈도우 CTA client-state 화** — 기존 `<a href="/centers/{id}">` full navigation → `<button type="button" data-info-detail>` + `centers:request-detail` CustomEvent dispatch. `centers-detail.js` 가 수신하여 `openDetail(id, pushHistory=true)` 실행. 페이지 재로드 제거. prototype.tsx L2144 `onClick={()=>{setDetailId(info.id);setSelectedId(null);}}` 재현.
> 2. **CustomEvent 채널로 모듈 decoupling** — `centers-detail.js` 와 `center-map.js` 는 서로 import 하지 않고 `document` 레벨 CustomEvent 3종 (`centers:detail-open` / `centers:detail-close` / `centers:request-detail`) 으로만 통신. 규약은 F0h-c2 spec §2-A 표 참조.
> 3. **HTMX afterSwap 리스너 스코프 축소** — `center-map.js:578~585` 의 `htmx:afterSwap` 리스너가 `.centers-container` 하위 어느 swap 에도 반응하여 map 매번 재생성됨(FAIL-3). 개정: 리스너를 리스트 스크롤 컨테이너(`data-centers-list-scroll`) swap 에만 반응하도록 필터링 + `map instance` 를 모듈 스코프에 캐싱하고 카드 리스트 재바인딩만 수행.

## 📌 2026-07-08 사고 회고 (사용자 지적)

F0h 시리즈 커밋 5개 (`5ace4f8` → `7d9a8f0`) 를 거쳤음에도 **§3-2 커스텀 인포윈도우** 스펙이 준수되지 않은 채 머지됨.

- **누락 항목**: 상단 이미지(110px)+scrim / 상태 뱃지 / 주소 / 🕒 운영시간 / 공유 버튼
- **실제 구현되었던 것**: 센터명 + 상세보기 링크 + X 닫기 (미니멀 팝오버)
- **원인**:
  1. 구현 단계에서 §3-2 표를 항목별 체크 안 함 — spec 을 "가이드" 로 취급, 세부 요소 스킵
  2. curl 마크업 grep 은 kakao SDK 로드 없이 실행되지 않는 `openInfoWindow` 결과를 못 봄
  3. Playwright e2e 프로파일은 kakao appkey 미설정 → fallback grid 만 렌더 → 마커 클릭 인터랙션 검증 skip
  4. 개인 PC 실 SDK 환경 시각 확인이 F0h 시리즈 5커밋 동안 부재
- **재발 방지**:
  - 인터랙션 spec (§3-2 표 등) 은 **구현 완료 후 표 각 행을 코드 위치와 함께 매핑 리포트** 제출. `docs/specs/{spec}.md` 에 "구현 매핑" 섹션 추가.
  - kakao SDK 필요 인터랙션은 e2e 프로파일에서도 검증 가능하도록 dev용 appkey 주입 경로 검토 (별도 티켓)
  - F0h 시리즈처럼 다중 커밋 시 **첫 커밋 직후** 개인 PC 시각 확인 게이트 통과 후 후속 커밋 진행 규칙

## 1. 디자인 출처 (3자산 모두 명시)
- **wireframe.png**: 청년센터 화면 지도 영역 (마커·인포윈도우·"이 지역에서 검색" floating 배치)
- **prototype.html**: 데모 프로토타입은 SVG 기반 시뮬레이션 지도. 실제 SDK 미포함이지만 마커·인포윈도우·"이 지역에서 검색" 시각 스펙 확인 가능 (grid 233~2160 인접 영역)
- **prototype.tsx**:
  - `CentersScreen` 함수 L1897 시작
  - 마커 렌더 블록 L2101~2116
  - "이 지역에서 검색" floating 버튼 L2117~2123
  - 카카오맵 라벨 (연동 예정 placeholder) L2124~2127
  - 커스텀 인포윈도우 L2128~2154
- **HANDOFF.md** §5.15 "청년센터 찾기" — L533~554
  - 마커 클러스터링 필수
  - "이 지역에서 검색": `idle` 이벤트 → 버튼 노출 → bounds 내 센터만
  - 지도↔목록 양방향 동기화
- **비교 대상 (현재 코드)**:
  - `src/main/resources/templates/center/list.html` (지도 panel, fallback 문구)
  - `src/main/resources/static/js/center-map.js` (기본 마커 + hover/click 동기화만 구현)
  - `src/main/resources/static/css/main.css` L3862~4000 (center-card / center-map-container / map-fallback)
  - `src/main/java/.../center/CenterController.java` (`kakaoMapAppKey` 주입)

## 1-A. 자산 간 갭 (3자산 비교)
| 항목 | wireframe.png | prototype.html | prototype.tsx | HANDOFF | 채택 |
|---|---|---|---|---|---|
| 커스텀 마커 (운영중 흰 pin / 종료 회색) | 표시 있음 | SVG 시뮬레이션 | L2107~2113 명시 | 마커 스타일 요구 | tsx 스펙 채택 |
| 선택 시 pill 라벨 확장 | 있음 | 있음 | L2108~2110 (borderRadius 14, padding 5px 12px, 센터명 label) | "선택 시 라벨 표시" | tsx 채택 |
| 인포윈도우 (이미지+뱃지+주소+운영시간+2버튼) | 있음 | 있음 | L2128~2154 상세 | 언급 | tsx 채택 |
| "이 지역에서 검색" floating | top 중앙 | top 중앙 pill | L2117~2123 (top 14, translateX -50%, primary 텍스트 + refresh 아이콘) | `idle` 이벤트 노출 | tsx + HANDOFF 결합 |
| dirty flag (지도 이동 시에만 노출) | 시맨틱만 | 데모는 상시 노출 | 주석: `데모: 상시` | `idle` 이벤트 트리거 필수 | **HANDOFF 채택** (실 구현은 idle 이벤트) |
| 마커 클러스터링 | 미표시 | 미표시 | 미구현 | ⚠️ 필수 반영 | HANDOFF: **선택 (40+ 시)** — 현 시드 규모 <20, 옵션으로 지원 |
| fallback (API 키 미설정) | 없음 | "카카오맵 API 연동 예정" 라벨 (L2126) | 동일 | 미기재 | **사용자 결정: grid SVG placeholder + 안내 텍스트** |

**모든 갭에 대해 사용자 결정 (2026-07-07) 이미 확정 → 추가 질문 없음.**

## 2. 변경 범위 (파일 단위)
- [ ] `src/main/resources/static/js/center-map.js` — 커스텀 오버레이 마커 / 인포윈도우 / dirty flag "이 지역에서 검색" / bounds 필터 / 클러스터링(옵션) 전면 개편
- [ ] `src/main/resources/templates/center/list.html` — floating 버튼 마크업 (숨김 초기 상태) + 인포윈도우 템플릿(선택) + fallback grid placeholder
- [ ] `src/main/resources/static/css/main.css` — `.center-marker`, `.center-marker--selected`, `.center-marker--closed`, `.center-marker-label`, `.center-marker-tail`, `.center-info-window`, `.centers-map-search-here`, `.center-map-fallback` (grid 패턴) 신규
- [ ] `src/main/resources/templates/center/list.html` (스크립트 로드부) — Kakao Clusterer 라이브러리 옵션 로드: `services,clusterer`
- [ ] `src/main/java/.../center/CenterListItem.java` — `imageUrl`, `hours`(운영시간 표시용) 필드 추가 검토 (현재 없으면 인포윈도우가 이미지 없이 렌더). 없으면 인포윈도우에서 이미지 영역 skip 처리 명시
- [ ] `src/main/java/.../center/Center.java` + 시드 (`DataInitializer`) — `imageUrl`, `operatingHours` 필드 없으면 신규 추가 검토 (별도 티켓 분리 가능)

## 3. 필드 / 컴포넌트 명세

### 3-1. 커스텀 마커 (`CustomOverlay` 기반)
| 상태 | 배경 | border | shape | 아이콘 | 크기 | 라벨 |
|---|---|---|---|---|---|---|
| 운영중·미선택 | `#fff` | `2.5px solid var(--color-primary)` | `border-radius: 50%` | pin (primary) | 30×30 | 없음 |
| 종료·미선택 | `#E5E7EB` | `2.5px solid var(--color-border)` | `border-radius: 50%` | pin (textTri) | 30×30 | 없음 |
| 선택 (운영중/종료 공통) | `var(--color-primary)` | `2.5px solid var(--color-primary)` | `border-radius: 14px` (pill) | pin (white) | auto height 30, padding `5px 12px` | 센터명 흰색 12px/700 |
| tail (마커 아래 꼬리) | 선택 시 primary / 미선택 시 border | width 2, height 8 | | | | |
| shadow (선택) | `drop-shadow(0 4px 8px rgba(63,48,233,0.45))` | | | | | |
| shadow (미선택) | `drop-shadow(0 2px 4px rgba(0,0,0,0.2))` | | | | | |
| zIndex | 선택 20, 미선택 10 | | | | | |
| transition | `all 200ms ease` | | | | | |

**구현**: `kakao.maps.CustomOverlay` + HTML DIV 로 렌더 (마커 이미지 파일 방식 불가 — 동적 pill 확장 필요).

### 3-2. 커스텀 인포윈도우 (마커 클릭 시)
| 요소 | 스펙 |
|---|---|
| 컨테이너 | width 300, border-radius 14, box-shadow `0 8px 32px rgba(0,0,0,0.18)`, background surface, overflow hidden |
| 위치 | 마커 위 (`bottom` 안 겹치도록 offset 계산). 화면 벗어남 방지: 지도 오른쪽 끝일 때 left 자동 clamp |
| 이미지 영역 | width 100%, height 110, `object-fit: cover`. imageUrl null → skip 또는 placeholder gradient |
| 오버레이 그라디언트 | `linear-gradient(transparent 40%, rgba(0,0,0,0.4))` |
| 닫기 버튼 (X) | top 8, right 8, 24×24 원형, `rgba(0,0,0,0.3)` |
| 상태 뱃지 | bottom 8, left 10, padding `2px 8px`, radius pill, 운영중=success / 종료=`#9CA3AF`, 흰색 텍스트 10px/600 |
| 본문 | padding `12px 14px` |
| 센터명 | 15px/700 text |
| 주소 | 12px textSec, line-height 1.5, marginBottom 8 |
| 운영시간 | 11px textTri, prefix `🕒 `, marginBottom 10 |
| 하단 버튼 | flex gap 8. [상세보기] flex 1, height 32, primary bg, radius 7, 12px/600 white. [공유] 32×32 정사각, border, radius 7, share 아이콘 |

**구현**: `kakao.maps.CustomOverlay` HTML 컨테이너. 표준 `InfoWindow` 는 스타일 커스텀 한계 있어 사용 안 함.

### 3-3. "이 지역에서 검색" floating 버튼
| 항목 | 스펙 |
|---|---|
| 위치 | 지도 panel 내부 `position: absolute`, `top: 14px`, `left: 50%`, `transform: translateX(-50%)`, `z-index: 25` |
| 크기 | padding `8px 16px`, border-radius pill (20) |
| 배경 | `var(--color-surface)`, box-shadow `0 3px 12px rgba(0,0,0,0.16)` |
| 콘텐츠 | refresh 아이콘 14 (primary) + "이 지역에서 검색" 13px/600 primary |
| 초기 상태 | 숨김 (`display: none` 또는 `.is-hidden`) |
| 노출 조건 | Kakao map `idle` 이벤트 (드래그/줌 종료) 발생 시. 단, **최초 렌더 직후 `setBounds` 로 인해 발생한 idle 은 무시** (dirty flag 초기 false) |
| 클릭 동작 | 현재 지도 `map.getBounds()` 조회 → 좌표 있는 카드 중 `bounds.contain(pos)` 통과분만 표시 (`.center-card` 에 `.is-out-of-bounds` 토글 → `display: none`) → 필터 후 버튼 숨김 |
| 리스트 카운트 연동 | c2 에서 도입될 "N개" 카운트 요소 있으면 필터 적용 후 재계산 (셀렉터: `.centers-list-count strong` — 실제 구현 기준 정정, list.html:96) |

### 3-4. 지도 ↔ 리스트 양방향 하이라이트 (기존 + 확장)
| 이벤트 | 동작 |
|---|---|
| 카드 mouseenter | (기존) 해당 마커 zIndex 999 + card `.is-hover` |
| 카드 mouseleave | (기존) zIndex 원복 + `.is-hover` 제거 |
| 마커 click | (기존) `highlightCard()` — 다른 카드 `.is-highlighted` 제거, 해당 카드 추가 + scrollIntoView |
| 마커 click (신규) | `selectedId` 상태 갱신 → 해당 마커 pill 라벨 확장 + 인포윈도우 오픈. 다른 마커는 원형으로 복귀 |
| 인포윈도우 닫기 (X) | `selectedId = null` → 마커 원형 복귀 + 인포윈도우 제거 |
| **인포윈도우 [상세보기] CTA (2026-07-09 개정)** | `<button type="button" data-info-detail data-center-id="{id}">` — 클릭 시 `document.dispatchEvent(new CustomEvent('centers:request-detail', { detail: { centerId: id } }))`. `centers-detail.js` 가 수신해 `openDetail(id, pushHistory=true)` 실행 → 인라인 상세 패널 open. **기존 `<a href="/centers/{id}">` full navigation 라인 삭제** |
| 지도 빈 곳 클릭 | `selectedId = null` (이벤트 `click` on map) |

### 3-4-A. list ↔ map 양방향 연동 (2026-07-09 개정 신설)

`centers-detail.js` (list 측) 와 `center-map.js` (map 측) 는 CustomEvent 로만 결합 (직접 참조 없음). 규약은 F0h-c2 §2-A 표와 동일.

| 방향 | 트리거 | 채널 | map 측 처리 |
|---|---|---|---|
| **list → map** | 카드 클릭 → `openDetail(id)` 완료 후 `centers:detail-open` dispatch | `document` CustomEvent | `selectMarker(centerId)` — 해당 마커 pill 확장 + 인포윈도우 open + zIndex 20 + **`map.panTo(new kakao.maps.LatLng(target.lat, target.lng))`** (마커가 뷰포트 밖일 때 selected 상태 시각화 보장, naver/kakao/google 지도 관행 준수). 카드 scrollIntoView 는 `highlightCard()` 내부에서 수행. **map 자체 재초기화 없음** (기존 map instance 재사용) |
| **list → map** | 상세 close (X 또는 popstate) → `centers:detail-close` dispatch | 동일 | `clearSelection()` — 마커 원형 복귀 + 인포윈도우 제거 |
| **map → list** | 마커 click | 내부 — map 자체 처리 | `highlightCard(id)` + `card.scrollIntoView({block:'nearest', behavior:'smooth'})`. 인포윈도우 open (기존 유지) |
| **map → list** | 인포윈도우 CTA 클릭 | `centers:request-detail` dispatch | `centers-detail.js` 가 수신해 `openDetail(id, pushHistory=true)`. map 측은 dispatch 후 관여하지 않음 (loopback 은 detail-open 수신 시 selectMarker 로 idempotent 처리) |

### 3-4-B. HTMX afterSwap 리스너 스코프 (FAIL-3 개정)

**문제**: `center-map.js:578~585` 의 `document.addEventListener('htmx:afterSwap', ...)` 가 `.centers-container` 하위 어느 swap 에도 반응해 map 을 매번 `new kakao.maps.Map()` 로 재초기화. 카드 리스트만 갱신되어도 map 재생성 → 상태(zoom/center/선택마커) 리셋 + memory leak.

**개정**:
1. `htmx:afterSwap` 리스너에서 `event.detail.target` 검사 → `target.matches('[data-centers-list-scroll]')` 이거나 그 하위 요소일 때만 후속 처리
2. `map` 인스턴스는 모듈 스코프 변수(`let mapInstance = null`)에 캐싱. afterSwap 시 이미 존재하면 재사용, 카드 리스트 재바인딩(`overlays` 배열 갱신 + `data-center-id` 기반 card 매핑)만 수행
3. Kakao SDK re-init 은 최초 1회 (`initializeMap()`) 에서만. afterSwap 은 절대 `new kakao.maps.Map()` 을 호출하지 않음
4. 회귀 방어: `mapInstance` 존재 여부 assertion 을 §5 시각 검증 N#4 로 추가

### 3-5. Fallback (KAKAO_MAP_APP_KEY 미설정)
| 항목 | 스펙 |
|---|---|
| 컨테이너 | `.center-map-fallback` — `.center-map-container` 와 동일 크기 |
| 배경 | grid SVG 패턴. CSS `background-image` 로 구현: `linear-gradient(to right, var(--color-border-light) 1px, transparent 1px), linear-gradient(to bottom, var(--color-border-light) 1px, transparent 1px)`, `background-size: 40px 40px`, `background-color: var(--color-bg)` |
| 중앙 안내 | 세로 flex center. 아이콘 (map pin, primary, 48px) + "지도 미설정" 14px/600 text + "좌측 목록에서 청년센터를 확인하실 수 있어요" 12px textSec |
| 기존 문구 대체 | 현재 "지도를 불러올 수 없습니다 / KAKAO_MAP_APP_KEY 환경변수..." 문구 삭제. 개발 힌트는 HTML 주석(파서 주석 `<!--/* */-->`) 으로 이동 |

### 3-6. 클러스터링 (옵션, 40+ 센터 대응)
| 항목 | 스펙 |
|---|---|
| 라이브러리 | Kakao SDK `libraries=clusterer` 추가 로드 |
| 활성화 조건 | `validCards.length >= 20` 일 때만 clusterer 사용. 미만이면 개별 마커 렌더 |
| 클러스터 스타일 | 기본 스타일 유지 (F0h-c4 범위에는 커스텀 스킨 제외). 확장은 별도 티켓 |
| 마커-클러스터 연동 | 클러스터 클릭 시 zoom-in. `disableClickZoom: false` |

## 4. 갭 리스트 (현재 코드 vs prototype)
| # | 항목 | 현재 상태 | prototype 명세 | 우선순위 |
|---|---|---|---|---|
| 1 | 마커 스타일 | Kakao 기본 마커 (빨간 pin) | 커스텀 pill/원형 오버레이 (운영중/종료/선택) | 높음 |
| 2 | 선택 상태 라벨 | 없음 | pill 확장 + 센터명 표시 | 높음 |
| 3 | 인포윈도우 | 상세 페이지만 (센터명 단일 텍스트) | 리스트 마커 클릭 시 이미지+뱃지+주소+운영시간+2버튼 | 높음 |
| 4 | "이 지역에서 검색" 버튼 | 없음 | idle 이벤트 기반 dirty flag + bounds 필터 | 높음 |
| 5 | bounds 필터 → 리스트 갱신 | 없음 | 카드 `.is-out-of-bounds` 토글 | 높음 |
| 6 | fallback | 텍스트만 | grid SVG placeholder + 아이콘 + 안내 텍스트 | 중 |
| 7 | 클러스터링 | 없음 | 20+ 시 활성 (옵션) | 중 |
| 8 | 인포윈도우 이미지 | 도메인에 imageUrl 필드 없음 | 필요 (없으면 skip 처리 명시) | 확인 필요 (별도 티켓 가능) |
| 9 | 인포윈도우 운영시간 | Center 엔티티에 operatingHours 필드 확인 필요 | 필요 | 확인 필요 |
| 10 | 마커 tail (꼬리) | 없음 | width 2 / height 8 아래 방향 stick | 중 |

## 5. 검증 시나리오 (ym-qa 가 실행할 항목)

### 정적 검증
- `./gradlew compileJava` 통과
- `./gradlew test` — 기존 CenterController / CenterListItem 관련 테스트 유지 통과
- JS syntax lint (수동 확인: 문자열 큰따옴표/이스케이프)

### 동적 검증 (bootRun + curl)
- `GET /centers` → 200 OK
- `curl /centers | grep centers-map-search-here` → 마크업 렌더 확인
- `curl /centers | grep center-map-fallback` → **KAKAO_MAP_APP_KEY 미설정 시** fallback DIV 렌더 확인
- `curl /js/center-map.js` → 200 OK
- Kakao SDK 스크립트 태그 확인 (`grep dapi.kakao.com`) — appkey 있을 때만 로드
- 정적 리소스 (`/css/main.css`, `/js/center-map.js`) 200 OK

### 시각 검증 (사용자 영역, KAKAO_MAP_APP_KEY 설정 후)
1. `/centers` 진입 시 커스텀 마커 (운영중 흰 pin / 종료 회색) 렌더
2. 마커 클릭 → pill 확장 + 센터명 라벨 + 커스텀 인포윈도우 오픈
3. **인포윈도우 [상세보기] CTA → 인라인 상세 패널 open (2026-07-09 개정)**: 페이지 전체 재로드 없음. Network 탭에 document navigation 요청 부재. fragment 2건만 발생 (`/centers/{id}/detail-fragment` + `/centers/cards?compact=true&...`). `centers:request-detail` → `openDetail` → `centers:detail-open` loopback 로 마커 selected 상태 유지
4. 인포윈도우 X → 마커 원형 복귀
5. 지도 드래그/줌 → "이 지역에서 검색" 상단 등장 → 클릭 시 좌측 리스트가 bounds 내 센터로 필터링 + 버튼 숨김
6. 카드 hover → 해당 마커 zIndex 상승 (기존)
7. 마커 클릭 → 좌측 카드 스크롤 + is-highlighted (기존)
8. **KAKAO_MAP_APP_KEY 미설정 환경**: grid SVG placeholder + "지도 미설정" 안내 표시 (텍스트 문구 대체 확인)
9. (선택) 시드 데이터에 센터 20+ 넣고 클러스터링 활성화 확인

### 양방향 연동 시나리오 (2026-07-09 개정 신설)
- **N#1** 카드 클릭 → 지도 마커 pill 확장 (시각 확인): `centers:detail-open` 수신 → `selectMarker(id)` 호출. 다른 마커는 원형 유지
- **N#2** 인포윈도우 CTA 클릭 → detail 인라인 open, **페이지 전체 재로드 없음** (network 계측: document navigation 요청 부재)
- **N#3** 상세 close (X 또는 popstate) → 마커 원형 복귀 (`centers:detail-close` 수신 처리)
- **N#4** 지도 재초기화 회귀 방어 — `htmx:afterSwap` 시마다 `new kakao.maps.Map()` 이 반복 호출되지 않음. DevTools memory heap 스냅샷 또는 JS 유닛 테스트로 `mapInstance` 참조가 동일한지 확인. 초기 1회 이후 map 생성 로그 없음

### 회귀 검증
- `/centers/{id}` 200 OK + 인라인 detail 패널 + compact 카드 렌더 확인 (F0h-c2 에서 별도 detail 라우트가 제거되고 동일 list.html 을 사용 — 구 "단일 마커 + 기본 InfoWindow 유지" 항목은 stale 하여 교체, 2026-07-08)
- 좌표 없는 센터 (lat/lng null) 는 리스트만 노출되고 마커 skip 유지
- F0h-c2 필터/정렬 결과와 bounds 필터 충돌 없이 결합 (AND 조건)

## 6. 의존성 / 선행 작업
- **F0h-c2** (좌측 리스트 필터·정렬·독립 스크롤) 머지 후 착수 — bounds 필터는 c2 필터 파이프라인에 이어붙는 방식이라 c2 셀렉터·카운트 요소 확정 필요
- **선행 확인 필요 (별도 티켓 분리 가능)**:
  - `Center.imageUrl` 필드 유무 → 없으면 인포윈도우 이미지 영역 skip 처리 (본 티켓 내 처리) 또는 F0h-c5 (엔티티 확장) 신설
  - `Center.operatingHours` 필드 유무 → 없으면 인포윈도우 "🕒 운영시간" 라인 skip

## 7. 작업 큐 메타
- 작업 ID: `F0h-c4`
- 상위: F0h (청년센터 지도 인터랙션 고도화)
- 우선순위: 중 (F0h-c2 이후)
- 추정 단위: 1 PR (JS 대폭 개편 + CSS + template 미세 수정)
- 상태: `spec_confirmed`
- 브랜치명: `feature/F0h-c4-map-interactions`

---

## 참고: Kakao Maps API 사용 API

| API | 용도 |
|---|---|
| `kakao.maps.CustomOverlay` | 커스텀 마커·인포윈도우 (HTML DIV) |
| `kakao.maps.event.addListener(map, 'idle', ...)` | 드래그/줌 종료 시 dirty flag 세팅 |
| `kakao.maps.event.addListener(map, 'click', ...)` | 지도 빈 곳 클릭 시 selection 해제 |
| `map.getBounds()` | 현재 뷰포트 bounds 조회 |
| `bounds.contain(latlng)` | 마커 좌표가 bounds 내인지 판정 |
| `map.relayout()` + `map.setBounds()` | 초기 렌더 회색 지도 방지 (기존 유지) |
| `kakao.maps.MarkerClusterer` | 클러스터링 (libraries=clusterer 로드 필요) |

SDK 로드 URL 확장 (list.html):
```html
<script th:if="${!#strings.isEmpty(kakaoMapAppKey)}"
        th:src="'https://dapi.kakao.com/v2/maps/sdk.js?appkey=' + ${kakaoMapAppKey} + '&autoload=false&libraries=clusterer'"></script>
```

`&libraries=clusterer` 추가 시 미사용 페이지에도 로드되지만 용량 미미 (약 20KB). 조건부 로드 복잡도 대비 이득 낮음 → 상시 로드로 단순화.

---

## 구현 매핑 (2026-07-08)

> §3-1~3-6 표 각 행 → 코드 위치. 상태: ✅ 구현 완료 / ⚠️ 구현했으나 실 SDK 렌더 필요 (회사 PC 판정 불가 — SDK 환경 확인 대기) / ❌ 미구현 (사유 명시).
> 파일 약칭: `js` = `src/main/resources/static/js/center-map.js`, `css` = `src/main/resources/static/css/main.css`, `html` = `src/main/resources/templates/center/list.html`, `DI` = `src/main/java/io/github/sihyuuun/youthmoa/common/DataInitializer.java`

### §3-1 커스텀 마커
| spec 행 | 상태 | 코드 위치 |
|---|---|---|
| 운영중·미선택 (흰 bg, primary border 2.5, 원형, pin primary, 30×30) | ✅ | css:4378~4397 `.center-marker__body` + js:547~558 `buildMarkerElement()` (pin SVG js:534~539) |
| 종료·미선택 (#E5E7EB, border 색, pin textTri) | ✅ | css:4427~4431 `.is-inactive:not(.is-selected)` |
| 선택 (primary bg, pill radius 14, 센터명 흰 12px/700) | ⚠️ | css:4433~4449. padding 세로값은 `height:30px` 고정 + 좌우 12px 로 구현 (spec `5px 12px` 와 시각 동등 — SDK 환경 확인 대기) |
| tail (선택 primary / 미선택 border, 2×8) | ✅ | css:4416~4426 기본 `var(--color-border)` + css:4441~4443 선택 시 primary (2026-07-08 수정: 종전 기본값 primary 였음) |
| shadow 선택/미선택 (drop-shadow) | ✅ | css:4366~4377 `.center-marker` / `.is-selected` filter |
| zIndex 선택 20 / 미선택 10 | ✅ | js:349~356 `selectMarker()` 의 `overlay.setZIndex()` (2026-07-08 수정: 종전 콘텐츠 div `el.style.zIndex` 방식은 오버레이 wrapper stacking context 에 갇혀 무효 가능 → SDK 메서드 호출로 교체. hover 999 는 js:293~302) |
| transition 200ms | ✅ | css:4372 (filter) + css:4394 (body width/bg/radius/padding) |

### §3-2 커스텀 인포윈도우
| spec 행 | 상태 | 코드 위치 |
|---|---|---|
| 컨테이너 (300px, radius 14, shadow, surface, overflow hidden) | ✅ | css:4452~4460 `.center-info-window` |
| 위치 (마커 위 offset + 화면 벗어남 방지) | ⚠️ | js:472~478 `yAnchor:1.4` (마커 위) + js:480~499 오픈 직후 지도 좌우 경계 초과분을 콘텐츠 div `translateX` 로 보정 (anchor 는 불변). 픽셀 계산은 실 SDK 렌더 필요 — SDK 환경 확인 대기 |
| 이미지 영역 (110px, cover, null → placeholder gradient) | ✅ | js:391~394 + css:4461~4475 |
| 오버레이 그라디언트 | ✅ | js:401 + css:4476~4481 `.center-info-window-scrim` |
| 닫기 버튼 (24×24 원형, rgba 0.3) | ✅ | js:402, 425~428 + css:4482~4498 |
| 상태 뱃지 (운영중 success / 종료 #9CA3AF) | ✅ | js:395~396, 403 + css:4499~4514 |
| 본문 padding 12 14 | ✅ | css:4515~4517 |
| 센터명 15/700 | ✅ | js:406 + css:4518~4523 |
| 주소 12 textSec | ✅ | js:407~409 + css:4524~4532 |
| 운영시간 🕒 11 textTri | ✅ | js:410~412 + css:4533~4537 |
| 하단 버튼 ([상세보기] primary + [공유] 32×32) | ✅ | js:413~422 (마크업) + js:429~471 (공유: 모바일 navigator.share / 데스크톱 클립보드 복사 + toast) + css:4538~4579 |

### §3-3 "이 지역에서 검색" floating 버튼
| spec 행 | 상태 | 코드 위치 |
|---|---|---|
| 위치 (top 14, 중앙, z-index 25) | ✅ | css:4293~4312 |
| 크기·배경 (pill 20, surface, shadow) | ✅ | css:4299~4303 |
| 콘텐츠 (refresh 아이콘 14 primary + 13px/600 텍스트) | ✅ | html:199~208 (2026-07-08 수정: ↻ 텍스트 문자 → currentColor 인라인 SVG 14px) + css:4316~4326 |
| 초기 숨김 | ✅ | html:199 `hidden` + css:4313~4315 |
| 노출 조건 (idle + 초기 setBounds idle 무시) | ✅ | js:504~510 (dirtySuppressed 500ms) |
| 클릭 → bounds 필터 (`.is-out-of-bounds` 토글) | ✅ | js:512~528 + css:4655~4657 |
| 리스트 카운트 연동 | ✅ | js:520~526 — 셀렉터 `.centers-list-count strong` (html:96). spec 본문 §3-3 표기도 동일하게 정정함 |

### §3-4 지도 ↔ 리스트 양방향 하이라이트
| spec 행 | 상태 | 코드 위치 |
|---|---|---|
| 카드 mouseenter/leave → 마커 zIndex 999/원복 | ✅ | js:293~302 (`overlay.setZIndex()` 로 교체) |
| 마커 click → 카드 `.is-highlighted` + scrollIntoView | ✅ | js:349~373 `selectMarker()` → `highlightCard()` (2026-07-08 신규 — 종전 미구현이었음). 다른 카드 하이라이트 제거 + `scrollIntoView({block:'nearest', behavior:'smooth'})`. 카드 매칭은 overlays[].card (data-center-id 기반 수집, js:255) |
| **list → map 카드 클릭 시 map.panTo (2026-07-09 개정)** | ✅ | js:377~379 `selectMarker()` 내 `openInfoWindow()` 직전에 `map.panTo(new kakao.maps.LatLng(target.lat, target.lng))` 호출. `target` null 방어는 기존 `if (target)` 블록에 포함됨. 사유: prototype 은 시뮬 정적 지도라 panning 미정의이나, 실 SDK 환경에서 마커가 뷰포트 밖일 때 selected 상태 시각화를 위해 필수 |
| 마커 click → pill 확장 + 인포윈도우 | ✅ | js:349~360 |
| 인포윈도우 X → 선택 해제 | ✅ | js:425~428 → js:375~383 `clearSelection()` (`.is-highlighted` 도 함께 제거) |
| 인포윈도우 [상세보기] → /centers/{id} | ✅ | js:414 |
| 지도 빈 곳 클릭 → 선택 해제 | ✅ | js:502 |

### §3-5 Fallback (KAKAO_MAP_APP_KEY 미설정)
| spec 행 | 상태 | 코드 위치 |
|---|---|---|
| 컨테이너 (지도와 동일 크기) | ✅ | css:4619~4635 `position:absolute; inset:0` |
| grid 배경 (40px linear-gradient) | ✅ | css:4626~4631 |
| 중앙 안내 (pin 48 primary + 타이틀 + 서브) | ✅ | html:221~232 (2026-07-08 수정: 📍 이모지 → currentColor 인라인 SVG 48px) + css:4636~4654 |
| 개발 힌트 주석 → 파서 주석 | ✅ | html:11~12, 221 `<!--/* ... */-->` (2026-07-08 수정: 응답 HTML 미노출) |

### §3-6 클러스터링 (활성 조건: 센터 ≥20 — 시드 48개로 충족)
| spec 행 | 상태 | 코드 위치 |
|---|---|---|
| `libraries=clusterer` 로드 | ✅ | html:13~14 |
| `validCards.length >= 20` 일 때만 활성 | ⚠️ | js:304~327 `registerOverlays()` — CLUSTER_THRESHOLD 20 |
| 기본 스타일 유지 / `disableClickZoom: false` | ⚠️ | js:313~317 (`averageCenter: true` 만 추가 지정) |
| CustomOverlay 를 clusterer 에 add | ⚠️ | js:318. **공식 문서 (apis.map.kakao.com) 의 `addMarkers` 파라미터 타입은 `Array<Marker>` 전용으로 명시** — CustomOverlay 는 getPosition/setMap 인터페이스 호환(duck-typing)으로 동작한다는 커뮤니티 보고가 다수이나 공식 보장 없음. try/catch 로 감싸 실패 시 개별 `overlay.setMap(map)` fallback (js:319~326). **실 SDK 환경 확인 대기 — 미동작(마커 미표시/클러스터 미형성) 시 사용자 결정 필요** (개별 마커 유지 vs 기본 Marker 로 클러스터 전환) |

### 회사 PC 판정 불가 항목 요약 (SDK 환경 확인 대기)
- §3-1 선택 pill 확장의 시각 치수, §3-2 인포윈도우 위치 보정, §3-6 클러스터러 CustomOverlay 수용 여부
- e2e 프로파일은 appkey 미설정 → fallback 렌더만 검증됨. 개인 PC (실 appkey) 시각 확인 게이트 필요

## spec 추록 (spec 미기재 구현 사항, 2026-07-08 기록)

본 spec 산출 후 구현 단계에서 추가된 사항. spec 대비 확장이므로 명시적으로 기록한다.

1. **"내 위치" 버튼 (geolocation)**: html:211~218, js:198~240, css:4328~4357. 지도 좌하단 40×40 원형 버튼 → 브라우저 geolocation permission → 성공 시 해당 위치로 이동(level 6) + 파란 점 CustomOverlay 표식. HTTPS 필수 (localhost 예외), 미지원 브라우저는 버튼 숨김.
2. **전역 toast 헬퍼 (`window.showToast`)**: js:15~37, css:4586~4616. 인포윈도우 공유 버튼의 복사 완료/실패 피드백용으로 도입 — 우하단 고정 스택, 자동 페이드아웃. 다른 화면에서 재사용 가능한 프로젝트 공통 유틸.
3. **zoom clamp 레벨 (MIN/MAX_LEVEL 3/7) — 확정 (2026-07-09 사용자 결정)**: js:331~345 `fitAndClamp()`. **MAX_LEVEL=7 유지**. 사유: 확대 시야 우선. 초기 뷰포트에서 경기 남·북 끝 마커가 이탈하는 UX 는 수용된 결정 — 사용자가 zoom-out 으로 전체 조망 가능. 종전 "사용자 확인 필요" flag 는 제거. ym-verify 시나리오에서 "초기 뷰포트 이탈" 은 UNVERIFIED 제외 (수용 결정).

## 회고 / 재발방지 (2026-07-09 두번째 개정 신설)

1. **spec 미결정 flag 를 남긴 채 `spec_confirmed` 로 두지 않는다** — 확정 필요 항목(예: zoom clamp MAX_LEVEL) 은 "사용자 결정 대기 리스트" 로 분리해 별도 큐잉. spec 본문에 결론 없이 남기면 후속 verify 단계에서 반복 지적 발생.
2. **모듈 간 결합은 CustomEvent 로 decouple** — 두 JS 모듈이 서로 import 하지 않고 이벤트로 통신하도록 규약을 spec §2-A / §3-4-A 표에 명시. 이후 유사 패턴(list ↔ map ↔ detail) 신설 시 동일 규약 재사용.
3. **HTMX afterSwap 리스너는 반드시 target 필터링** — `document` 레벨 listener 는 하위 어느 swap 에도 반응하므로 `event.detail.target.matches(...)` 로 스코프를 좁힌다. Map/Chart 등 heavy instance 는 모듈 스코프에 캐싱해 재초기화를 방지.
