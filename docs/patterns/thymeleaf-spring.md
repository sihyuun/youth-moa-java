# Thymeleaf / Spring Form 함정

> CLAUDE.md 에서 분리 (2026-07-28). 상시 준수 규칙이 아니라 **해당 영역을 건드릴 때 참조하는 사고 패턴 모음**이다.
> 여기 있는 항목은 전부 이 프로젝트에서 실제로 사고 난 것이다.

이 프로젝트에서 실제로 사고 났던 패턴 모음. 새 화면 작업 전 일독 권장.

### `th:field` 의 type=password 동작
`<input type="password" th:field="*{password}">` 는 **보안상 value 를 강제로 빈 문자열로 출력**. 검증 실패 후 폼 재표시 시 비밀번호 입력값이 사라지는 사고 발생.

```html
<!-- ❌ 검증 실패 후 value 가 비어짐 -->
<input type="password" th:field="*{password}">

<!-- ✅ name + th:value 수동 명시 → 값 보존 -->
<input type="password" id="password" name="password" th:value="*{password}">
```

`th:errors` 는 form 의 `th:object` 만 있으면 그대로 동작하므로 위 변경은 에러 표시에 영향 없음.

### `th:field` 가 checkbox id 를 자동 변경
`<input type="checkbox" th:field="*{termsAgreed}">` 의 실제 id 는 `termsAgreed1` 처럼 숫자 접미사가 붙음.
JS 에서 같은 폼 내 체크박스 제어 시 `getElementById('termsAgreed')` 는 null. **항상 name 으로 querySelector 사용**.

```javascript
// ❌ null
document.getElementById('termsAgreed')

// ✅
document.querySelector('input[type="checkbox"][name="termsAgreed"]')
```

### 정적 리소스 `static-path-pattern` 함정
`application.yml` 의 `spring.mvc.static-path-pattern: /static/**` 같은 설정을 두면 모든 정적 리소스가 `/static/` prefix 가 없는 한 404 → SecurityConfig 가 매칭 못 함 → 302 redirect → 화면 전체 깨짐.

해당 설정은 **두지 않는다**. `/css/**`, `/images/**`, `/webjars/**`, `/favicon.ico` 가 자동으로 서빙되는 기본 동작 유지.

### Thymeleaf cache + bootRun 의 sourceResources
- `application.yml`: `spring.thymeleaf.cache: false` 기본 적용 (개발 시 즉시 반영)
- `build.gradle.kts`: `bootRun { sourceResources(sourceSets["main"]) }` 적용 (2026-06-30 도입) → src/main/resources 가 classpath 에 직접 들어가 `.html` 변경 즉시 반영. `./gradlew processResources` 강제 실행 **불필요**.
- DevTools (`spring-boot-devtools` developmentOnly) 와 함께 작동 → Java 파일 변경 시 자동 restart.

### 정적 리소스(CSS/JS/이미지) 는 sourceResources 로도 즉시 반영 안 됨 — 별도 조치 필요

**배경**: 2026-07-02 D1b 작업 중 `main.css` 수정이 bootRun 서버에 반영 안 됐고, `curl /css/main.css` 로 확인 시 옛 버전이 계속 서빙됨. Java·`.html` 은 즉시 반영되는데 CSS 만 안 됨.

**원인**: IntelliJ `bootRun` 은 classpath 상 `build/resources/main/**` 을 먼저 서빙. `sourceResources` 가 `src/main/resources` 를 추가해도 static 파일은 build 산출물이 우선 로드됨 (Java·template 은 hot-reload 경로가 별도이므로 무관). 즉 CSS·JS·이미지 변경 후에는 반드시 `build/` 를 갱신해야 함.

**대응 패턴**:

```powershell
# 1. CSS/JS/이미지 변경 후
.\gradlew.bat processResources

# 2. 이미 서버가 로드한 CSS 는 브라우저 캐시도 잡고 있음 → 캐시 무효화
#    (a) Playwright/curl: URL 뒤에 ?v=timestamp 붙이거나 link[href] 를 JS 로 교체
#    (b) 브라우저: Ctrl+Shift+R (강제 새로고침)
```

**감지 방법**:
- `curl http://localhost:8080/css/main.css | grep "<변경한 클래스명>"` 로 실제 서빙 CSS 확인 → 옛 내용이면 processResources 미수행 상태
- 시각 확인 전 반드시 서빙 CSS 실측 필수 (변경 안 됐는데 눈으로만 확인하면 사고 재발)

**자동화 상태 (2026-07-07)**:
- `bootRun` 은 Gradle 태스크 그래프상 이미 `classes → processResources` 에 의존하므로 **기동 시점** 산출물은 항상 최신 — 문제는 서버 실행 중 변경분만임.
- 실행 중 변경분 대응: `.claude/hooks/post-edit-css.sh.proposed` 에 static/** 수정 시 `build/resources/main` 으로 즉시 미러 복사하는 훅 확장안 준비됨 (검토 후 본 파일로 교체 시 활성화).

### Form binding 의 boolean
hidden input 의 `value="true"` / `"false"` 를 Spring Form Binder 가 자동으로 boolean 으로 변환. JS 에서 `hidden.value = 'true'` 처럼 문자열로 set 해도 OK.

### Thymeleaf 모델 attribute 이름 예약어 충돌

Thymeleaf/Spring MVC 는 `application` / `session` / `request` 같은 이름을 **ServletContext scope 예약어** 로 취급. 모델 attribute 를 이 이름으로 넣으면 shadowing 되어 우리 객체가 아닌 servletContext 가 resolve 되며, 필드가 없으니 모두 `null` 로 렌더됨 (에러 안 남 → 시각 확인 없으면 놓치기 쉬움).

**금지 이름**: `application`, `session`, `request`, `response`, `servletContext`, `param`

```java
// ❌ shadowing — ${application.id} → null, ${application.appliedAt} → null
model.addAttribute("application", application);

// ✅ 다른 이름 사용
model.addAttribute("myApplication", application);
// 또는 도메인 별칭
model.addAttribute("apply", application);
```

**감지 방법**: 시각 확인 시 특정 객체의 여러 필드가 일제히 `null` 로 출력되면 이름 충돌 의심. `${application}` 을 통째로 출력해 보면 ServletContext 객체가 찍힘.

**2026-07-02 D1b 사고**: 신청 완료 페이지에 `#Anull`, `신청일시 null` 출력. 원인은 `application` 이름 shadowing.

### `<sec:authentication>` 태그는 Spring Security 7 에서 리터럴 렌더됨 → `#authentication` 유틸리티 사용

Spring Boot 4.1 (Spring Security 7) + `thymeleaf-extras-springsecurity6` 조합에서 **element 형태의 `<sec:authentication property="..."/>` 태그는 Thymeleaf 가 처리 못 하고 HTML 리터럴로 출력됨**. 브라우저에서 unknown element 로 무시되어 빈 텍스트로 보임.

```html
<!-- ❌ 리터럴로 렌더됨 (렌더 결과에 <sec:authentication .../> 이 그대로 남음) -->
<span class="header-user-name">
    <sec:authentication property="principal.displayName"/>님
</span>

<!-- ✅ Thymeleaf 표현식 유틸리티 사용 -->
<span class="header-user-name"
      th:text="|${#authentication.principal.displayName}님|">이름님</span>
```

- attribute 형태의 `sec:authorize="isAuthenticated()"` 는 **정상 동작**. element 형태만 문제.
- `#authentication` 유틸리티는 정상 → `${#authentication.principal.<field>}` 조합 안전.
- 감지 방법: `curl /` 응답에 `<sec:authentication`이 grep 되면 문제. 정상이면 그런 문자열 없음.
- **2026-07-03 E2E 대량 실패 사고**: 헤더 사용자 이름이 `. header-user-name` 안에서 whitespace + "님" 만 렌더됨 → login/header-nav spec 3개 실패.

### prototype 이 SVG 인 곳에 이모지로 대체 금지

**배경 (2026-07-09 F0h-c4 사고)**: `/centers` 상세 패널에 `📍`, `🕒`, `📞`, `🏢`, `×` 등을 사용했으나 prototype 은 lucide 스타일 인라인 SVG(`Icon` 컴포넌트) 를 사용. 이모지는 폰트별 렌더 편차·시각 편차·색상 제어 불가·아이콘 종류 오류(prototype 은 `📞` 대신 `user`, `🕒` 대신 `calendar` 사용) 등 여러 문제를 야기.

**규칙**:
- prototype.tsx 에 `<Icon n="...">` 또는 lucide 아이콘이 있으면 이모지 대체 금지. `templates/fragments/icons.html` 의 SVG fragment 를 재사용
- 새 아이콘 필요 시 prototype.tsx 의 `Icon` 컴포넌트 정의부(L54~77) 에서 path 데이터 그대로 이식
- 아이콘 종류(pin/calendar/user/close 등) 는 prototype 명시 값 그대로 사용 — "폰이니까 phone" 같은 임의 판단 금지
- `CenterListRenderTest.F0h_c4_*` 처럼 이모지 리터럴 부재 + `<svg` 존재 assertion 을 렌더 테스트에 포함해 회귀 방어

### `th:fragment` 는 반드시 별도 파일에 두기 (부모 body 안 금지)

**배경 (2026-07-09 F0h-c2 사고)**: `templates/center/list.html` 하단에 `<th:block th:fragment="detail-panel-content(...)">` 를 두었더니, `/centers` 렌더 시 fragment BODY 도 인라인 실행되어 `detailCenter=null` 상태에서 `${detailCenter.imageUrl}` 평가 → SpelEvaluationException.

Thymeleaf 는 "natural template" 특성상 부모 템플릿을 렌더할 때 body 내부 모든 요소를 평가한다. `th:fragment` 만으로는 실행을 막지 못하므로:

```
❌ templates/center/list.html
   <body>
     ...
     <th:block th:fragment="detail-panel-content(detailCenter, ...)">
       <div th:text="${detailCenter.name}">...  ← /centers 렌더 시 detailCenter=null 로 실행됨
     </th:block>
   </body>

✅ templates/center/list-fragments.html  (별도 파일)
   <body>
     <th:block th:fragment="detail-panel-content(detailCenter, ...)">
       <div th:text="${detailCenter.name}">...  ← th:replace 로 호출될 때만 실행
     </th:block>
   </body>

   templates/center/list.html:
     <th:block th:replace="~{center/list-fragments :: detail-panel-content(...)}"></th:block>
```

**규칙**: `th:fragment` 를 정의할 때는 항상 `<파일명>-fragments.html` 같은 별도 파일에 배치. 부모 body 안에 두지 말 것. 컨트롤러가 fragment 를 반환할 때(`return "center/list-fragments :: card-list-content";`)도 fragments 파일 참조.

### `th:if + th:replace` 같은 element 조합 금지

**배경 (2026-07-09 F0h-c2 사고)**: 다음 조합은 th:if 가 false 인데도 th:replace 가 실행되어 fragment body NPE 발생.

```html
❌ <th:block th:if="${detailCenter != null}"
             th:replace="~{center/list-fragments :: detail-panel-content(...)}"></th:block>

✅ <th:block th:if="${detailCenter != null}">
     <th:block th:replace="~{center/list-fragments :: detail-panel-content(...)}"></th:block>
   </th:block>
```

th:if(precedence 4) 와 th:replace(precedence 100) 는 서로 다른 요소에 두어 확실히 격리한다.

### HTMX 프래그먼트 재렌더 시 스타일 파라미터 왕복 (`hx-vals` 패턴)

HTMX `outerHTML` swap 으로 부분 렌더할 때, 프래그먼트가 **자신을 렌더한 컨텍스트를 다시 필요로 하면** (예: card 인지 detail 인지 구분하는 `styleClass`) 그 값을 서버가 알 방법이 없다. HTTP 요청은 stateless 이므로 클라이언트가 `hx-vals` 로 되돌려주는 패턴을 사용한다.

```html
<!-- ❌ 최초 render 는 되지만 outerHTML 응답에서 styleClass 가 null -->
<button th:hx-post="@{/toggle}" hx-swap="outerHTML"
        th:class="${styleClass + ' bookmark-btn'}">☆</button>

<!-- ✅ hx-vals 로 styleClass 왕복 -->
<button th:hx-post="@{/toggle}" hx-swap="outerHTML"
        th:attr="hx-vals=|{&quot;styleClass&quot;:&quot;${styleClass}&quot;}|"
        th:class="${styleClass + ' bookmark-btn'}">☆</button>
```

컨트롤러에서 `@RequestParam` 으로 받아 model 에 다시 넣는다. 누락하면 렌더 결과가 `class="null bookmark-btn ..."` 처럼 나와 카드/상세 스타일이 무너진다.

- **2026-07-03 사고**: `BookmarkController.toggle()` 이 model 에 `styleClass` 를 안 넣어 HTMX 응답이 `class="null bookmark-btn is-bookmarked"` → bookmark spec 3개 실패.
- 검증: `curl -X POST /bookmarks/programs/{id}/toggle` 응답의 `class="..."` 확인.

### HTML `[hidden]` 속성과 CSS `display` override 사고 방지

**배경 (2026-07-14 F-signup-03 이후 발견)**: `mypage/history.html` 취소 모달이 `<div class="mypage-modal" hidden>` 로 초기 숨김 의도였으나 CSS `.mypage-modal { display: flex; ... }` 가 브라우저 UA 의 기본 `[hidden] { display:none }` 을 override → 마이페이지 진입 시 취소 모달이 자동 노출됨.

**규칙**: `hidden` 속성으로 토글하는 요소의 CSS 규칙은 반드시 `:not([hidden])` 셀렉터로 감싼다.

```css
/* ❌ hidden 속성 무시됨 */
.my-modal { display: flex; position: fixed; ... }

/* ✅ hidden 시 display:none 유지, 그 외에만 flex */
.my-modal:not([hidden]) { display: flex; position: fixed; ... }
```

**감지**: `hidden` 속성 붙인 요소가 화면에 계속 보이면 CSS override 의심. `curl <path> | grep 'hidden'` 으로 마크업 존재 + 브라우저에서 display 실측 대조.
