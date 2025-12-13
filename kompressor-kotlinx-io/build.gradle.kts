import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic.android")
    id("com.ensody.build-logic.kmp")
    id("com.ensody.build-logic.publish")
}

setupBuildLogic {
    kotlin {
        sourceSets.commonMain.dependencies {
            api(project(":kompressor-core"))
            api(libs.kotlinx.io)
        }
        sourceSets.commonTest.dependencies {
            implementation(project(":kompressor-test"))
        }
    }
}
