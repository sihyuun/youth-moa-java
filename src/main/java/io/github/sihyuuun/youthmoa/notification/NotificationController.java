package io.github.sihyuuun.youthmoa.notification;

import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 알림 페이지·읽음 처리 컨트롤러.
 *
 * <ul>
 *   <li>{@code GET /notifications} — 전체보기 stub 페이지
 *   <li>{@code POST /notifications/read-all} — HTMX, 드롭다운 fragment + OOB dot 갱신
 *   <li>{@code POST /notifications/{id}/read} — HTMX 개별 읽음 후 링크 리다이렉트
 * </ul>
 */
@Controller
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final UserRepository userRepository;

  @GetMapping("/notifications")
  public String list(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(name = "unread", required = false, defaultValue = "false") boolean unreadOnly,
      Model model) {
    var user =
        userRepository
            .findById(principal.getId())
            .orElseThrow(() -> new IllegalStateException("Authenticated user missing"));
    List<Notification> all = notificationService.listAll(user); // 하위 호환용 (최근 20건)
    long unreadCount = notificationService.unreadCount(user);
    long totalCount = notificationService.totalCount(user);
    Map<String, List<Notification>> grouped = notificationService.findGrouped(user, unreadOnly);
    model.addAttribute("notifications", all); // 하위 호환 (기존 렌더 테스트 대비)
    model.addAttribute("groupedNotifications", grouped);
    model.addAttribute("filterUnread", unreadOnly);
    model.addAttribute("totalCount", totalCount);
    model.addAttribute("unreadCount", unreadCount);
    return "notification/list";
  }

  /**
   * "모두 읽음" — HTMX 대상: 드롭다운 자체. OOB swap 으로 헤더 dot 도 동시 갱신.
   *
   * <p>fragments/notification-panel.html :: panel fragment 를 반환 → hx-swap="outerHTML" 로 교체.
   */
  @PostMapping("/notifications/read-all")
  public String readAll(
      @AuthenticationPrincipal UserPrincipal principal,
      @org.springframework.web.bind.annotation.RequestHeader(value = "HX-Request", required = false)
          String hxRequest,
      Model model) {
    notificationService.markAllAsRead(principal.getId());
    // 전체 페이지 폼 제출은 리다이렉트 (HX 요청은 fragment 로 응답 유지)
    if (hxRequest == null) {
      return "redirect:/notifications";
    }
    var user = userRepository.findById(principal.getId()).orElseThrow();
    model.addAttribute("headerRecentNotifications", notificationService.recentForHeader(user));
    model.addAttribute("headerUnreadCount", notificationService.unreadCount(user));
    return "fragments/notification-panel :: panel";
  }

  /**
   * 개별 읽음 처리. 아이템 body 클릭 시 hx-post 로 호출.
   *
   * <p>HTMX 요청: 200 OK + OOB spans (bell-dot · panel-badge · unread-badge · 클릭한 item unread 표시 제거).
   * 응답에 `HX-Redirect` 헤더로 link 이동 (있으면).
   */
  @PostMapping("/notifications/{id}/read")
  @org.springframework.web.bind.annotation.ResponseBody
  public org.springframework.http.ResponseEntity<String> read(
      @PathVariable Long id,
      @AuthenticationPrincipal UserPrincipal principal,
      @org.springframework.web.bind.annotation.RequestHeader(value = "HX-Request", required = false)
          String hxRequest) {
    Notification n = notificationService.markAsRead(id, principal.getId());
    if (hxRequest == null) {
      String link = n.getLink() != null ? n.getLink() : "/notifications";
      return org.springframework.http.ResponseEntity.status(
              org.springframework.http.HttpStatus.FOUND)
          .location(java.net.URI.create(link))
          .build();
    }
    var user = userRepository.findById(principal.getId()).orElseThrow();
    long unread = notificationService.unreadCount(user);
    // 읽음 처리된 아이템의 unread border/bg 제거 → id 기반 OOB 로 클래스만 갱신.
    // 아이템 전체 HTML 을 클라이언트가 알 필요는 없고, class attribute 만 바꾸면 됨.
    String itemOob = buildItemReadOob(id);
    String badgesOob = buildBadgeAndDotOob(unread);
    var builder =
        org.springframework.http.ResponseEntity.ok()
            .header("Content-Type", "text/html; charset=UTF-8");
    // 링크가 있으면 HX-Redirect 로 클라이언트 이동.
    if (n.getLink() != null) {
      builder.header("HX-Redirect", n.getLink());
    }
    return builder.body(itemOob + badgesOob);
  }

  /**
   * 개별 알림 삭제. prototype L1305 · L356 close(X) 정합.
   *
   * <p>HTMX 요청: 200 OK 응답 본문에 OOB span 3개 포함 → 프론트가 hx-swap="delete" 로 대상 아이템 제거 + OOB 로 뱃지·bell
   * dot 동시 갱신. 폼 제출(non-HX): 302 /notifications 리다이렉트.
   *
   * <p>주의: HTMX 2.0 은 204 응답 시 swap 을 스킵한다. 200 OK 필수. OOB 대상이 페이지에 없으면 HTMX 가 무시하므로 헤더 팝업/알림 목록 어느
   * 컨텍스트에서 호출하든 안전.
   */
  @PostMapping("/notifications/{id}/delete")
  @org.springframework.web.bind.annotation.ResponseBody
  public org.springframework.http.ResponseEntity<String> deleteOne(
      @PathVariable Long id,
      @AuthenticationPrincipal UserPrincipal principal,
      @org.springframework.web.bind.annotation.RequestHeader(value = "HX-Request", required = false)
          String hxRequest) {
    notificationService.delete(id, principal.getId());
    if (hxRequest != null) {
      var user = userRepository.findById(principal.getId()).orElseThrow();
      long unread = notificationService.unreadCount(user);
      return org.springframework.http.ResponseEntity.ok()
          .header("Content-Type", "text/html; charset=UTF-8")
          .body(buildBadgeAndDotOob(unread));
    }
    return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
        .location(java.net.URI.create("/notifications"))
        .build();
  }

  /**
   * OOB 뱃지·bell dot 갱신 HTML 생성. read/delete 공용.
   *
   * <p>대상: {@code #header-bell-dot} (헤더 종 red dot), {@code #notif-panel-badge} (팝업 뱃지), {@code
   * #notif-unread-badge} (/notifications 페이지 뱃지). HTMX 는 페이지에 존재하는 target 만 swap 하고 나머지는 skip.
   */
  private String buildBadgeAndDotOob(long unread) {
    String hiddenAttr = unread > 0 ? "" : " hidden";
    String dotClass = unread > 0 ? "header-bell-dot" : "header-bell-dot header-bell-dot--hidden";
    return String.format(
        "<span id=\"header-bell-dot\" hx-swap-oob=\"outerHTML\" class=\"%s\""
            + " aria-hidden=\"true\"></span>"
            + "<span id=\"notif-panel-badge\" hx-swap-oob=\"outerHTML\""
            + " class=\"notif-panel-badge\"%s>%d</span>"
            + "<span id=\"notif-unread-badge\" hx-swap-oob=\"outerHTML\""
            + " class=\"notif-unread-badge\"%s>%d</span>",
        dotClass, hiddenAttr, unread, hiddenAttr, unread);
  }

  /**
   * 읽음 처리된 아이템의 unread 클래스 제거 OOB. /notifications 페이지의 {@code <li id="notif-item-{id}">} 를 대상으로 함.
   *
   * <p>기존 HTML: {@code <li id="notif-item-{id}" class="notif-page-item notif-item--unread">}. OOB
   * 응답으로 class 에서 {@code notif-item--unread} 만 제거. 다른 자식은 그대로 두기 위해 outerHTML 대신 attribute-only 갱신을
   * 위한 {@code hx-swap-oob="innerHTML"} 은 부적합 → 최소 marker 로 wrapper span 을 두거나, 여기서는 outerHTML 로 li
   * 전체를 재렌더하되 클라이언트 측 JS 로 처리하는 편이 낫다. 다만 이번 스코프에선 unread 클래스 제거만이 목표라 CSS class 조작 스크립트를 응답에 심는
   * 방식으로 처리.
   */
  private String buildItemReadOob(long itemId) {
    // hx-swap-oob 로는 attribute-only 변경이 불편 → 응답에 <script> 를 심어 client-side 로 class 제거.
    // 이 스크립트는 hx-swap-oob 응답 처리 후 실행됨.
    return String.format(
        "<script id=\"notif-item-read-%d\" hx-swap-oob=\"outerHTML\">"
            + "(function(){var el=document.getElementById('notif-item-%d');"
            + "if(el){el.classList.remove('notif-item--unread');"
            + "var ic=el.querySelector('.notif-icon');if(ic)ic.classList.remove('is-unread');}"
            + "})();</script>",
        itemId, itemId);
  }
}
