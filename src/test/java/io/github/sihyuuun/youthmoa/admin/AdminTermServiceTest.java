package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.user.Term;
import io.github.sihyuuun.youthmoa.user.TermRepository;
import io.github.sihyuuun.youthmoa.user.UserAgreementRepository;
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
 * A-admin-terms-crud (Qn-1 B · Qn-2 B · Qn-3~9 A, 2026-09-04) — AdminTermService 단위 검증.
 *
 * <p>범위: XSS sanitize · validate 규칙 · version bump · FK 참조 시 삭제 거부. RBAC 는 컨트롤러
 * {@code @PreAuthorize} 게이트라 별도 검증. AdminNoticeServiceTest 의 dynamic Proxy 스타일 승계.
 */
class AdminTermServiceTest {

  private AdminTermService service;
  private InMemoryTermStore termStore;
  private long agreementCount;

  @BeforeEach
  void setup() {
    termStore = new InMemoryTermStore();
    agreementCount = 0L;
    service =
        new AdminTermService(inMemoryTermRepo(termStore), agreementRepo(() -> agreementCount));
  }

  // ================= sanitize (XSS) =================

  @Test
  void sanitize_removesScriptTag() {
    String safe = service.sanitize("<p>안녕</p><script>alert('xss')</script>");
    assertThat(safe).contains("<p>안녕</p>");
    assertThat(safe).doesNotContain("<script>");
    assertThat(safe).doesNotContain("alert");
  }

  @Test
  void sanitize_removesEventHandler() {
    String safe = service.sanitize("<p onclick=\"alert(1)\">클릭</p>");
    assertThat(safe).doesNotContain("onclick");
    assertThat(safe).doesNotContain("alert(1)");
  }

  @Test
  void sanitize_rejectsEmptyResult() {
    assertThatThrownBy(() -> service.sanitize("<script>only()</script>"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("렌더 가능한 내용이 없어요");
  }

  @Test
  void sanitize_rejectsNull() {
    assertThatThrownBy(() -> service.sanitize(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("본문을 입력해주세요");
  }

  // ================= create validate =================

  @Test
  void create_rejectsInvalidCodeLowercase() {
    assertThatThrownBy(() -> service.create("service", "제목", "/terms", "<p>본문</p>", true, 1, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("영대문자와 밑줄");
  }

  @Test
  void create_rejectsDuplicateCode() {
    termStore.put(buildTerm(1L, "SERVICE"));
    assertThatThrownBy(() -> service.create("SERVICE", "제목", "/terms", "<p>본문</p>", true, 1, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("이미 존재하는 약관 코드");
  }

  @Test
  void create_rejectsContentPathWithoutSlash() {
    assertThatThrownBy(
            () -> service.create("NEW", "제목", "terms-no-slash", "<p>x</p>", true, 1, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("/ 로 시작");
  }

  @Test
  void create_rejectsSortOrderOutOfRange() {
    assertThatThrownBy(() -> service.create("NEW", "제목", "/terms", "<p>x</p>", true, 1000, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("999 이하");
  }

  @Test
  void create_succeeds_withVersion1() {
    Term saved = service.create("NEW", "새 약관", "/terms", "<p>본문</p>", false, 5, true);
    assertThat(saved.getCode()).isEqualTo("NEW");
    assertThat(saved.getVersion()).isEqualTo(1);
    assertThat(saved.getContent()).contains("<p>본문</p>");
  }

  // ================= update / version bump =================

  @Test
  void update_bumpVersionTrue_incrementsVersion() {
    Term existing = buildTerm(1L, "SERVICE");
    setField(existing, "version", 3);
    termStore.put(existing);

    service.update(1L, "새 제목", "/terms", "<p>개정</p>", true, 1, true, true);

    assertThat(existing.getVersion()).isEqualTo(4);
    assertThat(existing.getTitle()).isEqualTo("새 제목");
  }

  @Test
  void update_bumpVersionFalse_keepsVersion() {
    Term existing = buildTerm(1L, "SERVICE");
    setField(existing, "version", 3);
    termStore.put(existing);

    service.update(1L, "오타 수정", "/terms", "<p>x</p>", true, 1, true, false);

    assertThat(existing.getVersion()).isEqualTo(3);
    assertThat(existing.getTitle()).isEqualTo("오타 수정");
  }

  // ================= delete + FK =================

  @Test
  void delete_rejects_whenFkExists() {
    termStore.put(buildTerm(1L, "SERVICE"));
    agreementCount = 3L;

    assertThatThrownBy(() -> service.delete(1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("회원 동의 이력이 3건");
    assertThat(termStore.rows).hasSize(1);
  }

  @Test
  void delete_succeeds_whenNoAgreement() {
    termStore.put(buildTerm(1L, "SERVICE"));
    agreementCount = 0L;

    service.delete(1L);

    assertThat(termStore.rows).isEmpty();
  }

  // ================= countActiveRequired =================

  @Test
  void countActiveRequired_filtersInactiveAndOptional() {
    Term t1 = buildTerm(1L, "SERVICE");
    setField(t1, "required", true);
    setField(t1, "isActive", true);
    Term t2 = buildTerm(2L, "PRIVACY");
    setField(t2, "required", true);
    setField(t2, "isActive", false);
    Term t3 = buildTerm(3L, "MARKETING");
    setField(t3, "required", false);
    setField(t3, "isActive", true);
    termStore.put(t1);
    termStore.put(t2);
    termStore.put(t3);

    assertThat(service.countActiveRequired()).isEqualTo(1L);
  }

  // ================= 헬퍼 =================

  private Term buildTerm(Long id, String code) {
    Term t =
        Term.builder()
            .code(code)
            .title(code + " 약관")
            .contentPath("/terms")
            .content("<p>초기</p>")
            .required(true)
            .version(1)
            .sortOrder(1)
            .isActive(true)
            .build();
    setField(t, "id", id);
    return t;
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

  // ================= in-memory store + proxy repos =================

  static class InMemoryTermStore {
    final Map<Long, Term> rows = new HashMap<>();
    final AtomicLong idSeq = new AtomicLong(100);

    void put(Term t) {
      if (t.getId() == null) {
        setField(t, "id", idSeq.getAndIncrement());
      }
      rows.put(t.getId(), t);
    }
  }

  /**
   * TermRepository dynamic proxy —
   * save/findById/findByCode/delete/findAllByOrderBySortOrderAscIdAsc
   * /findByIsActiveTrueOrderBySortOrderAsc 만 지원 (AdminTermService 가 사용하는 메서드).
   */
  private static TermRepository inMemoryTermRepo(InMemoryTermStore store) {
    return (TermRepository)
        Proxy.newProxyInstance(
            AdminTermServiceTest.class.getClassLoader(),
            new Class<?>[] {TermRepository.class},
            (proxy, method, args) -> {
              String name = method.getName();
              switch (name) {
                case "save":
                  Term entity = (Term) args[0];
                  store.put(entity);
                  return entity;
                case "findById":
                  return Optional.ofNullable(store.rows.get((Long) args[0]));
                case "findByCode":
                  String code = (String) args[0];
                  return store.rows.values().stream()
                      .filter(t -> t.getCode().equals(code))
                      .findFirst();
                case "delete":
                  Term del = (Term) args[0];
                  store.rows.remove(del.getId());
                  return null;
                case "findAllByOrderBySortOrderAscIdAsc":
                  List<Term> all = new ArrayList<>(store.rows.values());
                  all.sort(
                      (a, b) -> {
                        int c = Integer.compare(a.getSortOrder(), b.getSortOrder());
                        return c != 0 ? c : Long.compare(a.getId(), b.getId());
                      });
                  return all;
                case "findByIsActiveTrueOrderBySortOrderAsc":
                  return store.rows.values().stream().filter(Term::isActive).toList();
                default:
                  if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                  if (method.getReturnType().equals(List.class)) return List.of();
                  if (method.getReturnType().equals(long.class)) return 0L;
                  if (method.getReturnType().equals(boolean.class)) return false;
                  return null;
              }
            });
  }

  /** UserAgreementRepository proxy — countByTerm 만 사용. */
  private static UserAgreementRepository agreementRepo(
      java.util.function.LongSupplier countSupplier) {
    return (UserAgreementRepository)
        Proxy.newProxyInstance(
            AdminTermServiceTest.class.getClassLoader(),
            new Class<?>[] {UserAgreementRepository.class},
            (proxy, method, args) -> {
              if ("countByTerm".equals(method.getName())) return countSupplier.getAsLong();
              if (method.getReturnType().equals(Optional.class)) return Optional.empty();
              if (method.getReturnType().equals(List.class)) return List.of();
              if (method.getReturnType().equals(long.class)) return 0L;
              if (method.getReturnType().equals(boolean.class)) return false;
              return null;
            });
  }
}
