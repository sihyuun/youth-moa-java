# admin-stats 계약 — `/admin/stats`

> 신설 2026-09-16 · A6 admin-stats · brancch `feature/A6-admin-stats`
> 계약 파일: `e2e/contracts/admin-stats.ts`

## 배경

관리자용 통계 대시보드. prototype `admin/prototype.html` L790~900 STATS 블록을 서버 SSR (Thymeleaf) 로 이식하되,
차트는 외부 CDN 없이 **서버가 SVG 문자열을 생성** 하여 `th:utext` 로 삽입한다 (Qn-1).

## 검사 대상 (요약)

| 영역 | 계약 항목 |
|---|---|
| GNB | `gnb.stats.active` — 통계 링크 활성 |
| KPI | 4개 카드 · 첫 카드 = 최근 30일 방문자 (Qn-9) |
| 방문자 차트 | 카드 · 타이틀 15px · 월/연도 탭 2개 · SVG 존재 · 요약 3셀 |
| 프로그램 테이블 | 카드 · 헤더 7열 |
| 도넛 | 성별 · 연령 카드 2개 · SVG 2개 (Qn-1 / Qn-5) |
| 리스트 | 마감임박(Qn-10) · 승인대기 2 카드 |

## 데이터 소스 · Qn 결정 요약 (spec §14)

- **성별** = `User.gender` GROUP BY 실시간 (Qn-A · 캐시 X · NULL → "무응답")
- **연령** = `AgeBucket.of(birthDate)` 실시간 (Qn-B · A6 신설 도메인 헬퍼)
- **방문자** = `VisitTrackingInterceptor` in-memory 누적 → `DailyVisitScheduler` cron `0 0 2 * * *` KST flush (Qn-C/D)
- **KPI 증감** = 최근 30일 vs 이전 30일 uniqueVisitors SUM 비교 (Qn-9)
- **마감 임박** = `applyEndDate` 우선 · fallback `endDate` · D-7 이내 (Qn-10)
- **연령 5구간 + UNKNOWN** = 19-24 · 25-29 · 30-34 · 35-39 · 40+ · UNKNOWN (Qn-2)
- **테스트 시** `spring.task.scheduling.enabled=false` (Qn-7)

## 회귀 방어 (spec §14 3점)

1. **A1 admin-dashboard.ts 계약 재검사 필수** — layout 변화 없음 확인
2. **Interceptor 성능** — postHandle 은 `DailyVisitCollector.record()` 만 호출 (in-memory, LongAdder)
3. **Observability PR #111 Counter 충돌 없음** — 별개 namespace, `youthmoa_stats_*` prefix 미사용 (통계는 DB 기반)

## deferred · deviation

| 항목 | 종류 | 사유 |
|---|---|---|
| Program.viewCount (조회수) | `deferred: A6-followup` | Program 엔티티에 컬럼 미도입 → 화면은 0 표시 |
| 실시간 chart hover 툴팁 | `deferred: A8-polish` | 서버 SVG 정적 렌더. hover 는 JS 로 후속 |
| Excel export | `deferred: A8` | |
| Cohort 분석 | `deferred: A6-followup` | |
| 방문자 원시 로그 | `deviation: Q5-A` | 개인정보 미저장 원칙 · 일별 집계만 |

## 관련 파일

- 컨트롤러: `AdminStatsController` (`/admin/stats` · `/admin/stats/chart`)
- 서비스: `AdminStatsService`
- 차트: `admin/chart/SvgChartRenderer`
- 인터셉터: `stats/VisitTrackingInterceptor`
- 스케줄러: `stats/DailyVisitScheduler`
- 엔티티: `stats/DailyVisit` · `user/AgeBucket`
- 마이그레이션: `V17__create_daily_visit.sql`
- 템플릿: `admin/stats.html` (fragment `chart`)
- CSS: `admin.css` (A6 섹션)
