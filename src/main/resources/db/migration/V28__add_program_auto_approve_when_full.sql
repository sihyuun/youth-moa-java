-- FOLLOW-waitlist-auto-approve (2026-10-08 · Q1 A):
--   Program.auto_approve_when_full BOOLEAN NOT NULL DEFAULT false 추가.
--   Q4: 기존 row 는 전부 false 로 백필 (OFF→ON 전환 시 기존 PENDING 일괄 승급 금지).
--   approval_mode 와 독립 — enum 확장 대신 별도 boolean (A4 범위 외 변경 차단).
ALTER TABLE program
    ADD COLUMN auto_approve_when_full BOOLEAN NOT NULL DEFAULT false;
