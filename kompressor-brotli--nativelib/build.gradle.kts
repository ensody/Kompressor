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
            api(libs.nativebuilds.brotli.common)
            api(libs.nativebuilds.brotli.dec)
            api(libs.nativebuilds.brotli.enc)
        }

        cinterops(libs.nativebuilds.brotli.headers) {
            definitionFile.set(file("src/nativeMain/cinterop/lib.def"))
        }
    }

    jniNativeBuild(
        name = "brotli-jni",
        nativeBuilds = listOf(
            libs.nativebuilds.brotli.headers,
            libs.nativebuilds.brotli.common,
            libs.nativebuilds.brotli.dec,
            libs.nativebuilds.brotli.enc,
        ),
    ) {
        includeDirs.from("../jni/common/include")
        inputFiles.from("src/jvmCommonMain/jni", "../jni/common/src")
    }
}
