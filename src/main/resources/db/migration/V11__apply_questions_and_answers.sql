-- F0c-dynamic-fields (2026-09-08 · Qn-1~10 A · Qn-Δ A · Qn-11 B · Qn-8 C):
-- 관리자 설정 동적 신청 필드 + 사용자 응답 저장.
--
-- 정책 요약:
--   Qn-3 A: field type = TEXT / DROPDOWN / ATTACHMENT
--   Qn-4 A: DROPDOWN options 를 JSON 배열로 apply_question.options TEXT 컬럼 저장
--   Qn-5 A: ATTACHMENT 는 admin-notice 정책 승계 (5MB · pdf/hwp/docx/xlsx) — 별도 컬럼 없음
--   Qn-6 A: 응답은 row per (application × question) → 별도 apply_answer 테이블
--   Qn-8 C: soft delete only. is_active=false 전환. 응답 이력 보존.
--   Qn-10 A: seed program #7 에만 3필드 백필 (TEXT · DROPDOWN · ATTACHMENT). 나머지 프로그램 0건.
--
-- 한 번 머지된 V 파일은 수정 금지 (CLAUDE.md DB 규칙).

CREATE TABLE apply_question (
    id           BIGSERIAL PRIMARY KEY,
    program_id   BIGINT       NOT NULL REFERENCES program(id) ON DELETE CASCADE,
    field_type   VARCHAR(20)  NOT NULL,
    label        VARCHAR(200) NOT NULL,
    is_required  BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order   INT          NOT NULL DEFAULT 1,
    options      TEXT,
    max_length   INT,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    CONSTRAINT apply_question_field_type_check
        CHECK (field_type IN ('TEXT', 'DROPDOWN', 'ATTACHMENT'))
);

CREATE INDEX idx_apply_question_program
    ON apply_question(program_id, is_active, sort_order);

-- 컬럼명 `value` 는 H2(MODE=PostgreSQL) / PostgreSQL 모두에서 예약어 계열이라
-- unquoted 사용 시 DDL 파싱 실패 (H2 42001, PG 는 quote 필요). Java 필드명은 `value` 유지,
-- 실제 컬럼만 `answer_value` 로 매핑 (2026-09-08 QA 반려 P0-1).
CREATE TABLE apply_answer (
    id                  BIGSERIAL PRIMARY KEY,
    application_id      BIGINT NOT NULL REFERENCES application(id) ON DELETE CASCADE,
    question_id         BIGINT NOT NULL REFERENCES apply_question(id),
    answer_value        TEXT,
    attachment_path     VARCHAR(500),
    attachment_filename VARCHAR(200),
    attachment_size     BIGINT,
    created_at          TIMESTAMP NOT NULL
);

CREATE INDEX idx_apply_answer_application ON apply_answer(application_id);
CREATE INDEX idx_apply_answer_question    ON apply_answer(question_id);

-- Qn-10 A: seed program #7 (친환경 도시농부 프로젝트 — DataInitializer 마지막 seed) 에만 3필드 백필.
-- id 는 auto-increment 로 1~3 확보 → SEED_APPLY_QUESTION_COUNT=3 상수와 정합.
-- program #7 이 존재하지 않으면 (기존 dev DB 등) INSERT 실패 대신 조용히 skip.
INSERT INTO apply_question
    (program_id, field_type, label, is_required, sort_order, options, max_length, is_active, created_at, updated_at)
SELECT 7, 'TEXT', '지원 동기', TRUE, 1, NULL, 500, TRUE, NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM program WHERE id = 7);

INSERT INTO apply_question
    (program_id, field_type, label, is_required, sort_order, options, max_length, is_active, created_at, updated_at)
SELECT 7, 'DROPDOWN', '관심 강좌', TRUE, 2, '["농작물 재배","유기농 요리","도시양봉"]', NULL, TRUE, NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM program WHERE id = 7);

INSERT INTO apply_question
    (program_id, field_type, label, is_required, sort_order, options, max_length, is_active, created_at, updated_at)
SELECT 7, 'ATTACHMENT', '포트폴리오', FALSE, 3, NULL, NULL, TRUE, NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM program WHERE id = 7);
