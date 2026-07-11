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
 * Validates every deck JSON file against the contract in `docs/deck.schema.json`, plus the
 * filename rule a JSON Schema can't see: the filename stem is the deck id, so it must be
 * kebab-case and ≤ 50 chars (id uniqueness is then free — filenames are unique). Enforces the
 * required fields and max lengths (title 60, card front/back 200) so a malformed or oversized
 * deck can't slip into the bundle or the generated index. Accumulates all problems and fails
 * with one message listing them. See CONTRIBUTING-DECKS.md.
 */
val validateDecks by tasks.registering {
    inputs.dir(decksDir)
    doLast {
        val slurper = JsonSlurper()
        val stemPattern = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
        val errors = mutableListOf<String>()

        val files = (decksDir.asFile.listFiles { f -> f.extension == "json" && f.name != "index.json" }
            ?: emptyArray()).sortedBy { it.name }

        fun err(file: String, msg: String) = errors.add("$file: $msg")

        for (file in files) {
            val name = file.name
            val stem = file.nameWithoutExtension
            // The filename IS the deck id, so validate the stem. (Uniqueness is free: the filesystem
            // guarantees distinct filenames, hence distinct ids.)
            when {
                stem.length > 50 -> err(name, "filename stem must be at most 50 characters (it is the deck id)")
                !stemPattern.matches(stem) -> err(name, "filename must be kebab-case (lowercase letters, digits, hyphens): \"$name\"")
            }

            val parsed = try {
                slurper.parse(file)
            } catch (e: Exception) {
                err(name, "not valid JSON (${e.message})")
                continue
            }
            val obj = parsed as? Map<*, *>
            if (obj == null) {
                err(name, "top level must be a JSON object")
                continue
            }

            if (obj.containsKey("id")) err(name, "remove the `id` field — the filename is the deck id")

            val title = obj["title"]
            when {
                title !is String || title.isBlank() -> err(name, "`title` is required and must be a non-blank string")
                title.length > 60 -> err(name, "`title` must be at most 60 characters")
            }

            val order = obj["order"]
            if (order != null && order !is Number) err(name, "`order` must be an integer if present")

            val cards = obj["cards"]
            if (cards !is List<*>) {
                err(name, "`cards` is required and must be an array")
            } else if (cards.isEmpty()) {
                err(name, "`cards` must contain at least one card")
            } else {
                cards.forEachIndexed { i, card ->
                    val c = card as? Map<*, *>
                    if (c == null) {
                        err(name, "card ${i + 1} must be an object")
                        return@forEachIndexed
                    }
                    for (side in listOf("front", "back")) {
                        val v = c[side]
                        when {
                            v !is String || v.isBlank() -> err(name, "card ${i + 1} `$side` is required and must be a non-blank string")
                            v.length > 200 -> err(name, "card ${i + 1} `$side` must be at most 200 characters")
                        }
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            throw GradleException(
                "Deck validation failed (${errors.size} problem(s)); see CONTRIBUTING-DECKS.md:\n" +
                    errors.joinToString("\n") { "  - $it" }
            )
        }
    }
}

/**
 * Generates `decks/index.json` from the deck JSON files, so the index never has to be
 * hand-maintained. Emits a list of deck summaries (`id`, `title`, `cardCount`) ordered by each
 * deck's `order` field (then filename). Output goes into a generated-resources dir that is wired
 * into the jsMain resources below, so it ships at the same relative `decks/index.json` path.
 */
val generateDeckIndex by tasks.registering {
    dependsOn(validateDecks)
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
            .map { (_, fileName, obj) ->
                mapOf(
                    "id" to fileName.removeSuffix(".json"),
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

// Validate decks as part of `check` so local test runs and CI catch bad decks.
tasks.named("check") { dependsOn(validateDecks) }
