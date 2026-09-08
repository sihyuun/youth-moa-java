package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.application.ApplyAnswerRepository;
import io.github.sihyuuun.youthmoa.program.ApplyQuestion;
import io.github.sihyuuun.youthmoa.program.ApplyQuestionRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.QuestionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * F0c-dynamic-fields (2026-09-08): 관리자 동적 신청 필드 CRUD 서비스.
 *
 * <p>RBAC (Qn-1 A): SYSTEM_ADMIN 만. 컨트롤러의 {@code @PreAuthorize("hasRole('SYSTEM_ADMIN')")} 로 게이트 →
 * 서비스 재검증 없음.
 *
 * <p>정책:
 *
 * <ul>
 *   <li>TEXT: maxLength 필수 (1 이상)
 *   <li>DROPDOWN: options 최소 1개 (한 줄에 하나 · 서비스가 JSON 배열로 정규화)
 *   <li>ATTACHMENT: 별도 필드 없음. 정책은 사용자 apply 시점에 5MB · pdf/hwp/docx/xlsx 승계 (Qn-5 A).
 *   <li>Qn-8 C: hard delete 금지. 오직 {@link #deactivate(Long)} 만 제공.
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApplyQuestionService {

  /** DROPDOWN options JSON literal 입력 판별용 (thread-safe). */
  private static final ObjectMapper MAPPER = JsonMapper.builder().build();

  private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

  private final ApplyQuestionRepository applyQuestionRepository;
  private final ProgramRepository programRepository;
  private final ApplyAnswerRepository applyAnswerRepository;

  // ================= Read =================

  @Transactional(readOnly = true)
  public Program findProgram(Long programId) {
    return programRepository
        .findById(programId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로그램이에요: " + programId));
  }

  @Transactional(readOnly = true)
  public List<ApplyQuestion> list(Long programId) {
    return applyQuestionRepository.findByProgramIdOrderBySortOrderAscIdAsc(programId);
  }

  @Transactional(readOnly = true)
  public ApplyQuestion findById(Long id) {
    return applyQuestionRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 동적 필드에요: " + id));
  }

  @Transactional(readOnly = true)
  public long countAnswers(Long questionId) {
    return applyAnswerRepository.countByQuestionId(questionId);
  }

  // ================= Write =================

  @Transactional
  public ApplyQuestion create(
      Long programId,
      QuestionType fieldType,
      String label,
      boolean isRequired,
      int sortOrder,
      String rawOptions,
      Integer maxLength) {
    Program program = findProgram(programId);
    validateLabel(label);
    validateSortOrder(sortOrder);
    String normalizedOptions = normalizeOptions(fieldType, rawOptions);
    Integer normalizedMaxLength = normalizeMaxLength(fieldType, maxLength);
    ApplyQuestion q =
        ApplyQuestion.builder()
            .program(program)
            .fieldType(fieldType)
            .label(label.trim())
            .isRequired(isRequired)
            .sortOrder(sortOrder)
            .options(normalizedOptions)
            .maxLength(normalizedMaxLength)
            .isActive(true)
            .build();
    ApplyQuestion saved = applyQuestionRepository.save(q);
    log.info(
        "[admin-apply-question] created id={} program={} type={} label={}",
        saved.getId(),
        programId,
        fieldType,
        saved.getLabel());
    return saved;
  }

  @Transactional
  public ApplyQuestion update(
      Long id,
      QuestionType fieldType,
      String label,
      boolean isRequired,
      int sortOrder,
      String rawOptions,
      Integer maxLength) {
    validateLabel(label);
    validateSortOrder(sortOrder);
    ApplyQuestion q = findById(id);
    String normalizedOptions = normalizeOptions(fieldType, rawOptions);
    Integer normalizedMaxLength = normalizeMaxLength(fieldType, maxLength);
    q.update(
        fieldType, label.trim(), isRequired, sortOrder, normalizedOptions, normalizedMaxLength);
    log.info(
        "[admin-apply-question] updated id={} type={} label={}",
        q.getId(),
        fieldType,
        q.getLabel());
    return q;
  }

  /** Qn-8 C: soft delete. hard delete 는 절대 신설하지 않는다. */
  @Transactional
  public void deactivate(Long id) {
    ApplyQuestion q = findById(id);
    q.deactivate();
    log.info("[admin-apply-question] deactivated id={}", id);
  }

  @Transactional
  public void reactivate(Long id) {
    ApplyQuestion q = findById(id);
    q.activate();
    log.info("[admin-apply-question] reactivated id={}", id);
  }

  // ================= 검증 & 정규화 =================

  private void validateLabel(String label) {
    if (label == null || label.trim().isEmpty()) {
      throw new IllegalArgumentException("질문 라벨을 입력해주세요.");
    }
    if (label.length() > 200) {
      throw new IllegalArgumentException("질문 라벨은 200자 이하여야 합니다.");
    }
  }

  private void validateSortOrder(int sortOrder) {
    if (sortOrder < 1 || sortOrder > 999) {
      throw new IllegalArgumentException("정렬 순서는 1 이상 999 이하여야 합니다.");
    }
  }

  /**
   * DROPDOWN 만 options 검증 · JSON 정규화. TEXT/ATTACHMENT 는 무조건 null 반환 (부적절한 값 누출 방지).
   *
   * <p>입력 형식 (관리자 편의로 3종 모두 허용, 최종 저장은 항상 JSON 배열로 정규화):
   *
   * <ol>
   *   <li>JSON 배열 literal: {@code ["A","B","C"]} → 그대로 파싱 (QA 반려 P0-2, 2026-09-08)
   *   <li>개행 구분: {@code A\nB\nC}
   *   <li>콤마 구분: {@code A,B,C} (개행 없을 때만)
   * </ol>
   *
   * <p>각 옵션은 trim, 빈 문자열은 제거. 최소 1개 이상, 최대 50개.
   */
  String normalizeOptions(QuestionType fieldType, String raw) {
    if (fieldType != QuestionType.DROPDOWN) return null;
    if (raw == null || raw.trim().isEmpty()) {
      throw new IllegalArgumentException("드롭다운 옵션을 한 줄에 하나씩 입력해주세요.");
    }
    List<String> opts = new ArrayList<>();
    String trimmedRaw = raw.trim();
    // Case 1: JSON 배열 literal 로 보이면 Jackson 파싱 시도. 실패 시 IllegalArgumentException.
    if (trimmedRaw.startsWith("[")) {
      try {
        List<String> parsed = MAPPER.readValue(trimmedRaw, STRING_LIST_TYPE);
        for (String p : parsed) {
          if (p == null) continue;
          String t = p.trim();
          if (!t.isEmpty()) opts.add(t);
        }
      } catch (JacksonException e) {
        throw new IllegalArgumentException("옵션은 JSON 배열 형식이어야 해요 예: [\"A\", \"B\", \"C\"]");
      }
    } else {
      // Case 2/3: 개행 기준 우선, 없으면 콤마.
      String[] parts =
          trimmedRaw.contains("\n") ? trimmedRaw.split("\\r?\\n") : trimmedRaw.split(",");
      for (String p : parts) {
        String t = p.trim();
        if (!t.isEmpty()) opts.add(t);
      }
    }
    if (opts.isEmpty()) {
      throw new IllegalArgumentException("드롭다운 옵션은 최소 1개 이상이어야 합니다.");
    }
    if (opts.size() > 50) {
      throw new IllegalArgumentException("드롭다운 옵션은 최대 50개까지 등록할 수 있어요.");
    }
    return ApplyQuestion.serializeOptions(opts);
  }

  /** TEXT 만 max_length 필수 (1 이상). 다른 타입은 무조건 null. */
  Integer normalizeMaxLength(QuestionType fieldType, Integer maxLength) {
    if (fieldType != QuestionType.TEXT) return null;
    if (maxLength == null || maxLength < 1) {
      throw new IllegalArgumentException("주관식 필드의 최대 글자수를 1 이상으로 입력해주세요.");
    }
    if (maxLength > 4000) {
      throw new IllegalArgumentException("주관식 필드의 최대 글자수는 4000 이하여야 합니다.");
    }
    return maxLength;
  }

  /** 관리자 UI 편의: 저장된 JSON 을 화면 편집용 개행 텍스트로 역직렬화. */
  public String optionsAsText(ApplyQuestion q) {
    if (q.getFieldType() != QuestionType.DROPDOWN) return "";
    List<String> list = q.getOptionList();
    return String.join("\n", list);
  }

  /** QuestionType enum 목록 (템플릿 드롭다운용). */
  public QuestionType[] allTypes() {
    return Arrays.copyOf(QuestionType.values(), QuestionType.values().length);
  }
}
