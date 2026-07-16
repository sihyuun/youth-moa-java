/*
 * common-ui.js — 전역 공통 Modal / Toast API (§4-S).
 *
 * window.Toast.show(message, variant='success', duration=2800)
 *   → 상단 중앙 slideDown 300ms 진입, 자동 소멸. role="status" aria-live="polite".
 *
 * window.Modal
 *   .open(id)                    지정 id 의 .modal-backdrop 을 aria-hidden=false 로 열기 + ESC/backdrop/focus trap
 *   .close(id)                   닫기 + 트리거로 포커스 복귀
 *   .confirm({title, message, confirmText, cancelText, variant, onConfirm})
 *                                → Promise<boolean> 반환. ConfirmDialog 카드 동적 생성
 *
 * M3 스태킹: base z-index 500, 스택마다 +10. 최상위만 백드롭 렌더 (누적 금지).
 */
(function () {
  'use strict';

  // ── Toast ──────────────────────────────────────────
  function getToastStack() {
    var stack = document.querySelector('.toast-stack');
    if (!stack) {
      stack = document.createElement('div');
      stack.className = 'toast-stack';
      stack.setAttribute('aria-live', 'polite');
      document.body.appendChild(stack);
    }
    return stack;
  }

  var SUCCESS_ICON =
    '<svg class="toast__icon" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
    '<circle cx="12" cy="12" r="10" fill="#22C55E" stroke="none"/>' +
    '<path d="M8 12l3 3 5-6" stroke="#fff"/></svg>';
  var ERROR_ICON =
    '<svg class="toast__icon" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
    '<circle cx="12" cy="12" r="10" fill="#EF4444" stroke="none"/>' +
    '<path d="M15 9l-6 6M9 9l6 6" stroke="#fff"/></svg>';
  var INFO_ICON =
    '<svg class="toast__icon" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
    '<circle cx="12" cy="12" r="10" fill="#3B82F6" stroke="none"/>' +
    '<path d="M12 8v5M12 16h.01" stroke="#fff"/></svg>';

  var Toast = {
    show: function (message, variant, duration) {
      variant = variant || 'success';
      duration = typeof duration === 'number' ? duration : 2800;
      var stack = getToastStack();
      var el = document.createElement('div');
      el.className = 'toast toast--' + variant;
      el.setAttribute('role', 'status');
      var icon = SUCCESS_ICON;
      if (variant === 'error') icon = ERROR_ICON;
      else if (variant === 'info') icon = INFO_ICON;
      el.innerHTML = icon + '<span class="toast__message"></span>';
      el.querySelector('.toast__message').textContent = String(message || '');
      stack.appendChild(el);
      requestAnimationFrame(function () { el.classList.add('is-visible'); });
      setTimeout(function () {
        el.classList.remove('is-visible');
        setTimeout(function () { if (el.parentNode) el.parentNode.removeChild(el); }, 300);
      }, duration);
      return el;
    }
  };

  // ── Modal ──────────────────────────────────────────
  var FOCUSABLE_SEL =
    'a[href], area[href], button:not([disabled]), input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

  // 스택: 열려 있는 모달 (id or element). z-index base 500, 스택마다 +10.
  var stack = [];

  function computeZIndex(depth) { return 500 + depth * 10; }

  function getFocusable(card) {
    return Array.prototype.slice.call(card.querySelectorAll(FOCUSABLE_SEL))
      .filter(function (el) { return el.offsetWidth > 0 || el.offsetHeight > 0 || el === document.activeElement; });
  }

  function trapKeydown(entry) {
    return function (e) {
      if (e.key === 'Escape') {
        e.preventDefault();
        closeEntry(entry, false);
        return;
      }
      if (e.key !== 'Tab') return;
      var focusables = getFocusable(entry.card);
      if (focusables.length === 0) { e.preventDefault(); return; }
      var first = focusables[0];
      var last = focusables[focusables.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault(); last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault(); first.focus();
      }
    };
  }

  function backdropClick(entry) {
    return function (e) { if (e.target === entry.backdrop) closeEntry(entry, false); };
  }

  function setBackdropVisibility() {
    // 최상위만 백드롭 렌더. 그 아래는 카드만.
    stack.forEach(function (entry, idx) {
      var isTop = idx === stack.length - 1;
      entry.backdrop.classList.toggle('modal-backdrop--transparent', !isTop);
    });
  }

  function setBackgroundInert(active) {
    // 배경(모달 외부) 접근성 차단
    var main = document.querySelector('body > main, main');
    if (main) {
      if (active) main.setAttribute('inert', ''); else main.removeAttribute('inert');
    }
  }

  function openEntry(entry) {
    entry.trigger = document.activeElement;
    var depth = stack.length;
    entry.backdrop.style.zIndex = String(computeZIndex(depth));
    entry.card.style.zIndex = String(computeZIndex(depth) + 1);
    entry.backdrop.hidden = false;
    entry.backdrop.setAttribute('aria-hidden', 'false');
    entry.card.setAttribute('role', 'dialog');
    entry.card.setAttribute('aria-modal', 'true');
    stack.push(entry);
    setBackdropVisibility();
    setBackgroundInert(true);

    entry._onKeydown = trapKeydown(entry);
    entry._onBackdropClick = backdropClick(entry);
    document.addEventListener('keydown', entry._onKeydown);
    entry.backdrop.addEventListener('click', entry._onBackdropClick);

    // 첫 포커스: 제목 다음 첫 인터랙티브 (없으면 card tabindex=-1)
    var focusables = getFocusable(entry.card);
    var target = focusables[0];
    if (!target) { entry.card.setAttribute('tabindex', '-1'); target = entry.card; }
    // 닫기 X 버튼은 스킵하여 본문 첫 요소로
    if (target && target.classList && target.classList.contains('modal-card__close') && focusables.length > 1) {
      target = focusables[1];
    }
    setTimeout(function () { try { target.focus(); } catch (e) {} }, 0);
  }

  function closeEntry(entry, silent) {
    var idx = stack.indexOf(entry);
    if (idx < 0) return;
    stack.splice(idx, 1);
    entry.backdrop.hidden = true;
    entry.backdrop.setAttribute('aria-hidden', 'true');
    document.removeEventListener('keydown', entry._onKeydown);
    entry.backdrop.removeEventListener('click', entry._onBackdropClick);
    setBackdropVisibility();
    if (stack.length === 0) setBackgroundInert(false);
    if (entry.trigger && typeof entry.trigger.focus === 'function') {
      try { entry.trigger.focus(); } catch (e) {}
    }
    if (entry._onClose && !silent) entry._onClose();
    if (entry._ephemeral && entry.backdrop.parentNode) {
      entry.backdrop.parentNode.removeChild(entry.backdrop);
    }
  }

  function findEntryById(id) {
    for (var i = 0; i < stack.length; i++) if (stack[i].id === id) return stack[i];
    return null;
  }

  function entryFromBackdrop(backdrop) {
    var card = backdrop.querySelector('.modal-card');
    return { id: backdrop.id, backdrop: backdrop, card: card, _ephemeral: false };
  }

  var Modal = {
    open: function (id) {
      var backdrop = document.getElementById(id);
      if (!backdrop) return;
      if (findEntryById(id)) return;
      var entry = entryFromBackdrop(backdrop);
      // 카드 내 [data-modal-close] 버튼 자동 연결
      var closers = entry.card ? entry.card.querySelectorAll('[data-modal-close]') : [];
      closers.forEach && closers.forEach(function (btn) {
        btn.addEventListener('click', function () { Modal.close(id); });
      });
      openEntry(entry);
    },
    close: function (id) {
      var entry = findEntryById(id);
      if (entry) closeEntry(entry, false);
    },
    confirm: function (opts) {
      opts = opts || {};
      return new Promise(function (resolve) {
        var backdrop = document.createElement('div');
        backdrop.className = 'modal-backdrop';
        var card = document.createElement('div');
        card.className = 'modal-card modal-card--sm modal-card--dialog';
        card.setAttribute('role', 'dialog');
        card.setAttribute('aria-modal', 'true');

        var titleId = 'modal-confirm-title-' + Date.now();
        var variant = opts.variant || 'default';
        var confirmClass = 'btn-auth btn-auth--primary';
        if (variant === 'danger') confirmClass = 'btn-auth btn-auth--danger';

        card.innerHTML =
          '<div class="modal-card__header">' +
          '  <h3 class="modal-card__title" id="' + titleId + '"></h3>' +
          '</div>' +
          '<div class="modal-card__body"><p class="modal-card__message"></p></div>' +
          '<div class="modal-card__footer">' +
          '  <button type="button" class="btn-auth btn-auth--secondary" data-role="cancel"></button>' +
          '  <button type="button" data-role="confirm"></button>' +
          '</div>';
        card.setAttribute('aria-labelledby', titleId);
        card.querySelector('.modal-card__title').textContent = opts.title || '확인';
        card.querySelector('.modal-card__message').textContent = opts.message || '';
        var cancelBtn = card.querySelector('[data-role="cancel"]');
        var confirmBtn = card.querySelector('[data-role="confirm"]');
        cancelBtn.textContent = opts.cancelText || '취소';
        confirmBtn.textContent = opts.confirmText || '확인';
        confirmBtn.className = confirmClass;

        backdrop.appendChild(card);
        document.body.appendChild(backdrop);

        var entry = { id: null, backdrop: backdrop, card: card, _ephemeral: true };
        entry._onClose = function () { resolve(false); };
        cancelBtn.addEventListener('click', function () { closeEntry(entry, false); });
        confirmBtn.addEventListener('click', function () {
          entry._onClose = null;
          closeEntry(entry, true);
          try { if (opts.onConfirm) opts.onConfirm(); } catch (e) {}
          resolve(true);
        });
        openEntry(entry);
      });
    }
  };

  window.Toast = Toast;
  window.Modal = Modal;
})();
