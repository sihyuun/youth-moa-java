# admin-program-bulk 디자인 계약

> 출처: `docs/specs/A8-admin-bulk-csv.md` · prototype.tsx L507, prototype.html L2772, L4091~4109

## 기본 정책

- Qn-P1: **프로그램 bulk publish/unpublish 미도입** (prototype.tsx L507 명시 금지). `Program.getStatus()` 는 신청기간·정원 파생이라 UPDATE 불가. Bulk deactivate/reactivate (isActive) 로 대체
- CENTER_ADMIN 도 자기 센터 프로그램만 조작 가능 (AdminScope organization 문자열 매칭)

## 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| POST | `/admin/programs/bulk/deactivate` | 다건 운영 중단 (isActive=false → SUSPENDED) |
| POST | `/admin/programs/bulk/reactivate` | 다건 재활성화 |

## 회귀 방어

- A2/A3 개별 flow 무변경 (`/admin/programs/{id}/delete` 등)
- Program 도메인 메서드 (`activate`/`deactivate`) 무변경
- CENTER_ADMIN 데이터 격리: bulk 진입 시 각 row 마다 organization 매칭 검사, 미매칭 시 개별 skip (per-row 정합)

## deferred

- Excel (xlsx) export → `A8-xlsx`
- 비동기 email 발송 CSV → `A8-follow-async`
