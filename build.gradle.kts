// Copyright 2024 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

import java.util.Properties

plugins {
    kotlin("android") version libs.versions.kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.bundletool)
}

android {
    namespace = "net.leodesouza.blitz"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.leodesouza.blitz"
        minSdk = 21
        targetSdk = 34
        versionCode = 185
        versionName = "1.8.5"

        base {
            archivesName = "${applicationId}_$versionCode"
        }
    }

    signingConfigs {
        register("release") {
            val keystorePropertiesFile = file("keystore.properties")
            if (keystorePropertiesFile.isFile) {
                val keystoreProperties = Properties()
                keystoreProperties.load(keystorePropertiesFile.inputStream())
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            } else {
                val debugSigningConfig = getByName("debug")
                storeFile = debugSigningConfig.storeFile
                storePassword = debugSigningConfig.storePassword
                keyAlias = debugSigningConfig.keyAlias
                keyPassword = debugSigningConfig.keyPassword
            }
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
    signingConfig {
        val releaseSigningConfig = android.signingConfigs["release"]
        storeFile = releaseSigningConfig.storeFile
        storePassword = releaseSigningConfig.storePassword
        keyAlias = releaseSigningConfig.keyAlias
        keyPassword = releaseSigningConfig.keyPassword
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

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get())
    }
}
