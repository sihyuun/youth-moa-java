---
id: D5-card-capacity-bar
status: spec_confirmed
created: 2026-07-13
decided_by: 사용자
---

# D5 — 프로그램 카드 CapacityBar 통일 (상세 페이지 fragment 통합 + prototype 매칭)

> **선행 발견**: 이 티켓은 신규 구축이 아니라 **검증·통일 티켓**. `ProgramCardDto` + `templates/fragments/capacity-bar.html` + `.capacity-bar-*` CSS 이미 존재. 홈/목록/검색 3곳은 fragment 사용 중이나 **상세 페이지만 별도 마크업** (`.detail-capacity-bar`) 이라 색상 임계 미적용.

## 확정 사항 (사용자 결정)

- **Q1 = b**: upcoming 시 `Program.startDate` 재활용 → `MM/dd 오픈` 라벨. 엔티티 확장 없음
  - **admin 트랙 이월** (`project_youth_moa_deferred_admin.md`): `applyStart`/`applyEnd` 분리 + `startDate`/`endDate` 를 운영기간으로 재정의
- **Q2 = a**: prototype 정확 매칭. 상단 라벨 = `정원 N/M명`, 색상 임계 라벨은 별도 위치 (`.capacity-bar-label--status`)
- **Q3 = b (Deferred)**: `showLabel=false` 미니 모드는 캘린더 뷰 티켓 착수 시 도입

## 변경 파일

- `templates/program/detail.html` L64~78 — 자체 `.detail-capacity-bar` 마크업 → `~{fragments/capacity-bar :: capacityBar(...)}` 호출
- `program/ProgramController.java` (상세) — `pct/colorClass/barLabel/capacityText` 모델 어트리뷰트 추가
- `templates/fragments/capacity-bar.html` — 상단 라벨을 `capacityText`(정원 N/M명) 로, 상태 라벨은 아래 별도 line 으로 재구성
  - upcoming 인 경우 우측에 `MM/dd 오픈` 표시 (`Program.startDate` `#temporals.format(startDate, 'MM/dd')`)
- `static/css/main.css` L946~964 — `.detail-capacity-bar*` dead code 제거
- `src/test/.../CapacityBarFragmentRenderTest.java` **신규** — pct 임계 5경계값 + upcoming 오픈일 표시 + 상세 페이지 fragment 렌더

## 색상 임계 (기존 유지)

| 조건 | colorClass | 상태 라벨 |
|---|---|---|
| status=UPCOMING | secondary | 신청 오픈 예정 |
| status=CLOSED | muted | 모집 마감 |
| ratio ≥ 0.9 | error | 마감임박 |
| 0.7 ≤ ratio < 0.9 | warning | 서두르세요 |
| 그 외 | primary | 모집중 |

## 검증

- 정적: `CapacityBarFragmentRenderTest` (경계값 5·upcoming 오픈일·상세 통합)
- 동적: `curl /programs/{id}` 응답에 `.capacity-bar-fill--warning` 등 임계 클래스 렌더 확인
- 시각: UPCOMING/70%/90%/CLOSED 4케이스 시각 대조
