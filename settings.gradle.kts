pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "MemTRSRepl"

include(":cora:app")
project(":cora:app").projectDir = file("cora/app")