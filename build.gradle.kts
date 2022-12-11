import java.nio.charset.StandardCharsets

allprojects {
    apply {
        plugin("java")
    }

    group = project.property("group").toString()

    repositories {
        maven {
            name = "Meteor - Releases"
            setUrl("https://maven.meteordev.org/releases/")
        }
        maven {
            name = "Meteor - Snapshots"
            setUrl("https://maven.meteordev.org/snapshots/")
        }
        maven {
            name = "Modrinth"
            setUrl("https://api.modrinth.com/maven/")

            content {
                includeGroup("maven.modrinth")
            }
        }
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        options.encoding = StandardCharsets.UTF_8.toString()

        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }
}
