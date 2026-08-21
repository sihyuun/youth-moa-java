/*
 * site-drawer.js — 모바일 우측 슬라이드 드로어 (P1 · F3 인증 전환)
 * 260821: Claude Design 가이드 rev.2 §2.2 반영.
 *
 * 동작:
 *   - 트리거 .header-menu-btn 클릭 → open
 *   - [data-drawer-close] · backdrop 클릭 · ESC · 내부 링크 클릭 → close
 *   - body scroll lock · aria-expanded 토글
 *   - F3: HTMX 401 응답 시 자동 close + /login?redirect=… 이동
 */
(function () {
    'use strict';

    const drawer = document.getElementById('site-drawer');
    const trigger = document.querySelector('.header-menu-btn');
    if (!drawer || !trigger) return;

    function isOpen() {
        return drawer.classList.contains('is-open');
    }

    function open() {
        drawer.hidden = false;
        // 강제 reflow — hidden 제거 직후 transition 이 적용되도록
        drawer.offsetHeight;  // eslint-disable-line no-unused-expressions
        drawer.classList.add('is-open');
        trigger.setAttribute('aria-expanded', 'true');
        document.body.style.overflow = 'hidden';
        // 최초 focus 는 close 버튼 (WCAG focus trap 진입점)
        const closeBtn = drawer.querySelector('.site-drawer-close');
        if (closeBtn) closeBtn.focus();
    }

    function close() {
        if (!isOpen()) return;
        drawer.classList.remove('is-open');
        trigger.setAttribute('aria-expanded', 'false');
        document.body.style.overflow = '';
        // 애니메이션 완료 후 hidden 처리
        setTimeout(() => {
            if (!isOpen()) drawer.hidden = true;
        }, 260);
        trigger.focus();
    }

    // 트리거 클릭 → open
    trigger.addEventListener('click', open);

    // 닫기 트리거: [data-drawer-close] 요소 (backdrop + close 버튼)
    drawer.querySelectorAll('[data-drawer-close]').forEach((el) => {
        el.addEventListener('click', close);
    });

    // ESC 로 닫기
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && isOpen()) close();
    });

    // F3-1: 드로어 내부 링크 클릭 → 즉시 닫음 (transitionend 대기 X, 페이지 전환과 겹치지 않게)
    drawer.querySelectorAll('a').forEach((a) => {
        a.addEventListener('click', () => {
            // 실제 이동은 브라우저가 처리. 여기선 시각 상태만 close.
            drawer.classList.remove('is-open');
            document.body.style.overflow = '';
        });
    });

    // F3-2: HTMX 401 응답 → 드로어 닫고 /login?redirect=… 로 이동
    document.body.addEventListener('htmx:responseError', (e) => {
        if (e?.detail?.xhr?.status === 401) {
            close();
            const redirect = encodeURIComponent(location.pathname + location.search);
            location.href = '/login?redirect=' + redirect;
        }
    });
})();
