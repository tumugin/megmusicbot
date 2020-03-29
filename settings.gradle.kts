rootProject.name = "com.myskng.megmusicbot"

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

includeBuild("./Discord4J") {
    dependencySubstitution {
        substitute(module("com.discord4j:discord4j-core")).with(project(":core"))
        substitute(module("com.discord4j:discord4j-common")).with(project(":common"))
        substitute(module("com.discord4j:discord4j-gateway")).with(project(":gateway"))
        substitute(module("com.discord4j:discord4j-rest")).with(project(":rest"))
        substitute(module("com.discord4j:discord4j-voice")).with(project(":voice"))
    }
}
includeBuild("./discord-json")
includeBuild("./Stores"){
    dependencySubstitution {
        substitute(module("com.discord4j:stores-jdk")).with(project(":jdk"))
    }
}
