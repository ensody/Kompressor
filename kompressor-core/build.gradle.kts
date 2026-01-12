import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic.conditionalandroid")
    id("com.ensody.build-logic.kmp")
    id("com.ensody.build-logic.publish")
}

setupBuildLogic {
    kotlin {
        sourceSets.commonMain.dependencies {
            api(libs.coroutines.core)
        }
        sourceSets["jvmCommonMain"].dependencies {
            api(libs.nativebuilds.loader)
        }
        sourceSets.commonTest.dependencies {
            implementation(project(":kompressor-test"))
        }
    }
}
