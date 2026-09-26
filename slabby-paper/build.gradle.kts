plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

//TODO: Compile error due to bukkit dependency
dependencies {
    compileOnly(libs.paper)

    implementation(libs.acf.paper)

    compileOnly(libs.vault) {
        isTransitive = false
    }

    implementation(libs.configurate.yaml)

    implementation(libs.hxgui)

    // hxgui needs hxcore now but expects slabby to bundle it
    implementation(libs.hxcore)

    compileOnly(libs.lands.api)

    implementation(libs.gson)

    implementation(project(":slabby-api"))
    implementation(project(":slabby-sqlite3"))
}

tasks.shadowJar {

    archiveFileName = "slabby-paper-${providers.gradleProperty("minecraft_version").get()}-${project.version}.jar"
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    enabled = false
}

tasks.compileJava {
    options.compilerArgs.add("-parameters")
    options.isFork = false
}

version = providers.gradleProperty("slabby_version").get()