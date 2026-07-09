# 작업 명세: fix/F0h-operating-hours-badge — 운영중 배지 시각 기반 판정 + operatingHours 구조화

- **상태**: `spec_draft`
- **브랜치**: `fix/F0h-operating-hours-badge`
- **선행**: 없음. F0h 시리즈(c1/c2/c3/c4) 머지 완료 상태 위에서 진행
- **병렬 관계**: `fix/F0h-real-coords` 와 별도 브랜치. 두 티켓 모두 Center 엔티티를 건드리나 필드가 겹치지 않으므로 물리 충돌 없음. 머지 순서는 real-coords → operating-hours-badge 권장 (CSV 스키마가 후자에서 확장되므로)
- **작업 단위**: 1~2 PR (엔티티 확장 + 도메인 메서드 + View 배지 로직 + 회귀 테스트)

---

## 0. 배경·목표·확정 사항

### 0-1. 사고 회고 (2026-07-09 F0h 확장성 검토)

`fix/F0h-real-coords` spec 산출 중 `CLAUDE.md` §확장성 원칙 §파생 시드 금지 규칙(2026-07-09 추가) 정합 검토 과정에서 **배지 표시 로직도 확장성 원칙 위반** 이 추가 발견되었습니다.

현재 `/centers` 카드·상세 패널·인포윈도우 의 "운영중"/"운영종료" 배지는 오직 `Center.isActive` boolean 필드 하나로 결정되고 있습니다.

```java
// 대략적인 현재 상태 (Thymeleaf)
<span th:if="${center.isActive}" class="badge-open">운영중</span>
<span th:unless="${center.isActive}" class="badge-closed">운영종료</span>
```

문제점:
- **접속 시각 무관**. 시드 시점의 `true` 값이 매번 그대로 반환됨
- 사용자 기대는 **현재 시각 기준 운영 여부**. 예를 들어 화요일 오후 3시면 "운영중", 일요일이면 "운영종료" 표시
- CSV 의 `operatingHours` 는 free-text (예: `"평일 10:00~21:00, 토 10:00~17:00, 일·공휴일 휴무"`) 로 파싱 어려움 → 구조화 필요
- 관리자 CRUD 로 운영시간을 편집해도 배지 판정 로직이 사용하지 않음 → **admin CRUD 무력화**

### 0-2. CLAUDE.md 확장성 원칙 정합

`CLAUDE.md` §확장성 원칙 §관리자 CRUD 실효성 체크 를 위반한 상태입니다.

> 관리자 페이지에서 편집 가능하도록 만든 필드가 실제 편집 결과가 유지되는지 검증:
> - 파생 로직이 편집값을 무력화하지 않는가?

또한 §파생 시드 금지 규칙의 다음 조항과도 대응됩니다.

> 파생이 필요하면 도메인 메서드 (예: `Center.isCurrentlyOpen(now)`) 로 런타임 계산. DB 저장값은 원천 필드만.

### 0-3. Y안 채택 (사용자 확정)

1. `Center.isActive` boolean 의 **의미 재정의**: "영업 중단·폐업 kill-switch" (관리자용 수동 토글). 폐업된 센터면 관리자가 `false` 로 두고, 화면에서 항상 "운영종료" 표시
2. 신규 도메인 메서드 `Center.isCurrentlyOpen(LocalDateTime now)` 추가 — 현재 시각과 구조화된 operatingHours 를 비교해 boolean 반환
3. **배지 표시 로직 최종**: `isActive && isCurrentlyOpen(now)` == true → "운영중", 그 외 → "운영종료"

---

## 1. 변경 범위 (파일 단위)

- [ ] `src/main/java/.../center/Center.java` — operatingHours 구조화 필드 추가 (옵션 A 채택 시 `@Embedded OperatingHours`) + 도메인 메서드 `isCurrentlyOpen(LocalDateTime)` 추가
- [ ] `src/main/java/.../center/OperatingHours.java` — 신규 `@Embeddable` 값 객체 (옵션 A 채택 시)
- [ ] `src/main/java/.../common/DataInitializer.java` — CSV 로더에서 구조화 필드 파싱·주입
- [ ] `src/main/resources/data/centers.csv` — 컬럼 확장 (§3 참조)
- [ ] `src/main/resources/templates/center/list.html` — 배지 표시 로직에 `#{...isCurrentlyOpen(...)}` 호출 반영
- [ ] `src/main/resources/templates/center/list-fragments.html` — 상세 패널 배지 동일 반영
- [ ] `src/main/resources/static/js/centers-map.js` (또는 인포윈도우 조립부) — 인포윈도우 HTML 조립 시 배지 판정
- [ ] `src/main/java/.../center/CenterController.java` — model 에 `now` 또는 헬퍼 유틸 주입 (또는 Thymeleaf `#temporals` 유틸리티 활용)
- [ ] `src/test/java/.../center/CenterOpeningTimeTest.java` — 시간대별 회귀 테스트 (신규)
- [ ] `src/test/java/.../center/CenterListRenderTest.java` — 배지 렌더 assertion 추가

---

## 2. 도메인 설계

### 2-1. `Center.isCurrentlyOpen(LocalDateTime now)` 스펙

```java
public boolean isCurrentlyOpen(LocalDateTime now) {
    if (this.operatingHours == null) {
        return false;  // 데이터 미확보 → 종료 표시가 안전
    }
    return this.operatingHours.isOpenAt(now);
}
```

- 배지 판정식은 서비스·컨트롤러가 아닌 **엔티티 도메인 메서드**에 위치 (§CLAUDE.md 도메인 메서드 규칙)
- `isActive` 조합은 View 또는 Service 층에서: `center.isActive() && center.isCurrentlyOpen(now)`
- `now` 파라미터를 강제해 테스트 용이성 확보 (`Clock` 주입은 오버스펙, 학습 단계에선 파라미터 방식 채택)

### 2-2. operatingHours 구조화 방안 (A / B / C 옵션 비교)

| 옵션 | 방식 | 장점 | 단점 | 학습 단계 적합성 |
|---|---|---|---|---|
| **A (권장)** | `@Embeddable OperatingHours` 값 객체 | 명확·검증 가능·admin 폼 편집 자연스러움. JPA 표준 문법으로 학습 가치 높음 | 요일별 개별 시간(월 10~18, 화~금 10~21 등) 지원 안 됨 → 근사 필요 | ⭐⭐⭐ (권장) |
| B | `@Entity OperatingHourSlot` 요일별 별도 엔티티 (`DayOfWeek + open + close + centerId`) | `월 10~18, 화~금 10~21` 처럼 요일별 다른 시간 완벽 지원 | 스키마 복잡, admin UI 도 복잡, JOIN 비용, 학습 단계에는 과함 | ⭐ |
| C | JSON 컬럼 (PostgreSQL jsonb) | 유연·확장성 최고 | 파싱 로직 별도, 검증 약함, JPA 지원 애매 (`hypersistence-utils` 등 추가 라이브러리) | ⭐⭐ |

#### 옵션 A 스키마 (권장 후보)

```java
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OperatingHours {

    private LocalTime weekdayOpen;    // 평일 open (예: 10:00). null = 평일 미운영
    private LocalTime weekdayClose;   // 평일 close (예: 21:00)
    private LocalTime saturdayOpen;   // 토요일 open. null = 미운영
    private LocalTime saturdayClose;
    private LocalTime sundayOpen;     // 일요일 open. null = 미운영·휴관
    private LocalTime sundayClose;
    private boolean holidayClosed;    // 공휴일 휴관 여부 (default true)

    public boolean isOpenAt(LocalDateTime now) {
        LocalDate date = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        DayOfWeek day = date.getDayOfWeek();

        // 1) 공휴일 판정 (§9 오픈 이슈: 판정 소스 결정 필요)
        if (holidayClosed && isKoreanHoliday(date)) return false;

        // 2) 요일별 open/close 결정
        LocalTime openTime, closeTime;
        if (day == DayOfWeek.SATURDAY) {
            openTime = saturdayOpen; closeTime = saturdayClose;
        } else if (day == DayOfWeek.SUNDAY) {
            openTime = sundayOpen; closeTime = sundayClose;
        } else {
            openTime = weekdayOpen; closeTime = weekdayClose;
        }

        if (openTime == null || closeTime == null) return false;

        // 3) 자정 넘김 케이스 (§9 오픈 이슈) — 예: 22:00~02:00
        //    현 스펙은 close > open 만 지원. 자정 넘김은 미지원 (§9 결정 대기)
        return !time.isBefore(openTime) && time.isBefore(closeTime);
    }

    private boolean isKoreanHoliday(LocalDate date) {
        // §9 오픈 이슈: 하드코딩 vs 공휴일 API 결정 필요
        return HolidayRegistry.isHoliday(date);
    }
}
```

#### 근사 케이스 처리

옵션 A 는 요일별 세밀한 스케줄을 표현 못 하는 한계가 있습니다. 이 한계를 커버하기 위해:

1. **원본 free-text `operatingHours` 필드는 유지** (Center 엔티티의 기존 String 필드) → UI 표시용 (예: 상세 패널의 "운영시간: 평일 10:00~21:00, 토 10:00~17:00")
2. **배지 판정은 구조화 `OperatingHours` 값 객체 사용** → `isCurrentlyOpen(now)` 계산의 진리 소스
3. 두 필드가 개념적으로 중복이나, 자연어 표시 vs 로직 판정의 목적이 다름을 주석으로 명시

---

## 3. CSV 스키마 확장 (구조화 필드 시드)

### 3-1. 현재 `centers.csv` (fix/F0h-real-coords 반영 가정, 8컬럼)

```
name,region,address,phone,operatingHours,lat,lng,isActive
```

### 3-2. 확장안 — 옵션 A 채택 시 7컬럼 추가 (총 15컬럼)

```
name,region,address,phone,operatingHours,lat,lng,isActive,
weekdayOpen,weekdayClose,saturdayOpen,saturdayClose,sundayOpen,sundayClose,holidayClosed
```

- 값 형식: `HH:mm` (예: `10:00`, `21:00`). 미운영은 빈 값
- 예시 row:
  ```
  양평딴딴회관,양평군,경기 양평군 ...,031-xxx-xxxx,"평일 10:00~21:00, 토 10:00~17:00, 일·공휴일 휴무",
  37.4923,127.4876,true,
  10:00,21:00,10:00,17:00,,,true
  ```
- 시드 시점 파싱: `DataInitializer` 가 `LocalTime.parse(...)` 로 변환, 빈 값이면 `null`

### 3-3. 대안 — 별도 파일 분리

```
src/main/resources/data/
├── centers.csv                    # 기본 정보 (좌표·주소 포함)
└── operating_hours.csv            # centerName,weekdayOpen,...
```

**결정 필요 (§9)**: 8컬럼 CSV 에 이어붙일지, 별도 파일로 분리할지. 별도 파일 분리는 JOIN 로딩 부담이 있으나 관심사 분리 측면에서 깔끔. **권장은 15컬럼 단일 CSV** — 학습 단계 단순성 우선.

---

## 4. 배지 표시 위치 목록 (View 영향)

`/centers` 페이지 안에서 배지가 표시되는 세 지점 모두 동일 판정 로직을 사용해야 합니다.

### 4-1. 리스트 카드 (`templates/center/list.html`)

```html
<!-- 현재 (예상) -->
<span th:if="${center.isActive}" class="badge-open">운영중</span>

<!-- 변경 후 -->
<span th:if="${center.isActive and center.isCurrentlyOpen(#temporals.createNow())}"
      class="badge-open">운영중</span>
<span th:unless="${center.isActive and center.isCurrentlyOpen(#temporals.createNow())}"
      class="badge-closed">운영종료</span>
```

또는 컨트롤러에서 `openStatusMap` 을 미리 계산해 model 로 넘기는 편이 렌더 부담 낮음:

```java
Map<Long, Boolean> openStatusMap = centers.stream()
    .collect(Collectors.toMap(Center::getId,
        c -> c.isActive() && c.isCurrentlyOpen(LocalDateTime.now())));
model.addAttribute("openStatusMap", openStatusMap);
```

```html
<span th:if="${openStatusMap[__${center.id}__]}" class="badge-open">운영중</span>
```

**권장**: 컨트롤러에서 사전 계산 (템플릿의 `#temporals` 직접 호출은 매 카드마다 반복 실행)

### 4-2. 상세 패널 (`templates/center/list-fragments.html`)

`detail-panel-content` fragment 내 배지 위치도 동일 방식. `detailCenter` 하나만 판정하므로 fragment 인자로 `isOpenNow` boolean 을 직접 전달.

```html
<th:block th:fragment="detail-panel-content(detailCenter, isOpenNow, ...)">
    <span th:if="${isOpenNow}" class="badge-open">운영중</span>
</th:block>
```

### 4-3. 카카오맵 인포윈도우

`static/js/centers-map.js` (또는 인포윈도우 HTML 조립부) — JS 에서 배지 렌더 시 서버가 판정한 값을 사용해야 함. 방안:

- **방안 1**: Center DTO 를 JSON 으로 넘길 때 `isOpenNow` 필드도 함께 포함
- **방안 2**: 인포윈도우 HTML 을 서버에서 렌더 (HTMX 로 lazy load)

**권장**: 방안 1. `/api/centers` JSON 응답에 `isOpenNow` 필드 포함해 JS 는 판정 없이 표시만.

---

## 5. 관리자 CRUD 실효성 (후속 티켓 준비)

이번 티켓의 직접 스코프는 아니지만, 후속 `feature/admin-center-crud` 티켓에서 아래를 편집 가능하도록 설계 방향 제시:

- **요일 세트 토글**: `[평일][토][일]` 세 그룹으로 나눠 각각 open/close time picker
- **공휴일 휴관 여부 체크박스**: `holidayClosed`
- **미운영 처리**: open/close time picker 를 비워두면 `null` 저장
- **`isActive` 스위치**: 영업 중단·폐업 상태 토글 (배지 강제 종료)
- **원본 free-text 필드**: 별도 textarea 로 노출 (자연어 표시용)

---

## 6. 회귀 방어 테스트

### 6-1. `CenterOpeningTimeTest` — 시간대별 판정 (신규)

`@DataJpaTest` 로 실행. 케이스:

| # | Center 상태 | 판정 시각 | 기대 결과 |
|---|---|---|---|
| 1 | 평일 10~21, 토 10~17, 일 미운영 | 화요일 15:00 | `true` |
| 2 | 평일 10~21, 토 10~17, 일 미운영 | 화요일 22:00 | `false` |
| 3 | 평일 10~21, 토 10~17, 일 미운영 | 토요일 12:00 | `true` |
| 4 | 평일 10~21, 토 10~17, 일 미운영 | 토요일 18:00 | `false` |
| 5 | 평일 10~21, 토 10~17, 일 미운영 | 일요일 15:00 | `false` |
| 6 | 평일 10~21, 토 10~17, 일 미운영, 공휴일 휴관 | 2026-01-01(공휴일) 15:00 | `false` |
| 7 | `isActive=false` (폐업), 평일 10~21 | 화요일 15:00 | `false` (isActive kill-switch) |
| 8 | `operatingHours=null` (데이터 미확보) | 화요일 15:00 | `false` (안전 default) |
| 9 | 평일 open 정각 경계 | 화요일 10:00 | `true` (inclusive open) |
| 10 | 평일 close 정각 경계 | 화요일 21:00 | `false` (exclusive close) |

### 6-2. `CenterListRenderTest.F0h_operating_hours_*` — HTML 렌더 assertion

- 리스트 카드에 `badge-open` / `badge-closed` 중 하나만 존재
- 상세 패널 fragment 에도 동일 배지 클래스 렌더
- 이모지·리터럴 "true"/"false" 잔존 부재

### 6-3. `JpaMappingTest.embeddedOperatingHours_mapped` — 매핑 검증

- `@Embedded OperatingHours` 필드가 `centers` 테이블 컬럼으로 flatten 되는지 확인
- 컬럼명 prefix 필요 시 `@AttributeOverrides` 로 명시

---

## 7. 다음 티켓 후보

1. **`feature/admin-center-crud`** — 관리자 페이지에서 Center CRUD (좌표·운영시간·isActive 편집). 이번 티켓과 real-coords 티켓이 admin CRUD 실효성의 전제 조건
2. **`chore/holiday-registry`** — 한국 공휴일 판정 소스 구축 (오픈 이슈 §9-1 후속)
3. **`feature/center-open-status-api`** — `/api/centers` 응답에 `isOpenNow` 필드 포함 (§4-3 방안 1 구현)

---

## 8. 검증 시나리오

### 8-1. 정적 검증
- `./gradlew compileJava` 통과
- `./gradlew test --tests JpaMappingTest` — `@Embedded` 매핑 확인
- `./gradlew test --tests CenterOpeningTimeTest` — 10개 케이스 PASS
- `./gradlew test --tests CenterListRenderTest` — 배지 렌더 assertion PASS

### 8-2. 동적 검증
- `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/centers` → 200 OK
- `curl -s http://localhost:8090/centers | grep -E "badge-(open|closed)"` → 각 카드마다 하나씩 렌더
- HTML 에 `${...}`, `th:*` 잔존 없음
- 상세 패널 열림 (`?detail=<id>`) → fragment 재렌더에도 배지 정상

### 8-3. 시각 확인 (사용자 영역)
- 브라우저에서 접속 시각별 배지 변화 확인 (평일 낮/저녁 · 주말)
- 폐업 처리된 센터 (`isActive=false`) 는 시각 무관 "운영종료"
- 카카오맵 인포윈도우 배지도 리스트 배지와 일치

---

## 9. 결정 확정 (2026-07-09)

### 9-1. operatingHours 구조화 방안 — **A 채택** (`@Embeddable OperatingHours`)
학습 단계 복잡도 대비 실용성. 요일별 다른 시간 근사 케이스는 free-text `operatingHours` 필드를 UI 표시용으로 유지해 커버.

### 9-2. 공휴일 판정 소스 — **`jollyday` 라이브러리 채택**
Korean calendar 지원. `holidayClosed=false` 인 센터는 판정 자체 skip 하므로 도입 시급도 낮으나, 유지비 낮고 정확도 확보. impl 시 하드코딩 임시 진행 후 안정화되면 라이브러리로 전환도 허용.

### 9-3. 자정 넘김 케이스 — **이번 티켓 범위 밖** (`close > open` 제약)
현재 청년센터 CSV 48개 어느 것도 자정 넘김 케이스 없음. `Center.isCurrentlyOpen(now)` 도메인 메서드 주석에 `close > open` 만 지원 명시. CSV 시드 로드 시 위반 시 fail-fast. 필요 시 후속 티켓 `feature/overnight-operating-hours` 로 분리.

### 9-4. CSV 스키마 — **15컬럼 단일 파일**
`centers.csv` 확장 (기존 8컬럼 + 7컬럼 = weekdayOpen, weekdayClose, saturdayOpen, saturdayClose, sundayOpen, sundayClose, holidayClosed). 관리 단일화 우선.

### 9-5. 배지 판정 실행 위치 — **Controller 사전 계산 Map**
`openStatusMap: Map<Long, Boolean>` 을 Controller 에서 미리 채워 Thymeleaf 에 전달. 매 카드 렌더마다 `LocalDateTime.now()` 호출·판정 반복 회피. 성능·재사용성 유리.

### 9-6. `now` 주입 방식 — **파라미터 방식**
`Center.isCurrentlyOpen(LocalDateTime now)`. 호출자가 시간 결정. 엔티티를 순수 값 객체로 유지. 테스트에서 명시적 시각 주입.
