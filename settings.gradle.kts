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
        // Dependências do PDFium usadas pelo Readium (Apache 2.0).
        maven(url = "https://jitpack.io") {
            content { includeGroup("com.github.marain87") }
        }
    }
}

rootProject.name = "Marginalia"
include(":app")
