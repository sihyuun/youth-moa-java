/**
 * centers-detail.js — F0h-c2 개정 (2026-07-09)
 *
 * 청년센터 목록 3-column 인터랙션 담당:
 *   - 카드 anchor click 을 가로채 (preventDefault) full page reload 회피
 *   - `.centers-list-col.has-detail` 클래스 토글로 CSS transition 250ms 발동 (prototype.tsx L2007 재현)
 *   - fetch 2건 (2026-07-10 htmx.ajax quirks 회피):
 *       1) GET /centers/{id}/detail-fragment  → .centers-detail-col innerHTML
 *       2) GET /centers/cards?compact=...     → .centers-list-scroll innerHTML (compact/full 전환)
 *   - history.pushState 로 URL 만 갱신 (뒤로가기 지원)
 *   - popstate 리스너: 브라우저 back/forward 로 상세 open/close 상태 재현
 *
 * center-map.js 는 무변경. 이 파일은 관심사(카드 클릭 상세 open/close) 만 담당.
 */
(function () {
  'use strict';

  var listCol = document.querySelector('[data-centers-list-col]');
  var detailCol = document.querySelector('[data-centers-detail-col]');
  var listScroll = document.querySelector('[data-centers-list-scroll]');

  if (!listCol || !detailCol || !listScroll) {
    return; // /centers 페이지 아닌 곳에서 방어
  }

  // 현재 URL 의 filter query 유지용
  function currentFilterParams() {
    var url = new URL(window.location.href);
    var params = new URLSearchParams();
    ['q', 'region', 'onlyActive', 'sort'].forEach(function (k) {
      var v = url.searchParams.get(k);
      if (v !== null && v !== '') params.set(k, v);
    });
    return params;
  }

  function buildUrl(base, params) {
    var qs = params.toString();
    return qs ? base + '?' + qs : base;
  }

  // fetch fragment HTML → target.innerHTML 교체. fetch 실패 시 warn 로그 후 skip.
  // htmx.ajax 는 DOM element target 처리에 quirks 있어 (2026-07-10 사고) fetch 로 대체.
  function fetchAndSwap(url, target) {
    return fetch(url, { headers: { 'Accept': 'text/html' } })
      .then(function (r) {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.text();
      })
      .then(function (html) { target.innerHTML = html; })
      .catch(function (err) { console.warn('[centers-detail] fetch failed:', url, err); });
  }

  // 상세 open (client-side)
  function openDetail(centerId, pushHistory) {
    var params = currentFilterParams();

    // 1) 상세 fragment 주입
    fetchAndSwap('/centers/' + centerId + '/detail-fragment', detailCol);
    detailCol.removeAttribute('hidden');

    // 2) 리스트 카드 fragment 재요청 (compact 로 전환 + activeId 부여)
    var cardsParams = new URLSearchParams(params);
    cardsParams.set('compact', 'true');
    cardsParams.set('activeId', String(centerId));
    fetchAndSwap(buildUrl('/centers/cards', cardsParams), listScroll);

    // 3) transition 발동
    listCol.classList.add('has-detail');

    // 4) URL 갱신
    if (pushHistory) {
      var pushed = buildUrl('/centers/' + centerId, params);
      window.history.pushState({ detailId: centerId }, '', pushed);
    }

    // 5) FAIL-1 fix: 마커 selected 상태 동기화 (center-map.js 가 이 이벤트를 수신하여 selectMarker 실행)
    document.dispatchEvent(new CustomEvent('centers:detail-open', { detail: { centerId: centerId } }));
  }

  // 상세 close (client-side)
  function closeDetail(pushHistory) {
    var params = currentFilterParams();

    // 1) 리스트 카드 fragment 재요청 (full 로 전환, activeId 제거)
    var cardsParams = new URLSearchParams(params);
    cardsParams.set('compact', 'false');
    fetchAndSwap(buildUrl('/centers/cards', cardsParams), listScroll);

    // 2) 상세 패널 숨김
    detailCol.setAttribute('hidden', 'hidden');
    detailCol.innerHTML = '';

    // 3) transition 발동 (240 → 360)
    listCol.classList.remove('has-detail');

    // 4) URL 갱신
    if (pushHistory) {
      var pushed = buildUrl('/centers', params);
      window.history.pushState({ detailId: null }, '', pushed);
    }

    // 5) 지도 인포윈도우/마커 선택은 유지 (2026-07-10 사용자 요청).
    //    사용자가 지도 상태를 별도로 정리하려면 인포윈도우 자체 X (.center-info-window-close) 사용.
    //    map 완전 초기화가 필요한 특수 케이스는 popstate 흐름에서만 처리 (아래 참조).
  }

  // FAIL-2 fix: 인포윈도우 CTA 클릭 → 상세 open (전역 request-detail 이벤트 수신)
  document.addEventListener('centers:request-detail', function (e) {
    if (e.detail && e.detail.centerId) {
      openDetail(e.detail.centerId, true);
    }
  });

  // 카드 anchor click 가로채기 (이벤트 위임 — fragment swap 후에도 유효)
  listScroll.addEventListener('click', function (e) {
    var card = e.target.closest('.center-card');
    if (!card) return;
    var centerId = card.getAttribute('data-center-id');
    if (!centerId) return;
    e.preventDefault();
    openDetail(centerId, true);
  });

  // 상세 패널 내부 close(×) 클릭 가로채기 (이벤트 위임 — innerHTML 교체 후에도 유효)
  detailCol.addEventListener('click', function (e) {
    var closeEl = e.target.closest('[data-centers-detail-close]');
    if (!closeEl) return;
    e.preventDefault();
    closeDetail(true);
  });

  // popstate: 뒤로가기/앞으로가기 시 상세 상태 재현
  // 상세 open 상태로 진입한 뒤 back → 목록만, forward → 상세 재open.
  window.addEventListener('popstate', function (e) {
    var pathMatch = window.location.pathname.match(/^\/centers\/(\d+)$/);
    if (pathMatch) {
      openDetail(pathMatch[1], false);
    } else if (/^\/centers\/?$/.test(window.location.pathname)) {
      closeDetail(false);
    }
  });

  // ═══════════════════════════════════════════════════════════════════════
  // 필터 partial swap (2026-07-10)
  //   - form (지역·정렬·운영중·검색) submit 을 intercept
  //   - fetch /centers/cards?{filter} → listScroll innerHTML 교체
  //   - visibleIds 추출 → centers:filter-changed 이벤트 dispatch
  //   - center-map.js 는 이 이벤트 수신해 overlay 표시/숨김 (풀 리로드 회피)
  //   - URL 은 pushState 로 현재 필터 반영
  // ═══════════════════════════════════════════════════════════════════════
  var filterForm = document.querySelector('.centers-filter-form');
  if (filterForm) {
    filterForm.addEventListener('submit', function (e) {
      e.preventDefault();
      applyFilter();
    });
  }

  function applyFilter() {
    var formData = new FormData(filterForm);
    var formParams = new URLSearchParams();
    formData.forEach(function (v, k) { if (v !== '') formParams.set(k, String(v)); });

    var isDetailOpen = listCol.classList.contains('has-detail');
    var activeCard = listScroll.querySelector('.center-card.is-active');
    var activeId = activeCard ? activeCard.getAttribute('data-center-id') : null;

    var cardsParams = new URLSearchParams(formParams);
    cardsParams.set('compact', isDetailOpen ? 'true' : 'false');
    if (isDetailOpen && activeId) cardsParams.set('activeId', activeId);

    fetch(buildUrl('/centers/cards', cardsParams), { headers: { 'Accept': 'text/html' } })
      .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.text(); })
      .then(function (html) {
        listScroll.innerHTML = html;
        // 필터 결과 visible ID 추출 → 지도 마커 반영
        var visibleIds = Array.prototype.slice.call(
          listScroll.querySelectorAll('.center-card[data-center-id]')
        ).map(function (el) { return el.getAttribute('data-center-id'); });
        document.dispatchEvent(new CustomEvent('centers:filter-changed', {
          detail: { visibleIds: visibleIds }
        }));
      })
      .catch(function (err) { console.warn('[centers-detail] filter fetch failed:', err); });

    // URL 갱신 — 현재 detail 상태 유지하며 필터만 반영
    var basePath = isDetailOpen && activeId ? '/centers/' + activeId : '/centers';
    var newUrl = buildUrl(basePath, formParams);
    window.history.pushState(
      { detailId: isDetailOpen ? activeId : null, filter: formParams.toString() },
      '',
      newUrl
    );
  }
})();
