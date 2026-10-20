# 작업 명세: A5-1-e2e Part 2-user — 사용자 비밀번호 변경 화면 (마이페이지 자발적 진입)

| 메타 | 값 |
|---|---|
| 상태 | **`spec_investigation`** — 디자인 수정 slot 확인 질문 포함. 사용자 응답 후 `spec_confirmed` 로 전환 |
| 브랜치 (예정) | `feature/A5-1-e2e-part2-user-password-change` |
| 선행 | ✅ A5-1 (#219) · ✅ A5-1-e2e Part 1 (admin staff management) 머지 가정 · ✅ `PasswordChangeController` / `PasswordChangeRequiredInterceptor` 완비 |
| 관련 spec | `A5-1-e2e-admin-staff-management.md` (Part 1 · 강제 변경 flow 재활성화) |
| prototype 참조 | `prototype.tsx` L1491~1518 (profile-edit 내 "비밀번호 변경" 서브헤드 · 입력쌍 · 불일치 inline) — **독립 화면은 prototype 에 없음** |
| 예상 규모 | **소** — 신규 Controller 1 (`UserPasswordChangeController`) · 재사용 템플릿 fragment 1 · 신규 템플릿 1 (`templates/mypage/password-change.html`) · 디자인 계약 신설 1 (`user/mypage-password-change.md` + `.ts`) · E2E spec 신설 1 · LOC 순증 300~500 |

---

## 0단계 — 조사 결과 (디자인 수정 요청 전)

### A. 세션 선택 무효화 (#6 "다른 기기 로그아웃") — ⚠️ **지금 구조에서는 불가**

| 항목 | 소스 | 상태 |
|---|---|---|
| `SessionRegistry` Bean | `SecurityConfig.java` 전체 | ❌ 미등록 |
| `sessionManagement().maximumSessions(…)` | `userSecurityFilterChain` | ❌ 설정 없음 — 동시 세션 추적 안 함 |
| `spring.session.store-type` | `application.yml` | ❌ 설정 없음 — **기본 Tomcat in-memory HttpSession** 사용 |
| 사용자별 세션 ID 조회 가능 여부 | — | ❌ 불가 (Registry 없음) |
| 유사 케이스 — `admin/users/{id}/deactivate` 가 즉시 강제 종료? | `AdminUserService.deactivate()` L117~126 | ❌ `target.deactivate(...)` 도메인 변경만. **활성 세션은 그대로 유지**. 다음 요청 시 `DisabledException` 로 로그인 차단되는 간접 방식 |

**결론**: "현재 기기 유지 + 다른 기기 로그아웃" 은 **지금 구현 불가 → 신규 구현 필요**.

**신규 구현 설계안 (단일)** — Spring Security `SessionRegistry` 등록 기반:

| 단계 | 작업 | LOC |
|---|---|---|
| 1 | `SecurityConfig` 에 `SessionRegistry` + `HttpSessionEventPublisher` 등록 | ~10 |
| 2 | `userSecurityFilterChain` 에 `sessionManagement().maximumSessions(-1).sessionRegistry(registry)` 추가 (최대값 미제한 — 추적만) | ~5 |
| 3 | 변경 성공 시 `SessionRegistry.getAllSessions(principal, false)` 로 활성 세션 조회 → **현재 요청 세션 ID 제외**하고 전부 `SessionInformation.expireNow()` | ~15 |
| 4 | 테스트 — 2개 세션 동시 로그인 → 변경 → 다른 세션의 다음 요청 401 리다이렉트 검증 | ~40 |

LOC 합계: ~70. **단, HttpSession 메모리 저장소 한계**: 단일 JVM 전제 (Redis·Hazelcast 등 분산 session 아님 → scale-out 시 깨짐). 현재 프로젝트는 단일 인스턴스이므로 수용 가능.

**사용자 결정 필요 (Qa)**:
- **Qa-A**: 설계안 그대로 신규 구현 (LOC 70) — "다른 기기 로그아웃" 보장
- **Qa-B**: Part 2 범위에서 **제외** — "변경 후 현재 세션 유지" 만 하고 다른 기기 세션은 그대로 둠 (만료까지 그대로 유효). 분산 세션 저장소 도입 시 재검토
- **Qa-C**: Part 2 범위에서 **제외하되 안전 대안**: 변경 후 현재 세션 포함 **모든 세션 로그아웃** → 재로그인 유도 (강제 변경 flow 와 동일). 사용자 경험 약간 저하지만 보안 가장 안전

> 선행 결정 #6 ("현재 기기 유지 + 다른 기기 로그아웃") 는 **Qa-A 선택 시만 성립**. Qa-B/C 선택 시 선행 결정 재조정 필요.

### B. 기존 자원 재사용 범위

| 자원 | 재사용 가능 여부 | 비고 |
|---|---|---|
| `PasswordChangeController` (강제 변경 `/password/change`) | ❌ **별도 Controller 필요** | URL 분리(#2) 및 로직 분기 (성공 후 redirect 처: 강제=`/logout`, 자율=`/mypage?tab=profile&changed=1`) 때문에 공유 어려움. 공통 검증 로직은 private helper 로 추출 가능 |
| `PasswordResetRequest` DTO | ⚠️ **부분 재사용** | 현재 비밀번호 필드 (`currentPassword`) 없음. 신규 DTO (`UserPasswordChangeRequest`) 필요하되 비밀번호 정책 어노테이션 (`@Size`·`@Pattern`) 은 복붙 |
| `templates/user/password-change.html` CSS·fragment | ⚠️ **일부 참조만** | 강제 변경 화면은 인라인 스타일 중심 (`main.css` fragment 없음). 자율 변경은 마이페이지 레이아웃 안이라 **새 `templates/mypage/password-change.html` 신설** 이 자연스러움. 눈 아이콘 토글은 `profile-edit.html` 의 `.auth-eye-btn` + `.auth-input-with-eye` 패턴 그대로 재사용 (L44~52) |
| `PasswordChangeRequiredInterceptor` 가 `/mypage/password` 통과? | ⚠️ **차단됨** | 현재 `EXACT_ALLOWED = Set.of("/password/change", "/login", "/logout", …)` 만 허용 → `mustChangePassword=TRUE` 사용자가 `/mypage/password` 접근 시 **`/password/change` 로 강제 redirect**. 이 동작은 **그대로 유지하는 것이 설계 의도** (강제 flag 켜진 사용자는 자율 flow 접근 금지). 추가 작업 없음 |

### C. 마이페이지 라우팅

| 항목 | 위치 | 확인 결과 |
|---|---|---|
| 레이아웃 fragment | `templates/mypage/layout.html` L10~93 | 프로필 카드 + 세그먼트 탭 4개 (`history`·`favorites`·`noti`·`profile`) |
| 개인정보 수정 탭 경로 | `?tab=profile` 쿼리 파라미터 | `MyPageController.mypage()` L68 에서 분기 |
| 개인정보 수정 템플릿 | `templates/mypage/profile-edit.html` | **비밀번호 변경 입력쌍이 이미 폼 내부에 있음** (L37~73 — `editNewPassword` + `editNewPasswordConfirm`) → **선행 결정 #1 과 상충. 사용자 결정 필요 (Qb)** |
| 비밀번호 변경 링크 추가 지점 | `profile-edit.html` L35~73 블록 | 기존 입력쌍 **대체** 또는 **별도 행 추가** 필요 |

**사용자 결정 필요 (Qb)**:
현재 `profile-edit.html` 에는 "새 비밀번호 / 새 비밀번호 확인" 입력쌍이 폼 안에 **직접 노출**되어 있고 (선택 입력, 빈 값이면 유지), `ProfileUpdateRequest.password` 로 함께 저장되는 구조. 선행 결정 #1 ("개인정보 수정 탭 안 '비밀번호 변경' 행 + 링크") 와 모순.
- **Qb-A**: 기존 입력쌍을 **제거**하고 "비밀번호 변경" 행(링크) 으로 교체. `ProfileUpdateRequest.password` 필드 제거. 자발적 비밀번호 변경은 새 화면(`/mypage/password`)에서만 수행 — **분리 완결성 최상, 기존 저장 플로우 리그레션 1건 (profile-edit submit 시 비밀번호 무시)**
- **Qb-B**: 기존 입력쌍 **유지** 하고 그 아래 "현재 비밀번호 재인증이 필요한 변경은 새 화면에서" 안내 링크 추가. 두 경로 공존 — **사용자 혼란 가능. 테스트 복잡도 증가**
- **Qb-C**: 기존 입력쌍 **유지 (현상 유지)**, 별도 `/mypage/password` 화면 추가 안 함 — Part 2 범위 축소. 하지만 "현재 비밀번호 재확인" 요구 (#5) 불가능 → **선행 결정 #4·#5 와 상충**

→ **권장: Qb-A**. 선행 결정 전체와 가장 정합. Part 2 의 "자발적 변경 화면 분리" 명시 목적에도 부합.

### D. 모바일 반응형 시안

`docs/00_assets/Mobile Responsive.dc.html` 를 grep 한 결과 — **"비밀번호 변경" 전용 모바일 화면 섹션 없음**. 등장 라인:
- L949 — 로그인 화면의 "비밀번호 찾기" 링크
- L997~998 — 회원가입 폼의 비밀번호 / 확인 입력쌍

**결론**: 모바일 전용 디자인 자산이 없으므로 **데스크톱 디자인 + 반응형 CSS 로 커버**. 마이페이지 레이아웃 (`.mypage-page`) 의 기존 모바일 반응형 breakpoint 를 그대로 상속하면 됨.

### E. 디자인 수정 slot 2건 (#4·#5)

prototype.tsx L1491~1518 (profile-edit 내 비밀번호 변경 영역) 를 보면:

| slot | 현재 디자인 (prototype) | 상태 |
|---|---|---|
| 현재 비밀번호 필드 아래 inline error (`-current-error`) | ❌ **없음** — profile-edit 는 현재 비밀번호 요구 자체 안 함 (pwVerified Step1 통과 전제) | **디자인 수정 필요** |
| 새 비밀번호 필드 아래 inline error (`-new-error`) | ⚠️ **"비밀번호가 일치하지 않습니다."** 메시지만 있음 (L1518 — newPw2 blur) | **"현재 비밀번호와 다른 비밀번호를 입력해야 합니다." inline 추가 필요** |
| 변경하기 CTA | prototype 은 "저장" 버튼 (L1549) 공유 | **독립 "변경하기" CTA 필요** |

→ **디자인 수정 요청 범위 (사용자에게 전달 예정)**:
1. 현재 비밀번호 입력 필드 추가 + 아래 inline error slot `-current-error`
2. 새 비밀번호 필드 아래 inline error slot `-new-error` (이전 비밀번호 재사용 금지 + 정책 위반 메시지 공용)
3. 새 비밀번호 확인 필드 아래 불일치 inline error slot `-confirm-error` (blur 시 노출 — #7)
4. "변경하기" / "취소" CTA 2개 (취소는 바로 이동 — #9)
5. 모바일 반응형 — 기존 `.mypage-page` breakpoint 상속

### 디자인 파일 경로 질문 (사용자에게)

> 사용자가 "디자인 수정 요청" 을 예고한 디자인 산출물 경로를 알려주세요. 현재 저장소에는 `/mypage/password` 전용 디자인 자산이 보이지 않습니다. (예: Figma URL · 신규 prototype 섹션 · 별도 HTML 시안 등)

---

## 1. 선행 결정 9건 (재논의 금지 — 사용자 확정)

| # | 결정 |
|---|---|
| 1 | 진입: 개인정보 수정 탭 안 "비밀번호 변경" 행 + 링크. 뒤로가기 복귀처 `/mypage?tab=profile` |
| 2 | URL 분리: `/mypage/password` 신설. 강제=`/password/change` 유지. testid 접두 공유 (`password-change-*`) |
| 3 | 어조 "~합니다" (서버 Bean Validation 재사용) |
| 4 | 이전 비밀번호 재사용 금지 적용. 문구 "현재 비밀번호와 다른 비밀번호를 입력해야 합니다." |
| 5 | 현재 비밀번호 틀림 — "현재 비밀번호가 올바르지 않습니다." inline. 횟수 제한 미도입 |
| 6 | 변경 후: 현재 기기 유지 + 다른 기기 세션 로그아웃. **⚠️ Qa 결정 필요** (조사 A 참조) |
| 7 | 불일치 메시지 — 확인 필드 blur 시 노출 |
| 8 | 변경하기 버튼 — 항상 활성, 제출 시 검증 |
| 9 | 취소 확인창 없음, 바로 이동 |

---

## 2. 변경 범위 (파일 단위)

- [ ] `src/main/java/.../user/UserPasswordChangeController.java` — **신규**. `@RequestMapping("/mypage/password")` + `@GetMapping` (폼) + `@PostMapping` (제출)
- [ ] `src/main/java/.../user/UserPasswordChangeRequest.java` — **신규** DTO. `currentPassword` + `newPassword` + `newPasswordConfirm`. Bean Validation 어노테이션 `PasswordResetRequest` 패턴 복붙
- [ ] `src/main/java/.../common/config/SecurityConfig.java` — **수정**. `userSecurityFilterChain` 의 `authenticated()` 매처에 `"/mypage/password"` 추가 (이미 `/mypage/**` 로 커버되지만 명시 선호). Qa-A 선택 시 `SessionRegistry` + `HttpSessionEventPublisher` 등록
- [ ] `src/main/resources/templates/mypage/password-change.html` — **신규** 템플릿. layout fragment 재사용 + 폼
- [ ] `src/main/resources/templates/mypage/profile-edit.html` — **수정**. Qb-A 선택 시 기존 비밀번호 입력쌍 (L35~73) 제거하고 "비밀번호 변경" 행 추가. 링크 `@{/mypage/password}`
- [ ] `src/main/java/.../user/ProfileUpdateRequest.java` — **수정** (Qb-A 선택 시). `password` / `passwordConfirm` 필드 제거
- [ ] `src/main/java/.../user/UserService.java` 또는 profile 저장 로직 — **수정** (Qb-A 선택 시). 비밀번호 저장 분기 제거
- [ ] `src/main/resources/static/css/main.css` — **추가**. `.mypage-password-change-*` 신규 블록 (`.mypage-edit-pw-*` 패턴 상속)
- [ ] `docs/design-contracts/user/mypage-password-change.md` — **신규** 디자인 계약 서술
- [ ] `e2e/contracts/user/mypage-password-change.ts` — **신규** 셀렉터 계약
- [ ] `e2e/tests/mypage-password-change.spec.ts` — **신규** 기능 E2E
- [ ] `src/test/java/.../user/UserPasswordChangeControllerTest.java` — **신규** `@WebMvcTest`

---

## 3. 셀렉터 명세 (data-testid)

testid 접두는 강제 변경 flow 와 공유 (#2):

| 요소 | data-testid | 비고 |
|---|---|---|
| 폼 전체 | `password-change-form` | 공통 접두 |
| 현재 비밀번호 input | `password-change-current` | 공통 접두 |
| 현재 비밀번호 inline error | `password-change-current-error` | **신규 slot** (디자인 수정) |
| 새 비밀번호 input | `password-change-new` | 공통 접두 |
| 새 비밀번호 inline error | `password-change-new-error` | **신규 slot** — 정책 위반 + 이전 PW 재사용 공용 |
| 새 비밀번호 확인 input | `password-change-confirm` | 공통 접두 |
| 새 비밀번호 확인 inline error | `password-change-confirm-error` | **신규 slot** — blur 시 불일치 |
| 변경하기 CTA | `password-change-submit` | 공통 접두 |
| 취소 CTA | `password-change-cancel` | **신규** — `/mypage?tab=profile` link |
| 성공 toast | `password-change-success` | 리다이렉트 후 flash |

Qa-A 선택 시 추가:
| 다른 기기 로그아웃 안내 | `password-change-session-notice` | "다른 기기에서 로그아웃되었어요." |

---

## 4. 필드 / 컴포넌트 명세

| 라벨 | 타입 | 필수 | 서버 검증 | 비고 |
|---|---|---|---|---|
| 현재 비밀번호 | password | ✓ | `@NotBlank` | 틀림 시 `password-change-current-error` |
| 새 비밀번호 | password | ✓ | `@Size(min=8)` + `@Pattern(영문+숫자)` | 정책 위반·이전 PW 재사용 금지 → `password-change-new-error` |
| 새 비밀번호 확인 | password | ✓ | 서버: `newPassword.equals(newPasswordConfirm)` | 클라이언트 blur 시 즉시 inline (#7) |

---

## 5. 상태 머신 (화면 흐름)

```
/mypage?tab=profile
  └─ [비밀번호 변경] 링크 클릭
       ↓
     /mypage/password  (GET)
       ├─ 유효 입력 + 제출 (POST)
       │    ├─ 성공 → redirect /mypage?tab=profile (flash: "비밀번호를 변경했습니다.")
       │    │           + Qa-A 시: 다른 기기 세션 expireNow
       │    │           + Qa-C 시: /logout → /login?changed=1
       │    └─ 실패 → 같은 폼 재렌더 + 필드별 inline error (입력값 보존 X — 보안)
       └─ [취소] → /mypage?tab=profile (바로 이동, 확인창 없음 #9)
```

---

## 6. 갭 리스트

| # | 항목 | 현재 상태 | 명세 | 우선순위 |
|---|---|---|---|---|
| 1 | `/mypage/password` 라우트 | ❌ 없음 | 신설 | 높음 |
| 2 | 자율 비밀번호 변경 Controller | ❌ 없음 (강제 `/password/change` 만) | `UserPasswordChangeController` 신설 | 높음 |
| 3 | profile-edit 안 비밀번호 입력쌍 | ✅ 있음 (L35~73) | **Qb-A 선택 시 제거** | **Qb 결정 대기** |
| 4 | 세션 레지스트리 | ❌ 미등록 | **Qa-A 선택 시 등록** | **Qa 결정 대기** |
| 5 | 디자인 계약 (md + ts) | ❌ 없음 | 신설 | 높음 |
| 6 | 모바일 전용 시안 | ❌ 없음 | 데스크톱 디자인 + 반응형 CSS | 중 |
| 7 | 디자인 slot 3종 (-current-error · -new-error · -confirm-error) | ❌ prototype 에 없음 | **디자인 수정 필요** | 높음 |

---

## 7. 검증 시나리오 (ym-qa 가 실행)

### 정적 검증
- `./gradlew compileJava test --tests UserPasswordChangeControllerTest`
- `*RenderTest` — `/mypage/password` GET 렌더 (fragment 포함)
- 디자인 계약 검사 — `cd e2e && npx playwright test --project=contracts` 갭 0

### 동적 검증 (curl + bootRun 8090 e2e 프로파일)
- `GET /mypage/password` 로그인 전 → 302 → `/login`
- `GET /mypage/password` 로그인 후 → 200 + `password-change-form` 렌더
- `GET /mypage/password` mustChangePassword=TRUE 사용자 → 302 → `/password/change` (interceptor 유지 확인)
- `POST /mypage/password` 성공 → 302 → `/mypage?tab=profile` + flash
- `POST /mypage/password` 현재 PW 틀림 → 같은 폼 재렌더 + `password-change-current-error`
- `POST /mypage/password` 이전 PW 재사용 → `password-change-new-error`
- `POST /mypage/password` 불일치 → `password-change-confirm-error`

### 인터랙션 E2E (`e2e/tests/mypage-password-change.spec.ts`)
- TC-1 : 자율 변경 왕복 — 로그인 → `/mypage?tab=profile` → "비밀번호 변경" 클릭 → `/mypage/password` → 입력 → 성공 → `/mypage?tab=profile` + toast
- TC-2 : 현재 PW 틀림 — inline error 노출
- TC-3 : 이전 PW 재사용 — inline error 노출
- TC-4 : 확인 불일치 — blur 시 inline error (#7)
- TC-5 : 취소 → `/mypage?tab=profile` (확인창 없음)
- TC-6 : 강제 변경 사용자 (`mustChangePassword=TRUE`) 가 `/mypage/password` 접근 → `/password/change` 로 redirect
- TC-7 (Qa-A 시) : 2개 세션 동시 로그인 → 변경 → 다른 세션 다음 요청 → 로그인 리다이렉트
- TC-8 (Qa-C 시) : 변경 후 전체 로그아웃 → `/login?changed=1`

### 시각 검증 (사용자)
- 모바일 폭 (375px · 320px) 레이아웃 깨짐 없음
- inline error 색상·간격 디자인 시안 정합
- 눈 아이콘 토글 (`profile-edit.html` 와 동일 패턴)

---

## 8. 의존성 / 선행 작업

- ✅ A5-1 (#219) · ✅ `PasswordChangeController` · ✅ `PasswordChangeRequiredInterceptor` 완비
- ⚠️ **Qa (세션 무효화) · Qb (profile-edit 입력쌍 처리) 결정 필요** — ym-impl 착수 전 사용자 응답 필수
- ⚠️ **디자인 자산 (경로 · 수정 slot 3종) 전달 필요** — 사용자 응답 후 계약 신설

---

## 9. 작업 큐 메타

| 항목 | 값 |
|---|---|
| 작업 ID | A5-1-e2e-part2-user |
| 우선순위 | 중 — A5-1-e2e Part 1 머지 후 착수 |
| 추정 단위 | 1 PR (Qa-A 선택 시 1.5 PR) |
| 상태 | `spec_investigation` |

---

## 사용자 응답 요청 (ym-spec → ym-impl 전 필수)

| ID | 질문 | 옵션 |
|---|---|---|
| **Qa** | 변경 후 다른 기기 세션 처리 (선행 #6 재확인) | A: `SessionRegistry` 신규 구현 (LOC ~70) / B: 범위 제외 (다른 기기 그대로 유효) / C: 전체 로그아웃 후 재로그인 |
| **Qb** | `profile-edit.html` 기존 비밀번호 입력쌍 처리 | A: 제거하고 링크로 교체 (권장) / B: 유지 + 안내 링크 병존 / C: 현상 유지 (Part 2 축소) |
| **Qc** | 디자인 수정 자산 경로 | Figma URL · 신규 prototype 섹션 · 별도 시안 HTML 등 |
| **Qd** | 디자인 수정 slot 3종 (`-current-error` · `-new-error` · `-confirm-error`) 디자인 반영 담당 | 사용자 직접 수정 / ym-impl 가 디자인 토큰 기반으로 CSS 결정 |

사용자 응답 수신 시 → `spec_confirmed` 로 전환 후 ym-impl 인계.
