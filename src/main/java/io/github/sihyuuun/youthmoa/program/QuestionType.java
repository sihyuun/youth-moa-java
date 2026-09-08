package io.github.sihyuuun.youthmoa.program;

/**
 * F0c-dynamic-fields (Qn-3 A): 관리자 설정 동적 질문 타입 3종.
 *
 * <ul>
 *   <li>{@link #TEXT} — 자유 입력 (max_length 관리자 지정)
 *   <li>{@link #DROPDOWN} — 관리자가 지정한 옵션 중 선택 (options JSON)
 *   <li>{@link #ATTACHMENT} — 파일 업로드 (5MB · pdf/hwp/docx/xlsx — Qn-5 A 전역 승계)
 * </ul>
 */
public enum QuestionType {
  TEXT,
  DROPDOWN,
  ATTACHMENT
}
