# 작업 명세: F0h-c4 — 청년센터 지도 인터랙션 고도화

> 상태: `spec_confirmed`
> 실행 순서: **F0h-c2 (좌측 리스트 필터/정렬) 머지 후 착수**. bounds 필터가 c2 필터 파이프라인과 연동됨
> 브랜치명: `feature/F0h-c4-map-interactions`

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
| 리스트 카운트 연동 | c2 에서 도입될 "N개" 카운트 요소 있으면 필터 적용 후 재계산 (셀렉터: `.centers-count` — c2 스펙과 정렬) |

### 3-4. 지도 ↔ 리스트 양방향 하이라이트 (기존 + 확장)
| 이벤트 | 동작 |
|---|---|
| 카드 mouseenter | (기존) 해당 마커 zIndex 999 + card `.is-hover` |
| 카드 mouseleave | (기존) zIndex 원복 + `.is-hover` 제거 |
| 마커 click | (기존) `highlightCard()` — 다른 카드 `.is-highlighted` 제거, 해당 카드 추가 + scrollIntoView |
| **마커 click (신규)** | `selectedId` 상태 갱신 → 해당 마커 pill 라벨 확장 + 인포윈도우 오픈. 다른 마커는 원형으로 복귀 |
| 인포윈도우 닫기 (X) | `selectedId = null` → 마커 원형 복귀 + 인포윈도우 제거 |
| 인포윈도우 [상세보기] | `/centers/{id}` 이동 |
| 지도 빈 곳 클릭 | `selectedId = null` (이벤트 `click` on map) |

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
3. 인포윈도우 [상세보기] → `/centers/{id}` 이동
4. 인포윈도우 X → 마커 원형 복귀
5. 지도 드래그/줌 → "이 지역에서 검색" 상단 등장 → 클릭 시 좌측 리스트가 bounds 내 센터로 필터링 + 버튼 숨김
6. 카드 hover → 해당 마커 zIndex 상승 (기존)
7. 마커 클릭 → 좌측 카드 스크롤 + is-highlighted (기존)
8. **KAKAO_MAP_APP_KEY 미설정 환경**: grid SVG placeholder + "지도 미설정" 안내 표시 (텍스트 문구 대체 확인)
9. (선택) 시드 데이터에 센터 20+ 넣고 클러스터링 활성화 확인

### 회귀 검증
- `/centers/{id}` 상세 페이지: 단일 마커 + 기본 InfoWindow (센터명) 유지 (본 티켓 스코프 외 — 회귀 없어야 함)
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
