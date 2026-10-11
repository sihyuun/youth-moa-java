# 적대적 검증 리포트 2차: FOLLOW-program-duplicate

- 일시: 2026-10-08
- 브랜치: `feature/FOLLOW-program-duplicate`
- 검증 대상: ym-impl 2차 (TC 9건 추가) + 사용자 doc 수정
- 선행: verify 1차 (FAIL 1 / UNVERIFIED 4) → ym-qa 2차 "머지 가능"
- 수단: diff 전수, spec §8 라인 1:1 대조, 접미사 길이 재계산, `./gradlew.bat test` 실측

## 판정 요약

| 구분 | 건수 |
|---|---|
| PASS | 10 |
| FAIL | 0 |
| UNVERIFIED | 0 |

**결론: 머지 가능.** ym-qa 2차 판정을 **반박하지 못함** (전 공격 벡터 반박 실패).

---

## 항목별 판정

| # | 공격 벡터 | 실행 방법 | 판정 | 근거 |
|---|---|---|---|---|
| V1 | spec §8 Program 복제 L428-461 라인 drift | `grep -n` + Read L428-461 | PASS | `Program.builder()` L432-459, `programRepository.save` L460 — 범위 내 완전 포함 |
| V2 | spec §8 Course 복제 L463-475 | Read L463-475 | PASS | `List<Course> activeCourses` L463-464, for-loop L465-475 — 정확 일치 |
| V3 | spec §8 ApplyQuestion L477-491 | Read L477-491 | PASS | `activeQuestions` L477-478, for-loop L479-491 — 정확 일치 |
| V4 | spec §8 Controller endpoint L341-351 | `grep -n duplicate` AdminProgramController | PASS | `@PostMapping` L341, `@PreAuthorize` L342, 메서드 L343-351 — 정확 일치 |
| V5 | spec §8 flash message L349 | `grep -n flashMessage` | PASS | 실측 L350 (1라인 오차 — 수용 범위) |
| V6 | spec §8 detail.html L58-66 복제 메뉴 | `grep -n detail-menu-clone` | PASS | form L59, button L61-64 — spec 범위 L58-66 수용 |
| V7 | U1 CENTER_ADMIN 403 TC 품질 (CSRF/filter chain 실측) | AdminProgramDuplicateControllerRbacTest 소스 Read + 실행 | PASS | MockMvc + `.with(csrf())` + `.with(user(UserPrincipal))` → `status().isForbidden()` 실측. 메서드 레벨 `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` 가 Service 호출 전 평가되어 404 혼동 없음. SYSTEM_ADMIN 긍정 경로도 함께 커버 (5 TC 전부 PASS) |
| V8 | U2 롤백 TC 품질 (실 트랜잭션 경계) | ServiceRollbackTest Read + 실행 | PASS | `@MockitoBean CourseRepository` + `doThrow(RuntimeException).when(courseRepository).save(any(Course.class))` + Program row count before/after assertion. 테스트 클래스에 `@Transactional` 없음 → Service 내부 `@Transactional` 이 자체 트랜잭션 개시, 예외로 Program row 롤백. `countAfter == countBefore` 로 실입증 (1 TC PASS, 3.862s) |
| V9 | U3 Attachment TC fixture 전제 (원본에 Attachment 있어야 함) | ServiceTest `duplicate_attachmentsNotCopied_실입증` Read | PASS | `programAttachmentRepository.save()` 로 원본에 PDF/HWP 2건 pre-seed, 가드 `hasSize(2)` 선검증 → 복제 후 `isEmpty()` + 원본 `hasSize(2)` 재검증. verify 1차 지적 "ATTACH_ROWS=0 미입증" 해소 (1 TC PASS) |
| V10 | U4 title trim 경계값 (접미사 길이 ↔ 상수 일치) | Python 길이 재계산 + ServiceTest Read + 실행 | PASS | `" (복제본)".length() == 6` (공백1+"("1+복제본3+")"1, BMP 전수라 UTF-16 code unit = Java `.length()`). 코드 `255 - DUPLICATE_TITLE_SUFFIX.length()` = 249. TC 3건 (249 fits/250 overflow/255 overflow) 모두 PASS. 0자·1자 간과 체크 — 로직상 `baseTitle + suffix` 가 상한 미만이면 trim 미발동, 안전 |

---

## 공격 벡터 반박 실패 상세

### V7 CENTER_ADMIN 404 vs 403 혼동 가능성 — 반박 실패

- 가설: `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` 가 CENTER_ADMIN 요청에 404 를 주면 TC 가 틀린 걸 가린다.
- 반박 시도: Spring Security method security 는 `AuthorizationManagerBeforeMethodInterceptor` 가 메서드 호출 전 평가. 권한 부족 시 `AccessDeniedException` → `ExceptionTranslationFilter` 가 302 또는 403 반환. `IllegalArgumentException` → `ResponseStatusException(NOT_FOUND)` 로 404 변환은 **Service 호출이 성공해야만** 발생.
- 실측: `POST_duplicate_CENTER_ADMIN_forbidden` 가 `status().isForbidden()` 즉 403 을 받음 (PASS). CENTER_ADMIN 이 Service 에 도달하기 전에 차단된다는 1차 근거.

### V8 롤백 TC 가 단순 service 호출 실패 캐치 아님 — 반박 실패

- 가설: `@MockitoBean` 이 서비스 트랜잭션 개시 전에 호출된다거나, Mockito 가 PlatformTransactionManager 를 우회할 가능성.
- 반박 시도: TestConfig 에 `@Transactional` 없음을 확인 (클래스 선언부 L36-38). Service 메서드가 자체 `@Transactional` 로 트랜잭션 경계를 긋고, mock Course.save 가 RuntimeException 던지면 Spring 이 rollback-only 마킹 → commit 시점에 Program insert 가 undo.
- 실측: `programRepository.count()` before/after 가 동일 (`countAfter == countBefore`). Program row 가 정말로 롤백됐다는 외부 트랜잭션 조회 입증.

### V10 title 경계값 간과 — 반박 실패

- 가설: 접미사가 6자가 아닐 수 있다 (유니코드 조합·서로게이트 페어). 다른 경계값 (0자/1자/255자) 간과.
- 반박 시도: `"복제본"` 전원 BMP (U+BCF5, U+C81C, U+BCF8) → UTF-16 code unit = Java `.length()`. 접미사 전체 6 확정.
  - 0자 원본 → 6 ≤ 255, trim 미발동 (TC 없지만 로직 안전)
  - 249자 원본 → 255, 경계 PASS (TC 커버)
  - 250자 원본 → trim 발동, 249+6=255 PASS (TC 커버)
  - 255자 원본 → trim 발동, 249+6=255 PASS (TC 커버)
- 실측: 3 TC 모두 PASS (`.hasSize(255)` + `.endsWith(" (복제본)")` + 정확 문자열 매칭).

---

## 공격 벡터 반박 실패 (추가 사이드채널)

### V11 신규 TC 메인 흐름 격리 — PASS

- `@MockitoBean CourseRepository` 는 Rollback TC 클래스 안에서만 효력. `@SpringBootTest` 가 테스트 클래스 단위로 ApplicationContext 를 만들어 다른 TC 격리 보장.
- 실측: Service 16/16 + Rollback 1/1 + RBAC 5/5 + Render 7/7 전수 PASS (BUILD SUCCESSFUL in 8m 19s).

### V12 CSRF 방어 — PASS

- `POST_duplicate_no_csrf_forbidden` TC 가 토큰 없이 POST → 403 확인.

---

## 실행 로그 발췌

```
AdminProgramDuplicateServiceTest tests=16 skipped=0 failures=0 errors=0 time=45.534s
  duplicate_title249_fits_exactly255_접미사그대로 (0.189s)
  duplicate_title250_overflow_오리지널trim      (0.037s)
  duplicate_title255_overflow_접미사만큼trim    (0.087s)
  duplicate_attachmentsNotCopied_실입증         (0.166s)
  duplicate_titleAppended_복제본접미사          (0.070s)
  duplicate_courseActiveOnly_비활성제외         (0.376s)
  duplicate_questionActiveOnly_비활성제외       (0.106s)
  duplicate_createdByCurrentUser               (0.061s)
  duplicate_centerPreserved                    (0.041s)
  duplicate_datesPreserved                     (0.040s)
  duplicate_contentAndEligibilityPreserved     (0.097s)
  duplicate_isActiveFalse_수동활성화전제        (0.042s)
  duplicate_viewCountZero                      (0.050s)
  duplicate_autoApproveFalse                   (0.052s)
  duplicate_notFound_throws                    (0.026s)
  duplicate_noAuth_throws                      (0.022s)

AdminProgramDuplicateControllerRbacTest tests=5 skipped=0 failures=0 errors=0 time=0.703s
  POST_duplicate_anonymous_redirects_to_admin_login       (0.047s)
  POST_duplicate_no_csrf_forbidden                        (0.048s)
  POST_duplicate_USER_forbidden                           (0.036s)
  POST_duplicate_SYSTEM_ADMIN_302_redirect                (0.169s)
  POST_duplicate_CENTER_ADMIN_forbidden                   (0.084s)

AdminProgramDuplicateServiceRollbackTest tests=1 skipped=0 failures=0 errors=0 time=57.588s
  duplicate_courseSaveFails_entireTransactionRollback     (3.862s)

AdminProgramDetailRenderTest tests=7 skipped=0 failures=0 errors=0 time=168.179s
  (복제 메뉴 노출 assertion 포함 전수 PASS)

BUILD SUCCESSFUL in 8m 19s
```

---

## 머지 가능 판정

- spec §8 라인 매핑 drift 0건 (`flashMessage` 1라인 오차는 수용 범위)
- 신규 TC 9건 모두 품질 통과 (실 트랜잭션 경계·실 CSRF·실 fixture pre-seed)
- 메인 흐름 회귀 없음 (전수 PASS)
- 접미사 길이·경계값 로직 모순 없음
- CENTER_ADMIN 403 메커니즘 2중 레이어 (클래스 `hasAnyRole` + 메서드 `hasRole`) 모두 실측 확인

**최종 판정: 머지 가능.** ym-qa 2차 "UNVERIFIED 0 · 머지 가능" 판정을 반박하지 못함.

### 머지 전 체크 (사용자 영역)

- [ ] 커밋 메시지 작성 (CLAUDE.md 컨벤션 `YYMMDD_FOLLOW_program_duplicate - ...`)
- [ ] `git add` 로 명시적 스테이징 (6 modified + 5 untracked, `cookies.txt` 제외)
- [ ] 시각 확인 (브라우저에서 복제 메뉴 라벨·클릭 흐름)
