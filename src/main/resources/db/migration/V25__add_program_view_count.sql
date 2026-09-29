-- A6-followup (2026-09-29): 프로그램 조회수 추적 컬럼.
-- ProgramController GET /programs/{id} 진입 시 USER/anon 만 세션 dedup 후 +1.
-- CENTER_ADMIN/SYSTEM_ADMIN 은 skip. admin-stats.programStats 셀 실 수치 렌더에 사용.

ALTER TABLE program ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0;
