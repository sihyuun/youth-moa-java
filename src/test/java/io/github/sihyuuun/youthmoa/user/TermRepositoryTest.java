package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(JpaConfig.class)
class TermRepositoryTest {

  @Autowired TermRepository termRepository;

  @Test
  void findByIsActiveTrueOrderBySortOrderAsc_활성만_sortOrder_정렬() {
    termRepository.save(
        Term.builder()
            .code("A")
            .title("A")
            .contentPath("/a")
            .required(true)
            .version(1)
            .sortOrder(2)
            .isActive(true)
            .build());
    termRepository.save(
        Term.builder()
            .code("B")
            .title("B")
            .contentPath("/b")
            .required(true)
            .version(1)
            .sortOrder(1)
            .isActive(true)
            .build());
    // 비활성 — 결과에서 제외
    termRepository.save(
        Term.builder()
            .code("Z")
            .title("Z")
            .contentPath("/z")
            .required(true)
            .version(1)
            .sortOrder(0)
            .isActive(false)
            .build());

    List<Term> result = termRepository.findByIsActiveTrueOrderBySortOrderAsc();
    assertThat(result).extracting(Term::getCode).containsExactly("B", "A");
  }

  @Test
  void findByCode_UNIQUE_조회() {
    termRepository.save(
        Term.builder()
            .code("UNIQUE_ONE")
            .title("t")
            .contentPath("/x")
            .required(false)
            .version(3)
            .sortOrder(1)
            .isActive(true)
            .build());

    assertThat(termRepository.findByCode("UNIQUE_ONE")).isPresent();
    assertThat(termRepository.findByCode("MISSING")).isEmpty();
  }
}
