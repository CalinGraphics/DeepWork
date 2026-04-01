pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "DeepWork"
include(
    ":app",
    ":core:common",
    ":core:ui",
    ":domain",
    ":data:local",
    ":data:remote",
    ":feature:timer",
    ":feature:tasks",
    ":feature:analytics",
    ":feature:pc-remote",
    ":feature:settings",
    ":desktop"
)
