import com.ensody.nativebuilds.jniNativeBuild
import com.ensody.buildlogic.setupBuildLogic
import com.ensody.nativebuilds.cinterops

plugins {
    id("com.ensody.build-logic.conditionalandroid")
    id("com.ensody.build-logic.kmp")
    id("com.ensody.build-logic.publish")
    id("com.ensody.nativebuilds")
}

setupBuildLogic(excludeJs = true) {
    kotlin {
        sourceSets.commonMain.dependencies {
            api(project(":kompressor-core"))
        }
        sourceSets.commonTest.dependencies {
            implementation(project(":kompressor-test"))
        }
        sourceSets["nonJsMain"].dependencies {
            api(libs.nativebuilds.zstd.core)
        }

        cinterops(libs.nativebuilds.zstd.headers) {
            definitionFile.set(file("src/nativeMain/cinterop/lib.def"))
        }
    }

    jniNativeBuild(
        name = "zstd-jni",
        nativeBuilds = listOf(
            libs.nativebuilds.zstd.headers,
            libs.nativebuilds.zstd.core,
        ),
    ) {
        includeDirs.from("../jni/common/include")
        inputFiles.from("src/jvmCommonMain/jni", "../jni/common/src")
    }
}
