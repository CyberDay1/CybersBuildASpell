plugins {
    id("java")
    id("maven-publish")
    id("net.neoforged.moddev") version "2.0.141"
}

version = findProperty("mod_version")?.toString() ?: "2.0.0"
group = findProperty("mod_group_id")?.toString() ?: "buildaspell"

base {
    archivesName.set(findProperty("mod_id")?.toString() ?: "buildaspell")
}

// Tag the built jar with the target MC version so the three parallel branches
// (1.21.1 / 26.1.X / 26.2) produce distinctly-named, non-colliding artifacts.
tasks.named<Jar>("jar") {
    archiveClassifier.set("mc" + (findProperty("minecraft_version")?.toString() ?: ""))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven {
        name = "CaffeineMC"
        url = uri("https://maven.caffeinemc.net/releases")
    }
    maven {
        name = "Klikli"
        url = uri("https://dl.cloudsmith.io/public/klikli-dev/mods/maven/")
    }
}

neoForge {
    version = findProperty("neo_version")?.toString() ?: "21.11.38-beta"

    runs {
        create("client") {
            client()
        }

        create("server") {
            server()
            programArgument("--nogui")
        }

        create("datagen") {
            serverData()
            programArguments.addAll(
                "--mod", "buildaspell", "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        create(findProperty("mod_id")?.toString() ?: "buildaspell") {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets.main.get().resources.srcDir("src/generated/resources")

dependencies {
    implementation("org.jetbrains:annotations:24.0.1")
    // NeoPortals optional dependency — provides see-through portal rendering when present
    compileOnly(files("libs/neoportals-1.0.0.jar"))
    // Modonomicon guidebook
    implementation("com.klikli_dev:modonomicon-26.1-neoforge:${findProperty("modonomicon_version")}")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// Publish to mavenLocal so sibling mods (e.g. NeoOrigins) can `compileOnly` against the
// Build A Spell public API (buildaspell.api.BuildASpellAPI). The three parallel
// branches (1.21.1 / 26.1.X / 26.2) all share mod_version 2.0.0, so the published version is
// suffixed with "-mc<minecraft_version>" to give each branch a DISTINCT coordinate that can
// coexist in mavenLocal (a plain shared "2.0.0" would overwrite itself across branches):
//     buildaspell:buildaspell:2.0.0-mc26.2
//     buildaspell:buildaspell:2.0.0-mc26.1.1
//     buildaspell:buildaspell:2.0.0-mc1.21.1
// The on-disk jar already carries the same "mc<version>" classifier; the publication strips
// that classifier and folds the marker into the version instead, so the artifact filename is a
// clean <artifactId>-<version>.jar. Run `./gradlew publishToMavenLocal` to install it.
publishing {
    publications {
        create<MavenPublication>("mod") {
            groupId = project.group.toString()
            artifactId = base.archivesName.get()
            version = project.version.toString() + "-mc" +
                    (findProperty("minecraft_version")?.toString() ?: "")
            artifact(tasks.named("jar")) {
                classifier = null
            }
        }
    }
    repositories {
        mavenLocal()
    }
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "mod_id" to findProperty("mod_id").toString(),
        "mod_name" to findProperty("mod_name").toString(),
        "mod_version" to findProperty("mod_version").toString(),
        "mod_authors" to findProperty("mod_authors").toString(),
        "mod_description" to findProperty("mod_description").toString(),
        "minecraft_version_range" to findProperty("minecraft_version_range").toString(),
        "neo_version_range" to findProperty("neo_version_range").toString(),
        "loader_version_range" to findProperty("loader_version_range").toString()
    )

    inputs.properties(replaceProperties)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replaceProperties)
    }
}
