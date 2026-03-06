// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

import java.util.Properties

val keystorePropertiesFile = file("keystore.properties").takeIf { it.isFile }
val keystoreProperties = Properties().apply { keystorePropertiesFile?.inputStream()?.let(::load) }

plugins {
    kotlin("plugin.compose") version libs.versions.kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.bundletool)
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.jvmToolchain.get())
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

android {
    namespace = "net.leodesouza.blitz"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "net.leodesouza.blitz"
        minSdk = 23
        targetSdk = 36
        versionCode = 207
        versionName = "2.0.7"
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
            signingConfig = signingConfigs.named("release").get()
        }
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    bundle {
        language {
            enableSplit = false
        }

        codeTransparency {
            signing {
                storeFile = keystoreProperties.getProperty("storeFile")?.let(::file)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("transparencyAlias")
                keyPassword = keystoreProperties.getProperty("transparencyPassword")
            }
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        jniLibs {
            excludes += "**/libandroidx.graphics.path.so"
        }
    }
}

base {
    archivesName = "${android.defaultConfig.applicationId}_${android.defaultConfig.versionCode}"
}

bundletool {
    val release by android.signingConfigs.existing

    signingConfig {
        storeFile.set(release.get().storeFile)
        storePassword.set(release.get().storePassword)
        keyAlias.set(release.get().keyAlias)
        keyPassword.set(release.get().keyPassword)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.coroutines.test)
}
