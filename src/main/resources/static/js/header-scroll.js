/*
 * F2c: 홈 헤더 transparent 모드 스크롤 감지.
 * - scrollY > 60 → body.is-scrolled 클래스 부여 → CSS 셀렉터가 백색 헤더로 전환
 * - 로드 시 초기 1회 평가 (브라우저 스크롤 위치 복원 대응)
 * - 홈 이외 페이지에서는 site-header--transparent 클래스가 없어 CSS 오버라이드가 발생하지 않으므로,
 *   is-scrolled 토글은 무해 (서브 페이지는 항상 백색).
 */
(function () {
  'use strict';

  var THRESHOLD = 60;
  var body = document.body;
  var ticking = false;

  function apply() {
    ticking = false;
    if (window.scrollY > THRESHOLD) {
      body.classList.add('is-scrolled');
    } else {
      body.classList.remove('is-scrolled');
    }
  }

  function onScroll() {
    if (!ticking) {
      window.requestAnimationFrame(apply);
      ticking = true;
    }
  }

  // 초기 상태 (F5 시 스크롤 위치 복원 대응)
  apply();

  window.addEventListener('scroll', onScroll, { passive: true });
})();
