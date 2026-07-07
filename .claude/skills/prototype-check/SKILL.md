---
name: prototype-check
description: youth-moa-java 의 Thymeleaf 템플릿과 디자인 자산 (prototype.html / prototype.tsx / wireframe.png) 의 갭을 분석. 화면 단위 또는 전체 일괄 스캔. ym-spec 에이전트의 단발성 명령 버전.
disable-model-invocation: true
---

사용자가 `/prototype-check` 를 입력하면 아래 절차를 실행한다. 인자 형태:

- `/prototype-check` — 전체 화면 일괄 분석 (8개)
- `/prototype-check <화면>` — 단일 화면 (예: `/prototype-check signup`, `home`, `list`, `detail`, `apply`, `login`, `header`, `footer`)

---

## 디자인 자산 우선순위 (사용자 확정 2026-06-30)

| 자산 | 역할 | 신뢰도 |
|---|---|---|
| `docs/00_assets/prototype.html` + `HANDOFF.md` | **절대 기준**. Claude Design 으로 생성한 구현 prototype | 🔵 최우선 |
| `docs/00_assets/prototype.tsx` | React 변환물. 동적 로직 (capInfo 등) 참조 | 🟢 보조. prototype.html 과 충돌 시 prototype.html 채택 |
| `docs/00_assets/wireframe.png` | Figma 원본 기획. PNG → Read 로 시각 확인 | 🟡 정책 원본. prototype.html 과 충돌 시 → 사용자 질문 |

---

## 화면 매핑 표

| 화면 | Thymeleaf | prototype.tsx 컴포넌트 | HANDOFF |
|---|---|---|---|
| `home` | `templates/index.html` | `HomeScreen` (line 416~574) | §5.1 |
| `list` | `templates/program/list.html` | `ProgramList` (695~819) + `FilterPopChip` + `ProgramCalendar` | §5.2 |
| `detail` | `templates/program/detail.html` | `ProgramDetail` (820~961) | §5.3 |
| `apply` | `templates/application/apply.html` | `ProgramApply` (962~1019) | §5.4 |
| `login` | `templates/user/login.html` | `LoginScreen` (1542~1581) | §5.5 |
| `signup` | `templates/user/signup.html` | `SignupScreen` (1414~1541) | §5.7 |
| `header` | `templates/fragments/header.html` | `Header` (319~381) | §4.4 |
| `footer` | `templates/fragments/footer.html` | `Footer` (384~412) | §4.5 |
| `centers` | `templates/center/list.html` + `detail.html` | `CentersScreen` (1919~2160) | §5.15 |
| `mypage` | `templates/mypage/*` | `MyPage*` | (D5 도입) |
| `search` | `templates/search/result.html` | `SearchResult` | (D4 도입) |

---

## Step 1 — 화면 식별

인자 없으면 위 8개 모두. 인자 있으면 해당 화면만.

## Step 2 — 3자산 정독 (해당 화면)

1. `prototype.html` — selector 또는 텍스트 검색으로 해당 컴포넌트 영역 추출
2. `prototype.tsx` — 매핑표의 line 범위 Read
3. `wireframe.png` — Read (이미지 직접 열람) 로 정책 텍스트 확인
4. `HANDOFF.md` — 매핑표의 section 정독
5. 현재 Thymeleaf 템플릿 Read

## Step 2-A — 아키텍처 레벨 대조 (skip 금지)

> **배경 (2026-07-07 F0h 사고)**: `/centers` PR 이 curl grep 기반 갭 스캔은 통과했으나 3-column vs 2-column 이라는 근본 아키텍처 차이를 못 잡아 재작업 발생.

curl grep 이전에 prototype 과 현재 구현의 **최상위 레이아웃 축**을 대조한다:

1. 페이지 최상위 flex/grid 축 갯수 (2-column vs 3-column vs stacked)
2. 각 컬럼의 폭·flex-basis (prototype 기준값 명시)
3. **인터랙션이 페이지 이동인가, 인라인 패널 전환인가**
4. sticky/floating 요소 (필터바, "이 지역에서 검색" 같은 map overlay)

셋 중 하나라도 다르면 "미세 갭" 이 아니라 **아키텍처 갭** 으로 분류해 리포트 최상단에 배치.

## Step 3 — 자산 간 갭 식별

```markdown
| 항목 | wireframe | prototype.html | prototype.tsx | 채택 |
|---|---|---|---|---|
| 예) 중복확인 버튼 | 있음 | 있음 | 누락 | prototype.html 우선 → 포함 |
```

**충돌 의사결정**:
- prototype.html ↔ prototype.tsx → prototype.html 채택
- prototype.html ↔ wireframe.png → **사용자 질문 (임의 결정 금지)**

## Step 4 — Thymeleaf vs prototype.html 갭

```markdown
| # | 항목 | 현재 Thymeleaf | prototype.html | 우선순위 |
|---|---|---|---|---|
| 1 | Hero 검색바 | 없음 | 있음 | 높 |
| 2 | ... | | | |
```

## Step 4-A — 화면별 체크리스트 (필수, 표준화)

매 화면 결과에 다음 체크리스트 포함 (사용자가 한눈에 갭 파악):

```markdown
### <화면> 체크리스트
- [ ] 헤더 / 푸터 fragment 일치
- [ ] Hero / 타이틀 / 본문 카피 (prototype 정합)
- [ ] 핵심 컴포넌트 (검색·필터·CTA 등) 모두 노출
- [ ] 동적 로직 (capInfo 같은 헬퍼) 반영
- [ ] 빈 상태 / 에러 상태 / 로딩 상태 디자인
- [ ] HTMX / JS 인터랙션 정합
- [ ] CSS 토큰 (color·spacing·radius·shadow) HANDOFF 정합
- [ ] a11y (aria-* / 키보드 / 색만 의미 전달 금지)
- [ ] 디자인 자산 누락 (이미지·아이콘)
- [ ] 라이팅 톤 (존댓말 / 전문용어 회피)
```

→ `[ ]` (미적용) / `[x]` (정합) / `[~]` (부분 정합) 마킹.

## Step 5 — 표준 리포트 출력

```markdown
# /prototype-check — <화면> (또는 전체)

## 화면별 결과
### <화면>
**디자인 출처**:
- prototype.html: <selector>
- prototype.tsx: line X~Y
- wireframe.png: <영역>
- HANDOFF: §

**자산 간 갭**: (있을 때만)
| 항목 | wireframe | prototype.html | prototype.tsx | 채택 |
| ... | | | | |

**Thymeleaf 갭**:
| # | 항목 | 현재 | prototype | 우선순위 |
| 1 | ... | | | |

**일치도 추정**: NN%

## 사용자 결정 필요 (정책 충돌)
- Q1: ...
- Q2: ...

## 다음 PR 후보 Top N
1. <작업 ID> — <설명> (영향: 화면 X·Y)
2. ...
```

---

## 출력 결과 관리

- 단일 화면 (`/prototype-check signup`) → 보고만, 파일 저장 X
- 전체 (`/prototype-check`) → 보고 + `docs/specs/_gap_<YYYYMMDD>.md` 저장 (의도 명시 시)
- 결과는 ym-spec 의 입력으로 그대로 활용 가능 (작업 큐 우선순위 정렬)

---

## 주의사항

- `disable-model-invocation: true` — 사용자가 명시적 `/prototype-check` 입력 시만 실행
- read-only — 코드 변경 금지
- ym-spec 에이전트도 동일 절차 따름. 본 Skill 은 단발성 명령용 단축
- 향후 P2 갭 루프 (`/loop`) 도입 시 본 Skill 의 전체 스캔 자동 반복
- `wireframe.png` 는 Read 도구로 직접 이미지 열람 가능 (PNG 시각 확인)
