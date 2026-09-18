-- A5-1 admin-staff-management (2026-09-18): 관리자 초기 password 자동생성 + 강제 변경 flag + 발급자 감사.
-- 회귀 방어:
--   - must_change_password DEFAULT FALSE → 기존 row (시드 + 일반 사용자) 무영향
--   - password_changed_at NULL 허용 → 기존 유저는 알 수 없음 (unknown)
--   - invited_by NULL 허용 → 자체 가입 유저는 invited_by=NULL 유지
-- 참조: docs/specs/A5-1-admin-staff-management.md §4

ALTER TABLE users
  ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
  ADD COLUMN password_changed_at TIMESTAMP;

ALTER TABLE users
  ADD COLUMN invited_by BIGINT REFERENCES users(id);

-- 강제 password 변경 대상 조회 최적화 (인터셉터에서 principal.mustChangePassword flag 로 판정하므로 실제 사용은 드묾)
CREATE INDEX idx_users_must_change_password ON users (must_change_password) WHERE must_change_password = TRUE;
