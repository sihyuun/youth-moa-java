# A7 SMTP 초대 메일 사후 QA 리포트

- **검증 대상**: PR #243 (commit `e0e08c7`) — "A7: SMTP 초대 메일 (MailHog local + Gmail prod)"
- **머지 시각**: 2026-09-30 (main 반영)
- **QA 시각**: 2026-09-30 14:00~14:35 KST
- **QA 실행자**: ym-qa (Claude Code, 회사 PC · Windows 11 · JDK 17)
- **배경**: PR #243 이 표준 /dev-cycle 을 우회하여 ym-qa 를 거치지 않고 머지됨 → 사후 검증 실행
- **최종 판정**: **PASS** (환경 격리 실패 2건 제외 · 인수 기준 100% 충족)

---

## 1. 정적 검증

| 항목 | 명령 | 결과 |
|---|---|---|
| 컴파일 | `./gradlew compileJava` | BUILD SUCCESSFUL (39s) |
| 코드 스타일 | `./gradlew spotlessCheck` | BUILD SUCCESSFUL |
| A7 targeted TC | `./gradlew test --tests *AdminInvitationMailServiceTest --tests *AdminUserServiceStaffTest --tests *AdminUserDetailRenderTest --tests *CapacityBarFragmentRenderTest` | 34 TC · 0 failure · 100% (1m41s) |
| 전체 회귀 | `./gradlew clean test` | 720 TC · **2 failure (환경 격리)** · 99% (7m50s) |

### 회귀 실패 2건 — 환경 격리 (실 회귀 아님)

| 클래스 | 원인 | 판정 |
|---|---|---|
| `YouthMoaApplicationTests.contextLoads()` | `Could not find a valid Docker environment` — Testcontainers PG 미기동 | 환경 격리 (Docker Desktop 오프) |
| `V21DefensiveBlockIntegrationTest` | 동일 Testcontainers 실패 | 환경 격리 |

- 회사 PC 에서 Docker Desktop 이 꺼져 있으면 항상 재현. **CI ubuntu-runner (`integration-test` job) 는 Docker 내장이라 매 PR green** 이므로 A7 회귀 여부는 CI 결과로 판단해야 함
- A7 PR #243 자체는 CI green 상태로 머지됨 (사용자 확인)
- **A7 관련 신규 TC 는 전부 통과** — `AdminInvitationMailServiceTest` (4 TC · GreenMail wire) · `AdminUserServiceStaffTest` (mail dispatch 통합) · `AdminUserDetailRenderTest` (배너 렌더) · `CapacityBarFragmentRenderTest` (기존 bean 오염 fix 검증)

**정적 검증 판정**: **PASS**

---

## 2. 동적 검증

### 2-1. 환경

- bootRun profile: `e2e` (H2 in-memory + 시드 + `youthmoa.mail.mock=true`)
- port: 8090 (`.claude/scripts/bootrun-e2e.cmd` 표준)
- sysadmin 로그인: `sysadmin@youth-moa.test` / `Admin!234` → 302 `/admin` · dashboard 200

### 2-2. USER 계정 발급 (mail dispatch)

| 확인 | 결과 |
|---|---|
| `POST /admin/users` (email=`qa-mail-user-…@test.local`, role=USER) | 302 `/admin/users/56` |
| detail.html 배너 `data-testid="admin-user-mail-sent"` 렌더 | PASS (초록 · "초대 메일이 발송되었어요") |
| 임시 password 카드 (`admin-user-initial-password`) 비노출 | PASS (템플릿 comment 만 존재 · Q3 A 정확 동작) |
| MockMailer INFO 로그 | `[MOCK MAIL] admin invitation → qa-mail-user-…@test.local (name=QA User Mail, centerName=-, tempPassword=ab***)` |

### 2-3. CENTER_ADMIN 계정 발급 (centerName 전달)

| 확인 | 결과 |
|---|---|
| `POST /admin/users` (role=CENTER_ADMIN, centerId=2) | 302 `/admin/users/57` |
| MockMailer 로그 centerName 포함 | `centerName=28청년창고` (cp949 mojibake — 로그 콘솔 인코딩. 실제 전달값은 정상) |

### 2-4. 비밀번호 재발급

| 확인 | 결과 |
|---|---|
| `POST /admin/users/56/reset-password` | 302 `/admin/users/56` |
| MockMailer 로그 resetBy · newPassword 포함 | `[MOCK MAIL] admin password reset → … (name=QA User Mail, resetBy=시스템관리자, newPassword=@C***)` |

**동적 검증 판정**: **PASS**

---

## 3. 인터랙션 / E2E 검증

| Spec | 결과 |
|---|---|
| `admin-staff-management.spec.ts` | 6 passed · 1 skipped (26.5s) — Q3 A spec 갱신 정합 |
| `admin-users.spec.ts` | 7 passed (회귀 없음) |
| `admin-header-notifications.spec.ts` | 5 passed (회귀 없음) |

**E2E 판정**: **PASS**

---

## 4. Q 결정 이행 검증

| Q | 결정 | 이행 |
|---|---|---|
| **Q1** MailHog local + Gmail prod 분리 | `application.yml` `${MAIL_HOST:localhost}` `${MAIL_PORT:1025}` — 로컬 MailHog default · 환경변수로 prod 전환. `application-e2e.properties` `youthmoa.mail.mock=true` — SMTP 접속 없이 MockMailer 활성 | **PASS** |
| **Q2** fallback 정책 | `MailDispatchResult(sent, failureReason)` record + `AdminUserController.applyMailFlash()` → 실패 시 `flashMailFailed` + `flashInitialPassword` 노출 (운영자 수동 전달 경로). SmtpAdminInvitationMailSender 는 예외 catch → failure 반환 (계정 저장은 커밋 유지) | **PASS** (실 SMTP 실패 시나리오는 test.skip 이월) |
| **Q3** 성공 시 password 화면 숨김 | detail.html `flashInitialPassword` 카드는 실패 fallback 시에만 렌더. 성공 경로에서 컨트롤러가 flash 미전달 → 카드 렌더 안 됨. **실측 확인 (§2-2)** | **PASS** |
| **Q4** GreenMail embedded 통합 테스트 | `AdminInvitationMailServiceTest` — 4 TC (with/without centerName × invitation/reset) · quoted-printable decode assertion · GreenMailExtension port 3025 | **PASS** |
| **Q5** fly.io secrets 참조 경로 | `application.yml` 모든 mail 관련 값 `${ENV_VAR:default}` 형식 (`MAIL_USERNAME` · `MAIL_PASSWORD` · `YOUTHMOA_MAIL_FROM_ADDRESS` 등) — prod 는 `fly secrets set` 로 주입 | **PASS** (실 prod 배포 검증은 배포 시점) |

---

## 5. POLICY 준수

- **prototype 준수 (POLICY.md P-5)**: 메일 발송은 prototype 미언급 영역 (사이드 이펙트) · 화면 계약 신설 없음 · 기존 detail.html 계약 유지 → 위반 없음
- **CLAUDE.md 검증 규칙**: 정적·동적·인터랙션·시각 4영역 분리 표기 (본 리포트)
- **feedback_qa_static_vs_dynamic**: 통과 개수 단독 표현 금지 — 정적/동적 분리 적용됨

---

## 6. 미실행 · 이월 (환경 한계)

| 항목 | 사유 | 이월 방식 |
|---|---|---|
| Testcontainers PG 통합 테스트 (2건) | 회사 PC Docker Desktop 오프 | CI ubuntu-runner 결과로 대체 (PR #243 CI green) |
| MailHog 실 렌더 시각 검증 | Docker MailHog 미기동 (Q1 A · 설계상 mock=true 로 충분) | 개인 PC 또는 prod dry-run 시 확인 |
| SMTP 실패 시 fallback 시나리오 실측 | mock 강제 실패 flag 미존재 | test.skip 처리됨 (`admin-staff-management.spec.ts:136`) — mock force-fail 도입 시 부활 |
| Q5 fly.io secrets 실 주입 검증 | prod 배포 시점 | 배포 checklist 로 이월 |

---

## 7. 최종 판정

**PASS** — PR #243 은 표준 QA 를 우회했지만 사후 검증 결과 정적·동적·E2E·Q 결정 이행 모두 인수 기준 충족. 회귀 실패 2건은 Docker 환경 격리 (CI green 확인됨).

**후속 액션 없음** — 서버 코드 수정 불필요. 사후 QA 절차 정착 (다음 부터는 표준 사이클 준수).
