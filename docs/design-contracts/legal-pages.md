# 법적 문서 3종 디자인 계약

> 대상: `/privacy` · `/terms` · `/email-policy`
> 정량 계약: [e2e/contracts/policy.ts](../../e2e/contracts/policy.ts)
> 스펙 원본: [docs/00_assets/HANDOFF.md](../00_assets/HANDOFF.md) L629~L652
> 신설: 260819

---

## 배경

`prototype.tsx` L468 에는 세 링크의 **이름 문자열만** 존재하고 페이지 컨텐츠·onClick·라우팅 모두 없다. 실제 페이지 스펙은 **wireframe(HANDOFF.md)** 이 유일한 근거.

## 공통 레이아웃 (데스크톱)

- 좌측 사이드바 220px + 우측 본문 1fr (`.policy-layout` grid).
- 활성 링크: 배경 `--color-primary-bg` + 1px `--color-primary` 테두리 + font-weight 600.
- h1 (`.policy-page-title`) 26/700, 시행일 13/textTri, 장 제목 16/700.

## 페이지별 특수 스펙

### 개인정보처리방침 (`/privacy`)
- 2번 섹션 "수집 항목" 은 **3열 테이블** (`.policy-table`): 구분 / 수집 항목 / 수집 목적.

### 이용약관 (`/terms`)
- 조문형 (제1조~제8조). 표 없음.

### 이메일 무단 수집거부 (`/email-policy`)
- privacy/terms 와 동일한 왼쪽 정렬 layout (`.policy-page-head` + `.policy-section`). 260819 통일성 조정 — wireframe L651 "centered" 는 사용자 지시로 이탈.
- 이메일 아이콘 원 72×72 (`--color-primary-bg` 배경) + 우상단 X 뱃지 28×28 (`--color-primary`). 260819 wireframe L649 "빨간 X (--color-error)" → 브랜드 정합성 위해 primary 로 변경 (사용자 지시).
- 페이지 이름 260819 변경: prototype/wireframe 원문 "이메일주소무단수집거부" → **"이메일 무단 수집거부"** (공백 포함, 가독성 개선).
- 안내 문안:
  > 본 사이트에 게시된 이메일 주소가 전자우편 수집 프로그램이나 그 밖의 기술적 장치를 이용하여
  > 무단으로 수집되는 것을 거부합니다. 이를 위반시 『정보통신망 이용 촉진 및 정보보호 등에 관한 법률』등에
  > 의해 처벌 받을 수 있습니다.

## 모바일 대응 (< 768px)

- 사이드바 대신 상단 sticky 타이틀 헤더 (뒤로가기 + 페이지 이름).
- 본문 아래 관련 문서 링크 카드 (현재 페이지 제외 2개).

## 회원가입 약관 모달 재사용

`user/signup.html` 의 약관보기 모달은 `/terms` · `/privacy` 를 fetch 하여 **`.policy-content` 만 추출** 해 주입한다. 사이드바·모바일 헤더·관련 문서 링크는 모달 컨텍스트에 부적절하므로 CSS 로 자연 배제(사이드바는 layout grid 밖), JS 로 `.policy-mobile-related` 명시 제거.

## 계약이 검사하지 않는 부분

- 아이콘의 실제 이메일 봉투 svg path 모양
- 사이드바 hover 색상 전환 애니메이션
- 모바일 뒤로가기 아이콘 heading 좌측 정렬

## 계약 통과 현황 (260819 신설 시점)

| 페이지 | 통과 | 갭 |
|---|---|---|
| privacy | 10/10 | 0 |
| terms | 8/8 | 0 |
| email-policy | 12/12 | 0 |
