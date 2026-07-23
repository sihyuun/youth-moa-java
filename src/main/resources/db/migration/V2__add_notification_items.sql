-- PR #108 (mypage 알림 설정 재구성) 이 추가한 User 엔티티 3개 알림 항목 컬럼.
-- baseline v1 은 활성화 시점 스냅샷이므로 PR #108 이후 Supabase 에 미반영. V2 로 정식 마이그레이션.
--
-- default 값은 User.java 필드 초기값과 일치:
--   notifyRemindD1 = true, notifyWaitlistEmpty = true, notifyNewProgramNews = false
--
-- IF NOT EXISTS: V1 을 실제 실행한 Testcontainers·빈 PG 환경에서는 이미 컬럼이 있어 no-op.
-- Supabase (V1 baseline 스킵) 에서만 실제 컬럼 추가.

ALTER TABLE users ADD COLUMN IF NOT EXISTS notify_remind_d1 boolean NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS notify_waitlist_empty boolean NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS notify_new_program_news boolean NOT NULL DEFAULT false;
