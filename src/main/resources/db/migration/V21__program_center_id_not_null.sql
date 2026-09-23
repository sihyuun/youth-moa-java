-- A9-b (2026-09-22): Program.center_id NOT NULL 승격.
--
-- A9-a (V20) 에서 nullable 로 추가하고 ProgramCenterBackfill 이 부팅 시 organization 문자열 매칭으로
-- 채워 왔다. Q-A9-b-1 결정: organization 컬럼은 V21 시점에는 유지 (Program.builder 가 계속 채우고,
-- V22 에서 DROP). Backfill 검증이 100% 완료된 상태이므로 NOT NULL 승격 안전.
--
-- verify UNVERIFIED #15 해소: A9-b 는 ProgramCenterBackfill 러너를 삭제하므로 (Q-A9-b-3),
-- 실 DB 에 seed 외 경로로 삽입된 row 가 center_id IS NULL 인 경우 감지 지점이 사라진다.
-- V21 이 대신 사전 방어 블록으로 명시적 오류를 던진다. cryptic constraint violation 대신
-- 명확한 메시지 + 대응 지침 안내.

DO $$
DECLARE
  null_count BIGINT;
BEGIN
  SELECT COUNT(*) INTO null_count FROM program WHERE center_id IS NULL;
  IF null_count > 0 THEN
    RAISE EXCEPTION
      'A9-b V21 blocked: % row(s) in program have center_id IS NULL. '
      'A9-a Backfill 이 이 row 를 채우지 못했거나, A9-a 이후 center 미할당 row 가 삽입됐습니다. '
      'SELECT id, title, organization FROM program WHERE center_id IS NULL; 로 확인 후 '
      'UPDATE program SET center_id = (SELECT id FROM center WHERE name = organization) WHERE center_id IS NULL; '
      '수동 정정 후 재시도하세요.', null_count;
  END IF;
END $$;

ALTER TABLE program
  ALTER COLUMN center_id SET NOT NULL;
