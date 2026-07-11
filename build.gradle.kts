import groovy.json.JsonOutput
import groovy.json.JsonSlurper

plugins {
    kotlin("multiplatform") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
}

repositories {
    mavenCentral()
}

val decksDir = layout.projectDirectory.dir("src/jsMain/resources/decks")
val generatedResourcesDir = layout.buildDirectory.dir("generated/deckIndex")

/**
 * Generates `decks/index.json` from the deck JSON files, so the index never has to be
 * hand-maintained. Emits a list of deck summaries (`id`, `title`, `cardCount`) ordered by each
 * deck's `order` field (then filename). Output goes into a generated-resources dir that is wired
 * into the jsMain resources below, so it ships at the same relative `decks/index.json` path.
 */
val generateDeckIndex by tasks.registering {
    inputs.dir(decksDir)
    outputs.dir(generatedResourcesDir)
    doLast {
        val slurper = JsonSlurper()
        val summaries = (decksDir.asFile.listFiles { f -> f.extension == "json" && f.name != "index.json" }
            ?: emptyArray())
            .map { file ->
                @Suppress("UNCHECKED_CAST")
                val obj = slurper.parse(file) as Map<String, Any?>
                val order = (obj["order"] as? Number)?.toInt() ?: Int.MAX_VALUE
                Triple(order, file.name, obj)
            }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .map { (_, _, obj) ->
                mapOf(
                    "id" to obj["id"],
                    "title" to obj["title"],
                    "cardCount" to (obj["cards"] as List<*>).size,
                )
            }

        val outDir = generatedResourcesDir.get().dir("decks").asFile
        outDir.mkdirs()
        outDir.resolve("index.json").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(summaries)))
    }
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
            }
        }
        val jsMain by getting {
            resources.srcDir(generatedResourcesDir)
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.12.0")
            }
        }
    }
}

// The generated index must exist before resources are processed into the bundle.
tasks.named("jsProcessResources") { dependsOn(generateDeckIndex) }
