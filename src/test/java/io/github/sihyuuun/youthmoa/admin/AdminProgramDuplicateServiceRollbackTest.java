package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import io.github.sihyuuun.youthmoa.program.Course;
import io.github.sihyuuun.youthmoa.program.CourseRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * FOLLOW-program-duplicate 트랜잭션 롤백 (ym-verify U2 보강): {@link AdminProgramService#duplicate(Long)} 가
 * {@code @Transactional} 로 선언돼 있어도 실제로 Course 저장 중 예외가 발생했을 때 Program row 가 함께 롤백되는지는 어노테이션
 * 정적 분석만으로는 보장되지 않는다. {@link CourseRepository} 를 {@link MockitoBean} 으로 교체해 save 시 예외를 던지도록 하고,
 * 호출 후 신규 Program row 가 DB 에 남아 있지 않음을 외부 트랜잭션에서 조회해 입증한다.
 *
 * <p>이 테스트 클래스는 Service 호출을 테스트 트랜잭션 바깥에서 수행하기 위해 {@code @Transactional} 을 사용하지 않는다.
 * Service 내부의 {@code @Transactional} 이 자신의 트랜잭션을 개시 → 예외로 롤백 → 외부 조회에서 미존재 확인.
 */
@SpringBootTest
@ActiveProfiles("e2e")
class AdminProgramDuplicateServiceRollbackTest {

  private static final long SEED_PROGRAM_ID = 1L;

  @Autowired AdminProgramService adminProgramService;
  @Autowired ProgramRepository programRepository;
  @Autowired UserRepository userRepository;

  /**
   * CourseRepository 전체를 mock 으로 교체. 복제 대상 원본의 활성 Course 조회에는 비어 있는 리스트를 반환할 수도 있으나,
   * 복제 로직이 Course.save 를 호출하기 전에 단 1건이라도 활성 Course 가 있어야 예외 경로가 발동되므로 조회는 Mockito 기본값
   * (빈 리스트)로 두되, 테스트 안에서 원본에 Course 1건을 미리 등록한다. (실 Repository 로 저장했으므로 DB 에는 존재하지만
   * mock CourseRepository 의 findBy… 는 빈 리스트 반환 → save 로 들어가는 흐름을 유도하기 위해 mock stubbing 재지정)
   */
  @MockitoBean CourseRepository courseRepository;

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

  @Test
  void duplicate_courseSaveFails_entireTransactionRollback() {
    loginAsSysadmin();
    // 원본은 SEED_PROGRAM_ID 사용. 복제 로직은 원본의 활성 Course 를 조회해 복제본으로 save 하는데
    // mock 이 활성 Course 1건을 반환하도록 stubbing → Course.save 호출 시 예외 → 전체 롤백.
    org.mockito.Mockito.when(
            courseRepository.findByProgramIdAndIsActiveTrueOrderBySortOrderAscIdAsc(SEED_PROGRAM_ID))
        .thenReturn(
            List.of(
                Course.builder()
                    .program(programRepository.findById(SEED_PROGRAM_ID).orElseThrow())
                    .name("강좌-롤백유도")
                    .sortOrder(1)
                    .isActive(true)
                    .build()));
    doThrow(new RuntimeException("intentional save failure (rollback test)"))
        .when(courseRepository)
        .save(any(Course.class));

    long countBefore = programRepository.count();

    assertThatThrownBy(() -> adminProgramService.duplicate(SEED_PROGRAM_ID))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("intentional save failure");

    long countAfter = programRepository.count();

    // Program 저장 → Course.save 예외 → Service @Transactional 롤백 → Program row 미존재.
    // 롤백이 Program 에 미치지 않았다면 countAfter == countBefore + 1 로 실패했을 것.
    assertThat(countAfter)
        .as("Course save 예외 발생 시 Program 도 함께 롤백돼야 함")
        .isEqualTo(countBefore);
  }
}
