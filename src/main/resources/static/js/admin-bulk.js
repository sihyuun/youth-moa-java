/*
 * A8 admin-bulk-csv (2026-09-17) — bulk selection + action bar JS.
 *
 * 정책 (spec §4-3 · POLICY.md):
 *  - QB: 페이지 이동 시 selection state 초기화 (DOM 내부 Set 만 관리, 저장 X)
 *  - Qn-3: 전체 선택 = 현재 페이지만 (prototype toggleProgramSelectAll 정합)
 *  - 상단 CSV 버튼: 현재 querystring 유지, 선택 있으면 ids 우선
 *  - 하단 CSV 버튼: 선택된 ids 만 export
 *  - form submit = 302 redirect + flash (HTMX 미사용)
 */
(function () {
    'use strict';

    // === Selection state (per page) ===
    const selected = new Set();
    const bar = document.querySelector('.admin-bulk-action-bar');
    if (!bar) return;

    const domain = bar.dataset.bulkDomain;
    const csvBase = bar.dataset.csvBase || '';
    const countEl = bar.querySelector('[data-bulk-count]');
    const form = document.querySelector('[data-bulk-form]');
    const modal = document.querySelector('[data-bulk-modal]');
    const modalTitle = modal ? modal.querySelector('[data-bulk-modal-title]') : null;
    const modalMsg = modal ? modal.querySelector('[data-bulk-modal-msg]') : null;
    const modalInput = modal ? modal.querySelector('[data-bulk-modal-input]') : null;
    const modalSelect = modal ? modal.querySelector('[data-bulk-modal-select]') : null;
    const modalConfirm = modal ? modal.querySelector('[data-bulk-modal-confirm]') : null;
    const modalCancel = modal ? modal.querySelector('[data-bulk-modal-cancel]') : null;

    // === Actions map ===
    const actionMap = {
        users: {
            deactivate: { path: '/admin/users/bulk/deactivate', label: '차단', requiresReason: true, promptMsg: '선택한 사용자를 차단할까요?' },
            reactivate: { path: '/admin/users/bulk/reactivate', label: '재활성화', promptMsg: '선택한 사용자를 재활성화할까요?' },
            role: { path: '/admin/users/bulk/role', label: '권한 변경', requiresRole: true, promptMsg: '선택한 사용자의 권한을 변경할까요?' }
        },
        programs: {
            deactivate: { path: '/admin/programs/bulk/deactivate', label: '운영 중단', promptMsg: '선택한 프로그램을 운영 중단할까요? 사용자 목록에서 숨겨져요.' },
            reactivate: { path: '/admin/programs/bulk/reactivate', label: '재활성화', promptMsg: '선택한 프로그램을 재활성화할까요?' }
        },
        applications: {
            approve: { path: null, label: '일괄 승인', promptMsg: '선택한 신청을 모두 승인할까요?' }
        }
    };

    // programs.bulk/approve path 는 programId 를 필요로 하므로 data attribute 로 받는다
    const applicationsBulkApprovePath = bar.dataset.approvePath; // ex: /admin/programs/7/applications/bulk/approve
    if (actionMap.applications && applicationsBulkApprovePath) {
        actionMap.applications.approve.path = applicationsBulkApprovePath;
    }

    // === UI update ===
    function updateUi() {
        const n = selected.size;
        if (countEl) countEl.textContent = String(n);
        bar.setAttribute('data-active', n > 0 ? 'true' : 'false');
    }

    // === Checkbox binding ===
    document.querySelectorAll('[data-bulk-checkbox]').forEach(cb => {
        cb.addEventListener('change', () => {
            const id = cb.value;
            if (cb.checked) selected.add(id);
            else selected.delete(id);
            updateUi();
            syncSelectAll();
        });
    });

    // 전체 선택 헤더 checkbox
    const selectAll = document.querySelector('[data-bulk-select-all]');
    function syncSelectAll() {
        if (!selectAll) return;
        const boxes = Array.from(document.querySelectorAll('[data-bulk-checkbox]'));
        const total = boxes.length;
        const checked = boxes.filter(b => b.checked).length;
        selectAll.checked = total > 0 && checked === total;
        selectAll.indeterminate = checked > 0 && checked < total;
    }
    if (selectAll) {
        selectAll.addEventListener('change', () => {
            const on = selectAll.checked;
            document.querySelectorAll('[data-bulk-checkbox]').forEach(cb => {
                cb.checked = on;
                if (on) selected.add(cb.value);
                else selected.delete(cb.value);
            });
            updateUi();
        });
    }

    // 해제
    const clearBtn = bar.querySelector('[data-bulk-clear]');
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            selected.clear();
            document.querySelectorAll('[data-bulk-checkbox]').forEach(cb => cb.checked = false);
            if (selectAll) { selectAll.checked = false; selectAll.indeterminate = false; }
            updateUi();
        });
    }

    // === Modal helpers ===
    function openModal({ title, msg, requiresReason, requiresRole, onConfirm }) {
        if (!modal) { onConfirm(null, null); return; }
        modalTitle.textContent = title;
        modalMsg.textContent = msg;
        if (requiresReason) { modalInput.hidden = false; modalInput.value = ''; }
        else { modalInput.hidden = true; }
        if (requiresRole) { modalSelect.hidden = false; }
        else { modalSelect.hidden = true; }
        modal.setAttribute('data-open', 'true');
        const cleanup = () => {
            modal.setAttribute('data-open', 'false');
            modalConfirm.removeEventListener('click', confirmH);
            modalCancel.removeEventListener('click', cancelH);
        };
        const confirmH = () => {
            const reason = requiresReason ? (modalInput.value || '').trim() : null;
            const role = requiresRole ? modalSelect.value : null;
            if (requiresReason && !reason) { alert('사유를 입력해주세요.'); return; }
            cleanup();
            onConfirm(reason, role);
        };
        const cancelH = () => cleanup();
        modalConfirm.addEventListener('click', confirmH);
        modalCancel.addEventListener('click', cancelH);
    }

    // === Action buttons ===
    bar.querySelectorAll('[data-bulk-action]').forEach(btn => {
        btn.addEventListener('click', (ev) => {
            ev.preventDefault();
            const action = btn.dataset.bulkAction;
            if (selected.size === 0) { alert('선택된 항목이 없어요.'); return; }

            if (action === 'csv-selected') {
                const ids = Array.from(selected).join(',');
                const url = csvBase + (csvBase.indexOf('?') >= 0 ? '&' : '?') + 'ids=' + encodeURIComponent(ids);
                window.location.href = url;
                return;
            }

            const cfg = (actionMap[domain] || {})[action];
            if (!cfg || !cfg.path) return;

            openModal({
                title: cfg.label,
                msg: cfg.promptMsg,
                requiresReason: !!cfg.requiresReason,
                requiresRole: !!cfg.requiresRole,
                onConfirm: (reason, role) => submitBulk(cfg.path, reason, role)
            });
        });
    });

    function submitBulk(path, reason, role) {
        if (!form) return;
        form.action = path;
        form.querySelector('input[name="ids"]').value = Array.from(selected).join(',');
        const reasonInput = form.querySelector('input[name="reason"]');
        if (reasonInput) reasonInput.value = reason || '';
        const roleInput = form.querySelector('input[name="role"]');
        if (roleInput) roleInput.value = role || '';
        form.submit();
    }

    updateUi();
})();
