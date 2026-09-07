-- A-admin-terms-crud (Qn-2 B, 2026-09-04 확정): 약관 본문(content)을 DB TEXT 컬럼으로 저장.
--
-- 배경: 기존 F-signup-terms-agreement 는 contentPath (예: "/terms") 로 정적 템플릿 페이지를 참조해
-- signup 모달이 fetch 로 로드했다. admin CRUD 도입 시 편집 가능한 본문을 DB 로 승격하는 게 실사용성에
-- 유리하여 재판정.
--
-- Step 1: nullable content 컬럼 추가
-- Step 2: 기존 시드 (SERVICE, PRIVACY — DataInitializer.seedTerms L112~131) 백필
-- Step 3: NOT NULL 승격
--
-- 한 번 머지된 V 파일은 수정 금지 (CLAUDE.md DB 규칙).

ALTER TABLE terms ADD COLUMN content TEXT;

UPDATE terms
   SET content = '<p>청년모아 서비스 이용약관 (기본 초안).</p><p>이 약관은 관리자 페이지에서 편집할 수 있어요. 실제 약관 문안은 관리자가 업데이트해주세요.</p>'
 WHERE code = 'SERVICE';

UPDATE terms
   SET content = '<p>청년모아 개인정보처리방침 안내 (기본 초안).</p><p>이 약관은 관리자 페이지에서 편집할 수 있어요. 실제 처리방침 문안은 관리자가 업데이트해주세요.</p>'
 WHERE code = 'PRIVACY';

ALTER TABLE terms ALTER COLUMN content SET NOT NULL;
