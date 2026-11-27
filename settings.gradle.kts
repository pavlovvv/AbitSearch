pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "AbitSearch"
include(":app")
// Сервер (Node.js) підключено як Gradle-проєкт лише для запуску з Android Studio.
include(":server")
