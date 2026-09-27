import net.ltgt.gradle.errorprone.errorprone

plugins {
    alias(libs.plugins.fabric.loom)
    id("maven-publish")
    alias(libs.plugins.errorprone)
}

val archivesBaseName = providers.gradleProperty("archives_base_name").get()
val mavenGroup = providers.gradleProperty("maven_group").get()
val runErrorProne = providers.gradleProperty("errorprone").isPresent

base {
    archivesName = archivesBaseName
    group = mavenGroup

    val suffix = providers.gradleProperty("build_number").getOrElse("local")
    version = "${libs.versions.minecraft.get()}-$suffix"
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
    maven {
        name = "ViaVersion"
        url = uri("https://repo.viaversion.com")
    }
    mavenCentral()

    exclusiveContent {
        forRepository {
            maven {
                name = "modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

val modInclude = configurations.create("modInclude")
val jij = configurations.create("jij")
val launcher = sourceSets.create("launcher") {
    java.srcDir("src/launcher/java")
}

configurations {
    // include mods
    implementation.configure {
        extendsFrom(modInclude)
    }
    include.configure {
        extendsFrom(modInclude)
    }

    // include libraries (jar-in-jar)
    implementation.configure {
        extendsFrom(jij)
    }
    include.configure {
        extendsFrom(jij)
    }
}

dependencies {
    // Fabric
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)

    val fapiVersion = libs.versions.fabric.api.get()
    modInclude(fabricApi.module("fabric-api-base", fapiVersion))
    modInclude(fabricApi.module("fabric-resource-loader-v1", fapiVersion))

    // Compat fixes
    compileOnly(fabricApi.module("fabric-renderer-indigo", fapiVersion))
    compileOnly(libs.sodium) { isTransitive = false }
    compileOnly(libs.lithium) { isTransitive = false }
    compileOnly(libs.iris) { isTransitive = false }
    compileOnly(libs.viafabricplus) { isTransitive = false }
    compileOnly(libs.viafabricplus.api) { isTransitive = false }

    compileOnly(libs.baritone)
    compileOnly(libs.modmenu)

    // Libraries (JAR-in-JAR)
    jij(libs.orbit)
    jij(libs.starscript)
    jij(libs.discord.ipc)
    jij(libs.reflections)
    jij(libs.netty.handler.proxy) { isTransitive = false }
    jij(libs.netty.codec.socks) { isTransitive = false }
    jij(libs.waybackauthlib)
    jij(libs.minecraft.auth) {
        exclude("com.google.code.gson")
        exclude("com.google.errorprone")
    }

    // Error Prone
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }

    if (System.getenv("CI")?.toBoolean() == true) {
        withSourcesJar()
        withJavadocJar()
    }
}

// Handle transitive dependencies for jar-in-jar
// Based on implementation from BaseProject by florianreuth/EnZaXD
// Source: https://github.com/florianreuth/BaseProject/blob/main/src/main/kotlin/de/florianreuth/baseproject/Fabric.kt
// Licensed under Apache License 2.0
val jijExcluded = setOf("org.slf4j", "jsr305")
listOf("api", "implementation", "include").forEach { configName ->
    configurations.named(configName).configure {
        defaultDependencies {
            configurations.getByName("jij").incoming.resolutionResult.allComponents
                .mapNotNull { it.id as? ModuleComponentIdentifier }
                .forEach { id ->
                    val notation = "${id.group}:${id.module}:${id.version}"
                    if (jijExcluded.none { notation.contains(it) }) {
                        add(project.dependencies.create(notation) {
                            isTransitive = false
                        })
                    }
                }
        }
    }
}

loom {
    accessWidenerPath = file("src/main/resources/meteor-client.classtweaker")
}

fun toMinecraftCompat(version: String): String {
    // Stable release
    val stable = Regex("""^(\d{2})\.([1-9]\d*)(?:\.(\d+))?$""")

    stable.matchEntire(version)?.let {
        val (year, drop, _) = it.destructured
        return "~$year.$drop"
    }

    // Prerelease
    val pre = Regex("""^(\d{2})\.([1-9]\d*)-pre[-.](\d+)$""")
    pre.matchEntire(version)?.let {
        return version.replace("-pre-", "-pre.")
    }

    // Release Candidate
    val rc = Regex("""^(\d{2})\.([1-9]\d*)-rc[-.](\d+)$""")
    rc.matchEntire(version)?.let {
        return version.replace("-rc-", "-rc.")
    }

    // fallback
    return version
}

tasks {
    processResources {
        val buildNumber = providers.gradleProperty("build_number").getOrElse("")
        val commit = providers.gradleProperty("commit").getOrElse("")

        val propertyMap = mapOf(
            "version" to project.version,
            "build_number" to buildNumber,
            "commit" to commit,
            "jdk_version" to libs.versions.jdk.get(),
            "minecraft_version" to toMinecraftCompat(libs.versions.minecraft.get()),
            "loader_version" to libs.versions.fabric.loader.get()
        )

        inputs.properties(propertyMap)
        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    // Compile launcher with Java 8 for backwards compatibility
    named<JavaCompile>("compileLauncherJava").configure {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
        options.compilerArgs.add("-Xlint:-options")
    }

    jar {
        inputs.property("archivesName", archivesBaseName)

        from("LICENSE") {
            rename { "${it}_$archivesBaseName" }
        }

        // Include launcher classes
        from(launcher.output)

        manifest {
            attributes["Main-Class"] = "meteordevelopment.meteorclient.Main"
        }
    }

    withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:deprecation",
                "-Xlint:unchecked"
            )
        )

        options.errorprone.enabled.set(runErrorProne)

        if (runErrorProne) {
            options.errorprone {
                check("NullAway", net.ltgt.gradle.errorprone.CheckSeverity.ERROR)
                option("NullAway:AnnotatedPackages", "meteordevelopment.meteorclient")
                option("NullAway:JSpecifyMode", "true")
                // Event handlers are discovered reflectively by Orbit.
                option("UnusedMethod:ExcludedAnnotations", "meteordevelopment.orbit.EventHandler")
            }
        }
    }

    javadoc {
        with(options as StandardJavadocDocletOptions) {
            addStringOption("Xdoclint:none", "-quiet")
            addStringOption("encoding", "UTF-8")
            addStringOption("charSet", "UTF-8")
        }
    }

    build {
        if (System.getenv("CI")?.toBoolean() == true) {
            dependsOn("javadocJar")
        }
    }
}

// ============================================================
// Translation tooling
// ============================================================

val langDir = layout.projectDirectory.dir("src/main/resources/assets/meteor-client/lang")

val validateTranslations by tasks.registering {
    group = "translation"
    description = "Validates all lang files against en_us.json. Fails on any error."

    val langDirProvider = langDir
    doLast {
        val slurper = groovy.json.JsonSlurper()
        val dir = langDirProvider.asFile
        val enUsFile = dir.resolve("en_us.json")

        if (!enUsFile.exists()) throw GradleException("en_us.json not found at ${enUsFile}")

        @Suppress("UNCHECKED_CAST")
        val canonical = slurper.parse(enUsFile) as Map<String, String>
        // Vanilla-format keys (KeyMapping / key categories) are consumed by Minecraft itself
        // and are exempt from the Meteor key schema and from per-language completeness.
        val vanillaKeys = canonical.keys.filter { it.startsWith("key.") }.toSet()
        val canonicalKeys = canonical.keys - vanillaKeys
        val failures = mutableListOf<String>()

        // Key format validation on en_us
        val keyPattern = Regex("^[a-z0-9][a-z0-9-]*(\\.[a-z0-9][a-z0-9-]*){1,}$")
        canonicalKeys.forEach { key ->
            if (!keyPattern.matches(key)) failures += "[en_us] INVALID KEY FORMAT: $key"
        }

        // Duplicate value detection in en_us (advisory only)
        val valueToKeys = mutableMapOf<String, MutableList<String>>()
        canonical.forEach { (k, v) -> valueToKeys.getOrPut(v) { mutableListOf() }.add(k) }
        valueToKeys.filter { it.value.size > 1 }.forEach { (v, ks) ->
            println("  [en_us] INFO - same value for multiple keys (review if intentional): \"$v\"")
            ks.forEach { println("    -> $it") }
        }

        // Per-language validation
        dir.listFiles()?.forEach { langFile ->
            if (langFile.name == "en_us.json" || !langFile.name.endsWith(".json")) return@forEach
            val code = langFile.name.removeSuffix(".json")
            @Suppress("UNCHECKED_CAST")
            val entries = slurper.parse(langFile) as Map<String, String>
            val langKeys = entries.keys - vanillaKeys

            // Missing entries are advisory: the runtime falls back to en_us, so a partial
            // community locale is a supported state rather than an error.
            val missing = canonicalKeys - langKeys
            if (missing.isNotEmpty()) {
                println("  [$code] INFO - ${missing.size} key(s) missing, will fall back to en_us")
            }
            (langKeys - canonicalKeys).forEach { failures += "[$code] UNKNOWN KEY: $it" }

            entries.forEach { (key, value) ->
                val en = canonical[key] ?: return@forEach
                val count = { s: String -> Regex("%[sdf]").findAll(s).count() }
                if (count(en) != count(value)) {
                    failures += "[$code] PLACEHOLDER MISMATCH on '$key': en has ${count(en)} arg(s), $code has ${count(value)}"
                }
            }
        }

        if (failures.isEmpty()) {
            println("\u2713 validateTranslations passed - all language files are valid.")
        } else {
            failures.forEach { println("  \u2717 $it") }
            throw GradleException("${failures.size} translation validation failure(s). See above.")
        }
    }
}

val extractTranslations by tasks.registering {
    group = "translation"
    description = "Scans source for remaining hardcoded user-facing strings."

    doLast {
        val results = mutableListOf<String>()
        val pattern = Regex("\\.(name|description)\\(\"([^\"]{2,})\"\\)")
        fileTree("src/main/java/meteordevelopment") {
            include("**/*.java")
        }.forEach { f ->
            pattern.findAll(f.readText()).forEach { m ->
                results += "${f.name}:${m.groupValues[1]}(\"${m.groupValues[2]}\")"
            }
        }
        if (results.isEmpty()) {
            println("\u2713 extractTranslations - no unmigrated hardcoded strings detected.")
        } else {
            println("Found ${results.size} possibly unmigrated strings:")
            results.forEach { println("  $it") }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "meteor-client"

            version = "${libs.versions.minecraft.get()}-SNAPSHOT"
        }
    }

    repositories {
        maven("https://maven.meteordev.org/snapshots") {
            name = "meteor-maven"

            credentials {
                username = System.getenv("MAVEN_METEOR_ALIAS")
                password = System.getenv("MAVEN_METEOR_TOKEN")
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}
