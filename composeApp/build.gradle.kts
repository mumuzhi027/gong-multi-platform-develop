import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.gradle.api.tasks.GradleBuild
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

val releaseStoreFile = localProperties.getProperty("release.storeFile")
val releaseStorePassword = localProperties.getProperty("release.storePassword")
val releaseKeyAlias = localProperties.getProperty("release.keyAlias")
val releaseKeyPassword = localProperties.getProperty("release.keyPassword")

val versionPropertiesFile = rootProject.file("version.properties")
val versionProperties = Properties().apply {
    if (versionPropertiesFile.exists()) {
        versionPropertiesFile.inputStream().use(::load)
    }
}

val storedVersionCode = versionProperties.getProperty("VERSION_CODE")?.toIntOrNull() ?: 1
val storedVersionName = versionProperties.getProperty("VERSION_NAME") ?: "3.1.20251108-beta"
val forcedVersionCode = findProperty("forcedVersionCode")?.toString()?.toIntOrNull()
val forcedVersionName = findProperty("forcedVersionName")?.toString()

fun bumpVersionName(versionName: String): String {
    val regex = Regex("""^(\d+(?:\.\d+)*)(?:-(.+))?$""")
    val match = regex.find(versionName) ?: return versionName

    val numericParts = match.groupValues[1]
        .split(".")
        .map { it.toInt() }
        .toMutableList()

    if (numericParts.size >= 4) {
        numericParts[numericParts.lastIndex] = numericParts.last() + 1
    } else {
        numericParts += 1
    }

    val suffix = match.groupValues.getOrNull(2).orEmpty()
    val numericPart = numericParts.joinToString(".")

    return if (suffix.isBlank()) numericPart else "$numericPart-$suffix"
}

fun persistVersionProperties(versionCode: Int, versionName: String) {
    versionProperties.setProperty("VERSION_CODE", versionCode.toString())
    versionProperties.setProperty("VERSION_NAME", versionName)
    versionPropertiesFile.outputStream().use { output ->
        versionProperties.store(output, "Managed by Gradle")
    }
}

val nextVersionCode = storedVersionCode + 1
val nextVersionName = bumpVersionName(storedVersionName)

val resolvedVersionCode = forcedVersionCode ?: storedVersionCode
val resolvedVersionName = forcedVersionName ?: storedVersionName


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    kotlin("plugin.serialization") version "2.2.20"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)

            implementation(libs.koin.android) // Android 特有
            implementation(libs.koin.androidx.compose) // Jetpack Compose 支持
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.glance.appwidget) // Jetpack Glance 支持
            implementation(libs.androidx.glance.material3)

        }

        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.core)           // Test core
            implementation(libs.androidx.runner)         // Instrumentation runner
            implementation(libs.androidx.junit.v115)      // JUnit extensions
            implementation(libs.androidx.room.testing.v260)   // Room testing helpers
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.json)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.resources)
            implementation(libs.ktor.client.logging)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            implementation(libs.kotlinx.serialization.json)

            implementation(libs.navigation.compose)

            implementation(libs.multiplatform.settings)

            implementation(libs.kotlinx.datetime)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
        all {
            languageSettings.enableLanguageFeature("PropertyParamAnnotationDefaultTargetMode")
        }
    }
}

android {
    namespace = "com.sky31.gongmultiplatform"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.sky31.gongmultiplatform"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            if (
                releaseStoreFile != null &&
                releaseStorePassword != null &&
                releaseKeyAlias != null &&
                releaseKeyPassword != null
            ) {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

android.applicationVariants.all {
    outputs.all outputLoop@{
        val output = this as? BaseVariantOutputImpl ?: return@outputLoop
        val variantVersionName = versionName ?: resolvedVersionName
        val variantVersionCode = versionCode
        val buildTypeName = buildType.name

        output.outputFileName =
            "GongMultiplatform-v${variantVersionName}-${buildTypeName}-${variantVersionCode}.apk"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    debugImplementation(compose.uiTooling)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

tasks.register("bumpVersionOnly") {
    group = "versioning"
    description = "Only bumps version.properties without building a new APK."
    notCompatibleWithConfigurationCache("This task mutates version.properties on disk.")

    doLast {
        persistVersionProperties(nextVersionCode, nextVersionName)
        println("Version bumped to versionCode=$nextVersionCode, versionName=$nextVersionName")
    }
}

val buildNextReleaseApk = tasks.register<GradleBuild>("buildNextReleaseApk") {
    group = "versioning"
    description = "Builds a release APK using the next version number."
    notCompatibleWithConfigurationCache("This task launches a nested Gradle build with overridden version properties.")
    tasks = listOf(":composeApp:assembleRelease")

    startParameter.projectProperties = gradle.startParameter.projectProperties + mapOf(
        "forcedVersionCode" to nextVersionCode.toString(),
        "forcedVersionName" to nextVersionName
    )
}

tasks.register("releaseWithAutoBump") {
    group = "versioning"
    description = "Builds the next release APK and then persists the bumped version."
    notCompatibleWithConfigurationCache("This task persists the next version after a successful release build.")
    dependsOn(buildNextReleaseApk)

    doLast {
        persistVersionProperties(nextVersionCode, nextVersionName)
        println("Release built with versionCode=$nextVersionCode, versionName=$nextVersionName")
    }
}
