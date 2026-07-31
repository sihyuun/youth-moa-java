# 작업 명세: F4-detail-requirements-grid — 프로그램 상세 자격요건 grid 데이터 연동 (entity 확장)

> 산출: ym-spec, 2026-07-07. 상태: **`impl_done` — PR #73 (`194605f` 260707_F4_requirements_data)**
> 구현 위치: `program/ProgramEligibility.java` (@Embeddable), `program/Program.java` (@Embedded eligibility), `templates/program/detail.html` 바인딩 완료 (Q3-B 기본 문구 포함)

## ✅ 결정 확정 (2026-07-07, 전부 권장안 채택)

| # | 결정 |
|---|---|
| Q1 | **3카드 확정** — "4-grid" 는 2×2 레이아웃 표기였음. 소득기준 등 4번째 항목 없음 |
| Q2 | **A안. `requirements` (@Lob) 제거 후 `eligibility.{age, region, etc}` 로 재편** — 기존 시드 문구는 연령+거주지로 분해 이관 |
| Q3 | **B안. null 필드는 기본 문구 표시** (연령 "제한 없음" 등) — 항상 3카드 유지 |
| Q4 | **`@Embeddable ProgramEligibility` + `@Embedded`** — 컬럼명 `eligibility_age` 등 |
> 사전 조사 핵심: UI 는 **PR #12 (`93910ae`) 에서 이미 구현 완료** (지원 대상 3-grid 카드 + 주의 문구 + CSS). 잔여 갭은 **데이터가 템플릿에 하드코딩**된 것 하나 — 연령·거주지 카드 값이 `detail.html` 리터럴, 기타 카드만 `program.requirements` 사용.
> wireframe.png 에는 자격요건 섹션이 **없음** (HANDOFF §5-E.8 에서 의도적으로 추가된 개선 — 충돌 아님). prototype/HANDOFF 모두 **3필드** — 큐의 "4-grid" 표기는 자산 근거 없음 (Q1).

## 1. 디자인 출처 (3자산 모두 명시)

- **wireframe.png**: 프로그램 상세 (x≈25,300~26,200 / y≈9,150~10,000). 정보 테이블 6행 + 설명만 존재. **자격요건 섹션 없음**
- **prototype.html**: `ProgramDetail` line 940~, "지원 대상" 섹션 **line 1026~1047** (h3 + `repeat(2,1fr)` grid 카드 3개 + warningLight 주의 박스)
- **prototype.tsx**: line 992~1013 (html 과 완전 동일)
- **HANDOFF.md**: **§5-E.8 (line 683~687)** — "지원 대상·자격요건 섹션 (2열 카드), 필드: 연령/거주지/기타, **소득기준 제외**, admin 등록 필드 필요, `Program.eligibility = { age, region, etc }`" / §5.3 (line 370~382)
- **비교 대상**: `templates/program/detail.html` line 165~216 (섹션 기구현), `static/css/main.css` line 1133~1171 (스타일 기구현)

## 2. 변경 범위 (파일 단위)

- [ ] `program/Program.java` — 자격요건 필드 확장 (연령·거주지·기타). 기존 `requirements` (@Lob) 재편 여부는 **Q2**
- [ ] `common/DataInitializer.java` — 시드 8건에 자격요건 3필드 값 부여
- [ ] `templates/program/detail.html` — line 179 (연령), 192 (거주지) 하드코딩 → `th:text` 바인딩, line 205 (기타) 바인딩 정리, null 처리 (**Q3**)
- [ ] JPA 매핑 테스트 — 신규 컬럼 저장·조회 round-trip TC
- `static/css/main.css` — **변경 없음** (스타일 기구현, prototype 토큰 일치 확인 완료)
- `ProgramController` / `ProgramService` — **변경 없음**

## 3. 필드 명세 (HANDOFF §5-E.8 `eligibility = {age, region, etc}`)

| 필드 | 컬럼 제안 | 타입 | 필수 | 비고 |
|---|---|---|---|---|
| 자격요건-연령 (eligibilityAge) | `@Column(length = 100)` | String | nullable | 예: "만 19세 ~ 39세 청년". **@Lob 금지** — open-in-view:false 의 PG LOB streaming 사고 회피 |
| 자격요건-거주지 (eligibilityRegion) | `@Column(length = 100)` | String | nullable | 예: "경기도 거주 또는 활동 중인 청년" |
| 자격요건-기타 (eligibilityEtc) | `@Column(length = 200)` | String | nullable | 예: "전 회차 참석 가능자 우대". 기존 `requirements` 관계는 Q2 |

구조 선택지 (학습 관점):

| 방식 | 장점 | 단점 |
|---|---|---|
| A. 평문 3컬럼 | 단순, 기존 규칙 동일 | Program 필드 수 증가 |
| B. `@Embeddable ProgramEligibility` + `@Embedded` | 응집·학습 가치 (신규 JPA 개념), HANDOFF 모델링 정합 | builder/update 시그니처 변경 폭 약간 큼 |

권장: **B (@Embeddable)** — 컬럼명 `eligibility_age` 등으로 생성.

### 화면 (기구현 — 바인딩만 교체)
| 카드 | 아이콘 | 라벨 (하드코딩 OK) | 값 (DB 바인딩) |
|---|---|---|---|
| 1 | user | 연령 | `${program.eligibility.age}` |
| 2 | pin | 거주지 | `${program.eligibility.region}` |
| 3 | calendar | 기타 | `${program.eligibility.etc}` |

### 확장성 원칙 판단
| 항목 | 판단 | 근거 |
|---|---|---|
| 자격요건 3필드 값 | **하드코딩 금지** → 엔티티 + 시드 | HANDOFF §5-E.8 "admin 등록/수정 화면 입력 필드 추가" 명시 |
| 카드 라벨·아이콘 | 하드코딩 OK | 필드 스키마 자체 (admin 변경 시나리오 없음) |
| 주의 문구 | 하드코딩 OK | UX 라이팅 |
| 별도 `Eligibility` 엔티티 (1:N) | 불채택 | 필드 3종 고정 — 과설계 회피 |

## 4. 갭 리스트

| # | 항목 | 현재 | 명세 | 우선순위 |
|---|---|---|---|---|
| 1 | 지원 대상 섹션 UI | PR #12 구현 완료 (prototype 일치) | 동일 | 없음 (완료) |
| 2 | 연령 카드 값 | line 179 "만 19세 ~ 39세 청년" **리터럴** | `eligibility.age` | **높음 (핵심)** |
| 3 | 거주지 카드 값 | line 192 "경기도 거주 또는…" **리터럴** | `eligibility.region` | **높음** |
| 4 | 기타 카드 값 | line 205 `program.requirements` (@Lob) — 시드가 전체 요약 문구라 연령·거주지와 의미 중복 | `eligibility.etc` (부가 조건) | **높음** (Q2) |
| 5 | 시드 데이터 | 8건 `requirements` 단일 문구 | 프로그램별 age/region/etc | 높음 |
| 6 | admin 등록 화면 입력 | 미구현 (admin 미착수) | HANDOFF "admin 추가 필요" | 낮음 — admin 트랙 후속, 범위 외 |

## 5. 검증 시나리오 (ym-qa)

### 정적
- compileJava + JPA 매핑 테스트 (신규 필드 round-trip)
- 실 렌더 통합 테스트 (PR #56) 에 상세 페이지 포함 시 재실행 — 평문 컬럼이라 LOB 사고 재발 없음 확인

### 동적 (curl / preview)
- `GET /programs/{시드 id}` 200
- `.detail-requirement-value` 3개가 **시드 값과 일치** — 두 개 이상 id 교차 확인 (모든 상세에서 "만 19세 ~ 39세" 동일 출력이면 실패)
- `${...}` / `th:*` 잔존 없음
- null 필드 프로그램 (시드 1건 의도적 null 권장) 500 없이 Q3 결정대로 렌더

### 시각 (사용자 영역)
1. 카드 3장 2열 grid, 프로그램별 상이한 값
2. 주의 문구 박스 유지
3. null 필드 프로그램 레이아웃 붕괴 없음

## 6. 의존성
- 선행 없음 (main `60723d8` 기준). admin 트랙의 프로그램 등록 폼이 본 스키마를 후속으로 사용
- `ddl-auto: create-drop` 단계라 컬럼 재편 비용 없음 (Q2 재편안에 유리한 시점)

## 7. 작업 큐 메타
- 작업 ID: F4-detail-requirements-grid / 추정: 1 PR (entity+시드+바인딩+테스트) / 상태: spec_done

## 사용자 결정 필요 질문

- **Q1. "4-grid" 표기 진의 — 카드 3장 확정?** prototype·HANDOFF 모두 3필드 (소득기준 명시 제외). "4-grid" 는 2x2 레이아웃 (3칸 채움) 표기로 추정. 3카드 확정 vs 4번째 항목 (소득기준·우대사항 분리) 추가 의도?
- **Q2. 기존 `requirements` (@Lob) 처리** — A안 (권장): 제거 후 `eligibility.{age,region,etc}` 로 재편 (시드 문구 분해 이관, @Lob 청산, 의미 중복 해소) / B안: `requirements` 를 기타 카드용 유지 + age/region 만 추가
- **Q3. null 자격요건 렌더링** — A안: 카드 숨김 (`th:if`) / B안 (권장): 기본 문구 "제한 없음" 표시 — 항상 3카드 유지
- **Q4. 엔티티 구조** — 평문 3컬럼 vs **@Embeddable ProgramEligibility (권장)**
