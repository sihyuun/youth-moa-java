-- 260826 chore/content-lob-to-text: content 컬럼을 OID(Large Object 참조 정수) → text 로 이관.
--
-- 배경 (PR #183 · PR #185 · PR #186 학습 산물):
--   Program.content · Notice.content 는 @Lob String 이었고 Hibernate 6 이 PG 에서 이걸 oid 로 매핑.
--   결과: lower(oid)·upper(oid)·SUBSTRING(oid) 전부 grammar 실패 → 통합 검색 500 발생.
--   PR #184 는 summary VARCHAR(300) 을 별도 신설해 우회. 이번 트랙은 근본 fix.
--
-- 조치:
--   ALTER COLUMN ... TYPE text USING convert_from(lo_get(content), 'UTF8')
--     · lo_get(oid) → LO 저장소에서 바이트 배열 로드
--     · convert_from(bytea, 'UTF8') → UTF-8 문자열 디코딩
--     · text 컬럼에 직접 저장
--
-- 대상 프로파일:
--   e2e/@DataJpaTest 는 Flyway off (H2 create-drop) → 무영향
--   Testcontainers PG + Supabase 만 실행
--
-- 후속:
--   V8 에서 summary 컬럼 DROP (근본 fix 후 dead column 이라 청소)
--   pg_largeobject orphan 정리는 별도 트랙 (필요 시)

ALTER TABLE program
    ALTER COLUMN content TYPE text
    USING convert_from(lo_get(content), 'UTF8');

ALTER TABLE notice
    ALTER COLUMN content TYPE text
    USING convert_from(lo_get(content), 'UTF8');
