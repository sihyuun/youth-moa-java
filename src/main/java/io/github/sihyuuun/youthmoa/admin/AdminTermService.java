package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.user.Term;
import io.github.sihyuuun.youthmoa.user.TermRepository;
import io.github.sihyuuun.youthmoa.user.UserAgreementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A-admin-terms-crud (2026-09-04 · Qn-1 B · Qn-2 B · Qn-3~9 A): 관리자 약관 CRUD 서비스.
 *
 * <p>RBAC (Qn-1 B):
 *
 * <ul>
 *   <li>Read (list · findById) — SYSTEM_ADMIN · CENTER_ADMIN 모두
 *   <li>Create · Update · Delete — SYSTEM_ADMIN 만. 컨트롤러의 {@code @PreAuthorize} 로 게이트되고, 서비스는 role
 *       재검증 없이 신뢰한다 ({@link AdminExceptionHandler} 가 {@link
 *       org.springframework.security.access.AccessDeniedException} 을 처리하지 않으므로 Spring Security 기본
 *       403 매핑).
 * </ul>
 *
 * <p>XSS (Qn-2 B): content 는 저장 전 OWASP HTML sanitizer 로 정화. {@code <script>} · 이벤트 핸들러 등은 제거되고
 * 서식·링크·블록·스타일·테이블 태그만 남는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTermService {

  /**
   * Qn-2 B: 저장 전 정화 정책. FORMATTING(b/i/em/strong/...) + LINKS(a) +
   * BLOCKS(p/ul/ol/li/blockquote/...) + STYLES + TABLES 조합. {@code <script>} · onclick 등 능동적 요소는 전부
   * 제거된다.
   */
  public static final PolicyFactory HTML_POLICY =
      Sanitizers.FORMATTING
          .and(Sanitizers.LINKS)
          .and(Sanitizers.BLOCKS)
          .and(Sanitizers.STYLES)
          .and(Sanitizers.TABLES);

  private final TermRepository termRepository;
  private final UserAgreementRepository userAgreementRepository;

  // ================= Read =================

  @Transactional(readOnly = true)
  public List<Term> list() {
    return termRepository.findAllByOrderBySortOrderAscIdAsc();
  }

  @Transactional(readOnly = true)
  public Term findById(Long id) {
    return termRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 약관이에요: " + id));
  }

  /** 관리 목록 상단 배너 표시용 — 활성 필수 약관 개수. 0 이면 signup 폼에 약관 섹션이 노출되지 않아 사용자 안내가 필요. */
  @Transactional(readOnly = true)
  public long countActiveRequired() {
    return termRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
        .filter(Term::isRequired)
        .count();
  }

  // ================= Write =================

  @Transactional
  public Term create(
      String code,
      String title,
      String contentPath,
      String rawContent,
      boolean required,
      int sortOrder,
      boolean isActive) {
    validateCode(code);
    validateTitle(title);
    validateContentPath(contentPath);
    validateSortOrder(sortOrder);
    termRepository
        .findByCode(code.trim())
        .ifPresent(
            existing -> {
              throw new IllegalArgumentException(
                  "이미 존재하는 약관 코드에요: " + code.trim() + " (편집으로 진행해주세요)");
            });
    String safeContent = sanitize(rawContent);
    Term term =
        Term.builder()
            .code(code.trim())
            .title(title.trim())
            .contentPath(contentPath.trim())
            .content(safeContent)
            .required(required)
            .version(1)
            .sortOrder(sortOrder)
            .isActive(isActive)
            .build();
    Term saved = termRepository.save(term);
    log.info(
        "[admin-term] created id={} code={} version={} isActive={}",
        saved.getId(),
        saved.getCode(),
        saved.getVersion(),
        saved.isActive());
    return saved;
  }

  /** Qn-6 A: bumpVersion=true 일 때만 version++. Qn-9 A: code 는 편집 대상 아님. */
  @Transactional
  public Term update(
      Long id,
      String title,
      String contentPath,
      String rawContent,
      boolean required,
      int sortOrder,
      boolean isActive,
      boolean bumpVersion) {
    validateTitle(title);
    validateContentPath(contentPath);
    validateSortOrder(sortOrder);
    Term term = findById(id);
    String safeContent = sanitize(rawContent);
    int prevVersion = term.getVersion();
    term.updateContent(
        title.trim(), contentPath.trim(), safeContent, required, sortOrder, isActive, bumpVersion);
    log.info(
        "[admin-term] updated id={} code={} version={}→{} isActive={} bumpVersion={}",
        term.getId(),
        term.getCode(),
        prevVersion,
        term.getVersion(),
        term.isActive(),
        bumpVersion);
    return term;
  }

  /** Qn-3 A: hard delete. FK 참조(UserAgreement) 있으면 400 + 비활성 처리 안내. */
  @Transactional
  public void delete(Long id) {
    Term term = findById(id);
    long agreements = userAgreementRepository.countByTerm(term);
    if (agreements > 0) {
      throw new IllegalArgumentException(
          "이 약관은 회원 동의 이력이 " + agreements + "건 있어 삭제할 수 없어요. 비활성으로 처리해주세요.");
    }
    termRepository.delete(term);
    log.info("[admin-term] deleted id={} code={}", id, term.getCode());
  }

  // ================= 검증 & sanitize =================

  /**
   * OWASP HTML sanitizer 적용. null → 빈 문자열. sanitize 후 실제 텍스트가 남지 않으면 400 (관리자 실수 조기 감지). raw 원본이
   * 존재해도 정책이 모두 제거하는 케이스 (예: 오로지 &lt;script&gt; 태그만 있는 경우) 를 걸러낸다.
   */
  String sanitize(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("본문을 입력해주세요.");
    }
    String safe = HTML_POLICY.sanitize(raw);
    if (safe == null || safe.trim().isEmpty()) {
      throw new IllegalArgumentException("본문에서 렌더 가능한 내용이 없어요. HTML 태그를 확인해주세요.");
    }
    return safe;
  }

  private void validateCode(String code) {
    if (code == null || code.trim().isEmpty()) {
      throw new IllegalArgumentException("약관 코드를 입력해주세요.");
    }
    String trimmed = code.trim();
    if (trimmed.length() > 50) {
      throw new IllegalArgumentException("약관 코드는 50자 이하여야 합니다.");
    }
    if (!trimmed.matches("^[A-Z_]+$")) {
      throw new IllegalArgumentException("약관 코드는 영대문자와 밑줄(_) 만 사용할 수 있어요.");
    }
  }

  private void validateTitle(String title) {
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("제목을 입력해주세요.");
    }
    if (title.length() > 100) {
      throw new IllegalArgumentException("제목은 100자 이하여야 합니다.");
    }
  }

  private void validateContentPath(String contentPath) {
    if (contentPath == null || contentPath.trim().isEmpty()) {
      throw new IllegalArgumentException("본문 경로를 입력해주세요.");
    }
    String trimmed = contentPath.trim();
    if (trimmed.length() > 200) {
      throw new IllegalArgumentException("본문 경로는 200자 이하여야 합니다.");
    }
    if (!trimmed.startsWith("/")) {
      throw new IllegalArgumentException("본문 경로는 / 로 시작해야 합니다.");
    }
  }

  private void validateSortOrder(int sortOrder) {
    if (sortOrder < 1 || sortOrder > 999) {
      throw new IllegalArgumentException("정렬 순서는 1 이상 999 이하여야 합니다.");
    }
  }
}
