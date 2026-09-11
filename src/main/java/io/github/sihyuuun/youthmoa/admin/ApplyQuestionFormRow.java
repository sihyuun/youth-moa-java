package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.program.QuestionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A3-2 admin-program-form-integration (2026-09-11): 신청 질문 폼 row.
 *
 * <p>id 가 있으면 update, 없으면 insert. 목록에서 사라진 기존 id 는 soft delete (Qn-8 C 승계 · F0c 동일).
 */
@Getter
@Setter
@NoArgsConstructor
public class ApplyQuestionFormRow {
  private Long id;
  private QuestionType fieldType;
  private String label;
  private boolean required;
  private String options;
  private Integer maxLength;

  public boolean isBlank() {
    return (id == null)
        && (label == null || label.trim().isEmpty())
        && (options == null || options.trim().isEmpty())
        && (fieldType == null);
  }
}
