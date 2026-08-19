# F-signup-terms-agreement — 약관 동의 엔티티화 + 동의 이력 저장

- 상태: `spec_draft`
- 우선순위: 3 (갭 수정 배치 완료 후 착수 — 사용자 결정 2026-07-29)
- 브랜치 후보: `feature/terms-agreement`
- 작성일: 2026-07-29

---

## 1. 배경

### 1-A. 확정된 정책 결정 (2026-07-29)

약관 동의는 **2건 분리 유지**로 확정. wireframe 은 `이용약관 동의` 단일 Checkbox 로 그렸으나(`docs/wireframe-policy/signup.md` #8), 그것은 정책 결정이 아니라 기획 단계에서 약관 구조를 아직 나누지 않은 상태로 판단했다 — 「wireframe vs prototype 판정 규칙」의 **비대칭 원칙 1 (prototype·구현의 세분화를 단순화로 되돌리지 않는다)** 적용. 근거:

1. `/terms`·`/privacy` 두 페이지가 이미 분리 존재 (`PolicyController:14,19` + `templates/policy/{terms,privacy}.html`) → 동의 구조와 문서 구조가 일치
2. 한국에서는 서비스 이용약관 동의와 개인정보 수집·이용 동의를 분리해 받는 것이 일반 관행 (법적 판단은 법무 검토 영역)
3. 2건 → 1건 통합은 동의 범위를 넓히는 방향이라 되돌리기 비용이 더 크다

> **`wireframe-policy/signup.md` #6 오기 정정 필요**: "prototype: 4개 세분화 (전체+3개별)" 로 기재됐으나 실제는 **전체 동의 1 + 개별 2 = 체크박스 3개**이고 개별 항목은 `회원가입약관`·`개인정보처리방침 안내` 둘뿐이다 (`prototype.tsx` L1964). 선택 항목은 없다. 쟁점은 "1개 vs 4개" 가 아니라 **필수 동의를 1건으로 묶을지 2건으로 분리할지** 였다.

### 1-B. 실제 공백 — 동의가 저장되지 않는다

`termsAgreed`·`privacyAgreed` 는 **`SignUpRequest` DTO 에만 존재**하고 `@AssertTrue`(`SignUpRequest:105~107`) 로 검증한 뒤 버려진다.

- `User` 엔티티에 동의 관련 필드 **0개** (필드 전수: id·email·password·name·phone·zipcode·address·addressDetail·birthDate·gender·role·center·centerScope)
- `users` 테이블에 동의 관련 컬럼 **0개** (`V1__baseline.sql`)

즉 "이 사용자가 약관에 동의했다" 는 사실이 시스템에 남지 않는다. 약관 동의는 분쟁·감사 시 **누가·언제·어떤 버전에** 동의했는지 증명해야 하는 성격의 데이터라 검증만 하고 버리는 것은 실질적 결함이다.

### 1-C. 왜 boolean 컬럼 2개가 아니라 엔티티인가

사용자가 **"약관 개수가 늘어날 수 있다"** 고 명시했다 (2026-07-29). boolean 컬럼 방식은 약관이 늘 때마다 컬럼 추가 + 마이그레이션 + 검증식 수정 + 폼 수정이 반복된다. `CLAUDE.md` 「확장성 원칙 → 하드코딩 금지 대상」 정면 위반이며, admin 트랙에서 약관 CRUD 를 붙일 수 없다.

**기존 동의 데이터가 없으므로 지금이 마이그레이션 부담이 가장 낮은 시점이다.**

---

## 2. 디자인 출처

| 자산 | 위치 | 내용 |
|---|---|---|
| prototype.tsx | SignupScreen L1955~1973 | 카드 제목 `이용약관 동의` / `약관 전체 동의` 마스터 토글(22×22, radius 6, 체크 시 `primaryBg` 배경) / 개별 2행 (18×18 SVG 체크, 라벨 14 `textSec` + 적색 `(필수)`, 우측 `약관보기 ›` 12.5 `textTri`) |
| prototype.tsx | L1860 | `if(!allAgreed){ alert('필수 약관에 동의해주세요.'); return; }` — 제출 차단 |
| prototype.tsx | L1958 | `allAgreed` 는 **파생값** (`terms && privacy`), 별도 상태 아님 |
| wireframe.png | 섹션 2 (회원가입) | Checkbox 1개, 필수, 미체크 시 헬프 텍스트 `필수 항목입니다.` → `docs/wireframe-policy/signup.md` #8 |
| 현재 구현 | `templates/user/signup.html` L243~272 (마크업), L376~389 (JS) | prototype 과 **형태 정합**. 양방향 동기화 구현됨 |

### 2-A. 형태 축은 이미 정합 — 이번 스코프 아님

`전체 동의` 는 이미 구현되어 있고 양방향 동기화까지 된다 (`signup.html:381` `syncAll()` = `all.checked = t.checked && p.checked`, 초기 렌더 1회 호출로 검증 실패 후 상태 복원). `th:field` 가 checkbox id 를 `termsAgreed1` 로 바꾸는 함정도 `name` 셀렉터로 회피됨.

**본 스펙은 백엔드 데이터 모델 스펙이다.** UI 는 "약관 목록을 동적 렌더" 로 바꾸는 것 외에 시각 변경 없음. 계약 검사(`e2e/contracts/`) 에 영향 없어야 한다.

---

## 3. 데이터 모델

### 3-A. gap 표

| 필요 정보 | 현재 | 조치 |
|---|---|---|
| 약관 정의 (제목·필수여부·순서) | 템플릿에 하드코딩 | **`Term` 엔티티 신설** |
| 약관 본문 | `templates/policy/*.html` 정적 파일 | `Term.contentPath` 로 경로 참조 (Q2) |
| 동의 여부 | DTO 검증 후 폐기 | **`UserAgreement` 엔티티 신설** |
| 동의 시각 | 없음 | `UserAgreement.agreedAt` |
| 동의한 약관 버전 | 없음 | `UserAgreement.agreedVersion` |

### 3-B. `Term` (약관 정의)

`CLAUDE.md` 「엔티티 작성 규칙」 준수: `Long` PK + `IDENTITY`, `@Getter` + `@NoArgsConstructor(PROTECTED)` + `@Builder` private, `@Setter` 금지, enum 은 `@Enumerated(STRING)`.

| 필드 | 타입 | 비고 |
|---|---|---|
| `id` | `Long` | IDENTITY |
| `code` | `String` | **UNIQUE**. `SERVICE` / `PRIVACY` / (향후 `MARKETING` 등). 코드로 조회하므로 enum 아님 — admin 이 추가 가능해야 함 |
| `title` | `String` | 폼 라벨 (예: `회원가입약관`) |
| `contentPath` | `String` | 약관 보기 경로 (예: `/terms`) |
| `required` | `boolean` | 필수 동의 여부 |
| `version` | `int` | 개정 시 증가 |
| `sortOrder` | `int` | 폼 노출 순서 |
| `isActive` | `boolean` | 비활성 약관은 폼에서 제외 (과거 동의 이력은 보존) |
| `effectiveFrom` | `LocalDate` | 시행일 (Q3) |

`BaseTimeEntity` 상속.

### 3-C. `UserAgreement` (동의 이력)

| 필드 | 타입 | 비고 |
|---|---|---|
| `id` | `Long` | IDENTITY |
| `user` | `User` | **단방향** `@ManyToOne(LAZY)` |
| `term` | `Term` | **단방향** `@ManyToOne(LAZY)` |
| `agreedVersion` | `int` | 동의 당시 `Term.version` **스냅샷** — 약관 개정 후 재동의 필요 판정용 |
| `agreed` | `boolean` | 선택 약관 철회 대비 (필수 약관은 항상 true) |
| `agreedAt` | `LocalDateTime` | |

- **양방향 컬렉션 없음** (`CLAUDE.md` 규칙). `User` 에 `@OneToMany` 추가 금지 → `UserAgreementRepository.findByUser(user)` 로 조회
- 이력이므로 **UPDATE 하지 않고 INSERT 만** 한다. 철회·재동의는 새 행. `(user, term)` UNIQUE 제약을 **걸지 않는다**

### 3-D. 마이그레이션

`V3__create_term_and_user_agreement.sql` (main 최신이 `V2__add_notification_items.sql` → 다음 번호 3. 병렬 브랜치가 V3 를 선점했으면 rebase 후 재부여)

- `terms` 테이블 — `term` 은 PostgreSQL 예약어가 **아니지만**, `users`(예약어 `user` 회피로 복수형) 선례에 맞춰 **복수형 `terms` 로 통일**한다. 예약어 회피가 아니라 명명 일관성 목적
- `user_agreements` 테이블 + FK 2개 + `(user_id, term_id)` 조회 인덱스
- **한 번 머지된 V 파일은 수정 금지** (`CLAUDE.md` DB 규칙)

### 3-E. 시드

`DataInitializer` 에 멱등 체크 후 2건:

| code | title | contentPath | required | version | sortOrder |
|---|---|---|---|---|---|
| `SERVICE` | 회원가입약관 | `/terms` | true | 1 | 1 |
| `PRIVACY` | 개인정보처리방침 안내 | `/privacy` | true | 2 | 2 |

`CLAUDE.md` 「파생 시드 금지」 준수 — 각 행이 진리 소스. 2건이므로 자원 파일 분리는 불필요 (≤10건 기준).

---

## 4. 변경 범위

- [ ] `user/Term.java` · `TermRepository.java` **신설** — `findByIsActiveTrueOrderBySortOrder()`, `findByCode()`
- [ ] `user/UserAgreement.java` · `UserAgreementRepository.java` **신설** — `findByUser(User)`
- [ ] `db/migration/V3__create_term_and_user_agreement.sql` **신설**
- [ ] `common/DataInitializer.java` — 약관 2건 멱등 시드
- [ ] `user/SignUpRequest.java` — `termsAgreed`/`privacyAgreed` boolean 2개 → `Map<String, Boolean> agreements` 또는 `List<Long> agreedTermIds` (Q1). `@AssertTrue` 하드코딩 제거
- [ ] `user/UserService.java` — `signUp()` 에서 필수 약관 전건 동의 검증 + `UserAgreement` INSERT (같은 트랜잭션)
- [ ] `user/UserController.java` — GET `/signup` 모델에 활성 약관 목록 주입
- [ ] `templates/user/signup.html` — 약관 2행 하드코딩 → `th:each` 동적 렌더. **전체 동의 JS 는 개별 체크박스를 name 하드코딩 대신 공통 셀렉터로 순회하도록 수정**

### 4-A. 폼 검증 일반화

현행 (`SignUpRequest:105~107`):

```java
@AssertTrue(message = "이용약관과 개인정보처리방침에 모두 동의해주세요.", groups = FormatCheck.class)
private boolean isAgreementsValid() { return termsAgreed && privacyAgreed; }
```

DTO 는 활성 약관 목록을 모르므로 **서비스 계층으로 이동**한다. `@AssertTrue` 로는 "필수 약관 전건" 을 표현할 수 없다 (약관 개수가 가변).

- 검증 그룹 체계(`RequiredCheck` → `FormatCheck`, `@GroupSequence` `OrderedChecks`) 는 유지
- 서비스에서 위반 시 `BindingResult` 에 반영되도록 컨트롤러에서 예외 → `rejectValue` 매핑. 메시지 어조는 `CLAUDE.md` 「메시지 어조 통일 규칙」 준수

---

## 5. 검증 시나리오

### 정적 검증

- `compileJava` 통과
- `JpaMappingTest` — `Term`·`UserAgreement` 매핑 + `ddl-auto: validate` 통과 (V3 와 엔티티 일치)
- `TermRepositoryTest` — `findByIsActiveTrueOrderBySortOrder()` 정렬·필터
- **`UserServiceTest` (정책 강제 — 필수)**
  - 필수 약관 전건 동의 시 가입 성공 + `UserAgreement` 2행 생성
  - 필수 약관 1건 누락 시 가입 실패
  - `agreedVersion` 이 `Term.version` 스냅샷으로 저장됨
  - 비활성(`isActive=false`) 약관은 검증 대상에서 제외
- `SignupRenderTest` — 동적 렌더 후에도 `약관 전체 동의`·`(필수)`·`약관보기 ›` 마크업 존재

> **정책은 계약 검사로 잡히지 않는다.** 「wireframe vs prototype 판정 규칙」의 강제 수단 표에 따라 위 서비스 테스트를 **같은 커밋에** 넣는다. 테스트 없이 정책만 코드에 넣으면 UX 축에서 이미 겪은 "산문 규칙은 지켜지지 않는다" 함정이 재현된다.

### 동적 검증

- `GET /signup` 200 + 약관 2행 렌더 (`curl | grep`)
- 미동의 제출 → 폼 재표시 + 입력값 보존 + 에러 메시지
- 동의 제출 → 가입 성공 후 `user_agreements` 2행 확인 (write→read 왕복)

### 계약 회귀 (필수)

- `npx playwright test --project=contracts` — **signup 계약은 아직 없으나** 공통(헤더·푸터) 갭 0 유지 확인
- `npx playwright test --project=chromium` — signup 기능 E2E green 유지
- ⚠️ **E2E 스펙 갱신이 거의 확실히 필요하다.** `e2e/tests/signup.spec.ts` **L115~116** 과 **L168~169** 가 `input[name="termsAgreed"]` / `input[name="privacyAgreed"]` 를 직접 참조한다. 동적 렌더로 바꾸면서 name 이 `agreements[SERVICE]` 형태로 바뀌면 4곳이 깨진다
  - 대응 (택 1): ① 동적 렌더 시에도 name 을 `termsAgreed`/`privacyAgreed` 로 유지 (code → name 매핑 테이블 필요, 확장성 훼손) ② **E2E 를 `[data-term-code="SERVICE"]` 같은 안정 셀렉터로 갱신 (권장)**
  - 2026-07-13 회고: E2E red 방치가 신규 회귀 5건을 가린 사고가 있었다. **같은 PR 에서 E2E 를 함께 고친다**

---

## 6. 의존성 / 선행

- **갭 수정 배치 완료 후 착수** (사용자 결정 2026-07-29). 배치가 `signup.html`·`main.css` 를 만지지 않으므로 충돌 위험은 낮으나 순서 유지
- Flyway 활성화 완료 (`chore-flyway-activation` `impl_done`) — V 파일 경로 사용 가능
- admin 약관 CRUD 는 **본 스펙 스코프 외**. `Term` 엔티티가 그 선행 조건이 된다 (`ADMIN-00-master-directive` 연계)

---

## 7. 사용자 결정 필요

| # | 항목 | 선택지 | 논점 |
|---|---|---|---|
| **Q1** | 폼 바인딩 형태 | (a) `Map<String,Boolean> agreements` (code 키) / (b) `List<Long> agreedTermIds` / (c) boolean 2개 유지 + 이력만 저장 | (a) 는 code 안정성 의존, (b) 는 id 노출, (c) 는 확장성 포기. **(a) 권장** — code 는 admin 이 바꾸지 않는 식별자 |
| **Q2** | 약관 본문 저장 | (a) `contentPath` 로 정적 템플릿 참조 (현행 `/terms`·`/privacy` 유지) / (b) `Term.content` 컬럼에 본문 저장 | (b) 는 admin 편집 가능하나 HTML 저장·XSS 처리 필요. **(a) 권장** — 지금은 경로 참조, admin 트랙에서 (b) 전환 검토 |
| **Q3** | `effectiveFrom` 도입 | (a) 지금 넣는다 / (b) 재동의 요구가 실제 생길 때 추가 | **(b) 권장** — 미사용 컬럼 방지. `version` 만으로 재동의 판정 가능 |
| **Q4** | 기존 사용자 동의 이력 | (a) 백필하지 않음 (이력 없음으로 남김) / (b) 시드 유저에 한해 백필 | 실사용자 데이터 없으므로 **(a) 권장** |
| **Q5** | 선택 약관 대비 | (a) `required=false` 지원만 넣고 UI 는 `(선택)` 라벨까지 준비 / (b) 필수만 지원, 선택 나오면 그때 | prototype 에 선택 항목이 없어 형태 선례 없음. **(a) 권장** — 스키마는 이미 지원하므로 라벨 분기만 추가 |

---

## 8. 작업 큐 메타

- 작업 ID: `F-signup-terms-agreement`
- 추정 단위: 1 PR (엔티티 2 + V3 + 시드 + 폼 + 테스트)
- 상태: `spec_draft` → Q1~Q5 결정 후 `spec_confirmed`
- 관련: `docs/wireframe-policy/signup.md` #6·#8, `docs/specs/ADMIN-00-master-directive.md`, `~/.claude/agents/ym-spec.md` 「자산 간 갭 발견 시 의사결정 흐름」
