plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

rootProject.name = "morninggloryclip"
include("java6", "java25")

// On-disk `java17` directory was kept (shell could not rename it). The Gradle
// subproject is exposed as `:java25` (matching upstream Hyacinthusclip and the
// JDK 25 toolchain used by the auto-update and leavesplugin features). To
// finish the rename, run `mv java17 java25` and delete this override.
project(":java25").projectDir = file("java17")
