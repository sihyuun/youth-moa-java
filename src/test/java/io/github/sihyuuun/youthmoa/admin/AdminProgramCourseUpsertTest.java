package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.program.ApprovalMode;
import io.github.sihyuuun.youthmoa.program.Course;
import io.github.sihyuuun.youthmoa.program.CourseRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * A3-2 admin-program-form-integration verify fix (2026-09-11): {@code upsertCourses} 신규 로직 회귀. spec
 * §5-1 · §5-3 · Qn-B / Qn-Δ-B-max / Qn-Δ-sortOrder / Qn-Δ-empty-row / hasCourses=false 승계 규칙 커버.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminProgramCourseUpsertTest {

  @Autowired AdminProgramService adminProgramService;
  @Autowired CourseRepository courseRepository;

  @AfterEach
  void clearAuth() {
    SecurityContextHolder.clearContext();
  }

  private ProgramFormRequest baseRequest(String title, boolean hasCourses) {
    ProgramFormRequest r = new ProgramFormRequest();
    r.setTitle(title);
    r.setOrganization("e2e 센터");
    r.setContent("본문");
    r.setDescription("설명");
    r.setStartDate(LocalDate.of(2026, 10, 1));
    r.setEndDate(LocalDate.of(2026, 10, 31));
    r.setApplyStartDate(LocalDate.of(2026, 9, 1));
    r.setApplyEndDate(LocalDate.of(2026, 9, 30));
    r.setCapacity(50);
    r.setApprovalMode(ApprovalMode.MANUAL);
    r.setActive(true);
    r.setHasCourses(hasCourses);
    r.setCourses(new ArrayList<>());
    return r;
  }

  private CourseFormRow row(Long id, String name, String schedule, Integer capacity) {
    CourseFormRow c = new CourseFormRow();
    c.setId(id);
    c.setName(name);
    c.setSchedule(schedule);
    c.setCapacity(capacity);
    return c;
  }

  @Test
  void create_강좌_3개_모두_insert_되고_sortOrder_1_2_3_재부여() {
    ProgramFormRequest r = baseRequest("course-create", true);
    r.getCourses().add(row(null, "A", "월", 10));
    r.getCourses().add(row(null, "B", "화", 20));
    r.getCourses().add(row(null, "C", "수", 30));

    Program saved = adminProgramService.create(r);

    List<Course> stored =
        courseRepository.findByProgramIdAndIsActiveTrueOrderBySortOrderAscIdAsc(saved.getId());
    assertThat(stored).hasSize(3);
    assertThat(stored).extracting(Course::getName).containsExactly("A", "B", "C");
    assertThat(stored).extracting(Course::getSortOrder).containsExactly(1, 2, 3);
  }

  @Test
  void update_기존_id_유지_새로운_id_추가_사라진_id_soft_delete() {
    // 1) 초기 생성 — A, B
    ProgramFormRequest create = baseRequest("course-upsert", true);
    create.getCourses().add(row(null, "A", "월", 10));
    create.getCourses().add(row(null, "B", "화", 20));
    Program saved = adminProgramService.create(create);

    List<Course> initial =
        courseRepository.findByProgramIdAndIsActiveTrueOrderBySortOrderAscIdAsc(saved.getId());
    Long idA = initial.get(0).getId();
    Long idB = initial.get(1).getId();

    // 2) 업데이트 — A(수정) 유지, B 제거, C 신규
    ProgramFormRequest update = baseRequest("course-upsert", true);
    update.getCourses().add(row(idA, "A-mod", "월-수정", 15));
    update.getCourses().add(row(null, "C", "수", 30));

    adminProgramService.update(saved.getId(), update);

    List<Course> active =
        courseRepository.findByProgramIdAndIsActiveTrueOrderBySortOrderAscIdAsc(saved.getId());
    assertThat(active).hasSize(2);
    assertThat(active).extracting(Course::getName).containsExactly("A-mod", "C");
    assertThat(active).extracting(Course::getSortOrder).containsExactly(1, 2);

    // B 는 활성/비활성 전체 조회에는 남아있고 isActive=false
    List<Course> all = courseRepository.findByProgramIdOrderBySortOrderAscIdAsc(saved.getId());
    Course b = all.stream().filter(c -> c.getId().equals(idB)).findFirst().orElseThrow();
    assertThat(b.isActive()).isFalse();
  }

  @Test
  void 상한_20_초과_400() {
    ProgramFormRequest r = baseRequest("course-max", true);
    for (int i = 0; i < 21; i++) {
      r.getCourses().add(row(null, "row-" + i, null, null));
    }
    assertThatThrownBy(() -> adminProgramService.create(r))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("20");
  }

  @Test
  void 강좌명_공백_400() {
    ProgramFormRequest r = baseRequest("course-blank", true);
    r.getCourses().add(row(null, "  ", "월", 10));
    assertThatThrownBy(() -> adminProgramService.create(r))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("강좌명");
  }

  @Test
  void hasCourses_false_로_전환하면_기존_활성_강좌_전량_soft_delete() {
    // 1) hasCourses=true 로 강좌 2개 생성
    ProgramFormRequest create = baseRequest("course-toggle", true);
    create.getCourses().add(row(null, "A", "월", 10));
    create.getCourses().add(row(null, "B", "화", 20));
    Program saved = adminProgramService.create(create);
    assertThat(
            courseRepository.findByProgramIdAndIsActiveTrueOrderBySortOrderAscIdAsc(saved.getId()))
        .hasSize(2);

    // 2) hasCourses=false 로 전환
    ProgramFormRequest toggle = baseRequest("course-toggle", false);
    // hasCourses=false 이면 rows 무시되어야 함
    toggle.getCourses().add(row(null, "should-ignore", null, null));
    adminProgramService.update(saved.getId(), toggle);

    List<Course> active =
        courseRepository.findByProgramIdAndIsActiveTrueOrderBySortOrderAscIdAsc(saved.getId());
    assertThat(active).isEmpty();
  }

  @Test
  void 빈_row_는_자동_스킵되고_상한_판정에도_제외() {
    // Qn-Δ-empty-row A: 완전히 비어있는 CourseFormRow 는 자동 skip
    ProgramFormRequest r = baseRequest("course-empty-skip", true);
    r.getCourses().add(row(null, "A", "월", 10));
    r.getCourses().add(row(null, null, null, null)); // blank row
    r.getCourses().add(row(null, "B", "화", 20));

    Program saved = adminProgramService.create(r);
    List<Course> stored =
        courseRepository.findByProgramIdAndIsActiveTrueOrderBySortOrderAscIdAsc(saved.getId());
    assertThat(stored).hasSize(2);
    assertThat(stored).extracting(Course::getName).containsExactly("A", "B");
  }

  @Test
  void capacity_0_이하_400() {
    ProgramFormRequest r = baseRequest("course-bad-cap", true);
    r.getCourses().add(row(null, "A", "월", 0));
    assertThatThrownBy(() -> adminProgramService.create(r))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("정원");
  }
}
