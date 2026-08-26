-- 260826 chore/content-lob-to-text: summary 컬럼 폐기.
--
-- 배경: PR #184 는 content(@Lob CLOB) 검색 우회 목적으로 program/notice 에 summary VARCHAR(300) 신설.
--       V7 로 content 를 text 매핑 이관해 원문 직접 검색 가능해짐 → summary 존재 이유 소멸.
--
-- 조치: 두 테이블에서 summary 컬럼 DROP. 엔티티 코드는 이미 필드/훅 삭제 완료 (Program.java · Notice.java).
--
-- 대상 프로파일: e2e/@DataJpaTest 는 Flyway off (H2 create-drop) → 무영향.
--              Testcontainers PG + Supabase 만 실행.

ALTER TABLE program DROP COLUMN summary;

ALTER TABLE notice DROP COLUMN summary;
