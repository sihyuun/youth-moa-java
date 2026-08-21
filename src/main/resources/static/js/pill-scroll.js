/*
 * pill-scroll.js — 모바일 가로 스크롤 pill 그룹 유틸 (F6)
 * 260821 P0: Claude Design 가이드 §2.9 반영.
 *
 * 두 가지 기능:
 *   ① revealActive: 활성 pill 을 좌측 16px 지점에 물리도록 스크롤 (뷰포트 밖일 때만)
 *   ② HTMX 부분 갱신 시 scrollLeft 유지 (sessionStorage 로 복원 — 뒤로가기·재진입 대응)
 *
 * 대상 컨테이너: [data-pill-row] 속성 부착된 요소.
 * 예: <div class="filter-left" data-pill-row="status">
 *
 * scroll-behavior: smooth 미사용 — 복원 시 애니메이션이 보이면 "위치 유지" 가 아니라 이동으로 오해된다.
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'pillScroll:' + location.pathname;

    /**
     * 활성 pill 이 뷰포트 밖이면 좌측 16px 지점으로 스크롤한다.
     * scrollIntoView 는 조상 스크롤 컨테이너와 페이지 전체를 함께 움직여 부적절.
     */
    function revealActive(row) {
        const active = row.querySelector('.is-active, .active, [aria-selected="true"]');
        if (!active) return;
        const start = active.offsetLeft;
        const end = start + active.offsetWidth;
        const viewStart = row.scrollLeft;
        const viewEnd = viewStart + row.clientWidth;
        if (start < viewStart || end > viewEnd) {
            row.scrollLeft = Math.max(0, start - 16);
        }
    }

    function saveAll() {
        const map = {};
        document.querySelectorAll('[data-pill-row]').forEach((row) => {
            map[row.dataset.pillRow] = row.scrollLeft;
        });
        try {
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify(map));
        } catch (e) {
            /* Storage quota or private mode — 무시하고 계속 */
        }
    }

    function restoreAll() {
        let map = {};
        try {
            map = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || '{}');
        } catch (e) {
            map = {};
        }
        document.querySelectorAll('[data-pill-row]').forEach((row) => {
            const key = row.dataset.pillRow;
            if (map[key] != null) row.scrollLeft = map[key];
            revealActive(row);
        });
    }

    // HTMX 부분 갱신 대응 — swap 전에 스크롤 위치 저장, swap 후 복원.
    document.body.addEventListener('htmx:beforeSwap', saveAll);
    document.body.addEventListener('htmx:afterSwap', restoreAll);

    // 초기 로드
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', restoreAll);
    } else {
        restoreAll();
    }

    window.PillScroll = { revealActive, saveAll, restoreAll };
})();
