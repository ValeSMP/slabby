plugins {
    alias(libs.plugins.shadow)
}

//TODO: Compile error due to bukkit dependency
dependencies {
    compileOnly(libs.paper)

    compileOnly(libs.plugin.annotations) {
        isTransitive = false
    }
    annotationProcessor(libs.plugin.annotations) {
        isTransitive = false
    }

    implementation(libs.acf.paper)

    compileOnly(libs.vault) {
        isTransitive = false
    }

    implementation(libs.configurate.yaml)

    implementation(libs.invui)

    compileOnly(libs.lands.api)

    implementation(libs.gson)

    implementation(project(":slabby-api"))
    implementation(project(":slabby-sqlite3"))
}

tasks.shadowJar {

    archiveFileName = "slabby-paper-${providers.gradleProperty("minecraft_version").get()}-${project.version}.jar"
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    enabled = false
}

tasks.compileJava {
    options.compilerArgs.add("-parameters")
    options.isFork = true
}