-- A6 admin-stats (2026-09-16): 일별 방문자 집계 테이블.
--
-- 수집 경로: VisitTrackingInterceptor (HandlerInterceptor.postHandle) 가
--   ConcurrentHashMap 에 in-memory 누적 → DailyVisitScheduler 가 매일 02:00 KST 에
--   flush + UPSERT. 매 요청 DB write 를 회피하여 p50 응답시간 영향을 최소화한다.
--
-- 컬럼:
--   * visit_date          집계 기준일 (KST). UNIQUE.
--   * unique_visitors     익명 + 인증 사용자 합산 (세션 단위)
--   * total_visits        페이지뷰 총합 (admin 제외 · 정적 리소스 제외)
--   * authenticated_visits 로그인 사용자 페이지뷰 (Qn-3 세부 · uniqueVisitors 하위 집합)

CREATE TABLE daily_visit (
    id BIGSERIAL PRIMARY KEY,
    visit_date DATE NOT NULL UNIQUE,
    unique_visitors INTEGER NOT NULL DEFAULT 0,
    total_visits INTEGER NOT NULL DEFAULT 0,
    authenticated_visits INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_visit_date ON daily_visit (visit_date DESC);
