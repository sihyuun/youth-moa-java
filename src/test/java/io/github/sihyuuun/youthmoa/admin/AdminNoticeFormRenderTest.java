package io.github.sihyuuun.youthmoa.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.notice.NoticeRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A-admin-notice-attachment (2026-09-03 · ym-qa): 관리자 공지 화면 Thymeleaf 실 렌더 회귀 방어.
 *
 * <p>배경 (2026-07-09 F0h-c2 사고): {@code compileJava} + {@code JpaMappingTest} 만으로는 Thymeleaf 파싱/SpEL
 * 평가/inline fragment 렌더 이슈를 감지 못 함. admin notice CRUD 4개 view (list/new/edit/_attachments-fragment)
 * 는 expression 이 많아 렌더 단계에서 깨질 여지가 큼 → 정적/동적 검증의 다리 역할 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminNoticeFormRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;
  @Autowired NoticeRepository noticeRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor sysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor centerAdmin() {
    User u = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  // ================= 목록 =================

  @Test
  void GET_admin_notices_list_렌더_시드_12건_이상() throws Exception {
    String body =
        mockMvc
            .perform(get("/admin/notices").with(sysadmin()))
            .andExpect(status().isOk())
            // 페이지 헤더
            .andExpect(content().string(containsString("공지 관리")))
            .andExpect(content().string(containsString("+ 신규 등록")))
            // 리스트 헤드 컬럼
            .andExpect(content().string(containsString("admin-notice-row--head")))
            .andExpect(content().string(containsString(">번호<")))
            .andExpect(content().string(containsString(">분류<")))
            .andExpect(content().string(containsString(">제목<")))
            .andExpect(content().string(containsString(">작성자<")))
            .andExpect(content().string(containsString(">등록일<")))
            // 시드 공지 제목 몇 건
            .andExpect(content().string(containsString("제1회 청년의 날 축제 안내")))
            .andExpect(content().string(containsString("2026년 상반기 청년센터 운영 방침")))
            // GNB active
            .andExpect(content().string(containsString("admin-nav-link active")))
            // Thymeleaf 표현식 잔존 없음
            .andExpect(content().string(not(containsString("${"))))
            .andExpect(content().string(not(containsString("th:each"))))
            .andReturn()
            .getResponse()
            .getContentAsString();

    // 시드 12건 이상 렌더 (한 페이지 20건이므로 전부 노출)
    int rowMatches = body.split("admin-notice-title-link").length - 1;
    org.junit.jupiter.api.Assertions.assertTrue(
        rowMatches >= 12, "시드 공지 12건 이상이 렌더되어야 함. 실측=" + rowMatches);
  }

  // ================= 신규 =================

  @Test
  void GET_admin_notices_new_렌더_신규모드_첨부섹션_미노출() throws Exception {
    mockMvc
        .perform(get("/admin/notices/new").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("공지 등록")))
        .andExpect(content().string(containsString("name=\"title\"")))
        .andExpect(content().string(containsString("name=\"content\"")))
        .andExpect(content().string(containsString("name=\"category\"")))
        .andExpect(content().string(containsString("name=\"isPinned\"")))
        // 신규 모드: 첨부 wrapper 미노출
        .andExpect(content().string(not(containsString("admin-notice-attachments-wrapper"))))
        // submit 버튼 "등록"
        .andExpect(content().string(containsString(">등록<")))
        // Thymeleaf 잔존 없음
        .andExpect(content().string(not(containsString("${"))));
  }

  // ================= 편집 =================

  @Test
  void GET_admin_notices_edit_렌더_편집모드_prefilled_첨부섹션_노출() throws Exception {
    Notice first = noticeRepository.findAll().stream().findFirst().orElseThrow();
    Long id = first.getId();

    mockMvc
        .perform(get("/admin/notices/" + id).with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("공지 편집")))
        // prefilled title (시드 제목 중 하나)
        .andExpect(content().string(containsString(first.getTitle())))
        // 편집 모드: 첨부 섹션 노출
        .andExpect(content().string(containsString("admin-notice-attachments-wrapper")))
        .andExpect(content().string(containsString("id=\"admin-notice-attachments\"")))
        // 업로드 폼 노출
        .andExpect(content().string(containsString("admin-notice-upload-form")))
        // 삭제 confirm 모달 markup
        .andExpect(content().string(containsString("notice-delete-modal")))
        // 삭제 버튼 (canEdit true — sysadmin)
        .andExpect(content().string(containsString("admin-btn--danger")))
        // submit "수정 저장"
        .andExpect(content().string(containsString(">수정 저장<")))
        // Thymeleaf 잔존 없음
        .andExpect(content().string(not(containsString("${"))));
  }

  @Test
  void GET_admin_notices_edit_없는id_404() throws Exception {
    mockMvc.perform(get("/admin/notices/999999").with(sysadmin())).andExpect(status().isNotFound());
  }

  // ================= RBAC (Controller · Service 통합 매트릭스) =================

  @Test
  void GET_edit_centerAdmin_타인공지_200_렌더되지만_canEdit_false_배너() throws Exception {
    // 시드 notice 는 sysadmin 이 작성 → centerAdmin 은 편집 불가 배너를 봐야 함
    Notice first = noticeRepository.findAll().stream().findFirst().orElseThrow();
    mockMvc
        .perform(get("/admin/notices/" + first.getId()).with(centerAdmin()))
        .andExpect(status().isOk())
        // 편집 불가 배너
        .andExpect(content().string(containsString("admin-notice-forbidden-banner")))
        .andExpect(content().string(containsString("다른 관리자가 작성한 항목이라")))
        // 삭제 confirm 모달 markup 미노출 (canEdit false)
        .andExpect(content().string(not(containsString("id=\"notice-delete-modal\""))));
  }

  @Test
  void POST_create_centerAdmin_200_302_redirect_후_자기공지_편집_가능() throws Exception {
    // CENTER_ADMIN 은 신규 작성 가능 (RBAC 매트릭스: Create ✅)
    mockMvc
        .perform(
            post("/admin/notices")
                .with(centerAdmin())
                .with(csrf())
                .param("title", "센터관리자 신규 공지 QA")
                .param("content", "본문 내용")
                .param("category", "NOTICE"))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            header().string("Location", org.hamcrest.Matchers.startsWith("/admin/notices/")));
  }

  @Test
  void POST_update_centerAdmin_타인공지_403() throws Exception {
    // centerAdmin 이 sysadmin 소유 공지 편집 시도 → AccessDeniedException → 403
    Notice first = noticeRepository.findAll().stream().findFirst().orElseThrow();
    mockMvc
        .perform(
            post("/admin/notices/" + first.getId())
                .with(centerAdmin())
                .with(csrf())
                .param("title", "hacked")
                .param("content", "hacked"))
        .andExpect(status().isForbidden());
  }

  @Test
  void POST_delete_centerAdmin_타인공지_403() throws Exception {
    Notice first = noticeRepository.findAll().stream().findFirst().orElseThrow();
    mockMvc
        .perform(
            post("/admin/notices/" + first.getId() + "/delete").with(centerAdmin()).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void GET_admin_notices_익명_login_리다이렉트() throws Exception {
    mockMvc.perform(get("/admin/notices")).andExpect(status().is3xxRedirection());
  }
}
