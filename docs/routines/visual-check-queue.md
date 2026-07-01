# 시각 확인 큐 루틴 프롬프트

- **cron**: `50 23 * * *` UTC = 매일 KST 08:50
- **모델**: claude-sonnet-4-6
- **MCP**: Notion
- **allowed_tools**: Read, Bash, Glob, Grep, (Notion create-pages)

---

## 프롬프트

개인 PC (Mac) 에서 처리해야 할 시각 확인 항목을 정리해 Notion 페이지로 생성한다. 회사 PC (Windows) 에서 자동 검증 불가능한 UI 변경 사항이 대상.

### 1. KST 날짜

```bash
date -u
```
+9h = KST.

### 2. 입력 수집

**A. STATE.md 의 "개인 PC 시각 확인 항목" 섹션**

```bash
cat docs/STATE.md | sed -n '/## .*개인 PC.*시각/,/^##/p'
```

메모리에 이미 기록된 미해결 항목 리스트 확보.

**B. 최근 24h 의 UI 관련 변경**

```bash
# 지난 24h 커밋 중 templates/ css/ 관련 파일 변경
git log --since="24 hours ago" --name-only --oneline -- \
  'src/main/resources/templates/**' \
  'src/main/resources/static/css/**' \
  'src/main/resources/static/js/**'
```

새로운 UI 변경 있으면 시각 확인 큐에 자동 추가.

**C. 열린 PR 중 UI 변경 포함**

```bash
git branch -r --list 'origin/feature/*' 'origin/fix/*' | head -20
# 각 브랜치에 대해 main 과 diff:
# git diff main..origin/<branch> --name-only | grep -E 'templates|static/(css|js)'
```

### 3. 카테고리 분류

| 카테고리 | 예시 |
|---|---|
| 🎨 레이아웃·CSS | 카드 배치, 반응형, 여백 |
| 🖱️ 인터랙션 | 호버, 클릭, HTMX 결과 |
| 🔐 인증 흐름 | 비로그인 리다이렉트, 세션 만료 |
| 📱 반응형 | 모바일 뷰포트 |
| 🔤 텍스트·라벨 | 카피, 안내 문구 |

### 4. Notion 페이지 생성

- `title`: `MM-DD 시각 확인 큐`
- `icon`: `👁️`
- `content`:

```markdown
> 📌 **한눈에**
> 1. 미처리 N건 (STATE.md 기존 M + 신규 K)
> 2. 최우선 항목 (예: PR #7 D2 즐겨찾기 토글 3개 미확인)
> 3. 개인 PC 접속 시 예상 소요 시간

## 즉시 확인 (개인 PC 접속 시 순서)

- [ ] **[PR #7]** 카드 목록 ★ overlay 위치 (이미지 우상단) + 호버 transform
- [ ] **[PR #7]** ★ 클릭 시 페이지 리로드 없이 amber 토글
- [ ] **[F0a]** 로그인 페이지 prototype 일치 (로고, 400px, "로그인" h2)
- [ ] **[F0a]** 푸터 fragment 정상 렌더
...

## 신규 (지난 24h 커밋)

- [ ] `feature/F0f-list-filter-redesign` — 목록 필터 UI (사이드바 체크박스 + FilterPopChip)
  - 확인 포인트: 필터 선택 시 결과 실시간 갱신, chip 클릭 시 팝오버 위치

## 잠재 회귀 확인

main.css 자기참조 변수 (`--color-text-tri`) 이슈 미해결 상태:
- [ ] 홈 카드 하단 날짜 텍스트 색 unset 여부

## 각 항목 확인 절차

1. Mac 에서 `./gradlew bootRun`
2. http://localhost:8080 접속
3. 위 체크박스 순회
4. 통과 → STATE.md "개인 PC 시각 확인 항목" 에서 제거 (다음 wrap-up 시)
5. 실패 → PR 코멘트 또는 fix 브랜치 생성

## 참고

- 회사 PC (Windows) 에서는 위 항목 자동 검증 불가. Mac 브라우저 필수.
- STATE.md 원본 링크: `docs/STATE.md`
```

### 5. 특수 케이스

- 미처리 항목이 하나도 없음 → `> 📌 모든 시각 확인 완료 — 신규 UI 변경도 없음. 오늘 이 항목 스킵 가능.`
- 개인 PC 미접속 여러 날 누적 → `누적 미처리 N일` 경고 표시

### 6. 결과 출력

Notion 페이지 URL 만 출력.

### 7. 코드 변경 금지

Read/Bash/Glob/Grep/Notion MCP 만 사용.
