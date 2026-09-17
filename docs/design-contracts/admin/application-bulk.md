# admin-application-bulk 디자인 계약

> 출처: `docs/specs/A8-admin-bulk-csv.md §후속` (2026-09-17 Qn-App1 A안 확정)

## deviation

**Applications bulk approve** 는 prototype.tsx/L1391 계열에 명시되지 않은 신규 편입 (deviation).

- 근거: 사용자 요구 · 성수기 운영 효율 · A4 개별 approve flow 정합
- 형식: 계약 파일 `admin-application-bulk.ts` 에 `deviation: 'A8 Qn-App1 A · deviation'` 명시

## Reject bulk 는 이월

Approve 만 편입, Reject 는 A4 개별 처리 유지.

- 근거: 반려 사유의 개별성 · 감사·항의 대응 정합 · 정부·청년몽땅 실무 정합
- deferred 티켓: `A8-reject-bulk` (향후 실무 사용 후 필요 시 템플릿 사유 dropdown 방식 검토)

## 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| POST | `/admin/programs/{id}/applications/bulk/approve` | 다건 승인 · `ids[]` |

per-row 트랜잭션. 각 성공 건마다 기존 `ApplicationApprovedEvent` 발송 (사용자 알림 자동 · A4 정합).

## 회귀 방어

- A4 개별 approve/reject/cancel endpoint 무변경
- Application 도메인 메서드 무변경
- 이벤트 발행 유지 → 알림·통계 계열 무회귀

## 스코프 필터

- SYSTEM_ADMIN: 전체 프로그램
- CENTER_ADMIN: `program.organization == AdminScope.effectiveCenterName()` 매칭. 미매칭 프로그램은 서비스 진입점에서 `IllegalStateException` → 컨트롤러 `AccessDeniedException` 승격 (403)
