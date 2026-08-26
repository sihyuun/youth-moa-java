-- 260826 P9 후속: 통합 검색 대상용 요약 컬럼 신설.
--
-- 배경: Program.content · Notice.content 는 @Lob → CLOB 이라 Hibernate 6 이 lower/upper(CLOB)
--       호출 시 grammar 에러 (500). content 자체를 검색 대상에서 제외하고, VARCHAR summary 필드로
--       요약 텍스트를 별도 관리해 검색 대상으로 사용.
--
-- 결정 사항 (2026-08-26):
--   - 필드명: summary
--   - 길이: 300 (검색 결과 preview · 시드 부담 균형)
--   - NULL 허용 (기존 row 무통증 마이그레이션 · 시드 없어도 부팅)
--   - 소스: 시드 수동 작성. admin 트랙 도입 후 자동 절단 훅 추가 예정
--   - 화면 노출 X (검색 매칭 대상 전용)

ALTER TABLE program ADD COLUMN summary VARCHAR(300);

ALTER TABLE notice ADD COLUMN summary VARCHAR(300);
