/**
 * F0h — 카카오맵 SDK 초기화 + 마커/리스트 동기화.
 *
 * 페이지 모드:
 *  1) 리스트 페이지 (/centers): #center-map + .center-card[data-lat][data-lng] 다수
 *     - 각 카드에 대응하는 마커 생성 (좌표 있는 것만)
 *     - 마커 클릭 → 해당 카드 스크롤 + is-highlighted 클래스
 *     - 카드 hover → 해당 마커 확대 애니메이션
 *  2) 상세 페이지 (/centers/{id}): #center-map[data-single-lat][data-single-lng]
 *     - 단일 마커 + infoWindow (센터명)
 *
 * SDK 미로드(appkey 미설정 or 로드 실패) 시 아무 것도 안 함 — 템플릿의 .map-fallback 이 문구 표시.
 */
(function () {
  'use strict';

  function ready(fn) {
    if (document.readyState !== 'loading') fn();
    else document.addEventListener('DOMContentLoaded', fn);
  }

  function initMap() {
    var mapEl = document.getElementById('center-map');
    if (!mapEl) return;

    // Kakao SDK 미로드 (appkey 없거나 네트워크 실패) → 조용히 종료
    if (typeof kakao === 'undefined' || !kakao.maps) {
      return;
    }

    kakao.maps.load(function () {
      var singleLat = mapEl.getAttribute('data-single-lat');
      var singleLng = mapEl.getAttribute('data-single-lng');
      if (singleLat && singleLng && singleLat !== 'null' && singleLng !== 'null') {
        initDetailMap(mapEl, parseFloat(singleLat), parseFloat(singleLng),
          mapEl.getAttribute('data-single-name') || '');
      } else {
        initListMap(mapEl);
      }
    });
  }

  function initDetailMap(mapEl, lat, lng, name) {
    var center = new kakao.maps.LatLng(lat, lng);
    var map = new kakao.maps.Map(mapEl, { center: center, level: 4 });
    var marker = new kakao.maps.Marker({ position: center, map: map });
    if (name) {
      var iw = new kakao.maps.InfoWindow({
        content: '<div style="padding:6px 10px;font-size:13px;">' + escapeHtml(name) + '</div>'
      });
      iw.open(map, marker);
    }
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

    // 좌표 있는 카드 없으면 경기도 중앙 기준 지도만 표시
    var defaultCenter = new kakao.maps.LatLng(37.4138, 127.5183); // 경기도 중앙
    var map = new kakao.maps.Map(mapEl, { center: defaultCenter, level: 10 });

    if (validCards.length === 0) return;

    var bounds = new kakao.maps.LatLngBounds();
    var markers = [];

    validCards.forEach(function (item) {
      var pos = new kakao.maps.LatLng(item.lat, item.lng);
      var marker = new kakao.maps.Marker({ position: pos, map: map });
      bounds.extend(pos);

      // 마커 클릭 → 카드 스크롤 + 하이라이트
      kakao.maps.event.addListener(marker, 'click', function () {
        highlightCard(item.card);
      });

      // 카드 hover → 마커 이미지 강조 (setZIndex + 살짝 offset)
      item.card.addEventListener('mouseenter', function () {
        marker.setZIndex(999);
        // 살짝 확대 효과: 카드 자체 클래스도 함께
        item.card.classList.add('is-hover');
      });
      item.card.addEventListener('mouseleave', function () {
        marker.setZIndex(1);
        item.card.classList.remove('is-hover');
      });

      markers.push({ marker: marker, card: item.card });
    });

    map.setBounds(bounds);
  }

  function highlightCard(card) {
    // 기존 하이라이트 제거
    document.querySelectorAll('.center-card.is-highlighted').forEach(function (el) {
      el.classList.remove('is-highlighted');
    });
    card.classList.add('is-highlighted');
    card.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  ready(initMap);
})();
