plugins {
    `java-library`
    checkstyle
}

subprojects {
    apply(plugin = "java-library")

    group = "com.valesmp.slabby"
    version = providers.gradleProperty("slabby_version").get()

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.34")
        annotationProcessor("org.projectlombok:lombok:1.18.34")

        testCompileOnly("org.projectlombok:lombok:1.18.34")
        testAnnotationProcessor("org.projectlombok:1.18.34")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
}