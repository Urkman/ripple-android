plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.stefansturm.ripple"
    compileSdk = libs.versions.compileSdk.get().toInt()

    // Release signing is supplied by the local build environment. The
    // keystore and passwords must never live in the repository.
    val releaseStoreFile = providers.gradleProperty("rippleReleaseStoreFile").orNull
        ?: System.getenv("RIPPLE_RELEASE_STORE_FILE")
    val releaseStorePassword = providers.gradleProperty("rippleReleaseStorePassword").orNull
        ?: System.getenv("RIPPLE_RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = providers.gradleProperty("rippleReleaseKeyAlias").orNull
        ?: System.getenv("RIPPLE_RELEASE_KEY_ALIAS")
    val releaseKeyPassword = providers.gradleProperty("rippleReleaseKeyPassword").orNull
        ?: System.getenv("RIPPLE_RELEASE_KEY_PASSWORD")
    val releaseSigningConfigured = listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

    if (releaseSigningConfigured) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "de.stefansturm.ripple"
        minSdk = libs.versions.minSdkPhone.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 3
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures.compose = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:storage"))
    implementation(project(":core:preferences"))
    implementation(project(":core:health"))
    implementation(project(":core:wear-sync"))
    implementation(project(":feature:today"))
    implementation(project(":feature:history"))
    implementation(project(":feature:stats"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))
    implementation(project(":system:widgets"))
    implementation(project(":system:quicksettings"))
    implementation(project(":system:notifications"))
    implementation(project(":system:appactions"))
    implementation(project(":system:export"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.wearable)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
