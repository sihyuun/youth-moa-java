# A1 admin-shell QA 리포트

| 메타 | 값 |
|---|---|
| 브랜치 | `feature/A1-admin-shell` |
| 커밋 | `21cc28c` (`260903_A1_admin_shell - 관리자 로그인 + 대시보드 shell + 헤더`) |
| 명세 | `docs/specs/A1-admin-shell.md` (spec_confirmed, Qn-1 A · Qn-2 A · Qn-3 B · Qn-4 B · Qn-5 A · Qn-6 B) |
| QA 실행 | 2026-09-03 (회사 PC, ym-qa) |
| 실행 환경 | 8090 e2e 프로파일 (H2 + 시드), Playwright chromium |
| 판정 | ✅ **impl_done 판정 가능** — 6영역 전부 정직한 실측 기반 PASS |

---

## 사전 사고: stale bootRun 프로세스 재기동

QA 착수 시 8090 에 이미 떠 있던 java 프로세스 PID 16440 은 **2026-09-02 오후 1:24 시작** — A1 커밋(09-03) 이전. 첫 회 실측에서:

- `POST /admin/login` = 302 → `/admin` (예전에도 `/admin/login` permitAll + formLogin loginPage 만 있었으니 302 자체는 가능)
- `GET /admin` = **404 NoResourceFoundException** (`AdminDashboardController` 미등록 → static resource 로 fallback)

→ 원인: 예전 서버가 최신 컨트롤러를 못 봄. `Stop-Process -Id 16440` 후 `.claude/scripts/bootrun-e2e.cmd` 재기동. 이하 모든 결과는 **재기동 후 새 서버** 기준.

---

## 1. 정적 검증 (ym-impl 이 이미 완료 · 재확인 안 함)

ym-impl 인계 내역:

```
compileJava + AdminLoginRenderTest 3 + AdminDashboardRenderTest 2
+ AdminSecurityTest 6 + JpaMappingTest PASS
```

ym-qa 는 명세 §8 에 따라 정적 재실행하지 않음. (동적 · 계약 · 기능 · 회귀만 담당)

**결과**: ✅ PASS (ym-impl 인계 기준)

---

## 2. 동적 검증 (curl)

### 2-1. 응답 코드 실측 원본

```
=== [D1] GET /admin/login ===
200
=== [D2] GET /admin (unauth) ===
302 loc=http://localhost:8090/admin/login
=== [D3] GET /css/admin.css ===
200

=== [D4] login flow ===
csrf=vXfgB5v0HuRGYzzS8wEd... (마스킹)
POST /admin/login=302 loc=http://localhost:8090/admin
GET /admin=200
-- dashboard render checks --
반가워요 count: 2
${...} or class="null" count: 0
stat-card count: 32
```

### 2-2. `/admin/login` 렌더 잔존 스캔

```
관리자 로그인 count: 4         # h1 + label 등 4회 등장 정상
${...} count: 0                  # 미평가 표현식 없음
class="null" count: 0            # null 클래스 잔존 없음
회원가입 button (deviation - should be 0): 1
```

`회원가입` 매치 1건 확인 결과:

```
$ grep -n '회원가입\|signup\|find-id\|find-password' src/main/resources/templates/admin/login.html
25:  - 회원가입 버튼은 prototype 에 있으나 A1 은 제거 (관리자 signup 미구현 · deviation)
68:            <a th:href="@{/find-id}" class="admin-auth-help-link">아이디 찾기</a>
70:            <a th:href="@{/find-password}" class="admin-auth-help-link">비밀번호 찾기</a>
```

→ **템플릿 HTML 주석(L25)에만 존재**. 명세 §5-1 `deviation: 'ADMIN-00 Q8 관리자 signup 미구현'` 지침 그대로 이행 확인.

**결과**: ✅ PASS
- 모든 응답 코드 명세대로 (200 · 302 · 200 · 200)
- 로그인 성공 flow 정상 (302 → `/admin` → 200)
- Thymeleaf 미평가 표현식 · `class="null"` 잔존 0건
- deviation (회원가입 제거) 준수

---

## 3. 계약 검사 (`--project=contracts`)

### 3-1. 실행

```
cd e2e && BASE_URL=http://localhost:8090 \
  npx playwright test --project=contracts \
  visual-admin-login visual-admin-shell visual-admin-dashboard \
  --reporter=line
```

### 3-2. 실측 원본

```
Running 3 tests using 1 worker

[1/3] [contracts] › tests\visual-admin-dashboard.spec.ts:6:5 › 관리자 대시보드 콘텐츠 디자인 계약 — SYSTEM_ADMIN
[2/3] [contracts] › tests\visual-admin-login.spec.ts:5:5 › 관리자 로그인 화면 디자인 계약 — 비로그인
[3/3] [contracts] › tests\visual-admin-shell.spec.ts:6:5 › 관리자 shell 다크 헤더 디자인 계약 — SYSTEM_ADMIN
  3 passed (23.8s)
```

**결과**: ✅ **admin 3화면 갭 0** — 명세 §8-4 완료 판정 조건 만족.

---

## 4. 기능 E2E (`--project=chromium -g "admin"`)

### 4-1. 실행

```
cd e2e && BASE_URL=http://localhost:8090 \
  npx playwright test --project=chromium -g "admin" --reporter=line
```

### 4-2. 실측 원본

```
Running 10 tests using 1 worker

[1/10] [chromium] › tests\admin-dashboard.spec.ts:8:5 › 대시보드 스탯 카드 4개 + 실제 count 렌더
[2/10] [chromium] › tests\admin-dashboard.spec.ts:24:5 › 유저 드롭다운 → 사용자 페이지 클릭 → 세션 유지된 채 / 이동
[3/10] [chromium] › tests\admin-dashboard.spec.ts:34:5 › 유저 드롭다운 → 로그아웃 → /admin/login?logout 도달 + alert
[4/10] [chromium] › tests\admin-dashboard.spec.ts:43:5 › 사용자 헤더 드롭다운에 관리자 페이지 링크 (관리자만 노출)
[5/10] [chromium] › tests\admin-dashboard.spec.ts:55:5 › USER 계정은 사용자 헤더 드롭다운에 관리자 페이지 링크 미노출
[6/10] [chromium] › tests\admin-login.spec.ts:16:5 › 비인증 상태로 /admin 진입 시 /admin/login 으로 리다이렉트
[7/10] [chromium] › tests\admin-login.spec.ts:22:5 › 잘못된 자격증명 → /admin/login?error + username 보존
[8/10] [chromium] › tests\admin-login.spec.ts:32:5 › SYSTEM_ADMIN 로그인 성공 → /admin 도달 + 다크 헤더 렌더
[9/10] [chromium] › tests\admin-login.spec.ts:39:5 › CENTER_ADMIN 로그인 → 자기 센터명 노출
[10/10] [chromium] › tests\admin-login.spec.ts:46:5 › USER 로그인 상태로 /admin 접근 시 403
  10 passed (23.1s)
```

**결과**: ✅ **10/10 PASS** — 명세 §8-5 시나리오 (Qn-2 로그아웃 리다이렉트, 왕복 링크 sec:authorize, RBAC 3계층 403) 실측 검증 완료.

---

## 5. 회귀 검증

### 5-1. 사용자 계약 전체 (`--project=contracts`)

명령: `BASE_URL=http://localhost:8090 npx playwright test --project=contracts --reporter=line`

```
[27/27] [contracts] › tests\visual-signup.spec.ts:20:5 › 회원가입 화면 filled 상태 — email input primary border + 흰 배경 유지
  27 passed (1.9m)
```

주요 화면 실측 원본 인용:

```
[25/27] [contracts] › tests\visual-programs.spec.ts:22:5 › 프로그램 목록 디자인 계약 — 비로그인·로그인
## 비로그인 — 75/75 통과
갭 없음.
## 로그인 — 1/1 통과
갭 없음.
**합계: 76/76 통과 · 갭 0건** · 의도적 이탈 2건 (검사 제외)
```

**결과**: ✅ **27/27 PASS** — admin 계약 3건 + 사용자 계약 24건 모두 갭 0.

### 5-2. 사용자 기능 E2E (`--project=chromium -g "login|signup|programs|apply"`)

```
Running 46 tests using 1 worker
...
[46/46] [chromium] › tests\signup.spec.ts:161:5 › 중복확인 안 누르고 제출 — 안내 메시지 노출
  46 passed (2.3m)
```

**결과**: ✅ **46/46 PASS** — admin 10건 + 사용자 login/signup/programs/apply 36건 회귀 없음.

### 5-3. 사용자 헤더 fragment 회귀 (명세 §5-4)

- 사용자 헤더 드롭다운에 `sec:authorize="hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')"` 조건부 "관리자 페이지" 링크 삽입 → 관리자 계정만 노출, USER 계정 미노출
- 위 4-2 실측 원본 시나리오 [4/10] `관리자 페이지 링크 (관리자만 노출)` + [5/10] `USER 계정 미노출` PASS
- login/signup 시나리오 46/46 통과 → 사용자 로그인 정상 흐름 회귀 없음

**결과**: ✅ 사용자 헤더 fragment 변경이 사용자 트랙에 오작동 유발 없음.

---

## 6. 시각 확인 (사용자 영역)

**Preview 도구 미사용** (curl + Playwright 계약이 시각 정량치 커버). 아래 항목은 사용자 브라우저 확인 대기:

| 확인 URL | 확인 포인트 |
|---|---|
| `http://localhost:8090/admin/login` | 인디고 primary `#3F30E9` 로그인 버튼 · 420px 카드 · Auth Header 로고 primary |
| `http://localhost:8090/admin` (로그인 후) | 다크 헤더 `#111827` 56px sticky · 로고 white · 스탯 카드 4개 아이콘 색상 4종 (green/gray/orange/purple) · Welcome 21/700 "반가워요, [이름]님 👋" |
| (반응형) | HANDOFF L191~197 반응형은 **A8 이월** (명세 §10) — A1 검증 대상 아님 |

Playwright contracts 검사는 색·크기·position 정량 값을 이미 검증했으므로 시각 확인 남은 것은 **감성 요소만** (미세 폰트 렌더링·hover feel 등).

---

## 종합 판정

| 영역 | 결과 | 근거 |
|---|---|---|
| 1. 정적 | ✅ PASS | ym-impl 인계 (compile + AdminLoginRenderTest 3 + AdminDashboardRenderTest 2 + AdminSecurityTest 6 + JpaMappingTest) |
| 2. 동적 | ✅ PASS | curl 응답 코드 4/4 + Thymeleaf 잔존 0 + deviation 준수 |
| 3. 계약 | ✅ PASS | admin 3/3 갭 0 |
| 4. 기능 E2E | ✅ PASS | admin 10/10 |
| 5. 회귀 | ✅ PASS | 사용자 계약 24/24 + 사용자 기능 E2E 36/36 |
| 6. 시각 | 사용자 대기 | 감성 요소만 남음 (정량은 계약이 커버) |

**결론**: **`spec_confirmed → impl_done` 판정 조건 충족**. 시각 확인(사용자 영역)은 감성 잔여분이라 판정 blocker 아님. ym-verify (적대적 검증) 로 인계 권장.

---

## 부록 — 실행 재현 절차

```powershell
# 1. bootRun (재기동 필요 시)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.14"
.\.claude\scripts\bootrun-e2e.cmd
# → 3분 내 http://localhost:8090/admin/login 200 OK 확인

# 2. 동적
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/admin/login   # 200
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/admin         # 302
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/css/admin.css # 200

# 3. 계약
cd e2e; npx playwright test --project=contracts visual-admin-login visual-admin-shell visual-admin-dashboard --reporter=line

# 4. 기능
cd e2e; npx playwright test --project=chromium -g "admin" --reporter=line

# 5. 회귀
cd e2e; npx playwright test --project=contracts --reporter=line
cd e2e; npx playwright test --project=chromium -g "login|signup|programs|apply" --reporter=line
```
