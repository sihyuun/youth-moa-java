package io.github.sihyuuun.youthmoa.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * A1: /admin 대시보드 실 렌더 회귀 방어.
 *
 * <p>SYSTEM_ADMIN / CENTER_ADMIN 두 조건 각각 200 + 필수 마크업. principal 은 실제 시드된 User 를 조회해 감싼
 * UserPrincipal 을 주입한다 (AdminScope 가 DB 조회로 center 를 확인하므로).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminDashboardRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  @Test
  void SYSTEM_ADMIN_로_대시보드_렌더() throws Exception {
    User sys = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(sys);

    mockMvc
        .perform(get("/admin").with(user(principal)))
        .andExpect(status().isOk())
        // 다크 헤더 마크업
        .andExpect(content().string(containsString("admin-header")))
        .andExpect(content().string(containsString(">ADMIN<")))
        // SYSTEM_ADMIN 은 센터 스코프 라벨 = "전체"
        .andExpect(content().string(containsString("admin-header-scope--system")))
        .andExpect(content().string(containsString("전체")))
        // Welcome
        .andExpect(content().string(containsString("반가워요")))
        .andExpect(content().string(containsString("오늘도 청년모아 프로그램 운영을 함께합니다.")))
        // Stat cards 4종
        .andExpect(content().string(containsString("진행중 프로그램")))
        .andExpect(content().string(containsString("마감 프로그램")))
        .andExpect(content().string(containsString("진행 예정")))
        .andExpect(content().string(containsString("전체 회원")))
        // Recent + pending + urgent 섹션
        .andExpect(content().string(containsString("최근 프로그램 현황")))
        .andExpect(content().string(containsString("승인 대기")))
        .andExpect(content().string(containsString("마감 임박 프로그램")))
        .andExpect(content().string(containsString("D-7 이내")))
        // 로그아웃 form (admin logout url)
        .andExpect(content().string(containsString("action=\"/admin/logout\"")))
        // 사용자 페이지 링크
        .andExpect(content().string(containsString("사용자 페이지")))
        // Thymeleaf 표현식 잔존 검사
        .andExpect(content().string(not(containsString("${"))));
  }

  @Test
  @Transactional
  void CENTER_ADMIN_로_대시보드_렌더_센터명_노출() throws Exception {
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(center1);
    String centerName = center1.getCenter().getName();

    mockMvc
        .perform(get("/admin").with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-header-scope--center")))
        // 자기 센터명 (전체 아님)
        .andExpect(content().string(containsString(centerName)));
  }
}
