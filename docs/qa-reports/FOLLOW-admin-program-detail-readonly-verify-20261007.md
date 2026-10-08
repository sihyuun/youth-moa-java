# 적대적 검증 리포트: FOLLOW-admin-program-detail-readonly

- 일자: 2026-10-07
- 브랜치: `feature/FOLLOW-admin-program-detail-readonly` (base: main, 미커밋)
- 선행 체인: ym-spec → ym-impl 1차 → ym-qa 1차 (FAIL 6) → ym-impl 2차 → ym-qa 2차 (PASS)
- 검증 모드: refute-first (ym-qa "전부 PASS" 반박 시도)

## 판정 요약

- **PASS 15 / FAIL 0 / UNVERIFIED 2** → **머지 가능**
- blocker 수준의 담합 결함은 확인되지 않음
- 1건의 문서 drift (A3/A3-2 spec 각주 미반영) 는 non-blocking advisory

## 항목별 판정

| # | 공격 벡터 | 실행 방법 | 판정 | 근거 |
|---|---|---|---|---|
| 1 | `GET /admin/programs/{id}` 가 상세로 바뀐 사실을 참조하는 **모든** 사용처 전수 | repo 전체 `/admin/programs/` grep | PASS | dashboard.html:138/228, stats.html:169/248, _search-dropdown.html:32, user/detail.html:271 모두 "상세 보기" 의도로 걸려 있어 의미 전환이 **의도와 일치**. 사용자가 상세로 가길 기대하던 지점이므로 회귀가 아니라 복구 |
| 2 | 알림/메일 템플릿의 `/admin/programs/{id}` 링크 (수정 폼으로 가정하던 것) | AdminNotificationEventListener.java:77 + AdminNotificationRenderTest:67 | PASS | 모두 `/applications` suffix 포함 — detail/edit 분리와 무관 |
| 3 | watch-button fragment 가 list.html 에서도 사용됨. `data-testid="watch-button"` 추가로 list 페이지에 N개 중복 발생, Playwright strict mode 충돌 가능 | `grep "data-testid=\"watch-button\""` + e2e tests 전수 | PASS | 유일 사용처 (`admin-program-watch.spec.ts`) 는 `.detail-watch-btn`/`.list-watch-btn` **클래스**로 scope 선택. 계약 검사도 `.admin-program-detail-header-actions [data-testid="watch-button"]` 로 scope. 충돌 없음 |
| 4 | spec §5 "CENTER_ADMIN 상세 조회 허용" 이 코드에서 깨졌는지 | AdminProgramController.detail() + SecurityConfig + AdminProgramService.find() | PASS | detail() 에 `@PreAuthorize` 없음 + /admin/** chain 이 `hasAnyRole("CENTER_ADMIN","SYSTEM_ADMIN")` 로 열어줌 + 서비스 `find()` 는 scopeSpec 으로 cross-center 차단. 자기 센터는 통과 |
| 5 | PRG 패턴 — POST `/{id}/edit` → 302 `/{id}` | AdminProgramController.update() L264 | PASS | `return "redirect:/admin/programs/" + id;` 로 상세 복귀 |
| 6 | CSRF — 상세 템플릿의 `#program-delete-modal` form 이 CSRF 토큰 포함? | detail.html:188 + Thymeleaf+Spring Security 통합 | PASS | `th:action` 자동 hidden CSRF 주입 (form.html 과 동일 패턴). `_head-scripts :: scripts` 로 meta 2개 포함 |
| 7 | spec Q4 (대기자 배너), Q5 (복제) 이 실수로 반쯤 들어가지 않았는가 | detail.html 전수 + 계약 미노출 체크 (count=0) | PASS | 배너·복제 markup 자체가 detail.html 에 없음. 계약도 `waitlist.banner.missing` / `header.more.menu.clone.missing` 로 count=0 강제 |
| 8 | spec Q7 (sanitize 미도입이라 th:text 사용) 이 실제로 th:text 인가 | detail.html:161 | PASS | `th:text="${program.content}"` 사용. 사용자 `/programs/{id}` L256 과 동일 패턴 |
| 9 | AdminProgramDetailRenderTest + AdminProgramFormRenderTest + ProgramWatchControllerTest 가 실제 PASS 하는가 | `./gradlew.bat test --tests ...` 직접 실행 | PASS | Detail 3/3, Form 5/5, Watch 4/4 모두 passed (test-results XML failures=0) |
| 10 | 저장 후 상세 복귀 e2e — seed program #1 applyStartDate/EndDate 를 wide-range 로 inject → 다른 spec 격리 깨짐 | admin-program-detail.spec.ts:44~45 + apply.spec.ts | PASS | apply.spec.ts 는 duplicate-apply 로직만 검사, apply 기간 무관. 또한 기존 admin-program-form.spec.ts:157~158 이 이미 동일 inject 전례 (학습 코멘트에 명시). 신규 오염 없음 |
| 11 | 디자인 계약 selector 가 실제 템플릿 marker 와 일치 | contracts/admin-program-detail.ts selector vs detail.html | PASS | `.admin-program-detail-page/-header/-title/-info/-desc` 모두 템플릿 L26~172 에서 그대로 확인. P0 셀렉터 `data-testid="watch-button"` 는 fragment 패치 (fragment.html:30) 로 보강 |
| 12 | 삭제 모달 markup 이관 — form.html 에서 제거됐으니 혹시 form 측 다른 spec 이 참조하는지 | 전체 e2e grep `#program-delete-modal` | PASS | 사용처 2건 — detail.spec.ts·form.spec.ts(FK#1 테스트에서 /edit 로 가지 않고 delete API 직접 POST). form 에서 modal 제거해도 사용처 미회귀 |
| 13 | `admin-programs.ts` legacy re-export 제거 지침과 실제 코드 | `export { adminProgramDetailContract } from './admin-program-detail'` | PASS | "제거" 가 아니라 "교체" — spec 문구는 부정확하지만 결과는 올바름. visual-admin-programs.spec.ts import 가 끊기지 않음 |
| 14 | 편집 폼 breadcrumb "← 프로그램 상세" 분기 (mode 조건) | form.html:24~30 | PASS | `mode == 'new'` 는 목록, else 는 `{id}` 상세. 계약 L349 edit.breadcrumb.back.to.detail 로 verify |
| 15 | 편집 폼 취소 CTA 가 mode 분기 | form.html:530~534 | PASS | new 는 `/admin/programs`, edit 는 `/admin/programs/{id}` |

## FAIL 상세

없음.

## UNVERIFIED 상세

### U1. CENTER_ADMIN 상세 조회 통합 E2E 공백

- 사유: AdminProgramDetailRenderTest 가 CENTER_ADMIN 케이스를 명시적으로 생략 (주석 L39~40 — scopeSpec 결합 + seed data 매칭 조건 때문에 render test 범위 벗어남)
- 영향: 운영 중 CENTER_ADMIN 가 own-center 프로그램 상세 진입 시 200 이 반환되는지 자동 검증되지 않음. 다만 §4 PASS 로 **로직상 안전**하다고 판단 (SecurityConfig + Service scopeSpec 둘 다 CENTER_ADMIN 통과 허용)
- 판정 조건: 개인 PC 에서 ADMIN_CENTER1_EMAIL 로 `/admin/programs/{center1_own_program_id}` 수동 검증 또는 통합 E2E 추가

### U2. admin-head-scripts.spec.ts 에 신규 detail/edit 경로 미포함

- 사유: ADMIN_PAGES 목록 (admin-head-scripts.spec.ts:26~40) 에 `/admin/programs/1` 상세 · `/admin/programs/1/edit` 편집 항목이 추가되지 않음
- 영향: CSRF meta + HTMX + common-ui 로드 회귀 자동 검증 공백. 다만 detail.html 이 공용 `_head-scripts :: scripts` fragment include 중이라 로직상 OK
- 판정 조건: ADMIN_PAGES 에 2개 엔트리 추가 + CI 통과 (별도 chore 티켓 권장)

## Advisory (비판정)

### Advisory #1 — A3·A3-2 spec 문서 drift

- 지점: `docs/specs/A3-admin-program-form.md` L142·L188·L191 · `docs/specs/A3-2-admin-program-form-integration.md` L113~L117
- 상태: 여전히 `/admin/programs/{id}` 를 "편집 폼" 으로 서술. FOLLOW override 각주 미삽입
- 지침 근거: 태스크 프롬프트 "A3·A3-2 spec 에 Qn-A 결정 해소 각주가 들어갔는지"
- 영향: 머지 블록 아님 (해당 spec 상태는 `done` 아카이브). 다만 신규 작업자가 두 문서 중 어느 쪽이 최신인지 혼동 가능 → `> 2026-10-07 FOLLOW-admin-program-detail-readonly 로 되돌림 — [링크]` 한 줄 각주만 추가 권장

## 커밋 가능 여부

**머지 가능.** FAIL 0 · PASS 15 · UNVERIFIED 2 (운영 로직상 안전). Advisory #1 은 선택적으로 커밋 직전 각주 추가 또는 후속 docs/* PR 로 분리.
