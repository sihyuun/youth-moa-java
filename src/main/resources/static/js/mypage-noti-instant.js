/*
 * mypage-noti-instant.js — F0f-fix-4
 *
 * 알림 채널 체크박스 change 이벤트마다 즉시 POST /mypage/notifications 로 저장.
 * 성공 시 window.Toast.show("저장되었어요"), 실패 시 error variant.
 * CSRF 는 <meta name="_csrf">, <meta name="_csrf_header"> 에서 읽어 헤더로 전달.
 */
(function () {
    'use strict';

    var form = document.getElementById('notiInstantForm');
    if (!form) return;

    function csrfToken() {
        var m = document.querySelector('meta[name="_csrf"]');
        return m ? m.getAttribute('content') : '';
    }

    function csrfHeader() {
        var m = document.querySelector('meta[name="_csrf_header"]');
        return m ? m.getAttribute('content') : 'X-CSRF-TOKEN';
    }

    function collectState() {
        var params = new URLSearchParams();
        var boxes = form.querySelectorAll('input[data-noti-toggle]');
        boxes.forEach(function (b) {
            // Spring boolean binding: 체크된 것만 true 전송 (미체크는 false 로 바인딩)
            params.append(b.name, b.checked ? 'true' : 'false');
        });
        return params;
    }

    function toast(msg, variant) {
        if (window.Toast && typeof window.Toast.show === 'function') {
            window.Toast.show(msg, variant || 'success');
        }
    }

    function save() {
        var body = collectState();
        var headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        var token = csrfToken();
        if (token) headers[csrfHeader()] = token;

        fetch('/mypage/notifications', {
            method: 'POST',
            headers: headers,
            body: body.toString(),
            credentials: 'same-origin'
        }).then(function (res) {
            if (res.ok || res.status === 302 || res.status === 200) {
                toast('저장되었어요.', 'success');
            } else {
                toast('저장에 실패했어요. 다시 시도해주세요.', 'error');
            }
        }).catch(function () {
            toast('저장에 실패했어요. 다시 시도해주세요.', 'error');
        });
    }

    form.querySelectorAll('input[data-noti-toggle]').forEach(function (input) {
        input.addEventListener('change', function () {
            // 부모 row 의 .is-on 클래스 즉시 토글 (토글 스위치 시각 반영)
            var row = input.closest('.noti-channel-row, .noti-item-row, .mypage-noti-row');
            if (row) row.classList.toggle('is-on', input.checked);
            save();
        });
    });
})();
