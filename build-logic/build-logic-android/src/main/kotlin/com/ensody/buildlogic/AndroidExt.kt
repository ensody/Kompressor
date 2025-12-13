package com.ensody.buildlogic

import com.android.build.gradle.TestedExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

fun Project.setupAndroid(
    coreLibraryDesugaring: Provider<MinimalExternalModuleDependency>?,
    javaVersion: JavaVersion = JavaVersion.VERSION_17,
) {
    configure<TestedExtension> {
        namespace = getDefaultPackageName()
        testNamespace = "$namespace.unittests"
        val sdk = 36
        compileSdkVersion(sdk)
        defaultConfig {
            minSdk = 21
            targetSdk = sdk
            versionCode = 1
            versionName = project.version as String
            // Required for coreLibraryDesugaring
            multiDexEnabled = true
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            isCoreLibraryDesugaringEnabled = coreLibraryDesugaring != null
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }

        testOptions {
            // Needed for Robolectric
            unitTests {
                // TODO: Remove this workaround for https://issuetracker.google.com/issues/411739086 once fixed in AGP
                isIncludeAndroidResources = listOf("androidUnitTest", "test").any { name ->
                    val sourceSet = file("src/$name")
                    sourceSet.exists() && sourceSet.walkTopDown().any { it.extension == "kt" }
                }
            }
        }

        packagingOptions {
            resources {
                pickFirsts.add("META-INF/*.kotlin_module")
                pickFirsts.add("META-INF/AL2.0")
                pickFirsts.add("META-INF/LGPL2.1")
            }
        }
    }
    if (coreLibraryDesugaring != null) {
        dependencies {
            add("coreLibraryDesugaring", coreLibraryDesugaring)
        }
    }

    // In Android unit tests (e.g. the KMP androidUnitTest sourceSet) we can't load Android JNI libs because the tests
    // run on the host system (Linux, macOS, Windows) instead of an Android emulator.
    // Normally, KMP Android dependencies only contain the embedded native libraries targeting Android hosts.
    // In order to run unit tests with native libs for the host we have to substitute all JNI-using dependencies.
    // The following rule is primarily meant to make all NativeBuilds work automatically:
    // See the project site for more information: https://github.com/ensody/native-builds
    configurations.all {
        // Only apply substitution to Android unit tests
        if (name.endsWith("UnitTestRuntimeClasspath")) {
            resolutionStrategy.eachDependency {
                // The NativeBuilds can all safely be substituted. They contain only the native shared libraries, but
                // no JNI code.
                if (requested.group == "com.ensody.nativebuilds" && requested.name.endsWith("-android") ||
                    // The NativeBuilds libs always get integrated in some JNI wrapper module that contains the actual
                    // JNI code. By convention we call these wrapper modules "...--jni-wrapper" and the final
                    // KMP Android artifact gets published to Maven with an additional "-android" suffix.
                    // This rule should result in a relatively low chance of false positives.
                    requested.name.endsWith("--jni-wrapper-android")
                ) {
                    // Replace with the -jvm artifact
                    useTarget("${requested.group}:${requested.name.removeSuffix("-android")}-jvm:${requested.version}")
                }
            }
        }
    }
}
