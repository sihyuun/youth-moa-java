# 적대적 검증 리포트 — FOLLOW-waitlist-auto-approve (2026-10-08)

> 역할: ym-verify (파이프라인 최종 관문, refute-first). ym-qa 3차 "머지 가능" 판정을 **반박** 시도.
> 범위: diff 전수 + 소스 재검증 + 공격 포인트 10개 중 해당 항목.
> 환경: Windows / JDK 17 bootstrap / Gradle wrapper / 코드 수정 없음 (read-only).

---

## 판정 요약

| 결과 | 건수 |
|---|---:|
| **PASS** (반박 실패) | 9 |
| **FAIL** (반박 성공, 블로커) | 0 |
| **UNVERIFIED** (판정 불가) | 2 |

→ **최종: 머지 가능.** QA 3차 판정 유지. UNVERIFIED 2건은 사용자/운영 영역이라 머지 블로커 아님.

---

## 항목별 판정

| # | 공격 벡터 | 실행 방법 | 판정 | 근거 |
|---|---|---|---|---|
| 1 | Q1 boolean + approvalMode AUTO 조합 꼬임 | 소스 정독 — `WaitlistPromotionService.promoteIfEligible` + `Program.java:181,239` | PASS | 승격 서비스는 `isAutoApproveWhenFull()` 와 `capacity != null` 만 분기. approvalMode 미참조. AUTO 모드는 신청 시점 자동 승인이라 PENDING 자체가 쌓이지 않아 `findFirstByProgramIdAndStatusOrderByAppliedAtAsc(…, PENDING)` 가 empty → no-op. 로직 꼬임 없음. |
| 2 | cancel 4번째 경로 누락 | `grep CANCELLED \| forceCancel \| \.reject\(` 전수 | PASS | 운영 호출 사이트 3경로 확정: `AdminApplicationService.reject(:205)` / `AdminApplicationService.forceCancel(:233)` / `ApplicationService.cancel(:390)`. `AdminApplicationBulkService` 는 `bulkApprove` 만 있고 reject/cancel 없음. `ApplicationService.reject(:336-352)` 는 외부 Controller 미호출(테스트 전용) → 운영 트리거 X. 승격 트리거 누락 유출 없음. |
| 3 | sysadmin 시드 존재 | `grep sysadmin@youth-moa` + `DataInitializer.java:195,198` | PASS | `existsByEmail` idempotent 체크 + 시드 생성. `AdminSeedInitializerTest` 가 CI 로 보증. `WaitlistPromotionService.SYSTEM_ADMIN_EMAIL` 상수와 일치. 시드 누락 시 `IllegalStateException` 로 즉시 fail-fast. |
| 4 | OFF→ON 전환 시 기존 PENDING | 소스 — `AdminProgramController:342`, `WaitlistPromotionService:60`, 테스트 ③ | PASS | 토글 endpoint 는 `Program.enable/disable…` 만 호출하고 승격 트리거 없음. 다음 CANCELLED/REJECTED 이벤트에서 비로소 승격 — Q4 결정 그대로. `ApplicationServiceAutoApproveTest ③` 가 "ON 후 CANCELLED 발생 → 승격" 흐름 커버. |
| 5 | 배너 조건 UX 모호 (approved=0, pending>>capacity) | spec Q5 결정 재확인 | PASS | 사용자 결정 Q5 그대로 반영. "꽉참" 의미를 approved+pending ≥ capacity 로 넓게 정의한 것은 명시적 선택. deviation 아님. |
| 6 | 승격 race condition (동시 CANCELLED 2건) | `promoteIfEligible` + `@Transactional REQUIRED` 추론 | PASS | 공석 1개 = CANCELLED 1건 = 트랜잭션 1건 → 트리거 1회. 공석 2개 생기면 트리거 2회 실행 → PENDING 2건 각각 승격 (정합 유지). 동일 공석 1개에 승격 2건 중복 발생 시나리오 성립 불가. 다만 `>=` 가드가 가장 강한 보호선. |
| 7 | CLAUDE.md 함정 (`th:if` + `th:replace`) | `detail.html:174-178` + grep | PASS | wrapper div 에 `th:if="${waitlistBannerVisible}"`, 내부 div 에 `th:replace` 로 **올바르게 분리**. 같은 요소에 병치 금지 조항 준수. 주석(L173-175) 에 함정 명시. |
| 8 | spec capacity=1 꼼수의 커버리지 한계 | e2e spec 설계 + JUnit TC 조합 재검토 | PASS | Playwright 는 capacity=1 로 "경계값" 검증. JUnit `ApplicationServiceAutoApproveTest` 4건 + `AdminApplicationServiceAutoApproveTest` 3건이 capacity=1 로 승격/비승격 전수 조합 커버. capacity N≥2 는 로직 분기점이 아니라(`approvedCount < capacity` 비교뿐) 추가 TC 가치 낮음. |
| 9 | gap-reports regenerate | `e2e/gap-reports/` 디렉토리 존재 | PASS | contracts diff 는 `admin-program-detail.ts` 의 ID/selector/desc 만 수정 (expected=0 유지, P1 severity 유지). QA 2차에 45/45 PASS 승계 — contract 수정이 구현 변경 없이 "미노출" 체크를 유지하므로 재실행 불필요. |
| 10 | impl 3차 셀프체크 (1 TC, 19.6s) 충분성 | QA 3차 리포트 재확인 | PASS | 3차 impl 변경이 TS spec 1개 파일 (`admin-waitlist-auto.spec.ts`) 로 국한. JUnit·Controller·Template·CSS 비변경이라 2차 전수 (761/761) 승계 로직 성립. `admin-program-detail.spec.ts` regression 3/3 PASS 가 "주변 영향 없음" 보증. |
| V1 | compile 재검증 | `./gradlew.bat compileJava compileTestJava` | PASS | BUILD SUCCESSFUL (34s). |
| V2 | 신규 테스트 재실행 | `./gradlew.bat test --tests '*AutoApprove*' --tests 'AdminProgramDetailRenderTest' --tests 'ApplicationServiceAutoApproveTest'` | PASS | BUILD SUCCESSFUL (2m 4s). H2 teardown 깔끔. |
| V3 | endpoint RBAC | `AdminProgramController:60` 클래스 레벨 `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CENTER_ADMIN')")` + `AdminProgramService.find(id)` scope 체크 | PASS | 메서드 레벨 어노테이션 없이도 클래스 레벨에서 상속. CENTER_ADMIN 자기 센터 아닌 경우 `find(id)` 가 `IllegalAccessError` → Controller catch → `AccessDeniedException`. 격리 유지. |
| U1 | Supabase 환경 Flyway V28 실 적용 | — | UNVERIFIED | 머지 후 Deployment 단계 `validate` 가 자동 수행. 로컬 PG Testcontainers 없이 바로 운영에 적용되므로 머지 후 1차 부팅 모니터링 필요. 블로커 아님 (V28 자체는 단순 `ALTER TABLE ADD COLUMN DEFAULT false`, downside 낮음). |
| U2 | 시각·감성 영역 (배너 UX, 토글 애니메이션, flashMessage 톤) | — | UNVERIFIED | 사용자 브라우저 확인 영역. QA 1~3차 모두 미실행. 블로커 아님. |

---

## FAIL 상세 (ym-impl 반려 사항)

**없음.**

---

## UNVERIFIED 상세

### U1 — Supabase 실 적용
- 사유: 로컬 Testcontainers PG 환경에서의 V28 사전 검증 미수행. H2 기반 e2e 프로파일만 커버됨.
- 검증 조건: 머지 → 다음 bootRun 에서 Flyway validate 통과 (`ddl-auto: validate` 가 schema 일치 확인).
- 리스크: 낮음 — V28 은 단일 컬럼 추가, NOT NULL + DEFAULT false 백필. 기존 row 손상 가능성 없음.

### U2 — 사용자 시각/감성 확인
- 사유: 배너 노란 톤, 토글 OFF/ON 상태 애니메이션, flashMessage "대기자 자동 승인을 켰어요/껐어요" 문구 톤.
- 검증 조건: 사용자가 로컬 `http://localhost:8090/admin/programs/{id}` (capacity 꽉 참 조건 충족 프로그램) 접근 후 눈으로 확인.
- 리스크: 낮음 — prototype admin L1580~1592 markup 재현 명시, CSS 변수 (`--color-warning-light`) 사용 — 디자인 토큰 일관.

---

## 추가 발견 (observability)

1. **데드코드 후보 — `ApplicationService.reject(:336-352)`**: 운영 Controller 에서 호출되지 않고 `ApplicationServiceStatusChangeTest`, `ApplicationNotificationListenerTest` 만 사용. 승격 로직이 걸려 있지 않아 "장차 이 메서드로 운영 reject 가 라우팅되면 승격 트리거 유출" 리스크. 현재 티켓 범위 밖이지만 후속 리팩터 티켓으로 `AdminApplicationService.reject` 로 통합 또는 승격 트리거 복제 검토 권장.
2. **spec 3-3 vs impl 구조 차이**: spec 은 "private helper 로 직접 호출" 명시했으나 impl 은 `WaitlistPromotionService` 별도 Service 로 분리. 근거(Javadoc L26-27): "AdminApplicationService / ApplicationService 양쪽 depend-on 하므로 순환 의존 방지". **정당한 이탈** — spec Q2 결정("② 대기자 승격") 의 **의도**를 더 깨끗하게 구현. 명세 vs 실제 구현 매핑표(spec §4) 는 3차에서 미갱신 상태이나 테스트가 동작으로 보증.

---

## 결론

QA 3차의 "머지 가능" 판정에 대한 반박 시도는 모두 실패했습니다. 10개 공격 포인트 중 FAIL 로 승격시킬 수 있는 항목이 없고, UNVERIFIED 2건은 성격상 사후 검증 영역입니다.

**머지 승인.** 커밋 후 Supabase 1차 부팅 모니터링과 사용자 시각 확인만 사후 수행하면 됩니다.
