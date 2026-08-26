-- 260826 P9 후속(A안): V5 로 신설된 summary 컬럼을 기존 row 에 backfill.
--
-- 배경: V5 는 컬럼만 신설 (nullable). PG 트랙(Testcontainers, Supabase) 은 기존 row 가 summary=null 로
--       존재. 이 상태로 배포하면 검색이 title/organization/region 만으로 좁아짐 (Notice 는 title 만).
--
-- 정책: content 앞 300자를 요약으로 파생 (엔티티 deriveSummaryFromContentIfMissing 과 동일 로직).
--       admin 트랙 도입 후 사람이 편집한 명시적 요약이 있으면 그 값이 우선 (WHERE summary IS NULL 로
--       기존 값 보존).
--
-- 대상: e2e/@DataJpaTest 는 Flyway off 이라 무영향. PG (Testcontainers/Supabase) 만 실행됨.
--
-- SUBSTRING(x, 1, 300) — H2·PG 표준. LEFT() 는 벤더 확장 (PG 만) 이라 표준 SUBSTRING 사용.

UPDATE program
SET summary = SUBSTRING(content, 1, 300)
WHERE summary IS NULL AND content IS NOT NULL;

UPDATE notice
SET summary = SUBSTRING(content, 1, 300)
WHERE summary IS NULL AND content IS NOT NULL;
