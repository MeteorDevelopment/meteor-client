plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("maven-publish")
    id("com.gradleup.shadow") version "9.0.0-beta11"
}

base {
    archivesName = properties["archives_base_name"] as String
    group = properties["maven_group"] as String

    val suffix = if (project.hasProperty("build_number")) {
        project.findProperty("build_number")
    } else {
        "local"
    }

    version = properties["minecraft_version"] as String + "-" + suffix
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
val library: Configuration by configurations.creating

configurations {
    // include mods
    modImplementation.configure {
        extendsFrom(modInclude)
    }
    include.configure {
        extendsFrom(modInclude)
    }

    // include libraries
    implementation.configure {
        extendsFrom(library)
    }
    shadow.configure {
        extendsFrom(library)
    }
}

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${properties["minecraft_version"] as String}")
    mappings("net.fabricmc:yarn:${properties["yarn_mappings"] as String}:v2")
    modImplementation("net.fabricmc:fabric-loader:${properties["loader_version"] as String}")

    modInclude(fabricApi.module("fabric-api-base", properties["fapi_version"] as String))
    modInclude(fabricApi.module("fabric-resource-loader-v0", properties["fapi_version"] as String))

    // Compat fixes
    modCompileOnly(fabricApi.module("fabric-renderer-indigo", properties["fapi_version"] as String))
    modCompileOnly("maven.modrinth:sodium:${properties["sodium_version"] as String}") { isTransitive = false }
    modCompileOnly("maven.modrinth:lithium:${properties["lithium_version"] as String}") { isTransitive = false }
    modCompileOnly("maven.modrinth:iris:${properties["iris_version"] as String}") { isTransitive = false }
    modCompileOnly("com.viaversion:viafabricplus:${properties["viafabricplus_version"] as String}") { isTransitive = false }
    modCompileOnly("com.viaversion:viafabricplus-api:${properties["viafabricplus_version"] as String}") { isTransitive = false }

    // Baritone (https://github.com/MeteorDevelopment/baritone)
    modCompileOnly("meteordevelopment:baritone:${properties["baritone_version"] as String}-SNAPSHOT")
    // ModMenu (https://github.com/TerraformersMC/ModMenu)
    modCompileOnly("com.terraformersmc:modmenu:${properties["modmenu_version"] as String}")

    // Libraries
    library("meteordevelopment:orbit:${properties["orbit_version"] as String}")
    library("meteordevelopment:starscript:${properties["starscript_version"] as String}")
    library("meteordevelopment:discord-ipc:${properties["discordipc_version"] as String}")
    library("org.reflections:reflections:${properties["reflections_version"] as String}")
    library("io.netty:netty-handler-proxy:${properties["netty_version"] as String}") { isTransitive = false }
    library("io.netty:netty-codec-socks:${properties["netty_version"] as String}") { isTransitive = false }
    library("de.florianmichael:WaybackAuthLib:${properties["waybackauthlib_version"] as String}")

    // Launch sub project
    shadow(project(":launch"))
}

loom {
    accessWidenerPath = file("src/main/resources/meteor-client.accesswidener")
}

afterEvaluate {
    tasks.migrateMappings.configure {
        outputDir.set(project.file("src/main/java"))
    }
}

tasks {
    processResources {
        val buildNumber = project.findProperty("build_number")?.toString() ?: ""
        val commit = project.findProperty("commit")?.toString() ?: ""

        val propertyMap = mapOf(
            "version" to project.version,
            "build_number" to buildNumber,
            "commit" to commit,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version")
        )

        inputs.properties(propertyMap)
        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }

        manifest {
            attributes["Main-Class"] = "meteordevelopment.meteorclient.Main"
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21

        if (System.getenv("CI")?.toBoolean() == true) {
            withSourcesJar()
            withJavadocJar()
        }
    }

    withType<JavaCompile> {
        options.release = 21
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())

        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }

        dependencies {
            exclude {
                it.moduleGroup == "org.slf4j"
            }
        }
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
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

            version = properties["minecraft_version"] as String + "-SNAPSHOT"
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
