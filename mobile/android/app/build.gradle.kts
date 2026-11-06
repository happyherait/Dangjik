plugins {
    id("com.android.application")
}

// 에뮬레이터에서 내 PC는 10.0.2.2 로 보인다. 실제 폰으로 볼 때는 -PmallUrl=http://내PC_IP:3005 로 바꾼다
val mallUrl = (findProperty("mallUrl") as String?) ?: "http://10.0.2.2:3005"
val apiUrl = (findProperty("apiUrl") as String?) ?: "http://10.0.2.2:18082"

android {
    namespace = "io.github.happyherait.dangjik.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.happyherait.dangjik.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 380
        versionName = "3.8.0"
        buildConfigField("String", "MALL_URL", "\"$mallUrl\"")
        buildConfigField("String", "API_URL", "\"$apiUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
