-- A-admin-notice-attachment (2026-09-03): Notice.createdBy 관리 (Qn-8 Custom 작성자 기반 RBAC).
-- CENTER_ADMIN 은 본인 작성 공지만 수정/삭제 가능하도록 작성자 FK 신설.
-- 3단계 안전 백필: nullable 로 컬럼 추가 → sysadmin 소유로 백필 → NOT NULL 승격.

-- Step 1: nullable 컬럼 추가
ALTER TABLE notice ADD COLUMN created_by BIGINT REFERENCES users(id);

-- Step 2: 기존 시드 공지 전량을 sysadmin 소유로 백필
UPDATE notice
SET created_by = (SELECT id FROM users WHERE email = 'sysadmin@youth-moa.test')
WHERE created_by IS NULL;

-- Step 3: NOT NULL 승격 (사용자 결정: null 미허용, sysadmin 소유로 승격)
ALTER TABLE notice ALTER COLUMN created_by SET NOT NULL;
