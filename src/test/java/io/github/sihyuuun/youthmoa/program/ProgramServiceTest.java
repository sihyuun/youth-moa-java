package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase
@Import({JpaConfig.class, ProgramService.class})
class ProgramServiceTest {

    @Autowired ProgramService programService;
    @Autowired ProgramRepository programRepository;

    private Program persistSample() {
        return programRepository.save(Program.builder()
                .title("취업 워크숍").organization("내일스퀘어")
                .category("취업").region("수원시")
                .content("8주 부트캠프 과정")
                .requirements("만 19~39세 청년")
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(20))
                .capacity(30).build());
    }

    @Test
    @DisplayName("findById는 존재하는 ID로 프로그램을 반환한다")
    void findById_found() {
        Program saved = persistSample();

        Program found = programService.findById(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getTitle()).isEqualTo("취업 워크숍");
        assertThat(found.getOrganization()).isEqualTo("내일스퀘어");
        assertThat(found.getCapacity()).isEqualTo(30);
    }

    @Test
    @DisplayName("findById는 존재하지 않는 ID에 IllegalArgumentException 을 던진다")
    void findById_notFound() {
        assertThatThrownBy(() -> programService.findById(999_999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999999");
    }

    @Test
    @DisplayName("findById로 가져온 Program 의 도메인 메서드(getStatus / getDdayLabel) 가 정상 동작한다")
    void findById_domainMethods() {
        Program saved = persistSample();

        Program found = programService.findById(saved.getId());

        assertThat(found.getStatus()).isEqualTo(ProgramStatus.ACTIVE);
        assertThat(found.getDaysUntilDeadline()).isEqualTo(20L);
        assertThat(found.getDdayLabel()).isEqualTo("D-20");
    }
}
