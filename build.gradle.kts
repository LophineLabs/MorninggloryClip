plugins {
    java
    application
    `maven-publish`
}

subprojects {
    apply(plugin = "java")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

val mainClass = "fun.bm.morninggloryclip.Main"

tasks.jar {
    val java6Jar = project(":java6").tasks.named("jar")
    val java25Jar = project(":java25").tasks.named("shadowJar")
    dependsOn(java6Jar, java25Jar)

    from(zipTree(java6Jar.map { it.outputs.files.singleFile }))
    from(zipTree(java25Jar.map { it.outputs.files.singleFile }))

    manifest {
        attributes(
            "Main-Class" to mainClass,
            "MorninggloryClip-Version" to project.version.toString()
        )
    }

    from(file("license.txt")) {
        into("META-INF/license")
        rename { "morninggloryclip-LICENSE.txt" }
    }
    rename { name ->
        if (name.endsWith("-LICENSE.txt")) {
            "META-INF/license/$name"
        } else {
            name
        }
    }
}

// Write the version string as a jar resource so AutoUpdate.getResourceAsStreamFromTargetJar
// can read it from /META-INF/morninggloryclip-version. Without this file, the auto-update
// indirection aborts because both jars must agree on the launcher version.
val writeVersionResource by tasks.registering {
    val versionFile = layout.buildDirectory.file("generated-resources/morninggloryclip-version")
    outputs.file(versionFile)
    doLast {
        val f = versionFile.get().asFile
        f.parentFile.mkdirs()
        f.writeText(project.version.toString())
    }
}

tasks.named("jar") {
    dependsOn(writeVersionResource)
    from(writeVersionResource) {
        into("META-INF")
        rename { "morninggloryclip-version" }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    val java6Sources = project(":java6").tasks.named("sourcesJar")
    val java25Sources = project(":java25").tasks.named("sourcesJar")
    dependsOn(java6Sources, java25Sources)

    from(zipTree(java6Sources.map { it.outputs.files.singleFile }))
    from(zipTree(java25Sources.map { it.outputs.files.singleFile }))

    archiveClassifier.set("sources")
}

val isSnapshot = project.version.toString().endsWith("-SNAPSHOT")

publishing {
    publications {
        register<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
            artifact(sourcesJar)
            withoutBuildIdentifier()

            pom {
                val repoPath = "LophineLabs/MorninggloryClip"
                val repoUrl = "https://github.com/$repoPath"

                name.set("MorninggloryClip")
                description.set(project.description)
                url.set(repoUrl)
                packaging = "jar"

                licenses {
                    license {
                        name.set("MIT")
                        url.set("$repoUrl/blob/main/license.txt")
                        distribution.set("repo")
                    }
                }

                issueManagement {
                    system.set("GitHub")
                    url.set("$repoUrl/issues")
                }

                developers {
                    developer {
                        id.set("DemonWav")
                        name.set("Kyle Wood")
                        email.set("demonwav@gmail.com")
                        url.set("https://github.com/DemonWav")
                    }
                }

                scm {
                    url.set(repoUrl)
                    connection.set("scm:git:$repoUrl.git")
                    developerConnection.set("scm:git:git@github.com:$repoPath.git")
                }
            }
        }

        repositories {
            val url = if (isSnapshot) {
                "https://artifactory.papermc.io/artifactory/snapshots/"
            } else {
                "https://artifactory.papermc.io/artifactory/releases/"
            }

            maven(url) {
                credentials(PasswordCredentials::class)
                name = "paper"
            }
        }
    }
}

tasks.register("printVersion") {
    doFirst {
        println(version)
    }
}
