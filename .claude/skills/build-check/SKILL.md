---
name: build-check
description: Gradle 빌드 + JPA 매핑 테스트를 실행해 컴파일 오류와 테스트 실패를 확인하고 요약 보고한다.
disable-model-invocation: true
---

사용자가 "/build-check" 를 입력하면:

## Step 1 — 컴파일 + 매핑 + 렌더 테스트 실행

```powershell
cd C:\Users\User\IdeaProjects\youth-moa-java
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.14"
.\gradlew.bat compileJava compileTestJava test `
  --tests JpaMappingTest `
  --tests ProgramSearchTest `
  --tests "*RenderTest" `
  --tests "*RenderingTest" 2>&1 | Select-String -Pattern "BUILD|error:|FAILED|Tests"
```

**렌더 테스트 포함 이유** (2026-07-09 F0h-c2 사고 회고):
- `compileJava` 는 Java 만 검증. Thymeleaf 파싱·SpEL 평가는 미수행.
- `@WebMvcTest` 는 view name / model attribute 만 검증. Thymeleaf 실 렌더 안 함.
- `*RenderTest` 는 `@SpringBootTest + MockMvc + e2e 프로파일` 로 실 Thymeleaf 파싱을 강제 → `th:fragment` 인라인 실행 NPE, `${detailCenter.imageUrl}` SpEL 오류, `th:if + th:replace` 조합 사고 등을 사전 감지.

기존 추가 테스트가 있으면 `--tests XxxTest` 인자를 함께 전달.

## Step 2 — 결과 분석 및 보고

**성공 시:**
> ✅ 빌드 성공 — 컴파일·테스트 모두 통과
> 실행 테스트: [클래스 목록]
> 소요 시간: [`BUILD SUCCESSFUL in NNs`]

**컴파일 실패 시:**
> ❌ 컴파일 실패 — N개 오류
>
> **오류 목록:**
> 1. `파일경로:줄번호` — 오류 메시지 (원인 한 줄 설명)
> 2. ...
>
> **수정 순서 제안:** [의존 관계 고려]

**테스트 실패 시:**
> ⚠️ 컴파일은 성공, 테스트 실패
>
> **실패 테스트:**
> 1. `클래스.메서드` — `expected: X, actual: Y` (가능한 원인)
> 2. ...

## Step 3 — 후속 조치 제안

오류 또는 테스트 실패가 있으면 수정 여부를 물어보고, 동의하면 바로 수정 작업을 시작한다.

## Step 4 — 화면 변경 시 curl 실측 (선택, 강력 권장)

Thymeleaf 템플릿·CSS·JS·Controller view name 을 변경한 PR 은 아래 절차를 추가 수행:

```powershell
# preview 로 e2e 프로파일 bootRun (8090 포트, H2 시드, Supabase 자격증명 불필요)
# preview_start 툴 사용 가능 시:
#   preview_start(name: "youth-moa-e2e")
# 또는 직접:
Start-Process -FilePath "$PWD\.claude\scripts\bootrun-e2e.cmd" -WindowStyle Hidden
# ...기동 완료 대기 후
curl.exe -s -o NUL -w "GET /path -> %{http_code}\n" http://localhost:8090/<path>
```

**curl 200 실측 없이 커밋 금지** (2026-07-09 F0h-c2 사고 이후).

## 주의사항

- 회사 PC 환경에선 Docker 미설치 → Testcontainers 가 포함된 `YouthMoaApplicationTests` 는 본 스킬에서 실행하지 않는다 (개인 PC 작업 시에만 별도 실행).
- `--tests` 인자 없이 `test` 전체 실행은 위 이유로 피하고, 항상 클래스 단위로 지정한다.
- 회사 PC 에서도 **e2e 프로파일 + 8090 포트** 로 bootRun 이 가능하다는 점 잊지 말 것 (Supabase 자격증명 부재 로 판단 X — 인프라 이미 완비, 2026-07-07 도입).
