# JPA / PostgreSQL 함정

> CLAUDE.md 에서 분리 (2026-07-28). 상시 준수 규칙이 아니라 **해당 영역을 건드릴 때 참조하는 사고 패턴 모음**이다.
> 여기 있는 항목은 전부 이 프로젝트에서 실제로 사고 난 것이다.

이 프로젝트에서 실제로 사고 났던 패턴. 새 엔티티·화면 작업 전 일독 권장.

### `@Lob` + `open-in-view: false` — LOB streaming 오류
`application.yml` 의 `spring.jpa.open-in-view: false` (현재 설정) 환경에서 `@Lob` 필드(예: `Program.content`, `Program.requirements`) 를 컨트롤러 반환 이후 템플릿에서 읽거나, 트랜잭션 밖에서 접근하면 다음 예외 발생:

```
org.postgresql.util.PSQLException: Large Objects may not be used in auto-commit mode.
```

**원인**: PostgreSQL 은 CLOB 을 `LargeObjectManager` 로 streaming 하며, streaming 은 트랜잭션 안에서만 가능. auto-commit 모드에서는 큰 객체 스트림을 열 수 없음.

**해결 패턴 (택 1)**:
```java
// A. Controller 메서드에 read-only 트랜잭션 부착 (권장 — 스코프 최소)
@GetMapping("/apply/complete")
@Transactional(readOnly = true)
public String complete(...) { ... }

// B. Service 로 옮기고 서비스 메서드에 @Transactional 부착
```

**어떻게 감지되는가**: `@WebMvcTest` 는 실 DB 를 안 쓰므로 이 사고를 못 잡음. **화면 변경 PR 은 curl 동적 검증 필수**.

### `@ManyToOne(LAZY)` + 템플릿 접근 → `LazyInitializationException`
`open-in-view: false` 상태에서 컨트롤러가 엔티티를 반환하고 템플릿에서 lazy 연관을 접근하면:

```
org.hibernate.LazyInitializationException: Could not initialize proxy [X] - no session
```

**해결 패턴 (권장)**: Repository 메서드에 `@EntityGraph` 로 fetch join.

```java
@EntityGraph(attributePaths = {"program", "user"})
Optional<Application> findWithProgramAndUserById(Long id);
```

컨트롤러에 `@Transactional(readOnly=true)` 만 부착해도 lazy 로딩은 되지만, 트랜잭션이 뷰 렌더 끝날 때까지 열려 있어야 하므로 커넥션 점유 시간이 길어짐. `@EntityGraph` 로 필요한 그래프만 로드하는 편이 성능·확장성 모두 유리.

**주의**: `@EntityGraph` 는 필요한 관계만 명시. 지나치게 많이 넣으면 카티션 곱 발생 → 별도 쿼리 필요.

### `@ElementCollection` 필드 참조 재할당 금지 — 반드시 mutate

**배경 (2026-07-14 F-signup-03 WelcomeScreen 저장 사고)**: `User.updateInterests(regions, categories)` 가 필드를 재할당 (`this.interestRegions = regions`) 하도록 구현되어, welcome 화면에서 선택한 값이 mypage 태그에 반영되지 않음. Hibernate 는 엔티티 로드 시 `@ElementCollection` 필드에 `PersistentSet` 프록시를 세팅해 변경사항을 추적하는데, 새 컬렉션 인스턴스를 재할당하면 트래킹이 끊겨 flush 시 DELETE/INSERT 가 일부 또는 전혀 실행되지 않음.

```java
// ❌ 재할당 — Hibernate PersistentSet 트래킹 끊김, 저장 실패 or 부분 저장
public void updateInterests(Set<String> regions, Set<String> categories) {
    this.interestRegions = regions;
    this.interestCategories = categories;
}

// ✅ 동일 인스턴스 mutate — DELETE + INSERT 정상 실행
public void updateInterests(Set<String> regions, Set<String> categories) {
    this.interestRegions.clear();
    if (regions != null) this.interestRegions.addAll(regions);
    this.interestCategories.clear();
    if (categories != null) this.interestCategories.addAll(categories);
}
```

**감지 방법**: 저장 API 200 응답 오는데 재조회 시 값이 옛 상태거나 빈 상태 → 재할당 패턴 의심. `@ElementCollection` `@OneToMany(orphanRemoval=true)` 등 컬렉션 관계 필드는 항상 mutate 패턴 사용.

### `@WebMvcTest` 는 실제 렌더링 하지 않음
`@WebMvcTest(Controller.class)` 는 view name / model attribute 만 검증. Thymeleaf 실제 파싱·EL 평가·엔티티 lazy 접근은 실행되지 않아 위 두 사고 유형 모두 통과함.

**대응**:
- 화면 변경 PR 은 **반드시** curl 동적 검증 (CLAUDE.md "검증 규칙" 재강조).
- 주요 렌더 경로는 `@SpringBootTest + MockMvc` 통합 렌더링 테스트 병행 검토 (후속 티켓 `chore/integration-test-render`).
