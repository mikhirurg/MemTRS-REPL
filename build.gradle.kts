plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

group = "io.github.mikhirurg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(project(":cora:app"))

    // Source: https://mvnrepository.com/artifact/jline/jline
    implementation("jline:jline:2.14.6")
}

tasks.test {
    useJUnitPlatform()
}

application {
    // Define the main class for the application.
    mainClass.set("io.github.mikhirurg.repl.MemTRSREPL")

    //Sets the application DefaultJVMArgs
    applicationDefaultJvmArgs = listOf("--enable-preview")
}

tasks {
    // Compiler options with preview java features enabled
    val COMPILER_OPTIONS =
        listOf("--enable-preview", "-Xlint:preview", "-Xlint:deprecation", "-Xlint:unchecked")

    withType<JavaCompile>() {
        options.compilerArgs = COMPILER_OPTIONS;
        options.encoding = "UTF8"
    }

    named<Test>("test") {
        // Use JUnit Platform for unit tests.
        useJUnitPlatform()
        jvmArgs = listOf("--enable-preview")
    }

    named<JavaExec>("run") {
        jvmArgs = listOf("--enable-preview")
        standardInput = System.`in`
    }

    named<Jar>("jar") {
        archiveBaseName.set("memtrs")
        archiveVersion.set("0.1")
        archiveClassifier.set("release")

        manifest {
            attributes["Main-Class"] = application.mainClass.get()
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith("jar") }
                .map { zipTree(it) }
        })
    }
}