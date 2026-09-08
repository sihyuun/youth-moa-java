package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * F0c-dynamic-fields (2026-09-08): 관리자가 프로그램별로 지정하는 신청 폼 추가 질문.
 *
 * <ul>
 *   <li>Qn-3 A: fieldType = TEXT / DROPDOWN / ATTACHMENT
 *   <li>Qn-4 A: DROPDOWN options 는 JSON 배열을 {@link #options} TEXT 컬럼에 직렬화
 *   <li>Qn-2 A: {@link #isRequired} 관리자별 지정
 *   <li>Qn-8 C: soft delete only ({@link #deactivate()})
 * </ul>
 */
@Getter
@Entity
@Table(name = "apply_question")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplyQuestion extends BaseTimeEntity {

  /** DROPDOWN options JSON 파싱 전용 (thread-safe). Jackson 3.x → JsonMapper 팩터리 사용. */
  private static final ObjectMapper MAPPER = JsonMapper.builder().build();

  private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @Enumerated(EnumType.STRING)
  @Column(name = "field_type", nullable = false, length = 20)
  private QuestionType fieldType;

  @Column(nullable = false, length = 200)
  private String label;

  @Column(name = "is_required", nullable = false)
  private boolean isRequired;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  /** DROPDOWN 옵션 JSON 배열 문자열 (예: {@code ["A","B","C"]}). 다른 타입은 null. */
  @Column(columnDefinition = "TEXT")
  private String options;

  /** TEXT 필드 최대 길이. 다른 타입은 null. */
  @Column(name = "max_length")
  private Integer maxLength;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Builder
  private ApplyQuestion(
      Program program,
      QuestionType fieldType,
      String label,
      boolean isRequired,
      int sortOrder,
      String options,
      Integer maxLength,
      Boolean isActive) {
    this.program = program;
    this.fieldType = fieldType;
    this.label = label;
    this.isRequired = isRequired;
    this.sortOrder = sortOrder;
    this.options = options;
    this.maxLength = maxLength;
    this.isActive = isActive == null || isActive;
  }

  /** 편집 (프로그램은 변경 불가). options·maxLength 는 타입별로만 유의미하지만 검증은 서비스 계층에서 수행하고, 엔티티는 값을 그대로 반영한다. */
  public void update(
      QuestionType fieldType,
      String label,
      boolean isRequired,
      int sortOrder,
      String options,
      Integer maxLength) {
    this.fieldType = fieldType;
    this.label = label;
    this.isRequired = isRequired;
    this.sortOrder = sortOrder;
    this.options = options;
    this.maxLength = maxLength;
  }

  /** Qn-8 C: soft delete. */
  public void deactivate() {
    this.isActive = false;
  }

  public void activate() {
    this.isActive = true;
  }

  /** DROPDOWN options JSON → List<String> 변환. 파싱 실패나 null 은 빈 리스트. */
  public List<String> getOptionList() {
    if (options == null || options.isBlank()) return List.of();
    try {
      return MAPPER.readValue(options, STRING_LIST_TYPE);
    } catch (JacksonException e) {
      return List.of();
    }
  }

  /** 사용자 응답 값이 이 질문의 허용 옵션 안에 있는지 검사 (DROPDOWN 전용). */
  public boolean isValidDropdownValue(String candidate) {
    if (fieldType != QuestionType.DROPDOWN) return true;
    if (candidate == null) return false;
    return getOptionList().contains(candidate);
  }

  /** 편의: List<String> 을 JSON 배열 문자열로 직렬화. 서비스에서 호출. */
  public static String serializeOptions(List<String> opts) {
    if (opts == null || opts.isEmpty()) return null;
    try {
      return MAPPER.writeValueAsString(opts);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("options 직렬화 실패: " + e.getMessage(), e);
    }
  }
}
