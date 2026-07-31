# F-home-30-pagination — 홈 프로그램 30개 · 4개씩 페이지네이션

> **상태**: `spec_declined` (2026-07-31 사용자 결정 — 아래 §0-A 참조)
> **브랜치 후보**: (없음 — 구현 없음)
> **결정**: **prototype 4장 유지** · wireframe 30개 정책을 **의도적 이탈 (deviation)** 로 기록
> **추정 단위**: 0 PR (문서 기록만)

---

## 0-A. 2026-07-31 결정 (사용자 확정)

**결정**: **prototype 유지 · wireframe 30개 페이지네이션 미채택**.

**근거**:
1. prototype.tsx L604~638 도 **4장 고정** (`PROGRAMS.slice(0,4)`) — 페이지네이션·carousel·화살표 UI 전무. wireframe 30개 정책은 **prototype 이탈**
2. 「wireframe vs prototype 비대칭 판정 규칙」에 따라 **prototype 이 확정 디자인**이며 wireframe 만 근거로 UX 를 확장하는 것은 규칙 역행
3. 계약 `e2e/contracts/home.ts` 의 `programs.card.count expected: 4` 가 현재 갭 0. 계약 재정의 비용이 큼

**후속 조치**:
- `docs/wireframe-policy/README.md` A1 항목을 "의도적 이탈" 로 마킹 (본 스펙 참조)
- 향후 wireframe 30개 재검토 필요 시 이 스펙 파일을 다시 활성화

**Q2~Q6 는 Q1 결정에 종속되므로 모두 미채택**. 아래 §7 은 참조용으로 보존.

---

## 0. 요약

`docs/wireframe-policy/home.md` 의 배치 항목 #1~#3 을 반영한다. 홈 프로그램 섹션을 **최신 등록순 최대 30개, 4개씩 페이지네이션 (좌/우 화살표), 마지막 페이지 `>` 클릭 시 프로그램 전체 목록(`/programs`) 이동** 으로 재구성.

- 대상 상태: **비로그인** 홈 (프로그램 섹션)
- 로그인 홈은 "맞춤 추천 4건" 유지 (범위 외 — Q1 참조)

---

## 1. 배경 · 3자산 대조

### 1-1. wireframe.png 정책 (`docs/wireframe-policy/home.md` L11~30, L52~57)

- 최신 등록된 프로그램부터 **최대 30개** 노출
- 홈 안에서 **4개씩** 페이지네이션 (좌 [이전] / 우 [다음])
- 마지막 페이지의 `>` 클릭 → WF-5-001-01 = `/programs` (전체보기)

### 1-2. prototype.tsx L604~638 (HomeScreen 비로그인 분기)

```tsx
{!isLoggedIn && (
  <div style={{ padding:'44px 80px 40px' }}>
    ...
    <Btn variant="outline" size="s" onClick={()=>go('programs')}>전체보기</Btn>
    ...
    <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:20 }}>
      {PROGRAMS.slice(0,4).map((pg,i)=>{ ... })}
    </div>
  </div>
)}
```

- **4개 고정 노출** (`PROGRAMS.slice(0,4)`)
- **페이지네이션 UI 없음**
- 우상단 `전체보기` 버튼 1개만 존재 → `/programs` 이동
- 정렬 축은 mock `PROGRAMS` 배열 순서 (createdAt 아님 — 판단 불가)
- carousel/dot indicator/화살표 컨트롤 **부재**

### 1-3. 현재 구현 (`HomeController` / `HomeService` / `index.html` L147~210)

- 비로그인: `findTop4ByIsActiveTrueOrderByEndDateAsc()` → **Top 4 마감임박** 고정
- 정렬 축: `endDate ASC` (마감임박)
- `전체보기` 링크 → `/programs` 이동
- 페이지네이션 UI 없음

### 1-4. 3자산 비교표

| 항목 | wireframe.png | prototype.tsx | 현재 구현 | 결론 |
|---|---|---|---|---|
| 노출 개수 | **최대 30개** | 4개 고정 | 4개 고정 | ⚠️ wireframe vs prototype 상충 → **Q1** |
| 정렬 축 | 최신 등록순 (`createdAt DESC`) | mock 순서 (판단 불가) | 마감임박 (`endDate ASC`) | ⚠️ **Q2** |
| 페이지네이션 UI | 좌 [이전] / 우 [다음] (스타일 미규정) | 없음 | 없음 | ⚠️ **Q3** — prototype 참조 불가, 신설 필요 |
| 전체보기 진입점 | 마지막 페이지 `>` 클릭 | 상단 `전체보기` 버튼 | 상단 `전체보기` 링크 | ⚠️ **Q4** |
| 상단 `전체보기` 버튼 존치 | 미규정 (배제하지 않음) | 있음 | 있음 | (Q4 종속) |
| 모바일 처리 | 미언급 (desktop 기준) | 반응형 미상 | 반응형 미상 | ⚠️ **Q5** |
| 구현 방식 | 미언급 | — | — | ⚠️ **Q6** (원 요청의 Q5) |

### 1-5. ⚠️ wireframe vs prototype 비대칭 판정 규칙 적용

**중요**: prototype 은 4장 고정이며 페이지네이션이 **없다**. wireframe 만 30개 · 페이지네이션을 명시한다.

프로젝트 CLAUDE.md 「디자인 계약이 대체」 규칙상 계약(=prototype 추출)이 기준이다. 따라서 **wireframe 30개 채택은 계약을 wireframe 정책으로 재정의하는 결정**이며, 반드시 사용자 명시 승인이 필요하다 (§7 Q1).

승인되면 다음 2가지가 동시에 일어난다:
- `docs/design-contracts/home.md` §2 표 (비로그인/로그인 분기) 수정: "카드 4개" → "30개 · 4개씩 페이지네이션"
- `e2e/contracts/home.ts` `programs.card.count` 검사 수정: `expected: 4` → 페이지당 4개 검사 + 30개 데이터 검사로 재설계

---

## 2. 디자인 출처

| 자산 | 위치 |
|---|---|
| wireframe.png 정책 | `docs/wireframe-policy/home.md` L11~30, L52~57 |
| prototype.tsx | L604~638 (HomeScreen 비로그인 분기) |
| 현재 구현 | `HomeController.java`, `HomeService.java`, `templates/index.html` L147~210 |
| 계약 (수정 대상) | `e2e/contracts/home.ts` `programs.card.count`, `programs.row.gap` |
| 서술 계약 (수정 대상) | `docs/design-contracts/home.md` §2 |

---

## 3. 데이터 모델 gap 표

prototype 이 참조하는 `PROGRAMS` mock 배열의 프로퍼티는 이미 Program 엔티티에 매핑되어 있음. **본 티켓은 스키마 변경 없음.**

| prototype 필드 | 현재 엔티티 | 조치 |
|---|---|---|
| `pg.id, title, center, date, ...` | Program.* | 매핑 완료 |
| — | `Program.createdAt` (BaseTimeEntity 상속) | 정렬 축으로 사용 (신규 컬럼 아님) |
| — | `Program.isActive` | 필터 조건 유지 |

**Flyway 마이그레이션 불필요**. `V4__*.sql` 발행 없음.

---

## 4. 변경 범위 (파일 단위)

### 4-1. Java

- [ ] `src/main/java/io/github/sihyuuun/youthmoa/program/ProgramRepository.java`
  - 신규 메서드: `findTop30ByIsActiveTrueOrderByCreatedAtDesc()` (List<Program>, 반환 크기 최대 30)
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/web/HomeService.java`
  - 신규 메서드: `findLatest30ProgramCards()` → 30개 ProgramCardDto 반환
  - 기존 `findTopPrograms()` / `findTopProgramCards()` 존치 (호환성 · 다른 호출부 있으면 유지, 없으면 삭제 가능 — 확인 필요)
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/web/HomeController.java`
  - `topPrograms` 모델 attribute 를 `latestPrograms` (30개) 로 교체 (또는 이름 유지 + 값 30개)
  - **Q6 결정에 따라**: 서버 사이드 페이지네이션 채택 시 `/`?`page=0` 파라미터 처리 + HTMX 부분 갱신 endpoint (`GET /home/programs?page=N` → fragment) 신설

### 4-2. Thymeleaf

- [ ] `src/main/resources/templates/index.html` L147~210
  - `home-program-row` 를 `.home-program-carousel` 래퍼로 감싸고 4개씩 slice
  - 좌/우 화살표 컨트롤 마크업 추가
  - 마지막 페이지 우측 화살표는 `<a href="/programs">` 로 라우팅
- [ ] `src/main/resources/templates/fragments/home-program-page.html` (Q6=A 채택 시 신규)
  - HTMX 로 교체할 fragment

### 4-3. CSS

- [ ] `src/main/resources/static/css/main.css`
  - `.home-program-carousel` 컨테이너 (position:relative, overflow:hidden 여부는 Q6 종속)
  - `.home-program-pagination` (좌·우 화살표 컨테이너)
  - `.home-program-page-btn` (버튼 스타일 — Q3 결정 대기)
  - `.home-program-page-btn--disabled` (첫 페이지 [이전] · Q4 결정에 따라 마지막 [다음] 처리)

### 4-4. JS (Q6=B 채택 시)

- [ ] `src/main/resources/static/js/home-program-pagination.js` (신규)
  - 30개 카드 마크업을 4장씩 슬라이스해 렌더
  - 좌/우 클릭 시 페이지 인덱스 이동 + 마지막 페이지에서 `>` 클릭 → `location.href='/programs'`

---

## 5. 컴포넌트 명세

### 5-1. 페이지 구성

- 총 데이터: 최대 30개 (실제 활성 프로그램 수 < 30 이면 그만큼)
- 페이지 크기: 4개
- 총 페이지 수: `ceil(N/4)` — 최대 8페이지 (30 → 8페이지, 마지막 페이지 2개)
- 초기 페이지: 0 (첫 4개)

### 5-2. 좌우 화살표 동작

| 위치 | 상태 | 클릭 동작 |
|---|---|---|
| 좌 [이전] | 첫 페이지 (page=0) | disabled (또는 hidden — Q3 결정) |
| 좌 [이전] | 그 외 | page-- |
| 우 [다음] | 첫 페이지 ~ 마지막-1 페이지 | page++ |
| 우 [다음] | 마지막 페이지 | **→ `/programs` 이동** (WF-5-001-01) |

### 5-3. 활성 데이터 부족 케이스

- 활성 프로그램 < 5 → 페이지네이션 컨트롤 자체 hidden (단일 페이지)
- 활성 프로그램 == 0 → 섹션 전체 hidden (또는 empty state — Q7 결정 여지)

---

## 6. 검증 시나리오

### 6-1. 정적 검증

- `./gradlew compileJava` 통과
- 단위 테스트:
  - `ProgramRepositoryTest` (@DataJpaTest, H2): `findTop30...OrderByCreatedAtDesc()` 가 정확히 30건 이하 + createdAt DESC 정렬 반환
  - `HomeServiceTest`: 30개 seed 시 findLatest30 반환 크기·순서
- `IndexRenderTest` (@WebMvcTest 또는 render): 비로그인 GET `/` 시 카드 최대 4개 렌더 (page=0 default) + 페이지네이션 컨트롤 마크업 존재

### 6-2. 동적 검증 (curl)

Q6=A (HTMX 서버 사이드) 채택 시:
```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/                          # 200
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/?page=0                   # 200 (page 파라미터 무시 시에도 200)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/home/programs?page=3      # 200 (HTMX fragment)
curl -s http://localhost:8090/ | grep -c 'home-program-card'                            # 4
curl -s http://localhost:8090/home/programs?page=7 | grep -c 'home-program-card'        # 2 (30→last page)
```

Q6=B (클라이언트 사이드) 채택 시:
```bash
curl -s http://localhost:8090/ | grep -c 'home-program-card'                            # 30 (전부 서버 렌더 후 JS slice)
```

### 6-3. 계약 검사 (필수)

`e2e/contracts/home.ts` 수정 후 통과 확인:
```bash
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts --grep home
```

**수정 대상 계약 항목**:
- `programs.card.count`: `expected: 4` (초기 노출 = page 0)
  - Q6=B 시: 상시 렌더 30개 → **초기 표시(display:block) 카드 4개** 검사로 변경 (`kind: 'count'` + `:visible` selector 활용)
- 신규 계약 추가:
  - `programs.pagination.exists` (좌·우 화살표 button 2개)
  - `programs.pagination.prev.disabled@page0` (첫 페이지에서 disabled)
  - `programs.pagination.next.route@lastpage` (마지막 페이지의 next 는 `<a href="/programs">`)
- `docs/design-contracts/home.md` §2 표: 비로그인 카드 개수 → "4 (page 0), 총 30개 페이지네이션"

### 6-4. 회귀 방어 (기능 E2E)

- `e2e/tests/home.spec.ts` (없으면 신규): 좌/우 화살표 클릭 시 카드 세트 교체, 마지막 페이지 next 클릭 시 `/programs` 이동
- `--project=chromium` green 유지

### 6-5. 시각 검증 (사용자 영역)

- 좌/우 화살표 hover/disabled 상태
- 페이지 전환 애니메이션 (Q3 결정에 따라 fade/slide/none)
- 모바일 (Q5 결정 반영 확인)

---

## 7. 사용자 결정 필요 (Q1~Q6)

### Q1. wireframe 30개 채택 vs prototype 4개 유지 vs 하이브리드

계약(=prototype) 이 4개 고정인데 wireframe 은 30개 페이지네이션을 요구한다. **비대칭 판정 규칙상 사용자 명시 승인 없이는 계약을 wireframe 으로 대체할 수 없다**.

| 옵션 | 설명 | 파급 |
|---|---|---|
| **A (권장)** | wireframe 정책 채택 → 30개 · 4개씩 페이지네이션 | 계약 재정의 필요 (home.ts / home.md 수정). 기획 원본이 wireframe 이므로 정합 |
| B | prototype 유지 → 4개 고정 | wireframe 정책과 배치. `docs/wireframe-policy/home.md` 배치 항목 #1 을 "prototype 우선"으로 종결 처리 |
| C | 하이브리드 — 노출은 4개지만 좌/우 화살표로 다음 4개 세트 (총 30개) | wireframe 의 "홈 안 전환" 정신은 유지하되 UI 는 prototype 스타일 유지. 실질 = A |

→ 본 spec 이후 문서는 **A 를 전제**로 이어짐.

### Q2. 정렬 축 — 최신 등록순 vs 마감임박 vs 하이브리드

| 옵션 | 정렬식 | 근거 |
|---|---|---|
| **A (권장, wireframe)** | `createdAt DESC` | wireframe L14 "최신 등록된 프로그램부터" 명시 |
| B (현행) | `endDate ASC` | 사용자 유입 유도 · 마감 임박 우선 |
| C 하이브리드 | 페이지 0 = 마감임박 4개, 페이지 1~ = 최신순 26개 | 두 정책 절충. 구현 복잡도 증가 |

wireframe 이 근거이므로 **A 를 전제**로 이어짐.

### Q3. 페이지네이션 UI 스타일

| 옵션 | 설명 | 근거 |
|---|---|---|
| **A (권장, wireframe에 근접)** | 좌우 화살표만 (`<` / `>`), 원형 배경 없음 or ghost 스타일 | wireframe 이 "이전/다음 버튼" 만 명시. dot indicator 언급 없음 |
| B | dot indicator (● ● ○ ○ ○ ○ ○ ○) 8개 + 화살표 | 페이지 수를 시각적으로 표시. 화살표만보다 정보량 ↑ |
| C | 페이지 번호 (`< 1 2 3 4 5 6 7 8 >`) | 목록 페이지에 가까운 UX. 홈 섹션치고는 무거움 |

**참고**: prototype 이 4장 고정이라 스타일 참조 불가. Btn 컴포넌트(`variant="outline"`)의 화살표 아이콘(Icon `chevronLeft` / `chevronRight`) 재사용 권장 — 톤 일관성.

### Q4. `>` 이동 버튼 위치 (전체보기 진입점)

| 옵션 | 설명 |
|---|---|
| **A (권장, wireframe)** | 마지막 페이지의 우측 화살표 = `/programs` 이동. 상단 `전체보기` 버튼도 유지 (2 진입점) |
| B | 마지막 페이지 우측 화살표만 (상단 `전체보기` 제거) | 진입점 통합. 하지만 첫 페이지에서 전체보기 즉시 접근 불가 → UX 저하 |
| C | 상단 `전체보기` 만 (마지막 페이지 next 는 hidden/disabled) | wireframe 정책 위배 |

### Q5. 모바일 처리 (wireframe 미언급)

| 옵션 | 설명 |
|---|---|
| **A (권장)** | Desktop 과 동일 (4개/페이지, 좌우 화살표). 카드 폭 축소 | 정책 일관성 |
| B | 무한 스크롤 (30개를 세로로) | wireframe 4개/페이지 정책 위배 |
| C | 그리드 축소 (2x2 = 페이지당 4개 유지) | A 와 실질 동일 |

### Q6. 구현 방식 — 서버 사이드 (HTMX) vs 클라이언트 사이드

| 옵션 | 설명 | 장 | 단 |
|---|---|---|---|
| **A (권장)** | HTMX 서버 사이드 부분 갱신. `GET /home/programs?page=N` → fragment | 초기 페이로드 4개만. SEO 첫 페이지 노출. Boot/HTMX 조합 학습 목적 부합 | 라우트 1개 추가 |
| B | 30개 전부 렌더 후 JS slice/display 토글 | 페이지 전환 즉각 · 서버 왕복 없음 | 초기 페이로드 30개 (이미지 lazy-load 필수). SEO 는 첫 페이지가 30개 노출 |

두 방식 모두 채택 가능. **A** 는 프로젝트 CLAUDE.md 의 HTMX 학습 지향에 부합.

---

## 8. 의존성 · 선행 작업

- **선행 없음** — 독립 실행 가능
- **후행 영향**:
  - `docs/design-contracts/home.md` §2 갱신 (같은 PR 에 포함)
  - `e2e/contracts/home.ts` 갱신 (같은 PR 에 포함)
  - `docs/wireframe-policy/home.md` 배치 항목 표에서 #1~#3 "결정 완료" 로 마킹

---

## 9. 데이터 소비 지점

홈 프로그램 카드 데이터를 표시하는 위치. 이 티켓의 정렬 축·개수 변경이 다른 곳에 영향 미치는지 확인.

| 소비 지점 | 파일 | 프로토타입 매칭 | 갭 |
|---|---|---|---|
| 홈 비로그인 프로그램 섹션 | `index.html` L155~209 | 이번 티켓 대상 | — |
| 홈 로그인 맞춤추천 | `index.html` L127~145 | 별개 알고리즘 (interests 스코어링) | **본 티켓 영향 없음** |
| `/programs` 목록 | `program/list.html` | 별개 (검색·필터·페이지네이션 존재) | **본 티켓 영향 없음** |

**write→read 왕복 검증**: 본 티켓은 read-only 데이터 표시 변경이므로 write 경로 없음. 신규 프로그램 시드/등록 후 홈 첫 페이지에 최신 프로그램이 노출되는지 확인 시나리오만 필요:
- (관리자 admin 트랙 완료 후 재검토) 프로그램 등록 → 홈 첫 페이지에 최신 반영 확인

---

## 10. 작업 큐 메타

| 키 | 값 |
|---|---|
| 작업 ID | `F-home-30-pagination` |
| 우선순위 | 미정 (Q1~Q6 결정 후 확정) |
| 추정 PR | 1 (Q6=A) 또는 1 (Q6=B 도 단일 PR) |
| 상태 | `spec_draft` |
| 다음 단계 | 사용자 Q1~Q6 결정 → `spec_confirmed` 승격 → `ym-impl` 인계 |
| 계약 회귀 방어 | `e2e/contracts/home.ts` + `docs/design-contracts/home.md` 동시 갱신 (필수) |

---

## 11. 명세 산출 완료 안내

명세 산출 완료. **Q1~Q6 결정** 이 필요합니다. 특히:

1. **Q1 은 계약 재정의 결정** 이므로 반드시 명시 승인 필요 (prototype 은 4장 고정)
2. Q2~Q5 는 권장안이 있으므로 "권장안 채택" 응답으로 진행 가능
3. Q6 은 학습 방향에 따라 선택 (HTMX 학습이면 A, JS 학습이면 B)

결정 반영 후 상태를 `spec_confirmed` 로 승격하고 ym-impl 에 인계할 수 있습니다.
