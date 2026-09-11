-- A3-2 admin-program-form-integration (2026-09-11 · Qn-A/B/C/D + Δ 13종 모두 A):
-- 프로그램에 강좌(Course) 다중 row 편집 기능 도입.
--
-- 정책 요약:
--   Qn-B A: 강좌 필드 name (필수) · schedule · capacity (모두 nullable schedule/capacity) · sort_order · is_active
--   Qn-Δ-B-max: 상한 20개 (application 계층 검증)
--   Qn-Δ-sortOrder A: 서버가 제출 순서대로 재부여 (클라이언트 sort_order 미노출)
--   Qn-Δ-drag A: 드래그·드롭 미도입, up/down 화살표만
--   ON DELETE CASCADE: 프로그램은 소프트 삭제만 하므로 실무 발생 확률 낮으나 admin 강제 정리 대비
--
-- 한 번 머지된 V 파일은 수정 금지 (CLAUDE.md DB 규칙).

ALTER TABLE program ADD COLUMN has_courses BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE course (
    id BIGSERIAL PRIMARY KEY,
    program_id BIGINT NOT NULL REFERENCES program(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    schedule VARCHAR(200),
    capacity INT,
    sort_order INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_course_program ON course (program_id, is_active, sort_order);
