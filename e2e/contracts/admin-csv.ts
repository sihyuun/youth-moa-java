/**
 * A8 admin-bulk-csv (2026-09-17) — CSV export 공통 계약.
 *
 * 이 파일은 CSS selector 기반의 시각 계약이 아니라 **CSV response header/body 검증 시나리오** 를 문서화한다.
 * 실제 assertion 은 `tests/admin-csv-*.spec.ts` 에서 `page.waitForEvent('download')` 로 파일을 받고 fs read
 * 로 헤더 첫 줄 · BOM · Content-Disposition 을 검증한다. 이 계약은 갭 리포트 대상이 아니므로 checks 는 비운다.
 *
 * 정책 (POLICY.md 신규):
 *  - P-CSV-1: 파일명 `{domain}_{yyyyMMdd_HHmmss}.csv` (KST)
 *  - P-CSV-2: UTF-8 + BOM (0xEF 0xBB 0xBF) · Content-Type `text/csv; charset=UTF-8`
 *  - P-CSV-3: CRLF 라인 종결 (opencsv 기본)
 *  - Qn-7: Content-Disposition = attachment (RFC 5987 `filename*=UTF-8''` 병용)
 *
 * 도메인별 CSV 헤더 (구현: AdminCsvController):
 *  - users:        id,email,name,phone,role,isActive,lastAccessAt,createdAt
 *  - programs:     id,title,centerName,category,applyStartDate,applyEndDate,
 *                  startDate,endDate,capacity,isActive,status,createdAt
 *  - applications: id,applicantEmail,applicantName,phone,status,appliedAt,
 *                  processedAt,processedBy,rejectReason,adminNote
 *
 * 접근 권한 (Qn-CSV2):
 *  - users CSV: SYSTEM_ADMIN 전용 (개인정보 대량 export)
 *  - programs CSV: SYSTEM_ADMIN + CENTER_ADMIN (센터 격리)
 *  - applications CSV: SYSTEM_ADMIN + CENTER_ADMIN (센터 격리)
 */

import type { ScreenContract } from './types';

export const adminCsvContract: ScreenContract = {
    screen: 'admin-csv',
    path: '/admin/users',
    source: 'docs/specs/A8-admin-bulk-csv.md §4-2 · POLICY.md P-CSV-1~3',
    viewport: { width: 1440, height: 900 },
    // Header/body assertion 은 기능 E2E 에서. 여기는 계약 등록만 하고 checks 는 비운다.
    checks: [],
};
