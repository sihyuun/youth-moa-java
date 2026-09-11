package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A3-2 admin-program-form-integration (2026-09-11 · Qn-B A): 프로그램별 강좌.
 *
 * <ul>
 *   <li>name 필수, schedule · capacity 선택
 *   <li>sortOrder 는 서버가 제출 순서대로 재부여 (Qn-Δ-sortOrder A)
 *   <li>상한 20개는 서비스 계층에서 강제 (Qn-Δ-B-max A)
 *   <li>소프트 삭제 ({@link #deactivate()}) — hard delete 시 강좌별 신청 이력 소실 위험 대비 유예
 * </ul>
 */
@Getter
@Entity
@Table(name = "course")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 200)
  private String schedule;

  @Column private Integer capacity;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Builder
  private Course(
      Program program,
      String name,
      String schedule,
      Integer capacity,
      Integer sortOrder,
      Boolean isActive) {
    this.program = program;
    this.name = name;
    this.schedule = schedule;
    this.capacity = capacity;
    this.sortOrder = sortOrder != null ? sortOrder : 1;
    this.isActive = isActive == null || isActive;
  }

  /** 편집 (프로그램은 변경 불가). */
  public void update(String name, String schedule, Integer capacity, int sortOrder) {
    this.name = name;
    this.schedule = schedule;
    this.capacity = capacity;
    this.sortOrder = sortOrder;
  }

  public void deactivate() {
    this.isActive = false;
  }

  public void activate() {
    this.isActive = true;
  }
}
