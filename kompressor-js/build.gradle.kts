import com.ensody.buildlogic.allJs
import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic.kmp")
    id("com.ensody.build-logic.publish")
}

setupBuildLogic(includeDefaultTargets = false) {
    kotlin {
        allJs()
        sourceSets.commonMain.dependencies {
            api(project(":kompressor-core"))
        }
        sourceSets.commonTest.dependencies {
            implementation(libs.coroutines.test)
            implementation(project(":kompressor-test"))
        }
    }
}
