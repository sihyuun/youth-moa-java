# F2c — 알림 채널 실 발송 어댑터 (Email · KakaoAlimtalk)

- 상태: `spec_draft`
- 우선순위: 3
- 브랜치 후보: `feature/F2c-notification-channels`
- 작성일: 2026-07-28

---

## 1. 배경

`Notification` 엔티티·저장·읽음 처리는 완결 (`ApplicationNotificationListener` 가 `AFTER_COMMIT` + `REQUIRES_NEW` 로 승인/반려/취소 이벤트 구독). 사용자 알림 채널 설정 (`User.notifyKakao/notifySms/notifyEmail`) 및 `NotificationChannelResolver` 도 존재. 그러나:

1. **알림은 DB 저장만 이루어지며 실 채널 fanout 이 없음** — 사용자는 사이트 접속 시 헤더 종에서만 확인 가능
2. `SmsSender` 는 인증번호 전송 목적으로만 존재 (`CoolSmsSender` / `MockSmsSender`) 하며, 알림 채널로는 아직 연결 안 됨
3. 이메일 / 카카오 알림톡 어댑터 자체가 없음

본 스펙은 **`Notification` 저장 후 사용자 설정 채널로 fanout 하는 어댑터 계층**을 도입.

---

## 2. 디자인 출처 (3자산)

| 자산 | 위치 | 내용 |
|---|---|---|
| prototype.tsx | L1557~1585 (알림 설정 UI) | 3채널 (카카오 알림톡 / 문자 SMS / 이메일) 개별 토글. 아래 4개 알림 항목 (승인반려·D-1·빈자리·신규) 개별 토글 |
| prototype.tsx | L264~309 (알림신청 모달) | 오픈/빈자리 알림 신청. `kakao/email` 2채널 선택. **⚠️ 여기 UI 는 SMS 없음**, 회원 설정과 다름 (설계상 갭) |
| prototype.tsx | L286 comment | "여러 개 선택 가능" — multi-channel fanout 확정 |
| prototype.html | 동일 | 렌더 결과 동일 |
| HANDOFF.md | 별도 항목 없음 | — |
| 현재 구현 | `ApplicationNotificationListener` L26~90 | Notification 저장만 수행. 채널 fanout 코드 없음 |
| 현재 구현 | `NotificationChannelResolver` L14~22 | 사용자 활성 채널 목록 반환은 이미 구현 — 재사용 |
| 현재 구현 | `SmsSender` / `CoolSmsSender` / `MockSmsSender` | 인증번호 전송 목적. **범용 SMS 발송으로 재활용 or 별도 어댑터 신설** (Q1) |

### 2-A. tsx 상태 다이어그램 (L1557~1585 인용)

```
프로필 → 알림 설정 탭
├─ [알림 받을 방법] 3채널 개별 스위치
│   ├─ kakao (카카오 알림톡) — sub: 사용자 phone
│   ├─ sms (문자) — sub: 사용자 phone
│   └─ email (이메일) — sub: 사용자 email
└─ [알림 항목] 4종 개별 스위치
    ├─ _lock (신청 승인/반려 결과) — 잠금, 끌 수 없음
    ├─ remind (D-1 리마인더)
    ├─ empty (빈자리 알림)
    └─ news (신규 프로그램 소식)
```

**⚠️ 현재 백엔드 갭**: `User` 엔티티에 채널별 boolean 은 있으나 **알림 항목별 on/off 는 없음**. Q6 결정 필요.

---

## 3. 자산 간 갭 표

| 항목 | prototype.tsx | prototype.html | HANDOFF | 채택 |
|---|---|---|---|---|
| 채널 종류 | kakao, sms, email (3종) | 동일 | — | 3종 유지 |
| 알림톡 종류 | 명시 안 됨 | — | — | **결정 필요 Q3** — 알림톡(템플릿) vs 친구톡 |
| 이메일 발송 방식 | 명시 안 됨 | — | — | **결정 필요 Q2** — SES vs SMTP vs Mock 우선 |
| 재시도 정책 | 명시 안 됨 | — | — | **Q5** — 즉시 1회 vs Spring Retry vs outbox |
| 사용자 전역 OFF | 항목별 개별 토글 (`_lock` 만 잠금) | 동일 | — | Q6 — 항목별 필드 or 전역만 |
| 신청 모달 SMS 부재 | kakao/email 만 | 동일 | — | **UX 불일치 갭** — 스펙에 별도 기록, Q7 |

---

## 4. 데이터 모델 gap 표

| prototype 필드 | 현재 엔티티 | 조치 |
|---|---|---|
| 채널 on/off (3종) | `User.notifyKakao/Sms/Email` | ✅ 존재 |
| 항목 on/off (`remind`/`empty`/`news`) | 없음 | **Q6** — 컬럼 3개 추가 or JSON preference or 이번 스코프 밖 |
| 발송 이력 | 없음 | **Q4** — `NotificationDelivery` 엔티티 신설 여부 (재시도·중복 방지) |
| 발송 실패 사유 | 없음 | Q4 와 함께 결정 |

**제안 (Q4-A 채택 시):**
```sql
-- V<N>__create_notification_delivery.sql
CREATE TABLE notification_delivery (
  id BIGSERIAL PRIMARY KEY,
  notification_id BIGINT NOT NULL REFERENCES notification(id),
  channel VARCHAR(20) NOT NULL,  -- KAKAO / SMS / EMAIL
  status VARCHAR(20) NOT NULL,   -- PENDING / SENT / FAILED
  attempt_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(500),
  sent_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_delivery_status ON notification_delivery(status, created_at);
```

---

## 5. 데이터 소비 지점

| 소비 지점 | prototype 참조 | 현재 상태 | 갭 |
|---|---|---|---|
| 알림 승인/반려 이벤트 → 채널 fanout | tsx L1572 `_lock` 항목 | DB 저장만, fanout 없음 | 어댑터 호출 신설 |
| D-1 리마인더 스케줄러 | tsx L1572 `remind` | 스케줄러 자체 없음 | 이번 스코프 밖 (Q8) |
| 빈자리 알림 트리거 | tsx L1572 `empty` | Waitlist 자체 미구현 | 이번 스코프 밖 |
| 신규 프로그램 소식 | tsx L1572 `news` | 트리거 없음 | 이번 스코프 밖 |
| 헤더 종 알림 목록 | 기존 화면 | 유지 | Notification 저장은 그대로 |
| 알림 설정 UI | mypage L1557~1585 | 3채널 토글은 있으나 항목별 토글 없음 | Q6 결정 시 UI 확장 |

---

## 6. 변경 범위

**패키지 신설**: `io.github.sihyuuun.youthmoa.notification.channel`

**어댑터 인터페이스**
- [ ] `NotificationSender.java` (신규 interface) — `void send(Notification, User)` + `NotificationChannel channel()`
- [ ] `EmailNotificationSender.java` — SMTP 또는 SES 구현
- [ ] `MockEmailNotificationSender.java` — 로그만
- [ ] `KakaoAlimtalkSender.java` — 카카오 비즈 메시지 API (알림톡 템플릿 방식)
- [ ] `MockKakaoAlimtalkSender.java` — 로그만
- [ ] `SmsNotificationSender.java` — 기존 `SmsSender` 재활용 or 별도 (Q1). 알림용 문구 템플릿 필요

각 실 구현체는 `@ConditionalOnProperty` 로 스위치:
```
youthmoa.notification.email.enabled=true
youthmoa.notification.kakao.enabled=true
youthmoa.notification.sms.enabled=true
```
기본은 Mock 활성.

**Fanout 서비스**
- [ ] `NotificationDispatcher.java` — `Notification` + `User` 받아 `NotificationChannelResolver.activeChannelsFor(user)` 로 활성 채널 조회, 각 채널에 대응하는 `NotificationSender` 를 `Map<NotificationChannel, NotificationSender>` 에서 lookup 해 호출

**이벤트 흐름**
- [ ] `ApplicationNotificationListener` 각 메서드 마지막에 `dispatcher.dispatch(notification, user)` 호출 추가
- [ ] 실패 시 `NotificationDelivery.status=FAILED` 로 기록, Notification 저장은 유지 (사용자 헤더 종에서는 계속 노출)

**엔티티 / 마이그레이션 (Q4-A / Q6 채택 시)**
- [ ] `NotificationDelivery` 엔티티
- [ ] `V<N>__create_notification_delivery.sql`
- [ ] `V<M>__add_user_notification_preferences.sql` (Q6-A 시)

**설정**
- [ ] `application.yml` — Mock 기본, 실 채널 자격증명 프로퍼티 목록 추가
- [ ] `application-e2e.yml` — 항상 Mock 강제

**의존성 (build.gradle.kts)**
- [ ] `spring-boot-starter-mail` (이메일 SMTP 시)
- 카카오 알림톡은 REST 호출 → 별도 라이브러리 불필요

---

## 7. PR 분할 제안

**PR-1 (본 명세 핵심)**: NotificationSender 추상화 + EmailSender/KakaoSender Mock 구현체 + Dispatcher + Listener 연결. 실 SMS 는 기존 `SmsSender` 재활용 (Q1-A). NotificationDelivery 엔티티 없이 즉시 발송.

**PR-2**: `NotificationDelivery` 엔티티 + 재시도 정책 (Spring Retry). PR-1 의 즉시 fanout 을 outbox 패턴으로 전환.

**PR-3**: 이메일 실 SES/SMTP 구현체 + 카카오 알림톡 실 API 구현체. `@ConditionalOnProperty` 활성화.

**PR-4**: 항목별 preference (Q6-A) UI + 백엔드 반영.

---

## 8. 검증 시나리오

### 정적
- `./gradlew compileJava`
- `./gradlew test --tests NotificationDispatcherTest` — Mock 3개 등록 후 사용자 채널 조합별 fanout 검증
- `./gradlew test --tests ApplicationNotificationListenerTest` — dispatcher 호출 검증 (Mockito)

### 동적 (curl + 로그)
- e2e 프로파일 (Mock 강제) 에서 신청 승인 API 호출 → Mock 로그에 `[MockEmail] to=X title=Y` 3채널 흔적
- `youthmoa.notification.email.enabled=false` 로 두면 활성 채널 목록에서 자연 제외

### write→read 왕복
- 사용자 A 가 알림 채널을 email 만 켠 상태에서 신청 승인 이벤트 발생 → `NotificationDelivery` 3개 row (SMS/KAKAO 는 SKIPPED, EMAIL 은 SENT) 저장 확인 (PR-2 이후)
- 발송 실패 시 재시도 후 `attempt_count` 증가 (PR-2)

### 시각 (사용자)
- Mock 어댑터 로그 확인 후 실 채널은 스테이징에서 사용자 폰/메일로 수신 확인

---

## 9. Q 리스트

| # | 질문 | 옵션 | 기본 제안 |
|---|---|---|---|
| Q1 | SMS 어댑터 | (a) 기존 `SmsSender` 재활용 (인증번호+알림 겸용) / (b) `NotificationSender` 계층에 별도 `SmsNotificationSender` 신설 후 내부에서 `SmsSender` 위임 | **(b)** — 관심사 분리, 인증 SMS 는 도메인 로직상 별개 |
| Q2 | 이메일 발송 방식 | (a) Spring Mail SMTP (Gmail App Password 등) / (b) AWS SES / (c) Mock 만 우선 | (c) → 스테이징 검증 시점에 (a) 로 승격. SES 는 계정 자산 준비 필요 |
| Q3 | 카카오톡 종류 | (a) 알림톡 (사전 심사 템플릿 필수, 정보성) / (b) 친구톡 (플러스친구 등록 필요, 마케팅 포함 가능) / (c) Mock 만 | (a) — 신청 승인/반려는 정보성이라 알림톡 적합. 심사 지연 대비 Mock 병행 |
| Q4 | 발송 이력 저장 | (a) `NotificationDelivery` 엔티티 신설 / (b) Notification 에 컬럼 추가 (`kakaoSentAt`, `emailSentAt`, `smsSentAt`) / (c) 이력 미저장 | **(a)** — 재시도·감사 대비. 컬럼 방식은 확장성 약함 |
| Q5 | 재시도 정책 | (a) 즉시 1회 실패 시 로그만 / (b) Spring Retry 3회 exp backoff / (c) outbox + 스케줄러 재시도 | PR-1 은 (a), PR-2 에서 (b) |
| Q6 | 항목별 (`remind`/`empty`/`news`) preference | (a) User 에 boolean 3필드 추가 / (b) `UserNotificationPreference` 엔티티 (확장성) / (c) 이번 스코프 밖 | **(c)** — PR-1 은 승인/반려(`_lock` 잠금) 만 대상. 항목별 토글은 PR-4 |
| Q7 | 알림신청 모달 SMS 부재 | (a) 프로토 준수 (kakao/email 만) / (b) 3채널 통일 | 별건 UX 결정. 백엔드는 3채널 모두 지원 |
| Q8 | D-1 리마인더 스케줄러 | (a) 이번 스코프 / (b) 별도 후속 PR | (b) — 스케줄러 (Spring `@Scheduled`) + 중복방지 로직 별건 |
| Q9 | Notification 저장 실패 시 fanout | (a) 저장 실패면 fanout 안 함 (현재 구조 유지) / (b) 저장 실패해도 fanout 시도 | (a) — 감사 가능성 위해 저장이 진리 |
| Q10 | Mock 어댑터 로그 레벨 | INFO / DEBUG | INFO — 개발 편의 |

---

## 10. 위험 / 주의

- **`@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW` 이미 적용됨** — fanout 을 여기 이어붙일 때 fanout 자체 실패가 Notification 저장을 롤백하지 않도록 try/catch 유지 (현재 코드 참조)
- **실 채널 자격증명은 시크릿** — application.yml 에 하드코딩 금지. 환경변수 or Spring Cloud Config
- **알림톡 템플릿 사전 등록** — 카카오비즈메시지 콘솔에서 승인/반려/취소 각각 템플릿 등록 필요. 스펙만 잡고 실 등록은 별건 운영 작업
- **CoolSMS 재활용 시 발신번호 등록 상태 확인** — 이미 인증번호로 등록됨. 문구만 다르므로 문제없음 (Q1-B 선택 시)
- **Mock 을 개발/e2e 기본으로** — 실수로 스테이징 자격증명이 로컬에 새어 실 발송되는 사고 방지
- **CLAUDE.md 확장성 원칙** — 항목별 preference 를 처음부터 엔티티로 (`UserNotificationPreference`) 잡아두는 편이 admin 트랙과 정합. Q6 검토 시 (b) 도 고려
