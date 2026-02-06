import com.ensody.buildlogic.allJs
import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic.conditionalandroid")
    id("com.ensody.build-logic.kmp")
    id("com.ensody.build-logic.publish")
}

setupBuildLogic {
    kotlin {
        allJs()
        sourceSets.commonMain.dependencies {
            api(libs.coroutines.core)
            api(project(":kompressor-core"))
            api(project(":kompressor-kotlinx-io"))
            api(libs.kotlin.test.main)
        }
    }
}
