# admin-csv (CSV export) 디자인 계약

> 출처: `docs/specs/A8-admin-bulk-csv.md §4-2` · POLICY.md P-CSV-1~3

## 공통 정책

- **P-CSV-1** 파일명: `{domain}_{yyyyMMdd_HHmmss}.csv` (KST · `_`). 예: `users_20260917_143025.csv`
- **P-CSV-2** 인코딩: UTF-8 + BOM (0xEF 0xBB 0xBF) · `Content-Type: text/csv; charset=UTF-8`
- **P-CSV-3** 라인 종결: CRLF (opencsv 기본)
- **Qn-7** `Content-Disposition: attachment; filename="..."; filename*=UTF-8''...` (RFC 5987 병용)
- **QD** 동기 스트림. 규모 확장 시 비동기 email 발송 이월 (`A8-follow-async`)

## 엔드포인트

| Method | Path | 필터 | 접근 권한 |
|---|---|---|---|
| GET | `/admin/users/export.csv` | `q, role, ids` | SYSTEM_ADMIN 전용 (개인정보) |
| GET | `/admin/programs/export.csv` | `q, status, ids` | SYSTEM_ADMIN + CENTER_ADMIN |
| GET | `/admin/programs/{id}/applications/export.csv` | `status, q, ids` | SYSTEM_ADMIN + CENTER_ADMIN |

## Qn-CSV1 이중 진입

- **상단 CSV 버튼**: `ids` 없음 → 현재 필터 결과 **전체** export (페이지네이션 무시)
- **하단 액션바 CSV**: `ids=1,2,3` → 선택 건만 export (필터 무시)

## 도메인별 컬럼 (구현: AdminCsvController)

| 도메인 | 컬럼 |
|---|---|
| users | id, email, name, phone, role, isActive, lastAccessAt, createdAt |
| programs | id, title, organization, category, applyStartDate, applyEndDate, startDate, endDate, capacity, isActive, status, createdAt |
| applications | id, applicantEmail, applicantName, phone, status, appliedAt, processedAt, processedBy, rejectReason, adminNote |

`viewCount` 는 Program 엔티티에 미도입 상태 (A6 이월). 도입 시 컬럼 추가.

## 회귀 방어

- opencsv 5.12.0 Reader (`CenterCsvLoader`) 무회귀 — 신규는 Writer 만 사용
- 세션·인증 flow 무영향 (attachment 응답, redirect 없음)
