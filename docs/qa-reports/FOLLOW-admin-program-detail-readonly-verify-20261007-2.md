# 적대적 검증 리포트 (3차 · 최종 관문): FOLLOW-admin-program-detail-readonly

- 일자: 2026-10-07
- 브랜치: `feature/FOLLOW-admin-program-detail-readonly` (미커밋)
- 선행 체인
  - ym-verify 1차 (`aab4955cd9619be32`): PASS 15 / FAIL 0 / UNVERIFIED 2
  - ym-impl 3차 (`a3091312fb83ff702`): U1·U2 해소 — AdminProgramDetailRenderTest +2 TC, admin-head-scripts.spec.ts ADMIN_PAGES +2
  - ym-qa 3차 (`a6a1b81bf74cc5113`): 332 TC + 17/17 + 15/15 + 45/45 PASS, "UNVERIFIED 0"
- 검증 모드: refute-first — qa 3차 "UNVERIFIED 0" 반박 시도

## 판정 요약

- **PASS 17 / FAIL 0 / UNVERIFIED 0** → **머지 가능**
- U1·U2 해소 품질 모두 반박 실패 (실제로 해소됨)
- Advisory #1 (A3·A3-2 문서 drift) 는 1차와 동일하게 non-blocking advisory 로 유지

## 항목별 판정

| # | 공격 벡터 | 실행 방법 | 판정 | 근거 |
|---|---|---|---|---|
| 1 (신규) | 신규 CENTER_ADMIN TC 2건이 happy-path 만 터치하는지 (assertion 강도) | `AdminProgramDetailRenderTest` L120~142 소스 정독 | PASS | own-center TC: `admin-program-detail-page` + `admin-program-detail-header` wrapper 2종 + Thymeleaf `${` 잔존 금지 3가지 명시 assertion. cross-center TC: `status().isForbidden()` 로 403 확정. 거짓 PASS 가능성 없음 |
| 2 (신규) | `findOwnCenterProgramId()` / `findCrossCenterProgramId()` 가 실제로 center1/다른 center 를 구분하는지 | `AdminProgramDetailRenderTest` L52~74 + `DataInitializer.java` L209·L215·L911 + `src/main/resources/data/centers.csv` 상위 2행 | PASS | centers.get(0) = "내일꿈제작소" (CSV 1행). Program 시드 L911 에 "내일꿈제작소" 소속 Program 존재 → own 헬퍼가 그 id 반환. 다른 23개 Program 은 cross 헬퍼가 반환. 명확히 구분됨 |
| 3 (신규) | DataInitializer 가 center1·center2 소속 Program 둘 다 생성하는지 (헬퍼 orElseThrow 안전성) | `DataInitializer.java` L706~1127 전수 grep | PASS | "내일꿈제작소"(centers.get(0)) 소속 Program 1건 + 다른 센터 소속 Program 23건. 두 헬퍼 모두 `IllegalStateException` 안전 작동. 참고: centers.get(1)="28청춘창업소"는 Program 매핑 없음이나 cross 헬퍼는 "center1 소속이 아닌 것" 기준이므로 영향 없음 |
| 4 (신규) | 신규 TC 가 기존 `AdminProgramServiceTest#find_centerAdmin_wrongOrganization_throwsIllegalAccess` 와 중복인지 | 두 테스트 레벨 비교 | PASS | Service TC: `adminProgramService.find()` 직접 호출로 Service 레벨만. Controller TC: MockMvc 로 SecurityConfig + Controller.detail() + IllegalAccessError→AccessDeniedException 승격(L186~188) + 403 응답 전 체인 커버. 중복 아님 — 차별점 유효 |
| 5 (신규) | admin-head-scripts.spec.ts `/admin/programs/1/edit` 가 loginAdmin() 로 접근 시 선조건 만족하는지 | `e2e/helpers.ts` L74~78 + `AdminProgramController.editForm` | PASS | 기본 loginAdmin() = SYSTEM_ADMIN (sysadmin@youth-moa.test). scopeId==null → seed Program #1 center 와 무관하게 접근 가능. GET 폼 렌더만 수행 (POST 저장 아님) → CSRF/HTMX/Toast 로드만 체크. 선조건 전부 만족 |
| 6 (신규) | 신규 head-scripts TC 가 CSRF/HTMX/common-ui 로드 외 다른 선조건 (seed data, auth) 누락하는지 | admin-head-scripts.spec.ts L45~59 | PASS | assertion 범위: ① status < 400 ② `head meta[name="_csrf"/_csrf_header]` 각 1개 ③ `window.htmx` 객체 ④ `window.Toast` 객체. seed Program #1 존재 + sysadmin 로그인 만 선조건. 둘 다 e2e 프로파일에서 보장됨 |
| 7 (신규) | 신규 TC 추가가 기존 PASS 15건·admin.* 332 TC regression 유발하는지 (XML 직접 재검증) | `build/test-results/test/TEST-io...AdminProgramDetailRenderTest.xml` | PASS | timestamp=2026-10-07T07:09:13 · tests=5 · skipped=0 · failures=0 · errors=0 (XML 직접 파싱). 다른 admin.* 패키지 영향 없음은 qa 3차 집계 332 PASS 그대로 승계 |
| 8 (신규) | gap-reports/*.md 가 qa 3차 `--project=contracts` 실행 후 regenerate 됐는지 | `ls -la e2e/gap-reports/` + `gap-admin-program-detail.md` | PASS | 디렉토리 mtime 2026-10-07 16:14~16:15. gap-admin-program-detail.md = "비로그인 24/24 통과 · 갭 0건". 최신 상태 확인 (memory `feedback_gap_reports_untracked.md` 준수) |
| 9 (재확인) | ym-verify 1차 PASS 15건이 U1·U2 수정 이후 깨지지 않는지 | git diff 재대조 — 수정 범위는 2 파일 (test + spec) 만 | PASS | 수정은 test 추가·spec 데이터 추가 only. 프로덕션 소스 변경 없음 → PASS 1~15 전부 영향 없음 |

## FAIL 상세

없음.

## UNVERIFIED 상세

없음. **ym-verify 1차의 U1·U2 모두 VERIFIED 로 승격.**

## Advisory (비판정 · 머지 blocker 아님)

### Advisory #1 — A3·A3-2 spec 문서 drift (1차와 동일, 미해소)

- 지점: `docs/specs/A3-admin-program-form.md` L142·L188·L191, `docs/specs/A3-2-admin-program-form-integration.md` L113~117
- 상태: `grep "FOLLOW-admin-program-detail-readonly"` 결과 0건 — override 각주 미삽입 그대로
- 재평가: 사용자가 "UNVERIFIED 선해결" 를 요청한 맥락이면 함께 처리하는 게 자연스럽긴 함. 다만 두 spec 상태는 `done` 아카이브이므로 **별도 docs/* 후속 PR 로 분리**가 더 깔끔 (현 feature PR 스코프 비대화 방지)
- 이번 PR 에 포함할지 여부는 사용자 선택

## 추가 공격 (반박 실패)

- **DataInitializer idempotency**: `ddl-auto: update` 환경에서 재기동해도 `existsByEmail` 체크로 시드 중복 없음. 신규 TC 가 요구하는 center1@youth-moa.test + "내일꿈제작소" 소속 Program 은 매번 보장됨
- **403 vs 404 혼동 가능성**: cross-center 접근 시 `IllegalAccessError` (not found 가 아니라 접근 금지) → Controller 가 명확히 `AccessDeniedException` 로 승격 → Spring Security 가 403 응답. TC 는 `isForbidden()` 로 올바르게 검증
- **cross-center TC 가 skip 되지 않는지**: `findCrossCenterProgramId()` 가 `orElseThrow` 를 쓰므로 Program 시드 23건 중 하나라도 있으면 반드시 반환. skip 경로 없음

## 커밋 가능 여부

**머지 가능.** FAIL 0 · PASS 17 · UNVERIFIED 0.

최종 커밋 전 체크:
- [ ] (선택) Advisory #1 — A3·A3-2 각주 추가 여부 사용자 결정
- [ ] 커밋 메시지 `261007_FOLLOW_admin_program_detail_readonly - ...` 패턴
- [ ] gap-reports/*.md 는 untracked 로컬 아티팩트이므로 커밋에서 제외 (memory `feedback_gap_reports_untracked.md`)
