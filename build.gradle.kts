// Copyright 2024 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

import java.util.Properties

val keystorePropertiesFile = file("keystore.properties").takeIf { it.isFile }
val keystoreProperties = Properties().apply { keystorePropertiesFile?.inputStream()?.let(::load) }

plugins {
    kotlin("android") version libs.versions.kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.bundletool)
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get())
    }
}

android {
    namespace = "net.leodesouza.blitz"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.leodesouza.blitz"
        minSdk = 21
        targetSdk = 34
        versionCode = 188
        versionName = "1.8.8"

        base {
            archivesName = "${applicationId}_$versionCode"
        }
    }

    signingConfigs {
        val debug by getting

        register("release") {
            storeFile = keystoreProperties.getProperty("storeFile")?.let(::file) ?: debug.storeFile
            storePassword = keystoreProperties.getProperty("storePassword") ?: debug.storePassword
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: debug.keyAlias
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: debug.keyPassword
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs["release"]
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
    }

    androidResources {
        generateLocaleConfig = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

bundletool {
    val release by android.signingConfigs

    signingConfig {
        storeFile = release.storeFile
        storePassword = release.storePassword
        keyAlias = release.keyAlias
        keyPassword = release.keyPassword
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.window)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}
