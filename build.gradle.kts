import java.util.Properties

plugins {
    alias(libs.plugins.fabric.loom)
    id("maven-publish")
}

// Multi-version build.
//
// Every supported Minecraft version is described by gradle/mc-versions/<version>.properties
// and may ship its own sources in src/mc/<version>/. The shared code in src/main/java stays
// version-agnostic and programs against the compatibility layer (see COMPAT.md).
//
// Select a version with -PminecraftVersion=<version>; the default is taken from libs.versions.toml.
val defaultMcVersion = libs.versions.minecraft.get()
val mcVersion = providers.gradleProperty("minecraftVersion").getOrElse(defaultMcVersion)
val mcVersionsDir = file("gradle/mc-versions")
val mcDescriptor = File(mcVersionsDir, "$mcVersion.properties")

require(mcDescriptor.isFile) {
    buildString {
        append("Unknown Minecraft version '").append(mcVersion).append("'. Expected a descriptor at ").append(mcDescriptor).append(". ")
        val available = mcVersionsDir.listFiles()?.filter { it.name.endsWith(".properties") }
            ?.map { it.name.removeSuffix(".properties") }?.sorted().orEmpty()
        append(if (available.isEmpty()) "No version descriptors found." else "Available versions: ${available.joinToString(", ")}.")
    }
}

val mcProperties = Properties().apply { mcDescriptor.inputStream().use { load(it) } }

fun mc(key: String): String = mcProperties.getProperty(key)
    ?: error("Missing property '$key' in $mcDescriptor")

logger.lifecycle("Building Meteor Client for Minecraft $mcVersion")

val archivesBaseName = providers.gradleProperty("archives_base_name").get()
val mavenGroup = providers.gradleProperty("maven_group").get()

base {
    archivesName = archivesBaseName
    group = mavenGroup

    val suffix = providers.gradleProperty("build_number").getOrElse("local")
    version = "${mc("minecraft")}-$suffix"
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

// Version-specific sources. Only the selected version's directory is compiled, so every
// supported version can use its own mappings for the version-fragile parts of the mod.
sourceSets.named("main") {
    java.srcDir("src/mc/$mcVersion")
    resources.srcDir("src/mc/$mcVersion")
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
    minecraft("com.mojang:minecraft:${mc("minecraft")}")
    implementation(libs.fabric.loader)

    val fapiVersion = mc("fabric-api")
    modInclude(fabricApi.module("fabric-api-base", fapiVersion))
    modInclude(fabricApi.module("fabric-resource-loader-v1", fapiVersion))

    // Compat fixes
    compileOnly(fabricApi.module("fabric-renderer-indigo", fapiVersion))


    // Libraries (JAR-in-JAR)
    jij(libs.orbit)
    jij(libs.starscript)
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

// The access widener is version specific too: prefer gradle/mc-versions/<version>.classtweaker
// and fall back to the shared one in src/main/resources.
val mcAccessWidener = sequenceOf(
    File(mcVersionsDir, "$mcVersion.classtweaker"),
    file("src/main/resources/meteor-client.classtweaker")
).first { it.isFile }

loom {
    accessWidenerPath = mcAccessWidener
}

// Architecture guard: the version-agnostic part of the compatibility layer must never
// import Minecraft directly. Everything Minecraft-specific belongs in a compat/impl package.
val compatApiDirs = buildList {
    file("src/main/java/meteordevelopment/meteorclient/compat")
        .takeIf { it.isDirectory }?.let { add(it) }

    file("src/mc").listFiles()?.filter { it.isDirectory }?.forEach { versionDir ->
        File(versionDir, "meteordevelopment/meteorclient/compat")
            .takeIf { it.isDirectory }?.let { add(it) }
    }
}

val checkCompatLayer = tasks.register("checkCompatLayer") {
    group = "verification"
    description = "Checks that the version-agnostic compatibility API does not depend on Minecraft."

    inputs.files(compatApiDirs).withPropertyName("compatApi")
    outputs.upToDateWhen { false }

    doLast {
        val offenders = compatApiDirs.asSequence()
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.name.endsWith(".java") } }
            .filter { it.parentFile.name != "impl" }
            .filter { file -> file.readLines().any { line -> line.trimStart().startsWith("import net.minecraft") } }
            .map { it.path }
            .toList()

        if (offenders.isNotEmpty()) {
            throw GradleException(
                "The compatibility API must stay version-agnostic, but net.minecraft imports were found in:\n" +
                    offenders.joinToString("\n") { "  - $it" } +
                    "\nMove version-specific code into a ...compat.impl package (see COMPAT.md)."
            )
        }

        logger.lifecycle("Compatibility API is version-agnostic (${compatApiDirs.size} source roots checked).")
    }
}

tasks.named("check") {
    dependsOn(checkCompatLayer)
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
            "minecraft_version" to toMinecraftCompat(mc("minecraft")),
            "loader_version" to libs.versions.fabric.loader.get(),
            "compat_impl" to mc("compat-impl")
        )

        inputs.properties(propertyMap)
        filesMatching(listOf("fabric.mod.json", "meteor-compat.properties")) {
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "meteor-client"

            version = "${mc("minecraft")}-SNAPSHOT"
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
