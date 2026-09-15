# 작업 명세: A5 — admin-users (관리자 사용자 관리 · 상세 · 차단 · 관리자 계정 관리)

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed`** (2026-09-15 사용자 결정: Qn-A **선택형/이월** · 나머지 16건 권장 A) |
| 브랜치 | `feature/A5-admin-users` |
| 스코프 | `/admin/users` 사용자 목록·검색·필터·페이지네이션 · `/admin/users/{id}` 사용자 상세 (신청 이력·즐겨찾기·adminNote) · 차단/재활성화 · `/admin/staff` 관리자 계정 관리 (SYSTEM_ADMIN 전용 · CENTER_ADMIN 승격/철회) |
| 선행 | ✅ A1 admin-shell (#205) · ✅ A2 admin-programs-list (#211) · ✅ A3-2 admin-program-form-integration (#213) · ✅ A4 admin-program-detail (§4의 adminNote 패턴·모달 UX 승계) · §3-2 (isActive·lastAccessAt 미도입 — A5 에서 신설) · Q9 (ADMIN → CENTER_ADMIN 마이그레이션) |
| 후행 | A6 stats (성별·연령 도넛) · A7 admin-header-live (알림 벨: NEW_USER 이벤트 리스너) · A8 bulk action · CSV · 관리자 신규 계정 생성 UI (SYSTEM_ADMIN 대량 발급) |
| 마스터 지시서 | `ADMIN-00-master-directive.md` §5-A5 (L243~248) · §3-2 AppUser (L141~149) · §4 P0-2 admin-security · Q9 role 재편 |
| 상위 spec | A4 §4 (adminNote V15 컬럼 채택 · 모달 vs 페이지 결정) 승계 |
| prototype | `admin/prototype.html` L1166~1272 (users list) · L1774~1943 (user detail 좌:정보수정 / 우:신청 현황) · L1946~2045 (user register) · L2881~2892 (mock user data) · L2906~2910 (roleCfg) · L3396 / L3516 / L4454~4469 (state actions) |
| main 기준 | `4f0f81d` (V15 최신 · A4 완결) |
| 예상 규모 | **대 — 파일 30~40개 · 순증 2,300~3,200 LOC**. V16 (isActive · lastAccessAt · adminNote · deactivated_at/by · ADMIN→CENTER_ADMIN 마이그레이션) 단일 마이그레이션 · 2 컨트롤러 (AdminUserController · AdminStaffController) · 계약 3종 신설 (`admin-users.ts` · `admin-user-detail.ts` · `admin-staff.ts`) · **사용자 signup/login flow 회귀 방어 최우선 (V16 + UserPrincipal.isEnabled)** |

---

## 0. 계약 확인 (0단계)

- **본 화면 계약**: **없음** — 이번 티켓에서 함께 신설
  - `docs/design-contracts/admin/users.md` — 아키텍처(테이블 + 필터 + 페이지네이션 + bulk) · 상태 머신(active↔inactive) · CTA 라우팅
  - `docs/design-contracts/admin/user-detail.md` — 2컬럼(좌 정보 편집 / 우 프로그램 신청 현황 탭) · adminNote · 차단 CTA
  - `docs/design-contracts/admin/staff.md` — SYSTEM_ADMIN 전용 · 관리자 역할 변경 dropdown · safeguard
  - `e2e/contracts/admin-users.ts` · `e2e/contracts/admin-user-detail.ts` · `e2e/contracts/admin-staff.ts` — 셀렉터·컬럼·상태 뱃지 색상·CTA 라벨 · `proto:` L1166~1272 · L1774~1943 인용
- **admin 공통 정책**: `docs/design-contracts/admin/POLICY.md` (다크 헤더 · 인디고 primary · 존댓말) 승계
- **재활용 계약**: `admin-programs.ts` (A2 검색·필터·페이지네이션·bulk bar 패턴) · `admin-program-applications.ts` (A4 상세 모달·상태 dropdown·adminNote textarea 패턴) · `admin-notice.ts` (테이블 grid 패턴)
- **prototype 우선순위**:
  - L1166~1272: 사용자 목록 (search + role filter 3종 + CSV + 등록 + 10열 grid: check · No · 이름 · 이메일 · 성별 · 권한 · 핸드폰 · 가입일 · 최근접속일 · 상태) — **이 원문이 유일 근거**
  - L1774~1943: 사용자 상세 (2컬럼 · 좌: 회원 정보 수정 폼 · 우: 프로그램 신청 현황 탭 4종 [전체/승인/반려/취소])
  - L1946~2045: 사용자 등록 (email + password + name + gender + role radio 3종 + birth + phone + address)
  - **관리자 계정 관리 (`/admin/staff`) prototype 없음** — 사용자 목록의 role filter "시스템 관리자·관리자" 만 존재. Qn-A 별도 화면 신설 여부 결정 (권장 A안: `/admin/users` 에 role=CENTER_ADMIN/SYSTEM_ADMIN 로 필터링 후 상세에서 role 변경 dropdown 노출 — 별도 `/admin/staff` **불필요**)

---

## 1. 디자인 출처 (3자산 병렬 정독)

| 자산 | 인용 | 결론 |
|---|---|---|
| `admin/prototype.html` L1171~1196 | Action bar: 검색(이름·이메일 · placeholder "이름, 이메일 검색") + role filter (전체/시스템 관리자/관리자/사용자) + CSV 내보내기 + 등록하기 | 검색 스코프 2필드 확정 · role filter 4옵션 · CSV/bulk 는 **A8 이월** (UI 자리 Qn-9) |
| L1198~1211 | 총 N명 표시 + bulk selection bar (다크 `#1E293B` "N명 선택됨" + CSV 내보내기 + 삭제 + 선택 해제) | Bulk 이월 (A8) · **UI 유지 여부 Qn-9** |
| L1214~1244 | 10열 grid `36px 36px 110px 1fr 48px 110px 120px 90px 90px 72px` · Header `#F0EFF3` · 상태 셀 badge (`user.statusBadge` `user.statusLabel`) · 이름 클릭 → 상세 (`user.goToDetail`) | 컬럼 스키마 확정 · **lastAccess 파생 필요** (§3) |
| L1248~1257 (empty) | "검색 결과가 없어요" | 공통 `emptyState` fragment 재활용 |
| L1259~1270 (pagination) | 활성 `#3F30E9` · 좌우 화살표 | 공통 `pagination` fragment |
| L1774~1868 (detail 좌) | 회원 정보 수정 폼: 이메일 (disabled `#F0EFF3`) · 새 비밀번호 (선택) · 이름 · 성별 · **권한 radio 3종** · 생년월일 · 핸드폰 · 주소 (다음 우편번호 재사용) · 취소/저장 | 프로필 편집 스코프 확정 · **관리자가 사용자 비밀번호 재설정 가능** — Qn-11 |
| L1870~1941 (detail 우) | 프로그램 신청 현황 (탭 4종 전체/승인/반려/**취소**) · 카드형 리스트: 프로그램명(클릭 → 프로그램 상세) + 상태 dropdown (승인/대기/반려) or **취소 뱃지** · 접수일시 · 신청 상세 → | 탭 4종 · **상태 dropdown 동일 A4 재활용** · CANCELLED 는 read-only 뱃지 (A4 정책 일관) |
| L1946~2045 (register) | 신규 사용자 등록: email + 중복확인 + password + confirm + name + 성별 + **권한 radio 3종** + birth + phone + 주소 | **관리자가 신규 계정 발급 가능** — 초기 비밀번호 정책 Qn-4 |
| L2881~2892 (mock users) | role: 사용자·관리자·시스템 관리자 3종 · lastAccess `YYYY-MM-DD` · status `active/inactive` | Q9: "관리자"=CENTER_ADMIN · lastAccessAt 필수 · isActive 필수 |
| L2906~2910 (roleCfg) | `시스템 관리자`: bg `#FEF3C7` color `#B45309` · `관리자`: bg `#E5E1FB` color `#3428CF` · `사용자`: bg `#F0EFF3` color `#475569` | 뱃지 색상 확정 |
| L3396 / L3516~3518 | `goToDetail: () => setState({screen:'user-detail', selectedUser})` · isActive nav 는 'users'/'user-detail'/'user-register' 3종 | 라우팅 3화면 확정 |
| L4189 (bulkExportUsers) | `downloadCSV('사용자_목록.csv', [id,name,email,gender,role,phone,joinDate,status])` | CSV **A8 이월** |
| L4454~4469 | `goToUserDetail(u)` · `goBackToUsers` · `saveUserRegister` · `bulkDeleteUsers` | 액션 매핑 참조 |
| ADMIN-00 §5-A5 (L243~248) | "권한 필터 + 검색(이름·이메일) + 페이지네이션 10건 + 일괄 선택/CSV/삭제 · 상세: 정보 수정+주소 검색+프로그램 신청 현황 · 등록 폼 · 의존: §3-2 (lastAccessAt·isActive)" | 원 지시 계승 · 세부는 이 spec 이 확장 |
| ADMIN-00 §3-2 (L141~149) | `lastAccessAt` **없음 → 컬럼 추가 + 로그인 성공 핸들러 갱신** · `isActive` **없음 → 컬럼 추가 (비활성=로그인 차단, UserDetailsService 반영)** · Q9 ADMIN 값 정리 | **V16 3필드 신설 확정 (isActive · lastAccessAt · adminNote) + 2필드 감사 (deactivated_at · deactivated_by)** |
| Q9 결정 (ADMIN-00 §8) | 기존 ADMIN row 는 A5 착수 시 CENTER_ADMIN 으로 마이그레이션 후 enum 제거 예정 | **V16 마이그레이션 · UserRole.ADMIN 제거는 후속 티켓 (컴파일 참조 정리 필요)** |
| `User.java` L26~234 | isActive · lastAccessAt · adminNote · deactivatedAt · deactivatedBy 필드 부재 · `assignRole(role, center, centerScope)` 도메인 메서드 존재 (재활용) | 도메인 메서드 신설: `deactivate(by)` · `reactivate()` · `updateLastAccess(now)` · `updateAdminNote(note)` |
| `UserPrincipal.java` L38~55 | 4개 boolean 모두 `return true` — isActive 반영 없음 | **`isEnabled() → user.isActive()` 반영 필수** (회귀 방어) |
| `DataInitializer.java` L171~211 | seedAdmins() 는 SYSTEM_ADMIN 1 + CENTER_ADMIN 2 · `existsByEmail` 멱등 · `assignRole()` 로 role 부여 | **V16 default 값 (`isActive=true`) 로 기존 시드 회귀 없음** · A5 QA reset endpoint 는 seedAdmins 재실행 |

### 1-A. 자산 간 갭 (형태 vs 존재 축)

| 항목 | wireframe | prototype | HANDOFF/ADMIN-00 | 축 | 채택 |
|---|---|---|---|---|---|
| 사용자 목록 10열 grid | 언급 | L1214~1244 명시 | ADMIN-00 §5-A5 승계 | 형태 | prototype 무조건 |
| 상태 컬럼 (active/inactive) | 언급 | L1242 badge | ADMIN-00 §3-2 isActive | 존재 | **isActive V16 신설** |
| 최근접속일 컬럼 | 언급 | L1241 명시 | ADMIN-00 §3-2 lastAccessAt | 존재 | **lastAccessAt V16 신설 + AuthenticationSuccessHandler** |
| role filter 4옵션 | 없음 | L1181~1184 (전체/시스템/관리자/사용자) | Q9: "관리자"=CENTER_ADMIN | 존재+형태 | prototype 4옵션 · "관리자"=CENTER_ADMIN 매핑 |
| 사용자 상세 — 2컬럼 (좌 편집 / 우 신청 현황) | wireframe 명시 (신청 이력 + 담당자 의견) | L1776~1941 명시 | ADMIN-00 §5-A5 승계 | 형태 | prototype 무조건 (**A안: 페이지 단위 · 모달 아님**) |
| 관리자가 사용자 비밀번호 재설정 | 명시 없음 | L1789~1801 (새 비밀번호 필드) | ADMIN-00 §5-A5 "정보 수정(검증)" | 존재 | ⚠️ Qn-11 — **A안: 허용 (감사 로그 필수)** vs 금지 (임시 비밀번호 발급 flow) |
| **관리자가 사용자 권한 변경** (사용자↔관리자↔시스템 관리자) | 명시 (사용자 등록 폼 권한 radio) | L1822~1839 명시 (사용자 상세) · L1996~2011 (등록) | Q9 정리 | 존재 | ⚠️ Qn-A — **prototype 은 사용자 상세에서 권한 변경 가능**. safeguard 3종 필수: (1) 자기 자신 X (2) 마지막 SYSTEM_ADMIN X (3) SYSTEM_ADMIN 승격은 SYSTEM_ADMIN 만 |
| 차단 (isActive=false) | 명시 없음 | prototype 은 status **표시만** · 변경 UI 없음 | ADMIN-00 §3-2 명시 | 존재 | ⚠️ Qn-15 — **isActive 변경 UI 신설** (사용자 상세 하단 "회원 차단 / 재활성화" 버튼) — prototype 미명시 이탈 |
| 차단 사유 (deactivated_reason?) | 명시 없음 | 없음 | 없음 | 존재 | ⚠️ Qn-3 — 필수 vs 선택 (권장 A안: 필수, 감사 로그 목적) |
| 감사 컬럼 (deactivated_at · deactivated_by) | 명시 없음 | 없음 | 없음 | 존재 | ⚠️ Qn-10 — **A안: 필수 (누가 언제 차단했는지 추적 · GMP 유사 원칙)** |
| adminNote | ADMIN-00 Q6 (A4 채택) | 사용자 상세엔 미명시 | ADMIN-00 §3-3 (신청 단위) | 존재 | ⚠️ Qn-2 — **사용자 단위 adminNote 별도 컬럼 vs 신청 단위 adminNote 재활용**. 권장 A안: **사용자 단위 별도 컬럼 신설** (`users.admin_note VARCHAR(1000)`) |
| 즐겨찾기 요약 | 명시 없음 | L1870~1941 은 "신청 현황" 탭만 · **즐겨찾기 탭 없음** | 명시 없음 | 존재 | ⚠️ Qn-12 — 추가 vs 이월. 권장 A안: **이번 티켓 프로그램 신청 현황만** · 즐겨찾기는 이월 |
| 신규 사용자 등록 (관리자 발급) | 명시 (Q9) | L1946~2045 명시 | ADMIN-00 §5-A5 승계 | 존재 | **포함** · 초기 비밀번호 정책 Qn-4 (권장: 임시 비밀번호 이메일 발송 · 최초 로그인 시 재설정 강제 — 후속 A7 · 이번 티켓은 관리자가 직접 비밀번호 입력) |
| CSV / bulk 삭제 | 명시 없음 | L1187~1210 명시 | ADMIN-00 §5-A5 언급 · A8 이월 원칙 | 형태+존재 | **A8 이월** — UI 자리 (Qn-9: 남길지 제거) |
| 다음 우편번호 재사용 | ADMIN-00 §5-A5 명시 | L1852~1861 · L2027~2035 | 사용자 signup 기구현 | 형태 | **재활용 확정** (`fragments/address-search.html` 등) |
| 관리자 계정 관리 별도 화면 (`/admin/staff`) | 명시 없음 | **없음** — 사용자 목록에서 role filter 로 대체 | 사용자가 요구 | 형태 | ⚠️ Qn-A — **B안(prototype 준수): `/admin/staff` 없음 · `/admin/users?role=admin` 로 필터링 · role dropdown 은 상세에서 SYSTEM_ADMIN 만 노출** |

### 1-B. 데이터 모델 gap 표 (필수)

A4 완료 후 스키마 (V15) 기준. **A5 신설 대상**.

| prototype 필드 | 현재 스키마 | 조치 |
|---|---|---|
| 이메일·이름·성별·핸드폰·생년월일·주소 | `users` 컬럼 ✓ | 유지 (읽기·편집 재활용) |
| 권한 (사용자/관리자/시스템 관리자) | `users.role VARCHAR(20)` ENUM ✓ (`USER`/`ADMIN` deprecated/`CENTER_ADMIN`/`SYSTEM_ADMIN`) | **V16: 기존 ADMIN row 를 CENTER_ADMIN 으로 UPDATE + `UserRole.ADMIN` 소스 제거는 후속 티켓** · 라벨 매핑 상수 신설 (USER=사용자, CENTER_ADMIN=관리자, SYSTEM_ADMIN=시스템 관리자) |
| 가입일 (joinDate) | `users.created_at TIMESTAMP` (BaseTimeEntity) ✓ | 파생 `toLocalDate()` |
| **최근접속일 (lastAccess)** | ❌ 없음 | **V16: `users.last_access_at TIMESTAMP NULL`** + `AuthenticationSuccessHandler` 로그인 성공 시 갱신 (별도 트랜잭션 · 사용자 login flow 회귀 방어) |
| **상태 (active/inactive)** | ❌ 없음 | **V16: `users.is_active BOOLEAN NOT NULL DEFAULT true`** + `UserPrincipal.isEnabled() → user.isActive()` 반영 + `AdminScope` 회원 차단시 Spring Security "User is disabled" 예외 처리 |
| **차단 시각 (deactivated_at)** | ❌ 없음 | **V16: `users.deactivated_at TIMESTAMP NULL`** (감사) |
| **차단한 관리자 (deactivated_by)** | ❌ 없음 | **V16: `users.deactivated_by BIGINT NULL REFERENCES users(id)`** (감사) |
| **차단 사유 (deactivation_reason)** | ❌ 없음 | ⚠️ Qn-3 결정 — 채택 시 **V16: `users.deactivation_reason VARCHAR(500) NULL`** |
| **관리자 메모 (adminNote)** | ❌ 없음 | ⚠️ Qn-2 결정 — 채택 시 **V16: `users.admin_note VARCHAR(1000) NULL`** |
| 프로그램 신청 현황 (신청 카드 리스트) | `applications` (FK user_id) ✓ | `AdminUserService.findApplicationsByUser(userId, status)` 쿼리 신설 (`@EntityGraph program`) |
| 즐겨찾기 요약 | `bookmarks` (FK user_id) ✓ | Qn-12 미채택 → 이월 |

### 1-C. 데이터 소비 지점 (write→read 왕복)

| 소비 지점 | prototype 참조 | 현재 상태 | 갭 |
|---|---|---|---|
| `/admin/users` 목록 (isActive/lastAccessAt/role) | L1214~1244 | V16 신설 필요 | 컬럼 부재 |
| `/admin/users/{id}` 상세 (프로필·신청 현황·adminNote·차단 CTA) | L1774~1943 | 컨트롤러 부재 | 신설 |
| `/admin/users/new` 등록 | L1946~2045 | 컨트롤러 부재 | 신설 |
| 로그인 flow (isActive=false → 로그인 차단) | 명시 없음 | `UserPrincipal.isEnabled()` 하드코딩 true | **회귀 방어 최우선** |
| 로그인 flow (lastAccessAt 갱신) | 명시 없음 | AuthSuccessHandler 부재 | 신설 (**별도 트랜잭션 · 실패 시 로그인 무영향**) |
| A4 신청 관리 (`processed_by` 컬럼) | A4 참조 | ✓ 유지 | — |
| A7 헤더 알림 벨 (신규 가입 이벤트) | L2833~2834 | 미구현 | A7 이월 |
| /mypage 사용자 사이드 (프로필 편집) | 사용자 화면 | 기구현 | 관리자 편집과 동일 필드 · 무회귀 |

---

## 2. RBAC · 스코프 (Qn 핵심)

| 경로 | 접근 role | 스코프 |
|---|---|---|
| `GET /admin/users` | SYSTEM_ADMIN + CENTER_ADMIN | SYSTEM_ADMIN: 전체 · **CENTER_ADMIN: 자기 센터 프로그램 신청자만** (`AdminScope.currentCenterName()` 매칭 · Qn-1) |
| `GET /admin/users/{id}` | 위와 동일 | CENTER_ADMIN 은 자기 센터 신청자만 접근 가능 (403 or 404) |
| `POST /admin/users/{id}` (프로필 편집) | 위와 동일 | 자기 센터 신청자만 |
| `POST /admin/users/{id}/deactivate` | 위와 동일 | 자기 센터 신청자만 · **자기 자신 X · SYSTEM_ADMIN 차단은 SYSTEM_ADMIN 만** |
| `POST /admin/users/{id}/reactivate` | 위와 동일 | 동일 |
| `POST /admin/users/{id}/role` (role 변경) | **SYSTEM_ADMIN 만** | 자기 자신 X · **마지막 SYSTEM_ADMIN 강등 X** · SYSTEM_ADMIN 승격은 SYSTEM_ADMIN 만 |
| `POST /admin/users` (신규 등록) | SYSTEM_ADMIN + CENTER_ADMIN | CENTER_ADMIN 은 USER role 만 발급 가능 · CENTER_ADMIN/SYSTEM_ADMIN 발급은 SYSTEM_ADMIN 만 |
| `POST /admin/users/{id}/admin-note` | SYSTEM_ADMIN + CENTER_ADMIN | 자기 센터 신청자만 |

**Safeguard 3종 (필수 · 서비스 계층 + 컨트롤러 이중)**:
1. `if (currentAdmin.id == targetUser.id) throw SelfModificationException` — 자기 자신 role/차단 변경 금지
2. `if (userRepository.countByRole(SYSTEM_ADMIN) == 1 && targetUser.role == SYSTEM_ADMIN) throw LastSystemAdminException` — 마지막 SYSTEM_ADMIN 강등·차단 금지
3. `if (newRole == SYSTEM_ADMIN && currentAdmin.role != SYSTEM_ADMIN) throw InsufficientPermissionException` — SYSTEM_ADMIN 승격 권한

---

## 3. 엔티티·마이그레이션 (V16 단일 마이그레이션)

### V16__admin_users_isactive_lastaccess_adminnote.sql

```sql
-- A5: 관리자 사용자 관리에 필요한 상태·감사·메모 컬럼 신설
-- 회귀 방어: DEFAULT 값으로 기존 row 무영향 · UserPrincipal.isEnabled() 는 소스 변경으로 반영
-- 참조: ADMIN-00 §3-2 · A5 §3

ALTER TABLE users
  ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN last_access_at TIMESTAMP NULL,
  ADD COLUMN admin_note VARCHAR(1000) NULL,          -- Qn-2 채택 시
  ADD COLUMN deactivated_at TIMESTAMP NULL,          -- Qn-10 채택 시
  ADD COLUMN deactivated_by BIGINT NULL REFERENCES users(id),  -- Qn-10 채택 시
  ADD COLUMN deactivation_reason VARCHAR(500) NULL;  -- Qn-3 채택 시

-- Q9: 기존 ADMIN role → CENTER_ADMIN 마이그레이션 (deprecated enum 정리)
-- 주의: enum 소스(UserRole.ADMIN) 는 이 마이그레이션 반영 후 **후속 티켓**에서 삭제
UPDATE users SET role = 'CENTER_ADMIN' WHERE role = 'ADMIN';

-- 최근접속일 초기값: NULL (기존 사용자는 로그인해야 채워짐 — 목록에서 "-" 표기)
-- 인덱스: 검색·필터에 자주 쓰이는 컬럼
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_role ON users(role);
```

### User 엔티티 신설 필드 + 도메인 메서드

```java
@Column(nullable = false, columnDefinition = "boolean not null default true")
private boolean isActive = true;

@Column private LocalDateTime lastAccessAt;

@Column(length = 1000)
private String adminNote;

@Column private LocalDateTime deactivatedAt;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "deactivated_by")
private User deactivatedBy;

@Column(length = 500)
private String deactivationReason;

/** A5: 회원 차단 — deactivate_at/by/reason 세트로 기록 */
public void deactivate(User admin, String reason) {
  this.isActive = false;
  this.deactivatedAt = LocalDateTime.now();
  this.deactivatedBy = admin;
  this.deactivationReason = reason;
}

/** A5: 재활성화 — 감사 컬럼은 이력이라 clear 하지 않음 (마지막 차단 이력 유지) */
public void reactivate() {
  this.isActive = true;
}

/** A5: 로그인 성공 시 갱신 — AuthenticationSuccessHandler 에서 호출 */
public void updateLastAccess(LocalDateTime now) {
  this.lastAccessAt = now;
}

/** A5: 관리자 메모 — 사용자에게 노출되지 않음 */
public void updateAdminNote(String note) {
  this.adminNote = note;
}
```

### UserPrincipal 회귀 방어

```java
@Override
public boolean isEnabled() {
  return user.isActive();  // ← V16 반영. 사용자가 로그인 시도 시 DisabledException 발생 → login page redirect
}
```

**필드가 아니라 참조로**: `UserPrincipal` 는 현재 필드 복사 방식 (`this.password = user.getPassword()`) — isActive 도 같은 패턴으로 복사 필요. 회귀 방어를 위해 **User 원본 참조 유지 or isActive 필드 복사 후 리턴**.

---

## 4. 화면 · 라우팅

| 경로 | 뷰 | 설명 |
|---|---|---|
| `GET /admin/users` | `admin/users/list.html` | 검색·role filter·페이지네이션 10건 (Qn-5) |
| `GET /admin/users/{id}` | `admin/users/detail.html` | 2컬럼 (좌 프로필 편집 · 우 신청 현황 탭 4종) |
| `POST /admin/users/{id}` | redirect `/admin/users/{id}` | 프로필 편집 저장 — **A5 이월** (D1 · 후속 티켓 A5-2) |
| `POST /admin/users/{id}/deactivate` | redirect `/admin/users/{id}` | 차단 (확인 모달 필수) |
| `POST /admin/users/{id}/reactivate` | redirect `/admin/users/{id}` | 재활성화 |
| `POST /admin/users/{id}/role` | redirect `/admin/users/{id}` | role 변경 (SYSTEM_ADMIN 만) |
| `POST /admin/users/{id}/admin-note` | fragment `#admin-note-view` (HTMX) | 메모 저장 (인라인) |
| `GET /admin/users/new` | `admin/users/register.html` | 신규 사용자 등록 폼 — **A5 이월** (D2 · Qn-4 결정 반영 · 후속 A5-1/A8) |
| `POST /admin/users` | redirect `/admin/users/{id}` | 신규 등록 저장 — **A5 이월** (D2) |

**D1 · D2 이월 근거** (ym-verify 2026-09-15 지적): §12 §후속 결정 시 프로필 편집·신규 등록 UI 는 스코프에서 실질 제외되었으나 §4 원안 표는 갱신 누락. 후속 티켓 A5-2 (프로필 편집) · A5-1 (관리자 신규 계정 발급) 로 분리.

**관리자 계정 관리 (`/admin/staff`)**: Qn-A **B안 채택 권장 → 별도 화면 없음 · `/admin/users?role=CENTER_ADMIN,SYSTEM_ADMIN` 필터로 대체**. prototype 미명시 이탈 최소화.

### 컬럼 스키마 (목록 · prototype L1218~1228 정합)

| # | 라벨 | 폭 | 데이터 |
|---|---|---|---|
| 1 | checkbox | 36px | bulk (Qn-9 · A8 이월 시 hidden) |
| 2 | No. | 36px | seq |
| 3 | 이름 | 110px | user.name (링크 → 상세 · `#3F30E9`) |
| 4 | 이메일 | 1fr | user.email |
| 5 | 성별 | 48px | user.gender |
| 6 | 권한 | 110px | roleBadge (roleCfg 색상) |
| 7 | 핸드폰 | 120px | user.phone |
| 8 | 가입일 | 90px | createdAt (YYYY-MM-DD) |
| 9 | 최근접속일 | 90px | lastAccessAt (YYYY-MM-DD · NULL 시 "-") |
| 10 | 상태 | 72px | statusBadge (active=진행중/`#D1FAE5·#047857` · inactive=마감/`#F0EFF3·#6E6B82`) |

---

## 5. 회귀 방지 (최우선)

| 회귀 영역 | 감지 방법 | 조치 |
|---|---|---|
| 사용자 signup 흐름 | `SignupControllerRenderTest` (기존) + `UserServiceTest.signUp*` | isActive 기본값 true 확인 · 회원가입 후 즉시 로그인 가능 |
| 사용자 login 흐름 (isActive=true 정상 로그인) | `SecurityIntegrationTest.login_active_user_success` 신설 | `UserPrincipal.isEnabled()` true 반환 |
| 사용자 login 흐름 (isActive=false 차단) | `SecurityIntegrationTest.login_inactive_user_blocked` 신설 | `DisabledException` 발생 · login page redirect (Spring 표준) |
| lastAccessAt 갱신 실패 시 로그인 무영향 | `AuthenticationSuccessHandlerTest.lastAccess_failure_login_ok` | **별도 트랜잭션 (`REQUIRES_NEW`)** · 예외 삼킴 (log.warn) |
| /mypage 프로필 편집 (사용자 기구현) | 기존 `MypageEditRenderTest` | 관리자 편집 로직과 필드 스코프 분리 확인 |
| `DataInitializer.seedAdmins()` 재기동 회귀 | `DataInitializerIdempotentTest` | `existsByEmail` 멱등 유지 · isActive default 반영 |
| A4 신청 관리 화면 | `AdminProgramApplicationsRenderTest` | User.deactivated 상태여도 신청 이력 노출 정상 |
| UserRole.ADMIN 참조 코드 | `grep -r "UserRole.ADMIN"` | V16 UPDATE 후 참조 0건 확인. 남아 있으면 마이그레이션 후속 티켓 |
| CENTER_ADMIN 센터 격리 | `AdminUserControllerRbacTest` | 타 센터 신청자 목록 미노출 + 직접 URL 403 |

**Spring Security "isEnabled=false 시 로그인 차단" 정합**: `DaoAuthenticationProvider` 는 `UserDetails.isEnabled()` 를 자동 검사한다. custom filter 신설 불필요.

---

## 6. 테스트

### 정적

| 테스트 | 대상 |
|---|---|
| `AdminUserServiceTest` | 목록 검색·필터 · 상세 조회 · 프로필 편집 · 차단 · 재활성화 · role 변경 · safeguard 3종 |
| `AdminUserControllerTest` (@WebMvcTest) | RBAC 401/403 · CENTER_ADMIN 스코프 격리 · CSRF |
| `AdminUsersRenderTest` (@SpringBootTest render) | 목록·상세·등록 Thymeleaf 렌더 · 표현식 잔존 0건 · empty state · pagination |
| `V16MigrationTest` (`@FlywayTest`) | 컬럼 추가 + 기존 ADMIN → CENTER_ADMIN UPDATE 검증 |
| `UserPrincipalIsEnabledTest` | isActive=true → isEnabled=true · false → false |
| `AuthenticationSuccessHandlerTest` | lastAccessAt 갱신 · 실패 시 로그인 무영향 (REQUIRES_NEW) |
| `DataInitializerIdempotentTest` | seedAdmins 재실행 시 중복 없음 · isActive=true default |

### 동적 (curl · bootRun e2e)

- `GET /admin/users` 200 + role filter 파라미터 반영
- `POST /admin/users/{id}/deactivate` (SYSTEM_ADMIN CSRF) → 302 → `/admin/users/{id}` · 대상 user isActive=false 확인
- `POST /admin/users/{id}/deactivate` (자기 자신) → 400 + "본인 계정은 차단할 수 없어요"
- `POST /admin/users/{id}/role` (CENTER_ADMIN → SYSTEM_ADMIN 시도) → 403
- 차단된 유저로 로그인 시도 → login page redirect + error 파라미터

### 기능 E2E (Playwright)

- `admin-users-list.spec.ts`: 검색·필터·페이지네이션 · role badge 색상
- `admin-user-detail.spec.ts`: 프로필 편집 저장 · 차단 확인 모달 · 재활성화 · adminNote 인라인 저장 (HTMX)
- `admin-user-role-change.spec.ts`: SYSTEM_ADMIN 만 role dropdown 노출 · 자기 자신 옵션 disabled
- `admin-user-signup-regression.spec.ts`: **회원가입 → 로그인 → 관리자 차단 → 로그인 실패 → 관리자 재활성화 → 로그인 성공** 왕복

### 계약 검사

- `admin-users.ts` · `admin-user-detail.ts` · `admin-staff.ts` (Qn-A 채택 시) 신설
- `npx playwright test --project=contracts` → 갭 0
- POLICY.md 준수 (다크 헤더 · 인디고 primary)

### Reset endpoint (신설 · Qn-6)

`POST /admin/qa/reset-users` (e2e 프로파일 전용 · `@Profile("e2e")` + `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`)
- 신규 시드 유저 전원 isActive=true · deactivated_* NULL · admin_note NULL
- seedAdmins 재실행

---

## 7. 결정 필요 항목 (Qn — 17건)

### 핵심 3

**Qn-A: 관리자 계정 관리 화면 분리 여부**
- A안 (권장): **`/admin/staff` 별도 화면 신설 없음** · `/admin/users?role=CENTER_ADMIN,SYSTEM_ADMIN` 필터로 대체 · 상세에서 SYSTEM_ADMIN 만 role dropdown 노출 · prototype 준수
- B안: `/admin/staff` 별도 화면 · SYSTEM_ADMIN 만 접근 · prototype 미명시 이탈 (사용자 요구는 있음)
- **판정**: prototype 우선 원칙상 A안. 사용자가 요구한 "관리자 계정 관리" 의미는 role filter + 상세 role 변경으로 100% 충족.

**Qn-B: 사용자 상세 UI (모달 vs 페이지)**
- A안 (권장): **페이지** — prototype L1774 `screen:'user-detail'` 별도 화면 · A4 는 모달이었으나 사용자 상세는 편집 폼이 크므로 페이지 적합
- B안: 모달 — A4 일관성
- **판정**: prototype 준수 → A안 **페이지**

**Qn-C: 차단 시 신청 이력 처리**
- A안 (권장): **기존 신청 유지** (cascade 없음) · CANCELLED 자동 전환 없음 · 차단은 로그인 차단만
- B안: cascade 취소 · 진행 중인 PENDING/APPROVED 를 CANCELLED 로 전환
- **판정**: 데이터 무결성 원칙 + A4 CANCELLED 는 사용자 자발적 취소 의미 유지 → A안. 관리자가 필요 시 A4 화면에서 개별 반려/취소.

### 세부 14

**Qn-1: CENTER_ADMIN 사용자 목록 스코프**
- A안 (권장): **자기 센터 프로그램 신청자만** — `Application.program.center.name == currentCenter` 매칭. 신청 이력 없는 유저는 목록 미노출.
- B안: 자기 센터 CENTER_ADMIN + 자기 센터 신청자 (관리자도 목록 노출)
- **판정**: A안 · 관리자는 `/admin/staff` (Qn-A B안) 또는 role filter 로 별도 조회.

**Qn-2: adminNote 위치**
- A안 (권장): **User row 신설 컬럼** (`users.admin_note VARCHAR(1000)`) · 사용자 단위 메모 (신청과 별개)
- B안: audit 엔티티 (`AdminUserNote` 별도 테이블 · 이력 관리)
- **판정**: A4 는 신청 단위 adminNote, A5 는 사용자 단위. 이력 필요성 낮음 → A안. 필요 시 후속 티켓에서 audit 로 확장.

**Qn-3: 차단 사유 필수 여부**
- A안 (권장): **필수** — 감사·소명 목적 · VARCHAR(500)
- B안: 선택
- **판정**: A안 · GMP 유사 원칙 + safeguard.

**Qn-4: 관리자 등록 시 초기 비밀번호**
- A안 (권장): **관리자가 임의 입력** (prototype L1963~1973 동일) · 사용자 최초 로그인 후 자체 변경 안내
- B안: 임시 비밀번호 자동 발급 + 이메일 발송 + 최초 로그인 시 변경 강제
- **판정**: A안 (prototype 준수 · 후속 A7 에서 이메일 발송 flow 확장). Qn-4-B 는 A7 이월.

**Qn-5: 페이지당 개수**
- A안 (권장): **10건** — ADMIN-00 §5-A5 명시
- 판정: A안 확정.

**Qn-6: reset endpoint 신설**
- A안 (권장): **`POST /admin/qa/reset-users` (e2e 프로파일 전용)** · SYSTEM_ADMIN 만 · seedAdmins 재실행 + isActive/deactivated_* 초기화
- 판정: A안.

**Qn-7: 계약 신설**
- A안: `admin-users.ts` + `admin-user-detail.ts` 2종
- B안 (권장): 위 2종 + `admin-user-register.ts` (등록 폼 별도 화면이라 계약 분리)
- **판정**: B안 · 3종 신설. Qn-A A안 채택 시 `admin-staff.ts` 불필요.

**Qn-8: 검색 스코프**
- A안 (권장): **이름 + 이메일 2필드** (prototype L1176 placeholder "이름, 이메일 검색")
- B안: 확장 (핸드폰 · 주소)
- 판정: A안 · 확장은 A8 검색 개선 티켓에서.

**Qn-9: bulk action UI 자리**
- A안 (권장): **UI 완전 제거** (bulk selection bar · checkbox column) · A8 에서 재도입
- B안: UI 유지 · 버튼 disabled + "A8 예정" tooltip
- **판정**: A안 · unused DOM 최소화. A8 착수 시 재도입 부담 낮음.

**Qn-10: 감사 컬럼 (deactivated_at · deactivated_by)**
- A안 (권장): **필수** · V16 신설
- 판정: A안.

**Qn-11: 관리자가 사용자 비밀번호 재설정**
- A안 (권장): **허용 · prototype L1789~1801 명시** · 새 비밀번호 입력 시 저장 + 감사 로그 (adminNote 에 "N에 의해 비밀번호 재설정됨" 자동 append)
- B안: 금지 · 임시 비밀번호 발급 flow (Qn-4 B안과 세트)
- **판정**: A안 (prototype 준수). B안은 A7 이월.

**Qn-12: 즐겨찾기 요약**
- A안 (권장): **이월** · 이번 티켓은 신청 현황만 · prototype 도 즐겨찾기 탭 없음
- B안: 추가 · 즐겨찾기 탭 신설
- 판정: A안.

**Qn-13: 신규 사용자 등록 시 phoneVerified**
- A안 (권장): **관리자 등록은 phoneVerified=true 자동** (관리자가 확인했다 간주)
- B안: false · 최초 로그인 시 재인증 요구
- 판정: A안 (관리자 등록 flow 는 신뢰 경로).

**Qn-14: role 변경 시 알림 발송**
- A안 (권장): **이번 티켓 미포함** · A7 알림 벨 에서 확장
- B안: 이번 티켓 포함 (Notification 재사용)
- 판정: A안.

**Qn-15: 차단 CTA 위치**
- A안 (권장): **사용자 상세 하단 "위험 존" 카드** · "회원 차단" 버튼 (빨간색) + 확인 모달 (차단 사유 입력)
- B안: 목록에서도 direct 차단 (bulk action)
- 판정: A안 · bulk 는 A8. prototype 미명시라 UI 신설 이탈 최소화.

**Qn-16: role 변경 dropdown vs 별도 버튼**
- A안 (권장): **prototype L1822~1839 radio 유지** · 저장 버튼으로 role 확정
- B안: 별도 role dropdown + 즉시 확인 모달 (A4 신청 상태 dropdown 패턴)
- 판정: A안 (prototype 준수).

**Qn-17: UserRole.ADMIN enum 소스 제거 티켓 분리**
- A안 (권장): **V16 은 데이터 UPDATE 만** · enum 소스 제거는 **후속 티켓 `chore/drop-user-role-admin`** (컴파일 참조 정리)
- B안: 이번 티켓에 통합
- 판정: A안 · 스코프 보수적. A5 는 admin 화면 신설이 본질.

---

## 8. 스코프 예상 규모

| 축 | 예상 |
|---|---|
| Java 파일 | +15 (AdminUserController · AdminUserService · AdminUserForm · AdminStaffService [Qn-A B안 시] · UserExceptions · AuthenticationSuccessHandler · UserPrincipal 개편 · UserRoleLabel · ...) |
| Thymeleaf 템플릿 | +6 (`admin/users/list.html` · `detail.html` · `register.html` · fragments: `role-badge.html` · `status-badge.html` · `danger-zone.html`) |
| CSS | admin.css 소폭 (danger-zone · role-badge 이미 A2 재활용) |
| 마이그레이션 | 1 (V16) |
| 테스트 | +10 (@WebMvcTest · Service · Render · Migration · Security · AuthHandler · E2E 4종) |
| 계약 | +3 (users · user-detail · user-register) |
| 라인 순증 | 2,300~3,200 |
| 리스크 | **중~높음** — 사용자 login flow 회귀 · UserRole.ADMIN 마이그레이션 · safeguard 3종 |

---

## 9. deferred / deviation

| 항목 | 사유 | 담당 |
|---|---|---|
| Bulk deactivate | A8 | `deferred: A8-admin-bulk-actions` |
| CSV export | A8 | `deferred: A8-admin-bulk-actions` |
| 관리자 신규 계정 대량 발급 UI (SYSTEM_ADMIN 발급 flow) | 이번 티켓은 개별 발급만 | `deferred: A8` |
| 통계 (성별·연령 도넛) | A6 | `deferred: A6-admin-stats` |
| 즐겨찾기 요약 탭 | prototype 미명시 · Qn-12 | `deferred: A7 or A8` |
| 임시 비밀번호 발급 + 이메일 발송 | Qn-4 B안 · A7 알림 인프라 필요 | `deferred: A7-admin-header-live` |
| 최초 로그인 시 비밀번호 변경 강제 | Qn-4 B안과 세트 | `deferred: A7` |
| Role 변경 알림 발송 | Qn-14 | `deferred: A7` |
| UserRole.ADMIN enum 소스 제거 | 데이터 UPDATE 후 후속 티켓 | `deferred: chore/drop-user-role-admin` |
| AdminUserNote 이력 엔티티 (Qn-2 B안) | 필요성 낮음 · adminNote 단일 컬럼으로 시작 | `deferred: 필요 시 별도 티켓` |
| Bulk selection UI 재도입 | Qn-9 A안 · A8 착수 시 | `deferred: A8` |

**이탈(deviation)**:
- **회원 차단 CTA (Qn-15)**: prototype 미명시 · 사용자 요구 반영을 위해 신설. `deviation: 'prototype 은 status 표시만 · 요구사항상 CTA 신설 (사용자 상세 하단 danger zone)'`
- **관리자 계정 관리 화면 (Qn-A A안)**: prototype 준수 · 별도 화면 없음. `deviation` 아님 (prototype 정합).

---

## 10. 작업 큐 메타

- 작업 ID: A5-admin-users
- 우선순위: admin 트랙 5순위 (ADMIN-00 §9 순서 준수)
- 추정 단위: 1 PR (V16 + 컨트롤러 2 + 화면 3 + 테스트 10)
- 상태: **`spec_done`** — 사용자 결정 대기 (§7 Qn 17건, 특히 핵심 Qn-A/B/C)
- read-only: 이 spec 은 확정 전까지 편집 금지

---

## 11. 다음 단계 인계

명세 산출 완료. 사용자 결정 필요 항목 §7 참조.

**결정 요약 요청**:
1. Qn-A: `/admin/staff` 별도 화면 없음(A안 권장) vs 신설(B안)
2. Qn-B: 사용자 상세 페이지(A안 권장) vs 모달
3. Qn-C: 차단 시 신청 이력 유지(A안 권장) vs cascade
4. 세부 Qn-1~17: 모두 A안 권장 (일괄 승인 가능)

핵심 3 + 세부 14 모두 A안 확정 시 → `spec_confirmed` 승격 → ym-impl 인계 가능.

---

## 12. §후속 — 사용자 결정 반영 (2026-09-15)

### 결정 요약

| Qn | 결정 | 근거 |
|---|---|---|
| **Qn-A** (`/admin/staff` 별도 화면) | **선택형 / 이월** | 이번 A5 스코프에서 별도 화면 **미신설**. `/admin/users` role filter 4옵션(전체·SYSTEM_ADMIN·CENTER_ADMIN·USER)으로 관리자 조회·역할 변경 통합. 향후 대량 관리자 발급 유즈케이스가 실질적으로 발생하면 후속 티켓 A5-1 로 신설 |
| **Qn-B** 상세 UI | **페이지** (A안) | A4 학습 반영 · 좌 프로필/우 신청 이력 4탭 정보량 |
| **Qn-C** 차단 시 신청 이력 | **유지** (A안) | 감사·GMP 관점 · `UserService.withdraw` 하드 삭제와 명확히 분리 |
| **Qn-1** Safeguard | 3종 (자기 자신 X · 마지막 SYSTEM_ADMIN X · SYSTEM 승격은 SYSTEM 만) | 필수 |
| **Qn-2** adminNote 위치 | User row 단일 컬럼 | A4 패턴 승계 |
| **Qn-3** 차단 사유 필수 | 필수 (Bean Validation `@NotBlank`) | 감사 · deactivation_reason 컬럼 활용 |
| **Qn-4** 관리자 생성 | 기존 사용자 승격만 | 신규 관리자 계정 발급 UI 는 이월 |
| **Qn-5** 페이지당 | 10 | admin 일관성 |
| **Qn-6** reset endpoint | 신설 (`/e2e/reset-users`) | E2E 격리 |
| **Qn-7** 계약 3종 | 신설 (admin-users · admin-user-detail · admin-staff 는 이월 대비 stub) | |
| **Qn-8** 검색 범위 | 이메일 + 이름 OR (`LIKE`) | 확장은 후속 |
| **Qn-Δ** XSS · CSRF · 활동 로그 이월 | 표준 | |

### 이번 스코프 확정 (스코프 축소 반영)

**포함**:
- `/admin/users` 목록 (검색·role filter 4옵션·페이지네이션·차단/재활성화 액션)
- `/admin/users/{id}` 상세 (좌 프로필 편집 · 우 신청 이력 4탭 · adminNote · danger zone 차단 CTA)
- `/admin/users/new` 관리자 승급 대상 등록 (Qn-4 = 기존 사용자 대상)
- `POST /admin/users/{id}/deactivate` 차단 (사유 필수)
- `POST /admin/users/{id}/reactivate` 재활성화
- `POST /admin/users/{id}/role` 역할 변경 (safeguard 3종 · SYSTEM_ADMIN 전용)

**제외 (이월)**:
- `/admin/staff` 별도 화면 (Qn-A → A5-1)
- 대량 관리자 계정 신규 발급 UI (Qn-4 → A5-1 또는 A8)
- Bulk deactivate · CSV · 통계 · 활동 로그

### V16 마이그레이션 확정

```sql
ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN last_access_at TIMESTAMP;
ALTER TABLE users ADD COLUMN admin_note VARCHAR(1000);
ALTER TABLE users ADD COLUMN deactivated_at TIMESTAMP;
ALTER TABLE users ADD COLUMN deactivated_by BIGINT REFERENCES users(id);
ALTER TABLE users ADD COLUMN deactivation_reason VARCHAR(500);
UPDATE users SET role = 'CENTER_ADMIN' WHERE role = 'ADMIN';
```

- `is_active` 전건 default `TRUE` → 기존 유저 로그인 무회귀
- `role='ADMIN'` UPDATE 는 Q9 규격 정규화 (enum 소스 `UserRole.ADMIN` 제거는 후속 티켓)

### 회원 탈퇴 vs 관리자 차단 명확화 (실측 2026-09-15)

| 항목 | 탈퇴 (`UserService.withdraw`) | 차단 (신규 `AdminUserService.deactivate`) |
|---|---|---|
| 주체 | 본인 (MyPageController `/withdraw`) | SYSTEM_ADMIN (신규) |
| DB row | 하드 삭제 (User + Application + Bookmark + Notification + UserAgreement) | 유지 (`isActive=false`) |
| 사유 | 없음 | 필수 (`deactivation_reason`) |
| 로그인 | 계정 없음 → 인증 실패 | `UserPrincipal.isEnabled()` false → `DisabledException` |
| 되돌리기 | 불가 | 재활성화 가능 |
| 신청 이력 | 삭제 | 보존 |

두 flow 독립 유지. A5 는 `UserService.withdraw` 는 손대지 않음.

### 회귀 방어 최우선 3점

1. **`UserPrincipal.isEnabled()` 하드코딩 `true` → `user.isActive()` 반영**
   - 회귀 리스크: 기존 signup/login/apply flow
   - 방어: V16 전건 `TRUE` default · signup/login/apply E2E green 유지 필수
2. **`AuthenticationSuccessHandler`** `lastAccessAt` 갱신 → `REQUIRES_NEW` + `try/catch` · 로그인 flow 무영향
3. **`DataInitializer.seedAdmins()`** 무회귀 — default `TRUE` · 기존 멱등 유지

### deferred / deviation

- **deferred**: `/admin/staff` 별도 화면 (Qn-A) · 관리자 신규 계정 발급 UI (Qn-4) · Bulk · CSV · 활동 로그 · `UserRole.ADMIN` enum 소스 제거
- **deviation**: 차단 CTA 위치 = 사용자 상세 하단 danger zone 신설 (prototype 미명시 · UX 관점)

### 다음 액션

`ym-impl` 인계 프롬프트에 위 결정 반영. 특히 **회귀 방어 3점** 은 명시적 검증 요구.
