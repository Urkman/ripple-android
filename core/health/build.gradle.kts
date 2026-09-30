plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.stefansturm.ripple.core.health"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdkPhone.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.health.connect.client)
    implementation(libs.kotlinx.coroutines.android)
}
