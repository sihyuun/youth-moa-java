-- A9-b (2026-09-22): Program.center_id NOT NULL 승격.
--
-- A9-a (V20) 에서 nullable 로 추가하고 ProgramCenterBackfill 이 부팅 시 organization 문자열 매칭으로
-- 채워 왔다. Q-A9-b-1 결정: organization 컬럼은 V21 시점에는 유지 (Program.builder 가 계속 채우고,
-- V22 에서 DROP). Backfill 검증이 100% 완료된 상태이므로 NOT NULL 승격 안전.
--
-- 실패 조건: center_id IS NULL 인 program row 가 존재하면 이 마이그레이션은 실패한다.
-- 이 경우 A9-a 의 Backfill 이 성공했는지, seed 이후 center 미할당 row 가 생겼는지 확인 필요.

ALTER TABLE program
  ALTER COLUMN center_id SET NOT NULL;
