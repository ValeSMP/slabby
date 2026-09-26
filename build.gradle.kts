plugins {
    `java-library`
    checkstyle
}

val lombok = libs.lombok

subprojects {
    apply(plugin = "java-library")

    group = "com.valesmp.slabby"
    version = providers.gradleProperty("slabby_version").get()

    dependencies {
        compileOnly(lombok)
        annotationProcessor(lombok)

        testCompileOnly(lombok)
        testAnnotationProcessor(lombok)
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }
}