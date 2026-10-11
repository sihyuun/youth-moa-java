package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.program.ApplyQuestion;
import io.github.sihyuuun.youthmoa.program.ApplyQuestionRepository;
import io.github.sihyuuun.youthmoa.program.Course;
import io.github.sihyuuun.youthmoa.program.CourseRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramAttachment;
import io.github.sihyuuun.youthmoa.program.ProgramAttachmentRepository;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.QuestionType;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * FOLLOW-program-duplicate (2026-10-08 · Q1~Q10 모두 권장안): AdminProgramService.duplicate() 검증.
 *
 * <ul>
 *   <li>Q2 제목 접미사 — "(복제본)" 포함
 *   <li>Q3 isActive=false
 *   <li>Q6 viewCount=0
 *   <li>Q8 autoApproveWhenFull=false
 *   <li>Q9 createdBy = 현재 세션 사용자
 *   <li>Q10 center = 원본 유지
 *   <li>Q1 ③ Course / ApplyQuestion isActive=true 만 복제, 비활성·첨부 미복제
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminProgramDuplicateServiceTest {

  private static final long SEED_PROGRAM_ID = 1L;

  @Autowired AdminProgramService adminProgramService;
  @Autowired ProgramRepository programRepository;
  @Autowired CourseRepository courseRepository;
  @Autowired ApplyQuestionRepository applyQuestionRepository;
  @Autowired ProgramAttachmentRepository programAttachmentRepository;
  @Autowired UserRepository userRepository;

  @AfterEach
  void clearAuth() {
    SecurityContextHolder.clearContext();
  }

  private User loginAsSysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(u);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    return u;
  }

  // ================= 기본 복제 매핑 =================

  @Test
  void duplicate_titleAppended_복제본접미사() {
    loginAsSysadmin();
    Program original = programRepository.findById(SEED_PROGRAM_ID).orElseThrow();
    Program copy = adminProgramService.duplicate(SEED_PROGRAM_ID);
    assertThat(copy.getId()).isNotEqualTo(original.getId());
    assertThat(copy.getTitle()).isEqualTo(original.getTitle() + " (복제본)");
  }

  @Test
  void duplicate_isActiveFalse_수동활성화전제() {
    loginAsSysadmin();
    Program copy = adminProgramService.duplicate(SEED_PROGRAM_ID);
    assertThat(copy.isActive()).isFalse();
  }

  @Test
  void duplicate_viewCountZero() {
    loginAsSysadmin();
    Program copy = adminProgramService.duplicate(SEED_PROGRAM_ID);
    assertThat(copy.getViewCount()).isZero();
  }

  @Test
  void duplicate_autoApproveFalse() {
    loginAsSysadmin();
    Program copy = adminProgramService.duplicate(SEED_PROGRAM_ID);
    assertThat(copy.isAutoApproveWhenFull()).isFalse();
  }

  @Test
  void duplicate_createdByCurrentUser() {
    User me = loginAsSysadmin();
    Program copy = adminProgramService.duplicate(SEED_PROGRAM_ID);
    assertThat(copy.getCreatedBy().getId()).isEqualTo(me.getId());
  }

  @Test
  void duplicate_centerPreserved() {
    loginAsSysadmin();
    Program original = programRepository.findById(SEED_PROGRAM_ID).orElseThrow();
    Program copy = adminProgramService.duplicate(SEED_PROGRAM_ID);
    assertThat(copy.getCenter().getId()).isEqualTo(original.getCenter().getId());
  }

  @Test
  void duplicate_datesPreserved() {
    loginAsSysadmin();
    Program original = programRepository.findById(SEED_PROGRAM_ID).orElseThrow();
    Program copy = adminProgramService.duplicate(SEED_PROGRAM_ID);
    assertThat(copy.getStartDate()).isEqualTo(original.getStartDate());
    assertThat(copy.getEndDate()).isEqualTo(original.getEndDate());
    assertThat(copy.getApplyStartDate()).isEqualTo(original.getApplyStartDate());
    assertThat(copy.getApplyEndDate()).isEqualTo(original.getApplyEndDate());
  }

  @Test
  void duplicate_contentAndEligibilityPreserved() {
    loginAsSysadmin();
    Program original = programRepository.findById(SEED_PROGRAM_ID).orElseThrow();
    Program copy = adminProgramService.duplicate(SEED_PROGRAM_ID);
    assertThat(copy.getContent()).isEqualTo(original.getContent());
    assertThat(copy.getCapacity()).isEqualTo(original.getCapacity());
    assertThat(copy.getApprovalMode()).isEqualTo(original.getApprovalMode());
    assertThat(copy.isHasCourses()).isEqualTo(original.isHasCourses());
    if (original.getEligibility() != null) {
      assertThat(copy.getEligibility()).isNotNull();
      assertThat(copy.getEligibility().getAge()).isEqualTo(original.getEligibility().getAge());
      assertThat(copy.getEligibility().getRegion()).isEqualTo(original.getEligibility().getRegion());
      assertThat(copy.getEligibility().getEtc()).isEqualTo(original.getEligibility().getEtc());
    }
  }

  // ================= Course / ApplyQuestion 복제 범위 =================

  @Test
  void duplicate_courseActiveOnly_비활성제외() {
    User me = loginAsSysadmin();
    Program source = seedProgramWithCourses(me);

    Program copy = adminProgramService.duplicate(source.getId());

    List<Course> copiedCourses =
        courseRepository.findByProgramIdOrderBySortOrderAscIdAsc(copy.getId());
    // 활성 2건만 복제 (비활성 1건 제외)
    assertThat(copiedCourses).hasSize(2);
    assertThat(copiedCourses).allMatch(Course::isActive);
    assertThat(copiedCourses)
        .extracting(Course::getName)
        .containsExactlyInAnyOrder("강좌1", "강좌2");
  }

  @Test
  void duplicate_questionActiveOnly_비활성제외() {
    User me = loginAsSysadmin();
    Program source = seedProgramWithQuestions(me);

    Program copy = adminProgramService.duplicate(source.getId());

    List<ApplyQuestion> copiedQuestions =
        applyQuestionRepository.findByProgramIdOrderBySortOrderAscIdAsc(copy.getId());
    assertThat(copiedQuestions).hasSize(2);
    assertThat(copiedQuestions).allMatch(ApplyQuestion::isActive);
    assertThat(copiedQuestions)
        .extracting(ApplyQuestion::getLabel)
        .containsExactlyInAnyOrder("질문1", "질문2");
  }

  @Test
  void duplicate_notFound_throws() {
    loginAsSysadmin();
    assertThatThrownBy(() -> adminProgramService.duplicate(999_999L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void duplicate_noAuth_throws() {
    // principal 없이 호출 — IllegalStateException
    assertThatThrownBy(() -> adminProgramService.duplicate(SEED_PROGRAM_ID))
        .isInstanceOf(IllegalStateException.class);
  }

  // ================= ym-verify U3: Attachment 미복제 입증 =================

  @Test
  void duplicate_attachmentsNotCopied_실입증() {
    // U3: 원본에 ProgramAttachment 2건 생성 후 복제 → 복제본 attachment=0 확인.
    // 이전 ym-qa 보고 ATTACH_ROWS=0 은 "원본에 Attachment 없음" 상태라 미입증 상태였음.
    User me = loginAsSysadmin();
    Program source = seedProgramWithCourses(me);
    programAttachmentRepository.save(
        ProgramAttachment.builder()
            .program(source)
            .fileName("spec.pdf")
            .fileSize(1024L)
            .contentType("application/pdf")
            .sortOrder(1)
            .data(new byte[] {1, 2, 3})
            .build());
    programAttachmentRepository.save(
        ProgramAttachment.builder()
            .program(source)
            .fileName("agenda.hwp")
            .fileSize(2048L)
            .contentType("application/x-hwp")
            .sortOrder(2)
            .data(new byte[] {4, 5, 6})
            .build());
    // 원본에 정상 적재됐는지 가드 (전제 검증)
    assertThat(programAttachmentRepository.findByProgramIdOrderBySortOrderAscIdAsc(source.getId()))
        .hasSize(2);

    Program copy = adminProgramService.duplicate(source.getId());

    // 복제본에는 Attachment 가 전무해야 함 (spec Q1 ③ — ProgramAttachment 미복제, 물리 파일 orphan 방지)
    List<ProgramAttachment> copied =
        programAttachmentRepository.findByProgramIdOrderBySortOrderAscIdAsc(copy.getId());
    assertThat(copied).isEmpty();
    // 원본 Attachment 는 그대로 유지
    assertThat(programAttachmentRepository.findByProgramIdOrderBySortOrderAscIdAsc(source.getId()))
        .hasSize(2);
  }

  // ================= ym-verify U4: title 255 trim 경계값 =================

  // 접미사 " (복제본)" 는 공백 1 + "(" 1 + 한글 3 + ")" 1 = 6 char (UTF-16 code units, BMP).
  // 상한 255 → trim 발동 임계: 원본 length > 255 - 6 = 249 일 때.

  @Test
  void duplicate_title249_fits_exactly255_접미사그대로() {
    // U4 경계: 249자 + 접미사 6 = 255 — 상한 정확히 맞춤, trim 없음.
    User me = loginAsSysadmin();
    Program source = seedProgramWithTitle(me, "A".repeat(249));

    Program copy = adminProgramService.duplicate(source.getId());

    assertThat(copy.getTitle()).hasSize(255);
    assertThat(copy.getTitle()).endsWith(" (복제본)");
    assertThat(copy.getTitle()).isEqualTo("A".repeat(249) + " (복제본)");
  }

  @Test
  void duplicate_title250_overflow_오리지널trim() {
    // U4 경계: 250자 + 접미사 6 = 256 — 1자 초과 → 원본을 249자로 trim 후 접미사.
    User me = loginAsSysadmin();
    Program source = seedProgramWithTitle(me, "B".repeat(250));

    Program copy = adminProgramService.duplicate(source.getId());

    assertThat(copy.getTitle()).hasSize(255);
    assertThat(copy.getTitle()).endsWith(" (복제본)");
    assertThat(copy.getTitle()).isEqualTo("B".repeat(249) + " (복제본)");
  }

  @Test
  void duplicate_title255_overflow_접미사만큼trim() {
    // U4 경계: 255자 원본 (상한) + 접미사 → 261 → trim 로직으로 249자+접미사.
    User me = loginAsSysadmin();
    Program source = seedProgramWithTitle(me, "C".repeat(255));

    Program copy = adminProgramService.duplicate(source.getId());

    assertThat(copy.getTitle()).hasSize(255);
    assertThat(copy.getTitle()).endsWith(" (복제본)");
    assertThat(copy.getTitle()).isEqualTo("C".repeat(249) + " (복제본)");
  }

  // ================= Helpers =================

  private Program seedProgramWithTitle(User admin, String title) {
    return programRepository.save(
        Program.builder()
            .title(title)
            .center(programRepository.findById(SEED_PROGRAM_ID).orElseThrow().getCenter())
            .category("c")
            .region("경기")
            .content("원본 콘텐츠")
            .startDate(java.time.LocalDate.now())
            .endDate(java.time.LocalDate.now().plusDays(30))
            .applyStartDate(java.time.LocalDate.now())
            .applyEndDate(java.time.LocalDate.now().plusDays(10))
            .createdBy(admin)
            .build());
  }

  private Program seedProgramWithCourses(User admin) {
    Program source =
        programRepository.save(
            Program.builder()
                .title("복제 테스트용 원본")
                .center(programRepository.findById(SEED_PROGRAM_ID).orElseThrow().getCenter())
                .category("c")
                .region("경기")
                .content("원본 콘텐츠")
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusDays(30))
                .applyStartDate(java.time.LocalDate.now())
                .applyEndDate(java.time.LocalDate.now().plusDays(10))
                .hasCourses(true)
                .createdBy(admin)
                .build());
    courseRepository.save(
        Course.builder().program(source).name("강좌1").sortOrder(1).isActive(true).build());
    courseRepository.save(
        Course.builder().program(source).name("강좌2").sortOrder(2).isActive(true).build());
    courseRepository.save(
        Course.builder().program(source).name("비활성강좌").sortOrder(3).isActive(false).build());
    return source;
  }

  private Program seedProgramWithQuestions(User admin) {
    Program source =
        programRepository.save(
            Program.builder()
                .title("복제 테스트용 원본Q")
                .center(programRepository.findById(SEED_PROGRAM_ID).orElseThrow().getCenter())
                .category("c")
                .region("경기")
                .content("원본 콘텐츠")
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusDays(30))
                .applyStartDate(java.time.LocalDate.now())
                .applyEndDate(java.time.LocalDate.now().plusDays(10))
                .createdBy(admin)
                .build());
    applyQuestionRepository.save(
        ApplyQuestion.builder()
            .program(source)
            .fieldType(QuestionType.TEXT)
            .label("질문1")
            .isRequired(true)
            .sortOrder(1)
            .maxLength(100)
            .isActive(true)
            .build());
    applyQuestionRepository.save(
        ApplyQuestion.builder()
            .program(source)
            .fieldType(QuestionType.TEXT)
            .label("질문2")
            .isRequired(false)
            .sortOrder(2)
            .maxLength(50)
            .isActive(true)
            .build());
    applyQuestionRepository.save(
        ApplyQuestion.builder()
            .program(source)
            .fieldType(QuestionType.TEXT)
            .label("비활성질문")
            .isRequired(false)
            .sortOrder(3)
            .maxLength(50)
            .isActive(false)
            .build());
    return source;
  }
}
