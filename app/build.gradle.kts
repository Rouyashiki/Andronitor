plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val releaseKeystore = providers.environmentVariable("ANDRONITOR_KEYSTORE").orNull
val releaseKeystorePassword = providers.environmentVariable("ANDRONITOR_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ANDRONITOR_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDRONITOR_KEY_PASSWORD").orNull
val releaseSigningConfigured = listOf(
    releaseKeystore,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.anatdx.andronitor"
    compileSdk = 37

    // The About row reads the version from BuildConfig so it cannot drift from
    // the value declared below.
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.anatdx.andronitor"
        minSdk = 26
        targetSdk = 37
        versionCode = 10001
        versionName = "1.0.0"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requireNotNull(releaseKeystore))
                storePassword = requireNotNull(releaseKeystorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    // Overrides the BOM: Material 3 Expressive's wavy progress indicators,
    // segmented list-group shapes and FAB menu only exist on the 1.5.0 line.
    implementation("androidx.compose.material3:material3:1.5.0-alpha25")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Xposed APIs — provided by the framework at runtime, never bundled into the APK.
    // Classic rovo89 API 82 for original Xposed / EdXposed / legacy LSPosed.
    compileOnly("de.robv.android.xposed:api:82")
    // Modern libxposed API for modern LSPosed (the module ships both entry points).
    compileOnly("io.github.libxposed:api:102.0.0")
    // Modern LSPosed service: lets the app talk to the framework (request scope,
    // etc.). Bundled — it is a client library, not framework-provided like the API.
    implementation("io.github.libxposed:service:102.0.0")
    ksp("androidx.room:room-compiler:2.8.5")
    testImplementation("junit:junit:4.13.2")
    // Real org.json for local unit tests — the android.jar variant is a stub.
    testImplementation("org.json:json:20260814")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
