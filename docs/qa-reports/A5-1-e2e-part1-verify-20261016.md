# 적대적 검증 리포트: A5-1-e2e Part 1 (Q1 + Q3 + Q6)

| 메타 | 값 |
|---|---|
| 작업 ID | `A5-1-e2e-admin-staff-management-part1` |
| 브랜치 | `feature/A5-1-e2e-admin-staff-management-part1` |
| HEAD | `1978531` |
| 검증 일시 | 2026-10-16 |
| 환경 | 회사 PC · 코드/계약 정독 (read-only) · bootRun 미기동 (ym-qa 리포트 선행 참조) |

---

## 판정 요약

- **PASS 10건 / FAIL 0건 / UNVERIFIED 2건 / LOW 지적 2건**
- **머지 가능 여부**: **조건부 가능** — 머지를 차단할 FAIL 없음. UNVERIFIED 2건은 "회사 PC bootRun 미기동 상태에서 저는 재실행 불가" 성격이며 ym-qa 리포트에 PASS 로 명시된 범위. LOW 지적 2건은 머지 블로커 아님.

---

## 항목별 판정

| # | 공격 벡터 | 실행 방법/근거 | 판정 | 요약 |
|---|---|---|---|---|
| 1-a | Q1 outer finally 가 force-fail OFF 를 호출하는가 | `admin-staff-management.spec.ts` L205~209 | PASS | outer finally 에서 `/__test__/mail/force-fail?enabled=false` 호출 확인 |
| 1-b | 중간 throw 시 다음 TC 오염 방지 | 동 L138~210 try/finally 구조 + `beforeEach` 가드 유무 | PASS (약함) | JS try/finally semantics 상 throw 시에도 outer finally 실행. 다만 beforeEach reset 가드는 없음 — TC-7 의 자체 try/finally 가 추가 안전망 |
| 1-c | force-fail state 범위 (session vs process) | `MockAdminInvitationMailSender.java` L46 `AtomicBoolean` singleton | PASS | process-global singleton. 복구 미실행 시 전체 후속 spec 오염 가능 — outer finally 로 방어 |
| 3-a | Q3 2 variant 전환 메커니즘 | `visual-admin-user-detail-after-issue.spec.ts` L35~84 | PASS | mail-sent variant 는 forceFail OFF 상태 그대로 발급, mail-fallback 은 ON 설정 후 발급 + finally OFF 복구 |
| 3-b | Q3 셀렉터 실존 여부 | `templates/admin/user/detail.html` grep | PASS | L50, L62, L81, L92, L198 모두 data-testid 존재. 계약 md 라인 번호와 완전 일치 |
| 3-c | deviation 표기 명시 | `docs/design-contracts/admin/user-detail-after-issue.md` "prototype 전제 (deviation)" 섹션 | PASS | prototype 미명시임을 명시적으로 선언, A5-1 보안 정책에 근거 |
| 6-a | maskPassword 공개 API 간접 검증 vs reflection | `MockAdminInvitationMailSenderTest.java` L41~44 ListAppender | PASS | Logback ListAppender 로 공개 API 호출 결과 로그 캡처. "실제 발송 경로가 안전한 포맷으로 로그를 남기는가" 까지 검증 — reflection 보다 robust |
| 6-b | raw password 미노출 검증 | 동 L67 `doesNotContain(rawPassword)` | PASS | `SuperSecret9!` 미노출 assertion. force-fail 경로에서는 애초에 maskPassword 호출 안 함 (template arg 자체 부재) → 역시 raw 미노출 |
| 6-c | null/empty 안전성 | 동 L111~127 + 소스 L100 `p.length() < 2` 분기 | PASS | null, "a" (len 1), "" (len 0) 모두 `"***"` fallback 분기 적용. NPE 발생하지 않음 |
| 5 | 기존 테스트 회귀 | `git diff main...HEAD --name-only` | PASS | admin-staff-management.spec.ts 변경은 TC-6 의 skip 해제·재구성에 국한. 다른 admin spec (admin-users, admin-user-detail, admin-users-actions) 미수정. production Java 소스 미수정 |
| 3-d | Q3 fallback 의 `initial.password.value.not.empty` 체크 실효성 | `admin-user-detail-after-issue.ts` L153~158 + `runner.ts` L52~56 | **LOW 지적** | `kind: 'attr-not-empty', attr: 'data-testid'` — `data-testid` 속성값은 항상 `"admin-user-initial-password-value"` 로 비어있지 않음. runner 가 getAttribute(attr) 로 검사하는데, "요소 안의 text (= 실 password)" 를 검사하지 않음. 사실상 `exists` 와 동치가 됨. 요소 자체는 다른 체크(`initial.password.card.exists`)로 커버됨 — 중복 안전망 효과만 있음. desc 와 동작 괴리 |
| 4 | spec L5 메타 vs L346 상태 라벨 | `docs/specs/A5-1-e2e-admin-staff-management.md` | **LOW 지적** | L5 `spec_confirmed (Part 1)` vs L346 `spec_draft`. L346 은 §11 작업 큐 메타 블록에 남아있는 이전 라벨. 머지 블로커 아님 |
| 4 (STATE) | STATE.md 완료 범위 표기 | `docs/STATE.md` 상단 | PASS | "A5-1-e2e Part 1 impl — Q1 TC-6 재활성화 · Q3 계약 신설 · Q6 Mock sender 단위 테스트. Part 2 (Q4 `/password/change` 계약) 는 pending" 로 정확 표기 |
| 1-d | TC-6 실행 결과 (실제 PASS 확인) | ym-qa 리포트 `A5-1-e2e-part1-qa-20261016.md` §3 인터랙션 | **UNVERIFIED** | 저는 bootRun 재기동이 안되어 직접 재실행 불가. ym-qa 리포트에 "admin spec 29 PASS + 2 skip, 결함 0건" 명시. QA 리포트 자체는 존재 확인됨 |
| 3-e | Q3 계약 러너 실제 PASS 확인 | 동 §2-1 | **UNVERIFIED** | 동일 사유. QA 리포트에 `visual-admin-user-detail-after-issue` 2/2 PASS 명시. 계약 코드·러너 코드·셀렉터 모두 코드 대조로 PASS 판정했지만 "실 bootRun 실행 재현" 은 저가 못함 |

---

## FAIL 상세

**없음.** 머지를 차단할 결함을 반박 성공으로 입증한 항목은 0건.

---

## LOW 지적 (머지 블로커 아님, Part 2 또는 후속에서 교정 권장)

### LOW-1. Q3 fallback 계약의 `initial.password.value.not.empty` 체크 실효성 문제

**현상**: `admin-user-detail-after-issue.ts` L153~158 에서

```ts
{
  id: 'initial.password.value.not.empty',
  desc: 'password value 가 비어있지 않음',
  selector: '[data-testid="admin-user-initial-password-value"]',
  kind: 'attr-not-empty',
  attr: 'data-testid',
  ...
}
```

로 선언. `runner.ts` L52~56 은 `first.getAttribute('data-testid')` 를 확인하므로, 해당 attribute 값은 항상 `"admin-user-initial-password-value"` (hardcoded 셀렉터) 로 비어있지 않음 → 항상 `true`. 실 password text 가 비었어도 PASS 됨.

**영향도**: 낮음. 요소 존재 자체는 바로 위 `initial.password.card.exists` 체크로 커버됨. password text 가 실제로 비는 경로는 `AdminUserController` 가 `flashInitialPassword` 를 심을 때 null·빈 문자열을 전달하는 경우뿐이며, 현재 `SecureRandomPasswordGenerator` 가 12자 생성을 보장. 다만 **desc 와 동작의 괴리** 는 그대로 남는다.

**교정 제안**: `kind: 'text-match'` + `expected: '/.{6,}/'` 또는 `attr: 'value'` (만약 input 이면) 로 변경. 다만 현재 `<strong data-testid="…-value">실값</strong>` 구조이므로 `innerText` 검사가 자연. runner 에 `text-match` 지원 확인 후 변경 권장.

### LOW-2. spec L346 "작업 큐 메타" 블록의 상태 라벨 잔존

L5 는 `spec_confirmed (Part 1)` 로 승격됐으나 §11 L346 은 `spec_draft` 로 남음. L346 은 역사적 초안으로 해석되지만, 다음 Part (Q4) 명세 승격 시 동시에 교체 권장.

---

## UNVERIFIED 상세

### UNV-1. Q1 TC-6 실제 PASS

- **사유**: 적대적 검증 세션 시점에 bootRun 이 기동되어 있지 않아, Playwright `--project=chromium` 재실행이 불가.
- **대체 근거**: ym-qa 리포트 `A5-1-e2e-part1-qa-20261016.md` §3 "admin spec 29 PASS + 2 skip, 결함 0건" 명시. 리포트 파일 존재·내용 확인 완료.
- **코드 레벨 반박 시도**: outer finally 복구·process-global state 안전성·셀렉터 존재·fallback 추출 로직 전부 PASS. 반박 실패 → 코드 상 "작동할 것" 으로는 확정. 실행 재현만 UNVERIFIED.

### UNV-2. Q3 계약 러너 실제 PASS (2 variant 각 1건)

- **사유**: UNV-1 과 동일.
- **대체 근거**: ym-qa 리포트 §2-1 "`visual-admin-user-detail-after-issue` 2/2 PASS", §2-2 "`--project=contracts` 전건 49/49 PASS".
- **코드 레벨 반박 시도**: 셀렉터 실존·2 variant 분기·deviation 표기·finally 복구 모두 PASS. 실행 재현만 UNVERIFIED.

---

## 머지 권고

| 영역 | 결론 |
|---|---|
| 치명 결함 (FAIL) | 0건 |
| 잠재 결함 (LOW) | 2건 — 머지 블로커 아님. Part 2 또는 후속 소규모 교정 권장 |
| 재현 대기 (UNVERIFIED) | 2건 — ym-qa 리포트가 선행 PASS 입증. "회사 PC bootRun 비기동 시간대" 라는 환경 제약이지 코드 결함 아님 |

**결론**: Part 1 (Q1 + Q3 + Q6) 머지 가능. LOW-1 은 Part 2 명세 작성 시 함께 교정하거나, 별건으로 "contract runner — text-match 지원 확인" 티켓 분리 권장.
