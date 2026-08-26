package io.github.sihyuuun.youthmoa.search;

import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.notice.NoticeRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.ProgramSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 검색 서비스. 프로그램(제목·기관·지역·본문) + 공지(제목·본문)를 각각 페이징 조회한다.
 *
 * <p>q 가 null/blank 이면 빈 결과 반환. q 100자 초과 시 앞 100자만 사용(UX 안전장치).
 *
 * <p>@Transactional(readOnly=true) — lazy 관계 확장 대비 readOnly 트랜잭션 명시. 260826
 * chore/content-lob-to-text: content 는 이제 @JdbcTypeCode(LONGVARCHAR) 매핑이라 @Lob 스트리밍 트랜잭션 요구는 사라졌지만
 * 다른 fetch 사고 방어 목적으로 유지.
 */
@Service
@RequiredArgsConstructor
public class SearchService {

  private static final int PROGRAM_PAGE_SIZE = 12;
  private static final int NOTICE_PAGE_SIZE = 10;
  private static final int MAX_QUERY_LENGTH = 100;

  private final ProgramRepository programRepository;
  private final NoticeRepository noticeRepository;

  @Transactional(readOnly = true)
  public SearchResult search(String rawQuery, int programPage, int noticePage) {
    if (rawQuery == null || rawQuery.isBlank()) {
      Page<Program> emptyPrograms = new PageImpl<>(java.util.List.of());
      Page<Notice> emptyNotices = new PageImpl<>(java.util.List.of());
      return new SearchResult("", emptyPrograms, emptyNotices);
    }
    String q = rawQuery.trim();
    if (q.length() > MAX_QUERY_LENGTH) {
      q = q.substring(0, MAX_QUERY_LENGTH);
    }

    Specification<Program> spec = ProgramSpec.withKeyword(q).and(ProgramSpec.isActive());
    Page<Program> programs =
        programRepository.findAll(
            spec,
            PageRequest.of(
                Math.max(programPage, 0),
                PROGRAM_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")));

    // 260826 chore/content-lob-to-text: content 는 이제 text 매핑이라 원문 검색 가능. summary 우회 폐기.
    Page<Notice> notices =
        noticeRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            q,
            q,
            PageRequest.of(
                Math.max(noticePage, 0),
                NOTICE_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")));

    return new SearchResult(q, programs, notices);
  }
}
