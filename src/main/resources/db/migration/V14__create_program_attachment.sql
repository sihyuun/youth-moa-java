-- A3-2 admin-program-form-integration (2026-09-11 · Qn-C A: NoticeAttachment 승계):
-- 프로그램 첨부파일 테이블 신설.
--
-- 정책 요약:
--   Qn-C-ext: pdf · hwp · docx · xlsx (application 계층 검증)
--   Qn-C-size: 5MB (application 계층 검증)
--   Qn-C-max : 10개 (application 계층 검증)
--   Qn-C-dup : storedName UUID + fileName 원본 유지 (NoticeAttachment 패턴)
--   Qn-Δ-C-bucket: 전용 bucket `program-attachments`
--   data bytea: NoticeAttachment 승계 (LocalFileStorage 폴백 + 다운로드 endpoint 호환)
--
-- 한 번 머지된 V 파일은 수정 금지 (CLAUDE.md DB 규칙).

CREATE TABLE program_attachment (
    id BIGSERIAL PRIMARY KEY,
    program_id BIGINT NOT NULL REFERENCES program(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255),
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    sort_order INT NOT NULL DEFAULT 0,
    data BYTEA,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_program_attachment_program ON program_attachment (program_id, sort_order);
