plugins {
    id("java")
    id("application")
    id("com.diffplug.spotless") version "8.7.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "br.edu.ifal.lsor"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-cli:commons-cli:1.11.0")
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.26.0"))
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-core")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("br.edu.ifal.lsor.chat.ChatApplicationMain")
}

javafx {
    version = "17.0.19"
    modules = listOf("javafx.controls")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runClient") {
    group = "application"
    description = "Runs the JavaFX chat client."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("br.edu.ifal.lsor.chat.ChatApplicationMain")
    args(
        "--client",
        "--host",
        providers.gradleProperty("chat.host").getOrElse("127.0.0.1"),
        "--port",
        providers.gradleProperty("chat.port").getOrElse("8080")
    )
}

tasks.register<JavaExec>("runServer") {
    group = "application"
    description = "Runs the chat server."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("br.edu.ifal.lsor.chat.ChatApplicationMain")
    args(
        "--server",
        "--host",
        providers.gradleProperty("chat.host").getOrElse("0.0.0.0"),
        "--port",
        providers.gradleProperty("chat.port").getOrElse("8080")
    )
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "br.edu.ifal.lsor.chat.ChatApplicationMain",
            "Implementation-Version" to project.version
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}

spotless {
    java {
        googleJavaFormat("1.22.0")
        target("src/**/*.java")
    }
}
