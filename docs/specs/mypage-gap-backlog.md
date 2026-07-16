---
id: mypage-gap-backlog
status: backlog
created: 2026-07-16
source: ym-spec 마이페이지 전면 prototype 갭 분석 (F-signup-03 완료 시점)
---

# 마이페이지 갭 backlog (T1~T12)

F-signup-03 (WelcomeScreen + 프로필 요약 뱃지 그룹형) 완료 후 발견된 마이페이지 잔여 갭 정리. prototype (tsx L1230~1500 · HANDOFF §5.8~5.12 · §5-E.6) 대비 총 31건 (🔴 14 · 🟡 12 · 🟢 5).

## 심각도별 우선순위 · 규모

| # | 티켓 후보 | 심각도 | 규모 | 우선순위 근거 |
|---|---|---|---|---|
| **T9** | `feature/mypage-profile-edit-interests` — profile-edit 관심 지역·분야 편집 UI | 🔴 | 중 | CLAUDE.md "데이터 소비 지점 규칙" 명시 위반. WelcomeScreen 저장 데이터의 편집 경로 부재 |
| **T1** | `feature/mypage-tabs-svg-icons` — 탭바 4종 SVG 아이콘 이식 | 🔴 | 소 | CLAUDE.md "prototype SVG 이모지·텍스트 대체 금지" 위반. 저비용 fix |
| **T12** | `chore/mypage-labels-align` — KPI·탭·history 서브·favorites 헤더 문구 4건 | 🟢 | 소 | 문구 미세 조정. 다른 티켓과 병합 가능 |
| **T10** | `feature/mypage-kpi-deeplink` — KPI 클릭 시 `?tab=` 라우팅 | 🟡 | 소 | UX 개선 |
| **T2** | `feature/mypage-history-filter` — 기간(4버튼) + 상태(칩 5개) | 🔴 | 중 | Controller/Repo 확장 |
| **T3** | `feature/mypage-history-card-redesign` — 이미지·상세링크·풀폭 danger CTA·재신청+취소 disabled | 🔴 | 대 | history 카드 대공사 |
| **T4** | `feature/application-detail-page` — `/mypage/applications/{id}` 신설 (tsx L1449~1542) | 🔴 | 중 | T3 라우팅 대상 |
| **T5** | `feature/program-status-inactive` — Program.status INACTIVE + 운영중단 뱃지 | 🔴 | 소 | F0f-fix 통합 가능 |
| **T6** | `feature/mypage-favorites-grid` — 4열 ProgramCard grid + 카드 필드 확장 | 🔴 | 중 | Q3 결정 필요 (compact variant 여부) |
| **T7** | `feature/mypage-notifications-redesign` — 2섹션 (채널·항목) + 아이콘 뱃지 + Toggle | 🔴 | 대 | Q4 결정 필요 (User 필드 or 별도 엔티티) |
| **T8** | `feature/user-login-id-field` — loginId 필드 신설 or email 유지 | 🟡 | 결정 | Q1 결정 필요 |
| **T11** | (U-COMMON-01 통합) — `.mypage-modal` → 공통 `.modal-card` 마이그레이션 | 🟡 | — | U-COMMON-01 머지 후 자연 해소 |

---

## 사용자 결정 대기 (착수 시점에 결정)

- **Q1 (T8)**: Step1 재확인 필드 = (a) email 유지 / (b) loginId 신설 — 현재 User 엔티티엔 loginId 부재
- **Q2 (T9)**: 관심 지역·분야 편집 = (a) WelcomeScreen 재사용 (재진입 링크) / (b) profile-edit 인라인 폼
- **Q3 (T6)**: 즐겨찾기 카드 = (a) 목록과 동일 카드 재사용 / (b) `ProgramCard.compact` variant 신설
- **Q4 (T7)**: 알림 3항목 저장 = (a) User 필드 3개 (`notifyRemindDayBefore` · `notifyEmptySeat` · `notifyNewProgram`) / (b) `UserNotificationPref` 별도 엔티티

---

## CLAUDE.md 규칙 위반 (우선 정리 대상)

1. **T1 (탭바 텍스트 대체)** — SVG 이모지·텍스트 대체 금지 규칙 위반 (F0h-c4 사고 규칙)
2. **T9 (관심 편집 UI 부재)** — 데이터 소비 지점 규칙 위반 (2026-07-14 신설 규칙)
3. **T11** — 화면 전용 클래스 (`.mypage-modal`) HANDOFF L336 규칙 위반 (U-COMMON-01 통합)

---

## 추천 착수 순서

1. **T12 + T1 통합 소규모 커밋** — 문구 4건 + 탭 SVG 아이콘 (파일 겹침 없어 안전)
2. **T5** — F0f-fix 통합 시 함께
3. **T9** — CLAUDE.md 규칙 위반 우선 해소. Q2 결정 후 착수
4. **T2 + T3 + T4** 순차 — history 탭 대공사 3티켓 연계
5. **T6** — Q3 결정 후 착수
6. **T7** — Q4 결정 후 착수 (엔티티 확장이라 별도 마이그레이션 필요)
7. **T8** — 필요성 재검토 후 (email 유지가 정책일 수도)
8. **T10** — 소규모, 언제든
9. **T11** — U-COMMON-01 (Modal/Toast) 머지 후 자연 통합

---

## 참고 파일

- `docs/00_assets/prototype.tsx` L1202~1446 (MyPageScreen)
- `docs/00_assets/prototype.html` 동일 섹션
- `docs/00_assets/HANDOFF.md` §5.8~5.12, §5-E.6
- `src/main/resources/templates/mypage/{layout,history,favorites,notifications,profile-verify,profile-edit}.html`
- `src/main/java/io/github/sihyuuun/youthmoa/user/{User,MyPageController}.java`
