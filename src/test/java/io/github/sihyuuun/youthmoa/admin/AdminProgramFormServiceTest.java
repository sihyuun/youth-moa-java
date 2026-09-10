package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.program.ApprovalMode;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * A3-1 admin-program-form (2026-09-10): AdminProgramService.create/update/delete + validation 검증. Qn-3 A
 * (FK 있으면 400) 및 Qn-Δ4 A (신청기간 필수) 등 spec 결정 사항 회귀.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminProgramFormServiceTest {

  @Autowired AdminProgramService adminProgramService;
  @Autowired ProgramRepository programRepository;

  @AfterEach
  void clearAuth() {
    SecurityContextHolder.clearContext();
  }

  private ProgramFormRequest validRequest(String title) {
    ProgramFormRequest r = new ProgramFormRequest();
    r.setTitle(title);
    r.setOrganization("e2e 센터");
    r.setContent("본문");
    r.setDescription("설명");
    r.setStartDate(LocalDate.of(2026, 10, 1));
    r.setEndDate(LocalDate.of(2026, 10, 31));
    r.setApplyStartDate(LocalDate.of(2026, 9, 1));
    r.setApplyEndDate(LocalDate.of(2026, 9, 30));
    r.setVenue("장소");
    r.setContact("02-000-0000");
    r.setCapacity(50);
    r.setApprovalMode(ApprovalMode.MANUAL);
    r.setActive(true);
    return r;
  }

  @Test
  void create_필수값_모두_있으면_저장() {
    Program saved = adminProgramService.create(validRequest("신규-a"));
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getTitle()).isEqualTo("신규-a");
    assertThat(saved.getApprovalMode()).isEqualTo(ApprovalMode.MANUAL);
    assertThat(saved.isActive()).isTrue();
    assertThat(saved.getVenue()).isEqualTo("장소");
  }

  @Test
  void create_title_누락_400() {
    ProgramFormRequest r = validRequest("x");
    r.setTitle("  ");
    assertThatThrownBy(() -> adminProgramService.create(r))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("제목");
  }

  @Test
  void create_organization_누락_400() {
    ProgramFormRequest r = validRequest("y");
    r.setOrganization("");
    assertThatThrownBy(() -> adminProgramService.create(r))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("청년센터");
  }

  @Test
  void create_content_누락_400() {
    ProgramFormRequest r = validRequest("z");
    r.setContent(null);
    assertThatThrownBy(() -> adminProgramService.create(r))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("상세 내용");
  }

  @Test
  void create_신청_기간_누락_400() {
    ProgramFormRequest r = validRequest("no-apply");
    r.setApplyStartDate(null);
    assertThatThrownBy(() -> adminProgramService.create(r))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("신청 기간");
  }

  @Test
  void create_신청시작_마감_역전_400() {
    ProgramFormRequest r = validRequest("reverse-apply");
    r.setApplyStartDate(LocalDate.of(2026, 10, 1));
    r.setApplyEndDate(LocalDate.of(2026, 9, 1));
    assertThatThrownBy(() -> adminProgramService.create(r))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("신청 시작일");
  }

  @Test
  void create_진행시작_종료_역전_400() {
    ProgramFormRequest r = validRequest("reverse-run");
    r.setStartDate(LocalDate.of(2026, 11, 1));
    r.setEndDate(LocalDate.of(2026, 10, 1));
    assertThatThrownBy(() -> adminProgramService.create(r))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("진행 시작일");
  }

  @Test
  void create_capacity_0_이하_400() {
    ProgramFormRequest r = validRequest("bad-cap");
    r.setCapacity(0);
    assertThatThrownBy(() -> adminProgramService.create(r))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("모집 인원");
  }

  @Test
  void update_기존_프로그램_수정_반영() {
    Program saved = adminProgramService.create(validRequest("upd-src"));
    ProgramFormRequest r = validRequest("upd-mod");
    r.setVenue("바뀐 장소");
    Program updated = adminProgramService.update(saved.getId(), r);
    assertThat(updated.getTitle()).isEqualTo("upd-mod");
    assertThat(updated.getVenue()).isEqualTo("바뀐 장소");
  }

  @Test
  void update_없는_id_400() {
    assertThatThrownBy(() -> adminProgramService.update(9_999_999L, validRequest("x")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("존재하지 않는");
  }

  @Test
  void delete_FK_없는_신규_삭제_성공() {
    Program saved = adminProgramService.create(validRequest("del-me"));
    Long id = saved.getId();
    adminProgramService.delete(id);
    assertThat(programRepository.findById(id)).isEmpty();
  }

  @Test
  void delete_FK_있는_시드_프로그램_1_삭제_400() {
    // 시드 프로그램 #1 은 다수 seed 신청 존재 → FK 참조로 삭제 거부
    assertThatThrownBy(() -> adminProgramService.delete(1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("신청 이력");
  }

  @Test
  void delete_없는_id_400() {
    assertThatThrownBy(() -> adminProgramService.delete(9_999_999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("존재하지 않는");
  }

  @Test
  void create_approvalMode_null_이면_MANUAL_로_저장() {
    ProgramFormRequest r = validRequest("mode-null");
    r.setApprovalMode(null);
    Program saved = adminProgramService.create(r);
    assertThat(saved.getApprovalMode()).isEqualTo(ApprovalMode.MANUAL);
  }

  @Test
  void update_active_false_로_설정_시_SUSPENDED() {
    Program saved = adminProgramService.create(validRequest("act-toggle"));
    ProgramFormRequest r = validRequest("act-toggle");
    r.setActive(false);
    Program updated = adminProgramService.update(saved.getId(), r);
    assertThat(updated.isActive()).isFalse();
  }
}
