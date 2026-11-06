# 기여 방법

## 작업 흐름

1. 작업은 GitHub Issues에 등록한다.
2. `main`에서 브랜치를 만든다. 이름은 `feat/짧은-설명` 또는 `fix/짧은-설명`.
3. 커밋하고 push하면 GitHub Actions가 자동으로 검사한다.
4. Pull Request를 열고, 리뷰 후 `main`에 머지한다.

## 커밋 메시지

첫 줄에 무엇을 했는지 짧게 쓴다. 예: `Add seed products for bakery partner`

## 로컬 실행

| 대상 | 명령 | 포트 |
|---|---|---|
| Oracle | `docker compose up -d --wait` | 1524 |
| API | `mvn -f backend/pom.xml -DskipTests package` 후 `java -jar backend/app/target/dangjik-app-0.1.0.jar` | 18082 |
| 몰 화면 | `frontend/mall`에서 `npm ci`, `npm run dev` (API를 먼저 실행) | 3005 |
| 관리자 화면 | `frontend/admin`에서 `npm ci`, `npm run dev` | 5175 |

## AI 당직과 Claude API 키

- `ANTHROPIC_API_KEY` 환경 변수가 있으면 실제 Claude를 부르고, 없으면 데모 모델로 동작한다. 화면 제목 옆에 어느 쪽인지 표시된다.
- 키는 저장소에 넣지 않는다. PowerShell에서 API를 실행하기 전에 그 터미널에만 넣는다.

```powershell
$env:ANTHROPIC_API_KEY="sk-ant-..."
java -jar backend/app/target/dangjik-app-0.1.0.jar
```

- 모델은 `ANTHROPIC_MODEL`로 바꿀 수 있다. 기본값은 `claude-sonnet-5-5`.
- CI는 키 없이 데모 모델과 가짜 모델로 공격 시나리오까지 검사한다.

## 테스트

- 런북 규칙: `mvn -f backend/pom.xml -pl runbook test` (DB 필요 없음)
- 백엔드 전체: `mvn -f backend/pom.xml verify` (Oracle 필요)
- 몰 화면: `frontend/mall`에서 `npm run typecheck`, `npm test`
- 관리자 화면: `frontend/admin`에서 `npm run typecheck`, `npm test`

## 데이터 규칙

회사, 사람, 상품, 주문 데이터는 모두 가상으로 만든다. 실제 회사 이름, 채용 공고 원문이나 이미지는 저장소에 올리지 않는다.
