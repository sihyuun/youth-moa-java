/*
 * F-signup-01: 휴대폰 인증 상태 머신.
 *
 * 상태: IDLE → SENDING → SENT → VERIFYING → VERIFIED / EXPIRED
 * - 3분(180s) 타이머, 만료 시 EXPIRED
 * - 재발송 30초 cooldown
 * - CSRF: meta[name="_csrf"] + meta[name="_csrf_header"] 값을 fetch 헤더에 부착
 *
 * 서버 미신뢰: phoneVerifiedHidden 은 UX 용. 서버는 세션 phoneVerifiedAt/Number 로 재검증.
 */
(function () {
  const phoneInput = document.getElementById('phone');
  const sendBtn = document.getElementById('btn-send-code');
  const verifyBtn = document.getElementById('btn-verify-code');
  const resendBtn = document.getElementById('btn-resend-code');
  const codeRow = document.getElementById('code-row');
  const codeInput = document.getElementById('verify-code');
  const timerEl = document.getElementById('code-timer');
  const badge = document.getElementById('phone-verified-badge');
  const msg = document.getElementById('phone-verify-msg');
  const hidden = document.getElementById('phoneVerifiedHidden');
  if (!phoneInput || !sendBtn) return;

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

  const CODE_TTL = 180;
  const RESEND_COOLDOWN = 30;

  let state = 'IDLE';
  let ttlTimerId = null;
  let cooldownTimerId = null;
  let remainingTtl = 0;
  let remainingCooldown = 0;

  function showMsg(text, isError) {
    msg.textContent = text;
    msg.hidden = !text;
    msg.classList.toggle('signup-field-ok', !isError);
  }

  function fmt(sec) {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return m + ':' + String(s).padStart(2, '0');
  }

  function setState(next) {
    state = next;
    if (next === 'VERIFIED') {
      badge.hidden = false;
      hidden.value = 'true';
      codeRow.hidden = true;
      sendBtn.textContent = '인증완료';
      sendBtn.disabled = true;
      stopTtl();
      stopCooldown();
      showMsg('인증이 완료되었습니다.', false);
    } else if (next === 'EXPIRED') {
      showMsg('인증 시간이 만료되었습니다. 다시 요청해주세요.', true);
      stopTtl();
      resendBtn.disabled = false;
    }
  }

  function stopTtl() {
    if (ttlTimerId) { clearInterval(ttlTimerId); ttlTimerId = null; }
  }
  function startTtl() {
    stopTtl();
    remainingTtl = CODE_TTL;
    timerEl.textContent = fmt(remainingTtl);
    ttlTimerId = setInterval(() => {
      remainingTtl--;
      if (remainingTtl <= 0) {
        timerEl.textContent = '0:00';
        setState('EXPIRED');
        return;
      }
      timerEl.textContent = fmt(remainingTtl);
    }, 1000);
  }

  function stopCooldown() {
    if (cooldownTimerId) { clearInterval(cooldownTimerId); cooldownTimerId = null; }
    resendBtn.textContent = '재전송';
  }
  function startCooldown() {
    stopCooldown();
    remainingCooldown = RESEND_COOLDOWN;
    resendBtn.disabled = true;
    resendBtn.textContent = '재전송 (' + remainingCooldown + ')';
    cooldownTimerId = setInterval(() => {
      remainingCooldown--;
      if (remainingCooldown <= 0) {
        stopCooldown();
        resendBtn.disabled = false;
        return;
      }
      resendBtn.textContent = '재전송 (' + remainingCooldown + ')';
    }, 1000);
  }

  function headers() {
    const h = { 'Content-Type': 'application/json' };
    if (csrfToken) h[csrfHeader] = csrfToken;
    return h;
  }

  async function sendCode() {
    const phone = (phoneInput.value || '').replace(/\D/g, '');
    if (!/^0\d{9,10}$/.test(phone)) {
      showMsg('올바른 휴대폰 번호를 입력해주세요.', true);
      return;
    }
    setState('SENDING');
    try {
      const res = await fetch('/api/phone/send-code', {
        method: 'POST',
        headers: headers(),
        body: JSON.stringify({ phone })
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        showMsg(data.error || '인증번호 발송에 실패했습니다.', true);
        setState('IDLE');
        return;
      }
      setState('SENT');
      codeRow.hidden = false;
      codeInput.value = '';
      codeInput.focus();
      startTtl();
      startCooldown();
      showMsg('인증번호를 발송했습니다. 3분 이내에 입력해주세요.', false);
    } catch (e) {
      showMsg('네트워크 오류가 발생했습니다.', true);
      setState('IDLE');
    }
  }

  async function verifyCode() {
    const phone = (phoneInput.value || '').replace(/\D/g, '');
    const code = (codeInput.value || '').trim();
    if (!/^\d{6}$/.test(code)) {
      showMsg('6자리 인증번호를 입력해주세요.', true);
      return;
    }
    setState('VERIFYING');
    try {
      const res = await fetch('/api/phone/verify-code', {
        method: 'POST',
        headers: headers(),
        body: JSON.stringify({ phone, code })
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok || data.ok === false) {
        showMsg(data.error || '인증번호가 올바르지 않습니다.', true);
        setState('SENT');
        return;
      }
      setState('VERIFIED');
    } catch (e) {
      showMsg('네트워크 오류가 발생했습니다.', true);
      setState('SENT');
    }
  }

  sendBtn.addEventListener('click', sendCode);
  verifyBtn?.addEventListener('click', verifyCode);
  resendBtn?.addEventListener('click', sendCode);

  // phone 값 변경 → 인증 상태 리셋
  phoneInput.addEventListener('input', () => {
    if (state === 'VERIFIED') {
      hidden.value = 'false';
      badge.hidden = true;
      sendBtn.disabled = false;
      sendBtn.textContent = '인증요청';
      state = 'IDLE';
      showMsg('', false);
    }
  });
})();
