import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic.bom")
    id("com.ensody.build-logic.publish")
}

setupBuildLogic {
    dependencies {
        constraints {
            api(project(":kompressor-brotli--nativelib"))
            api(project(":kompressor-brotli-ktor"))
            api(project(":kompressor-core"))
            api(project(":kompressor-js"))
            api(project(":kompressor-kotlinx-io"))
            api(project(":kompressor-ktor"))
            api(project(":kompressor-test"))
            api(project(":kompressor-zlib--nativelib"))
            api(project(":kompressor-zlib-ktor"))
            api(project(":kompressor-zstd--nativelib"))
            api(project(":kompressor-zstd-ktor"))
        }
    }
}
