# 당직 (Dangjik)

유통 커머스 사이트를 운영유지보수하는 AI 당직자 프로토타입입니다.
AI는 장애 원인을 진단하고 조치안을 제안하며, 조치 실행은 서버 검증과 사람의 승인을 거쳐서만 이뤄집니다.

- 몰 (Nuxt), 파트너 포털 · CRM · 운영 콘솔 (Vue), 하이브리드 앱 (Kotlin, Swift)
- Spring Boot, Oracle
- 회사, 사용자, 상품, 주문 데이터는 모두 가상입니다. (가상 회사: 하루마켓)

**소개와 Q&A**: https://happyherait.github.io/Dangjik/  
**공개 데모** (서버 없이 브라우저에서 동작): https://happyherait.github.io/Dangjik/demo/
**데모 영상** (2분 22초): https://youtu.be/XXQylW9ss6Y

## 핵심 원칙

- AI 도구는 읽기 전용 조회와 런북 제안만 할 수 있습니다. 임의 SQL이나 임의 API를 실행하지 않습니다.
- 런북은 코드로 정의된 고정 작업입니다. AI가 대상을 제안해도 서버가 사실(주문, PG 원장)로 다시 판단합니다.
- 승인은 실행 계획의 해시에 묶입니다. 승인 뒤 계획이 바뀌면 다시 승인해야 합니다.
- 요청자 본인은 승인할 수 없고, 운영 팀장만 승인할 수 있습니다.

## 데모 장애 시나리오

배포 `mall-api 2.3.1`에서 PG 웹훅 수신 경로가 바뀌어, 앱 주문 `ORD-20261105-0007`은 PG가 승인했는데도 결제대기로 남아 있습니다.
고객 문의 → SR 티켓 → AI 당직 진단 → 런북 "결제 상태 재동기화" 제안 → 팀장 승인 → 실행 순서로 해결합니다.

## 화면

| 영역 | 주소 | 내용 |
|---|---|---|
| 몰 | localhost:3005 | 상품, 주문 조회 |
| 파트너 포털 | localhost:5175 | 대시보드, 재고, 주문/출고, 클레임, 정산 |
| CRM | localhost:5175 | 회원 360, 상담 이력, 쿠폰, 세그먼트 |
| 모바일 앱 | Android 에뮬레이터, iOS 시뮬레이터 | 몰을 WebView로 띄우는 셸, 공유, 강제 업데이트 |
| 운영 콘솔 | localhost:5175 | AI 당직, SR 티켓, 장애 타임라인, 런북 승인, 배치, 배포, 앱 버전, 감사 로그 |

## AI가 할 수 있는 일과 없는 일

- 할 수 있음: 문의, 주문, PG 거래, 웹훅 기록, 처리 이력, 배포, 배치, 상품 조회. 런북 "결제 상태 재동기화" 제안
- 할 수 없음: 주문이나 가격 직접 변경, 임의 SQL, 승인. 이런 도구는 서버에 아예 없다
- 고객 문의와 상품 설명은 `external_data`로 감싸서 넘기고, 그 안의 지시를 따르지 않도록 한다
- 모델이 속더라도 없는 도구는 실행되지 않고, 제안은 서버가 사실로 다시 판단하며, 실행은 팀장 승인이 있어야 한다

## 구조

```text
backend/
  runbook/   런북 계획과 승인 규칙 (순수 Java, Spring/DB/AI 의존성 없음)
  app/       Spring Boot API, Flyway, Oracle
frontend/
  mall/      Nuxt: 고객용 몰 (홈, 상품, 주문 조회)
  admin/     Vue: 파트너 포털, CRM, 운영 콘솔
mobile/
  android/   Kotlin WebView 셸 (AGP 9.4, Gradle 9.6)
  ios/       Swift WKWebView 셸 (XcodeGen)
shared/
  golden/    Java와 TypeScript가 함께 쓰는 런북 골든 케이스
site/        소개와 Q&A 페이지 (GitHub Pages)
docs/        기여 방법, 설계 기록
```

## 실행과 기여

[docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)를 참고하세요.
