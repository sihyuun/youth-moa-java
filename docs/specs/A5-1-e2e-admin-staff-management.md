# 작업 명세: A5-1-e2e — admin staff management 기능 E2E 보강 + 디자인 계약 신설

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed (Part 1)`** (Q1·Q3·Q6 결정 확정 2026-10-16 — Part 1 ym-impl 착수. Part 2 는 Q4 결정 대기 → `spec_draft` 유지) |
| 브랜치 (예정) | `feature/A5-1-e2e-admin-staff-management` |
| 선행 | ✅ A5-1 머지 (#219 `15b2dcd`, 2026-09-18) · ✅ A7 SMTP 메일 (#243 `e0e08c7`) · ✅ A7 mail followup force-fail flag (#244 `eb05337`) · ✅ 기능 E2E 1차 (#224 `6f39ab8` — `admin-staff-management.spec.ts` 7 active + 1 skip) |
| 관련 spec | `A5-1-admin-staff-management.md` (A5-1 본체) · `A7-admin-header-live.md` (SMTP) |
| prototype 재인용 | `admin/prototype.html` L1946~2045 (신규 사용자 등록 폼 · 옵션 B 편입) · L1774~1943 (사용자 상세 · 발급 후 랜딩) |
| 예상 규모 | **소~중** — 신규 Java 파일 0~1 · 신규 TS 계약 1종 (`admin-user-new.ts`) + 기존 1종 확장 (`admin-user-detail.ts` 발급 후속 셀렉터) · 신규 디자인 계약 md 1종 + 기존 1종 보강 · 기존 spec (`admin-staff-management.spec.ts`) 재가동 + 보강 · LOC 순증 400~800 |

---

## 0. 계약 확인 (0단계)

### 계약 유무

| 화면 | 디자인 계약 (md) | 계약 셀렉터 (ts) | 상태 |
|---|---|---|---|
| `/admin/users` 목록 | ✅ `admin/users.md` | ✅ `admin-users.ts` | 완전 |
| `/admin/users/{id}` 상세 | ✅ `admin/user-detail.md` | ✅ `admin-user-detail.ts` | **갭 — A5-1 발급 flash 섹션 (초기 password 카드 · 메일 성공 배너 · 임시 PW 재발급 CTA · 에러 카드) 미커버. A5 시점 작성이라 A5-1 셀렉터는 미반영** |
| `/admin/users/new` 신규 발급 폼 | ❌ 없음 | ❌ 없음 | **이번 티켓 신설 필수** |
| `/password/change` 강제 변경 | ❌ 없음 | ❌ 없음 | **이번 티켓 신설 범위 Q로 결정** |
| `/admin/users/bulk/*` | ✅ `admin/user-bulk.md` | ✅ `admin-user-bulk.ts` | 완전 (별건) |

### 공통 정책 적용

`docs/design-contracts/admin/README.md` + `POLICY.md` 승계. 다크 헤더 · 인디고 primary (#3F30E9) · 존댓말 · SVG 아이콘 유지.

---

## 1. 디자인 출처 (3자산 재정독)

### prototype 인용

| 자산 | 라인 | 내용 | A5-1-e2e 반영 |
|---|---|---|---|
| `admin/prototype.html` | L1946~2045 | 신규 사용자 등록 폼 — email · 중복확인 · password · confirm · name · gender · role radio 3종 · birth · phone · address | **옵션 B 편입 결과 반영본 (`templates/admin/user/new.html`) 과 1:1 매칭 — 계약 셀렉터 커버** |
| L1996~2005 | 권한 radio (사용자 · 관리자) 2옵션 | `admin-user-new-account-type-user|admin` data-testid 기 적용 |
| L2000~2010 (관리자 선택 시) | 관리자 종류 sub-radio (CENTER_ADMIN · SYSTEM_ADMIN) | `admin-user-new-role-center|system` data-testid 기 적용 |
| L2010~2020 (CENTER_ADMIN 시) | 소속 센터 select | `admin-user-new-center` data-testid 기 적용 |
| L1774~1943 | 사용자 상세 (A5 기 반영) | **A5-1 발급 완료 후 랜딩 지점**. flash 카드 (초기 password · 메일 배너) 는 prototype 미명시 → **deviation 재확인 · 계약에 명시** |
| L2130~ (추정) | 사용자 상세 Danger zone | A5 시점 반영 |

### 구현 상태 재검증 (2026-10-15 소스 실측)

| 요소 | 소스 라인 | 상태 |
|---|---|---|
| `GET /admin/users/new` | `AdminUserController.java` L95~100 | ✅ |
| `POST /admin/users` | L103~141 | ✅ 자동 password 생성 · 메일 발송 · fallback 분기 완비 |
| `POST /admin/users/{uid}/reset-password` | L144~162 | ✅ |
| `PasswordChangeRequiredInterceptor` | `user/PasswordChangeRequiredInterceptor.java` | ✅ `/password/change` 로 redirect |
| `PasswordChangeController` GET/POST | `user/PasswordChangeController.java` | ✅ 이전 PW 검증 유지 (Qn-11 정정 B안 반영) |
| `SecureRandomPasswordGenerator` | 12자 · 4 클래스 · SecureRandom | ✅ |
| `AdminUserSafeguard.assertCanCreateStaff` / `assertCanResetPassword` | A5-1 섹션 L84~102 | ✅ |
| `AdminInvitationMailService` 2구현체 (`SmtpAdminInvitationMailSender` + `MockAdminInvitationMailSender`) | A7 반영 | ✅ `maskPassword` + `forceFail` flag 완비 |
| `POST /__test__/mail/force-fail?enabled=` | `test/TestFixtureController.java` | ✅ A7 mail followup 신설 |
| `templates/admin/user/new.html` | 전체 | ✅ 모든 테스트용 data-testid 기 부착 |
| `templates/admin/user/detail.html` | L50 · L81 · L92 · L198 | ✅ `admin-user-mail-sent` · `admin-user-initial-password` · `admin-user-initial-password-value` · `admin-user-reset-password-btn` data-testid 기 부착 |
| `templates/user/password-change.html` | 전체 | ✅ `password-change-force-notice` · `-current` · `-new` · `-confirm` · `-submit` data-testid 부착 |

### 기존 E2E 상태 (`admin-staff-management.spec.ts` 237줄 2026-10-15 기준)

| # | 테스트 | 상태 | 비고 |
|---|---|---|---|
| 1 | 신규 발급 폼 렌더 — 폼·핵심 필드 노출 | ✅ active | 계약 셀렉터로 재구성 가능 |
| 2 | 동적 섹션 — ADMIN 선택 시 관리자 종류·센터 섹션 순차 노출 | ✅ active | 좋음 |
| 3 | USER 계정 발급 — redirect + 메일 발송 배너 (password 카드 비노출) | ✅ active | Q3=A 보안 우선 반영 |
| 4 | 이메일 중복 발급 — 에러 메시지 노출 | ✅ active | 좋음 |
| 5 | 임시 비밀번호 재발급 — 메일 재발송 배너 노출 | ✅ active | 좋음 |
| 6 | **강제 비밀번호 변경 왕복** (발급→로그인→redirect→변경→재로그인→정상) | ⚠️ **skip** | mock force-fail flag 가 생겼으므로 **재활성화 가능**. 단 skip 사유: "메일 성공 시 password 는 메일에만 담김" → **force-fail ON → fallback 화면에서 tempPw 추출 → 재로그인** 패턴으로 되살림. 또는 임시로 `MockAdminInvitationMailSender.getLastPassword()` getter 추가 |
| 7 | USER 발급 · 메일 실패 시 password 카드 fallback 노출 | ✅ active | force-fail flag 활용 |
| 8 | RBAC — CENTER_ADMIN 403 | ✅ active | 좋음 |

### 1-A. 자산 간 갭 (형태 vs 존재 축)

A5-1 본체 spec 이 이미 커버. **A5-1-e2e 추가 갭**:

| 항목 | 축 | 채택 |
|---|---|---|
| `/admin/users/new` 디자인 계약 (md + ts) | 형태 | **신설** |
| `/admin/users/{id}` A5-1 발급 flash 섹션 (초기 password 카드 · 메일 배너 · 재발급 CTA · 에러 카드) 계약 셀렉터 | 형태 | **기존 `admin-user-detail.ts` 확장** |
| `/password/change` 디자인 계약 | 형태 | **Q4 결정** — spec 범위 포함 vs 별건 |
| 강제 PW 변경 왕복 E2E (현재 skip) | 형태 | **재활성화 Q1** |
| 자율 PW 변경 flow (flag=FALSE 유저가 `/password/change` 로 자발 진입) | 존재 | **Q5 — 메뉴 노출 포함 여부 (후속 티켓 분리 권장)** |
| 메일 성공 배너 · fallback 카드 모델 간 정합 테스트 (write→read 왕복) | 형태 | **커버 중** — 유지 |

### 1-B. 데이터 모델 gap 표 (필수)

**신규 엔티티·마이그레이션 없음** — A5-1 본체(V19) · A7(mail · ApplicationProperties) 완료 상태 그대로 재활용.

| prototype/요구 필드 | 현재 스키마 | 조치 |
|---|---|---|
| `must_change_password` · `password_changed_at` · `invited_by` | V19 (`#219`) 적용 완료 | **무회귀 재확인만** — E2E 왕복에서 flag=TRUE → FALSE 전환 assertion 유지 |
| mail 발송 결과 (성공·실패 사유) | 영속 X (화면 flash + log) | **무변경** — fallback flag 는 세션성 (force-fail toggle) |

### 1-C. 데이터 소비 지점 (write→read 왕복)

| 소비 지점 | 현재 상태 | 왕복 테스트 |
|---|---|---|
| `/admin/users/new` 발급 폼 | ✅ | ✅ TC-3·7 |
| `/admin/users/{id}` 상세 — 메일 성공 배너 · fallback password 카드 · 재발급 CTA · 에러 카드 | ✅ 구현 · ⚠️ 계약 셀렉터 미반영 | ✅ TC-3·5·7 (셀렉터 ad-hoc) → **계약화 후 재bind** |
| `/password/change` 강제 변경 폼 | ✅ 구현 · ❌ TC skip | **Q1: 재활성화** |
| `/password/change` 자율 변경 flow | ✅ 구현 접근 가능 · 메뉴 미연결 | **Q5: 범위 결정** |
| 메일 발송 — Mock 로그 assertion | ❌ 미연결 (현재는 sent/fail 분기만 UI 로 검증) | **Q2: Mock 로그 포집 assertion 추가 여부** |
| 로그인 flow — mustChangePassword=TRUE 사용자 interceptor | ✅ 구현 · ❌ TC skip | Q1 과 세트 |
| 변경 완료 → `/logout` → 재로그인 → `/admin` 정상 | ✅ 구현 · ❌ TC skip | Q1 과 세트 |

---

## 2. 배경 · 스코프

### 왜 이 티켓인가

A5-1 본체(#219)는 "기능 E2E 를 이월" 하면서 머지됐고 PR #224 가 7건 + skip 1건으로 1차 커버했지만:

1. **디자인 계약이 비어 있음** — `/admin/users/new` 는 ad-hoc 셀렉터로만 검증. 계약 검사(`--project=contracts`)에 포함되지 않아 prototype 정합 회귀가 자동 감지되지 않음. A5-1 본체 spec §12 가 이미 "계약 신설 1종: `admin-user-new.md`" 를 결정했으나 실제 신설 안 됨.
2. **강제 PW 변경 왕복 테스트 skip 상태** — "메일 성공 시 password 는 메일에만 담김" 이라는 Q3=A 결정으로 skip. 하지만 force-fail flag 가 PR #244 로 도입된 후 되살릴 수 있음.
3. **상세 화면의 A5-1 발급 결과 섹션** (초기 password 카드 · 메일 배너 · 재발급 CTA · 에러 카드) 이 기존 `admin-user-detail.ts` 계약에 없음 — A5 시점 작성.
4. **메일 발송의 write 는 UI 로만 간접 검증** — mock 로그 캡처 assertion 추가 가능성.

### 포함 (A5-1-e2e)

| 항목 | 설명 |
|---|---|
| **F1. 신규 발급 폼 디자인 계약 신설** | `docs/design-contracts/admin/user-new.md` + `e2e/contracts/admin-user-new.ts`. prototype L1946~2045 인용. **옵션 B 편입** 결과 (ADMIN 선택 → 관리자 종류 sub-radio → CENTER_ADMIN 선택 → 센터 select) 동적 섹션 2단계도 계약화 |
| **F2. 상세 화면 A5-1 발급 결과 섹션 셀렉터 보강** | 기존 `admin-user-detail.ts` 에 선택적(exists kind) 체크 추가 — 초기 password 카드 · password value 노출 · 메일 성공 배너 · 재발급 CTA · 발급 에러 카드. 재랜더 전제 조건 (flashInitialPassword · flashMailSent · flashError) 때문에 계약 검사 러너에서 skip 될 가능성 → **별도 계약(`admin-user-detail-after-issue.ts`) 분리 vs 기존 계약 확장 — Q3 결정** |
| **F3. 강제 PW 변경 왕복 E2E 재활성화** | 현 skip TC-6. **force-fail ON → 발급 → 화면 password 카드에서 tempPw 추출 → force-fail OFF → 신규 관리자 로그인 → `/password/change` 강제 → 변경 → 재로그인 → `/admin` 정상** 왕복. finally 블록 반드시 force-fail OFF 복구 |
| **F4. `/password/change` 디자인 계약 신설** (Q4 A 채택 시) | `docs/design-contracts/password-change.md` + `e2e/contracts/password-change.ts`. 강제 flow 와 자율 flow 두 상태의 fragment 차이 (force-notice visible/hidden) 계약화 |
| **F5. 회귀 방어 재검** | DataInitializer.seedAdmins 로그인 flow · 일반 사용자 signup/login/apply · A5 `/admin/users/{id}` 기존 기능 모두 통과 유지 |
| **F6. 보강 유닛 테스트** (선택) | `MockAdminInvitationMailSender` `maskPassword` · `forceFail` toggle 단위 테스트 (현재 미존재) |

### 제외 (이월)

| 항목 | 사유 | 담당 |
|---|---|---|
| 자율 PW 변경 flow 메뉴 연결 (mypage → `/password/change`) | 범위 외 UX | A5-2 (mypage 트랙) 또는 별건 |
| 초대 메일 템플릿 디자인 계약 (HTML 본문 토큰) | spec 범위 — Playwright 로 메일 본문 검증은 과잉 | A7-mail-template-contract (후속) |
| SMTP 실 발송 통합 테스트 CI 포함 | 비용·안정성 | `AdminInvitationMailServiceTest` (GreenMail) 로 커버 중 |
| 발급 시 mail 발송 실패 통지 로그 집계 | admin audit 범위 | 후속 |

---

## 3. RBAC · 스코프

A5-1 본체 그대로. **E2E 범위에서 RBAC 재검 TC 유지**:

| 경로 | 접근 role | TC |
|---|---|---|
| `GET /admin/users/new` | SYSTEM_ADMIN 만 (CENTER_ADMIN 403) | TC-8 기존 |
| `POST /admin/users` | SYSTEM_ADMIN 만 | 신설 — CSRF 미포함 요청 reject · CENTER_ADMIN POST 403 |
| `POST /admin/users/{uid}/reset-password` | SYSTEM_ADMIN 만 · 자기 자신 금지 | 신설 — 자기 리셋 시 flashError (Safeguard) |
| `/password/change` | 인증된 사용자 (role 무관) | 신설 — 익명 접근 시 로그인 redirect |

---

## 4. 변경 범위 (파일 단위)

### 신설

- [ ] `docs/design-contracts/admin/user-new.md` — 신규 발급 폼 계약 설명
- [ ] `e2e/contracts/admin-user-new.ts` — 계약 셀렉터 20~30건
- [ ] (Q3=A 채택 시) `e2e/contracts/admin-user-detail-after-issue.ts` — 발급 후속 상태 계약 (별도)
- [ ] (Q4=A 채택 시) `docs/design-contracts/password-change.md` + `e2e/contracts/password-change.ts`
- [ ] (Q6=A 채택 시) `src/test/java/.../admin/MockAdminInvitationMailSenderTest.java` — forceFail · maskPassword 단위 테스트

### 수정

- [ ] `e2e/tests/admin-staff-management.spec.ts` — 기존 7 active 테스트를 계약 셀렉터로 재바인딩 + TC-6 (강제 변경 왕복) `.skip` 제거 + fallback 추출 재구성
- [ ] (Q3=B 채택 시) `e2e/contracts/admin-user-detail.ts` — A5-1 flash 섹션 셀렉터 추가 (optional kind)
- [ ] `docs/design-contracts/admin/README.md` — 신규 계약 등록
- [ ] `docs/STATE.md` — A5-1-e2e 완료 반영

### 수정 없음

- 모든 Java 소스 (A5-1 · A7 완비)
- Thymeleaf 템플릿 (data-testid 전부 기 적용)
- V19 마이그레이션 (완료)

---

## 5. 필드 / 셀렉터 명세 (주요)

### `admin-user-new.ts` 계약 셀렉터 (prototype L1946~2045 매핑)

| id | selector | kind | proto | 비고 |
|---|---|---|---|---|
| page.title | `.admin-user-new-title` 또는 h1 | text | L1950 "신규 사용자 등록" | - |
| form.exists | `form[data-testid="admin-user-new-form"]` | exists | L1954 | - |
| email.input | `input[data-testid="admin-user-new-email"]` | attr `type=email` + `required` | L1958 | - |
| email.error | 서버 flash → `.admin-user-new-error` text 패턴 | optional-text | - | 중복 발급 테스트에서 "이미 사용" 매칭 |
| name.input | `input[data-testid="admin-user-new-name"]` | exists + required | L1972 | - |
| account-type.user | `input[data-testid="admin-user-new-account-type-user"]` | attr `type=radio` + `checked` 기본값 | L1996 | - |
| account-type.admin | `input[data-testid="admin-user-new-account-type-admin"]` | exists | L2000 | - |
| admin-role.section | `#adminRoleSection` 기본 hidden | attr `style` 또는 computed `display:none` | L2004 (toggleable) | 동적 섹션 — 계약 검사에서는 초기 hidden 만 검증 |
| admin-role.center | `input[data-testid="admin-user-new-role-center"]` | exists | L2007 | |
| admin-role.system | `input[data-testid="admin-user-new-role-system"]` | exists | L2010 | |
| center.section | `#centerSection` 기본 hidden | - | L2015 | 동적 섹션 |
| center.select | `select[data-testid="admin-user-new-center"]` | exists | L2020 | |
| submit.cta | `button[data-testid="admin-user-new-submit"]` | text "등록하기" | L2030 | |
| cancel.link | 뒤로가기/취소 링크 | optional | L2035 | 구현 확인 필요 |

### `admin-staff-management.spec.ts` 재구성 TC 매트릭스

| # | 플로우 | 상태 | 변경 |
|---|---|---|---|
| 1 | 폼 렌더 | 유지 | 계약 재사용 시 중복 → 삭제 가능 (계약 검사에서 커버) |
| 2 | 동적 섹션 2단계 | 유지 | **인터랙션** — 계약으로 안 잡히므로 유지 필수 |
| 3 | USER 발급 → 메일 성공 배너 | 유지 | - |
| 4 | 이메일 중복 → 에러 | 유지 | - |
| 5 | PW 재발급 → 배너 | 유지 | - |
| 6 | 강제 PW 변경 왕복 | **재활성화** | force-fail ON으로 tempPw 추출 → OFF 후 재로그인 흐름 |
| 7 | USER 발급 · 메일 실패 fallback | 유지 | - |
| 8 | RBAC CENTER_ADMIN 403 | 유지 | - |
| 9 (신설) | POST /admin/users CENTER_ADMIN 403 | **신설** | API 레벨 RBAC |
| 10 (신설) | 자기 자신 reset-password → flashError | **신설** | Safeguard assertCanResetPassword |
| 11 (신설) | 마지막 SYSTEM_ADMIN 리셋 — 허용 확인 (Qn-6 A: 허용) | **신설 선택** | 리셋은 차단과 다름 |
| 12 (신설) | 자율 PW 변경 (`/password/change` 직접 진입, flag=FALSE) | **Q5 결정** | 메뉴 미연결이지만 URL 접근 허용되는지만 |

---

## 6. 결정 필요 항목 (Qn — 5건)

### Q1: 강제 PW 변경 왕복 TC 재활성화 방식

- **A안 (권장)**: force-fail ON → 발급 → 화면 fallback 카드에서 tempPw 추출 → force-fail OFF → 신규 관리자 로그인 → `/password/change` 강제 → 변경 → `/logout` → 새 PW 재로그인 → `/admin` 정상 왕복. finally 로 force-fail OFF 보장.
- B안: `MockAdminInvitationMailSender` 에 `getLastSentPassword()` getter 추가. 메일 성공 상태 그대로 두고 Java 메모리에서 password 획득.
- 판정: **A안** — mock sender 공개 API 를 보안 민감 데이터로 늘리지 않음. force-fail flag 는 이미 테스트 전용으로 승인된 수단.

### Q2: Mock 메일 발송 로그 assertion 추가 여부

- A안 (추가): Playwright 가 `/__test__/mail/log` (신규 endpoint) 로 최근 발송 기록 조회 → "발송 1건 to 이메일 X" assertion
- **B안 (권장 · 유지)**: 현행대로 UI 배너로만 간접 검증. Java 레이어는 `AdminInvitationMailServiceTest` (GreenMail) 가 커버
- 판정: **B안** — 로그 포집 endpoint 신설은 스코프 초과. Java 레벨 커버가 충분.

### Q3: 상세 화면 A5-1 발급 결과 섹션 계약 — 기존 확장 vs 별도 신설

- A안: 기존 `admin-user-detail.ts` 에 `kind: 'exists'` + `optional: true` 로 추가 (계약 검사에서 미노출 시 skip)
- **B안 (권장)**: `admin-user-detail-after-issue.ts` 신설 — 발급 직후 상태(= flashInitialPassword/flashMailSent 심어진 상태)에서만 돌아가는 독립 계약. runner 가 "발급 → 리다이렉트 → 셀렉터 검증" 시나리오 1건 실행.
- 판정: **B안** — 계약 검사는 "임의 상태로 페이지 로드 후 셀렉터 매치" 전제. flash 는 1회성이라 optional 처리로는 기대값 명시가 모호해짐.

### Q4: `/password/change` 디자인 계약 신설 여부

- **A안 (권장)**: 신설 — 강제 상태(`?force` 또는 flag=TRUE) 와 자율 상태(flag=FALSE) 2 fragment 를 계약화. 셀렉터 7~10건.
- B안: 미신설 — 기능 E2E(TC-6) 만으로 충분. prototype 미명시 화면이라 계약 신설 기준 낮음.
- 판정: **A안 (약함)** — 강제 변경은 보안 경로이므로 회귀 자동 감지가 필요. 단 prototype 원문이 없어 "deviation 전제 계약" 표기 필수.

### Q5: 자율 PW 변경 flow (flag=FALSE 유저가 직접 `/password/change` 진입) E2E 포함 여부

- A안: TC 신설 — 메뉴 미연결 상태지만 URL 직진 접근 허용 확인
- **B안 (권장)**: **제외** — 메뉴 연결이 없는 상태에서는 사용자 flow 가 아님. A5-2 mypage 트랙에서 메뉴 연결 시 함께 신설.
- 판정: **B안** — 범위 유지.

### Q6: `MockAdminInvitationMailSender` 단위 테스트 신설 여부

- **A안 (권장)**: `MockAdminInvitationMailSenderTest` 신설 — forceFail toggle · maskPassword 패턴 · null 안전성 · log 포맷 2~3건
- B안: 미신설 — 통합 테스트 (`AdminInvitationMailServiceTest`) 와 E2E (`admin-staff-management.spec.ts`) 로 간접 커버
- 판정: **A안 (약함)** — maskPassword 는 보안 (로그 유출 방어) 코드라 단위 테스트가 적절. 소규모 (파일 1개, ~60 LOC).

---

## 7. 검증 시나리오 (ym-qa 실행)

### 정적 검증

- `./gradlew compileJava test --tests JpaMappingTest` 통과
- (Q6=A 시) `./gradlew test --tests MockAdminInvitationMailSenderTest` 통과
- (Q6=A 시) `./gradlew test --tests AdminUserServiceStaffTest` 재실행 (회귀 재확인)

### 계약 검사

- `cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts`
- 신규/확장 계약 galp 0:
  - `admin-user-new` galp 0 (신설)
  - `admin-user-detail` galp 0 (유지)
  - (Q3=B) `admin-user-detail-after-issue` galp 0 (신설, 시나리오 러너)
  - (Q4=A) `password-change` galp 0 (신설, deviation 표기)

### 동적 검증 (bootRun e2e · curl)

- `./.claude/scripts/bootrun-e2e.cmd` (포트 8090)
- `GET /admin/users/new` 200 (SYSTEM_ADMIN 로그인 세션)
- `GET /admin/users/new` 403 (CENTER_ADMIN)
- `POST /admin/users` → 302 `/admin/users/{id}` + 메일 성공 배너 노출 (default mock)
- `POST /__test__/mail/force-fail?enabled=true` → 204 → `POST /admin/users` → fallback password 카드 노출
- `POST /__test__/mail/force-fail?enabled=false` → 204 (복구)

### 기능 E2E (`--project=chromium`)

- `admin-staff-management.spec.ts` 전건 실행 — **TC-6 skip 해제 후 포함 전체 11건 이상 PASS**
- `admin-users.spec.ts` · `admin-user-detail.spec.ts` 회귀 확인
- `admin-head-scripts.spec.ts` 등 admin 공통 회귀 확인

### 시각 검증 (사용자 영역)

- 신규 발급 폼 — 모바일 폭에서 계정타입/관리자종류/센터 섹션 전환 애니메이션
- 상세 화면 — 초기 password 카드 색/강조 (운영자가 못 보고 넘어가지 않도록)

---

## 8. 갭 리스트 (현재 vs 완료 기준)

| # | 항목 | 현재 | 완료 기준 | 우선순위 |
|---|---|---|---|---|
| 1 | `admin-user-new.md` + `.ts` 계약 | ❌ 없음 | ✅ 신설, 갭 0 | **P0** |
| 2 | 강제 PW 변경 왕복 TC | ⚠️ skip | ✅ active · PASS | **P0** |
| 3 | 상세 화면 발급 결과 섹션 계약 커버 | ❌ 미반영 | ✅ 커버 (Q3 B 안) | P1 |
| 4 | `/password/change` 계약 (Q4 A 안) | ❌ 없음 | ✅ 신설 | P2 |
| 5 | POST /admin/users CENTER_ADMIN 403 TC | ❌ 없음 | ✅ 신설 (API 레벨 RBAC) | P1 |
| 6 | 자기 자신 reset-password → flashError TC | ❌ 없음 | ✅ 신설 (Safeguard 재검) | P1 |
| 7 | `MockAdminInvitationMailSender` 단위 테스트 (Q6 A 안) | ❌ 없음 | ✅ 신설 | P2 |
| 8 | docs/STATE.md 업데이트 (A5-1-e2e 완료 표기) | 대기 | ✅ 반영 | P1 |

---

## 9. 의존성 / 선행 작업

- ✅ A5-1 본체 머지 (#219)
- ✅ A7 SMTP 메일 (#243)
- ✅ A7 mail followup force-fail flag (#244)
- ✅ 기능 E2E 1차 (#224)

**선행 없음** — 바로 착수 가능.

---

## 10. 회귀 방지 (최우선)

| 영역 | 감지 방법 |
|---|---|
| DataInitializer.seedAdmins 로그인 flow | 기존 `admin-login.spec.ts` 재실행 — sysadmin 계정 flag=FALSE 유지 |
| 일반 사용자 signup / login / apply | `signup.spec.ts` · `login.spec.ts` · `apply.spec.ts` 재실행 |
| A5 `/admin/users` 목록 / 상세 / role / 차단 | `admin-users.spec.ts` · `admin-user-detail.spec.ts` 재실행 |
| A8 bulk endpoint | `admin-users-actions.spec.ts` · `admin-bulk-csv.spec.ts` 재실행 |
| A7 SMTP (GreenMail) | `AdminInvitationMailServiceTest` 재실행 |
| Mock mail force-fail 복구 누락 | spec 전건 실행 후 `/__test__/mail/force-fail?enabled=false` idempotent 호출로 종결 (watchdog) |
| 계약 검사 전건 | 전 화면 갭 0 유지 |

---

## 11. 작업 큐 메타

- 작업 ID: `A5-1-e2e-admin-staff-management`
- 우선순위: admin 트랙 완결 (A5-1 이월 소진)
- 추정 단위: **1 PR** (계약 신설 + skip 해제 + TC 보강)
- 상태: **`spec_draft`** — Q1~Q6 결정 대기

---

## 12. 다음 단계 인계

**결정 요약 요청**:

1. **Q1**: 강제 PW 변경 TC 재활성화 — **A안 (force-fail toggle)** 권장
2. **Q2**: Mock mail 로그 assertion — **B안 (현행 유지, 추가 X)** 권장
3. **Q3**: 상세 발급결과 계약 — **B안 (별도 신설 `admin-user-detail-after-issue.ts`)** 권장
4. **Q4**: `/password/change` 계약 신설 — **A안 (신설, deviation 표기)** 약권장
5. **Q5**: 자율 PW 변경 flow — **B안 (제외, A5-2 mypage 로 이월)** 권장
6. **Q6**: `MockAdminInvitationMailSender` 단위 테스트 — **A안 (신설, 소규모)** 약권장

Q1~Q6 모두 권장안 확정 시 → `spec_confirmed` 승격 → ym-impl 인계 가능.

**ym-impl 위임 프롬프트에 반드시 포함할 회귀 방어 3점**:
1. force-fail toggle 테스트 후 반드시 OFF 복구 (finally)
2. TC-6 재활성화 시 신규 관리자 이메일 unique (Date.now() 패턴 유지) — H2 create-drop 이지만 단일 부팅 내 중복 방어
3. 계약 셀렉터 재바인딩 시 기존 `admin-staff-management.spec.ts` 모든 테스트 PASS 유지 (ad-hoc 셀렉터 → 계약 참조로 점진 전환)
