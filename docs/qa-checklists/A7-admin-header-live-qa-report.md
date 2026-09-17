# A7 admin 헤더 알림 벨 QA 리포트

- 브랜치: `feature/A7-admin-header-live`
- 커밋: `14fca6f` (spec `dbed897` 위 impl 23 파일 · +1582 LOC)
- 실행 환경: 회사 PC · Java 17 + Spring Boot 4 · bootRun e2e 프로파일(8090, H2 in-memory) · JDK 21 target
- 실행 일자: 2026-09-17

## 판정 요약

| 영역 | 결과 | 비고 |
|---|---|---|
| 1. 정적 | PASS (조건부) | 603 / 604 TC PASS. 1 FAIL = `YouthMoaApplicationTests` Testcontainers Docker 부재 → 회사 PC 한계, 회귀 아님 |
| 2. 동적 (curl 8090) | **FAIL 1건** | `/admin/notifications/badge` 응답이 `<span id="admin-notif-badge">` **2개** 반환 (id 중복 · fragment 이름 충돌) |
| 3. 계약 | PARTIAL | `admin-header-notifications.md` + `.ts` 신설되었으나 **이를 실행하는 spec 파일이 없음** → 계약 검사 미실행. admin-dashboard·admin-shell·admin-users 계약 4건 PASS (A1·A5·A6 무회귀) |
| 4. 기능 E2E | 회귀 PASS | `admin-header-notifications.spec.ts` 신설 X (spec 상 이월 명시). 기존 `notification-bell.spec.ts` 6/6 PASS · apply/signup 18/18 PASS |
| 5. 회귀 | **PASS** | 사용자 알림 flow 무영향 실증 (아래 참조) |
| 6. 시각 | 미확인 | 사용자 영역 대기 |

**최종 판정**: **재반려 (FAIL)**. 아래 §동적 검증 P0 사고 1건은 프로덕션 코드 수정 필요.

---

## 1. 정적 검증

### 1-1. compileJava + spotless
```
> Task :compileJava UP-TO-DATE
> Task :spotlessJavaCheck UP-TO-DATE
BUILD SUCCESSFUL in 15s
```

### 1-2. `./gradlew test` 전체 실행 (12m 11s)
```
604 tests completed, 1 failed
```

**실패 1건 분석**:
- 클래스: `io.github.sihyuuun.youthmoa.YouthMoaApplicationTests`
- 원인 (원문 인용):
  ```
  Caused by: BeanCreationException:
    ... Could not find a valid Docker environment.
    Please see logs and check configuration
  ```
- 판정: **환경 한계** — 회사 PC Docker Desktop 미기동. 신규 코드 회귀 아님. 회사 CI (ubuntu 러너 Docker 내장) 또는 개인 PC에서 재검증 필요.

### 1-3. A7 관련 테스트 통과 실측 (build/test-results/test/TEST-*.xml `<testsuite>` header 인용)
```
notification.admin.AdminNotificationEventListenerTest       tests="4"  failures="0" errors="0"
notification.admin.AdminNotificationRenderTest              tests="2"  failures="0" errors="0"
notification.NotificationTypeAdminEnumTest                  tests="4"  failures="0" errors="0"
```

### 1-4. 회귀 대상 notification 기존 테스트 (변경 없어야 함)
```
notification.ApplicationNotificationListenerTest    tests="4" failures="0"
notification.HeaderNotificationAdviceTest           tests="4" failures="0"
notification.NotificationChannelResolverTest        tests="4" failures="0"
notification.NotificationControllerTest             tests="9" failures="0"
notification.NotificationRepositoryTest             tests="3" failures="0"
notification.NotificationServiceDeleteTest          tests="3" failures="0"
```
→ 27 TC 전건 PASS. 기존 사용자 알림 flow 무회귀 실증 (회귀 방어 3점 #1).

---

## 2. 동적 검증 (bootRun 8090 · e2e 프로파일)

### 2-1. 로그인 & 인증 실측
```
POST /admin/login (sysadmin@youth-moa.test / Admin!234)
  → 302 Location: http://localhost:8090/admin  ✅
```

### 2-2. endpoint 응답 코드
| endpoint | status | 응답 크기 |
|---|---|---|
| GET /admin | 200 | 23735 bytes |
| GET /admin/notifications/dropdown | 200 | 232 bytes |
| GET /admin/notifications/badge | 200 | 245 bytes |
| POST /admin/notifications/mark-all-read (HX-Request:true) | 200 | 232 bytes |

### 2-3. 격리 검증
```
USER 계정 (seed1@youth-moa.test) → GET /admin/notifications/dropdown → 403  ✅
USER 계정                        → GET /notifications                → 200  ✅ (기존 flow 무영향)
```

### 2-4. **FAIL P0 — 배지 fragment 이름 충돌로 인한 id 중복 렌더**

`GET /admin/notifications/badge` 응답 원문 (245 bytes, `id="admin-notif-badge"` 2회 등장):

```html
<span id="admin-notif-badge"
      class="admin-notif-badge"
      aria-live="polite" hidden="hidden"></span><span id="admin-notif-badge"
      class="admin-notif-badge"
      data-notif-badge="0"
      aria-live="polite" hidden="hidden"></span>
```

또한 `/admin` 초기 렌더에서도 `admin-header-bell` 내부에 **동일 id 를 가진 span 이 2개** 렌더됨 (admin.html L99~104 실측):
```html
<span id="admin-notif-badge"
      class="admin-notif-badge"
      data-notif-badge="0"
      aria-live="polite" hidden="hidden"></span><span id="admin-notif-badge"
      class="admin-notif-badge"
      aria-live="polite" hidden="hidden"></span>
```

**정량 지표** (regex match count):
- `id="admin-notif-badge"` count = **2** (HTML 명세 위반 — id 는 문서 내 유일해야 함)
- `data-notif-badge` count = **1** (두 span 중 하나만 속성 보유)

**원인**: `templates/admin/fragments/_notification-badge.html` 에 동일 이름 `badge` 인 fragment 가 **두 정의 (L15 `badge(count)` + L27 `badge`)** 로 공존. Thymeleaf 는 `~{template :: badge(...)}` 호출 시 이름이 일치하는 fragment 를 모두 매치해 두 span 모두 렌더.

**폴링 영향**: `hx-select="#admin-notif-badge"` 는 첫 번째 매치만 취하므로 polling 후 배지에 `data-notif-badge` 속성이 소실됨. 계약 `[data-notif-badge]` selector 로 접근하는 후속 인터랙션 spec 이 실패할 것.

**계약 미검출 원인**: 계약 `#admin-notif-badge exists` 는 `kind: exists` (존재 여부만) 로 정의 → id 중복은 검사되지 않음.

**수정 방향 (프로덕션 · ym-impl 재반려 사유)**:
- 옵션 A (권장): fragment 정의를 하나로 통합. header.html 의 `th:replace` 호출에서 `${adminHeaderUnreadCount}` 를 그대로 넘기고, argumentless 정의를 제거. Controller `badge()` 도 `badge(${adminUnreadCount})` 로 명시 호출.
- 옵션 B: argumentless fragment 이름을 `badgeFromModel` 등으로 분리.

---

## 3. 계약 검사

### 3-1. A7 신규 계약 실행 여부
- 계약 파일 `docs/design-contracts/admin/header-notifications.md` + `e2e/contracts/admin-header-notifications.ts` **신설 확인**
- **실행 spec 파일 없음**: `e2e/tests/` 에 `visual-admin-header-notifications.spec.ts` 부재 (`grep -rn adminHeaderNotifications e2e/` 결과 계약 파일 내부 자기 참조만 매치)
- `npx playwright test --project=contracts --grep admin-header-notifications` → `Error: No tests found`
- 결과: 계약이 정의만 되고 실행되지 않음. **impl 이 spec 파일도 함께 신설해야 갭 0 판정 가능**. (spec §10-3 은 `--project=contracts` 갭 0 을 요구하지만 실질적 갭 검사가 이뤄지지 않음)

### 3-2. A1·A5·A6 회귀 계약 검사 (헤더 변경 파급 확인)
```
[1/4] visual-admin-dashboard.spec.ts       ✅
[2/4] visual-admin-shell.spec.ts           ✅   (헤더 GNB · 로고 · 유저 드롭다운 A1 무회귀)
[3/4] visual-admin-users.spec.ts (목록)     ✅
[4/4] visual-admin-users.spec.ts (상세)     ✅
4 passed (34.7s)
```
→ 회귀 방어 3점 #3 (admin/fragments/header.html A1·A5 무회귀) **실증**.

---

## 4. 기능 E2E 검증

### 4-1. 신규 인터랙션 spec 미신설
- spec §14 `A7-createdBy-recipient` 등 후속 티켓 목록에 `admin-header-notifications.spec.ts` 이월 언급 없음. 계약 파일 L120 dropdown.width 만 `deferred: tests/admin-header-notifications.spec.ts` 로 이월 표기.
- CLAUDE.md §인터랙션 검증 규칙: "HTMX 속성 신설 시 기능 E2E 최소 1개 함께 신설" → **불충족**. `admin-header-bell` `hx-get` `hx-swap` `hx-trigger` `hx-select` 신규 도입에도 클릭 시나리오 E2E 없음.
- 이 항목은 **P1 이월** — 프로덕션 회귀는 아니지만 재발 방지 규칙 위반.

### 4-2. 회귀 방어 3점 #2 실증 (apply/signUp 트랜잭션 무영향)
```
apply-complete + apply-dynamic-response + apply + login (apply) + qa-dynamic-admin-term + signup
→ 18 passed (1.4m)
```
→ `@TransactionalEventListener(AFTER_COMMIT)` 정합 + 이벤트 발행 예외 격리로 apply/signUp 자체는 100% 성공 유지.

### 4-3. 사용자 알림 flow 무영향 실증 (회귀 방어 3점 #1)
```
notification-bell.spec.ts
→ 6/6 passed (29.9s)
  · 비로그인 종 아이콘 미노출
  · 로그인 시 종 + unread dot
  · 종 클릭 시 드롭다운 4건 + unread 배지
  · 항목 title/message/link 렌더
  · "모두 읽음" OOB swap
  · 전체보기 → /notifications
```

---

## 5. 회귀 검증 (사용자 명시 3점 · 최우선)

| # | 항목 | 판정 | 실측 근거 |
|---|---|---|---|
| 1 | 사용자 알림 flow (PR #143) 무영향 | **PASS** | `NotificationType` 기존 6종 무변경 (enum 파일 L4~9 소스 확인) · `/notifications` 200 · notification-bell E2E 6/6 · notification/*Test 27/27 |
| 2 | `ApplicationService.apply()` · `UserService.signUp()` 무영향 | **PASS** | `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)` + try/catch 예외 격리 (Listener L56~120 소스 확인) · apply/signup E2E 18/18 |
| 3 | admin/fragments/header.html A1·A5 무회귀 | **PASS** | admin-shell·dashboard·users 계약 4/4 PASS · GNB 6개 항목 · 로고 · profile 드롭다운 위치 유지 (source diff 확인) |

---

## 6. 시각 확인 (사용자 영역 · 대기)

- 다크 헤더 배경 위 배지 색 대비 (E72D0F vs 111827)
- 드롭다운 라이트 카드 톤 · 300px 폭 (**FAIL P0 수정 후 재확인 필요**)
- unread dot 우측 세로 스택 (X 위, dot 아래)
- CENTER_ADMIN 로그인 시 organization 매칭 시나리오

---

## 7. 처리 규칙 판정

### 프로덕션 수정 대상 (재반려 · ym-impl 인계)
- **P0**: `_notification-badge.html` fragment 이름 충돌 → id 중복 렌더. §2-4 옵션 A/B 중 택1.

### 이월 (본 티켓 재작업 불필요)
- **P1**: `visual-admin-header-notifications.spec.ts` 신설. CLAUDE.md 인터랙션 규칙 준수 목적. 후속 티켓 `A7-interaction-spec` 로 관리.
- **환경**: `YouthMoaApplicationTests` — 개인 PC 또는 CI ubuntu 러너에서 재검증.

### 통과 (진행 가능)
- 회귀 3점 전부 PASS
- 신규 테스트 10 TC PASS
- endpoint 응답 코드·격리 정상

---

## 8. 다음 단계

1. ym-impl 재반려: fragment 이름 충돌 수정 (옵션 A 권장 — 정의 1개로 통합)
2. 재검증 시 `/admin/notifications/badge` 응답에 `id="admin-notif-badge"` 정확히 1회, `data-notif-badge` 속성 포함 확인
3. `/admin` 초기 렌더에서도 동일 검증
4. 시각 확인은 P0 수정 후 사용자 브라우저 실측
