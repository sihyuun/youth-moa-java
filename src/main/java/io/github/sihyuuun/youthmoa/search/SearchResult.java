package io.github.sihyuuun.youthmoa.search;

import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.program.Program;
import org.springframework.data.domain.Page;

/**
 * 통합 검색 결과 DTO. 프로그램 페이지 + 공지 페이지 + 정규화된 쿼리 문자열을 함께 보관한다.
 *
 * <p>Java record — 불변 데이터 홀더. Lombok @Value 대비 정형화된 문법 + Java 표준 라이브러리 지원.
 */
public record SearchResult(String query, Page<Program> programs, Page<Notice> notices) {

  public long programCount() {
    return programs != null ? programs.getTotalElements() : 0L;
  }

  public long noticeCount() {
    return notices != null ? notices.getTotalElements() : 0L;
  }

  public long totalCount() {
    return programCount() + noticeCount();
  }
}
