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
        // Xposed / LSPosed API (compileOnly, provided by the framework at runtime).
        maven("https://api.xposed.info/")
    }
}

rootProject.name = "Andronitor"
include(":app")
