# iOS 앱 셸

Mac과 Xcode가 있어야 실행할 수 있습니다. Windows에서는 GitHub Actions(macOS)가 빌드와 테스트를 대신 확인합니다.

```bash
brew install xcodegen
cd mobile/ios
xcodegen generate
open DangjikApp.xcodeproj
```

시뮬레이터에서 실행하기 전에 내 Mac에서 API(18082)와 몰(3005)을 먼저 켜 둡니다.
