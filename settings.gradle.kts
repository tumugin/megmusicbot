rootProject.name = "com.myskng.megmusicbot"

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        mavenCentral()
        maven("https://jitpack.io")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "jarmonica" -> {
                    useModule("com.github.KenjiOhtsuka:harmonica:${requested.version}")
                }
            }
        }
    }
}