# Postmortem: Gradle test worker EOFException (incremental cache)

**일자**: 2026-10-06
**영향**: 로컬 `./gradlew test` (clean 없이) 시 fork 직후 `java.io.EOFException` 으로 BUILD FAILED. CI 는 영향 없음 (ubuntu 러너는 매번 fresh cache).

## 재현 조건

- Windows 11 Pro 로컬 PC
- Gradle 9.7.1 + Java 17 (Foojay 다운로드)
- `./gradlew test` **without** preceding `./gradlew clean`
- 재현률: 2026-10-06 bisect 매트릭스에서 100% (2회/2회)

## 격리 매트릭스 (D5-Q1c bisect, 2026-10-06)

| Case | 브랜치 상태 | 명령 | 결과 | 소요 |
|---|---|---|---|---|
| A | D5-Q1d baseline (d08dae5, Q1c stashed) | `./gradlew test` (cached) | **BUILD FAILED** (EOF) | 12m 32s |
| B | D5-Q1d baseline (d08dae5, Q1c stashed) | `./gradlew clean test` | **BUILD SUCCESSFUL** | 8m 44s |
| C | D5-Q1c WIP (stash pop) | `./gradlew test` (cached) | **BUILD SUCCESSFUL** | 8m 8s |
| D | D5-Q1c WIP + clean (QA 기존) | `./gradlew clean test` | **746/746 PASS** | 9m 2s |

**결론**:
- **Q1c 변경과 Gradle crash 는 인과 없음** — Case A 는 baseline (d08dae5) 에서 재현, Q1c 가 포함된 Case C/D 는 PASS. Q1c 는 변수가 아님.
- **clean 선행 (B/D) 은 항상 PASS**.
- **cached 모드 (A/C) 는 첫 실행 FAIL 후 self-repair** — Case A 실패 직후 돌린 Case C 는 PASS. gradle incremental cache 가 partially corrupted → first run 이 cache 를 재구성 후 crash → second run 은 복구된 cache 사용 → PASS 패턴.

## 원인 가설

Gradle 9.7.1 test worker fork 과정에서 incremental build cache 가 부분 손상된 상태로 재사용될 때 worker 가 stream 을 읽다 EOF 를 만나는 것으로 추정. 재현 조건이 환경 종속 (Windows PC 1대) 이라 Gradle 업스트림 버그보다 로컬 cache 디렉토리 상태 가능성 큼.

관련 이슈 검색 키워드:
- `gradle test worker EOFException`
- `gradle incremental build cache corruption`

## 임시 완화책

**`./gradlew test` 실행 전 `./gradlew clean` 선행**:
```powershell
./gradlew clean test
```

또는 incremental cache 수동 삭제:
```powershell
./gradlew --stop
Remove-Item -Recurse -Force build/, .gradle/
./gradlew test
```

## 영구 조치 후보

1. **CI 신뢰 패턴**: 로컬 불안정 때는 `git push` 후 CI (ubuntu 러너) 결과를 1차 신뢰, 로컬은 `compileJava` + 변경 범위 단위 테스트만 확인
2. **Gradle 10.x 업그레이드**: 버그 수정됐을 가능성, 다만 Spring Boot 4.1 호환성 확인 필요
3. **wrapper hook**: `.claude/scripts/` 또는 pre-build hook 에 `clean` 자동 선행 (단, 매번 full build 라 시간 비용)
4. **gradle.properties 조정**: `org.gradle.daemon=false` 로 daemon 격리 — 재현률 변화 관찰

## 담당 / 후속

- 별도 환경 티켓 `OPS-gradle-eof-worker-crash` 로 분리 (현재 Q1c 커밋 블로킹 아님)
- 다음 PR 작성 시 `./gradlew clean test` 명시
- CLAUDE.md 검증 섹션에 "로컬 test 실행 시 clean 선행 권장" 1줄 추가 검토
