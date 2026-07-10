---
name: qa
description: youth-moa-java 의 QA 검증 4영역(정적·동적·회귀·시각)을 일괄 실행하고 표준 포맷으로 분리 리포트. 커밋 전 / 사이클 종료 시 / 회귀 점검 시 호출. ym-qa 에이전트의 메인 호출자.
disable-model-invocation: true
---

사용자가 `/qa` 를 입력하면 아래 절차를 순차 실행한다. 인자 형태:

- `/qa` — 전체 검증 (모든 화면 + 전체 회귀 + Playwright E2E)
- `/qa <화면 경로>` — 해당 화면 동적 검증만 (예: `/qa /signup`)
- `/qa --static` — 정적 + 회귀만 (bootRun 안 띄운 상태)
- `/qa --no-e2e` — Playwright 스킵 (정적 + curl 만)

---

## Step 1 — 정적 검증 (필수)

```powershell
cd C:\Users\User\IdeaProjects\youth-moa-java
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.14"
.\gradlew.bat compileJava compileTestJava test 2>&1 | Select-String -Pattern "BUILD|error:|FAILED|Tests|tests completed"
```

- 컴파일 실패 → 즉시 중단·보고
- 테스트 실패 → 어느 클래스인지 분석 후 회귀 깨짐 여부 판단
- `YouthMoaApplicationTests` 실패는 **Docker daemon 동작 여부** 먼저 확인. `docker info` 로 점검.

## Step 2 — 동적 검증 (bootRun 동작 시)

### 2-1. bootRun 상태 점검

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/
```

- 000 → bootRun 죽음. 사용자에게 재기동 요청 후 Step 2 보류
- 200 → 진행

### 2-2. 화면별 응답 확인

| 경로 | 기대 |
|---|---|
| `/` | 200 |
| `/login` | 200 |
| `/signup` | 200 |
| `/programs` | 200 |
| `/programs/1` | 200 |
| `/programs/1/apply` | 302 (인증 필요) |
| `/api/users/check-email?email=test@example.com` | 200 + `{"available":true/false}` |
| `/api/ping` (POST) | 200 |

```bash
for p in "/" "/login" "/signup" "/programs" "/programs/1" "/programs/1/apply" "/api/users/check-email?email=test@example.com"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080${p}")
  echo "${code} ${p}"
done
```

### 2-3. 정적 리소스 확인

| 경로 | 기대 |
|---|---|
| `/css/main.css` | 200 |
| `/images/logo_symbol.png` | 200 |
| `/webjars/htmx.org/dist/htmx.min.js` | 200 (현재 302 이슈 — 알려진 기술 부채) |
| `/favicon.ico` | 200 |

### 2-4. 마크업 검증 (선택)

특정 화면이 인자로 지정되면 응답 HTML 에서:
- Thymeleaf 표현식 잔존 (`${...}`, `th:*`) 없음
- 의도한 핵심 마크업 (`expected class`) 존재

## Step 3 — 회귀 검증

Step 1 의 `gradlew test` 결과를 회귀로 간주.

- 직전 main 머지 시점의 TC 수 대비 동일 / 신규 추가만 OK
- 깨진 테스트 발견 시 **회귀 사고** 로 분리 표기

## Step 4 — Playwright E2E (시각·동작 자동화)

Playwright 가 brower (Chromium) 으로 실제 화면 동작을 검증. curl 로 못 잡는 form validation 메시지·실시간 JS·HTMX 토글·alert 등을 모두 자동화.

```bash
cd C:/Users/User/IdeaProjects/youth-moa-java/e2e
npx playwright test --reporter=line 2>&1 | tail -15
```

**현재 spec 커버리지** (`tests/signup.spec.ts`):
- 빈 폼 제출 시 1단계(RequiredCheck) 메시지만 노출
- 비밀번호 실시간 정책 검증 (JS buildMessage)
- 서버 측 비밀번호 정책 통합 메시지
- 다중 @AssertTrue 위반 모두 노출 (회귀 박제)
- 중복확인 안 누르고 제출

신규 화면 작업 후 `e2e/tests/<화면>.spec.ts` 추가가 표준 워크플로우의 일부.

**환경 주의**:
- 회사 PC 의 SSL 프록시 — 최초 셋업 시 `NODE_TLS_REJECT_UNAUTHORIZED=0 npx playwright install chromium` 필요
- bootRun 이 안 떠 있으면 모든 테스트 실패 → 사용자에게 재기동 요청

## Step 5 — 시각 확인 (사용자 영역, 최소화)

Playwright 가 못 잡는 영역만:
- 색·폰트·spacing 미세 디테일 (스크린샷 비교 도입 전까지)
- 반응형 break point 별 동작 (현재 desktop chrome 만)
- 디자인 직관 (prototype.tsx 와의 시각적 일치도)

→ "사용자 시각 확인 대기" 항목은 명시적으로 분리 표기. **떠넘기기 금지**.

---

## Step 6 — 표준 리포트 출력

```markdown
# /qa 검증 리포트 — <YYYY-MM-DD HH:mm> / branch <name>

## ✅ 정적 검증
- compileJava: SUCCESS / FAIL
- compileTestJava: SUCCESS / FAIL
- gradlew test: N TC PASS / N FAIL
  - 깨진 테스트: <목록 또는 "없음">
  - Docker 의존: <YouthMoaApplicationTests 등 별도 명시>

## 🔵 동적 검증 (curl)
| 경로 | 응답 | 비고 |
|---|---|---|
| / | 200 | OK |
| ... | | |

| 정적 리소스 | 응답 |
|---|---|
| /css/main.css | 200 |
| ... | |

## 🟣 E2E 검증 (Playwright)
- N spec PASS / M FAIL
- 깨진 spec: <목록 또는 "없음">

## 🔁 회귀 검증
- 회귀 깨짐: <건수>
- TC 수 변화: 이전 N → 현재 M

## 👁 시각 확인 대기 (사용자 영역, 최소)
- ① <Playwright 가 못 잡는 항목만>

## ⚠️ 발견된 문제 / 기술 부채
- <항목 + 우선순위>

## 결정 가능 여부
- 머지 가능: ✅ / ⏸ 시각 확인 후 / ❌ 회귀 깨짐 해소 후
```

---

## 주의사항

- `disable-model-invocation: true` — 사용자가 명시적으로 `/qa` 입력했을 때만 실행. 자동 호출 금지.
- ym-qa 에이전트가 호출할 때도 동일 절차. 본 Skill 의 결과를 그대로 리포트에 포함.
- 향후 P1 회귀 루프 (`/loop`) 도입 시 본 Skill 의 Step 1·3 만 자동 반복.
- bootRun 미기동 시 Step 2 건너뛰고 Step 1+3+4 로 축약 리포트.
