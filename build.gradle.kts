plugins {
    alias(libs.plugins.fabric.loom)
    id("maven-publish")
}

base {
    archivesName = properties["archives_base_name"] as String
    group = properties["maven_group"] as String

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
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com")
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

val modInclude: Configuration by configurations.creating
val jij: Configuration by configurations.creating

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
}

sourceSets {
    val launcher by creating {
        java {
            srcDir("src/launcher/java")
        }
    }
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
    val match = Regex("""^(\d{2})\.([1-9]\d*)(?:\.([1-9]\d*))?$""")
        .matchEntire(version)
        ?: error("Invalid Minecraft version format: $version. Expected YY.D or YY.D.H")

    val (year, drop, _) = match.destructured
    return "~$year.$drop"
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
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }

        // Include launcher classes
        from(sourceSets["launcher"].output)

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
