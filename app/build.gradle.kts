plugins { id("com.android.application"); kotlin("android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "com.ezshare.app"
    compileSdk = 35
    defaultConfig { applicationId = "com.ezshare.app"; minSdk = 29; targetSdk = 35; versionCode = 2; versionName = "0.1.1-alpha-design" }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true; unitTests.all { it.systemProperty("roborazzi.test.record", "true"); it.maxHeapSize = "2g" } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        getByName("debug") {
            System.getenv("EZ_DEBUG_KEYSTORE_PATH")?.let {
                storeFile = file(it); storePassword = "android"; keyAlias = "androiddebugkey"; keyPassword = "android"
            }
        }
    }
    buildTypes { release { isMinifyEnabled = false } }
}
dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.44.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.44.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
