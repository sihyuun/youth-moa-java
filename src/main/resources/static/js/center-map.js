/**
 * F0h — 청년센터 3-column 인터랙션.
 *
 * - 커스텀 지역 드롭다운 (검색 + 하이라이트 + 클릭 시 URL 반영)
 * - 정렬 pill (name / programs) → hidden input 세팅 후 form submit
 * - 운영중 토글 스위치 → change 시 form submit
 * - 카카오맵: 커스텀 오버레이 마커 + 인포윈도우 + "이 지역에서 검색" (dirty flag)
 * - 카드 hover 지도 동기화
 *
 * SDK 미로드(appkey 미설정 or 실패) 시 지도 관련 로직만 skip, 필터 UX 는 정상 동작.
 */
(function () {
  'use strict';

  function ready(fn) {
    if (document.readyState !== 'loading') fn();
    else document.addEventListener('DOMContentLoaded', fn);
  }

  function escapeHtml(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function getForm() {
    return document.querySelector('.centers-filter-form');
  }

  function setUrlParamAndSubmit(name, value) {
    var form = getForm();
    if (!form) return;
    // remove existing extra hidden of same name
    var existing = form.querySelectorAll('input[type="hidden"][name="' + name + '"]');
    for (var i = 0; i < existing.length; i++) existing[i].remove();
    if (value !== null && value !== undefined && value !== '') {
      var input = document.createElement('input');
      input.type = 'hidden';
      input.name = name;
      input.value = value;
      form.appendChild(input);
    }
    form.submit();
  }

  // ── 커스텀 지역 드롭다운 ─────────────────────────
  function initRegionDropdown() {
    var root = document.querySelector('[data-region-dropdown]');
    if (!root) return;
    var trigger = root.querySelector('[data-region-trigger]');
    var panel = root.querySelector('[data-region-panel]');
    var searchInput = root.querySelector('[data-region-search]');
    var list = root.querySelector('[data-region-list]');
    var clearBtn = root.querySelector('[data-region-clear]');

    // × 클리어 (트리거 안쪽) — 상위 클릭 전파 막기
    if (clearBtn) {
      clearBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        setUrlParamAndSubmit('region', '');
      });
    }

    trigger.addEventListener('click', function (e) {
      // × 영역 클릭이면 위에서 처리됨
      if (e.target && e.target.hasAttribute('data-region-clear')) return;
      var isOpen = !panel.hasAttribute('hidden');
      if (isOpen) {
        panel.setAttribute('hidden', '');
      } else {
        panel.removeAttribute('hidden');
        if (searchInput) {
          searchInput.value = '';
          searchInput.focus();
          filterRegionList('');
        }
      }
    });

    document.addEventListener('click', function (e) {
      if (!root.contains(e.target)) {
        panel.setAttribute('hidden', '');
      }
    });

    if (searchInput) {
      searchInput.addEventListener('input', function () {
        filterRegionList(searchInput.value);
      });
    }

    list.addEventListener('click', function (e) {
      var item = e.target.closest('.centers-filter-region-item');
      if (!item) return;
      var region = item.getAttribute('data-region');
      setUrlParamAndSubmit('region', region);
    });

    function filterRegionList(query) {
      var q = (query || '').trim().toLowerCase();
      var items = list.querySelectorAll('.centers-filter-region-item');
      items.forEach(function (item) {
        var raw = item.getAttribute('data-region') || '';
        var lower = raw.toLowerCase();
        if (!q) {
          item.style.display = '';
          item.innerHTML = escapeHtml(raw);
          return;
        }
        var idx = lower.indexOf(q);
        if (idx < 0) {
          item.style.display = 'none';
        } else {
          item.style.display = '';
          var before = escapeHtml(raw.substring(0, idx));
          var match = escapeHtml(raw.substring(idx, idx + q.length));
          var after = escapeHtml(raw.substring(idx + q.length));
          item.innerHTML = before + '<mark>' + match + '</mark>' + after;
        }
      });
    }
  }

  // ── 정렬 pill ─────────────────────────
  function initSortPill() {
    var pills = document.querySelectorAll('.centers-sort-pill-item');
    pills.forEach(function (btn) {
      btn.addEventListener('click', function () {
        var sort = btn.getAttribute('data-sort');
        setUrlParamAndSubmit('sort', sort);
      });
    });
  }

  // ── 운영중 토글 스위치 ─────────────────────────
  function initOnlyActiveToggle() {
    var input = document.querySelector('[data-only-active-toggle]');
    if (!input) return;
    input.addEventListener('change', function () {
      // native form submit — checked 여부에 따라 hidden name=onlyActive 를 form 이 자동 전송
      var form = getForm();
      if (form) form.submit();
    });
  }

  // ── 카카오맵 ─────────────────────────
  function initMap() {
    var mapEl = document.getElementById('center-map');
    if (!mapEl) return;
    if (typeof kakao === 'undefined' || !kakao.maps) return;

    kakao.maps.load(function () {
      initListMap(mapEl);
    });
  }

  function initListMap(mapEl) {
    var cards = document.querySelectorAll('.center-card[data-lat][data-lng]');
    var validCards = [];
    for (var i = 0; i < cards.length; i++) {
      var lat = parseFloat(cards[i].getAttribute('data-lat'));
      var lng = parseFloat(cards[i].getAttribute('data-lng'));
      if (!isNaN(lat) && !isNaN(lng) && lat !== 0 && lng !== 0) {
        validCards.push({ card: cards[i], lat: lat, lng: lng });
      }
    }

    var defaultCenter = new kakao.maps.LatLng(37.4138, 127.5183);
    var map = new kakao.maps.Map(mapEl, { center: defaultCenter, level: 10 });

    // (B) ZoomControl UI — 사용자 수동 zoom 조절 (레퍼런스: 네이버 부동산, 카카오맵)
    map.addControl(new kakao.maps.ZoomControl(), kakao.maps.ControlPosition.RIGHT);

    var searchHereBtn = document.querySelector('[data-search-here]');
    var overlays = [];
    var selectedId = null;
    var infoOverlay = null;

    if (validCards.length === 0) return;

    var bounds = new kakao.maps.LatLngBounds();

    validCards.forEach(function (item) {
      var pos = new kakao.maps.LatLng(item.lat, item.lng);
      bounds.extend(pos);
      var card = item.card;
      var centerId = card.getAttribute('data-center-id');
      var name = (card.querySelector('.center-card-name') || card.querySelector('.center-card-compact-name'));
      var nameText = name ? name.textContent.trim() : '';
      var isActive = !card.querySelector('.center-card-badge.inactive');

      var markerEl = document.createElement('div');
      markerEl.className = 'center-marker';
      markerEl.setAttribute('data-marker-id', centerId);
      markerEl.style.cssText = markerStyle(false, isActive);
      markerEl.innerHTML = markerInner(false, isActive, nameText);

      var overlay = new kakao.maps.CustomOverlay({
        position: pos,
        content: markerEl,
        yAnchor: 1,
        zIndex: 10
      });
      overlay.setMap(map);

      markerEl.addEventListener('click', function (e) {
        e.stopPropagation();
        selectMarker(centerId);
      });

      overlays.push({
        id: centerId,
        card: card,
        pos: pos,
        overlay: overlay,
        el: markerEl,
        name: nameText,
        isActive: isActive
      });

      card.addEventListener('mouseenter', function () {
        markerEl.style.zIndex = 999;
        card.classList.add('is-hover');
      });
      card.addEventListener('mouseleave', function () {
        markerEl.style.zIndex = selectedId === centerId ? 20 : 10;
        card.classList.remove('is-hover');
      });
    });

    // (A + C) bounds fit + zoom clamp
    //   - 필터 결과 (validCards) 만 bounds 에 포함되므로 지역 필터 시 자동으로 그 지역 중심 fit (C)
    //   - clamp: level 5 (너무 확대 방지) ~ 9 (너무 축소 방지). 레퍼런스: 에어비앤비/Google Maps maxZoom clamp (A)
    //   - 단일 마커 케이스: setBounds 는 level 1 로 확대해버림 → minLevel 5 로 clamp (동네 시야)
    function fitAndClamp() {
      map.relayout();
      map.setBounds(bounds);
      var lv = map.getLevel();
      var MIN_LEVEL = 5;   // 확대 상한 (건물 단위까지 안 가게)
      var MAX_LEVEL = 9;   // 축소 상한 (경기도 도 전체는 넘지 않게)
      // 필터 결과 개수 별 미세 조정
      if (validCards.length === 1) {
        map.setLevel(MIN_LEVEL);
      } else if (lv < MIN_LEVEL) {
        map.setLevel(MIN_LEVEL);
      } else if (lv > MAX_LEVEL) {
        map.setLevel(MAX_LEVEL);
      }
    }
    fitAndClamp();
    setTimeout(fitAndClamp, 100);

    function selectMarker(id) {
      selectedId = id;
      overlays.forEach(function (o) {
        var isSel = o.id === id;
        o.el.style.cssText = markerStyle(isSel, o.isActive);
        o.el.innerHTML = markerInner(isSel, o.isActive, o.name);
      });
      var target = overlays.find(function (o) { return o.id === id; });
      if (target) openInfoWindow(target);
    }

    function clearSelection() {
      selectedId = null;
      overlays.forEach(function (o) {
        o.el.style.cssText = markerStyle(false, o.isActive);
        o.el.innerHTML = markerInner(false, o.isActive, o.name);
      });
      if (infoOverlay) { infoOverlay.setMap(null); infoOverlay = null; }
    }

    function openInfoWindow(target) {
      if (infoOverlay) infoOverlay.setMap(null);
      var container = document.createElement('div');
      container.className = 'center-info-window';
      container.style.cssText =
        'width:300px;border-radius:14px;box-shadow:0 8px 32px rgba(0,0,0,0.18);' +
        'background:var(--color-surface);overflow:hidden;position:relative;';
      container.innerHTML =
        '<div style="padding:12px 14px;">' +
        '<div style="font-size:15px;font-weight:700;color:var(--color-text);margin-bottom:6px;">' +
          escapeHtml(target.name) + '</div>' +
        '<a href="/centers/' + encodeURIComponent(target.id) +
          '" style="display:inline-block;padding:6px 12px;background:var(--color-primary);color:#fff;' +
          'text-decoration:none;border-radius:7px;font-size:12px;font-weight:600;">상세보기</a>' +
        '<button type="button" data-info-close style="position:absolute;top:6px;right:8px;' +
          'width:22px;height:22px;border-radius:50%;background:rgba(0,0,0,0.35);color:#fff;' +
          'border:none;font-size:14px;cursor:pointer;">×</button>' +
        '</div>';
      container.querySelector('[data-info-close]').addEventListener('click', function (e) {
        e.stopPropagation();
        clearSelection();
      });
      infoOverlay = new kakao.maps.CustomOverlay({
        position: target.pos,
        content: container,
        yAnchor: 1.4,
        zIndex: 100
      });
      infoOverlay.setMap(map);
    }

    kakao.maps.event.addListener(map, 'click', clearSelection);

    // dirty flag: 최초 setBounds 로 인한 idle 은 무시
    var dirtySuppressed = true;
    setTimeout(function () { dirtySuppressed = false; }, 500);
    kakao.maps.event.addListener(map, 'idle', function () {
      if (dirtySuppressed) return;
      if (searchHereBtn) searchHereBtn.hidden = false;
    });

    if (searchHereBtn) {
      searchHereBtn.addEventListener('click', function () {
        var b = map.getBounds();
        overlays.forEach(function (o) {
          if (b.contain(o.pos)) o.card.classList.remove('is-out-of-bounds');
          else o.card.classList.add('is-out-of-bounds');
        });
        // 카운트 재계산
        var count = document.querySelector('.centers-list-count strong');
        if (count) {
          var visible = 0;
          document.querySelectorAll('.center-card').forEach(function (c) {
            if (!c.classList.contains('is-out-of-bounds')) visible++;
          });
          count.textContent = visible;
        }
        searchHereBtn.hidden = true;
      });
    }
  }

  function markerStyle(selected, isActive) {
    if (selected) {
      return 'display:inline-flex;align-items:center;gap:4px;padding:5px 12px;' +
        'background:var(--color-primary);border:2.5px solid var(--color-primary);' +
        'border-radius:14px;color:#fff;font-size:12px;font-weight:700;' +
        'box-shadow:0 4px 8px rgba(63,48,233,0.45);cursor:pointer;' +
        'transition:all 200ms ease;';
    }
    var bg = isActive ? '#fff' : '#E5E7EB';
    var bd = isActive ? 'var(--color-primary)' : 'var(--color-border)';
    return 'display:inline-flex;align-items:center;justify-content:center;' +
      'width:30px;height:30px;border-radius:50%;background:' + bg + ';' +
      'border:2.5px solid ' + bd + ';' +
      'box-shadow:0 2px 4px rgba(0,0,0,0.2);cursor:pointer;' +
      'transition:all 200ms ease;';
  }

  function markerInner(selected, isActive, name) {
    if (selected) return '<span>📍</span><span>' + escapeHtml(name) + '</span>';
    var color = isActive ? 'var(--color-primary)' : 'var(--color-text-tri)';
    return '<span style="color:' + color + ';font-size:14px;">📍</span>';
  }

  ready(function () {
    initRegionDropdown();
    initSortPill();
    initOnlyActiveToggle();
    initMap();
  });
})();
