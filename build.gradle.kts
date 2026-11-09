// Copyright 2025 Léo de Souza
// SPDX-License-Identifier: Apache-2.0

import com.android.build.api.artifact.SingleArtifact
import com.android.tools.build.bundletool.androidtools.Aapt2Command
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.model.Password
import com.android.tools.build.bundletool.model.SigningConfiguration
import java.util.Optional
import java.util.Properties
import kotlin.io.path.moveTo

val keystorePropertiesFile = file("keystore.properties").takeIf(File::isFile)
val keystoreProperties = Properties().apply { keystorePropertiesFile?.inputStream()?.let(::load) }

plugins {
    kotlin("plugin.compose") version libs.versions.kotlin
    alias(libs.plugins.android.application)
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.jvmToolchain.get())
    }
}

android {
    namespace = "net.leodesouza.blitz"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "net.leodesouza.blitz"
        minSdk = 23
        targetSdk = 37
        versionCode = 209
        versionName = "2.0.9"
    }

    signingConfigs {
        val debug = getByName("debug")

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
            optimization {
                enable = true
                keepRules {
                    ignoreFrom("androidx.graphics:graphics-path")
                }
            }
            signingConfig = signingConfigs.getByName("release")
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

abstract class BuildApksTask : DefaultTask() {
    @get:InputFile
    abstract val bundle: RegularFileProperty

    @get:OutputFile
    abstract val apks: RegularFileProperty

    @get:InputFile
    abstract val aapt2: RegularFileProperty

    @get:InputFile
    abstract val storeFile: RegularFileProperty

    @get:Input
    abstract val keyAlias: Property<String>

    @get:Internal
    abstract val storePassword: Property<String>

    @get:Internal
    abstract val keyPassword: Property<String>

    @TaskAction
    fun buildApks() {
        BuildApksCommand.builder()
            .setBundlePath(bundle.get().asFile.toPath())
            .setOutputFile(apks.get().asFile.toPath())
            .setOverwriteOutput(true)
            .setAapt2Command(Aapt2Command.createFromExecutablePath(aapt2.get().asFile.toPath()))
            .setSigningConfiguration(
                SigningConfiguration.extractFromKeystore(
                    storeFile.get().asFile.toPath(),
                    keyAlias.get(),
                    Optional.of(Password.createFromStringValue("pass:${storePassword.get()}")),
                    Optional.of(Password.createFromStringValue("pass:${keyPassword.get()}")),
                ),
            )
            .build()
            .execute()
    }
}

androidComponents.onVariants { variant ->
    val name = variant.name.replaceFirstChar(Char::uppercase)
    val outputDir = layout.projectDirectory.dir(variant.name)
    val fileName = base.archivesName.get()
    val signingConfig = variant.buildType?.let(android.buildTypes::getByName)?.signingConfig
        ?: return@onVariants

    for (output in variant.outputs) {
        output.outputFileName = "$fileName.apk"
    }

    variant.artifacts.use(tasks.register<Copy>("copy${name}Apk") {
        description = "Copies APK outputs for variant ${variant.name} to ${variant.name}/"
        into(outputDir)
    })
        .wiredWith { copy -> objects.directoryProperty().also { copy.from(it) } }
        .toListenTo(SingleArtifact.APK)

    variant.artifacts.use(tasks.register<Copy>("copy${name}Bundle") {
        description = "Copies bundle for variant ${variant.name} to ${variant.name}/$fileName.aab"
        into(outputDir)
        rename { "$fileName.aab" }
    })
        .wiredWith { copy -> objects.fileProperty().also { copy.from(it) } }
        .toListenTo(SingleArtifact.BUNDLE)

    tasks.named("redirectIdeBundleOutputs$name"::equals).configureEach {
        val redirectedFile = outputDir.file("$fileName-${variant.name}.aab").asFile
        mustRunAfter("copy${name}Bundle")
        doLast {
            redirectedFile.takeIf(File::isFile)
                ?.toPath()
                ?.moveTo(outputDir.file("$fileName.aab").asFile.toPath(), overwrite = true)
        }
    }

    tasks.register<BuildApksTask>("buildApks$name") {
        description = "Assembles APK set for variant ${variant.name}"
        bundle = variant.artifacts.get(SingleArtifact.BUNDLE)
        apks = outputDir.file("$fileName.apks")
        aapt2 = androidComponents.sdkComponents.aapt2.flatMap { it.executable }
        storeFile = signingConfig.storeFile
        keyAlias = signingConfig.keyAlias
        storePassword = signingConfig.storePassword
        keyPassword = signingConfig.keyPassword
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
