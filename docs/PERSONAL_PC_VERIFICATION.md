# 개인 PC 시각 검증 항목 (Personal PC Verification Backlog)

> **용도**: 회사 PC 에서 실 kakao SDK / Playwright / Testcontainers 등을 실행 불가한 항목을 개인 PC (Mac + Docker) 세션에서 검증할 리스트.
> 각 항목은 검증 후 이 문서에 결과 기록 → 통과 시 STATE.md 큐에서 제거.

**마지막 갱신**: 2026-07-10

---

## 1. F0h ym-verify UNVERIFIED 4건 (PR #78 산출 시점)

### 1-1. Kakao MarkerClusterer CustomOverlay 수용 여부
- **위치**: `src/main/resources/static/js/center-map.js:329~342` `registerOverlays()` 클러스터 등록 로직
- **배경**: `MarkerClusterer.addMarkers()` 공식 타입은 `Marker` 배열이나 우리는 `CustomOverlay` 배열 전달 (duck-typing, 공식 미보장). try/catch fallback 있음
- **검증 방법**:
  - 개인 PC 에서 `KAKAO_MAP_APP_KEY` 환경변수 설정 후 `./gradlew bootRun` 기동
  - `/centers` 접속 → 지도에 마커 48개가 지역별로 클러스터 형성되는지 확인
  - 브라우저 DevTools 콘솔에서 에러/경고 로그 확인
  - 특히 "CustomOverlay 미지원" 로 인한 fallback 로그가 뜨는지
- **PASS 기준**: 클러스터 정상 형성 + 콘솔 에러 없음
- **FAIL 시 조치**: `_clusterer = null` 로 강제 (개별 렌더 fallback). 성능 저하 없이 동작

### 1-2. 인포윈도우 좌·우 경계 300px 보정
- **위치**: `static/js/center-map.js:544~575` `requestAnimationFrame` 안의 좌우 보정 로직
- **배경**: 인포윈도우(300px) 가 지도 좌·우 끝 마커에서 열릴 때 뷰포트 밖으로 삐져나오는 것을 CSS `translateX` 로 보정. 실 SDK 픽셀 계산 필요
- **검증 방법**:
  - 지도를 지역 최서단(김포·인천 방향) 마커·최동단(연천·포천 방향) 마커로 스크롤
  - 각 마커 클릭 → 인포윈도우가 뷰포트 안에 완전히 들어오는지
- **PASS 기준**: 인포윈도우가 지도 좌·우 경계 안쪽에 렌더 (짤림 없음)
- **FAIL 시 조치**: `mapRect.width` 계산 로직 재검토. 컨테이너 실 offsetWidth 확인

### 1-3. `overlay.setZIndex()` hover 시 wrapper stacking
- **위치**: `static/js/center-map.js:313~320` mouseenter/mouseleave 핸들러 (`overlay.setZIndex(999)`)
- **배경**: kakao CustomOverlay wrapper 의 z-index 제어. CSS z-index 는 stacking context 에 갇힐 수 있어 API 사용
- **검증 방법**:
  - 마커가 겹친 지역 (예: 화성 2건 같은 건물) 에서 카드 hover
  - hover 마커가 다른 마커 위로 부상하는지 시각 확인
- **PASS 기준**: hover 마커가 최상단 표시
- **FAIL 시 조치**: 부상 안 되면 wrapper element 에 CSS class 추가 시도

### 1-4. Zoom MAX_LEVEL=7 초기 뷰포트 이탈 위험 → **유지 결정 (사용자)**
- **위치**: `static/js/center-map.js:354` `MAX_LEVEL = 7`
- **배경**: 전체 마커 fitBounds 후 강제 setLevel(7). 경기 남·북 끝 (연천·평택 등) 일부 마커가 초기 뷰포트 밖에 놓일 수 있음
- **결정**: **MAX_LEVEL=7 유지** (사용자 확대 시야 우선). UX 로 수용
- **검증 방법**: `/centers` 초기 진입 후 뷰포트 밖 마커 개수 확인 (참고용)
- **PASS 기준**: N/A (결정 완료 항목 — 참고 검증만)

---

## 2. F0h 실 지도 인터랙션 최종 시각 검증

### 2-1. 카드 클릭 → panTo + setLevel(4) + 인포윈도우 중앙 정렬
- **PR 참고**: #78 세션 마지막 배치
- **검증**:
  - 리스트 카드 클릭 시 지도가 마커 위치로 부드럽게 이동 + 동 단위로 확대
  - 인포윈도우가 뷰포트 세로 중앙에 위치 (yAnchor 1.4 기준 0.9*infoH offset 적용)
- **PASS 기준**: 3가지 동작 모두 자연스러움. 인포윈도우 짤림 없음

### 2-2. 필터 partial swap (지도 리로드 없음)
- **검증**:
  - "운영중만 보기" 토글, 지역 드롭다운, 정렬 pill, 검색어 변경
  - 각 조작 시 지도가 재로드 안 되고 (깜빡임 없음) 마커만 필터링됨
- **PASS 기준**: 지도 인스턴스 유지 + 마커 표시/숨김만 반응 + URL 반영

### 2-3. 상세 X 닫기 시 지도 상태 유지
- **검증**: 상세 패널 X 클릭 시 detail 만 닫히고 마커 selected · 인포윈도우는 유지
- **PASS 기준**: 지도 상태 무변화. 인포윈도우 자체 X 로만 닫힘

### 2-4. 브라우저 뒤로가기 (popstate)
- **검증**: 카드 클릭 → 뒤로가기 → 상세 close + URL 복귀 (지도 상태 유지)
- **PASS 기준**: popstate 정상 트리거

---

## 3. Testcontainers 로컬 E2E

### 3-1. `YouthMoaApplicationTests.contextLoads()`
- **위치**: `src/test/java/io/github/sihyuuun/youthmoa/YouthMoaApplicationTests.java`
- **배경**: 회사 PC 는 Docker 데몬 연결 불가 → 실행 skip
- **검증 방법 (Mac)**:
  ```bash
  ./gradlew test --tests YouthMoaApplicationTests
  ```
- **PASS 기준**: Testcontainers PostgreSQL 컨테이너 정상 기동 + 컨텍스트 로드 성공

### 3-2. 다른 통합 테스트 (있으면)
- 개인 PC 세션에서 전체 테스트 스위트 실행:
  ```bash
  ./gradlew test
  ```

---

## 4. Playwright E2E 커버리지

### 4-1. `/centers` E2E spec 신설
- **선행**: F0h-c2/c4 stable (이번 세션 완료)
- **시나리오**:
  - 카드 클릭 → 상세 open + 지도 panTo + 인포윈도우 중앙
  - 필터 조작 → partial swap
  - X 닫기 → detail close + 지도 유지
  - 뒤로가기 → popstate
- **위치**: `e2e/tests/centers.spec.ts` (신규 예정)

### 4-2. 기존 backlog
| 시나리오 | 선행 조건 |
|---|---|
| `/apply/complete` 신청 완료 화면 | D1b 머지 완료 (7/2) |
| 공지사항 목록·상세 | F0g 머지 완료 (7/3) |
| 청년센터 + 카카오맵 | F0h stable (7/10) — 위 4-1 |
| 마이페이지 (신청 내역/즐겨찾기/개인정보) | D5-mypage 머지 완료 (7/6) |
| 아이디·비밀번호 찾기 | F0i 머지 완료 (7/6) |
| 검색바 + 결과 | D4-search 머지 완료 (7/6) |

---

## 5. 결과 기록 방식

각 항목 검증 완료 시 이 문서 해당 섹션에 아래 형식으로 결과 추가:

```
- **[YYYY-MM-DD 검증]** PASS / FAIL / N/A. 상세: 브라우저 · OS · 세션 · 관찰 · 스크린샷 링크
```

FAIL 시 별도 티켓 발행 후 STATE.md 큐에 등재.

## 6. 스크린샷 저장 위치 (제안)

- 로컬: `~/Screenshots/youth-moa-java/YYYY-MM-DD_<항목>.png`
- 원격 공유 필요 시 GitHub Issue 첨부 or 프로젝트 Wiki

