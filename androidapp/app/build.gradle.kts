import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Absent on machines without the release key (CI included): release then falls back to
// building unsigned rather than failing the whole build.
val keystoreProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use(::load)
}

// Passwords can come from the RELEASE_STORE_PASSWORD/RELEASE_KEY_PASSWORD env vars instead of
// keystore.properties, for a one-off signed build without writing the password to disk.
fun signingProperty(key: String, envVar: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(envVar)

android {
    namespace = "com.cloudimny"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.cloudimny"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val releaseStorePassword = signingProperty("storePassword", "RELEASE_STORE_PASSWORD")
        val releaseKeyPassword = signingProperty("keyPassword", "RELEASE_KEY_PASSWORD")
        if (releaseStorePassword != null && releaseKeyPassword != null) {
            create("release") {
                storeFile = rootProject.file(
                    keystoreProperties.getProperty("storeFile") ?: "release.keystore"
                )
                storePassword = releaseStorePassword
                keyAlias = keystoreProperties.getProperty("keyAlias") ?: "cloudimny"
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.sshj)
    implementation(libs.bcprov.jdk18on)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.database)
    implementation(libs.androidx.media3.datasource.okhttp)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

tasks.withType<Test> {
    useJUnitPlatform()
}