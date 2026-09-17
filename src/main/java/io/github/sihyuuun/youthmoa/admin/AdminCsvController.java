package io.github.sihyuuun.youthmoa.admin;

import com.opencsv.CSVWriter;
import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A8 admin-bulk-csv (2026-09-17 · Qn-QC + Qn-CSV1/2/3 A): CSV export 엔드포인트.
 *
 * <ul>
 *   <li>P-CSV-1 파일명 = {@code {domain}_{yyyyMMdd_HHmmss}.csv} (KST)
 *   <li>P-CSV-2 UTF-8 + BOM (Excel 한글 호환)
 *   <li>P-CSV-3 CRLF 라인 종결 (opencsv 기본 = CRLF)
 *   <li>Qn-CSV1 상단(필터 전체) · 하단(선택 건) 이중 진입 지원 — {@code ids} 파라미터 여부로 분기
 *   <li>Qn-CSV2 Users CSV = SYSTEM_ADMIN 전용 (개인정보)
 *   <li>Qn-7/CSV3 Content-Disposition = attachment (RFC 5987 병용)
 * </ul>
 *
 * <p>opencsv CSVWriter 저수준 사용 (Qn-Δ A): 리플렉션 없이 컬럼 순서·escape 를 명시적으로 제어. BOM 은 응답 스트림에 직접 write.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminCsvController {

  /** UTF-8 BOM (0xEF 0xBB 0xBF). Excel 한글 호환 필수. */
  private static final char[] BOM = {'﻿'};

  private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  private final UserRepository userRepository;
  private final ProgramRepository programRepository;
  private final ApplicationRepository applicationRepository;
  private final AdminScope adminScope;

  // ============ Users CSV ============

  @GetMapping("/admin/users/export.csv")
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  @Transactional(readOnly = true)
  public void exportUsers(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) String ids,
      HttpServletResponse response)
      throws IOException {
    List<User> users = resolveUsers(q, role, ids);
    writeCsv(
        response,
        "users",
        new String[] {
          "id", "email", "name", "phone", "role", "isActive", "lastAccessAt", "createdAt"
        },
        users,
        u ->
            new String[] {
              String.valueOf(u.getId()),
              nvl(u.getEmail()),
              nvl(u.getName()),
              nvl(u.getPhone()),
              u.getRole() == null ? "" : u.getRole().name(),
              String.valueOf(u.isActive()),
              u.getLastAccessAt() == null ? "" : u.getLastAccessAt().toString(),
              u.getCreatedAt() == null ? "" : u.getCreatedAt().toString()
            });
  }

  private List<User> resolveUsers(String q, String role, String ids) {
    Set<Long> idSet = parseIds(ids);
    if (!idSet.isEmpty()) {
      return userRepository.findAllById(idSet);
    }
    Specification<User> spec = (root, query, cb) -> cb.conjunction();
    if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
      try {
        UserRole r = UserRole.valueOf(role.toUpperCase());
        spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), r));
      } catch (IllegalArgumentException ignore) {
        // 무시
      }
    }
    if (q != null && !q.isBlank()) {
      String pattern = "%" + q.trim().toLowerCase() + "%";
      spec =
          spec.and(
              (root, query, cb) -> {
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate emailLike = cb.like(cb.lower(root.get("email")), pattern);
                return cb.or(nameLike, emailLike);
              });
    }
    return userRepository.findAll(spec);
  }

  // ============ Programs CSV ============

  @GetMapping("/admin/programs/export.csv")
  @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CENTER_ADMIN')")
  @Transactional(readOnly = true)
  public void exportPrograms(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String ids,
      HttpServletResponse response)
      throws IOException {
    List<Program> programs = resolvePrograms(q, status, ids);
    writeCsv(
        response,
        "programs",
        new String[] {
          "id",
          "title",
          "organization",
          "category",
          "applyStartDate",
          "applyEndDate",
          "startDate",
          "endDate",
          "capacity",
          "isActive",
          "status",
          "createdAt"
        },
        programs,
        p ->
            new String[] {
              String.valueOf(p.getId()),
              nvl(p.getTitle()),
              nvl(p.getOrganization()),
              nvl(p.getCategory()),
              p.getApplyStartDate() == null ? "" : p.getApplyStartDate().toString(),
              p.getApplyEndDate() == null ? "" : p.getApplyEndDate().toString(),
              p.getStartDate() == null ? "" : p.getStartDate().toString(),
              p.getEndDate() == null ? "" : p.getEndDate().toString(),
              p.getCapacity() == null ? "" : String.valueOf(p.getCapacity()),
              String.valueOf(p.isActive()),
              p.getStatus() == null ? "" : p.getStatus().name(),
              p.getCreatedAt() == null ? "" : p.getCreatedAt().toString()
            });
  }

  private List<Program> resolvePrograms(String q, String status, String ids) {
    String scope = adminScope.effectiveCenterName();
    Set<Long> idSet = parseIds(ids);
    if (!idSet.isEmpty()) {
      List<Program> found = programRepository.findAllById(idSet);
      if (scope != null) {
        return found.stream().filter(p -> scope.equals(p.getOrganization())).toList();
      }
      return found;
    }
    Specification<Program> spec = (root, query, cb) -> cb.conjunction();
    if (scope != null) {
      final String s = scope;
      spec = spec.and((root, query, cb) -> cb.equal(root.get("organization"), s));
    }
    if (q != null && !q.isBlank()) {
      String pattern = "%" + q.trim().toLowerCase() + "%";
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.or(
                      cb.like(cb.lower(root.get("title")), pattern),
                      cb.like(cb.lower(root.get("organization")), pattern)));
    }
    // status 는 파생 필드라 여기서는 isActive 로 근사 (SUSPENDED 만) — 나머지 상태는 런타임 파생
    if ("SUSPENDED".equalsIgnoreCase(status)) {
      spec = spec.and((root, query, cb) -> cb.isFalse(root.get("isActive")));
    }
    return programRepository.findAll(spec);
  }

  // ============ Applications CSV ============

  @GetMapping("/admin/programs/{programId}/applications/export.csv")
  @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CENTER_ADMIN')")
  @Transactional(readOnly = true)
  public void exportApplications(
      @PathVariable Long programId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String ids,
      HttpServletResponse response)
      throws IOException {
    Program program =
        programRepository
            .findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("프로그램을 찾을 수 없어요: " + programId));
    String scope = adminScope.effectiveCenterName();
    if (scope != null && !scope.equals(program.getOrganization())) {
      throw new IllegalStateException("자신의 센터 프로그램만 조회할 수 있어요.");
    }
    List<Application> apps = resolveApplications(programId, status, q, ids);

    writeCsv(
        response,
        "applications_p" + programId,
        new String[] {
          "id",
          "applicantEmail",
          "applicantName",
          "phone",
          "status",
          "appliedAt",
          "processedAt",
          "processedBy",
          "rejectReason",
          "adminNote"
        },
        apps,
        a ->
            new String[] {
              String.valueOf(a.getId()),
              a.getUser() == null ? "" : nvl(a.getUser().getEmail()),
              a.getUser() == null ? "" : nvl(a.getUser().getName()),
              a.getUser() == null ? "" : nvl(a.getUser().getPhone()),
              a.getStatus() == null ? "" : a.getStatus().name(),
              a.getAppliedAt() == null ? "" : a.getAppliedAt().toString(),
              a.getProcessedAt() == null ? "" : a.getProcessedAt().toString(),
              a.getProcessedBy() == null ? "" : nvl(a.getProcessedBy().getEmail()),
              nvl(a.getRejectReason()),
              nvl(a.getAdminNote())
            });
  }

  private List<Application> resolveApplications(
      Long programId, String status, String q, String ids) {
    Set<Long> idSet = parseIds(ids);
    Specification<Application> spec =
        (root, query, cb) -> {
          if (query != null && Long.class != query.getResultType()) {
            root.fetch("user");
          }
          return cb.equal(root.get("program").get("id"), programId);
        };
    if (!idSet.isEmpty()) {
      final Set<Long> f = idSet;
      spec = spec.and((root, query, cb) -> root.get("id").in(f));
    } else {
      if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
        try {
          ApplicationStatus s = ApplicationStatus.valueOf(status.toUpperCase());
          spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), s));
        } catch (IllegalArgumentException ignore) {
          // 무시
        }
      }
      if (q != null && !q.isBlank()) {
        String pattern = "%" + q.trim().toLowerCase() + "%";
        spec =
            spec.and(
                (root, query, cb) ->
                    cb.or(
                        cb.like(cb.lower(root.get("user").get("name")), pattern),
                        cb.like(cb.lower(root.get("user").get("email")), pattern)));
      }
    }
    return applicationRepository.findAll(spec);
  }

  // ============ 공통 write ============

  private <T> void writeCsv(
      HttpServletResponse response,
      String domain,
      String[] header,
      List<T> rows,
      RowMapper<T> mapper)
      throws IOException {
    String filename = domain + "_" + LocalDateTime.now().format(FILE_TS) + ".csv";
    response.setContentType("text/csv; charset=UTF-8");
    // RFC 5987 filename* 병용 — Excel/브라우저 한글 파일명 호환
    String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    response.setHeader(
        "Content-Disposition",
        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);

    try (Writer w = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
      // BOM prefix
      w.write(BOM);
      try (CSVWriter csv = new CSVWriter(w)) {
        csv.writeNext(header);
        int count = 0;
        for (T row : rows) {
          csv.writeNext(mapper.map(row));
          count++;
        }
        log.info("[admin-csv-export] domain={} rows={} filename={}", domain, count, filename);
      }
    }
  }

  private static String nvl(String v) {
    return v == null ? "" : v;
  }

  private static Set<Long> parseIds(String ids) {
    Set<Long> set = new HashSet<>();
    if (ids == null || ids.isBlank()) return set;
    for (String piece : ids.split(",")) {
      String trimmed = piece.trim();
      if (trimmed.isEmpty()) continue;
      try {
        set.add(Long.parseLong(trimmed));
      } catch (NumberFormatException ignore) {
        // 잘못된 조각은 무시
      }
    }
    return set;
  }

  @FunctionalInterface
  private interface RowMapper<T> {
    String[] map(T row);
  }

  @SuppressWarnings("unused")
  private static String[] concat(String[] a, String[] b) {
    String[] r = Arrays.copyOf(a, a.length + b.length);
    System.arraycopy(b, 0, r, a.length, b.length);
    return r;
  }
}
