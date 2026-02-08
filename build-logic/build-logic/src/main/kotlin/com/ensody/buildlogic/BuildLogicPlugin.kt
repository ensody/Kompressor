@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import com.android.build.gradle.BaseExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlatformExtension
import org.gradle.api.plugins.catalog.CatalogPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.repositories
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File

/** Base setup. */
class BaseBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {}
}

class ConditionalAndroidBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            if (OS.current in setOf(OS.macOS, OS.Linux)) {
                pluginManager.apply("com.ensody.build-logic.android")
            }
        }
    }
}

fun Project.initBuildLogic() {
    group = "com.ensody.kompressor"

    initBuildLogicBase {
        setupRepositories()
    }
}

fun Project.setupRepositories() {
    repositories {
        google()
        mavenCentral()
        if (System.getenv("RUNNING_ON_CI") != "true") {
            mavenLocal()
        }
    }
}

fun Project.setupBuildLogic(block: Project.() -> Unit) {
    setupBuildLogicBase {
        setupRepositories()
        if (extensions.findByType<JavaPlatformExtension>() != null) {
            setupPlatformProject()
        }
        if (extensions.findByType<BaseExtension>() != null) {
            setupAndroid(coreLibraryDesugaring = rootLibs.findLibrary("desugarJdkLibs").get())
        }
        if (extensions.findByType<KotlinMultiplatformExtension>() != null) {
            setupKmp {
                when (OS.current) {
                    OS.Linux -> {
                        androidTarget()
                        jvm()
                        linuxArm64()
                        linuxX64()
                    }

                    OS.Windows -> {
                        jvm()
                        mingwX64()
                    }

                    OS.macOS -> {
                        if (project.name.endsWith("--nativelib") || project.name.endsWith("-ktor")) {
                            addAllNonJsTargets()
                        } else {
                            addAllTargets()
                        }
                    }
                }
                if (targets.any { it.platformType == KotlinPlatformType.androidJvm }) {
                    sourceSets["androidInstrumentedTest"].apply {
                        dependsOn(sourceSets["androidUnitTest"])
                        dependencies {
                            implementation(rootLibs.findLibrary("androidx-test-runner").get())
                        }
                    }
                }
                compilerOptions {
                    optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    optIn.add("kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi")
                    optIn.add("kotlinx.coroutines.FlowPreview")
                }

                sourceSets["jvmCommonTest"].dependencies {
                    implementation(rootLibs.findLibrary("kotlin-test-junit").get())
                    implementation(rootLibs.findLibrary("junit").get())
                }
            }
            tasks.register("testAll") {
                group = "verification"
                dependsOn("jvmTest")
                when (OS.current) {
                    OS.Linux -> {
                        when (CpuArch.current) {
                            CpuArch.aarch64 -> {
                                dependsOn(
                                    "linuxArm64Test",
                                )
                            }

                            CpuArch.x64 -> {
                                dependsOn(
                                    "linuxX64Test",
                                )
                            }
                        }
                    }

                    OS.macOS -> {
                        dependsOn(
                            "testDebugUnitTest",
                            "iosSimulatorArm64Test",
                            "iosX64Test",
                            "macosArm64Test",
                            "macosX64Test",
                        )
                    }

                    OS.Windows -> {
                        dependsOn(
                            "mingwX64Test",
                        )
                    }
                }
            }
        }
        if (extensions.findByType<KotlinBaseExtension>() != null) {
            setupKtLint(rootLibs.findLibrary("ktlint-cli").get())
        }
        if (extensions.findByType<KotlinJvmExtension>() != null) {
            setupKotlinJvm()
        }
        if (extensions.findByType<DetektExtension>() != null) {
            setupDetekt()
        }
        if (extensions.findByType<DokkaExtension>() != null) {
            setupDokka(copyright = "Ensody GmbH")
        }
        if (extensions.findByType<CatalogPluginExtension>() != null) {
            setupVersionCatalog()
        }
        if (extensions.findByType<GradlePluginDevelopmentExtension>() != null) {
            setupGradlePlugin(rootLibs.findVersion("kotlinForGradlePlugins").get().toString())
        }
        extensions.findByType<MavenPublishBaseExtension>()?.apply {
            configureBasedOnAppliedPlugins(sourcesJar = true, javadocJar = System.getenv("RUNNING_ON_CI") == "true")
            publishToMavenCentral(automaticRelease = true, validateDeployment = false)
            if (System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")?.isNotBlank() == true) {
                signAllPublications()
            }
            pom {
                name = "${rootProject.name}: ${project.name}"
                description = project.description?.takeIf { it.isNotBlank() }
                    ?: "Kotlin Multiplatform compression algorithms"
                url = "https://github.com/ensody/Kompressor"
                licenses {
                    apache2()
                }
                scm {
                    url.set(this@pom.url)
                }
                developers {
                    developer {
                        id = "wkornewald"
                        name = "Waldemar Kornewald"
                        url = "https://www.ensody.com"
                        organization = "Ensody GmbH"
                        organizationUrl = url
                    }
                }
            }
        }
        extensions.findByType<PublishingExtension>()?.apply {
            repositories {
                maven {
                    name = "localMaven"
                    val outputDir = File(rootDir, "build/localmaven")
                    url = outputDir.toURI()
                }
            }
        }
        block()
    }
}
