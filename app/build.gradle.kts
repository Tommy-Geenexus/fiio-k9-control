import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.navigation.safe.args)
    alias(libs.plugins.spotless)
    alias(libs.plugins.versions)
}

android {
    namespace = "io.github.tommygeenexus.fiiok9control"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.tommygeenexus"
        minSdk = 31
        targetSdk = 35
        versionCode = 20
        versionName = "2.0.5"
    }

    signingConfigs {
        create("release") {
            val keyStorePassword = "KS_PASSWORD"
            val keyStoreKeyAlias = "KS_KEY_ALIAS"
            val properties = Properties().apply {
                val file = File(projectDir.parent, "keystore.properties")
                if (file.exists()) {
                    load(FileInputStream(file))
                }
            }
            val password = properties
                .getOrDefault(keyStorePassword, null)
                ?.toString()
                ?: System.getenv(keyStorePassword)
            val alias = properties
                .getOrDefault(keyStoreKeyAlias, null)
                ?.toString()
                ?: System.getenv(keyStoreKeyAlias)
            storeFile = File(projectDir.parent, "keystore.jks")
            storePassword = password
            keyAlias = alias
            keyPassword = password
            enableV1Signing = false
            enableV2Signing = false
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
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

    lint {
        disable += "LogNotTimber"
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

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.window.core.ExperimentalWindowCoreApi",
            "-opt-in=kotlin.ExperimentalStdlibApi"
        )
    }
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
    ksp(libs.bundles.ksp)
}
