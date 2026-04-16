plugins {
    `java-library`
    checkstyle
}

subprojects {
    apply(plugin = "java-library")

    group = "com.valesmp.slabby"
    version = providers.gradleProperty("slabby_version").get()

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.40")
        annotationProcessor("org.projectlombok:lombok:1.18.40")

        testCompileOnly("org.projectlombok:lombok:1.18.40")
        testAnnotationProcessor("org.projectlombok:lombok:1.18.40")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }
}