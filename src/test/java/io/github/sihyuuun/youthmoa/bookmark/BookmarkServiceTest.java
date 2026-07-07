package io.github.sihyuuun.youthmoa.bookmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase
@Import({JpaConfig.class, BookmarkService.class})
class BookmarkServiceTest {

  @Autowired BookmarkService bookmarkService;
  @Autowired BookmarkRepository bookmarkRepository;
  @Autowired UserRepository userRepository;
  @Autowired ProgramRepository programRepository;

  private User user;
  private Program program;

  @BeforeEach
  void seed() {
    user =
        userRepository.save(
            User.builder()
                .email("bookmark@test.com")
                .password("hashed")
                .name("즐겨찾기 유저")
                .role(UserRole.USER)
                .build());

    program =
        programRepository.save(
            Program.builder()
                .title("샘플 프로그램")
                .organization("내일스퀘어")
                .category("취업")
                .region("수원시")
                .content("c")
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(10))
                .capacity(30)
                .build());
  }

  @Test
  @DisplayName("첫 토글은 즐겨찾기 추가 → true 반환")
  void toggle_first_adds() {
    boolean result = bookmarkService.toggle(user.getEmail(), program.getId());

    assertThat(result).isTrue();
    assertThat(bookmarkRepository.count()).isEqualTo(1);
    assertThat(bookmarkRepository.existsByUserAndProgram(user, program)).isTrue();
  }

  @Test
  @DisplayName("두 번째 토글은 즐겨찾기 해제 → false 반환")
  void toggle_second_removes() {
    bookmarkService.toggle(user.getEmail(), program.getId());

    boolean result = bookmarkService.toggle(user.getEmail(), program.getId());

    assertThat(result).isFalse();
    assertThat(bookmarkRepository.count()).isEqualTo(0);
  }

  @Test
  @DisplayName("연속 토글 3번 → 최종 추가 상태")
  void toggle_three_times() {
    bookmarkService.toggle(user.getEmail(), program.getId()); // add
    bookmarkService.toggle(user.getEmail(), program.getId()); // remove
    boolean result = bookmarkService.toggle(user.getEmail(), program.getId()); // add

    assertThat(result).isTrue();
    assertThat(bookmarkRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("isBookmarked — 즐겨찾기 추가 후 true")
  void isBookmarked_after_add() {
    bookmarkService.toggle(user.getEmail(), program.getId());

    assertThat(bookmarkService.isBookmarked(user.getEmail(), program.getId())).isTrue();
  }

  @Test
  @DisplayName("isBookmarked — 즐겨찾기 없으면 false")
  void isBookmarked_default_false() {
    assertThat(bookmarkService.isBookmarked(user.getEmail(), program.getId())).isFalse();
  }

  @Test
  @DisplayName("isBookmarked — userEmail 이 null 이면 false (비인증)")
  void isBookmarked_null_user() {
    assertThat(bookmarkService.isBookmarked(null, program.getId())).isFalse();
  }

  @Test
  @DisplayName("isBookmarked — 존재하지 않는 사용자/프로그램이면 false")
  void isBookmarked_unknown() {
    assertThat(bookmarkService.isBookmarked("ghost@nowhere.com", program.getId())).isFalse();
    assertThat(bookmarkService.isBookmarked(user.getEmail(), 999_999L)).isFalse();
  }

  @Test
  @DisplayName("toggle — 존재하지 않는 프로그램 → IllegalArgumentException")
  void toggle_program_notFound() {
    assertThatThrownBy(() -> bookmarkService.toggle(user.getEmail(), 999_999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("프로그램을 찾을 수 없습니다");
  }

  @Test
  @DisplayName("toggle — 존재하지 않는 사용자 → IllegalArgumentException")
  void toggle_user_notFound() {
    assertThatThrownBy(() -> bookmarkService.toggle("ghost@nowhere.com", program.getId()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("사용자를 찾을 수 없습니다");
  }

  @Test
  @DisplayName("getBookmarkedProgramIds — 즐겨찾기 N개 추가 후 정확한 ID Set 반환")
  void getBookmarkedProgramIds_returnsAll() {
    Program program2 =
        programRepository.save(
            Program.builder()
                .title("샘플 2")
                .organization("내일스퀘어")
                .category("취업")
                .region("수원시")
                .content("c")
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusDays(10))
                .build());
    Program program3 =
        programRepository.save(
            Program.builder()
                .title("샘플 3")
                .organization("내일스퀘어")
                .category("교육")
                .region("부천시")
                .content("c")
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusDays(10))
                .build());

    bookmarkService.toggle(user.getEmail(), program.getId());
    bookmarkService.toggle(user.getEmail(), program3.getId());
    // program2 는 즐겨찾기 안 함

    assertThat(bookmarkService.getBookmarkedProgramIds(user.getEmail()))
        .containsExactlyInAnyOrder(program.getId(), program3.getId())
        .doesNotContain(program2.getId());
  }

  @Test
  @DisplayName("getBookmarkedProgramIds — userEmail 이 null 이면 빈 Set (비인증)")
  void getBookmarkedProgramIds_null_user() {
    assertThat(bookmarkService.getBookmarkedProgramIds(null)).isEmpty();
  }

  @Test
  @DisplayName("getBookmarkedProgramIds — 존재하지 않는 사용자도 빈 Set (예외 던지지 않음)")
  void getBookmarkedProgramIds_unknown_user() {
    assertThat(bookmarkService.getBookmarkedProgramIds("ghost@nowhere.com")).isEmpty();
  }
}
