import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.navigation.safe.args)
    alias(libs.plugins.spotless)
    alias(libs.plugins.versions)
}

android {
    namespace = "com.tomg.fiiok9control"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tomg.fiiok9control"
        minSdk = 31
        targetSdk = 34
        versionCode = 18
        versionName = "2.0.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    fun isNonStable(version: String): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { keyWord ->
            version.uppercase().contains(keyWord)
        }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(version)
        return isStable.not()
    }
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "1.8"
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "1.8"
}

detekt {
    baseline = file("$projectDir/config/detekt/baseline.xml")
}

ktlint {
    android = true
    filter {
        exclude { element -> element.file.path.contains("generated/") }
        include("**/*.kt")
    }
}

spotless {
    kotlin {
        ratchetFrom("origin/main")
        target("**/*.kt")
        licenseHeaderFile(rootProject.file("spotless/copyright.txt"))
    }
}

dependencies {
    implementation(project(":blelibrary"))
    implementation(project(":gaialibrary"))
    debugImplementation(libs.leakcanary)
    implementation(libs.bundles.implementation)
    kapt(libs.bundles.kapt)
}
