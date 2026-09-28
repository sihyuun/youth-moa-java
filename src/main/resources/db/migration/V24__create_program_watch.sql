-- A7-watcher-ui (2026-09-28): ProgramWatch 엔티티 신설.
-- Q-A7W-1 결정 (사용자 확정) — UNIQUE(admin_id, program_id) 로 동일 (admin, program) 중복 방지.
-- Q-A7W-8 결정 — 상한 20개 · 초과 시 오래된 자동 삭제 (Bookmark 일관, 애플리케이션 레벨 처리).
--
-- 인덱스 설계:
--   - idx_program_watch_program            = ApplicationCreatedEvent fan-out (WatcherRecipientResolver) 진입점
--   - idx_program_watch_admin_created DESC = 대시보드 "지켜보는 프로그램" 카드 최근 5개 조회
CREATE TABLE program_watch (
  id           BIGSERIAL PRIMARY KEY,
  admin_id     BIGINT      NOT NULL REFERENCES users(id),
  program_id   BIGINT      NOT NULL REFERENCES program(id),
  created_at   TIMESTAMP   NOT NULL,
  CONSTRAINT uk_program_watch_admin_program UNIQUE(admin_id, program_id)
);

CREATE INDEX idx_program_watch_program ON program_watch(program_id);
CREATE INDEX idx_program_watch_admin_created ON program_watch(admin_id, created_at DESC);
