plugins {
    id("com.android.application")
}

android {
    namespace = "com.ganjinehnoor.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ganjinehnoor.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 13
        versionName = "1.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
