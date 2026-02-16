import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic.conditionalandroid")
    id("com.ensody.build-logic.kmp")
    id("com.ensody.build-logic.publish")
}

setupBuildLogic(excludeJs = true) {
    kotlin {
        sourceSets.commonMain.dependencies {
            api(project(":kompressor-ktor"))
            api(project(":kompressor-zlib--nativelib"))
        }
        sourceSets.commonTest.dependencies {
            implementation(project(":kompressor-test"))
        }
    }
}
