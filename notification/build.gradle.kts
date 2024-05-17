//
// © 2024-present https://github.com/cengiz-pz
//

import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android") version "1.9.23"
}

val pluginName = "NotificationSchedulerPlugin"
val pluginPackageName = "org.godotengine.plugin.android.notification"
val resultActivityClassPath = "$pluginPackageName.ResultActivity"
val receiverClassPath = "$pluginPackageName.NotificationReceiver"
val godotVersion = "4.2.2"
val pluginVersion = "2.0"
val demoAddOnsDirectory = "../demo/addons"
val templateDirectory = "addon_template"
val pluginDependencies = arrayOf(
    "androidx.appcompat:appcompat:1.6.1"
)

android {
    namespace = pluginPackageName
    compileSdk = 33

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 21

        manifestPlaceholders["godotPluginName"] = pluginName
        manifestPlaceholders["godotPluginPackageName"] = pluginPackageName
        buildConfigField("String", "GODOT_PLUGIN_NAME", "\"${pluginName}\"")
        setProperty("archivesBaseName", "$pluginName-$pluginVersion")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("org.godotengine:godot:$godotVersion.stable")
    pluginDependencies.forEach { implementation(it) }
}

val copyDebugAARToDemoAddons by tasks.registering(Copy::class) {
    description = "Copies the generated debug AAR binary to the plugin's addons directory"
    from("build/outputs/aar")
    include("$pluginName-$pluginVersion-debug.aar")
    into("$demoAddOnsDirectory/$pluginName/bin/debug")
}

val copyReleaseAARToDemoAddons by tasks.registering(Copy::class) {
    description = "Copies the generated release AAR binary to the plugin's addons directory"
    from("build/outputs/aar")
    include("$pluginName-$pluginVersion-release.aar")
    into("$demoAddOnsDirectory/$pluginName/bin/release")
}

val cleanDemoAddons by tasks.registering(Delete::class) {
    delete("$demoAddOnsDirectory/$pluginName")
}

val copyPngsToDemo by tasks.registering(Copy::class) {
    description = "Copies the PNG images to the plugin's addons directory"
    from(templateDirectory)
    into("$demoAddOnsDirectory/$pluginName")
    include("**/*.png")
}

val copyAddonsToDemo by tasks.registering(Copy::class) {
    description = "Copies the export scripts templates to the plugin's addons directory"

    dependsOn(cleanDemoAddons)
    finalizedBy(copyDebugAARToDemoAddons)
    finalizedBy(copyReleaseAARToDemoAddons)
    finalizedBy(copyPngsToDemo)

    from(templateDirectory)
    into("$demoAddOnsDirectory/$pluginName")
    exclude("**/*.png")

    var dependencyString = ""
    for (i in pluginDependencies.indices) {
        dependencyString += "\"${pluginDependencies[i]}\""
        if (i < pluginDependencies.size-1) dependencyString += ", "
    }

    filter(ReplaceTokens::class,
        "tokens" to mapOf(
            "pluginName" to pluginName,
            "pluginVersion" to pluginVersion,
            "pluginPackage" to pluginPackageName,
            "resultClass" to resultActivityClassPath,
            "receiverClass" to receiverClassPath,
            "pluginDependencies" to dependencyString))
}

tasks.register<Zip>("packageDistribution") {
    archiveFileName.set("${pluginName}-${pluginVersion}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))

    from("../demo/addons/${pluginName}") {
        into("${pluginName}-root/addons/${pluginName}")
    }
}

tasks.named<Delete>("clean").apply {
    dependsOn(cleanDemoAddons)
}

afterEvaluate {
    tasks.named("assembleDebug").configure {
        finalizedBy(copyAddonsToDemo)
    }
    tasks.named("assembleRelease").configure {
        finalizedBy(copyAddonsToDemo)
    }
}
