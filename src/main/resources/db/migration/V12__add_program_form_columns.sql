-- A3-1 admin-program-form (2026-09-10 · Qn-A/B/C/1~8/Δ1~6 모두 A):
-- 관리자 프로그램 등록·편집 폼(3탭: 정보/신청/약관) 을 위한 Program 컬럼 확장.
--
-- 정책 요약:
--   Qn-Δ1 A: 컬럼 6종 신설 (신청기간 · 진행장소 · 문의처 · 승인방식 · 약관 3개 · 설명)
--   Qn-Δ2 A: approval_mode = AUTO / MANUAL (기본 MANUAL)
--   Qn-Δ3 A: terms_service / terms_privacy / terms_marketing (prototype 약관 3분류)
--   Qn-Δ4 A: apply_start_date · apply_end_date 필수 (validation 은 서비스 계층)
--   Qn-8   A: 기존 시드 23건은 편집 없이 유지. 신설 컬럼은 nullable / default 로 무회귀
--   A3-2 이월: Course · ProgramAttachment · 이미지 파일 업로드
--
-- 한 번 머지된 V 파일은 수정 금지 (CLAUDE.md DB 규칙).

ALTER TABLE program ADD COLUMN apply_start_date DATE;
ALTER TABLE program ADD COLUMN apply_end_date DATE;
ALTER TABLE program ADD COLUMN venue VARCHAR(200);
ALTER TABLE program ADD COLUMN contact VARCHAR(100);
ALTER TABLE program ADD COLUMN approval_mode VARCHAR(10) NOT NULL DEFAULT 'MANUAL'
    CHECK (approval_mode IN ('AUTO', 'MANUAL'));
ALTER TABLE program ADD COLUMN terms_service   TEXT;
ALTER TABLE program ADD COLUMN terms_privacy   TEXT;
ALTER TABLE program ADD COLUMN terms_marketing TEXT;
ALTER TABLE program ADD COLUMN description     TEXT;
