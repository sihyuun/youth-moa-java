---
id: F0f-fix-program-list
status: spec_confirmed
created: 2026-07-13
decided_by: 사용자
---

# F0f-fix — 프로그램 목록 카드 CTA + 필터 전면 개편 + 알림 백엔드

## 3 PR 분리

| 순서 | 브랜치 | 범위 |
|---|---|---|
| 1 | `feature/F0f-fix-1-cta` | 카드 CTA 3분기 (신청/오픈알림/빈자리알림) + 모달 (U-COMMON 인프라 활용) |
| 2 | `feature/F0g-alert-subscription` | ProgramAlertSubscription 엔티티 + 등록 API + 스케줄러 |
| 3 | `feature/F0f-fix-2-filter` | 필터바 사이드바 → 인라인 pop chip 이관 (파급 큼) |

## 확정 사항

### C-Q1 = 도입 (INACTIVE 상태)
- `ProgramStatus` 에 `INACTIVE` 추가 (운영 중단)
- admin 페이지에서 세팅 가능 (admin 트랙 이월)
- CTA: `[운영이 중단되었어요]` disabled

### C-Q2 = prototype 기준 (필터바 인라인 pop chip)
- 좌측 사이드바 완전 제거
- 필터바 상단 인라인 pop chip (지역·청년센터 버튼 → 드롭다운 팝오버)
- U-COMMON 드롭다운 인프라 재사용
- featured Region/Center 개념 재검토 필요

### C-Q3 = 통일 ("기본순")
- UI 라벨: "최신순" → "기본순"
- 백엔드 sort 키 `newest` 는 유지 (호환성)

### C-Q4 = 통합 (캘린더 뷰 구현)
- disabled placeholder 해제 + `ProgramCalendar` 구현
- D5 CapacityBar Q3(`showLabel=false` 미니 모드) 도 이 시점에 도입
- 별도 세부 spec 필요 (F0f-fix-2 착수 전 또는 후)

### C-Q5 = B (알림 백엔드 포함, F0g 별도 PR)
- `ProgramAlertSubscription(id, user, program, type[OPEN|WAITLIST], channels[], createdAt)` 엔티티
- `POST /programs/{id}/alerts` HTMX 엔드포인트
- 스케줄러:
  - 매일 새벽: `Program.startDate` 도래 프로그램 OPEN 구독자 발송
  - 신청 취소 이벤트: 마감 프로그램 WAITLIST 구독자 발송 (`ApplicationNotificationListener` 확장)

### C-Q6 = a + b 조합 (팝오버 fragment 재도입)
- fragment 시그니처 단순화: `group: String` 만 인자, `options` 는 model attribute (`allRegions`/`allCenters`) 직접 참조
- 인터랙션: `th:onclick` → `th:data-*` (data-group, data-target) + JS 이벤트 delegation (`filter-popover.js`)
- 두 팝오버(regions/centers) 마크업 중복 제거

### C-Q7 = a (수동/기간 마감 분리)
- **만석** (`pct >= 100`): `[빈자리 알림 받기]` → WaitlistModal
- **CLOSED but pct < 100** (기간 만료): `[종료된 프로그램]` 회색 disabled 라벨
- **INACTIVE** (운영 중단): `[운영이 중단되었어요]` disabled

## 카드 CTA 3분기 (#6)

| 상태 | 조건 | 라벨 | 아이콘 | 색상 | 액션 |
|---|---|---|---|---|---|
| ACTIVE (신청 가능) | `status=ACTIVE && pct<100` | 신청하기 | check | primary outline | `/programs/{id}/apply` |
| UPCOMING | `status=UPCOMING` | 오픈 알림 받기 | bell | secondary(주황) outline | `openAlertModal` |
| 만석 | `pct>=100` | 빈자리 알림 받기 | bell | 회색 (borderLight bg) | `waitlistModal` |
| 기간 만료 | `status=CLOSED && pct<100` | 종료된 프로그램 | — | 회색 disabled | 클릭 불가 |
| INACTIVE | `status=INACTIVE` | 운영이 중단되었어요 | — | 회색 disabled | 클릭 불가 |

## 검증

### 정적
- `ProgramListRenderTest` 5분기 CTA 라벨·클래스·아이콘(`<svg`) assertion
- `ProgramAlertServiceTest` (구독 등록·중복 방지·발송 트리거)
- `FilterPopoverRenderTest` (팝오버 재도입 후 응답 truncation 없음)

### 동적
- `curl /programs` 각 CTA 존재 확인
- `curl -X POST /programs/1/alerts -d "type=WAITLIST&channels=EMAIL"` 200
- 필터 pop chip 클릭 → 드롭다운 열림 (F0f-fix-2)

### 시각 (사용자)
- Preview snapshot + prototype 대조 (3분기 CTA 색상·아이콘)
- 모달 오픈/닫기 · 필터바 반응형 flex-wrap · 캘린더 뷰 진입
