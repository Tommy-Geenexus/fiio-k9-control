/**************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.qualcomm.qti.libraries.ble"
    compileSdk = 34

    defaultConfig {
        minSdk = 30
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.app.compat)
    implementation(libs.kotlin.stdlib)
}
