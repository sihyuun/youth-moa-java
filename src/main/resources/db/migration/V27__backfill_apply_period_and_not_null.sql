-- D5-Q1d (2026-10-06): applyStartDate/applyEndDate NOT NULL 승격.
--
-- 배경: V12 (2026-09-10) 로 apply_start_date / apply_end_date 를 nullable 로 추가했다.
-- 레거시 row 는 startDate/endDate 만 가지고 있어 Program.getStatus() · ProgramSpec · Repository
-- 전역에 COALESCE / effectiveApply* 폴백 블록이 10여 개 흩어졌다. 정량 축을 하나로 정리하기 위해
-- 운영 기간을 신청 기간으로 복사 백필한 뒤 NOT NULL 로 승격한다.
--
-- Set-A Q2=A 확정 (2026-10-02 Q1 명세 §Q2): 신청기간은 운영기간과 분리되지만, 레거시 데이터
-- 복원 전략은 단순 복사. 이후 운영자가 admin 폼에서 갱신한다.

-- 1) start_date → apply_start_date 백필 (null row 만)
UPDATE program
   SET apply_start_date = start_date
 WHERE apply_start_date IS NULL
   AND start_date IS NOT NULL;

-- 2) end_date → apply_end_date 백필
UPDATE program
   SET apply_end_date = end_date
 WHERE apply_end_date IS NULL
   AND end_date IS NOT NULL;

-- 3) NOT NULL 승격 — H2 2.x · PostgreSQL 양쪽 호환 ANSI 문법.
ALTER TABLE program ALTER COLUMN apply_start_date SET NOT NULL;
ALTER TABLE program ALTER COLUMN apply_end_date   SET NOT NULL;
