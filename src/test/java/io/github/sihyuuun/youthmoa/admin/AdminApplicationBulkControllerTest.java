package io.github.sihyuuun.youthmoa.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A5-2 admin-approval-bulk Controller RBAC/경계 보강 (2026-10-12 ym-impl 3차):
 *
 * <ul>
 *   <li>U2 — reason 200자 성공 / 201자 → flashError (@Size 경계)
 *   <li>U3 — ids 501건 → MAX_BULK_SIZE 가드 flashError (실제 Application 생성 X · fake id 리스트)
 *   <li>U4 — CENTER_ADMIN 이 타 센터 프로그램으로 bulk POST → 403 (@PreAuthorize + scope 가드)
 * </ul>
 *
 * <p>Service 가 아닌 Controller 레벨에서 검증: 200/201자 길이 체크는 {@link AdminApplicationController#bulkReject}
 * 가 선처리하고 (Service 호출 전 flash redirect), 501건 가드는 Service assertSizeCap 이 던지는
 * IllegalArgumentException 을 Controller 가 flashError 로 변환한다. 두 흐름 모두 Controller 통합 테스트가 유일한 현실적 검증
 * 지점.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminApplicationBulkControllerTest {

  private static final long SEED_PROGRAM_ID = 3L; // program 3 은 전부 PENDING (시드 규약)

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;
  @Autowired ProgramRepository programRepository;

  // ================= U2: reason 200/201 경계 =================

  @Test
  void bulkReject_reason_200chars_success_flash() throws Exception {
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    String reason200 = "가".repeat(200);
    // ids 는 비어도 Controller 는 200자 길이 체크를 통과시킨 뒤 Service 로 넘기고
    // Service 는 빈 ids → 즉시 BulkResult.total=0 반환 → Controller 는 "선택된 항목이 없어요" flash.
    // 핵심은 "200자는 길이 가드를 통과한다" (flashError 가 길이 메시지가 아니어야 함).
    mockMvc
        .perform(
            post("/admin/programs/" + SEED_PROGRAM_ID + "/applications/bulk/reject")
                .with(user(new UserPrincipal(sysadmin)))
                .with(csrf())
                .param("reason", reason200))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/programs/" + SEED_PROGRAM_ID + "/applications"))
        // 200자 길이 가드는 통과 → "200자 이하" 문구가 flashError 에 없어야 함
        .andExpect(
            flash().attribute("flashError", Matchers.not(Matchers.containsString("200자 이하"))));
  }

  @Test
  void bulkReject_reason_201chars_rejects_with_length_flash() throws Exception {
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    String reason201 = "가".repeat(201);
    mockMvc
        .perform(
            post("/admin/programs/" + SEED_PROGRAM_ID + "/applications/bulk/reject")
                .with(user(new UserPrincipal(sysadmin)))
                .with(csrf())
                .param("reason", reason201)
                .param("ids", "1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("flashError", Matchers.containsString("200자 이하")));
  }

  // ================= U3: ids 500/501 경계 =================

  @Test
  void bulkApprove_ids_501_rejects_with_cap_flash() throws Exception {
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    // 실제 Application 500건 생성은 비용·시드 오염 커서 생략. fake id 리스트만 전달.
    // Service assertSizeCap 이 size > 500 을 가장 먼저 체크 (findById 조회 전) → IllegalArgumentException.
    // Controller 가 flashError 로 변환한다.
    List<String> ids501 =
        IntStream.rangeClosed(1, 501).mapToObj(String::valueOf).collect(Collectors.toList());
    var req =
        post("/admin/programs/" + SEED_PROGRAM_ID + "/applications/bulk/approve")
            .with(user(new UserPrincipal(sysadmin)))
            .with(csrf());
    for (String id : ids501) req = req.param("ids", id);
    mockMvc
        .perform(req)
        .andExpect(status().is3xxRedirection())
        .andExpect(
            flash()
                .attribute(
                    "flashError",
                    Matchers.allOf(
                        Matchers.containsString("500건"), Matchers.containsString("501"))));
  }

  @Test
  void bulkReject_ids_501_rejects_with_cap_flash() throws Exception {
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    List<String> ids501 =
        IntStream.rangeClosed(1, 501).mapToObj(String::valueOf).collect(Collectors.toList());
    var req =
        post("/admin/programs/" + SEED_PROGRAM_ID + "/applications/bulk/reject")
            .with(user(new UserPrincipal(sysadmin)))
            .with(csrf())
            .param("reason", "501건 가드 테스트 사유");
    for (String id : ids501) req = req.param("ids", id);
    mockMvc
        .perform(req)
        .andExpect(status().is3xxRedirection())
        .andExpect(
            flash()
                .attribute(
                    "flashError",
                    Matchers.allOf(
                        Matchers.containsString("500건"), Matchers.containsString("501"))));
  }

  // ================= U4: CENTER_ADMIN cross-center 403 =================

  @Test
  void bulkApprove_centerAdmin_otherCenterProgram_forbidden() throws Exception {
    User centerAdmin = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    Long myCenterId = centerAdmin.getCenter() == null ? null : centerAdmin.getCenter().getId();
    // center1 소속이 아닌 프로그램을 찾는다
    Program other =
        programRepository.findAll().stream()
            .filter(
                p ->
                    p.getCenter() == null
                        || myCenterId == null
                        || !myCenterId.equals(p.getCenter().getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("other-center program seed missing"));

    mockMvc
        .perform(
            post("/admin/programs/" + other.getId() + "/applications/bulk/approve")
                .with(user(new UserPrincipal(centerAdmin)))
                .with(csrf())
                .param("ids", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  void bulkReject_centerAdmin_otherCenterProgram_forbidden() throws Exception {
    User centerAdmin = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    Long myCenterId = centerAdmin.getCenter() == null ? null : centerAdmin.getCenter().getId();
    Program other =
        programRepository.findAll().stream()
            .filter(
                p ->
                    p.getCenter() == null
                        || myCenterId == null
                        || !myCenterId.equals(p.getCenter().getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("other-center program seed missing"));

    mockMvc
        .perform(
            post("/admin/programs/" + other.getId() + "/applications/bulk/reject")
                .with(user(new UserPrincipal(centerAdmin)))
                .with(csrf())
                .param("reason", "타 센터 접근 시도")
                .param("ids", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  void bulkApproveAllPending_centerAdmin_otherCenterProgram_forbidden() throws Exception {
    User centerAdmin = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    Long myCenterId = centerAdmin.getCenter() == null ? null : centerAdmin.getCenter().getId();
    Program other =
        programRepository.findAll().stream()
            .filter(
                p ->
                    p.getCenter() == null
                        || myCenterId == null
                        || !myCenterId.equals(p.getCenter().getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("other-center program seed missing"));

    mockMvc
        .perform(
            post("/admin/programs/" + other.getId() + "/applications/bulk/approve-all-pending")
                .with(user(new UserPrincipal(centerAdmin)))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }
}
