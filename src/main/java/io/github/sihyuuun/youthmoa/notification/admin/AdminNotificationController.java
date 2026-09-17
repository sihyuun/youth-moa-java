package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.notification.Notification;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * A7 admin 헤더 알림 벨 (2026-09-17): admin 전용 알림 endpoint.
 *
 * <p>사용자 트랙 {@link io.github.sihyuuun.youthmoa.notification.NotificationController} 와 완전 분리. 두
 * 컨트롤러는 URL prefix (`/admin/notifications/**` vs `/notifications/**`) 와 role 이 다르며 서로의 응답 형식·swap
 * OOB 도 겹치지 않는다.
 *
 * <h2>endpoints</h2>
 *
 * <ul>
 *   <li>{@code GET /admin/notifications/dropdown} — HTMX fragment (최근 5건 · 클릭 시 trigger)
 *   <li>{@code GET /admin/notifications/badge} — HTMX fragment (unread count · 30s polling target)
 *   <li>{@code POST /admin/notifications/{id}/read} — 개별 읽음 + HX-Redirect
 *   <li>{@code POST /admin/notifications/mark-all-read} — 모두 읽음 + fragment 재렌더
 *   <li>{@code POST /admin/notifications/{id}/delete} — 삭제 + OOB 배지 갱신
 * </ul>
 *
 * <h2>Security</h2>
 *
 * <p>클래스 레벨 {@code @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CENTER_ADMIN')")}. USER 접근 시 403. P0-2
 * SecurityConfig 의 {@code /admin/**} 매처 + 여기 prePost 로 이중 방어.
 */
@Controller
@RequestMapping("/admin/notifications")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'CENTER_ADMIN')")
@RequiredArgsConstructor
public class AdminNotificationController {

  private final AdminNotificationService adminNotificationService;

  /** 헤더 벨 클릭 시 최근 5건 fragment 반환. */
  @GetMapping("/dropdown")
  public String dropdown(@AuthenticationPrincipal UserPrincipal principal, Model model) {
    List<Notification> items = adminNotificationService.recentForHeader(principal.getId());
    long unread = adminNotificationService.unreadCount(principal.getId());
    model.addAttribute("adminNotifications", items);
    model.addAttribute("adminUnreadCount", unread);
    return "admin/fragments/_notification-dropdown :: dropdown";
  }

  /** 30s polling target — 배지 fragment 만 반환. */
  @GetMapping("/badge")
  public String badge(@AuthenticationPrincipal UserPrincipal principal, Model model) {
    long unread = adminNotificationService.unreadCount(principal.getId());
    model.addAttribute("adminUnreadCount", unread);
    return "admin/fragments/_notification-badge :: badge";
  }

  /**
   * 개별 읽음 처리 → link 이동. HTMX 요청은 200 OK + HX-Redirect 헤더, 일반 요청은 302 리다이렉트.
   *
   * <p>사용자 트랙과 달리 OOB fragment 는 반환하지 않는다 — admin 은 클릭 즉시 항상 링크 이동(A4/A5)이 발생하는 UX 라 페이지 재로드로 배지가
   * 자연스럽게 갱신되기 때문.
   */
  @PostMapping("/{id}/read")
  @ResponseBody
  public ResponseEntity<String> read(
      @PathVariable Long id,
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
    Notification n = adminNotificationService.markAsRead(id, principal.getId());
    String link = n.getLink() != null ? n.getLink() : "/admin";
    if (hxRequest == null) {
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(link)).build();
    }
    // HTMX 2.0: 빈 본문 200 OK + HX-Redirect 헤더로 클라이언트 이동. 204 는 swap 스킵되므로 금지.
    return ResponseEntity.ok()
        .header("Content-Type", "text/html; charset=UTF-8")
        .header("HX-Redirect", link)
        .body("");
  }

  /** 모두 읽음. HTMX 는 드롭다운 fragment 재렌더 반환, 일반 요청은 302 /admin 로 리다이렉트. */
  @PostMapping("/mark-all-read")
  public String markAllRead(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestHeader(value = "HX-Request", required = false) String hxRequest,
      Model model) {
    adminNotificationService.markAllAsRead(principal.getId());
    if (hxRequest == null) {
      return "redirect:/admin";
    }
    List<Notification> items = adminNotificationService.recentForHeader(principal.getId());
    long unread = adminNotificationService.unreadCount(principal.getId());
    model.addAttribute("adminNotifications", items);
    model.addAttribute("adminUnreadCount", unread);
    return "admin/fragments/_notification-dropdown :: dropdown";
  }

  /** 개별 삭제. HTMX 요청은 200 OK + OOB 배지 갱신 응답. HTMX 2.0 의 204 스킵 이슈를 피하려 200 유지. */
  @PostMapping("/{id}/delete")
  @ResponseBody
  public ResponseEntity<String> deleteOne(
      @PathVariable Long id,
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
    adminNotificationService.delete(id, principal.getId());
    if (hxRequest == null) {
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/admin")).build();
    }
    long unread = adminNotificationService.unreadCount(principal.getId());
    return ResponseEntity.ok()
        .header("Content-Type", "text/html; charset=UTF-8")
        .body(buildBadgeOob(unread));
  }

  /** OOB 배지 갱신 HTML. 대상: {@code #admin-notif-badge}. Qn-6: unread=0 이면 hidden 속성으로 완전 숨김. */
  private String buildBadgeOob(long unread) {
    String hiddenAttr = unread > 0 ? "" : " hidden";
    return String.format(
        "<span id=\"admin-notif-badge\" hx-swap-oob=\"outerHTML\""
            + " class=\"admin-notif-badge\" data-notif-badge=\"%d\"%s>%d</span>",
        unread, hiddenAttr, unread);
  }
}
