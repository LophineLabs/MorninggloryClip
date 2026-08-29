import org.gradle.api.file.DuplicatesStrategy

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://repo.leavesmc.org/releases/")
    maven("https://repo.leavesmc.org/snapshots/")
}

dependencies {
    implementation("io.sigpipe:jbsdiff:1.0")
    implementation("org.leavesmc:leaves-plugin-mixin-condition:1.0.0")
    implementation("io.github.llamalad7:mixinextras-common:0.4.1")
    implementation("net.fabricmc:access-widener:2.1.0")
    implementation("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7") {
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.jetbrains:annotations:15.0")
}

tasks.shadowJar {
    // ServiceLoader descriptors must follow relocated Mixin interfaces/classes.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()

    val prefix = "morninggloryclip.libs"
    listOf(
        "org.apache",
        "org.tukaani",
        "io.sigpipe",
        "com.google",
        "org.spongepowered",
        "org.leavesmc",
        "net.fabricmc.accesswidener",
        "io.github.llamalad7"
    ).forEach { pack ->
        relocate(pack, "$prefix.$pack")
    }

    exclude("META-INF/LICENSE.txt")
    exclude("META-INF/NOTICE.txt")
}
