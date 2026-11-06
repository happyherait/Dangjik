# Android 앱 셸

하루마켓 몰을 WebView로 띄우는 하이브리드 앱입니다. Kotlin, Android Gradle Plugin 9.4(Kotlin 내장), Gradle 9.6.

## Android Studio로 실행

1. 내 PC에서 API(18082)와 몰(3005)을 먼저 켭니다.
2. Android Studio → Open → `mobile/android` 폴더를 엽니다.
3. 에뮬레이터를 고르고 Run을 누릅니다. 에뮬레이터에서 내 PC는 `10.0.2.2`로 보이므로 따로 바꿀 것이 없습니다.

## 앱이 하는 일

- 사용자 에이전트 끝에 `DangjikApp/3.8.0 (Android)`를 붙여서, 몰이 앱 안에서 열린 것을 알게 합니다.
- `window.DangjikApp.getAppInfo()`, `window.DangjikApp.share(text)` 두 기능만 웹에 엽니다.
- 몰 주소 밖의 링크는 앱 안에서 열지 않고 브라우저로 넘깁니다.
- 켜질 때 `/api/app/version-check`로 버전을 묻고, 최소 지원 버전보다 낮으면 닫을 수 없는 업데이트 안내를 띄웁니다.
- http는 로컬 개발 주소(10.0.2.2, localhost)에서만 허용합니다.
