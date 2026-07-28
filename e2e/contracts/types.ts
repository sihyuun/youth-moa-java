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
     * prototype 을 의도적으로 벗어나기로 결정한 항목.
     * 값이 있으면 검사에서 제외되며, 사유가 기록으로 남는다.
     */
    deviation?: string;
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
