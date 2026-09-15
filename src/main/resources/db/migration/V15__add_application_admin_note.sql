-- A4 admin-program-detail (Qn-1 A): 관리자 담당자 의견 저장 컬럼.
-- prototype.html L2739 textarea "담당자 의견을 입력해주세요. 사용자에게 노출됩니다."
-- 단일 컬럼 · nullable · 최대 1000자. 이력은 후행 티켓에서 별도 테이블로 승격 고려.
ALTER TABLE application ADD COLUMN admin_note VARCHAR(1000);
