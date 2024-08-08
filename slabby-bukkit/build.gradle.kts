plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.paper)

    compileOnly(libs.plugin.annotations)
    annotationProcessor(libs.plugin.annotations)

    implementation(libs.acf.paper)

    compileOnly(libs.vault)

    implementation(libs.configurate.yaml)

    implementation(libs.invui)

    compileOnly(libs.lands.api)

    implementation(libs.gson)

    implementation(project(":slabby-api"))
    implementation(project(":slabby-sqlite3"))
}

tasks.shadowJar {
    minimize()
}

tasks.jar {
    enabled = false
}

tasks.compileJava {
    options.compilerArgs.add("-parameters")
    options.isFork = true
}