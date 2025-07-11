rootProject.name = "slabby"

include("slabby-api", "slabby-sqlite3", "slabby-paper")

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/groups/public/")
        maven("https://repo.xenondevs.xyz/releases")
        maven("https://repo.aikar.co/content/groups/aikar/")
        maven("https://jitpack.io")
    }
}