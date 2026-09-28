-- A7-createdBy-recipient (2026-09-28): Program.createdBy 신설.
-- Q-B3-1 결정 (사용자 확정) — 기존 program 에 작성자 필드가 없어 신설.
-- Q-B3-A 결정 — 기존 시드/운영 프로그램은 sysadmin 소유로 일괄 백필 (Notice V9 선례 승계).
-- Q-B3-4 결정 — createdBy 비활성 시 알림 skip 은 Resolver 레벨 처리, DB 는 항상 NOT NULL 유지.
-- 3단계 안전 백필: nullable 컬럼 추가 → sysadmin 소유로 백필 → NOT NULL 승격.

-- Step 1: nullable 컬럼 추가 (FK 즉시 설정)
ALTER TABLE program ADD COLUMN created_by BIGINT REFERENCES users(id);

-- Step 2: 기존 프로그램 전량을 sysadmin 소유로 백필
UPDATE program
SET created_by = (SELECT id FROM users WHERE email = 'sysadmin@youth-moa.test')
WHERE created_by IS NULL;

-- Step 3: NOT NULL 승격 (Q-B3-1 · Q-B3-A 결정: null 미허용, sysadmin 소유로 일괄 승격)
ALTER TABLE program ALTER COLUMN created_by SET NOT NULL;
