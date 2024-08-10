import java.net.URI

plugins {
    id("fabric-loom") version "1.7-SNAPSHOT"
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val devBuild: String = (project.findProperty("devbuild") ?: "").toString()
val commit: String = (project.findProperty("commit") ?: "").toString()

base {
    archivesName = project.property("archives_base_name").toString()
    version = "${project.property("mod_version").toString()}-$devBuild"
    group = project.property("maven_group").toString()
}

repositories {
    maven("https://maven.meteordev.org/releases") {
        name = "meteor-maven"
    }
    maven("https://maven.meteordev.org/snapshots") {
        name = "meteor-maven-snapshots"
    }
    maven("https://maven.bawnorton.com/releases") {
        name = "modrinth"
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://maven.vram.io//") {
        name = "vram"
    }
    maven("https://repo.viaversion.com") {
        name = "viaversion"
    }
    mavenCentral()
}

configurations {
    // modImplementation.configure { extendsFrom(modInclude) } TODO
    // include.configure { extendsFrom(modInclude) } TODO

     implementation.configure { extendsFrom(library) } TODO
     shadow.configure { extendsFrom(library) } TODO
}

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    include(fabricApi.module("fabric-resource-loader-v0", project.property("fapi_version").toString()))

    // Compat fixes
    modCompileOnly(fabricApi.module("fabric-renderer-indigo", project.property("fapi_version").toString()))
    modCompileOnly("maven.modrinth.sodium:${project.property("sodium_version").toString()}") { isTransitive = false }
    modCompileOnly("maven.modrinth.lithium:${project.property("lithium_version").toString()}") { isTransitive = false }
    modCompileOnly("maven.modrinth.iris:${project.property("iris_version").toString()}") { isTransitive = false }
    modCompileOnly("maven.modrinth.indium:${project.property("indium_version").toString()}") { isTransitive = false }
    modCompileOnly("de.florianmichael:ViaFabricPlus:${project.property("viafabricplus_version").toString()}") { isTransitive = false }

    // Baritone
    modCompileOnly("meteordevelopment:baritone:${project.property("baritone_version").toString()}")

    // Libraries

    // library "meteordevelopment:orbit:${project.orbit_version}" TODO
    // library "meteordevelopment:starscript:${project.starscript_version}" TODO
    // library "meteordevelopment:discord-ipc:${project.discordipc_version}" TODO
    // library "org.reflections:reflections:${project.reflections_version}" TODO
    // library("io.netty:netty-handler-proxy:${project.netty_version}") { transitive = false } TODO
    // library("io.netty:netty-codec-socks:${project.netty_version}") { transitive = false } TODO
    // library "de.florianmichael:WaybackAuthLib:${project.waybackauthlib_version}" TODO

    // Launch sub project
    shadow(project(":launch"))
}

loom {
    accessWidenerPath = file("src/main/resources/meteor-client.accesswidener")
}

afterEvaluate {

    // TODO figure this out
    // migrateMappings.configure {
    //     outputDir = project.file("src/main/java")
    // }
}

tasks {
    processResources {
        val propertiesMap = mapOf(
            "version" to project.version,
            "devbuild" to devBuild,
            "commit" to commit,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version")
        )

        inputs.properties(propertiesMap)
        filesMatching("fabric.mod.json") {
            expand(propertiesMap)
        }
    }

    jar {
        from("LICENSE") {
            rename{
                "${it}_${project.base.archivesName}"
            }
        }

        manifest {
            attributes["Main-Class"] = "meteordevelopment.meteorclient.main"
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21

        withSourcesJar()
        withJavadocJar()
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())

        from("LICENSE") {
            rename { "${it}_${project.base.archivesName}" }
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
        options {
            this as StandardJavadocDocletOptions
            encoding = "UTF-8"
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    build {
        // dependsOn(javadocJar) TODO
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "meteor-client"

            version = project.property("mod_version").toString()
            if (project.hasProperty("devbuild")) version += "-SNAPSHOT"
        }
    }

    repositories {
        maven {
            name = "meteor-maven"
            url = if (project.hasProperty("devbuild"))
                URI("https://maven.meteordev.org/snapshots")
            else URI("https://maven.meteordev.org/releases")

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
