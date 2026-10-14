# FOLLOW: admin 캘린더 패널 카드 prototype 매칭 (bar 제거 + 진행/신청 기간·현황 텍스트)

> **상태**: `spec_draft` — Q1~Q6 사용자 결정 후 `spec_confirmed` 로 전환
> **담당 사이클**: ym-spec (본 문서) → 사용자 Q 결정 → ym-impl → ym-qa → ym-verify
> **선행 티켓**: `D5-Q3-capacity-bar-showLabel.md` (user 쪽 공용 fragment 전환) — **반드시 머지 후 착수**. CSS 공유 룰 cleanup 충돌을 피하기 위함
> **원본 발견 경로**: 2026-10-14 D5-Q3 Q0 재검토 중 admin prototype L1125~1157 재분석. 현재 admin 구현 (`admin/program/_calendar-fragment.html` L168~174) 이 user prototype 패턴을 흉내낸 capacity bar 렌더 중이나, admin prototype 은 완전히 다른 디자인

---

## 1. 디자인 출처

### admin prototype — `docs/00_assets/admin/prototype.html` L1125~1157 (프로그램 관리 내장 캘린더 패널)

주요 발췌:

| 라인 | 내용 |
|---|---|
| L1130 | 썸네일: `width:100%; height:100px; background:#F1EFFC; display:flex; align-items:center; justify-content:center; font-size:32px; font-weight:700; color:#3F30E9` — 보라 bg 에 32px 보라 텍스트 썸네일 (`{{ progCalPanel.thumbText }}`) |
| L1134 | 제목 h3: `font-size:14px; font-weight:700; color:#2B2A3D; line-height:1.4; cursor:pointer; text-decoration:underline; text-decoration-color:#E3E1E8` — 밑줄 **점선 느낌의 연한 회색**, 클릭 시 `progCalGoDetail` |
| L1135 | 센터명 span: `font-size:11px; color:#6E6B82` |
| L1137 | 구분선: `height:1px; background:#F0EFF3` |
| L1139~1142 | **진행 기간** 라인: `justify-content:space-between`. 좌측 라벨 `font-size:11px; color:#A6A3B3` ("진행 기간"), 우측 값 `font-size:11px; color:#374151; font-family:Inter` |
| L1143~1146 | **신청 기간** 라인: 같은 구조. 라벨 "신청 기간", 값 `{{ progCalPanel.applyPeriod }}` |
| L1147~1150 | **신청 현황** 라인: 라벨 "신청 현황" (`#A6A3B3`), 값 `font-size:12px; font-weight:700; color:#3F30E9` ("`{applied}/{capacity}명`" primary bold) |
| L1152~1155 | **"상세보기" 버튼**: `width:100%; padding:9px; background:#3F30E9; color:white; border:none; border-radius:8px; font-size:13px; font-weight:600`, 오른쪽 chevron SVG 아이콘 포함, 클릭 시 `progCalGoDetail` |

### admin prototype 전용 CALENDAR 화면 — L2235~ (참고용)

프로그램 관리 내장 캘린더(L1125~)와 별도로 **CALENDAR 화면 (`isCalendar`)** 이 존재. 셀/pill 디자인은 유사하나 패널 구조는 다름. 이번 티켓은 L1125~1157 의 **프로그램 관리 내장 캘린더 패널** 매칭을 기준으로 한다. CALENDAR 화면 자체는 아직 구현 대상 밖.

### HANDOFF.md

- L232: "Google Calendar 패턴 — 좌측 캘린더 + 우측 slide 패널" (현행 구조 유지)
- 토큰 매핑: `#2B2A3D=text`, `#6E6B82=textSec`, `#A6A3B3=textTri`, `#3F30E9=primary`, `#F1EFFC=primary-bg`, `#F0EFF3=border-light`, `#374151=text (근접)`

### 현재 구현 — `src/main/resources/templates/admin/program/_calendar-fragment.html` L144~177

```thymeleaf
<a th:each="card : ${entry.value}"
   th:href="@{/admin/programs/{id}(id=${card.id})}"
   class="program-calendar-panel-card">
  <div class="program-calendar-panel-card-thumb">
    <img ... width="60" height="60"/>   ← prototype 은 100%×100px
    <span ...>IMG</span>                 ← prototype 은 `thumbText` (제목 이니셜 32px)
  </div>
  <div class="program-calendar-panel-card-body">
    <div class="program-calendar-panel-card-meta">
      <span ... legend-dot ...></span>
      <span class="program-calendar-panel-card-org">센터</span>
      <span class="program-calendar-panel-card-chip ...">D-3</span>   ← prototype 에 chip 없음
    </div>
    <div class="program-calendar-panel-card-title">제목</div>
    <div class="program-calendar-panel-card-capacity">
      <div class="program-calendar-panel-card-bar">                    ← prototype 에 bar 없음
        <div class="program-calendar-panel-card-bar-fill ..." style="width:XX%"></div>
      </div>
    </div>
  </div>
</a>
```

**이탈 지점**: 썸네일 크기 / chip 존재 / capacity bar 존재 / 진행·신청 기간 텍스트 부재 / 신청 현황 숫자 라인 부재 / "상세보기" 버튼 부재.

---

## 2. 목표 구조 (admin prototype 매칭 TO-BE)

```thymeleaf
<article class="admin-program-calendar-panel-card">
  <!-- 썸네일 (100% × 100px) — 썸네일 방식은 Q1 결정 -->
  <div class="admin-program-calendar-panel-card-thumb">
    <!-- (a) 이미지 우선: 이미지 있으면 img, 없으면 thumbText fallback -->
    <!-- (b) prototype 그대로 thumbText 만 -->
    <span class="admin-program-calendar-panel-card-thumb-text" th:text="${card.thumbText}">가</span>
  </div>

  <!-- Body -->
  <div class="admin-program-calendar-panel-card-body">
    <!-- 제목 (상세 이동 링크) + 센터 -->
    <div>
      <h3 class="admin-program-calendar-panel-card-title">
        <a th:href="@{/admin/programs/{id}(id=${card.id})}" th:text="${card.title}">제목</a>
      </h3>
      <span class="admin-program-calendar-panel-card-center" th:text="${card.centerName}">센터명</span>
    </div>

    <!-- 구분선 -->
    <div class="admin-program-calendar-panel-card-divider"></div>

    <!-- 정보 3라인 -->
    <dl class="admin-program-calendar-panel-card-info">
      <div><dt>진행 기간</dt><dd th:text="${card.period}">2026-10-01~10-31</dd></div>
      <div><dt>신청 기간</dt><dd th:text="${card.applyPeriod}">2026-09-15~09-30</dd></div>
      <div><dt>신청 현황</dt>
          <dd class="admin-program-calendar-panel-card-applied"
              th:text="${card.applied} + '/' + ${card.capacity} + '명'">8/20명</dd></div>
    </dl>

    <!-- 상세보기 버튼 -->
    <a class="admin-program-calendar-panel-card-detail-btn"
       th:href="@{/admin/programs/{id}(id=${card.id})}">
      상세보기
      <svg>...chevron...</svg>
    </a>
  </div>
</article>
```

---

## 3. 변경 범위 (파일 단위)

- [ ] `src/main/resources/templates/admin/program/_calendar-fragment.html` L144~177 — 패널 카드 전면 재작성
- [ ] `src/main/java/.../admin/program/AdminProgramCalendarPanelCardDto.java` **또는** 기존 DTO 확장 (Q3 결정)
      - 필드 추가 가능성: `period`, `applyPeriod`, `applied`, `capacity`, `thumbText`
      - 기존 공용 `ProgramCardDto` 는 `dateLabel`(진행기간), `applicantCount`, `capacity`, `title` 보유 → `applyPeriod` 와 `thumbText` 만 신규 산출 필요
      - `applyPeriod` 포매터 재사용 범위는 Q4
- [ ] `AdminProgramCalendarService.java` — 패널 데이터 산출 시 신규 필드 세팅. 현행 `panelData: Map<Integer, List<ProgramCardDto>>` 를 그대로 쓸지 전용 DTO 로 교체할지 Q3
- [ ] `src/main/resources/static/css/main.css` **또는** `admin.css` — admin 전용 패널 카드 룰셋 신설. **D5-Q3 가 CSS 공유 룰 삭제 보류**로 처리한 경우 FOLLOW 가 삭제 완료:
      - 삭제: `.program-calendar-panel-card-bar{,-fill,-fill--*}`, `.program-calendar-panel-card-capacity`, `.program-calendar-panel-card-chip*`, `.program-calendar-panel-card-meta`, `.program-calendar-panel-card-thumb` (prototype 과 크기/배경 다르므로 재작성 전제)
      - 신설: 썸네일 100px 보라 bg·32px 보라 텍스트, 제목 밑줄, 구분선, 정보 3라인 dl, 상세보기 버튼
- [ ] `e2e/contracts/admin-programs-calendar.ts` — 패널 카드 셀렉터 전수 갱신:
      - `panel.card.thumb_text.exists` 또는 `panel.card.thumb_img.exists` (Q1)
      - `panel.card.title.exists` + `title.font_size: 14px, font_weight: 700`
      - `panel.card.center.exists` + 11px / `#6E6B82`
      - `panel.card.info.period.exists` / `info.apply_period.exists`
      - `panel.card.applied.font_weight: 700` / `color: var(--color-primary)`
      - `panel.card.detail_btn.exists` + `background: var(--color-primary)` / `color: white`
      - `panel.card.capacity_bar.absent` (prototype 에 없음 — negative check)
      - 모든 check 에 `proto: 'admin/prototype.html L11xx'` 라인 인용
- [ ] `docs/design-contracts/admin/programs-list-calendar.md` — 패널 카드 섹션 전면 갱신. "D5-Q3 와 결별: admin 캘린더는 capacity bar 를 사용하지 않는다 — 진행/신청 기간 + 신청 현황 숫자 라인 + 상세보기 버튼 구조" 명시
- [ ] `src/test/java/.../admin/program/AdminProgramCalendarRenderTest.java` — 신설 또는 보강:
      - `.admin-program-calendar-panel-card-thumb` 존재
      - `.admin-program-calendar-panel-card-title` 안 `<a>` 존재 (상세 이동)
      - 정보 3라인 (`dt` 텍스트 "진행 기간" / "신청 기간" / "신청 현황")
      - `.admin-program-calendar-panel-card-applied` 가 `applied/capacity명` 패턴
      - `.admin-program-calendar-panel-card-detail-btn` 존재
      - `.capacity-bar` **0개** (regression — bar 재발 방지)
- [ ] `tests/admin-program-calendar.spec.ts` — 패널 카드 렌더 + "상세보기" 버튼 클릭 → `/admin/programs/{id}` 이동 1 assertion

---

## 4. 선후 관계

| # | 티켓 | 상태 |
|---|---|---|
| 선행 1 | PR #262 D-admin-calendar | 머지 완료 (2026-10-13) |
| 선행 2 | `D5-Q3-capacity-bar-showLabel.md` (user only) | spec_confirmed, ym-impl 대기 |
| **본 티켓** | FOLLOW admin 캘린더 패널 카드 prototype 매칭 | spec_draft |

**D5-Q3 머지 전에 착수 금지**. 이유:
- D5-Q3 가 CSS 공유 룰 `program-calendar-panel-card-bar*` 삭제 여부를 "admin 아직 참조 중" 조건으로 보류 처리할 수 있는데, FOLLOW 가 admin 쪽 참조를 제거해야 그 CSS 를 clean up 할 수 있다
- 두 티켓이 동일 fragment 파일 (`_calendar-fragment.html`) 과 동일 CSS 섹션을 건드리므로 순차 머지가 안전

---

## 5. 리스크

| 리스크 | 완화책 |
|---|---|
| `AdminProgramCalendarPanelCardDto` 가 존재하지 않고 공용 `ProgramCardDto` 가 쓰이는 경우 (현행 `CalendarViewDto.panelData: Map<Integer, List<ProgramCardDto>>` 로 보임) — `applyPeriod`, `thumbText` 추가 지점이 공용 DTO 가 됨 | Q3 에서 "admin 전용 DTO 분기" vs "공용 DTO 에 getter 추가" 선택. 공용 DTO 에 getter 만 추가하면 user 쪽은 호출 안 하므로 비파괴. 전용 DTO 분기는 명세 cleanliness 상승 but LOC 증가 |
| `applyPeriod` 포매팅이 user `dateLabel` 포매터와 다르면 포매터 중복 발생 | Q4 결정. prototype L1141 vs L1145 둘 다 `font-family:Inter` 로 동일 포맷 ("YYYY-MM-DD ~ YYYY-MM-DD" or "YYYY-MM-DD~MM-DD") → `ProgramCardDto.dateLabel` 산출 로직 재사용 가능 |
| `thumbText` 산출 규칙 (제목 첫 글자? 첫 2글자? 영문 이니셜?) 결정 필요 | Q1 세부. 가장 안전한 선택은 "제목 첫 1 글자" (한글 1 글자). prototype 은 표기 글자 미지정 |
| "신청 현황" 숫자가 primary bold 인데 Dday chip 과 중복 강조 느낌 | Q5 결정. prototype 은 chip 자체가 없고 신청 현황 숫자만 primary bold. 현행 `calendarChipLabel/Kind` 는 폐기 또는 다른 자리로 이동 |
| CalendarCell 의 `pill` 자체는 변경 없음 — 셀 UI 는 prototype 과 매칭됨 | 범위 밖 |

---

## 6. 비용 estimate

| 항목 | LOC |
|---|---|
| `_calendar-fragment.html` 패널 카드 재작성 | +40 / -30 |
| DTO `applyPeriod` / `thumbText` getter | +15 (공용 DTO 확장) 또는 +60 (전용 DTO 신설) |
| `AdminProgramCalendarService` 세팅 로직 | +5 ~ +15 |
| CSS 신설 (썸네일·info·버튼) | +40 |
| CSS 삭제 (bar / chip / meta / thumb 기존) | -25 |
| `admin-programs-calendar.ts` 계약 전면 갱신 | +50 / -15 |
| `programs-list-calendar.md` 섹션 갱신 | +15 |
| `AdminProgramCalendarRenderTest` 신설·보강 | +40 |
| `admin-program-calendar.spec.ts` 인터랙션 1건 | +15 |
| **합계 (공용 DTO 확장안)** | **≈ +220 / -70 (net ≈ +150 LOC)** |
| **합계 (전용 DTO 분기안)** | **≈ +280 / -70 (net ≈ +210 LOC)** |

Q3 결정에 따라 ±60 LOC 변동. 사용자 범위 estimate 80~120 보다는 큼 (DTO·계약·테스트 전면 갱신 합산).

---

## 7. Q1~Q6 결정 질문 (사용자 확정 필요)

### Q1. 썸네일 구현 방식

prototype L1130 은 `{{ progCalPanel.thumbText }}` — 보라 bg 위 32px 보라 텍스트 (즉 **텍스트 썸네일 전용**). 그러나 실 운영에서 프로그램 이미지가 있을 수 있고, 현행 구현도 `card.imageUrl` 이미지 우선 로직을 가지고 있음.

- (a) **이미지 우선 + 텍스트 fallback** — 이미지 있으면 100% × 100px 로 노출, 없으면 보라 bg + 텍스트. 실무 안전
- (b) **텍스트 전용** — prototype 그대로. 이미지 무시
- (c) **이미지 전용 + placeholder** — 이미지 없으면 "IMG" placeholder (현행)

**추천**: (a). prototype 디자인 의도(보라 통일 썸네일) 는 "텍스트 썸네일 전용" 이지만 실 데이터에 이미지가 있는 프로그램을 무시하면 운영 피드백 이슈 가능. (a) 가 가장 안전.

### Q2. "상세보기" 버튼 vs 셀 전체 클릭

prototype L1134 (제목 클릭) + L1152 (버튼 클릭) 둘 다 `progCalGoDetail`. 즉 **버튼 명시 + 제목도 링크**. 현행 구현은 카드 전체(`<a href>`)가 상세 링크.

- (a) **버튼 명시 + 제목만 추가 링크** — prototype 그대로. 센터명·기간 라인은 비링크
- (b) **버튼 명시 + 카드 전체도 링크 유지** — a11y 측면에서 중첩 anchor 위험 (허용 안 됨). 카드 wrapper 는 `<article>` 로 변경

**추천**: (a). HTML spec 상 anchor 중첩 불가 (현행 `<a>` wrapper 는 그대로 두면 안의 `<a>` 와 충돌). 카드 wrapper 는 `<article>` 로 변경하고 제목·버튼만 `<a>` 로.

### Q3. DTO 응답 모델

- (a) **공용 `ProgramCardDto` 에 `applyPeriod`, `thumbText` getter 추가** — user 쪽은 호출 안 하므로 비파괴. LOC 작음
- (b) **`AdminProgramCalendarPanelCardDto` 전용 분기** — admin 전용 필드 격리. clean 하지만 매퍼 1개 추가 (`ProgramCardDto → AdminProgramCalendarPanelCardDto`)
- (c) **Program 엔티티에 직접 getter 추가** — `Program.getApplyPeriod()` — 가장 가볍지만 뷰 로직이 엔티티로 흐름

**추천**: (a). ym-impl 이 지점 1곳만 수정하면 되고, DTO 추상화 비용이 LOC 60 더 큼. 미래 user 쪽에서 `applyPeriod` 가 필요해지면 자연스럽게 재사용 가능.

### Q4. "진행 기간" / "신청 기간" 포매팅

prototype L1141: `font-family:Inter, sans-serif`. 포맷 글자는 "YYYY-MM-DD~MM-DD" 또는 "YYYY-MM-DD ~ YYYY-MM-DD" 모두 가능 (prototype 표기 미지정).

현행 `ProgramCardDto.dateLabel` (`ProgramCardDto.java` L80~92): 같은 해면 `yyyy-MM-dd~MM-dd`, 다른 해면 `yyyy-MM-dd~yyyy-MM-dd`.

- (a) **진행기간은 `dateLabel` 재사용, 신청기간은 같은 로직 메서드 추출** — `ProgramCardDto.formatRange(start, end)` 유틸로 리팩터 후 양쪽 호출
- (b) **진행기간은 `dateLabel` 재사용, 신청기간은 신규 포매터 (예: 하이픈 공백 포함 "YYYY-MM-DD ~ YYYY-MM-DD")** — prototype 과 더 가까운 스타일이면 신규
- (c) **둘 다 신규 포매터** — 과거 포맷 호환 안 되지만 디자인 일관성

**추천**: (a). 포매터 1곳으로 통일. prototype 문자 간격은 디자인 디테일이라 CSS `letter-spacing` 으로 흡수 가능.

### Q5. "신청 현황" 색상·형식

prototype L1148: 라벨 "신청 현황" (`#A6A3B3`), 값 `font-size:12px; font-weight:700; color:#3F30E9` ("`8/20명`" 패턴).

- (a) **prototype 그대로** — primary bold, "`{applied}/{capacity}명`"
- (b) **capacity=null 처리** — `capacity` 가 null/0 인 프로그램에선 "제한 없음" 등 다른 라벨. (prototype 은 null 케이스 미지정)
- (c) **마감 상태 추가 노출** — isFull 이면 "`{applied}/{capacity}명 (마감)`" 등

**추천**: (a) 기본 + (b) null 케이스는 "제한 없음" fallback. 운영 실데이터에 capacity null 프로그램이 존재 → fallback 없으면 "null/null명" 렌더 사고.

**연쇄 결정**: 현행 `calendarChipLabel/Kind` ("D-3" / "마감" / "종료" chip) 는 prototype 의 "신청 현황" 숫자 라인과 중복 강조 → **chip 제거** 또는 **다른 자리로 이동 (예: 썸네일 모서리)**. 추천은 chip 제거 (prototype 매칭).

### Q6. FOLLOW 티켓 네이밍·추적 체계

- (a) **`FOLLOW-admin-calendar-panel-card-prototype-match`** (현재) — 설명적
- (b) **`D-admin-calendar-panel-prototype-match`** — D-시리즈 (D-admin-calendar 후속임을 강조)
- (c) **`FOLLOW-D5-Q3-admin`** — D5-Q3 의 admin 분리 사실을 강조

**추천**: (c) 를 계약 라인 인용(`proto: admin/prototype.html L1125`)에 맞춰 변경하거나, (a) 유지 후 본문 선행 티켓에 D5-Q3 명시로 충분. ym-impl 호출 시 파일명 결정.

---

## 8. 작업 큐 메타

- 작업 ID: `FOLLOW-admin-calendar-panel-card-prototype-match` (Q6 결정 시 변경 가능)
- 우선순위: 중 (운영 데이터 노출 정합성 — admin 사용자가 bar 로 신청 현황 파악 중)
- 추정 단위: 1~2 PR (DTO·CSS 분리 시 2 PR)
- 선행: D5-Q3 머지
- 상태: `spec_draft` — Q1~Q6 결정 후 confirmed

---

## 9. 다음 단계

사용자 Q1~Q6 결정 → 상태 `spec_confirmed` 전환 → ym-impl 호출. 예상 2 세션 (DTO·fragment 1 세션, 계약·테스트 1 세션).
