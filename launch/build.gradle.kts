plugins {
    id("java")
}

group = "meteordevelopment"
version = "0.1.0"

tasks {
    withType<JavaCompile> {
        options.release = 8
    }
}
