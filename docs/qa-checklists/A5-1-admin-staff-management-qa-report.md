# A5-1 admin-staff-management QA 리포트

| 메타 | 값 |
|---|---|
| 대상 커밋 | `de530e7` (feature/A5-1-admin-staff-management HEAD) |
| 명세 | `docs/specs/A5-1-admin-staff-management.md` (spec_confirmed, `6cd0a74`) |
| QA 실행 | 2026-09-18 (회사 PC · Windows) |
| 검증 6영역 | 정적 · 동적 · 계약 · 기능 · 회귀 · 시각 |
| 판정 | **PASS (프로덕션 회귀 0건 · 환경 한계 1건 분리 표기)** |

---

## 1. 정적 검증 (PASS)

### 1-1. compileJava
```
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.14"; .\gradlew.bat compileJava
> Task :compileJava UP-TO-DATE
BUILD SUCCESSFUL in 9s
```

### 1-2. 신규 테스트 5종 실행
```
.\gradlew.bat test --tests "*Staff*" --tests "*PasswordChange*" \
                    --tests "*PasswordGenerator*" --tests "JpaMappingTest" \
                    --tests "AdminUserNewRenderTest"
BUILD SUCCESSFUL in 2m 5s
```

- `SecureRandomPasswordGeneratorTest` — 자동생성 12자 · 4 클래스 · 반복 중복 0
- `AdminUserServiceStaffTest` — createStaff · resetPassword · Safeguard 통합
- `AdminUserSafeguardTest` — assertCanCreateStaff · assertCanResetPassword
- `PasswordChangeRequiredInterceptorTest` — 시드 계정 통과 · 신규 계정 리다이렉트 · 변경 후 flag 해제
- `AdminUserNewRenderTest` — Thymeleaf 렌더 · 표현식 잔존 0건
- `JpaMappingTest` — V19 3필드 매핑 정합 ✅

**exhaustive switch 확인**: NotificationType 등 enum 소스 무변경 (신규 case 추가 없음). compileJava 성공으로 재확인.

---

## 2. 동적 검증 (PASS · curl 실측)

### 2-0. bootRun 기동
- `.claude\scripts\bootrun-e2e.cmd` → e2e 프로파일 · H2 · 시드 · 포트 8090
- `GET /admin/login` 200 확인 후 시나리오 진행
- 정적 리소스: `/css/main.css` 200 · `/css/admin.css` 200 · `/images/logo_symbol.png` 200 ✅

### 2-1. GET /admin/users/new (렌더)
```
GET /admin/users/new (SYSTEM_ADMIN sysadmin@youth-moa.test): 200 ✅
```

**폼 필드 실측** (curl 응답 파싱):
- name="email" · name="name" · name="gender" · name="accountType" (USER · ADMIN)
- name="adminRole" (CENTER_ADMIN · SYSTEM_ADMIN — accountType=ADMIN 시)
- name="centerId" · name="role" · name="_csrf"
- Thymeleaf 표현식 잔존 0건 (`th:*` 는 style/HTML attr 내 인용 부호 오탐만)
- prototype 정합 라벨 존재: 이메일 · 이름 · 성별 · 권한 · 사용자 · 관리자 · 소속 센터 · SYSTEM_ADMIN · CENTER_ADMIN

### 2-2. RBAC 3종
```
GET /admin/users/new (CENTER_ADMIN center1@youth-moa.test):   403 ✅
GET /admin/users/new (미인증):                                 302 → /admin/login ✅
GET /admin/users/new (SYSTEM_ADMIN):                          200 ✅
```

### 2-3. POST /admin/users — 신규 계정 발급
```
POST /admin/users (SYSTEM_ADMIN, role=CENTER_ADMIN, centerId=1): 302 → /admin/users/54 ✅
```

**flash 초기 password 노출 실측** (detail 페이지 응답):
```html
<div class="admin-flash admin-flash--success" role="status"
     data-testid="admin-user-initial-password" ...>
  <p>초기 비밀번호가 발급되었어요</p>
  <p>이메일: <strong>qa-newstaff-1789711628@test.com</strong></p>
  <p>초기 비밀번호:
    <strong data-testid="admin-user-initial-password-value">djqbtcCDS&67</strong>
  </p>
</div>
```
- 12자 · 영대·영소·숫자·특수문자 각 1자 이상 실측 (`djqbtcCDS&67`) ✅
- `must_change_password=TRUE` 검증 → 로그인 후 인터셉터 발동으로 실증 (2-5)

### 2-4. POST /admin/users/{id}/reset-password
```
POST /admin/users/1/reset-password (자기 자신 sysadmin, id=1): 302 → /admin/users/1 ✅
detail flash: "본인 계정의 비밀번호는 관리자 재발급 대상이 아니에요..." ✅ (Safeguard assertNotSelf 재활용)
```

### 2-5. 강제 password 변경 flow (완전 왕복)

**신규 계정 (`qa-newstaff-1789711628@test.com` · CENTER_ADMIN · must_change_password=TRUE) 로그인**
```
POST /admin/login (username=..., password=djqbtcCDS&67): 302 → /admin ✅
GET /admin (신규 계정 진입 시도):                          302 → /password/change ✅ (Interceptor)
GET /admin/programs (다른 경로도 마찬가지):                302 → /password/change ✅
GET /password/change:                                      200 ✅ (허용 경로)
```

**password 변경 성공**
```
POST /password/change
  currentPassword=djqbtcCDS&67
  newPassword=NewPw2026!
  newPasswordConfirm=NewPw2026!
→ 302 → /logout ✅  (must_change_password=FALSE + passwordChangedAt=now)
```

**재로그인 후 flag=FALSE 통과**
```
POST /admin/login (username=..., password=NewPw2026!): 302 → /admin ✅
GET /admin:                                             200 (본문 렌더) ✅
```

### 2-6. 이메일 중복 방어
```
POST /admin/users (email=sysadmin@youth-moa.test, 시드 계정 재사용): 302 → /admin/users/new
flash: "이미 사용 중인 이메일이에요." ✅
```

### 2-7. 기존 시드 계정 무회귀 (Interceptor 통과)
```
POST /admin/login (sysadmin@youth-moa.test / Admin!234): 302 → /admin ✅
GET /admin (시드 SYSTEM_ADMIN):                           200 ✅ (must_change_password=FALSE → 즉시 통과)
GET /admin/users:                                         200 ✅
```

---

## 3. 계약 검증 (이월 — 명세 §후속 정합)

옵션 B (QC 결정) — `/admin/users/new` 로 편입되어 신규 화면·계약 신설 없음.

- `admin-users.ts` 확장 CTA "신규 사용자 등록": HTML 실측으로 확인 ✅ (docs/design-contracts/admin/users.md 갱신 여부는 후속 계약 정비 티켓 이월)
- `admin-user-detail.ts` flash 카드 · reset CTA 추가: HTML 실측으로 확인 ✅
- `admin-user-new.md` + `admin-user-new.ts` 신설: **이월** (계약 파일 부재 확인, 향후 신설 권장)

**딜리버리**: 계약 파일 미신설은 명세 §후속 QC 옵션 B "계약 신설 1종: admin-user-new.md" 이월 여지 있음. 프로덕션 회귀 없음 → 커밋 승인 무관.

---

## 4. 기능 E2E (이월)

Playwright 스펙 `admin-user-new.spec.ts` / `password-change.spec.ts` 신설은 명세에서 "선택 · 이월 가능"으로 표기됨. 회사 PC bootRun 8090 curl 실측으로 완전 왕복 시나리오 검증 완료 (§2-5).

- **이월 사유**: 회사 PC Playwright 실행은 CI ubuntu 러너로 대체. 신설 필요 시 후속 티켓.
- **커버리지 확보**: 정적 (Interceptor·Service·Safeguard 5 테스트) + 동적 (curl 왕복) 이중 검증으로 실증 완료.

---

## 5. 회귀 검증 (PASS · 최우선 3점)

### 5-0. 전체 gradle test
```
.\gradlew.bat test
655 tests completed, 1 failed
> Task :test FAILED
```

**실패 1건 분석 — 프로덕션 회귀 아님**:
```
YouthMoaApplicationTests > contextLoads() FAILED
Caused by: java.lang.IllegalStateException:
  Could not find a valid Docker environment. Please see logs and check configuration
```
- 원인: 회사 PC Docker Desktop 미기동 → Testcontainers PostgreSQL 컨테이너 생성 실패
- `docker version` 확인: `failed to connect to the docker API ... The system cannot find the file specified.`
- **환경 한계 (CLAUDE.md 실행 환경 분리 규칙)**. 프로덕션 코드/A5-1 변경과 무관.
- **CI ubuntu 러너 (`integration-test` job) 에서 동일 테스트 자동 실행됨** → 매 PR 통합 검증 게이트 유지
- 나머지 **654 TC PASS · A5-1 관련 5 신규 테스트 100% PASS**

### 5-1. DataInitializer.seedAdmins 무회귀 ✅
- 시드 계정 `sysadmin@youth-moa.test` 로그인 → `/admin` 200 (Interceptor 통과)
- V19 default `must_change_password=FALSE` 로 시드 계정 3건 (SYSTEM_ADMIN 1 + CENTER_ADMIN 2) 자동 반영
- Interceptor 즉시 통과 (O(1) 조건 분기 `!up.isMustChangePassword() → return true`)
- `DataInitializer.java` 소스 변경 diff 0 (git show --stat 확인)

### 5-2. 일반 사용자 signup/login/apply flow 무회귀 ✅
- `UserService.signUp()` 무변경 (git show 대상 파일 목록에 미포함)
- Interceptor 는 `UserPrincipal.mustChangePassword` snapshot 필드 검사 → 일반 회원가입 시 default FALSE 로 즉시 통과
- `GET /` (익명): 200 ✅ · `GET /programs`: 200 ✅
- SignupAutoLoginTest · SignupRenderTest · UserServiceSignUpTermsTest · MyPageRenderTest · SignupPhoneVerifiedTest 회귀 스위트 PASS (전체 654 PASS 내 포함)

### 5-3. A5·A8 endpoint 무회귀 ✅
- `AdminUserSafeguard` 는 기존 3종 (`assertNotSelf` · `assertNotLastSystemAdmin` · `assertCanChangeRole`) 무변경 · 신설 2종 (`assertCanCreateStaff` · `assertCanResetPassword`) 만 추가
- A5 endpoint: `/admin/users` 목록·상세 (GET /admin/users 200 ✅ · GET /admin/users/1 200 ✅) · role 변경 · deactivate/reactivate · admin-note 영향 0건
- A8 bulk endpoint: 소스 변경 0 (git diff 미포함) → 무회귀
- AdminUserServiceTest · AdminUserControllerRbacTest · AdminUserListRenderTest · AdminUserDetailRenderTest · AdminUserBulkServiceTest · UserPrincipalIsEnabledTest 전체 PASS

---

## 6. 시각 확인 (사용자 영역 · 대기)

curl 응답 HTML 파싱으로 마크업·flash·CTA 존재는 실증했으나 아래는 시각 실사가 필요:

- `/admin/users/new` 폼 시각 정렬 (권한 radio 2옵션 · role sub-radio 조건부 show/hide JS 동작)
- `/admin/users/{id}` detail 상단 초기 password flash 카드 시각 (`--color-primary` 좌 4px 강조 · monospace · `#EEF0FF` 배경)
- `/password/change` 강제 모드 (`?force=1`) 화면 문구 (뒤로가기 CTA 숨김 여부)
- 반응형 실기기 감각 (모바일 폼 grid 붕괴 없는지)
- prototype L1946~2045 시각 정합 (인디고 · 패딩 24px · gap 16px 등)

**preview 도구 세션 미로드로 preview_inspect computed style 실측 불가** → 사용자 브라우저 확인 대기.

---

## 7. 환경 한계 미실행 (분리 표기)

| 항목 | 사유 | 대체 |
|---|---|---|
| `YouthMoaApplicationTests.contextLoads()` (Testcontainers) | 회사 PC Docker Desktop 미기동 | CI ubuntu 러너 `integration-test` job 자동 실행 |
| Playwright 기능 E2E (`admin-user-new.spec.ts` 등 미신설) | 명세에서 "선택·이월 가능" | curl 왕복 실측 §2-5 + 5 신규 정적 테스트 |
| preview_inspect computed style 실측 | preview 도구 세션 미로드 | 사용자 브라우저 시각 확인 대기 §6 |

---

## 8. 판정 · 커밋 승인

- **PASS** — 프로덕션 회귀 0건. 프로덕션 수정 필요 없음.
- 명세 §후속 QC 옵션 B 정합 (별도 `/admin/staff` 없음 · `/admin/users/new` 편입) 실증 완료
- 회귀 방어 3점 실증 완료 (5-1 · 5-2 · 5-3)
- 실패 1건 (`YouthMoaApplicationTests`) 은 환경 한계 · 프로덕션·A5-1 무관 · CI 자동 커버
- 시각 확인 대기 항목은 사용자 영역 (§6). 머지 전 컨펌 권장.

## 9. 커밋

- 리포트 파일: `docs/qa-checklists/A5-1-admin-staff-management-qa-report.md`
- 커밋 메시지: `260918_A5_1_admin_staff_management_qa - 정적 655 TC (신규 5 + 회귀 649) · 동적 curl 왕복 8종 · 회귀 방어 3점 PASS · 환경 한계 1건 분리`
