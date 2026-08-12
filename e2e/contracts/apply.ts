/**
 * 프로그램 신청 폼 화면 (`/programs/{id}/apply`) 디자인 계약.
 *
 * 도입 목적 (2026-08-11): 사용자 트랙 남은 9화면 계약 시리즈 4번째. login·signup·notices·
 * notice-detail 에 이은 인증 필요 화면 첫 계약.
 *
 * 추출 기준: `docs/00_assets/prototype.tsx` `ProgramApply` L1125~1214
 *   (참고: html 라인 = tsx 라인 + 35, 두 파일은 같은 소스이므로 tsx 만 인용)
 *
 * 실행 조건 (visual-apply.spec.ts):
 *  - auth 필요 → `helpers.login(page, seedEmail(30))` 로 fresh 유저 선로그인 후 `/programs/{id}/apply`
 *  - path 는 `program-detail.ts` 와 동일하게 seed program id 3 (마음건강 힐링 캠프, ACTIVE, seed30 미신청)
 *  - 초기 렌더 시점 step=1 → 필드 크기·컨테이너 계약은 모두 step1 카드 노출 상태에서 측정
 *  - step2·3 카드는 `.apply-step-card:not(.is-active) { display:none }` 라 정적 계약 대상 아님
 *    (visual-apply.spec.ts 별도 인터랙션 블록으로 step 전환 후 스텝별 요소 존재만 확인)
 *
 * 특이 사항:
 *  - prototype 은 폼 내부 필드가 3개 카드로 순차 표시 (`step===1/2/3` conditional)
 *    → 구현은 단일 form 안에 3개 `.apply-step-card` 를 두고 JS 가 `.is-active` 토글로 show/hide.
 *    계약은 폼 아키텍처(위저드) 정합만 검증하고 필드 값 유효성은 apply.spec.ts 가 커버.
 *  - Summary 카드는 항상 노출 (step 무관) → 크기·구조 정적 계약 가능.
 *  - prototype 신청자 정보 readonly 박스는 `inputRO` 스타일 (proto L60) — height 46 · border · padding.
 *    구현 `.apply-applicant-value` 는 height 46 (proto 정합) 이라 그대로 계약 반영.
 *  - 뒤로가기 아이콘 버튼은 prototype 이 `arrowL` SVG · 구현은 `&larr;` HTML 엔티티(문자).
 *    → POLICY P-3 위반. `deferred` 로 표시하고 별도 티켓에서 SVG fragment 이식.
 *  - 개인정보 동의 체크는 prototype 이 원형 라디오 (`custom div`) · 구현은 `input[checkbox]:checked+span::after`.
 *    UX 동등이므로 정합 취급.
 *  - **F0c-remainder Q5 (F0c-dynamic-fields)** 는 admin 트랙 선행 필요 → 계약 반영 유예.
 */

import type { ScreenContract } from './types';

export const applyContract: ScreenContract = {
    screen: 'apply',
    path: '/programs/3/apply',
    source: 'prototype.tsx ProgramApply L1125~1214 · 2026-08-11 신설',
    viewport: { width: 1440, height: 900 },
    checks: [
        // ══════════════════════════════════════════════════════
        // 컨테이너 · 레이아웃 (proto L1138 maxWidth:700, padding:36 0 48)
        // ══════════════════════════════════════════════════════
        {
            id: 'container.max-width',
            desc: 'apply-inner max-width 700 (prototype L1138)',
            selector: '.apply-inner',
            kind: 'box',
            prop: 'width',
            expected: 700,
            tolerance: 1,
            proto: 'tsx L1138 maxWidth:700',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'header.absent',
            desc: '공용 헤더 미렌더 (prototype 정합, Q-1(A) 확정 2026-08-11)',
            selector: 'header.header, .site-header',
            kind: 'count',
            expected: 0,
            proto: 'tsx L1137~1213 Header 미렌더',
            severity: 'P1',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // 뒤로가기 아이콘 버튼 (proto L1139 38x38 · arrowL SVG)
        // ══════════════════════════════════════════════════════
        {
            id: 'back-btn.exists',
            desc: '뒤로가기 버튼 존재',
            selector: '.apply-back-btn',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1139',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'back-btn.width',
            desc: '뒤로가기 버튼 width 38 (prototype L1139)',
            selector: '.apply-back-btn',
            kind: 'box',
            prop: 'width',
            expected: 38,
            tolerance: 1,
            proto: 'tsx L1139 width:38',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'back-btn.height',
            desc: '뒤로가기 버튼 height 38',
            selector: '.apply-back-btn',
            kind: 'box',
            prop: 'height',
            expected: 38,
            tolerance: 1,
            proto: 'tsx L1139 height:38',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'back-btn.border-radius',
            desc: '뒤로가기 버튼 radius 9 (prototype)',
            selector: '.apply-back-btn',
            kind: 'css',
            prop: 'border-radius',
            expected: '9px',
            proto: 'tsx L1139 borderRadius:9',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'back-btn.icon.svg',
            desc: '뒤로가기 아이콘 SVG (POLICY P-3, 문자 대체 금지)',
            selector: '.apply-back-btn svg',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1140 <Icon n="arrowL"/> — SVG 강제',
            severity: 'P1',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // 타이틀 (proto L1142 fontSize:26/700 · textAlign:center)
        // ══════════════════════════════════════════════════════
        {
            id: 'title.text',
            desc: '타이틀 텍스트 "프로그램 신청"',
            selector: '.apply-title',
            kind: 'text',
            expected: '프로그램 신청',
            proto: 'tsx L1142',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'title.font-size',
            desc: '타이틀 font-size 26',
            selector: '.apply-title',
            kind: 'css',
            prop: 'font-size',
            expected: '26px',
            proto: 'tsx L1142 fontSize:26',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'title.font-weight',
            desc: '타이틀 굵기 700',
            selector: '.apply-title',
            kind: 'css',
            prop: 'font-weight',
            expected: '700',
            proto: 'tsx L1142 fontWeight:700',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'title.text-align',
            desc: '타이틀 중앙 정렬',
            selector: '.apply-title',
            kind: 'css',
            prop: 'text-align',
            expected: 'center',
            proto: 'tsx L1142 textAlign:center',
            severity: 'P2',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // 스텝 프로그레스 (3단계 · proto L1144~1156)
        // ══════════════════════════════════════════════════════
        {
            id: 'stepper.count',
            desc: '스텝 3개 (신청자 정보 · 추가 정보 · 약관 동의)',
            selector: '.apply-step',
            kind: 'count',
            expected: 3,
            proto: 'tsx L1129 STEPS ["신청자 정보","추가 정보","약관 동의"]',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'stepper.step1.label',
            desc: '스텝1 라벨',
            selector: '.apply-step[data-step="1"] .apply-step-label',
            kind: 'text',
            expected: '신청자 정보',
            proto: 'tsx L1129',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'stepper.step2.label',
            desc: '스텝2 라벨',
            selector: '.apply-step[data-step="2"] .apply-step-label',
            kind: 'text',
            expected: '추가 정보',
            proto: 'tsx L1129',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'stepper.step3.label',
            desc: '스텝3 라벨',
            selector: '.apply-step[data-step="3"] .apply-step-label',
            kind: 'text',
            expected: '약관 동의',
            proto: 'tsx L1129',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'stepper.circle.width',
            desc: '스텝 원형 인디케이터 width 34 (proto L1148)',
            selector: '.apply-step-circle',
            kind: 'box',
            prop: 'width',
            expected: 34,
            tolerance: 1,
            proto: 'tsx L1148 width:34',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'stepper.circle.height',
            desc: '스텝 원형 인디케이터 height 34',
            selector: '.apply-step-circle',
            kind: 'box',
            prop: 'height',
            expected: 34,
            tolerance: 1,
            proto: 'tsx L1148 height:34',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'stepper.connector.count',
            desc: '스텝 사이 연결선 2개',
            selector: '.apply-step-connector',
            kind: 'count',
            expected: 2,
            proto: 'tsx L1153 n<3 && connector',
            severity: 'P1',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // Summary 카드 (proto L1158 · 항상 노출)
        // ══════════════════════════════════════════════════════
        {
            id: 'summary.exists',
            desc: 'Summary 카드 존재',
            selector: '.apply-summary-card',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1158',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'summary.padding',
            desc: 'Summary 카드 padding 16 (proto L1158)',
            selector: '.apply-summary-card',
            kind: 'css',
            prop: 'padding',
            expected: '16px',
            proto: 'tsx L1158 padding:16',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'summary.gap',
            desc: 'Summary 카드 gap 16 (썸네일↔메타)',
            selector: '.apply-summary-card',
            kind: 'css',
            prop: 'gap',
            expected: '16px',
            proto: 'tsx L1158 gap:16',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'summary.image.width',
            desc: 'Summary 썸네일 width 64',
            selector: '.apply-summary-image',
            kind: 'box',
            prop: 'width',
            expected: 64,
            tolerance: 1,
            proto: 'tsx L1159 width:64',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'summary.image.height',
            desc: 'Summary 썸네일 height 64',
            selector: '.apply-summary-image',
            kind: 'box',
            prop: 'height',
            expected: 64,
            tolerance: 1,
            proto: 'tsx L1159 height:64',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'summary.title.font-size',
            desc: 'Summary 프로그램 제목 font-size 15',
            selector: '.apply-summary-title',
            kind: 'css',
            prop: 'font-size',
            expected: '15px',
            proto: 'tsx L1160 fontSize:15',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'summary.badge.exists',
            desc: 'Summary 상태 뱃지 존재',
            selector: '.apply-summary-badge',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1160 <Badge/>',
            severity: 'P1',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // 단계 카드 (surface, radius-lg, border-light, shadow · proto L1162)
        // 초기 렌더 시 step1 만 노출 (data-step-card="1" 이 .is-active)
        // ══════════════════════════════════════════════════════
        {
            id: 'step-card.count',
            desc: '단계 카드 3개 (JS 가 show/hide)',
            selector: '.apply-step-card',
            kind: 'count',
            expected: 3,
            proto: 'tsx L1163/1181/1188 step===1/2/3',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'step-card.active.padding',
            desc: '활성 단계 카드 padding 24 26 (proto L1162)',
            selector: '.apply-step-card.is-active',
            kind: 'css',
            prop: 'padding',
            expected: '24px 26px',
            proto: 'tsx L1162 padding:"24px 26px"',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'step-card.active.border-radius',
            desc: '활성 단계 카드 radius-lg (12)',
            selector: '.apply-step-card.is-active',
            kind: 'css',
            prop: 'border-radius',
            expected: '12px',
            proto: 'tsx L1162 borderRadius:T.radius (lg)',
            severity: 'P2',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // Step 1 — 신청자 정보 (readonly · proto L1163~1179)
        // ══════════════════════════════════════════════════════
        {
            id: 'step1.header.title',
            desc: '스텝1 카드 헤더 타이틀',
            selector: '.apply-step-card[data-step-card="1"] .apply-step-card-title',
            kind: 'text',
            expected: '신청자 정보',
            proto: 'tsx L1166',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'step1.auto-badge.text',
            desc: '"회원정보 자동입력" 뱃지 텍스트',
            selector: '.apply-auto-badge',
            kind: 'text',
            expected: '회원정보 자동입력',
            proto: 'tsx L1167',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'step1.auto-badge.font-size',
            desc: '자동입력 뱃지 font-size 11.5',
            selector: '.apply-auto-badge',
            kind: 'css',
            prop: 'font-size',
            expected: '11.5px',
            proto: 'tsx L1167 fontSize:11.5',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'step1.applicant-fields.count',
            desc: '신청자 정보 필드 3개 (이름·핸드폰·이메일)',
            selector: '.apply-step-card[data-step-card="1"] .apply-applicant-field',
            kind: 'count',
            expected: 3,
            proto: 'tsx L1170 [이름·핸드폰·이메일]',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'step1.applicant.value.height',
            desc: 'readonly 값 박스 height 46 (구현) — proto 는 inputRO 로 회색 배경',
            selector: '.apply-applicant-value',
            kind: 'box',
            prop: 'height',
            expected: 46,
            tolerance: 1,
            proto: 'tsx L60 inputRO height:46 (구현 정합)',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'step1.applicant.gap',
            desc: '신청자 정보 필드 간 gap 14 (proto L1169)',
            selector: '.apply-applicant-fields',
            kind: 'css',
            prop: 'gap',
            expected: '14px',
            proto: 'tsx L1169 gap:14',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'step1.mypage-note.text',
            desc: '마이페이지 안내 문구',
            selector: '.apply-mypage-note',
            kind: 'text',
            expected: '정보 수정은 마이페이지 › 개인 정보 수정에서 가능합니다.',
            proto: 'tsx L1178',
            severity: 'P2',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // 네비게이션 (이전 flex:1 / 다음 flex:2 · proto L1204~1208)
        // ══════════════════════════════════════════════════════
        {
            id: 'nav.gap',
            desc: '네비게이션 버튼 간 gap 10 (proto L1204)',
            selector: '.apply-nav',
            kind: 'css',
            prop: 'gap',
            expected: '10px',
            proto: 'tsx L1204 gap:10',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'nav.next.exists',
            desc: '"다음" 버튼 존재 (초기 step=1 상태)',
            selector: '#applyNavNext',
            kind: 'exists',
            expected: true,
            proto: 'tsx L1207',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'nav.next.text',
            desc: '"다음" 버튼 텍스트',
            selector: '#applyNavNext',
            kind: 'text',
            expected: '다음',
            proto: 'tsx L1207',
            severity: 'P0',
            states: ['auth'],
        },
        {
            id: 'nav.next.height',
            desc: '다음 버튼 Btn size:l → height 50 (prototype L85 sizes.l)',
            selector: '#applyNavNext',
            kind: 'box',
            prop: 'height',
            expected: 50,
            tolerance: 1,
            proto: 'tsx L85 sizes.l h:50',
            severity: 'P1',
            states: ['auth'],
        },
        {
            id: 'nav.next.font-size',
            desc: '다음 버튼 font-size 16 (Btn size:l)',
            selector: '#applyNavNext',
            kind: 'css',
            prop: 'font-size',
            expected: '16px',
            proto: 'tsx L85 sizes.l fs:16',
            severity: 'P2',
            states: ['auth'],
        },
        {
            id: 'nav.next.border-radius',
            desc: '다음 버튼 radius 8 (Btn)',
            selector: '#applyNavNext',
            kind: 'css',
            prop: 'border-radius',
            expected: '8px',
            proto: 'tsx L97 borderRadius:8',
            severity: 'P2',
            states: ['auth'],
        },

        // ══════════════════════════════════════════════════════
        // 서버 알림 (prototype 없음 · 구현 추가)
        // 계약 대상 아님 — POLICY P-5 로 apply.md §5 에만 기록
        // ══════════════════════════════════════════════════════

        // ══════════════════════════════════════════════════════
        // Footer (proto L1211 <Footer/>)
        // ══════════════════════════════════════════════════════
        {
            id: 'footer.exists',
            desc: '푸터 존재',
            selector: 'footer.site-footer, footer',
            kind: 'count',
            expected: 1,
            proto: 'tsx L1211 <Footer/>',
            severity: 'P1',
            states: ['auth'],
        },
    ],
};
