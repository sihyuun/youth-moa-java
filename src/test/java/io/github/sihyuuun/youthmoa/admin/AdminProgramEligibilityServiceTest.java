package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramEligibility;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * F4-admin-eligibility (2026-09-09) — AdminProgramEligibilityService 단위 검증.
 *
 * <p>범위: 3필드 CRUD · Qn-2 A 공란=삭제 · Qn-4 A 길이 검증 (100/100/200) · null-safe. RBAC 는
 * 컨트롤러 @PreAuthorize 게이트라 별도 검증. AdminApplyQuestionServiceTest 의 dynamic Proxy 스타일 승계.
 */
class AdminProgramEligibilityServiceTest {

  private AdminProgramEligibilityService service;
  private Program program;

  @BeforeEach
  void setup() {
    program =
        Program.builder()
            .title("청년 문화예술 스쿨")
            .organization("의왕청년발전소")
            .content("본문")
            .isActive(true)
            .eligibility(
                ProgramEligibility.builder()
                    .age("만 19세 ~ 39세 청년")
                    .region("의왕시 거주 또는 활동")
                    .etc("문화예술 입문자 대상")
                    .build())
            .build();
    setField(program, "id", 7L);
    service = new AdminProgramEligibilityService(inMemoryProgramRepo(program));
  }

  // ================= 정상 케이스 =================

  @Test
  void update_replacesAllThreeFields() {
    service.update(7L, "만 25세 ~ 34세", "안양시 거주", "포트폴리오 필수");

    ProgramEligibility next = program.getEligibility();
    assertThat(next).isNotNull();
    assertThat(next.getAge()).isEqualTo("만 25세 ~ 34세");
    assertThat(next.getRegion()).isEqualTo("안양시 거주");
    assertThat(next.getEtc()).isEqualTo("포트폴리오 필수");
  }

  @Test
  void update_trimsWhitespaceValues() {
    service.update(7L, "  만 20세  ", " 서울시 ", "  기타  ");

    assertThat(program.getEligibility().getAge()).isEqualTo("만 20세");
    assertThat(program.getEligibility().getRegion()).isEqualTo("서울시");
    assertThat(program.getEligibility().getEtc()).isEqualTo("기타");
  }

  @Test
  void update_partialFields_nullsUnfilledOnes() {
    // age 만 채우고 region/etc 는 공란 → age 는 값, 나머지는 null
    service.update(7L, "만 19세 이상", "", "");

    ProgramEligibility next = program.getEligibility();
    assertThat(next).isNotNull();
    assertThat(next.getAge()).isEqualTo("만 19세 이상");
    assertThat(next.getRegion()).isNull();
    assertThat(next.getEtc()).isNull();
  }

  // ================= Qn-2 A: 공란 = 삭제 =================

  @Test
  void update_allBlank_setsEligibilityToNull() {
    service.update(7L, "", "", "");

    // Qn-2 A: 3필드 모두 공란 → embedded 값을 null 로 저장 (삭제 취급)
    assertThat(program.getEligibility()).isNull();
  }

  @Test
  void update_allNullInputs_setsEligibilityToNull() {
    service.update(7L, null, null, null);
    assertThat(program.getEligibility()).isNull();
  }

  @Test
  void update_allWhitespaceInputs_setsEligibilityToNull() {
    service.update(7L, "   ", "\t", "\n");
    assertThat(program.getEligibility()).isNull();
  }

  // ================= Qn-4 A: 길이 검증 (100/100/200) =================

  @Test
  void update_rejectsAgeOver100() {
    String over = "A".repeat(101);
    assertThatThrownBy(() -> service.update(7L, over, "region", "etc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("연령")
        .hasMessageContaining("100")
        .hasMessageContaining("입력해주세요");
  }

  @Test
  void update_rejectsRegionOver100() {
    String over = "B".repeat(101);
    assertThatThrownBy(() -> service.update(7L, "age", over, "etc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("거주지")
        .hasMessageContaining("100");
  }

  @Test
  void update_rejectsEtcOver200() {
    String over = "C".repeat(201);
    assertThatThrownBy(() -> service.update(7L, "age", "region", over))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("기타")
        .hasMessageContaining("200");
  }

  @Test
  void update_acceptsExactly100And200() {
    String age100 = "A".repeat(100);
    String region100 = "B".repeat(100);
    String etc200 = "C".repeat(200);
    service.update(7L, age100, region100, etc200);
    assertThat(program.getEligibility().getAge()).hasSize(100);
    assertThat(program.getEligibility().getRegion()).hasSize(100);
    assertThat(program.getEligibility().getEtc()).hasSize(200);
  }

  // ================= 존재하지 않는 프로그램 =================

  @Test
  void findProgram_notFound_throwsIllegalArgument() {
    assertThatThrownBy(() -> service.findProgram(999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("존재하지 않는 프로그램");
  }

  @Test
  void update_nonexistentProgram_throwsIllegalArgument() {
    assertThatThrownBy(() -> service.update(999L, "age", "region", "etc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("존재하지 않는 프로그램");
  }

  // ================= null-safe: eligibility 가 원래 null 이었던 프로그램 =================

  @Test
  void update_onProgramWithoutEligibility_setsFreshEligibility() {
    Program noElig =
        Program.builder().title("t").organization("o").content("c").isActive(true).build();
    setField(noElig, "id", 8L);
    AdminProgramEligibilityService s2 =
        new AdminProgramEligibilityService(inMemoryProgramRepo(noElig));

    s2.update(8L, "만 20세", null, null);

    assertThat(noElig.getEligibility()).isNotNull();
    assertThat(noElig.getEligibility().getAge()).isEqualTo("만 20세");
  }

  // ================= 헬퍼 =================

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

  private static ProgramRepository inMemoryProgramRepo(Program program) {
    return (ProgramRepository)
        Proxy.newProxyInstance(
            AdminProgramEligibilityServiceTest.class.getClassLoader(),
            new Class<?>[] {ProgramRepository.class},
            (proxy, method, args) -> {
              if ("findById".equals(method.getName())) {
                Long id = (Long) args[0];
                return program.getId().equals(id) ? Optional.of(program) : Optional.empty();
              }
              if (method.getReturnType().equals(Optional.class)) return Optional.empty();
              if (method.getReturnType().equals(java.util.List.class)) return java.util.List.of();
              if (method.getReturnType().equals(long.class)) return 0L;
              if (method.getReturnType().equals(boolean.class)) return false;
              return null;
            });
  }
}
