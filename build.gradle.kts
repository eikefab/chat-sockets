plugins {
    id("java")
    id("com.diffplug.spotless") version "8.7.0"
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

tasks.test {
    useJUnitPlatform()
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
