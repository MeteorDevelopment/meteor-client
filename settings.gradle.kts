pluginManagement {
    repositories {
        maven {
            name = "FabricMC"
            setUrl("https://maven.fabricmc.net/")
        }
        mavenCentral()
    }
}

include("meteor-api", "meteor-core", "meteor")
