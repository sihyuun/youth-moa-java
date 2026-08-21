/*
 * carousel-fade.js — 가로 스크롤 캐러셀 스크롤 위치 페이드 (F4)
 * 260821 P2: Claude Design 가이드 rev.2 §2.5 반영.
 *
 * 대상: 클래스 `.home-program-row`, `.home-space-row`, `.detail-related-row`
 * 각 컨테이너에 스크롤 상태 클래스를 토글:
 *   .is-scroll-single — 카드 1장, 마스크 안 걸림
 *   .is-scroll-mid    — 중간 (양쪽 페이드)
 *   .is-scroll-end    — 끝 (좌측만 페이드)
 *   (기본)            — 시작 (우측만 페이드) - CSS 기본값
 *
 * rAF throttle 로 scroll 성능 확보. `mask-image` 미지원 브라우저는 마스크만 사라지고 스크롤 정상.
 */
(function () {
    'use strict';

    const SELECTOR = '.home-program-row, .home-space-row, .detail-related-row';

    function updateState(row) {
        if (row.scrollWidth <= row.clientWidth + 2) {
            row.classList.add('is-scroll-single');
            row.classList.remove('is-scroll-mid', 'is-scroll-end');
            return;
        }
        row.classList.remove('is-scroll-single');
        const atStart = row.scrollLeft <= 2;
        const atEnd = row.scrollLeft + row.clientWidth >= row.scrollWidth - 2;
        row.classList.toggle('is-scroll-mid', !atStart && !atEnd);
        row.classList.toggle('is-scroll-end', atEnd);
    }

    function bind(row) {
        if (row.dataset.carouselFadeBound === '1') return;
        row.dataset.carouselFadeBound = '1';
        let ticking = false;
        const onScroll = () => {
            if (ticking) return;
            ticking = true;
            requestAnimationFrame(() => {
                updateState(row);
                ticking = false;
            });
        };
        row.addEventListener('scroll', onScroll, { passive: true });
        // 초기 상태 반영
        updateState(row);
    }

    function init() {
        document.querySelectorAll(SELECTOR).forEach(bind);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // HTMX 부분 갱신 후 재바인딩
    document.body.addEventListener('htmx:afterSwap', init);

    // 리사이즈 시 재판정 (뷰포트 회전 등)
    window.addEventListener('resize', () => {
        document.querySelectorAll(SELECTOR).forEach(updateState);
    });

    window.CarouselFade = { updateState, bind, init };
})();
