-- A9-b (2026-09-22): Program.organization 컬럼 완전 제거.
--
-- A9-a 에서 병행 유지하던 organization (String, length=100) 컬럼을 DROP.
-- 이후 모든 소비 지점은 program.center.name 을 사용한다.
--
-- Q-A9-b-2 결정: V21 (NOT NULL) 과 V22 (DROP) 를 단일 PR 로 배포. 학습 프로젝트 · Backfill
-- 검증 완료 · 롤백 리스크 낮음. 단일 원자적 전환으로 병행 상태 종료.

ALTER TABLE program
  DROP COLUMN organization;
