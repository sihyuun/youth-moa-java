# 적대적 검증 리포트: A5 admin-users

- 대상 브랜치: `feature/A5-admin-users` (HEAD `740e39c`)
- 대상 spec: `docs/specs/A5-admin-users.md` (`spec_confirmed`, 2026-09-15)
- 판정 근거: 커밋 `df63d08` 프로덕션 + `740e39c` 자동화 자산의 실 파일과 test 실행 로그
- 검증자: ym-verify (refute-first)
- 실행 환경: 회사 PC · JDK 17.0.14 · Gradle 9.6.1 · JAVA_HOME 강제 지정
- 실행 명령: `./gradlew.bat test --tests <6개 클래스>` (15분 01초, BUILD SUCCESSFUL, exit 0)

---

## 판정 요약

| 판정 | 건수 |
|---|---|
| PASS | 14 |
| FAIL | 0 |
| **UNVERIFIED** | 4 |
| deviation (기록) | 2 |

- **커밋 가능 여부**: **커밋 가능** — FAIL 0건. 단, 아래 deviation 2건과 UNVERIFIED 4건은 **spec 문서에 기록·이월 처리**해야 이후 세션 재해석 사고를 방지.
- 프로덕션 회귀 방어 3점은 유닛 테스트로 모두 PASS. 회사 PC 에서 실 signup/login/apply E2E 왕복은 미수행 (UNVERIFIED).

---

## 항목별 판정

| # | 공격 벡터 | 실행 방법 | 판정 | 근거 |
|---|---|---|---|---|
| 1 | V16 마이그레이션 컬럼 정합 | `V16__add_users_active_and_admin_fields.sql` 직접 read | PASS | is_active BOOLEAN NOT NULL DEFAULT TRUE / last_access_at TIMESTAMP / admin_note VARCHAR(1000) / deactivated_at TIMESTAMP / deactivated_by BIGINT REFERENCES users(id) / deactivation_reason VARCHAR(500) + UPDATE role='ADMIN'→'CENTER_ADMIN' + idx_users_is_active / idx_users_role — spec §12 §후속 DDL 과 100% 일치 |
| 2 | User 도메인 메서드 시그니처 | `User.java` L262-290 read | PASS | `deactivate(User admin, String reason)` / `reactivate()` / `updateLastAccess(LocalDateTime now)` / `updateAdminNote(String note)` / `promoteTo(UserRole newRole)` — spec §3 코드 스니펫과 시그니처·본문 정합. Lombok `@Setter` 부재 확인 |
| 3 | UserPrincipal.isEnabled() 회귀 방어 | `UserPrincipal.java` L61-65 read + `UserPrincipalIsEnabledTest` 5 TC 실행 | PASS | `return active;` (L64) — 하드코딩 true 아님. 생성자에서 `this.active = user.isActive();` 로 스냅샷. 5 TC (isActive=true→isEnabled=true / false→false / snapshot / other flags true / ROLE_ prefix) 모두 PASS |
| 4 | @PreAuthorize 클래스 레벨 | `AdminUserController.java` L42 read | PASS | `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` 클래스 레벨. `@EnableMethodSecurity` 는 `SecurityConfig.java` L37 확인. RBAC test 12 TC PASS (CENTER_ADMIN forbidden 4건 · SYSTEM_ADMIN ok 3건 · anonymous redirect · missing 404 · CSRF 부재 forbidden 등) |
| 5 | 목록 컬럼 9열 (Qn-9 A안 checkbox 제거) | `admin/user/list.html` L69-79 read | PASS | No·이름·이메일·성별·권한·핸드폰·가입일·최근접속·상태 = 9열. checkbox 열 없음. spec §4 컬럼 스키마 (Qn-9 A안 = checkbox hidden) 정합 |
| 6 | role filter 4옵션 | list.html L52-61 read | PASS | 전체/시스템 관리자/관리자/사용자 4옵션. `roleFilter` 파라미터 반영 |
| 7 | 상세 2컬럼 · 4탭 | `admin/user/detail.html` L48/176-185 read | PASS | admin-user-detail-grid 좌: 프로필+role+메모+danger zone / 우: 신청 이력. 탭 4종 (ALL·APPROVED·REJECTED·CANCELLED) 링크로 활성 tab query param |
| 8 | Safeguard 3종 | `AdminUserServiceTest` deactivate/reactivate/changeRole 시나리오 실행 | PASS | (1) 본인 self 3건 rejected (2) 마지막 SYSTEM_ADMIN 강등/차단 rejected + seedExtraSystemAdmin() 로 2명 확보 후 강등 허용 실증 (3) SYSTEM_ADMIN 승격 권한은 컨트롤러 @PreAuthorize(hasRole('SYSTEM_ADMIN')) 로 확보. 38 TC 모두 PASS |
| 9 | REQUIRES_NEW + 예외 삼킴 | `LastAccessAuthenticationSuccessHandlerTest.updater_class_annotated_with_REQUIRES_NEW` + `updater_exception_swallowed_login_still_succeeds` | PASS | Reflection 으로 `@Transactional(propagation=REQUIRES_NEW)` 확인 + willThrow 후 response 302 확인 (예외 상승 없음). 4 TC PASS |
| 10 | Qn-A 이월 준수 (/admin/staff 파일 없음) | `Grep "admin/staff"` src 검색 | PASS | 매치된 2건 모두 주석 (컨트롤러/서비스 docstring). 실제 `/admin/staff` RequestMapping / register.html / template 존재 안 함 |
| 11 | Qn-6 reset endpoint 신설 | `TestFixtureController.resetUsers` L299-310 read | PASS | `POST /__test__/reset-users` 신설 · e2e 프로파일 격리 · is_active TRUE 로 초기화 + deactivated_* / admin_note NULL |
| 12 | Qn-3 차단 사유 필수 | detail.html L246-247 + `AdminUserService.deactivate` L106-108 | PASS | HTML `required` + 서비스 IllegalArgumentException("차단 사유를 입력해주세요."). `deactivate_reason_blank_rejected` / `_null_rejected` 2 TC PASS |
| 13 | DisabledException blocked URL 분기 | `SecurityConfig.failureHandler` L54-75 read | PASS | DisabledException → `/login?blocked` (사용자) 또는 `/admin/login?blocked` (관리자) 분기. 그 외는 defaultFailureUrl |
| 14 | UserRole.ADMIN enum 소스 유지 (Qn-17 A) | `UserRole.java` grep | PASS | ADMIN, CENTER_ADMIN, SYSTEM_ADMIN 모두 유지. V16 은 데이터 UPDATE 만. Qn-17 A안 (enum 제거는 후속 티켓) 정합 |
| U1 | 프로필 편집 저장 POST /admin/users/{id} | AdminUserController grep | **UNVERIFIED / spec 이탈** | spec §4 라우팅 표에 명시된 `POST /admin/users/{id}` (프로필 편집 저장) 컨트롤러에 없음. detail.html L50 은 "편집은 후속 스코프" 로 축소. **spec §12 §후속은 이 항목을 명시적으로 이월하지 않음** → §4 표와 실 구현이 모순. deviation 로 명시 필요 |
| U2 | 신규 사용자 등록 GET /admin/users/new + POST /admin/users | 컨트롤러·template grep | **UNVERIFIED / spec 이탈** | spec §4 라우팅 표 + §12 §후속 "포함: `/admin/users/new` 관리자 승급 대상 등록" 명시. 실제 `admin/user/register.html` · `@GetMapping("/new")` · `@PostMapping` (create) 모두 부재. §12 §후속 "제외 (이월): 대량 관리자 계정 신규 발급 UI" 와 문구가 모호 (개별 등록도 이월?). deviation 명시 필요 |
| U3 | 기존 사용자 signup/login/apply/withdraw E2E 회귀 | 전체 스위트 미실행 | **UNVERIFIED** | 회사 PC 15분 이상 소요 예상 · 단일 test-class 6종만 실행. 프로덕션 회귀는 유닛 수준으로만 방어됨. 개인 PC 또는 CI (ubuntu integration-test job) 에 위임 필요. UserPrincipalIsEnabledTest snapshot 검증 통해 최소 정적 회귀 방어는 확보 |
| U4 | Playwright E2E 4종 실제 실행 | e2e/tests/admin-user* + visual-admin-users spec 미실행 | **UNVERIFIED** | 회사 PC 에서 Playwright 러너 미기동 · bootRun 8090 off. spec/커밋에는 스펙 파일만 있고 실 실행 근거 없음. CI 위임 |

---

## deviation 상세 (spec 문서 갱신 필요)

### deviation D1 — 프로필 편집 저장 (U1)

- **spec 표기**: §4 라우팅 표 5행 `POST /admin/users/{id}` (프로필 편집 저장) · §후속 §12 §후속 "포함" 항목엔 명시 없음.
- **실 구현**: 컨트롤러에 해당 엔드포인트 부재. detail.html 이 읽기 전용 (`admin-user-field-value--disabled` 클래스) 프로필로 렌더. 라인 50 주석 "편집은 후속 스코프".
- **판단**: **§12 §후속 결정에 프로필 편집이 명시적으로 포함되지 않았고, 실 구현이 이를 반영해 축소한 것**. §4 표는 확정 전 초안 상태로 §12 §후속에서 스코프 축소가 있었으나 §4 표를 갱신하지 않음. deviation 로 spec 에 기록 필요.
- **재현 방법**: `POST /admin/users/1` → 404 (컨트롤러 매핑 없음).
- **후속 조치**: A5-1 or 후속 티켓에서 프로필 편집 재도입 or spec §4 표에서 해당 행 삭제·이월 표기.

### deviation D2 — 신규 사용자 등록 UI (U2)

- **spec 표기**: §4 라우팅 표 `GET /admin/users/new` · `POST /admin/users` · §12 §후속 "포함: `/admin/users/new` 관리자 승급 대상 등록 (Qn-4 = 기존 사용자 대상)".
- **실 구현**: 컨트롤러·템플릿 모두 부재.
- **판단**: §12 §후속 "제외 (이월): 대량 관리자 계정 신규 발급 UI (Qn-4 → A5-1 또는 A8)" 와 spec §12 §후속 "포함:" 목록이 서로 모순. **개별 신규 등록도 이월된 것으로 해석**하고 spec §4 라우팅 표 갱신 필요.
- **후속 조치**: A5-1 후속 티켓에서 신규 등록 UI 도입 시 §4 표 재활성.

---

## UNVERIFIED 상세

| # | 이유 | 판정 조건 |
|---|---|---|
| U1 | spec §4 vs 실 구현 모순 (deviation D1) | spec 문서 갱신 or 후속 티켓 |
| U2 | spec §4 vs 실 구현 모순 (deviation D2) | spec 문서 갱신 or 후속 티켓 |
| U3 | 전체 유닛/통합 회귀 스위트 미실행 | 개인 PC 또는 CI ubuntu integration-test job green 확인 |
| U4 | Playwright E2E 실 실행 미수행 | e2e 프로파일 bootRun 8090 + `npx playwright test --project=chromium tests/admin-user*.spec.ts` |

---

## 실측 근거 (원문 인용)

### 테스트 결과 (BUILD SUCCESSFUL in 15m 1s, exit 0)

```
<testsuite name="io.github.sihyuuun.youthmoa.admin.AdminUserControllerRbacTest" tests="12" skipped="0" failures="0" errors="0" ... time="357.782">
<testsuite name="io.github.sihyuuun.youthmoa.admin.AdminUserDetailRenderTest" tests="8" skipped="0" failures="0" errors="0" ... time="15.157">
<testsuite name="io.github.sihyuuun.youthmoa.admin.AdminUserListRenderTest" tests="8" skipped="0" failures="0" errors="0" ... time="45.094">
<testsuite name="io.github.sihyuuun.youthmoa.admin.AdminUserServiceTest" tests="38" skipped="0" failures="0" errors="0" ... time="97.758">
<testsuite name="io.github.sihyuuun.youthmoa.user.LastAccessAuthenticationSuccessHandlerTest" tests="4" skipped="0" failures="0" errors="0" ... time="10.719">
<testsuite name="io.github.sihyuuun.youthmoa.user.UserPrincipalIsEnabledTest" tests="5" skipped="0" failures="0" errors="0" ... time="0.160">
```

합계 **75 TC · failures 0 · errors 0**. ym-qa 리포트 주장 확증.

### 컴파일

```
> Task :compileJava UP-TO-DATE
BUILD SUCCESSFUL in 51s
```

### diff 범위

```
git diff --stat df63d08~1..740e39c
 ...
 30 files changed, 3927 insertions(+), 9 deletions(-)
```

프로덕션 순수 신설 15 파일 + 자동화 15 종. 기존 프로덕션 삭제/재포맷 라인 = 9 (V16/SecurityConfig/UserPrincipal/UserRepository/login.html 소폭 편집 반영) — 회귀 유발 재포맷 없음.

---

## 결론

프로덕션 코드는 spec §12 §후속 결정 (스코프 축소 후) 을 충실히 반영. 테스트 커버리지도 spec §6 요구를 정적으로 만족.

**이월 필요**: spec §4 라우팅 표와 §12 §후속 결정 간 문구 모순 → §4 표 갱신 (프로필 편집 저장 · 신규 등록 2행을 "이월 A5-1" 로 표기) 필요.

**FAIL 0 · 커밋 가능**. UNVERIFIED 4건은 CI ubuntu integration-test job + 개인 PC E2E 실행에 위임.
