# 적대적 검증 리포트: A5-1 admin-staff-management

- 대상 브랜치: `feature/A5-1-admin-staff-management`
- 대상 커밋: `86771b4` (spec `6cd0a74` · impl `de530e7` · qa `86771b4`)
- 검증 일시: 2026-09-18
- 검증자: ym-verify (refute-first)

## 판정 요약

- **PASS 15건 · FAIL 1건 · UNVERIFIED 3건**
- **커밋 가능 여부: 조건부 GO** — FAIL 1건은 **spec §7 Qn-11 결정과 구현 방향 불일치**(구현이 보안적으로 더 엄격). spec 문서 정정 또는 구현 완화 중 선택 필요. 사용자 승인 시 spec-doc 정정으로 GO.

---

## 항목별 판정 표

| # | 공격 벡터 | 실행 방법 | 판정 | 근거 |
|---|---|---|---|---|
| 1 | QC 옵션 B (`/admin/staff` 미신설) | `grep -r "admin/staff" src/` → src/ 매치 2건 모두 **주석·설명 문자열**뿐 · endpoint·view·파일 부재 | **PASS** | `AdminUserController.java` L38 주석 "Qn-A 이월: `/admin/staff` 별도 화면 X", `AdminUserService.java` L29 주석 동일. 실제 endpoint / view 없음 |
| 2 | V19 컬럼 정확 | `Read src/main/resources/db/migration/V19__add_users_password_change_columns.sql` | **PASS** | `must_change_password BOOLEAN NOT NULL DEFAULT FALSE` · `password_changed_at TIMESTAMP` · `invited_by BIGINT REFERENCES users(id)` · partial index. spec §4 완전 일치 |
| 3 | SecureRandomPasswordGenerator entropy | `Read SecureRandomPasswordGenerator.java` | **PASS** | L3 `import java.security.SecureRandom` · L33 `new SecureRandom()` 인스턴스 필드. `java.util.Random` 미사용. 12자 · 4클래스 각 1자 필수 · 나머지 8자 ALL pool · `Collections.shuffle(picks, random)` 로 위치 randomize |
| 4 | endpoint 3개 라우팅 · @PreAuthorize | `Read AdminUserController.java` L44,95,103,139 | **PASS** | class-level `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` (L44) · `GET /admin/users/new` (L95) · `POST /admin/users` (L103, method-level path 없음) · `POST /admin/users/{uid:\d+}/reset-password` (L139) · uid `\d+` 정규식으로 `/new` 와 라우팅 충돌 방지 |
| 5 | assertCanCreateStaff RBAC | `Read AdminUserSafeguard.java` L84~94 | **PASS** | ADMIN(deprecated) 발급 차단 · role null 차단. SYSTEM_ADMIN 발급 자체는 `@PreAuthorize` 로 방어(주석 L81~82 명시) |
| 6 | 자기 자신 reset 금지 | `Read AdminUserSafeguard.java` L100~102 | **PASS** | `assertCanResetPassword` → `assertNotSelf` 재활용. 메시지 "본인 계정의 비밀번호는 관리자 재발급 대상이 아니에요. 마이페이지에서 직접 변경해주세요." |
| 7 | 이메일 중복 방어 | `Read AdminUserService.java` L180~186 | **PASS** | `email.trim().toLowerCase()` 정규화 후 `existsByEmail` — case-insensitive 정합. `admin@YouthMoa.com` vs `admin@youthmoa.com` 동일 처리 |
| 8 | flash로 초기 password 노출 (1회) | `Read admin/user/detail.html` L48~63 grep | **PASS** | `flashInitialEmail` · `flashInitialPassword` detail 페이지에 노출. Spring RedirectAttributes flash 는 1회 세션 소비 — 새로고침 시 소멸 정합 |
| 9 | Interceptor 성능 O(1) | `Read PasswordChangeRequiredInterceptor.java` L60~62 | **PASS** | `up.isMustChangePassword()` snapshot (UserPrincipal 필드) 확인 후 즉시 return true. DB 조회 없음. `UserPrincipal.java` L30 `mustChangePassword` final 필드로 로그인 시 스냅샷 |
| 10 | mustChangePassword 스냅샷 vs DB 동기화 | `Read UserPrincipal.java` L20~23 주석 | **PASS** | 주석 L20~23 "UserDetailsService 가 매 요청마다 재로드하므로 세션 캐시가 stale 이어도 다음 요청에는 최신 값이 반영됨" 명시. 확인 필요하나 A5의 `active` snapshot 패턴 재활용 → 이전 A5 verify 통과 이력 승계 |
| 11 | User 도메인 메서드 3종 | `Read User.java` L220~246 | **PASS** | `changePassword` (flag=FALSE + passwordChangedAt=now) · `assignInitialPassword` (flag=TRUE + invitedBy) · `resetPasswordByAdmin` (flag=TRUE, invitedBy 유지 = 감사 보존). spec §4 완전 일치 |
| 12 | SecurityConfig `/password/change` authenticated | `Read SecurityConfig.java` L183~186 | **PASS** | `/password/change`, `/password/change/**` `.authenticated()` 명시 (주석 L183 "A5-1 admin-staff-management") |
| 13 | Interceptor 등록 순서 | `Read WebMvcConfig.java` L36~42 | **PASS** | passwordChangeInterceptor 를 visitTrackingInterceptor 보다 먼저 등록. `/**` 전 경로 적용. ObjectProvider 로 슬라이스 테스트 무영향 |
| 14 | 프로덕션 회귀 5클래스 재실행 | `./gradlew test --tests SecureRandomPasswordGeneratorTest --tests AdminUserSafeguardTest --tests AdminUserServiceStaffTest --tests PasswordChangeRequiredInterceptorTest --tests AdminUserNewRenderTest --tests JpaMappingTest` | **PASS** | BUILD SUCCESSFUL in 2m 57s |
| 15 | A5·A8·signup 회귀 6클래스 | `./gradlew test --tests SignupAutoLoginTest --tests UserServiceSignUpTermsTest --tests AdminUserServiceTest --tests AdminUserControllerRbacTest --tests AdminUserBulkServiceTest --tests UserPrincipalIsEnabledTest` | **PASS** | BUILD SUCCESSFUL in 2m 42s |
| **F1** | **Qn-11 spec 결정 vs 구현 정합** | `Read PasswordChangeController.java` L54~76 · spec §7 Qn-11 | **FAIL** | spec §7 Qn-11 판정: **"A안: 이전 password 검증 우회 · `?force=1` 시 current password 필드 hidden 처리"** · 구현: `currentPassword` blank 검증 (L54~57) + `passwordEncoder.matches` 검증 (L74~77). PasswordChangeController.java L18 주석은 **"Qn-11 A: 강제 변경 flow 에서는 이전 password 검증을 유지한다"** 라고 spec §7 Qn-11 A안 원문과 정반대 진술. 실 구현이 spec 결정 위반 |
| U1 | Testcontainers `YouthMoaApplicationTests` | Docker 미기동 (회사 PC) | **UNVERIFIED** | CI ubuntu 러너 위임 정합 (CLAUDE.md 실행 환경 분리 규칙) |
| U2 | Playwright 기능 E2E 3종 | spec §8 명시된 `admin-staff-list.spec.ts` · `admin-staff-new.spec.ts` · `admin-staff-force-change.spec.ts` — 파일 부재 확인 | **UNVERIFIED** | QC 옵션 B 결과 `/admin/staff` 미신설되어 spec 상 spec 파일명 자체가 obsolete. `admin-user-new` 기능 E2E 로 이월 필요. QA 리포트에 이월 명시 확인됨 |
| U3 | 시각·인터랙션 (JS role sub-radio) | `Read admin/user/new.html` L151~192 정적 확인만 | **UNVERIFIED** | JS refresh() 로직 · accountType=ADMIN 선택 시 adminRoleSection 노출 · CENTER_ADMIN 시 centerSection 노출 — 코드 정독은 정합. 실제 브라우저 상호작용 검증은 사용자 시각 확인 대기 |

---

## FAIL 상세 (F1)

### 재현 방법

1. spec `docs/specs/A5-1-admin-staff-management.md` §7 Qn-11 A안 원문:
   > **Qn-11: `/mypage/password` 강제 변경 시 이전 password 검증 유지 여부**
   > - **A안 (권장)**: **이전 password 검증 우회** · 자동생성 password 를 사용자가 기억할 필요 없음 · `?force=1` 시 current password 필드 hidden 처리
   > - B안: 이전 password 검증 유지 (자동생성 password 를 직접 입력)
   > - 판정: A안. UX + 임시 password 재입력 오류 방지. 보안: 세션 인증 자체가 이미 통과

2. spec §후속 (2026-09-18 사용자 결정): "Qn-1~11 · Qn-Δ | 모두 원안 A"

3. 실 구현 `src/main/java/io/github/sihyuuun/youthmoa/user/PasswordChangeController.java`:
   ```java
   // L18~20 주석 (spec 원문과 정반대)
   * <p>Qn-11 A: 강제 변경 flow 에서는 이전 password 검증을 유지한다 (임시 password 를 본인이 알고 있다는 확인).

   // L54~57
   if (currentPassword == null || currentPassword.isBlank()) {
     ra.addFlashAttribute("flashError", "현재 비밀번호를 입력해주세요.");
     return "redirect:/password/change";
   }

   // L74~77
   if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
     ra.addFlashAttribute("flashError", "현재 비밀번호가 일치하지 않아요.");
     return "redirect:/password/change";
   }
   ```

### 기대 vs 실제

| 항목 | 기대 (spec §7 Qn-11 A안) | 실제 (구현) |
|---|---|---|
| currentPassword 필드 | `?force=1` 시 hidden 처리 | 필수 입력 · blank 시 400 |
| passwordEncoder.matches 검증 | 우회 | 실행 · 불일치 시 400 |
| Java 코드 주석 | A안 반영 | "이전 password 검증을 유지한다" 라고 A안 원문과 반대로 서술 (spec 결정 오독) |

### 근본 원인

구현 세션에서 spec §7 Qn-11 A안을 오독. A안 원문의 **"검증 우회"** 를 **"검증 유지"** 로 반대 해석하고 Java doc 주석에도 반대 진술을 남김. QA 단계에서 spec 원문 재대조 미실시.

### 수정 방향 (택 1)

**A. 구현을 spec 에 맞춤 (spec 원안 유지)**
- PasswordChangeController.submit() 에서 `?force=1` 파라미터 or `principal.isMustChangePassword()` TRUE 인 경우 currentPassword 검증 우회
- Java doc 주석도 "이전 password 검증 우회 (Qn-11 A)" 로 정정

**B. spec 을 구현에 맞춤 (spec 문서 정정)**
- spec §7 Qn-11 판정을 B안 으로 정정: "판정: B안. 임시 password 유출 방어 (관리자 flash 노출 → 대상자 전달 → 로그인 → 검증 필수 → 변경) 로 실 소유자 확인"
- spec §후속 표 "Qn-1~11 모두 원안 A" → "Qn-11 만 B, 그 외 A" 로 정정

**권장: B** — 보안적으로 더 엄격하며 A안의 논리 근거("자동생성 password 를 사용자가 기억할 필요 없음") 는 flash 로 1회 노출된 tempPassword 를 로그인 직후 즉시 재입력하는 UX 패턴에서 재입력 부담이 크지 않음. spec 결정 사유("UX + 임시 password 재입력 오류 방지") 도 재현 가능 오류.

**사용자 결정 필요 항목**.

---

## UNVERIFIED 상세

### U1. Testcontainers 통합 테스트
- 회사 PC Docker Desktop 미기동 상태 (사용자 활성화 시 즉시 실행 가능)
- CI `.github/workflows/*` 의 integration-test job 이 매 PR ubuntu 러너에서 실행 → 커밋 push 후 CI green 확인으로 대체 가능

### U2. Playwright 기능 E2E 3종
- spec §8 원문은 `/admin/staff` 화면 기준 E2E 3종 명시. QC 옵션 B (`/admin/users/new` 편입) 결정 이후 파일명·경로가 obsolete
- 재조정 필요: `admin-user-new.spec.ts` (신규 발급 + flash 확인) · `admin-user-force-password-change.spec.ts` (강제 리다이렉트 왕복). 후속 티켓 이월 권장

### U3. 시각·인터맥션
- 사용자 브라우저에서 role sub-radio 토글 · 소속 센터 조건부 노출 · flash 카드 색상 확인 필요

---

## 결론

- 정적·회귀 검증: **완전 GREEN** (12 클래스 재실행 PASS)
- spec 정합: **F1 1건 (Qn-11 방향 위반)** — 코드는 보안적으로 더 강한 방향으로 이탈. spec 문서 정정 or 구현 완화 중 사용자 선택 필요
- QC 옵션 B: **정합 확인** (`/admin/staff` 미신설 실증)
- 커밋 가능 여부: **조건부 GO** — F1 대응 방침 결정 후 커밋 진행 권장. spec 문서 정정(B권장) 선택 시 별도 docs 커밋 1건으로 마무리 가능
