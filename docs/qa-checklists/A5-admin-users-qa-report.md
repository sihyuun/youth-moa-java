# A5 admin-users QA 리포트

| 메타 | 값 |
|---|---|
| 대상 커밋 | `df63d08` (feature/A5-admin-users) |
| spec | `docs/specs/A5-admin-users.md` (7ed99b1 · spec_confirmed) |
| 검증 일자 | 2026-09-15 |
| 검증자 | ym-qa (회사 PC) |
| 회귀 판정 | **CONDITIONAL PASS** — 프로덕션 코드 회귀 0건. 단 spec 요구 신설 테스트·계약·E2E 스펙 자산 미이행 (test-only 이월 필요) |

---

## 1. 정적 검증

### 결과 요약
- `compileJava` **SUCCESS** (UP-TO-DATE, 41s)
- `test --tests *User* --tests *Admin* --tests JpaMappingTest` **BUILD SUCCESSFUL** (7m 23s)

### 실행된 회귀 테스트 (31 클래스 · failures=0 errors=0)

| 클래스 | tests | time(s) |
|---|---|---|
| JpaMappingTest | 3 | 86.9 |
| AdminApplicationDetailModalRenderTest | 3 | 64.3 |
| AdminApplicationServiceTest | 24 | 35.9 |
| AdminApplicationListRenderTest | 4 | 2.8 |
| AdminApplyQuestionServiceTest | 16 | 0.3 |
| AdminDashboardRenderTest | 2 | 1.3 |
| AdminLoginRenderTest | 3 | 0.7 |
| AdminNoticeFormRenderTest | 9 | 3.2 |
| AdminNoticeServiceTest | 12 | 0.1 |
| AdminProgramAttachmentServiceTest | 8 | 0.9 |
| AdminProgramCourseUpsertTest | 7 | 0.5 |
| AdminProgramDetailRenderTest | 3 | 1.0 |
| AdminProgramEligibilityFormRenderTest | 6 | 0.8 |
| AdminProgramEligibilityServiceTest | 13 | 0.05 |
| AdminProgramFormRenderTest | 4 | 1.2 |
| AdminProgramFormServiceTest | 15 | 0.5 |
| AdminProgramImageServiceTest | 8 | 0.5 |
| AdminProgramListRenderTest | 5 | 4.0 |
| AdminProgramServiceTest | 11 | 0.7 |
| AdminSecurityTest | 6 | 1.5 |
| AdminTermFormRenderTest | 6 | 2.1 |
| AdminTermServiceTest | 14 | 0.2 |
| ApplicationCompleteControllerTest | 1 | 18.7 |
| AdminSeedInitializerTest | 3 | 1.2 |
| SecurityConfigAdminAccessTest | 4 | 1.5 |
| NotificationRepositoryTest | 1 | 2.2 |
| UserServiceNotificationChannelsTest | 2 | 1.6 |
| UserServicePhoneVerifiedResetTest | 3 | 0.05 |
| UserServiceSignUpTermsTest | 4 | 3.0 |
| UserUpdateProfileMutateTest | 2 | 16.8 |
| HomeControllerTest | 1 | 10.9 |

**합계 약 200 TC / 31 클래스 / 실패 0 / 오류 0**

### 미이행 (spec §6 정적 검증 요구 — 신설 필요)

**아래 신설 테스트 파일이 커밋에 포함되지 않음:**

| spec 요구 | 실제 파일 | 상태 |
|---|---|---|
| `AdminUserServiceTest` (safeguard 3종·차단·재활성화·role 변경·검색) | 없음 | **FAIL — 미신설** |
| `AdminUserControllerTest` (@WebMvcTest RBAC 401/403·CSRF) | 없음 | **FAIL — 미신설** |
| `AdminUsersRenderTest` (list/detail 렌더) | 없음 | **FAIL — 미신설** |
| `V16MigrationTest` (컬럼 추가·ADMIN→CENTER_ADMIN UPDATE) | 없음 | **FAIL — 미신설** |
| `UserPrincipalIsEnabledTest` (isActive 반영) | 없음 | **FAIL — 미신설** |
| `AuthenticationSuccessHandlerTest` (REQUIRES_NEW · 실패 시 로그인 무영향) | 없음 | **FAIL — 미신설** |
| `DataInitializerIdempotentTest` (isActive default TRUE) | `AdminSeedInitializerTest` (기존) | **PARTIAL — 기존 테스트가 일부 커버** |

**정적 검증 판정: 회귀 무영향 ✅ / spec 요구 자동화 누락 ❌**

---

## 2. 동적 검증 (curl · bootRun e2e · port 8090)

### 로그인·접근제어

| 시나리오 | 기대 | 실측 | 판정 |
|---|---|---|---|
| SYSTEM_ADMIN 로그인 | 302 → `/admin` | 302 → `/admin` | ✅ |
| CENTER_ADMIN 로그인 | 302 → `/admin` | 302 → `/admin` | ✅ |
| CENTER_ADMIN 로 `/admin/users` 접근 | 403 | 403 | ✅ (@PreAuthorize SYSTEM_ADMIN) |
| SYSTEM_ADMIN 로 `/admin/users` 접근 | 200 | 200 | ✅ |

### 목록 `/admin/users`

| 항목 | 기대 (spec §4 컬럼 스키마) | 실측 | 판정 |
|---|---|---|---|
| status | 200 | 200 | ✅ |
| Thymeleaf 표현식 잔존 (`${`) | 0건 | 0건 | ✅ |
| `th:*` 속성 잔존 | 0건 | 0건 | ✅ |
| 컬럼 헤더 (`admin-user-col-*`) | 9종 (no·name·email·gender·role·phone·join·last·status) | `no, name, email, gender, role, phone, join, last, status` = 9종 | ✅ |
| Row 개수 (default page=0) | 10 | 10 | ✅ (Qn-5) |
| Role filter | 4옵션 (전체·SYSTEM_ADMIN·CENTER_ADMIN·USER) | `href="/admin/users?q="` + `role=SYSTEM_ADMIN` + `role=CENTER_ADMIN` + `role=USER` = 4 | ✅ |
| 페이지네이션 링크 | 존재 | `page=0..4` + prev/next 확인 | ✅ |
| 검색 (`?q=seed30`) | matching row 반환 | 1 매치 row | ✅ |
| 필터 (`?role=SYSTEM_ADMIN`) | sysadmin 만 | 1 매치 row (sysadmin) | ✅ |
| 페이지 (`?page=2`) | 200 + row | 200 + 10 row | ✅ |

### 상세 `/admin/users/{id}`

| 항목 | 기대 | 실측 | 판정 |
|---|---|---|---|
| status | 200 | 200 | ✅ |
| Thymeleaf 잔존 | 0 | 0 | ✅ |
| 4탭 링크 (ALL/APPROVED/REJECTED/CANCELLED) | 모두 존재 | 4탭 확인 | ✅ |
| tab 필터 (`?tab=APPROVED`) | 200 + active | 200 + tab--active | ✅ |
| Danger zone (차단 CTA) | 존재 (deviation 수용) | `admin-user-danger` 14 매치 | ✅ |
| 권한 radio 3종 (SYSTEM_ADMIN/CENTER_ADMIN/USER) | SYSTEM_ADMIN 만 노출 | 3 radio + role change form | ✅ |

### 액션 왕복

| 시나리오 | 기대 | 실측 | 판정 |
|---|---|---|---|
| `POST /admin/users/53/deactivate` (정상 사유) | 302 → `/admin/users/53` + flash success + isActive=false | 302 + `사용자를 차단했어요.` + `status-badge--inactive`/`차단` | ✅ |
| 차단된 유저의 `/login` 시도 | 302 → `/login?blocked` (DisabledException 분기) | 302 → `/login?blocked` | ✅ **회귀 방어 최우선 항목** |
| 활성 유저의 `/login` 시도 | 302 → `/` | 302 → `/` | ✅ **정상 로그인 회귀 없음** |
| `POST /admin/users/53/reactivate` | 302 + flash success + isActive=true | 302 + `사용자를 재활성화했어요.` + `status-badge--active`/`활성` | ✅ |
| Reset endpoint `POST /__test__/reset-users` | 204 + 전건 isActive=TRUE | 204 + 재활성화 확인 (재로그인 성공) | ✅ |
| `POST /admin/users/53/role` (role=CENTER_ADMIN) | 302 + flash + role 변경 | 302 + `권한을 변경했어요.` + `CENTER_ADMIN 관리자` badge | ✅ |
| `POST /admin/users/53/admin-note` | 302 + flash | 302 + `관리자 메모를 저장했어요.` | ✅ |

### Safeguard 검증

| Safeguard | 시나리오 | 실측 | 판정 |
|---|---|---|---|
| ① 자기 자신 차단 X | `POST /admin/users/1/deactivate` (sysadmin=id 1) | 302 + `본인 계정은 차단할 수 없어요.` flash | ✅ |
| ① 자기 자신 role X | `POST /admin/users/1/role` (self) | 302 + `본인 계정의 권한은 변경할 수 없어요.` flash | ✅ |
| ② 마지막 SYSTEM_ADMIN 강등 X | 시드 SYSTEM_ADMIN 1명뿐이라 self 방어에 먼저 걸림. **단독 커버는 자동 테스트 필요** | curl 재현 불가 (self 방어와 동시 작용) | ⏸ UNVERIFIED (자동 테스트 이월) |
| ③ SYSTEM_ADMIN 승격은 SYSTEM_ADMIN 만 | Controller `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` 로 이중 방어. CENTER_ADMIN → `/admin/users` 접근 자체가 403 | 403 확인 | ✅ (경로 자체 차단으로 본 조건 충족) |

### 정적 리소스

| 경로 | 기대 | 실측 |
|---|---|---|
| `/css/main.css` | 200 | 200 ✅ |
| `/css/admin.css` | 200 | 200 ✅ |
| `/images/logo_symbol.png` | 200 | 200 ✅ |

**동적 검증 판정: PASS ✅ (Safeguard ② 는 자동 테스트 이월 필요)**

---

## 3. 계약 검사 (spec §0 · §6 요구)

### 신설 요구 (Qn-7 B — 3종)

| spec 요구 | 실제 상태 | 판정 |
|---|---|---|
| `docs/design-contracts/admin/users.md` | **없음** | **FAIL — 미신설** |
| `docs/design-contracts/admin/user-detail.md` | **없음** | **FAIL — 미신설** |
| `e2e/contracts/admin-users.ts` | **없음** | **FAIL — 미신설** |
| `e2e/contracts/admin-user-detail.ts` | **없음** | **FAIL — 미신설** |
| `e2e/contracts/admin-user-register.ts` (Qn-7 B) | **없음** — 단 레지스터 화면은 spec §12 스코프에서 "기존 사용자 승격만" 로 축소되었으므로 등록 폼 자체 이월 대상. 당초 §7 Qn-7 B 는 §12 축소로 부분 무효화 | ⏸ **DEFERRED** (스코프 축소 반영) |

**계약 검사 판정: FAIL ❌ (users.md · user-detail.md · admin-users.ts · admin-user-detail.ts 4종 미신설)**

prototype L1166~1272 / L1774~1943 정량 인용이 계약에 존재하지 않아, 이후 갭 자동 검출이 불가능하다. CLAUDE.md "계약이 있는 화면은 갭 0 이 완료 기준" 원칙을 만족하지 못한다.

---

## 4. 기능 E2E (Playwright)

### 요구 (spec §6 · §7 Qn-Δ)

| spec 요구 | 실제 파일 | 판정 |
|---|---|---|
| `e2e/tests/admin-users.spec.ts` (검색·필터·페이지네이션·차단·재활성화·safeguard 3종) | **없음** | **FAIL — 미신설** |
| `e2e/tests/admin-user-role-change.spec.ts` | **없음** | **FAIL — 미신설** |
| `e2e/tests/admin-user-signup-regression.spec.ts` (signup→login→차단→로그인 실패→재활성화→로그인 성공 왕복) | **없음** | **FAIL — 미신설** |
| `e2e/tests/visual-admin-users.spec.ts` | **없음** | **FAIL — 미신설** |

**기능 E2E 판정: FAIL ❌** — Interaction 검증 (CLAUDE.md "인터랙션 검증 — 클릭·상태 전환은 기능 E2E 필수", 2026-08-13 신설) 원칙 위반.

동적 curl 로 왕복 시나리오는 검증되었으나 회귀 박제 (Playwright spec) 가 없어 다음 PR 에서 회귀 발생 시 자동 탐지 불가.

---

## 5. 회귀 검증 (최우선)

### UserPrincipal.isEnabled 파급 (spec §5)

| 회귀 영역 | 검증 방식 | 실측 | 판정 |
|---|---|---|---|
| signup flow | 소스 검토 — isActive default `true` (User 엔티티 · V16 SQL) | User.java 는 도메인 메서드 `deactivate/reactivate` 만 노출. signup 경로에서 isActive 를 조작하는 코드 없음 | ✅ |
| login flow (활성 유저) | curl login (seed30) | 302 → `/` | ✅ |
| login flow (차단 유저) | curl login (seed49 · 차단됨) | 302 → `/login?blocked` | ✅ (DisabledException 분기) |
| apply flow | AdminApplicationServiceTest 24 TC PASS · ApplicationCompleteControllerTest PASS | 회귀 없음 | ✅ |
| lastAccessAt 갱신 실패 시 무영향 | 소스 검토 — `LastAccessAuthenticationSuccessHandler` 는 try/catch + REQUIRES_NEW. `Updater.updateLastAccess` 는 별도 컴포넌트 (프록시 자기 호출 회피). `chainedSuccessHandler` 는 예외 삼킴 후 redirect 진행 | 코드 의도 정합. **단독 자동 테스트 부재 (AuthenticationSuccessHandlerTest 미신설)** | ⚠️ 코드 검토 통과, 자동 테스트 이월 |
| withdraw flow (하드 삭제) 무손실 | UserService.withdraw 는 이 커밋에서 미변경. spec §12 에서 명시적으로 분리 | 이 커밋 diff 15 파일에 UserService.java 없음 | ✅ |
| admin/dashboard·programs·notices·terms 무회귀 | curl 실측 | `/admin` `/admin/programs` `/admin/notices` `/admin/terms` 모두 200 | ✅ |
| 기존 admin 테스트 (26 클래스) | test task PASS | failures=0 errors=0 | ✅ |
| DataInitializer.seedAdmins 멱등 | AdminSeedInitializerTest 3 TC PASS | 회귀 없음 | ✅ |

### UserRole.ADMIN 참조 잔존 (spec §5)

Qn-17 A — enum 소스 제거는 후속 티켓. V16 은 데이터 UPDATE 만 수행. 소스에는 `UserRole.ADMIN` 참조가 남아있을 수 있음 (spec 예상). **검증 필요**:

<확인>: `AdminUserService.roleLabel(...)` 의 switch 에 `case ADMIN, CENTER_ADMIN -> "관리자"` 존재. `UserRole.ADMIN` enum 값을 계속 사용 중이며 safeguard 의도와 정합. Qn-17 후속 티켓 대비 정합.

**회귀 검증 판정: PASS ✅** (프로덕션 코드 회귀 0건)

---

## 6. 시각 확인 (사용자 영역)

curl 로 커버 불가한 영역만 남김:

| 항목 | 확인 방법 | 상태 |
|---|---|---|
| 색·폰트·spacing (role-badge/status-badge/danger-zone 색상) | 브라우저 실측 필요 (roleCfg L2906~2910: 시스템 관리자 bg `#FEF3C7` color `#B45309` · 관리자 bg `#E5E1FB` color `#3428CF` · 사용자 bg `#F0EFF3` color `#475569`) | ⏸ 사용자 영역 |
| 반응형 (모바일/태블릿) | 실기기 확인 | ⏸ 사용자 영역 |
| 2컬럼 좌 프로필/우 신청 이력 4탭 레이아웃 시각 | brwoser 실측 | ⏸ 사용자 영역 |
| Flash message 애니메이션·색감 | 브라우저 실측 | ⏸ 사용자 영역 |

---

## 종합 판정

| 영역 | 결과 |
|---|---|
| 정적 (컴파일·기존 회귀) | ✅ PASS (31 클래스, ~200 TC PASS) |
| 정적 (spec 요구 신설 테스트 7종) | ❌ **FAIL — 미신설** |
| 동적 (curl 왕복) | ✅ PASS (14 시나리오 모두 통과) |
| 계약 (users.md/user-detail.md + admin-users.ts/admin-user-detail.ts) | ❌ **FAIL — 미신설** |
| 기능 E2E (4 spec) | ❌ **FAIL — 미신설** |
| 회귀 방어 (isActive·lastAccessAt·기존 admin·apply·signup) | ✅ PASS |
| 시각 (사용자 영역) | ⏸ 대기 |

**최종 판정: CONDITIONAL PASS**

- **프로덕션 코드**: 회귀 0건. spec 확정된 결정 (Qn-A 이월 · Qn-B 페이지 · Qn-C 유지 · Safeguard 3종 · V16 default TRUE · REQUIRES_NEW + try/catch · DisabledException `?blocked` 분기) 이 모두 소스·동적 검증에서 정합.
- **테스트/계약/E2E 자산**: spec §0 · §6 · §7 Qn-Δ 요구인 **자동화 자산 (테스트 7종 + 계약 4종 + E2E 4종 = 15종) 이 전혀 신설되지 않음**.

### 재반려 여부

**프로덕션 코드는 재반려하지 않음** — 회귀 발견 없음 · 동적 검증 왕복 모두 통과.

**test-only 이월 필요** (ym-impl 이 아니라 test-only 후속 작업으로 보완 가능한 범위):
1. `AdminUserServiceTest` (safeguard 3종 자동화 · 특히 Safeguard ② 마지막 SYSTEM_ADMIN 은 curl 로 재현 불가)
2. `AdminUserControllerTest` (@WebMvcTest RBAC)
3. `AdminUsersRenderTest` (list/detail 렌더)
4. `V16MigrationTest`
5. `UserPrincipalIsEnabledTest`
6. `AuthenticationSuccessHandlerTest` (**최우선** — 회귀 방어 3점 중 1 개 · curl 로 불가)
7. 계약 4종 (users.md · user-detail.md · admin-users.ts · admin-user-detail.ts)
8. 기능 E2E 4 spec

### 처리 권고

옵션 A (권장): 본 QA 리포트 첨부 후 커밋 · 후속 `chore/A5-admin-users-tests` 티켓으로 테스트·계약·E2E 15종 일괄 신설 작업.

옵션 B: ym-impl 재작업 (spec §6 요구 자동화 자산 미이행 사유). 이 경우 `df63d08` 은 폐기하지 않고 test-only 커밋 추가로 확장.

**사용자 결정 필요.**

---

## 부록 — 실측 원본

### bootRun 프로파일

- 기동: `.claude/scripts/bootrun-e2e.cmd` (H2 in-memory + seed · 포트 8090)
- 로그: `logs/bootrun-e2e-qa.log` (2359+ line)

### 실측 curl 커맨드 (재현용)

```bash
# 관리자 로그인
curl -c cookies.txt http://localhost:8090/admin/login -o loginpage.html
TOKEN=$(grep -oE 'name="_csrf" value="[^"]+"' loginpage.html | head -1 | sed 's/.*value="//; s/"$//')
curl -b cookies.txt -c cookies.txt -X POST http://localhost:8090/admin/login \
  --data-urlencode "username=sysadmin@youth-moa.test" \
  --data-urlencode "password=Admin!234" \
  --data-urlencode "_csrf=$TOKEN"

# deactivate
curl -b cookies.txt -X POST http://localhost:8090/admin/users/53/deactivate \
  -H "Content-Type: application/x-www-form-urlencoded; charset=UTF-8" \
  --data-urlencode "reason=QA verification test" \
  --data-urlencode "_csrf=<csrf>"

# blocked user login
curl -b cookies.txt -X POST http://localhost:8090/login \
  --data-urlencode "username=seed49@youth-moa.test" \
  --data-urlencode "password=Test1234!" \
  --data-urlencode "_csrf=<csrf>"
# → 302 Location: /login?blocked

# reset
curl -X POST http://localhost:8090/__test__/reset-users
# → 204
```

### 실측 flash 메시지

- `사용자를 차단했어요.`
- `사용자를 재활성화했어요.`
- `권한을 변경했어요.`
- `관리자 메모를 저장했어요.`
- `본인 계정은 차단할 수 없어요.`
- `본인 계정의 권한은 변경할 수 없어요.`

### 실측 상태 뱃지 클래스

- 활성: `admin-user-status-badge--active` + text `활성`
- 차단: `admin-user-status-badge--inactive` + text `차단`

### 실측 role badge

- SYSTEM_ADMIN → text `시스템 관리자`
- CENTER_ADMIN → text `관리자`
- USER → text `사용자`

### CSRF 인코딩 사고 (재현 방지 노트)

curl `--data-urlencode` 에 한글 값을 넣을 때 Git Bash 로케일이 non-UTF-8 이면 Tomcat 이 `InvalidParameterException: Character decoding failed. Parameter [reason] with value [QA ����] has been ignored` 를 던지며 400 반환. **재현 시 ASCII-only 사유 사용** 또는 `-H "Content-Type: application/x-www-form-urlencoded; charset=UTF-8"` 명시.
