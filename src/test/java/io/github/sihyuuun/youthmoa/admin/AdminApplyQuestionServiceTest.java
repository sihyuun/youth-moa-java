package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.application.ApplyAnswerRepository;
import io.github.sihyuuun.youthmoa.program.ApplyQuestion;
import io.github.sihyuuun.youthmoa.program.ApplyQuestionRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.QuestionType;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * F0c-dynamic-fields (2026-09-08) — AdminApplyQuestionService 단위 검증.
 *
 * <p>범위: CRUD · 타입별 validation · JSON options 직렬화 · Qn-8 C soft delete. RBAC 는 컨트롤러 @PreAuthorize
 * 게이트라 별도 검증. AdminTermServiceTest 의 dynamic Proxy 스타일 승계.
 */
class AdminApplyQuestionServiceTest {

  private AdminApplyQuestionService service;
  private InMemoryQuestionStore store;
  private Program program;
  private long answerCount;

  @BeforeEach
  void setup() {
    store = new InMemoryQuestionStore();
    answerCount = 0L;
    program = buildProgram(7L, "친환경 도시농부 프로젝트");
    service =
        new AdminApplyQuestionService(
            inMemoryQuestionRepo(store),
            inMemoryProgramRepo(program),
            answerRepo(() -> answerCount));
  }

  // ================= create (validation) =================

  @Test
  void create_rejectsBlankLabel() {
    assertThatThrownBy(() -> service.create(7L, QuestionType.TEXT, "  ", true, 1, null, 100))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("질문 라벨");
  }

  @Test
  void create_rejectsSortOrderOutOfRange() {
    assertThatThrownBy(() -> service.create(7L, QuestionType.TEXT, "라벨", true, 1000, null, 100))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("정렬 순서");
  }

  @Test
  void create_TEXT_requiresMaxLength() {
    assertThatThrownBy(() -> service.create(7L, QuestionType.TEXT, "라벨", true, 1, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("최대 글자수");
  }

  @Test
  void create_TEXT_rejectsMaxLengthOverLimit() {
    assertThatThrownBy(() -> service.create(7L, QuestionType.TEXT, "라벨", true, 1, null, 5000))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("4000 이하");
  }

  @Test
  void create_DROPDOWN_requiresOptions() {
    assertThatThrownBy(() -> service.create(7L, QuestionType.DROPDOWN, "라벨", true, 1, "  ", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("드롭다운 옵션");
  }

  @Test
  void create_DROPDOWN_normalizesOptionsNewlineToJson() {
    ApplyQuestion q = service.create(7L, QuestionType.DROPDOWN, "라벨", true, 1, "사과\n배\n감", null);
    assertThat(q.getOptions()).isEqualTo("[\"사과\",\"배\",\"감\"]");
    assertThat(q.getOptionList()).containsExactly("사과", "배", "감");
  }

  @Test
  void create_DROPDOWN_normalizesOptionsCommaToJson() {
    ApplyQuestion q = service.create(7L, QuestionType.DROPDOWN, "라벨", true, 1, "A,B,C", null);
    assertThat(q.getOptionList()).containsExactly("A", "B", "C");
  }

  @Test
  void create_ATTACHMENT_setsNoOptionsNoMaxLength() {
    ApplyQuestion q = service.create(7L, QuestionType.ATTACHMENT, "포트폴리오", false, 3, null, null);
    assertThat(q.getOptions()).isNull();
    assertThat(q.getMaxLength()).isNull();
    assertThat(q.getFieldType()).isEqualTo(QuestionType.ATTACHMENT);
  }

  @Test
  void create_TEXT_succeedsIsActiveTrue() {
    ApplyQuestion q = service.create(7L, QuestionType.TEXT, "지원 동기", true, 1, null, 300);
    assertThat(q.isActive()).isTrue();
    assertThat(q.getMaxLength()).isEqualTo(300);
    assertThat(q.getLabel()).isEqualTo("지원 동기");
  }

  // ================= update =================

  @Test
  void update_changesFieldsAndKeepsActive() {
    ApplyQuestion existing = buildQuestion(1L, QuestionType.TEXT, "old", 500);
    store.put(existing);

    service.update(1L, QuestionType.TEXT, "new label", false, 5, null, 200);

    assertThat(existing.getLabel()).isEqualTo("new label");
    assertThat(existing.getMaxLength()).isEqualTo(200);
    assertThat(existing.getSortOrder()).isEqualTo(5);
    assertThat(existing.isRequired()).isFalse();
  }

  // ================= Qn-8 C soft delete =================

  @Test
  void deactivate_setsInactiveButKeepsRow() {
    ApplyQuestion existing = buildQuestion(1L, QuestionType.TEXT, "라벨", 100);
    store.put(existing);

    service.deactivate(1L);

    assertThat(existing.isActive()).isFalse();
    assertThat(store.rows).hasSize(1);
  }

  @Test
  void reactivate_restoresActive() {
    ApplyQuestion existing = buildQuestion(1L, QuestionType.TEXT, "라벨", 100);
    existing.deactivate();
    store.put(existing);

    service.reactivate(1L);

    assertThat(existing.isActive()).isTrue();
  }

  // ================= optionsAsText 역직렬화 =================

  @Test
  void optionsAsText_dropdown_returnsNewlineJoined() {
    ApplyQuestion q =
        ApplyQuestion.builder()
            .program(program)
            .fieldType(QuestionType.DROPDOWN)
            .label("x")
            .sortOrder(1)
            .options("[\"a\",\"b\"]")
            .isActive(true)
            .build();
    assertThat(service.optionsAsText(q)).isEqualTo("a\nb");
  }

  @Test
  void optionsAsText_nonDropdown_returnsEmpty() {
    ApplyQuestion q =
        ApplyQuestion.builder()
            .program(program)
            .fieldType(QuestionType.TEXT)
            .label("x")
            .sortOrder(1)
            .maxLength(100)
            .isActive(true)
            .build();
    assertThat(service.optionsAsText(q)).isEmpty();
  }

  // ================= 헬퍼 =================

  private Program buildProgram(Long id, String title) {
    Program p =
        Program.builder()
            .title(title)
            .organization("test org")
            .content("test")
            .isActive(true)
            .build();
    setField(p, "id", id);
    return p;
  }

  private ApplyQuestion buildQuestion(Long id, QuestionType type, String label, Integer maxLen) {
    ApplyQuestion q =
        ApplyQuestion.builder()
            .program(program)
            .fieldType(type)
            .label(label)
            .isRequired(true)
            .sortOrder(1)
            .maxLength(maxLen)
            .isActive(true)
            .build();
    setField(q, "id", id);
    return q;
  }

  static void setField(Object target, String name, Object value) {
    try {
      Field f = findField(target.getClass(), name);
      f.setAccessible(true);
      f.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
    Class<?> c = cls;
    while (c != null) {
      try {
        return c.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
        c = c.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name);
  }

  static class InMemoryQuestionStore {
    final Map<Long, ApplyQuestion> rows = new HashMap<>();
    final AtomicLong idSeq = new AtomicLong(100);

    void put(ApplyQuestion q) {
      if (q.getId() == null) {
        setField(q, "id", idSeq.getAndIncrement());
      }
      rows.put(q.getId(), q);
    }
  }

  private static ApplyQuestionRepository inMemoryQuestionRepo(InMemoryQuestionStore store) {
    return (ApplyQuestionRepository)
        Proxy.newProxyInstance(
            AdminApplyQuestionServiceTest.class.getClassLoader(),
            new Class<?>[] {ApplyQuestionRepository.class},
            (proxy, method, args) -> {
              switch (method.getName()) {
                case "save":
                  ApplyQuestion q = (ApplyQuestion) args[0];
                  store.put(q);
                  return q;
                case "findById":
                  return Optional.ofNullable(store.rows.get((Long) args[0]));
                case "findByProgramIdOrderBySortOrderAscIdAsc":
                  List<ApplyQuestion> all = new ArrayList<>(store.rows.values());
                  all.sort(
                      (a, b) -> {
                        int c = Integer.compare(a.getSortOrder(), b.getSortOrder());
                        return c != 0 ? c : Long.compare(a.getId(), b.getId());
                      });
                  return all;
                case "findByProgramIdAndIsActiveTrueOrderBySortOrderAsc":
                  return store.rows.values().stream().filter(ApplyQuestion::isActive).toList();
                case "existsByProgramIdAndIsActiveTrue":
                  return store.rows.values().stream().anyMatch(ApplyQuestion::isActive);
                default:
                  if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                  if (method.getReturnType().equals(List.class)) return List.of();
                  if (method.getReturnType().equals(long.class)) return 0L;
                  if (method.getReturnType().equals(boolean.class)) return false;
                  return null;
              }
            });
  }

  private static ProgramRepository inMemoryProgramRepo(Program program) {
    return (ProgramRepository)
        Proxy.newProxyInstance(
            AdminApplyQuestionServiceTest.class.getClassLoader(),
            new Class<?>[] {ProgramRepository.class},
            (proxy, method, args) -> {
              if ("findById".equals(method.getName())) {
                Long id = (Long) args[0];
                return program.getId().equals(id) ? Optional.of(program) : Optional.empty();
              }
              if (method.getReturnType().equals(Optional.class)) return Optional.empty();
              if (method.getReturnType().equals(List.class)) return List.of();
              if (method.getReturnType().equals(long.class)) return 0L;
              if (method.getReturnType().equals(boolean.class)) return false;
              return null;
            });
  }

  private static ApplyAnswerRepository answerRepo(java.util.function.LongSupplier countSupplier) {
    return (ApplyAnswerRepository)
        Proxy.newProxyInstance(
            AdminApplyQuestionServiceTest.class.getClassLoader(),
            new Class<?>[] {ApplyAnswerRepository.class},
            (proxy, method, args) -> {
              if ("countByQuestionId".equals(method.getName())) return countSupplier.getAsLong();
              if ("existsByQuestionId".equals(method.getName()))
                return countSupplier.getAsLong() > 0;
              if (method.getReturnType().equals(Optional.class)) return Optional.empty();
              if (method.getReturnType().equals(List.class)) return List.of();
              if (method.getReturnType().equals(long.class)) return 0L;
              if (method.getReturnType().equals(boolean.class)) return false;
              return null;
            });
  }
}
