/*
 * bottom-sheet.js — 모바일 바텀시트 공용 유틸 (F5 swipe-to-dismiss 포함)
 * 260821 P0: Claude Design 가이드 §2.8 반영. 필터/모달-카드/지도 peek 시트에서 재사용.
 *
 * 사용 방법:
 *   <div class="bottom-sheet" data-sheet="filter">
 *     <div class="bottom-sheet-handle"></div> (grabber ::before 대체 또는 추가 마크업)
 *     <div class="bottom-sheet-body">...</div>
 *   </div>
 *   BottomSheet.attach(sheetElement, { onClose, backdrop, excludeFullscreen })
 *
 * 주의: 전체화면 시트 (주소 검색·약관) 는 excludeFullscreen: true 로 드래그 비활성.
 */
(function () {
    'use strict';

    const CLOSE_DISTANCE = 100; // px
    const CLOSE_VELOCITY = 0.5; // px/ms
    const BACKDROP_MAX_OPACITY = 0.45;
    const BACKDROP_MIN_OPACITY = 0.08;
    const DAMPING_DISTANCE = 240;
    const REDUCED_MOTION = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    /**
     * 바텀시트에 swipe-to-dismiss 를 부착한다.
     * @param {HTMLElement} sheet 시트 root
     * @param {object} opts
     * @param {() => void} opts.onClose 닫힘 시 콜백 (실제 hidden 처리·backdrop 제거)
     * @param {HTMLElement} [opts.backdrop] backdrop element (opacity 연동)
     * @param {boolean} [opts.excludeFullscreen] 전체화면 시트면 true (드래그 비활성)
     */
    function attach(sheet, opts) {
        if (!sheet || opts?.excludeFullscreen) return;
        const backdrop = opts?.backdrop;
        const onClose = opts?.onClose || (() => {});

        let y0 = null;
        let t0 = 0;
        let dy = 0;

        function isHandle(target) {
            // grabber (::before) 는 실제 element 아니므로, 헤더/handle 클래스로 판정
            return target.classList?.contains('bottom-sheet-handle') ||
                   target.closest('.bottom-sheet-handle') ||
                   target.closest('.bottom-sheet-head');
        }

        function bodyScrollTop() {
            const body = sheet.querySelector('.bottom-sheet-body, .filter-popover-options, .modal-card-body');
            return body ? body.scrollTop : 0;
        }

        sheet.addEventListener('touchstart', (e) => {
            if (!isHandle(e.target) && bodyScrollTop() > 0) return;
            y0 = e.touches[0].clientY;
            t0 = performance.now();
            sheet.style.transition = 'none';
        }, { passive: true });

        sheet.addEventListener('touchmove', (e) => {
            if (y0 == null) return;
            dy = Math.max(0, e.touches[0].clientY - y0);
            sheet.style.transform = `translateY(${dy}px)`;
            if (backdrop) {
                const opacity = Math.max(BACKDROP_MIN_OPACITY, BACKDROP_MAX_OPACITY * (1 - dy / DAMPING_DISTANCE));
                backdrop.style.opacity = String(opacity / BACKDROP_MAX_OPACITY);
            }
        }, { passive: true });

        sheet.addEventListener('touchend', () => {
            if (y0 == null) return;
            const dt = performance.now() - t0;
            const velocity = dt > 0 ? dy / dt : 0;
            sheet.style.transition = REDUCED_MOTION ? 'none' : 'transform 160ms ease';

            if (dy >= CLOSE_DISTANCE || velocity >= CLOSE_VELOCITY) {
                sheet.style.transform = `translateY(100%)`;
                const delay = REDUCED_MOTION ? 0 : 220;
                setTimeout(() => {
                    sheet.style.transform = '';
                    sheet.style.transition = '';
                    if (backdrop) backdrop.style.opacity = '';
                    onClose();
                }, delay);
            } else {
                sheet.style.transform = 'translateY(0)';
                if (backdrop) backdrop.style.opacity = '';
            }

            y0 = null;
            dy = 0;
        });
    }

    window.BottomSheet = { attach };
})();
