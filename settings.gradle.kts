buildCache {
    local {
        // 所有 worktree 共享同一个 build cache 目录
        // 路径指向 HuaJuan 根目录的上一级，避免被任何 worktree 覆盖
        directory = File(rootDir.parent, ".gradle-build-cache")
        removeUnusedEntriesAfterDays = 14
    }
}
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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "HuaJuan"
include(":app")