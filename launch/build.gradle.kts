plugins {
    id("java")
}

group = "motordevelopment"
version = "0.1.0"

tasks {
    withType<JavaCompile> {
        options.release = 8
    }
}
