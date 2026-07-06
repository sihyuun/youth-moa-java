/**
 * F0e-2: Hero 배경 이미지 크로스페이드 로테이션.
 *
 * - 8초 간격으로 .hero-bg 요소에 .is-active 토글 (opacity transition 1.2s)
 * - 첫 로드 시 나머지 이미지 preload (data-src)
 * - prefers-reduced-motion: reduce → 로테이션 비활성 (첫 이미지 고정)
 */
(function () {
  'use strict';

  const INTERVAL_MS = 8000;

  function init() {
    const nodes = document.querySelectorAll('.hero .hero-bg');
    if (nodes.length < 2) return;

    // 접근성: 모션 감소 선호 → 로테이션 스킵
    if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      return;
    }

    // 첫 번째 제외한 나머지 preload
    for (let i = 1; i < nodes.length; i++) {
      const src = nodes[i].dataset.src;
      if (src) {
        const img = new Image();
        img.src = src;
      }
    }

    let currentIdx = 0;
    setInterval(function () {
      nodes[currentIdx].classList.remove('is-active');
      currentIdx = (currentIdx + 1) % nodes.length;
      nodes[currentIdx].classList.add('is-active');
    }, INTERVAL_MS);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
