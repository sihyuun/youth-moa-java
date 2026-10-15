# 적대적 검증 리포트: FOLLOW-admin-calendar-panel-card-prototype-match

- verify agent: ym-verify (수동 호출, 2026-10-15)
- 대상 브랜치: `feature/FOLLOW-admin-calendar-panel-card-prototype-match`
- 대상 커밋: `b27570a` (spec_confirmed) + `69dc2cd` (impl)
- 공격 벡터: Q1 ~ Q14 (14건)

## 판정 요약

- **PASS 14 / FAIL 0 / UNVERIFIED 0 → 커밋·머지 가능**
- ym-impl / ym-qa 보고를 1차 재대조한 결과, 반박 지점을 찾지 못했음
- 유일한 유의 사항은 Q4 (chip/bar 범위 재판정) — 지시된 공격 벡터는 "전수 삭제"였으나, **확정 명세 §9 자체가 "삭제하지 않음"으로 명시**하고 있어 공격 벡터 쪽이 틀렸음. 공격 벡터를 명세에 맞게 재조정 후 PASS

## 항목별 판정

| # | 공격 벡터 | 실행 방법 (명령/파일) | 판정 | 근거 |
|---|---|---|---|---|
| Q1 | diff 범위 vs 명세 §3 | `git diff main..HEAD --stat` | PASS | 8 파일 변경, 모두 §3 지정 범위 (admin fragment / 공용 DTO / admin.css 블록 / admin contracts / admin spec / 디자인계약 doc / RenderTest 신설 / 명세 자체). user 쪽 손상 없음 |
| Q2 | prototype L1125~1157 매칭 | `docs/00_assets/admin/prototype.html` L1125~1157 vs `_calendar-fragment.html` + `main.css` 블록 대조 | PASS | 썸네일 100%×100px / 32px 텍스트, h3 title anchor + underline, 11px center, 1px divider, dl 3-row (11px 라벨, 11px #374151 값, applied 12px primary bold), full-width primary 버튼 + chevron — 전수 일치. CSS primary 는 `var(--color-primary)=#3F30E9` 정확 일치, 썸네일 bg 는 `var(--color-primary-bg)` oklch(0.96 0.0255 280) ≈ `#F1EFFC` (디자인 토큰 사용 — Q11 정책 준수) |
| Q3 | Q1~Q6 결정 반영 | 코드 1:1 | PASS | Q1(b) 썸네일 텍스트 전용(이미지 로직·`card.imageUrl` 참조 제거 완료), Q2(a) `<article>` + 제목 anchor + 버튼 anchor (중첩 anchor 없음, HTML spec 준수), Q3(a) 공용 `ProgramCardDto` 에 `getApplyPeriod()`/`getThumbText()` 추가, Q4(a) `formatRange(start,end)` static 유틸 추출 + `dateLabel` 재사용, Q5(a+b) applied primary bold + capacity null/0 → "제한 없음", Q6 네이밍 유지 |
| Q4 | chip/bar/capacity-bar negative check (admin) | `Grep "program-calendar-panel-card-bar\|chip\|calendarChip\|capacity-bar"` under `src/main/resources/templates/admin/` | PASS | admin 하위 매치 0건. 공격 벡터 원문은 "DTO·fragment·CSS 전수 삭제"였으나 명세 §9 ("CSS 삭제 범위 재판정: 삭제하지 않음 — user 쪽 참조") 와 Q3(a) ("공용 DTO 에 getter 추가, 비파괴") 가 명시적으로 반대 결정. user 쪽 `program/_calendar-fragment.html` 이 chip 을 여전히 사용 중이므로 공용 DTO 와 CSS 유지는 올바름. admin 네임스페이스 격리만 요구됨 — 충족 |
| Q5 | user regression 없음 | `./gradlew test --tests "*CapacityBarFragmentRenderTest*" "*ProgramCardDto*" "*ProgramListRenderTest*"` + user 캘린더 spec | PASS | CapacityBarFragmentRenderTest 13/13, ProgramCardDtoTest 17/17, ProgramListRenderTest 14/14, user `programs-calendar.spec.ts` 24/24 전수 PASS. D5-Q3 capacity-bar 호출 4곳 손상 없음 |
| Q6 | DTO `formatRange` correctness | `git diff -- "**/ProgramCardDto.java"` | PASS | `formatRange(LocalDate,LocalDate)` static, null-safe (start/end 둘 중 하나 null → null), 같은 해 → `yyyy-MM-dd~MM-dd`, 다른 해 → `yyyy-MM-dd~yyyy-MM-dd`. `dateLabel` 생성 로직이 이 유틸 호출로 리팩터됨 (중복 포매터 제거). `getApplyPeriod()` 가 동일 유틸 재사용 → 두 getter 포맷 완전 일치 보장. `applyStartDate/applyEndDate` null 가능성에 안전 |
| Q7 | "제한 없음" fallback 정확성 | Thymeleaf expression 직접 확인 | PASS | fragment 삼항: `(card.capacity == null or card.capacity == 0) ? '제한 없음' : card.applicantCount + '/' + card.capacity + '명'`. `thumbText` null/empty 처리는 DTO 레벨 (`""` 반환). `applicantCount` 는 공용 DTO 가 Long 보장 (null 가능성 적음) |
| Q8 | 계약 전수 (10 신설 + 2 negative) | `cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts admin-programs-calendar` | PASS | 21/21 통과, 갭 0건 (`e2e/gap-reports/gap-admin-programs-calendar.md` 재생성 확인). 신설 10건 (wrapper/thumb_text/title.a/center/divider/info.rows.count/applied/detail_btn + capacity_bar.absent/chip.absent) 모두 `proto: 'admin/prototype.html Lxxxx'` 라인 인용 포함 |
| Q9 | 인터랙션 신규 assertion | `BASE_URL=http://localhost:8090 npx playwright test --project=chromium admin-program-calendar.spec.ts` | PASS | 6/6 PASS. 신설 테스트 "FOLLOW: 패널 \"상세보기\" 버튼 클릭 → /admin/programs/{id} 이동" (line 85) — 셀 클릭 → 패널 slide-in → `.admin-program-calendar-panel-card-detail-btn` href `/admin/programs/\d+` 매칭 → 클릭 후 URL 전환 검증 완료 |
| Q10 | 디자인 계약 md 갱신 | `docs/design-contracts/admin/programs-list-calendar.md` | PASS | "FOLLOW 2026-10-14 — D5-Q3 와 결별" 섹션 신설, prototype L1125~1157 매칭 사실 명시. 기존 D-admin-calendar 섹션은 **유지** (추가 섹션 방식 — 버전 흔적 보존). source 라인에도 FOLLOW 티켓명 appended |
| Q11 | CSS 삭제 범위 재판정 | `Grep "program-calendar-panel-card-bar" src/main/resources/templates/` | PASS | user `program/_calendar-fragment.html` L168~170 에서 `calendarChipLabel/Kind` + `program-calendar-panel-card-chip*` 참조 중. 명세 §9 결정대로 공유 CSS 유지. admin 은 `admin-` 네임스페이스로 분리 → 충돌 없음 |
| Q12 | 커밋 메시지 컨벤션 | `git log --format="%s%n%n%b" b27570a..HEAD` | PASS | `261014_FOLLOW_admin_calendar_panel_card - admin prototype L1125~1157 매칭` 형식 준수. 본문: bullet 요약 + "정적 검증: ... / 동적 검증: ..." 분리 섹션 포함 |
| Q13 | QA 리포트 §5 전수 테스트 재확인 | 로컬 `./gradlew test --tests AdminProgramCalendarRenderTest` 재실행 | PASS | `BUILD SUCCESSFUL`, `TEST-io.github.sihyuuun.youthmoa.admin.program.AdminProgramCalendarRenderTest.xml` 확인: `admin_캘린더_패널_카드_prototype_매칭_마크업_렌더()` 5.915s PASS, `capacity_null_인_프로그램은_제한없음_fallback()` 1.171s PASS. Testcontainers 테스트는 별도 스코프 (본 PR 범위 밖), ym-qa Monitor `blr6p9g77` 결과 신뢰 가능 |
| Q14 | 시각 샘플 멀티모달 확인 | Read `docs/qa-reports/screenshots-FOLLOW-admin-calendar/관리자-캘린더-패널-카드-prototype-매칭.png` | PASS | 썸네일 lavender bg + 32px 보라 "취" 텍스트, h3 "취업역량 강화 워크숍" + "내일스퀘어 양평" 센터, divider, "진행 기간 2026-10-05~10-18 / 신청 기간 2026-10-01~10-17 / 신청 현황 28/30명" 3-row (applied 보라 bold), full-width primary "상세보기 >" 버튼 전수 시각 확인. **chip·capacity bar 미노출** — negative check 시각적 재검증 완료 |

## FAIL 상세

없음.

## UNVERIFIED 상세

없음.

## 종합

- 명세 §3 변경 범위를 벗어난 파일 변경 **없음**
- 공용 `ProgramCardDto` 확장 (Q3=a) 은 신규 getter 추가 only — 기존 호출자 영향 없음을 user-side 테스트 65건 (13+17+14 유닛 + 24 e2e) PASS 로 입증
- 계약 신설 10 + negative 2 는 모두 `proto` 라인 인용 포함, 갭 0
- 인터랙션 "상세보기 클릭 → 상세 이동" assertion 신설 완료 (F0f 캘린더뷰 머지 이래 계속 공백이던 자리)
- CSS 삭제 범위 재판정 (§9) 이 사전 명시되어 있어 user 손상 리스크 사전 차단됨
- 시각 샘플 L1125~1157 전수 일치

**결론: 커밋·머지 가능. 반박 실패 (= 통과).**
