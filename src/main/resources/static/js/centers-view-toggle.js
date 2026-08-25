/**
 * 청년센터 모바일 뷰 세그먼트 토글.
 *
 * <p>data-view="list|map" 값을 body class 로 반영해 CSS 가 지도/리스트를 배타적으로 노출.
 * 상태는 sessionStorage 로 새로고침·페이지 이동 후 복원.
 *
 * 데스크톱(≥768px) 에서는 body class 무관하게 CSS 가 3-column 유지 (모바일 media 안에서만 스코프).
 *
 * 신설: 2026-08-25 P8.
 */
(function () {
  'use strict';

  var STORE_KEY = 'centers.view';
  var VIEWS = ['list', 'map'];

  function apply(view) {
    if (VIEWS.indexOf(view) === -1) view = 'list';
    var body = document.body;
    VIEWS.forEach(function (v) {
      body.classList.toggle('centers-view-' + v, v === view);
    });
    var buttons = document.querySelectorAll('[data-centers-view]');
    buttons.forEach(function (btn) {
      var on = btn.getAttribute('data-centers-view') === view;
      btn.classList.toggle('is-active', on);
      btn.setAttribute('aria-pressed', on ? 'true' : 'false');
    });
    try {
      sessionStorage.setItem(STORE_KEY, view);
    } catch (e) {
      /* 시크릿 모드 등 저장 불가 시 무시 */
    }
  }

  function init() {
    var toggle = document.querySelector('[data-centers-view-toggle]');
    if (!toggle) return;
    // 초기값: sessionStorage → default list
    var initial = 'list';
    try {
      var saved = sessionStorage.getItem(STORE_KEY);
      if (saved && VIEWS.indexOf(saved) !== -1) initial = saved;
    } catch (e) {
      /* ignore */
    }
    apply(initial);
    toggle.addEventListener('click', function (e) {
      var btn = e.target.closest('[data-centers-view]');
      if (!btn) return;
      e.preventDefault();
      apply(btn.getAttribute('data-centers-view'));
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
