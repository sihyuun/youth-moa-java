-- A9-a (2026-09-21): Program → Center FK 병행 도입.
--
-- 병행 유지 스텝. center_id 는 nullable 로 추가하고, ProgramCenterBackfill 이 부팅 시
-- organization 문자열과 Center.name 매칭으로 채운다. NOT NULL 전환·organization DROP 은 A9-b (V21/V22).
--
-- 배경: organization 은 100자 자유 문자열이라 오타·표기 불일치 위험이 크고,
--       admin CRUD 에서 Center 를 select 로 선택하도록 UX 개선하려면 FK 가 필요하다.
--       프로그램 상세의 문의처 전화 조회 등도 FK 로 매핑 명확화.

ALTER TABLE program
  ADD COLUMN center_id BIGINT REFERENCES center(id);

CREATE INDEX idx_program_center_id ON program(center_id);
