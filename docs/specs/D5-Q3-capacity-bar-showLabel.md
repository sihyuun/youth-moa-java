# D5-Q3: capacity-bar fragment `showLabel` 파라미터 도입 (user 캘린더 전용)

> **상태**: `spec_confirmed` — 사용자가 Q0 재검토 결과 **user 만 범위**로 축소 확정 (2026-10-14).
> **담당 사이클**: ym-impl → ym-qa → ym-verify
> **배경 메모리**: 2026-07-13 D5 Q3 이월 결정 ("캘린더 뷰 구현 시" 조건 성립 — D-admin-calendar PR #262 머지 2026-10-13)
> **원본 티켓**: `project_youth_moa_deferred_admin.md` — D5 Q3 (CapacityBar showLabel 미니 모드)
> **범위 축소 사유**: admin prototype (`docs/00_assets/admin/prototype.html` L1125~1157) 재분석 결과, admin 캘린더 패널 카드는 **capacity bar 를 아예 사용하지 않고** 썸네일 100px + 진행/신청 기간 + 신청 현황 텍스트 + "상세보기" 버튼 구조로 디자인됨. 현재 admin 구현의 `program-calendar-panel-card-bar` 는 user prototype 을 흉내낸 것으로 **admin prototype 과 이탈 상태**. 올바른 admin 매칭은 별도 FOLLOW 티켓(`FOLLOW-admin-calendar-panel-card-prototype-match.md`)으로 분리.
>
> **이번 PR 은 user 캘린더만 다룬다**. admin `_calendar-fragment.html` 수정 / admin 계약 수정 / admin 쪽 `program-calendar-panel-card-bar*` CSS 삭제는 **FOLLOW 티켓 범위**이며, 이 PR 에서 건드리지 않는다.

---

## 1. 디자인 출처 (3자산 모두 명시)

| 자산 | 라인/섹션 |
|---|---|
| `docs/00_assets/prototype.tsx` L214~249 | `CapacityBar({ pg, showLabel=true })` 정의. `showLabel` false 시 상단 라벨 라인 전체(`<div>…</div>`) 제거, bar(h=6, radius=3)만 노출 |
| `docs/00_assets/prototype.tsx` L795~807 | 사용자 홈 캘린더 "이 날 선택된" 미니 카드 — `<CapacityBar pg={pg} showLabel={false}/>` (**이번 명세의 매칭 지점**) |
| `docs/00_assets/prototype.tsx` L991 | 프로그램 상세 — `<CapacityBar pg={pg} showLabel={false}/>` 호출. 상세는 bar 바로 위에 자체 커스텀 header 를 그려 쓰므로 **기존 `detailCapacityBar` fragment 가 이미 커버** (매칭 완료, 범위 밖) |
| `docs/00_assets/prototype.html` | tsx 와 동일 소스 |
| `docs/00_assets/HANDOFF.md` | CapacityBar 토큰·spacing 가이드 — 현행 `.capacity-bar{,-fill,-label}` 반영 완료 |
| **admin prototype.html L1125~1157** | admin 캘린더 패널 카드. **bar 없음** — bar 대신 텍스트 라인 구조. 이번 PR 범위 **밖**이며 FOLLOW 티켓에서 다룸 |
| 비교 대상 Thymeleaf | `src/main/resources/templates/fragments/capacity-bar.html`, `program/_calendar-fragment.html` L174~179 (user) |

---

## 1-A. 자산 간 갭 (user 캘린더 한정)

prototype.html ↔ prototype.tsx 는 동일 소스라 생략. 아래는 user prototype vs 현재 user 구현.

| 항목 | prototype.tsx | 현재 구현 (user) | 채택 |
|---|---|---|---|
| CapacityBar `showLabel` 파라미터 | L214 default `true` 지원, false 시 상단 라인 제거 | fragment 미지원 (파라미터 없음) | **prototype 채택 — 파라미터 추가** |
| 미니 카드 bar 네임스페이스 | 공용 CapacityBar 재사용 (`showLabel={false}`) | user 캘린더 패널 카드가 자체 `program-calendar-panel-card-bar{,-fill}` 네임스페이스 사용 | **prototype 채택 — fragment 호출로 전환** |
| bar 높이·radius·background | h=6, radius=3, bg=T.borderLight | `.capacity-bar` h=6 (매칭 완료) | 현상 유지 |
| 색상 클래스 축 | `pg.color` 5종 | fragment 는 `colorClass` string 수용 (primary/warning/error/secondary/muted) | **매칭 완료**. `ProgramCardDto.colorClass` 가 이미 공용 토큰 사용 중 (`ProgramCardDto.java` L96~137 확인) |

**갭 없는 영역**: detail 호출처(L991)는 `detailCapacityBar` fragment 가 이미 담당 — 범위 밖. admin 패널 카드는 **별도 디자인(bar 없음)** 이라 이 PR 밖. FOLLOW 티켓이 담당.

---

## 1-B. 데이터 모델 gap 표

이번 작업은 **template fragment 시그니처 변경 + user 캘린더 fragment 리팩터**로 **DB·엔티티·DTO 변경 전혀 없음**.

| prototype 필드 | 현재 엔티티/DTO | 조치 |
|---|---|---|
| (해당 없음 — 뷰 모델 재조립만) | `ProgramCardDto.pct`, `colorClass` 그대로 사용 | 변경 없음 |

---

## 2. 변경 범위 (파일 단위)

**Q0=(c) user 만 확정**. 단일 PR.

- [ ] `src/main/resources/templates/fragments/capacity-bar.html`
      — `capacityBar(..., showLabel)` 파라미터 추가. label `<div>` 전체를 `th:if="${showLabel}"` 로 감쌈 (bar 는 기존 `showBar` 유지).
- [ ] `src/main/resources/templates/program/_calendar-fragment.html` L174~179
      — `<div class="program-calendar-panel-card-bar">` → `~{fragments/capacity-bar :: capacityBar(pct=card.pct, colorClass=card.colorClass, primaryLabel='', secondaryLabel=null, showBar=true, showLabel=false)}` 호출로 전환.
- [ ] 기존 4곳 호출처 (home / favorites / search / program list) 명시 `showLabel=true` 전달 추가 (Q1=a)
      — `templates/index.html`, `templates/program/_card.html` 등 — ym-impl 이 Grep 으로 호출 지점 열거 후 1곳씩 `showLabel=true` 추가.
- [ ] `src/main/resources/static/css/main.css` — **user 쪽** `.program-calendar-panel-card-bar{,-fill,-fill--*}` 룰셋만 삭제 (Q7=a). admin 쪽은 FOLLOW 티켓이 담당하므로 **이 PR 에서는 건드리지 않는다**.
      - admin 과 user 가 **같은 CSS 셀렉터를 공유**할 경우 (단일 룰셋이 둘 다 커버): user 캘린더 template 은 더 이상 그 클래스를 쓰지 않지만 admin 캘린더가 여전히 쓰므로 **삭제 보류**. ym-impl 이 grep 결과를 먼저 확인하고, 공유 시 "FOLLOW 머지 후 cleanup" 코멘트만 남기고 CSS 유지. 전용 룰이면 삭제.
- [ ] `src/test/java/.../program/CapacityBarFragmentRenderTest.java`
      — `showLabel=false` 시 `.capacity-bar-label` 미렌더 / bar 1개 렌더
      — `showLabel=true` 시 label 1개 + bar 1개
      — `showBar=false, showLabel=true` (기존 경로 보존) 1개
- [ ] `src/test/java/.../program/ProgramCalendarRenderTest.java` (신설 또는 보강)
      — user 캘린더 패널 카드가 `.capacity-bar` 1개 + `.capacity-bar-label` **0개** 를 렌더
- [ ] `e2e/contracts/programs-calendar.ts` — `panel.card.capacity_bar.exists` 체크 1건 신설 (셀렉터 `.program-calendar-panel-card .capacity-bar`)
- [ ] 공용 — 기존 홈·목록·검색·favorites 계약은 변경 없음. 다만 Q5=b 2중 방어로 **`.capacity-bar-label` exists 체크를 홈/목록/검색/favorites 계약 중 1곳에 추가** (regression 보호). `e2e/contracts/programs.ts` 또는 `index.ts` 중 label 존재가 가장 명확한 지점 1곳에 신설.
- [ ] `docs/design-contracts/programs-calendar.md` — 패널 카드 bar 섹션에 "공용 `capacityBar` fragment 재사용 (`showLabel=false`)" 1줄 추가.

### 제외 (FOLLOW 티켓 범위)

- ❌ `templates/admin/program/_calendar-fragment.html` (admin 은 bar 자체를 쓰지 않을 예정)
- ❌ `e2e/contracts/admin-programs-calendar.ts`
- ❌ `docs/design-contracts/admin/programs-list-calendar.md`
- ❌ `AdminProgramCalendarRenderTest`
- ❌ admin 전용 CSS 룰 삭제

---

## 3. capacityBar fragment 시그니처 before/after

### Before (`src/main/resources/templates/fragments/capacity-bar.html` L24~40)
```thymeleaf
<th:block th:fragment="capacityBar(pct, colorClass, primaryLabel, secondaryLabel, showBar)">
    <div class="capacity-bar-wrap">
        <div class="capacity-bar-label">
            <span class="capacity-bar-primary" ... th:text="${primaryLabel}">...</span>
            <span class="capacity-bar-secondary" th:if="${secondaryLabel != null}" ...>...</span>
        </div>
        <div th:if="${showBar}" class="capacity-bar">
            <div class="capacity-bar-fill" ...></div>
        </div>
    </div>
</th:block>
```

### After
```thymeleaf
<th:block th:fragment="capacityBar(pct, colorClass, primaryLabel, secondaryLabel, showBar, showLabel)">
    <div class="capacity-bar-wrap">
        <div th:if="${showLabel}" class="capacity-bar-label">
            <span class="capacity-bar-primary" ... th:text="${primaryLabel}">...</span>
            <span class="capacity-bar-secondary" th:if="${secondaryLabel != null}" ...>...</span>
        </div>
        <div th:if="${showBar}" class="capacity-bar">
            <div class="capacity-bar-fill" ...></div>
        </div>
    </div>
</th:block>
```

**변경 요약**:
- 파라미터 1개 추가 (`showLabel`, 마지막 위치 — Thymeleaf named arg 사용이라 기존 호출 순서 영향 없음)
- label `<div>` 전체를 `th:if="${showLabel}"` 로 감쌈
- user 캘린더 패널 카드 호출: `capacityBar(pct=card.pct, colorClass=card.colorClass, primaryLabel='', secondaryLabel=null, showBar=true, showLabel=false)`
- 기존 4곳 호출처: `showLabel=true` 명시 추가 (Q1=a)

---

## 4. 디자인 계약 영향

### user — `e2e/contracts/programs-calendar.ts`

| 체크 | 현재 상태 | 이번 작업 후 |
|---|---|---|
| 기존 캘린더 셀/pill 체크 | 유지 | 유지 |
| **(신설)** `panel.card.capacity_bar.exists` | 없음 | `.program-calendar-panel-card .capacity-bar` kind=exists 추가 |

현재 user 계약은 `program-calendar-panel-card-bar-fill` 셀렉터를 사용하지 않음 → 셀렉터 전환 regression 없음.

### 공용 — `e2e/contracts/programs.ts` 또는 index 계약

Q5=b: `.capacity-bar-label` exists 체크 1건 신설. 홈/목록/검색/favorites 중 label 존재가 가장 명확한 지점 1곳. default `showLabel=true` → 명시 `true` 전환이 regression 없음을 보증.

### admin 계약·공용 계약 (`detail` 포함)

**이번 PR 변경 없음**. detail 은 `detailCapacityBar` 범위, admin 은 FOLLOW 범위.

---

## 5. 검증 시나리오

### 정적 검증 (ym-qa)

- `./gradlew compileJava test --tests JpaMappingTest` 통과
- `./gradlew test --tests CapacityBarFragmentRenderTest`
  - `showLabel=true`: `.capacity-bar-label` 1개 + `.capacity-bar-primary` 1개 + `.capacity-bar` 1개
  - `showLabel=false`: `.capacity-bar-label` 0개 + `.capacity-bar` 1개
  - `showBar=false, showLabel=true`: label 1개 + bar 0개 (기존 경로 보존)
- `./gradlew test --tests ProgramListRenderTest IndexRenderTest SearchResultRenderTest FavoritesRenderTest`
  - 기존 4곳이 label 유지 (명시 `showLabel=true` 전달 확인)
- `./gradlew test --tests ProgramCalendarRenderTest`
  - 캘린더 패널 카드가 `.capacity-bar` 1개 + `.capacity-bar-label` **0개** 렌더

### 동적 검증 (bootRun e2e 프로파일, 포트 8090)

1. `curl -s http://localhost:8090/ | Select-String -Pattern 'capacity-bar-label' -SimpleMatch | Measure-Object -Line` — 홈 카드 수와 일치 (기존 동작 보존)
2. `curl -s "http://localhost:8090/programs?view=calendar" | Select-String -Pattern 'capacity-bar-label' -SimpleMatch | Measure-Object -Line` — 패널은 사전 렌더 후 hidden. panel-group 안 label 0개, bar N개
3. `/css/main.css` 200 OK — user 전용 CSS 삭제 반영 (공유 CSS 라면 보류 코멘트 반영됨)
4. `curl -s http://localhost:8090/admin/programs?view=calendar` — **변경 없음** 확인 (admin 은 이번 범위 밖, 기존 bar 렌더 그대로)

### 계약 검사

```powershell
cd e2e; $env:BASE_URL="http://localhost:8090"; npx playwright test --project=contracts programs-calendar programs
```

- user `programs-calendar` 갭 0
- `programs` 또는 index 계약: 신설 `.capacity-bar-label` exists 체크 통과

**`admin-programs-calendar` 계약은 이번 PR 에서 변경하지 않으며**, admin 기존 bar 렌더가 그대로 통과하는지만 참고용으로 확인.

### 인터랙션 검증

- `tests/program-calendar.spec.ts` — 캘린더 셀 클릭 → 패널 slide-in → 카드 bar 렌더 → bar 폭이 `card.pct%` 실제 적용되는지 1회 assertion
- 홈·목록·검색·favorites 기존 spec green 유지

### 시각 검증 (사용자 영역)

- `/programs?view=calendar` → 날짜 셀 클릭 → 패널 카드 bar 가 **라벨 없이** bar 만 노출 (수치는 CSS 변경 없어 pixel 동일해야 함)
- `/admin/programs?view=calendar` → **현상 유지**. 변화 없어야 함 (FOLLOW 티켓이 다룸)

---

## 6. CSS 삭제 전략 (user 전용)

- user `_calendar-fragment.html` 이 더 이상 `program-calendar-panel-card-bar{,-fill}` 참조하지 않음을 Grep 으로 확인
- ym-impl 이 `grep -rn program-calendar-panel-card-bar src/main/resources` 로 **admin fragment 가 아직 쓰는지** 체크
  - admin 만 참조 → CSS 룰 **유지** (FOLLOW 가 삭제)
  - 아무도 참조 안 함 → CSS 룰 삭제
- 공유/전용 판정 결과를 PR 본문에 1줄로 기록

---

## 7. 갭 리스트 / 리스크 / 비용

### 갭 리스트 (user 캘린더 vs prototype)

| # | 항목 | 현재 상태 | prototype | 우선순위 |
|---|---|---|---|---|
| 1 | fragment `showLabel` 파라미터 | 없음 | L214 default `true` | 높음 |
| 2 | user 캘린더 패널 카드 bar 네임스페이스 | 자체 class | 공용 `CapacityBar` | 높음 |
| 3 | user 패널 카드 bar 색상 variant | `ProgramCardDto.colorClass` 재사용 가능 | `pg.color` 토큰 | 매칭됨 |

### 리스크

| 리스크 | 완화책 |
|---|---|
| 기존 4개 호출처 default 의존 → 명시 `true` 누락 시 라벨 사라짐 | Q1=a 명시 전달 + Q5=b 계약 label exists 체크 2중 방어. 각 호출처 render test 유지 |
| CSS 삭제 후 admin 쪽에서 참조 중이면 admin UI 깨짐 | Grep 선행. 공유 시 삭제 보류 명시 |
| 패널 카드 wrapper `margin-bottom: 10px` 과잉 간격 가능성 | Q6: ym-impl bootRun 시각 확인 후 결정. 필요 시 `.program-calendar-panel-card .capacity-bar-wrap { margin-bottom: 0 }` 1 라인 추가 |

### 비용 estimate (admin 제외로 감소)

| 항목 | LOC |
|---|---|
| capacity-bar.html 변경 | +2 / -1 |
| program/_calendar-fragment.html (user) | +7 / -5 |
| 기존 4곳 호출처 명시 `showLabel=true` | +4 |
| main.css user 쪽 삭제 (조건부) | -5 ~ -10 |
| CapacityBarFragmentRenderTest 추가 case | +25 |
| ProgramCalendarRenderTest 신설·보강 | +15 |
| programs-calendar.ts 체크 신설 | +10 |
| programs.ts label exists 체크 1건 | +6 |
| programs-calendar.md 1줄 | +1 |
| **합계** | **≈ +70 / -11 (≈ 59 net LOC — admin 제거로 약 ~20 LOC 감소)** |

---

## 8. 작업 큐 메타

- 작업 ID: `D5-Q3`
- 우선순위: 중
- 추정 단위: 1 PR
- 선행: D-admin-calendar PR #262 (머지 완료 2026-10-13) — 충족
- 후속: `FOLLOW-admin-calendar-panel-card-prototype-match` (admin 캘린더 패널 카드 전면 재디자인). **이 PR 머지 후 착수** (CSS 공유 룰 cleanup 충돌 피함)
- 상태: `spec_confirmed` (2026-10-14 사용자 결정)

---

## 9. 사용자 결정 요약 (2026-10-14 확정)

| Q | 결정 | 근거 |
|---|---|---|
| Q0 | **(c) user 만** | admin prototype 재분석 — admin 은 bar 없음. 올바른 매칭은 FOLLOW 티켓 |
| Q1 | **(a) 기존 4곳 명시 `showLabel=true` 전달** | 명시가 작업자 가독성 + render test 전수 검증 |
| Q2 | **(a) `ProgramCardDto.colorClass` 그대로 전달** | DTO 이미 공용 토큰(primary/warning/error/secondary/muted) 산출 중 (`ProgramCardDto.java` L96~137) |
| Q3 | **(a) 단일 PR** | admin 제외로 범위 더 작아짐 (≈59 LOC) |
| Q4 | **(b) POLICY 신설 안 함** | 캘린더 외 재현 가능성 낮음, 화면별 계약으로 충분 |
| Q5 | **(b) RenderTest + e2e 계약 2중 방어** | fragment-level + regression 보호 |
| Q6 | **ym-impl 시각 확인 후 결정 (유보)** | bootRun 시각 확인 결과에 따라 1 라인 override 추가 가능 |
| Q7 | **(a) user 쪽 CSS 삭제 (조건부)** | admin 공유 여부 Grep 후 결정. 공유 시 FOLLOW 머지 후 cleanup |
| Q8 | **(a) 권장안 일괄 채택** | — |

---

## 10. 관련 티켓

- **후속**: [FOLLOW-admin-calendar-panel-card-prototype-match.md](./FOLLOW-admin-calendar-panel-card-prototype-match.md) — admin 캘린더 패널 카드를 admin prototype L1125~1157 매칭 (bar 제거 + 진행/신청 기간·현황 텍스트 + 상세보기 버튼)
- **선행**: PR #262 D-admin-calendar (머지 완료 2026-10-13)
- **원본**: 메모리 `project_youth_moa_deferred_admin.md` D5 Q3

---

## 11. 다음 단계

ym-impl 호출 → compileJava → RenderTest → bootRun curl 1회 → 계약 검사 → PR. 예상 1 세션.
