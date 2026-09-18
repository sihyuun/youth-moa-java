# 작업 명세: A5-1 — admin-staff-management (관리자 계정 관리 · 신규 발급)

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed`** (2026-09-18 사용자 결정: QC → 옵션 B (별도 화면 X · `/admin/users/new` 편입 · prototype L1946~2045 정합) · QA/QB/QD + 세부 원안 A) |
| 브랜치 | `feature/A5-1-admin-staff-management` |
| 스코프 | `/admin/staff` 관리자 계정 관리 별도 화면 (SYSTEM_ADMIN 전용) + `/admin/staff/new` 신규 관리자 계정 발급 (초기 password 자동생성 + 강제 변경 flag) + `/admin/staff/{id}/reset-password` 임시 password 리셋. **A5 §12 Qn-A · Qn-4 이월 항목 해소**. Bulk 발급은 §7 QD 이월. |
| 선행 | ✅ A5 admin-users (#213 · V16 · isActive · AdminUserSafeguard 3종 · UserPrincipal.isEnabled · AuthenticationSuccessHandler) · ✅ A8 admin-bulk-csv (#218 · AdminUserBulkService · Safeguard 개별+bulk 공유) |
| 후행 | A5-2 관리자 프로필 편집 (D1 이월) · A7 SmtpMailSender (초기 password 이메일 발송 · Qn-7 이월) · MFA/SSO (원거리 이월) |
| 마스터 지시서 | `ADMIN-00-master-directive.md` §5-A5 (L243~248) · Q9 role 재편 · §8 결정 기록 |
| 상위 spec | `A5-admin-users.md` §12 Qn-A (이월) · §12 Qn-4 (이월) · §9 deferred "관리자 신규 계정 대량 발급 UI" |
| prototype | `admin/prototype.html` L1946~2045 (**신규 사용자 등록 폼** — 관리자 발급 UI 로 재해석) · L1822~1839 (role radio 3종 — 승급용) · L2906~2910 (roleCfg 뱃지 색상). **`/admin/staff` 별도 화면 prototype 없음** — A5 목록 화면(L1166~1272)을 role filter 로 축약한 형태로 재활용 |
| main 기준 | `21fd503` (A1~A8 완결) |
| 예상 규모 | **중** — 파일 15~22 · 순증 1,200~1,700 LOC · V19 마이그레이션 (must_change_password + password_changed_at + invited_by 3필드) · endpoint 3~4개 (`GET /admin/staff` · `GET /admin/staff/new` · `POST /admin/staff` · `POST /admin/staff/{id}/reset-password`) · 계약 2종 신설 (`admin-staff-list.ts` · `admin-staff-new.ts`) |

---

## 0. 계약 확인 (0단계)

- **본 화면 계약**: **없음** — 이번 티켓에서 함께 신설
  - `docs/design-contracts/admin/staff-list.md` — SYSTEM_ADMIN 전용 · 관리자 목록 (SYSTEM_ADMIN + CENTER_ADMIN 만) · 마지막 로그인 · 소속 센터 · 담당 프로그램 수 · 액션 (role 변경 링크 · 비밀번호 리셋 · 차단)
  - `docs/design-contracts/admin/staff-new.md` — 신규 관리자 계정 발급 폼 (Email · Name · Role radio 2종 · Center · 초기 password 자동생성 안내) · flash 로 초기 password 1회 노출
  - `e2e/contracts/admin-staff-list.ts` · `e2e/contracts/admin-staff-new.ts` — 셀렉터·컬럼·CTA 라벨 · `proto:` L1946~2045 · L1214~1244 (users list grid 재활용) 인용
- **admin 공통 정책**: `docs/design-contracts/admin/POLICY.md` (다크 헤더 · 인디고 primary · 존댓말) 승계
- **재활용 계약**: `admin-users.ts` (A5 검색·필터·페이지네이션·role badge 패턴) · `admin-user-detail.ts` (프로필 폼 · danger zone · role radio 저장 패턴) · `admin-programs.ts` (테이블 grid 패턴)
- **prototype 우선순위**:
  - **`/admin/staff` 별도 화면 prototype 없음** — 사용자 요구로 신설. A5 목록 grid 를 role filter (SYSTEM_ADMIN + CENTER_ADMIN 만) 로 필터링한 후 컬럼을 관리자 특성 (소속 센터 · 담당 프로그램 수 · 마지막 로그인) 로 조정 → **prototype 이탈. `deviation: 'A5 목록 UI 재활용 · 관리자 특성 컬럼으로 재구성 (prototype 미명시)'`**
  - L1946~2045: 신규 사용자 등록 폼 — 관리자 발급 UI 로 재해석. **password 필드 제거 후 "자동생성" 안내 문구로 대체** (Qn-4 A안: 자동생성 + 강제 변경)
  - L1822~1839: role radio 3종 — 관리자 발급 시 role 은 CENTER_ADMIN/SYSTEM_ADMIN 2종만 노출 (USER 제거)

---

## 1. 디자인 출처 (3자산 병렬 정독)

| 자산 | 인용 | 결론 |
|---|---|---|
| `admin/prototype.html` L1946~2045 | 신규 사용자 등록 폼: email + 중복확인 + password + confirm + name + gender + role radio 3종 + birth + phone + address | **재해석**: 관리자 발급 폼은 password 입력 제거 · role radio 2종 (USER 제거) · gender/birth/phone/address 선택 사항으로 격하 (관리자 계정은 최소 필드만) |
| L1822~1839 | 사용자 상세 role radio 3종 (사용자/관리자/시스템 관리자) | 발급 시 role 2종 (관리자 · 시스템 관리자) 만 노출 |
| L2906~2910 (roleCfg) | 시스템 관리자 bg `#FEF3C7` color `#B45309` · 관리자 bg `#E5E1FB` color `#3428CF` | 뱃지 색상 A5 승계 |
| L1214~1244 (A5 목록 grid) | 10열 grid (checkbox · No · 이름 · 이메일 · 성별 · 권한 · 핸드폰 · 가입일 · 최근접속일 · 상태) | **재구성**: staff-list grid = No · 이름 · 이메일 · 권한 · **소속 센터** · **담당 프로그램 수** · 가입일 · 마지막 로그인 · 상태 · 액션. 체크박스 제외 (bulk 는 QD 이월) |
| ADMIN-00 §5-A5 (L243~248) | 사용자 관리 원안. 관리자 계정 관리는 별도 언급 없음 | A5-1 이 신설 소관 |
| ADMIN-00 §8 Q9 | ADMIN → CENTER_ADMIN 마이그레이션 완료 (A5 V16) · enum 소스 제거는 후속 chore | A5-1 무관 (이미 정리됨) |
| A5 §12 Qn-A 결정 | "선택형 / 이월. `/admin/staff` 별도 화면 **미신설**. 향후 대량 관리자 발급 유즈케이스가 실질적으로 발생하면 후속 티켓 A5-1 로 신설" | **A5-1 정당성 · 신설 착수** |
| A5 §12 Qn-4 결정 | "기존 사용자 승격만. 신규 관리자 계정 발급 UI 는 이월" | **A5-1 이 신규 발급 flow 신설 착수** |
| `AdminUserController.java` L143~156 | `POST /admin/users/{id}/role` (SYSTEM_ADMIN 전용 · Safeguard 3종 적용) 기존재 | **재활용 · 신설 X**. A5-1 은 staff 목록에서 이 endpoint 로 링크 |
| `AdminUserSafeguard.java` L28~75 | `assertNotSelf` · `assertNotLastSystemAdmin` · `assertCanChangeRole` · `assertCanDeactivate` · `assertCanReactivate` | **재활용 3종 · 신설 X**. 신규 발급 시 role=SYSTEM_ADMIN 발급 권한 검사만 재활용 (assertCanChangeRole 확장 or 새 메서드) |
| `DataInitializer.seedAdmins()` L171~211 | SYSTEM_ADMIN 1 (`sysadmin@youth-moa.test`) + CENTER_ADMIN 2 (`existsByEmail` 멱등 · `assignRole()`) | **무회귀 · 무변경**. 시드 계정 email 은 A5-1 발급 화면에서도 중복 검증 대상 |
| `UserService.signUp()` L180~208 | 일반 사용자 회원가입 · phoneVerified 파라미터 · 약관 동의 필수 · UserRole.USER 고정 | **재활용 불가**. 관리자 발급은 약관 동의 불필요 · role 파라미터 수용 필요 → 별도 `AdminStaffService.create()` 신설 |
| `User.java` (A5 반영 후) | isActive · lastAccessAt · adminNote · deactivatedAt · deactivatedBy · deactivationReason 필드 존재. **mustChangePassword · passwordChangedAt · invitedBy 부재** | **V19 3필드 신설 결정 필요** — Qn-3 |
| `UserPrincipal.isEnabled()` (A5 반영) | `user.isActive()` 반영됨 | **무회귀**. mustChangePassword 는 별도 flag (isEnabled 무관) — 로그인 후 password 변경 강제 리다이렉트로 처리 |

### 1-A. 자산 간 갭 (형태 vs 존재 축)

| 항목 | wireframe | prototype | ADMIN-00 / A5 이월 | 축 | 채택 |
|---|---|---|---|---|---|
| `/admin/staff` 별도 화면 | 명시 없음 | 없음 | A5 §12 Qn-A 이월 → A5-1 신설 | 존재+형태 | ⚠️ **QC 결정** — A안 (신설 · 사용자 요구 · deviation) vs B안 (A5 목록 재활용 유지 · A5-1 취소) |
| 신규 관리자 계정 발급 UI | 명시 없음 | L1946~2045 (사용자 등록 · role radio 3종 포함) | A5 §12 Qn-4 이월 → A5-1 신설 | 존재 | **신설** (prototype 은 재해석 · password 자동생성으로 변경) |
| 초기 password 정책 | 명시 없음 | password 필드 수동 입력 | A5 §12 결정 없음 | 존재 | ⚠️ **QA 결정** — A안 (자동생성 + 강제 변경 · 권장) vs B안 (관리자 수동 입력) |
| Email invitation flow | 명시 없음 | 없음 | 없음 | 존재 | ⚠️ **QB 결정** — A안 (즉시 활성 · 권장 · A7 SMTP 이월) vs B안 (invitation 링크 · SMTP 필요 · Qn-7 세트) |
| Bulk CSV 발급 | 명시 없음 | 없음 | A5 §9 deferred "대량 발급 UI" | 존재 | ⚠️ **QD 결정** — A안 (이월 · 권장) vs B안 (편입) |
| 관리자 특성 컬럼 (소속 센터 · 담당 프로그램 수 · 마지막 로그인) | 명시 없음 | A5 grid 재활용 | 사용자 요구 | 형태 | **재구성 신설** (deviation) |
| Password 리셋 (임시 재발급) | 명시 없음 | A5 detail L1789~1801 (관리자가 비밀번호 재설정 · A5 Qn-11 A안 채택) | A5 반영 | 존재 | **staff 화면 별도 CTA + 자동생성 통일** (A5 profile 편집은 A5-2 이월 상태 → staff 는 즉시 신설) |
| Role 변경 (SYSTEM_ADMIN ↔ CENTER_ADMIN) | 명시 없음 | A5 detail L1822~1839 | A5 기존재 | 형태 | **A5 `/admin/users/{id}/role` 재활용** (staff 목록 액션에서 링크) |
| 자기 자신 role 변경 방지 | 명시 없음 | 없음 | A5 Safeguard 1 | 존재 | **재활용** |
| 마지막 SYSTEM_ADMIN 강등 방지 | 명시 없음 | 없음 | A5 Safeguard 2 | 존재 | **재활용** |
| SYSTEM_ADMIN 승격 권한 (SYSTEM_ADMIN 만) | 명시 없음 | 없음 | A5 Safeguard 3 | 존재 | **재활용** + 신규 발급에도 적용 (`assertCanCreate(role)` 신설 or `assertCanChangeRole` 확장) |
| 최초 로그인 시 password 변경 강제 | 명시 없음 | 없음 | 없음 | 존재 | ⚠️ **Qn-3 결정** — mustChangePassword flag 신설 여부 |

### 1-B. 데이터 모델 gap 표 (필수)

A8 완료 후 스키마 (V18) 기준. **A5-1 신설 대상**.

| prototype/요구 필드 | 현재 스키마 (V18) | 조치 |
|---|---|---|
| Email · Name · Phone · Address · Gender · BirthDate | `users` 컬럼 ✓ (A5 승계) | 발급 폼에서 최소 필드만 사용 (email · name · role · center) |
| Role (CENTER_ADMIN / SYSTEM_ADMIN) | `users.role` ENUM ✓ | 발급 시 2종만 노출 |
| 소속 센터 | `users.center_id` FK ✓ (A5 기존재 · assignRole 시 세팅) | 조회 시 `@EntityGraph center` |
| 담당 프로그램 수 | ❌ 없음 (파생) | `ProgramRepository.countByCenter(centerId)` 파생 · CENTER_ADMIN 만 |
| 마지막 로그인 | `users.last_access_at` ✓ (A5 승계) | 재활용 |
| 초기 password (자동생성) | `users.password` ✓ | `SecureRandomPasswordGenerator` 신설 · bcrypt 저장 |
| **최초 로그인 시 password 변경 강제 flag** | ❌ 없음 | ⚠️ Qn-3 채택 시 **V19: `users.must_change_password BOOLEAN NOT NULL DEFAULT FALSE`** + `PasswordChangeRequiredInterceptor` 신설 (모든 요청 → `/mypage/password` 리다이렉트 · 자기 자신 로그아웃 제외) |
| **password 마지막 변경 시각 (감사)** | ❌ 없음 | ⚠️ Qn-3 채택 시 **V19: `users.password_changed_at TIMESTAMP NULL`** |
| **누가 발급했는가 (감사)** | ❌ 없음 | ⚠️ Qn-3 채택 시 **V19: `users.invited_by BIGINT NULL REFERENCES users(id)`** |

**최소 안 (Qn-3 A안 = V19 3필드 신설 · 권장)**:

```sql
-- V19__admin_staff_password_flags.sql
-- A5-1: 관리자 초기 password 자동생성 + 강제 변경 flag + 감사
ALTER TABLE users
  ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN password_changed_at TIMESTAMP NULL,
  ADD COLUMN invited_by BIGINT NULL REFERENCES users(id);

-- 기존 사용자 password_changed_at 초기값: NULL (unknown · GMP 유사 원칙상 null 허용)
-- 신규 발급 시 must_change_password=TRUE + password_changed_at=NULL
-- 자체 password 변경 시 must_change_password=FALSE + password_changed_at=now()
CREATE INDEX idx_users_must_change_password ON users(must_change_password) WHERE must_change_password = TRUE;
```

### 1-C. 데이터 소비 지점 (write→read 왕복)

| 소비 지점 | prototype 참조 | 현재 상태 | 갭 |
|---|---|---|---|
| `/admin/staff` 목록 | 없음 (신설 재구성) | 컨트롤러 부재 | 신설 |
| `/admin/staff/new` 발급 폼 | L1946~2045 (재해석) | 컨트롤러 부재 | 신설 |
| `POST /admin/staff` 신규 발급 | 없음 | Service 부재 | 신설 (`AdminStaffService.create()`) |
| `POST /admin/staff/{id}/reset-password` | 없음 | Service 부재 | 신설 |
| flash 로 초기 password 1회 노출 | 없음 | flash 인프라 존재 (A4 승계) | 신설 |
| 최초 로그인 후 password 변경 화면 | 없음 | `/mypage/password` 기존재 (A7 설정 계정 탭 검토 필요) | Qn-3 채택 시 강제 리다이렉트 인터셉터 추가 |
| `/admin/users` A5 목록 | L1166~1272 | ✓ (A5 완료) | **무회귀 · role filter 4옵션 유지** |
| `POST /admin/users/{id}/role` A5 | L1822~1839 | ✓ (A5 완료) | **staff 목록에서 링크 (재활용)** |
| DataInitializer.seedAdmins | 없음 | ✓ 멱등 | **무회귀 · 시드 email 은 발급 폼 중복 검증** |
| 로그인 flow (mustChangePassword 처리) | 없음 | `UserPrincipal.isEnabled` 만 반영 | 신설 (인터셉터 or Handler) |

---

## 2. 배경 · 스코프

### 포함 (A5-1)

- **`/admin/staff` 별도 화면 (Qn-A 해소)**: SYSTEM_ADMIN 전용
  - 관리자 계정 목록 (SYSTEM_ADMIN + CENTER_ADMIN 만)
  - 컬럼: No · 이름 · 이메일 · 권한 · 소속 센터 · 담당 프로그램 수 · 가입일 · 마지막 로그인 · 상태 · 액션
  - 검색 (이름 · 이메일) · 필터 (role · center · isActive) · 페이지네이션 10건
  - 액션 컬럼: role 변경 (`/admin/users/{id}` 링크 · 재활용) · 비밀번호 리셋 (신규 CTA) · 차단 (`/admin/users/{id}/deactivate` 링크 · 재활용)
- **관리자 신규 계정 발급 UI (Qn-4 해소)**: SYSTEM_ADMIN 전용
  - `GET /admin/staff/new` · `POST /admin/staff`
  - 필드: Email · Name · Role (CENTER_ADMIN / SYSTEM_ADMIN radio 2종) · Center (CENTER_ADMIN 시만 필수)
  - 초기 password 자동생성 (Qn-4 A안 · secure random 12자 · 영대·영소·숫자·특수문자 각 1자 이상)
  - 발급 후 flash 로 초기 password 1회 노출 + "관리자에게 안전 채널로 전달하세요" 안내
  - `mustChangePassword=TRUE` 세트 (Qn-3 A안)
- **비밀번호 리셋 (임시 재발급)**: SYSTEM_ADMIN 전용
  - `POST /admin/staff/{id}/reset-password`
  - 자동생성 password 로 교체 · `mustChangePassword=TRUE` 세트 · flash 로 새 password 1회 노출
- **강제 password 변경 인터셉터 (Qn-3 A안)**:
  - `mustChangePassword=TRUE` 사용자는 로그인 후 모든 요청 → `/mypage/password` 리다이렉트
  - 예외 경로: `/mypage/password` (POST · 변경 처리) · `/logout` · `/css/**` · `/images/**` · `/webjars/**` · `/login`
  - `/mypage/password` 성공 시 flag=FALSE + `passwordChangedAt=now()`

### 제외 (이월)

| 항목 | 이유 | 담당 |
|---|---|---|
| 관리자 role 상세 편집 | A5 개별 endpoint 재활용 | `/admin/users/{id}/role` (재활용) |
| 관리자 프로필 자체 편집 (본인 정보 수정) | A5-2 이월 (D1) | A5-2 |
| Email invitation flow | SMTP 인프라 필요 | A7 (Qn-B B안 채택 시) |
| SmtpMailSender 발송 (초기 password 이메일) | SMTP 인프라 필요 | A7 (Qn-7 이월) |
| **Bulk CSV 관리자 대량 발급** | 사용 빈도 낮음 · 원자성 · 실패 시 flash 처리 복잡 | ⚠️ **QD 이월 권장** (필요 시 A8-1 후속) |
| 관리자 활동 로그 dashboard (action history) | 별도 감사 테이블 필요 | 후속 티켓 |
| MFA · SSO | 인증 인프라 개편 | 원거리 이월 |

---

## 3. RBAC · 스코프

| 경로 | 접근 role | 스코프 |
|---|---|---|
| `GET /admin/staff` | **SYSTEM_ADMIN 만** | 전체 관리자 (SYSTEM_ADMIN + CENTER_ADMIN) |
| `GET /admin/staff/new` | **SYSTEM_ADMIN 만** | — |
| `POST /admin/staff` | **SYSTEM_ADMIN 만** | Safeguard: role=SYSTEM_ADMIN 발급도 SYSTEM_ADMIN 이 하는 것이므로 통과 (자기 자신 발급은 email 중복으로 자연 차단) |
| `POST /admin/staff/{id}/reset-password` | **SYSTEM_ADMIN 만** | 자기 자신 리셋 금지 (Qn-5) · 마지막 SYSTEM_ADMIN 리셋은 허용 (차단이 아니라 password 만 재발급) |

**@PreAuthorize 명시**: `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` — CENTER_ADMIN 은 403.

**Safeguard 재활용 (신설 X)**:
1. `assertNotSelf(admin, target)` — password 리셋 시 자기 자신 금지 (재활용)
2. `assertNotLastSystemAdmin(target)` — 관리자 차단은 A5 endpoint 로 재활용 (Safeguard 자동 적용)
3. `assertCanChangeRole(admin, target, newRole)` — role 변경은 A5 endpoint 로 재활용

**Safeguard 신규 (경량 1건)**:
- `assertCanCreateStaff(admin, newRole)` — 신규 발급 시 SYSTEM_ADMIN 만 SYSTEM_ADMIN 발급 가능. **A5 기존 `assertCanChangeRole` 로직 재활용** (target 이 없으므로 admin+newRole 만 검사하는 헬퍼 추출)

---

## 4. 엔티티·마이그레이션 (V19 단일)

### V19__admin_staff_password_flags.sql (Qn-3 A안 채택 시)

```sql
-- A5-1: 관리자 초기 password 자동생성 + 강제 변경 flag + 감사
-- 회귀 방어: DEFAULT FALSE 로 기존 row 무영향 · 일반 사용자 로그인 flow 무회귀
-- 참조: A5-1 §1-B

ALTER TABLE users
  ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN password_changed_at TIMESTAMP NULL,
  ADD COLUMN invited_by BIGINT NULL REFERENCES users(id);

CREATE INDEX idx_users_must_change_password ON users(must_change_password) WHERE must_change_password = TRUE;
```

### User 엔티티 신설 필드 + 도메인 메서드

```java
@Column(nullable = false, columnDefinition = "boolean not null default false")
private boolean mustChangePassword = false;

@Column private LocalDateTime passwordChangedAt;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "invited_by")
private User invitedBy;

/** A5-1: 관리자가 신규 발급 시 · 자동생성 password 세팅. */
public void assignInitialPassword(String encoded, User invitedBy) {
  this.password = encoded;
  this.mustChangePassword = true;
  this.passwordChangedAt = null;
  this.invitedBy = invitedBy;
}

/** A5-1: 관리자가 임시 password 재발급 시. */
public void resetPassword(String encoded) {
  this.password = encoded;
  this.mustChangePassword = true;
  this.passwordChangedAt = null;
}

/** A5-1: 본인이 password 변경 시 · 강제 변경 flag 해제. */
public void changePasswordSelf(String encoded) {
  this.password = encoded;
  this.mustChangePassword = false;
  this.passwordChangedAt = LocalDateTime.now();
}
```

### 회귀 방어 (UserPrincipal 무변경)

- `UserPrincipal.isEnabled()` 는 `user.isActive()` (A5 기 반영) 유지 · `mustChangePassword` 는 로그인 자체는 허용
- 강제 변경은 **HandlerInterceptor** 로 처리 — Spring Security 4가지 boolean 을 오염시키지 않음

### PasswordChangeRequiredInterceptor 신설

```java
@Component
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {
  private static final Set<String> ALLOWED = Set.of(
      "/mypage/password", "/logout", "/login"
  );
  private static final List<String> STATIC_PREFIXES = List.of("/css/", "/images/", "/js/", "/webjars/", "/error");

  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
    // 인증되지 않은 경우 스킵
    // 인증되었고 mustChangePassword=TRUE 이며 허용 경로가 아니면 302 → /mypage/password?force=1
    // /mypage/password 변경 성공 후 flag=FALSE 로 갱신됨
  }
}
```

---

## 5. 화면 · 라우팅

| 경로 | 뷰 | 설명 |
|---|---|---|
| `GET /admin/staff` | `admin/staff/list.html` | SYSTEM_ADMIN 전용 · 관리자 목록 · 검색·필터·페이지네이션 |
| `GET /admin/staff/new` | `admin/staff/new.html` | SYSTEM_ADMIN 전용 · 신규 발급 폼 |
| `POST /admin/staff` | flash + redirect `/admin/staff` | 신규 발급 · 초기 password flash 노출 |
| `POST /admin/staff/{id}/reset-password` | flash + redirect `/admin/staff` | 임시 password 재발급 · flash 노출 |

**재활용 endpoint (신설 X)**:
- `/admin/users/{id}` (프로필 상세 · A5)
- `POST /admin/users/{id}/role` (role 변경 · A5)
- `POST /admin/users/{id}/deactivate` (차단 · A5)
- `POST /admin/users/{id}/reactivate` (재활성화 · A5)

### staff-list 컬럼 스키마

| # | 라벨 | 폭 | 데이터 |
|---|---|---|---|
| 1 | No. | 36px | seq |
| 2 | 이름 | 110px | user.name (링크 → `/admin/users/{id}` 상세) |
| 3 | 이메일 | 1fr | user.email |
| 4 | 권한 | 110px | roleBadge (SYSTEM_ADMIN · CENTER_ADMIN 만) |
| 5 | 소속 센터 | 140px | user.center?.name ?? "-" (SYSTEM_ADMIN 은 "전체") |
| 6 | 담당 프로그램 | 100px | programCount (CENTER_ADMIN 만 · SYSTEM_ADMIN 은 "-") |
| 7 | 가입일 | 90px | createdAt (YYYY-MM-DD) |
| 8 | 마지막 로그인 | 90px | lastAccessAt (YYYY-MM-DD · NULL 시 "-") |
| 9 | 상태 | 72px | statusBadge (A5 재활용) |
| 10 | 액션 | 180px | "role 변경" · "비밀번호 리셋" · "차단/재활성화" 3링크 |

### staff-new 폼 필드

| 라벨 | 타입 | 필수 | 검증 | 비고 |
|---|---|---|---|---|
| 이메일 | textfield | ✓ | `@Email` + 중복 검증 | seedAdmins email 도 중복 대상 |
| 이름 | textfield | ✓ | `@NotBlank` `@Size(max=50)` | |
| 권한 | radio (CENTER_ADMIN · SYSTEM_ADMIN) | ✓ | `@NotNull` | SYSTEM_ADMIN 발급 가능은 Safeguard 로 재검증 |
| 소속 센터 | select | 조건부 (CENTER_ADMIN 시 필수) | `@AssertTrue` | SYSTEM_ADMIN 시 null 허용 |
| 초기 password | (숨김) | — | 자동생성 · 저장 X | 발급 후 flash 로 1회 노출 |

발급 후 flash 노출 예:
```
관리자 계정이 발급되었습니다.
이메일: {email}
초기 비밀번호: {tempPassword}

이 비밀번호는 이 화면에서만 확인 가능합니다. 안전한 채널로 전달하고, 관리자는 최초 로그인 후 즉시 비밀번호를 변경해야 합니다.
```

---

## 6. 회귀 방지 (최우선)

| 회귀 영역 | 감지 방법 | 조치 |
|---|---|---|
| **A5 사용자 관리 flow** | `AdminUserControllerTest` · `AdminUsersRenderTest` (기존 A5) 재실행 | `/admin/users` 목록·상세·role·차단 endpoint 무영향 (A5-1 은 별도 컨트롤러) |
| **A8 bulk · Safeguard** | `AdminUserBulkServiceTest` (기존 A8) 재실행 | `AdminUserSafeguard` 재활용 · 신설 헬퍼(`assertCanCreateStaff`)만 추가 |
| **DataInitializer.seedAdmins** | `DataInitializerIdempotentTest` (기존 A5) 재실행 | 시드 계정 (`sysadmin@youth-moa.test` · 2 CENTER_ADMIN) 무변경 · V19 3필드 default 값 반영 확인 (mustChangePassword=FALSE) |
| **UserService.signUp** | `UserServiceTest` 재실행 | 일반 사용자 회원가입 flow 무영향 · V19 3필드 default 값 반영 |
| **UserPrincipal.isEnabled** | `UserPrincipalIsEnabledTest` (A5) 재실행 | mustChangePassword 는 isEnabled 에 영향 없음 (별도 flag) |
| **일반 사용자 로그인 flow** | `SecurityIntegrationTest.login_regular_user_success` 신설 | mustChangePassword=FALSE 인 일반 사용자는 인터셉터 통과 |
| **강제 변경 인터셉터 · 시드 계정** | `PasswordChangeRequiredInterceptorTest` 신설 | seedAdmins 계정은 mustChangePassword=FALSE → 정상 로그인 · admin 페이지 접근 가능 |
| **강제 변경 인터셉터 · 신규 발급 계정** | 위 테스트 확장 | 발급 직후 로그인 → `/admin/staff` 접근 시 302 → `/mypage/password?force=1` |
| **강제 변경 후 flag 해제** | 위 테스트 확장 | `/mypage/password` 성공 후 mustChangePassword=FALSE · 다음 요청부터 정상 흐름 |
| **UserRole ENUM 파싱** | grep `UserRole.ADMIN` 참조 0건 (A5 chore 완료 전제) | A5-1 코드 내 신설 참조 금지 |
| **CENTER_ADMIN → CENTER 매핑** | `AdminStaffServiceTest.create_center_admin_without_center_rejected` | Center 필수 검증 |

**Spring Security 정합**: `HandlerInterceptor` 는 `WebMvcConfigurer.addInterceptors()` 로 등록 · `/admin/**` + `/mypage/**` + `/` 등에 적용. `/mypage/password` 자체는 exclude.

---

## 7. 결정 필요 항목 (Qn — 15건)

### 핵심 4

**QA: 초기 password 정책**
- **A안 (권장)**: 자동생성 (secure random 12자 · 영대·영소·숫자·특수문자 각 1자 이상) + `mustChangePassword=TRUE` + flash 로 1회 노출
- B안: 관리자가 수동 입력 (prototype L1963~1973 · A5 Qn-11 A안 동일 패턴)
- **판정**: A안. 실무·보안 관점 (관리자가 임의 password 를 입력할 시 재사용·유출 위험 · 최소 복잡도 미달 위험). prototype 이탈은 A5 §12 결정으로 이미 이월된 상태 · A5-1 에서 자동생성으로 통일

**QB: Email invitation vs 즉시 활성**
- **A안 (권장)**: 즉시 활성 · isActive=TRUE + mustChangePassword=TRUE + flash 로 password 노출 → 관리자가 안전 채널(직접 전달·기업 메신저) 로 password 공유
- B안: Email invitation (SmtpMailSender 필요) + 링크 클릭 후 초기 password 설정
- **판정**: A안. SMTP 인프라 (Qn-7) 는 A7 이월 상태 · A5-1 에서 SMTP 신설은 스코프 초과. B안은 후속 A7 SmtpMailSender 완료 후 확장

**QC: `/admin/staff` 별도 화면 vs A5 목록 재활용**
- **A안 (권장)**: `/admin/staff` 별도 화면 신설 · SYSTEM_ADMIN 전용 · 관리자 특성 컬럼 (소속 센터 · 담당 프로그램 수) 재구성
- B안: `/admin/users?role=SYSTEM_ADMIN,CENTER_ADMIN` 필터 재활용 · A5-1 취소 · 신규 발급만 A5 detail 폼에 통합
- **판정**: A안. A5 §12 Qn-A 사용자 결정 "향후 발생 시 A5-1 신설" 을 실현 · 관리자 특성 컬럼 (담당 프로그램 수) 은 A5 grid 로 표현 불가. **deviation 명시**: prototype 미명시 신설

**QD: Bulk CSV 관리자 대량 발급 편입 여부**
- **A안 (권장)**: **이월** · 개별 발급만 이번 티켓 · 대량 발급 실무 빈도 낮음 · 원자성 · flash 다건 노출 UX 복잡
- B안: 편입 · CSV 업로드 + AdminUserBulkService 재활용 + 실패 row 리포트
- **판정**: A안. A5-1 스코프 유지 · 필요 시 A8-1 후속. deferred: A8-1-admin-staff-bulk

### 세부 11

**Qn-1: 신규 발급 email 중복 검증 (시드 계정 포함 여부)**
- **A안 (권장)**: `existsByEmail` 전체 대상 · 시드 계정 (`sysadmin@youth-moa.test` · CENTER_ADMIN 2건) 중복 시 400 + "이미 사용 중인 이메일입니다."
- 판정: A안.

**Qn-2: password 자동생성 알고리즘**
- **A안 (권장)**: `SecureRandom` 기반 12자 · 영대·영소·숫자·특수문자(`!@#$%^&*`) 각 최소 1자 · 비속어 회피 X
- B안: 20자 · UUID 파생 · 특수문자 제외 (전달 편의)
- 판정: A안. 표준 정책 · 사용자 password 검증 규칙 (`@Pattern` 영문+숫자 8자 이상) 상회

**Qn-3: mustChangePassword flag + V19 3필드 신설 여부**
- **A안 (권장)**: **V19 신설 (must_change_password · password_changed_at · invited_by)** + PasswordChangeRequiredInterceptor
- B안: flag 없이 관리자 수동 안내로 대체 (compliance 낮음)
- **판정**: A안. 실무 · 감사 요구. B안은 임시 password 유출 시 방어 불가

**Qn-4: 계약 신설 세분화**
- **A안 (권장)**: 2종 — `admin-staff-list.ts` (목록) · `admin-staff-new.ts` (발급 폼)
- B안: 1종 통합 (`admin-staff.ts`)
- **판정**: A안. 목록·폼 grid 스키마 상이 · 재사용성 낮음

**Qn-5: password 리셋 자기 자신 금지 여부**
- **A안 (권장)**: **자기 자신 리셋 금지** (`assertNotSelf`) · 본인은 `/mypage/password` 로 자체 변경
- B안: 허용 (자기 자신 password 도 자동생성으로 교체)
- 판정: A안. Safeguard 1 재활용

**Qn-6: 마지막 SYSTEM_ADMIN 리셋 허용 여부**
- **A안 (권장)**: 허용 · 리셋은 차단이 아니라 password 재발급 (계정은 유지)
- B안: 금지 (Safeguard 2 확장)
- 판정: A안. Safeguard 2 는 차단·강등에만 적용

**Qn-7: SmtpMailSender 발송 신설 여부**
- **A안 (권장)**: **이월 (A7)** · 이번 티켓은 flash 로 password 노출만
- B안: 이번 티켓 신설 (JavaMailSender + spring-boot-starter-mail)
- 판정: A안. SMTP 서버 · 자격증명 인프라 필요 · 스코프 초과

**Qn-8: 관리자 활동 로그 (별도 뷰)**
- **A안 (권장)**: **이월** · adminNote 컬럼 (A5) 으로 임시 대체 · 별도 감사 테이블 후속
- B안: audit_log 테이블 신설
- 판정: A안. 스코프 유지

**Qn-9: 시드 관리자 계정 mustChangePassword 초기값**
- **A안 (권장)**: **FALSE** · 시드는 개발·테스트용 · 고정 password (`Test1234!` 등) 유지 · 변경 강제 시 매 재기동마다 인터셉터 발동
- B안: TRUE · 초기 로그인 시 password 변경 강제 (운영 유사)
- **판정**: A안. 시드 flow 회귀 방지 최우선

**Qn-10: 강제 password 변경 인터셉터 경로**
- **A안 (권장)**: `/mypage/password` 로 통일 · 사용자 기존 페이지 재활용 · `?force=1` 쿼리로 화면 문구 조정 (뒤로가기 CTA 숨김)
- B안: `/change-password` 신설 페이지
- 판정: A안. 신규 페이지 최소화

**Qn-11: `/mypage/password` 강제 변경 시 이전 password 검증 유지 여부**
- **A안 (권장)**: **이전 password 검증 우회** · 자동생성 password 를 사용자가 기억할 필요 없음 · `?force=1` 시 current password 필드 hidden 처리
- B안: 이전 password 검증 유지 (자동생성 password 를 직접 입력)
- 판정: A안. UX + 임시 password 재입력 오류 방지. 보안: 세션 인증 자체가 이미 통과

---

## 8. 테스트

### 정적

| 테스트 | 대상 |
|---|---|
| `AdminStaffServiceTest` | 목록 · 신규 발급 · 이메일 중복 · center 필수 (CENTER_ADMIN) · 리셋 · Safeguard 재활용 (SYSTEM_ADMIN 발급 권한) |
| `AdminStaffControllerRbacTest` (@WebMvcTest) | RBAC — CENTER_ADMIN 403 · 익명 401 · SYSTEM_ADMIN 200 · CSRF |
| `AdminStaffRenderTest` (SpringBootTest render) | 목록·신규 발급 폼 Thymeleaf 렌더 · 표현식 잔존 0건 · empty state · pagination · flash 노출 |
| `V19MigrationTest` (@FlywayTest) | 3컬럼 추가 · default 값 · index 생성 검증 |
| `PasswordGeneratorTest` | 자동생성 12자 · 4가지 문자 클래스 포함 · 100건 반복 시 중복 0 · 엔트로피 검증 |
| `PasswordChangeRequiredInterceptorTest` | 시드 계정 (flag=FALSE) 통과 · 발급 계정 (flag=TRUE) 리다이렉트 · 허용 경로 통과 · 변경 후 flag 해제 후 통과 |
| `DataInitializerIdempotentTest` (기존 A5) 확장 | seedAdmins 재실행 시 V19 신설 필드 default 반영 확인 |

### 동적 (curl · bootRun e2e)

- `GET /admin/staff` (SYSTEM_ADMIN) 200 + 관리자 목록 렌더
- `GET /admin/staff` (CENTER_ADMIN CSRF) 403
- `POST /admin/staff` (SYSTEM_ADMIN 새 CENTER_ADMIN 발급) 302 + flash + DB row 확인 (mustChangePassword=TRUE · password bcrypt · invitedBy 세팅)
- `POST /admin/staff` (SYSTEM_ADMIN 새 SYSTEM_ADMIN 발급) 302 + 정상
- `POST /admin/staff` (email 중복) 400 + "이미 사용 중인 이메일입니다."
- `POST /admin/staff/{id}/reset-password` (자기 자신) 400 + Safeguard 메시지
- 새 관리자 로그인 → 임의 admin 페이지 접근 → 302 → `/mypage/password?force=1`
- `/mypage/password` 변경 성공 → 이후 `/admin/staff` 접근 200

### 기능 E2E (Playwright)

- `admin-staff-list.spec.ts`: 목록 · 검색 · 필터 · 페이지네이션
- `admin-staff-new.spec.ts`: 신규 발급 → flash 로 password 노출 → DB 확인
- `admin-staff-force-change.spec.ts`: 발급 → 새 관리자 로그인 → 강제 변경 리다이렉트 → 변경 → 정상 흐름 왕복

### 계약 검사

- `admin-staff-list.ts` · `admin-staff-new.ts` 신설
- `npx playwright test --project=contracts` → 갭 0
- POLICY.md 준수

---

## 9. 스코프 예상 규모

| 축 | 예상 |
|---|---|
| Java 파일 | +8~10 (AdminStaffController · AdminStaffService · AdminStaffCreateForm · SecureRandomPasswordGenerator · PasswordChangeRequiredInterceptor · WebMvcConfig 확장 · AdminUserSafeguard 헬퍼 확장 · User 도메인 메서드 3종) |
| Thymeleaf 템플릿 | +2 (`admin/staff/list.html` · `admin/staff/new.html`) + `admin/fragments/staff-row.html` 선택 |
| CSS | admin.css 소폭 (액션 컬럼 3링크 정렬 · flash password 강조 카드) |
| 마이그레이션 | 1 (V19) |
| 테스트 | +7 (Service · Controller · Render · Migration · Generator · Interceptor · E2E 3종) |
| 계약 | +2 (staff-list · staff-new) |
| 라인 순증 | 1,200~1,700 |
| 리스크 | **중** — 강제 변경 인터셉터 (전 사용자 로그인 flow 영향) · V19 마이그레이션 · 시드 계정 무회귀 |

---

## 10. deferred / deviation

| 항목 | 사유 | 담당 |
|---|---|---|
| Email invitation flow · SMTP 발송 | Qn-7 · SMTP 인프라 필요 | `deferred: A7-admin-header-live` |
| Bulk CSV 관리자 대량 발급 | QD · 사용 빈도 낮음 | `deferred: A8-1-admin-staff-bulk` (조건부 신설) |
| 관리자 활동 로그 dashboard | Qn-8 · 별도 audit 엔티티 필요 | `deferred: 후속 티켓` |
| A5-2 관리자 프로필 편집 (본인 정보 수정) | A5 §4 이월 (D1) | `deferred: A5-2-admin-self-profile` |
| MFA / SSO 통합 | 인증 인프라 개편 | `deferred: 원거리` |

**이탈 (deviation)**:
- **`/admin/staff` 별도 화면 신설**: prototype 미명시 · A5 §12 Qn-A 이월 조건 (실무 요구 발생) 충족. `deviation: 'prototype 미명시 · A5 §12 Qn-A 이월 결정 실현 · A5 grid 재활용 후 관리자 특성 컬럼(소속 센터·담당 프로그램 수) 재구성'`
- **초기 password 자동생성**: prototype L1963~1973 수동 입력 이탈. `deviation: 'A5-1 QA A안 — 실무·보안 관점 자동생성 채택'`
- **강제 password 변경 인터셉터**: prototype 미명시 · GMP 유사 원칙. `deviation: 'A5-1 Qn-3 A안 — 임시 password 유출 방어'`

---

## 11. 작업 큐 메타

- 작업 ID: A5-1-admin-staff-management
- 우선순위: A5·A8 이월 완결 (admin 트랙 정합성 회복)
- 추정 단위: 1 PR (V19 + AdminStaffController + AdminStaffService + Interceptor + 화면 2 + 테스트 7 + 계약 2)
- 상태: **`spec_done`** — 사용자 결정 대기 (§7 Qn 15건, 특히 핵심 QA/QB/QC/QD)
- read-only: 이 spec 은 확정 전까지 편집 금지

---

## 12. 다음 단계 인계

명세 산출 완료. 사용자 결정 필요 항목 §7 참조.

**결정 요약 요청**:
1. **QA**: 초기 password = **자동생성 (A안 권장)** vs 수동 입력
2. **QB**: **즉시 활성 + flash 노출 (A안 권장)** vs Email invitation (SMTP)
3. **QC**: **`/admin/staff` 별도 화면 신설 (A안 권장 · deviation)** vs A5 목록 재활용 (A5-1 취소)
4. **QD**: Bulk CSV 발급 = **이월 (A안 권장)** vs 편입
5. 세부 Qn-1~11: 모두 A안 권장 (일괄 승인 가능)

핵심 4 + 세부 11 모두 A안 확정 시 → `spec_confirmed` 승격 → ym-impl 인계 가능.

**회귀 방어 3점** (ym-impl 위임 프롬프트 반영 필수):
1. **DataInitializer.seedAdmins 무회귀** — V19 default (mustChangePassword=FALSE) · 시드 계정 로그인 flow 무영향
2. **일반 사용자 signup/login/apply flow 무회귀** — V19 default · 인터셉터는 flag=TRUE 만 발동
3. **A5 · A8 endpoint 무회귀** — AdminUserSafeguard 재활용 (신설 헬퍼는 확장만) · A5 `/admin/users` 화면 무영향

---

## §후속 — 사용자 결정 반영 (2026-09-18)

### 핵심 결정 (QC 변경 · 나머지 원안 A)

| Qn | 결정 |
|---|---|
| **QA** 초기 password | 자동생성 (secure random 12자 · 4문자 클래스) + `mustChangePassword=TRUE` + flash 1회 노출 |
| **QB** invitation vs 즉시 활성 | 즉시 활성 + flash 노출 (SMTP 인프라 A7 이월 상태) |
| **QC** 별도 화면 vs A5 재활용 | **옵션 B — `/admin/users/new` 편입 (prototype 정합)** — 별도 `/admin/staff` 화면 없음. prototype L1946~2045 "신규 사용자 등록" 폼에 권한 radio (사용자·관리자) 통합 |
| **QD** Bulk CSV 발급 | 이월 (A8-1 조건부) — 사용 빈도 낮음 · flash 다건 UX 복잡 |
| Qn-1~10 · Qn-Δ | 모두 원안 A |
| **Qn-11 (예외 · B안)** | **이전 password 검증 유지** (spec 원안 A → B 로 정정 · 2026-09-18 verify F1 반영) — 초기 password 는 flash 로 1회 노출되어 스크린샷·어깨너머·세션 hijacking 유출 리스크 있음. 유출된 임시 password 로 로그인한 공격자가 검증 없이 password 변경 가능하면 계정 탈취 완결. 실무 정합 (AWS Console · GCP · Okta 등 임시 password 변경 시 재확인 필수). 실 구현 (`PasswordChangeController.java` L54~77) 은 보안 우위 방향으로 이미 검증 유지 |

### prototype 실측 근거 (QC 옵션 B)

- `/admin/staff` · `admin-staff` · `staff-list` · `StaffScreen` · `관리자 계정` · `관리자 관리` — prototype 매치 **0건**
- L1950 "신규 사용자 등록" 제목 · L1996~2005 권한 radio (사용자·관리자 2옵션) · L2824 `uf` 폼 상태
- L3559 화면 라우팅 목록: `user-register:'사용자 등록'` 만 존재 · `staff-*` 없음

### 스코프 재조정 (옵션 B 반영)

**포함**:
- `GET /admin/users/new` (SYSTEM_ADMIN 전용) — prototype L1946~2045 정합
- `POST /admin/users` — 신규 계정 발급 · 자동 password + `mustChangePassword=TRUE` + flash 노출
- `POST /admin/users/{id}/reset-password` — 임시 password 리셋 (SYSTEM_ADMIN 만)
- `/login` flow 확장 — `mustChangePassword=TRUE` 유저 로그인 후 password 변경 페이지 강제 redirect
- V19 마이그레이션 (`must_change_password` · `password_changed_at` · `invited_by` 3필드)

**제외 (옵션 B 정합)**:
- ~~`/admin/staff` 별도 화면~~ (prototype 미존재)
- ~~`/admin/staff/new`~~ · ~~`GET /admin/staff` 목록~~
- 관리자 특화 컬럼 (담당 프로그램 수 · 마지막 로그인) → `/admin/users` role filter 결과에 조건부 노출 (필요 시 후속 티켓)

### 스코프 축소

- **소~중** — 파일 12~18 · +1000~1400 LOC (별도 화면 제거로 감소)
- 계약 신설 1종: `admin-user-new.md` (기존 `admin-users.ts` 확장)
- endpoint 3개: `GET /admin/users/new` · `POST /admin/users` · `POST /admin/users/{id}/reset-password`

### 이월

- Email invitation (A7 SMTP 미완)
- 관리자 활동 로그 dashboard
- Bulk CSV 발급 (A8-1 조건부)
- 관리자 특화 컬럼 노출 (`/admin/users` 조건부 · 후속 티켓)
- MFA/SSO

### 다음 액션

ym-impl 인계 프롬프트에 위 옵션 B 결정 반영. 특히 회귀 방어 3점 + prototype 정합 명시.

---

## §구현 매핑 (ym-impl 2026-09-18)

| 명세 요구 | 구현 위치 |
|---|---|
| V19 마이그레이션 (3필드) | `src/main/resources/db/migration/V19__add_users_password_change_columns.sql` |
| User 엔티티 mustChangePassword / passwordChangedAt / invitedBy | `user/User.java` (A5-1 섹션) |
| User.assignInitialPassword / resetPasswordByAdmin / changePassword flag 해제 | `user/User.java` (도메인 메서드 3종) |
| UserPrincipal.mustChangePassword 스냅샷 | `user/UserPrincipal.java` |
| SecureRandomPasswordGenerator (12자 · 4 클래스) | `admin/SecureRandomPasswordGenerator.java` |
| AdminUserSafeguard.assertCanCreateStaff / assertCanResetPassword | `admin/AdminUserSafeguard.java` |
| AdminUserService.createStaff / resetPassword | `admin/AdminUserService.java` (A5-1 섹션) |
| GET /admin/users/new · POST /admin/users · POST /admin/users/{uid}/reset-password | `admin/AdminUserController.java` (신규 발급 폼 섹션) |
| admin/user/new.html — 이메일·이름·성별·권한 radio (사용자/관리자) · 관리자 sub-radio (CENTER/SYSTEM) · 소속 센터 | `templates/admin/user/new.html` |
| admin/user/list.html — 신규 사용자 등록 CTA (SYSTEM_ADMIN 전용) | `templates/admin/user/list.html` |
| admin/user/detail.html — 초기 password flash 카드 · 임시 password 재발급 CTA | `templates/admin/user/detail.html` |
| PasswordChangeController + templates/user/password-change.html | `user/PasswordChangeController.java`, `templates/user/password-change.html` |
| PasswordChangeRequiredInterceptor + WebMvcConfig 등록 | `user/PasswordChangeRequiredInterceptor.java`, `common/config/WebMvcConfig.java` |
| SecurityConfig `/password/change` authenticated | `common/config/SecurityConfig.java` |
| 테스트: password generator · service (createStaff+reset) · safeguard 확장 · interceptor · new render | `test/admin/SecureRandomPasswordGeneratorTest.java`, `test/admin/AdminUserServiceStaffTest.java`, `test/admin/AdminUserSafeguardTest.java`, `test/user/PasswordChangeRequiredInterceptorTest.java`, `test/admin/AdminUserNewRenderTest.java` |

**회귀 방어 실증** (모두 PASS):
- 일반 사용자 flow: SignupAutoLoginTest · SignupRenderTest · UserServiceSignUpTermsTest · MyPageRenderTest · SignupPhoneVerifiedTest
- A5·A8 endpoint: AdminUserServiceTest · AdminUserSafeguardTest · AdminUserControllerRbacTest · AdminUserListRenderTest · AdminUserDetailRenderTest · AdminUserBulkServiceTest · UserPrincipalIsEnabledTest
- JpaMappingTest (V19 컬럼 매핑 정합)

