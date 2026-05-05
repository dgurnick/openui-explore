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

rootProject.name = "openui-explore"
include(":shared", ":androidApp")
project(":shared").projectDir = file("app/shared")
project(":androidApp").projectDir = file("app/androidApp")
