plugins {
    kotlin("multiplatform") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
}

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "flasher.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.12.0")
            }
        }
    }
}
