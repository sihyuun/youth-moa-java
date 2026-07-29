/**
 * 디자인 계약 공통 타입.
 *
 * 계약 = prototype 에서 추출한 "기계가 검사할 수 있는 기대값" 의 목록.
 * 각 항목은 반드시 prototype 라인 출처(`proto`)를 갖는다 — 출처 없는 수치는 추측이므로 계약에 넣지 않는다.
 *
 * 배경·사용법: docs/design-contracts/README.md
 */

/** 검사 종류 */
export type CheckKind =
    /** getBoundingClientRect 의 width/height (px) */
    | 'box'
    /** getComputedStyle 의 임의 속성 */
    | 'css'
    /** textContent (trim 후 완전 일치) */
    | 'text'
    /** 셀렉터 매칭 개수 */
    | 'count'
    /** 존재 여부 (expected: true/false) */
    | 'exists';

export type Severity = 'P0' | 'P1' | 'P2';

/** 로그인 상태 — 해당 검사를 어느 상태에서 실행할지 */
export type AuthState = 'anon' | 'auth';

export interface Check {
    /** 안정적인 식별자. 실패 리포트·예외 처리에서 이 값으로 참조한다 */
    id: string;
    /** 사람이 읽는 설명 */
    desc: string;
    /** CSS 셀렉터. 여러 개 매칭 시 nth 로 좁힌다 */
    selector: string;
    kind: CheckKind;
    /** kind='box' → 'width'|'height' / kind='css' → CSS 속성명 */
    prop?: string;
    expected: string | number | boolean;
    /** box 검사 허용 오차 (px). 기본 1 */
    tolerance?: number;
    /** prototype 출처 — 예: 'tsx L526' */
    proto: string;
    severity: Severity;
    /** 실행할 로그인 상태. 기본 ['anon'] */
    states?: AuthState[];
    /**
     * prototype 을 **영구히** 벗어나기로 결정한 항목 (= 앞으로도 안 맞출 것).
     * 값이 있으면 검사에서 제외되며, 사유가 기록으로 남는다.
     * 전 화면 공통 정책이면 `docs/design-contracts/POLICY.md` 의 항목 번호를 인용한다.
     */
    deviation?: string;

    /**
     * prototype 대로 맞출 예정이지만 **이번 범위가 아닌** 항목 (= 나중에 할 것).
     * 값에는 담당 티켓·스펙 경로를 적는다 (예: 'docs/specs/F0f-calendar-view.md').
     *
     * `deviation` 과 구분하는 이유: 이월 항목을 deviation 으로 적으면 "안 하기로 했다" 로
     * 의도가 왜곡되고, 반대로 갭으로 남겨두면 그 화면의 갭이 영구히 0 이 되지 않아
     * 스크린샷 baseline 등록·블로킹 승격을 막는다. 검사에서는 빠지고 리포트에는 별도 절로 실린다.
     */
    deferred?: string;
}

export interface ScreenContract {
    /** 화면 이름 */
    screen: string;
    /** 진입 경로 */
    path: string;
    /** 계약 추출 기준 */
    source: string;
    /** 측정 뷰포트 */
    viewport: { width: number; height: number };
    checks: Check[];
}
