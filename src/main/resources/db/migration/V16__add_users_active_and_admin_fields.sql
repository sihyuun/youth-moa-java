-- A5 admin-users (2026-09-15): 관리자 사용자 관리에 필요한 상태·감사·메모 컬럼 신설.
--
-- 회귀 방어:
--   * is_active DEFAULT TRUE → 기존 유저 무영향 (UserPrincipal.isEnabled 반영 후에도 로그인 정상)
--   * V16 이 baseline 다음이라 기존 Supabase 는 Flyway history 에 이 스크립트만 추가 적용됨
--
-- 참조: ADMIN-00 §3-2 · docs/specs/A5-admin-users.md §3, §12

ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN last_access_at TIMESTAMP;
ALTER TABLE users ADD COLUMN admin_note VARCHAR(1000);
ALTER TABLE users ADD COLUMN deactivated_at TIMESTAMP;
ALTER TABLE users ADD COLUMN deactivated_by BIGINT REFERENCES users(id);
ALTER TABLE users ADD COLUMN deactivation_reason VARCHAR(500);

-- Q9: 기존 ADMIN role → CENTER_ADMIN 정규화 (deprecated enum 정리).
-- 주의: enum 소스 UserRole.ADMIN 제거는 후속 티켓 (chore/drop-user-role-admin).
UPDATE users SET role = 'CENTER_ADMIN' WHERE role = 'ADMIN';

-- 검색·필터 지원 인덱스
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_role ON users(role);
