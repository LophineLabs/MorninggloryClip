plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

rootProject.name = "morninggloryclip"
include("java6", "java17")
project(":java17").projectDir = file("java17")
