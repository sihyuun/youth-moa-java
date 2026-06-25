---
name: build-check
description: Gradle 빌드 + JPA 매핑 테스트를 실행해 컴파일 오류와 테스트 실패를 확인하고 요약 보고한다.
disable-model-invocation: true
---

사용자가 "/build-check" 를 입력하면:

## Step 1 — 컴파일 + 매핑 테스트 실행

```powershell
cd C:\Users\User\IdeaProjects\youth-moa-java
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.14"
.\gradlew.bat compileJava compileTestJava test --tests JpaMappingTest --tests ProgramSearchTest 2>&1 | Select-String -Pattern "BUILD|error:|FAILED|Tests"
```

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

## 주의사항

- 회사 PC 환경에선 Docker 미설치 → Testcontainers 가 포함된 `YouthMoaApplicationTests` 는 본 스킬에서 실행하지 않는다 (개인 PC 작업 시에만 별도 실행).
- `--tests` 인자 없이 `test` 전체 실행은 위 이유로 피하고, 항상 클래스 단위로 지정한다.
