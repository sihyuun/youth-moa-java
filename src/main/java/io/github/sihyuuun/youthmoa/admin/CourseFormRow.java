package io.github.sihyuuun.youthmoa.admin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A3-2 admin-program-form-integration (2026-09-11 · Qn-Δ-encoding A): 강좌 폼 row.
 *
 * <p>Spring 표준 repeated form fields ({@code courses[0].name}) 로 바인딩. id 가 있으면 update, 없으면 insert.
 * 목록에서 사라진 기존 id 는 soft delete.
 */
@Getter
@Setter
@NoArgsConstructor
public class CourseFormRow {
  private Long id;
  private String name;
  private String schedule;
  private Integer capacity;

  /** 완전 빈 row (Qn-Δ-empty-row A: 자동 스킵). */
  public boolean isBlank() {
    return (id == null)
        && (name == null || name.trim().isEmpty())
        && (schedule == null || schedule.trim().isEmpty())
        && capacity == null;
  }
}
