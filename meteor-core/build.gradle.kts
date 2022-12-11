@file:Suppress("UnstableApiUsage")

plugins {
    id("fabric-loom") version "1.0-SNAPSHOT"
}

version = project.property("api_version").toString()

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    // API
    implementation(project(path = ":meteor-api", configuration = "namedElements"))
    include(project(path = ":meteor-api", configuration = "namedElements"))
}

tasks.withType<ProcessResources> {
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
