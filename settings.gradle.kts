pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ByeBox"

// Модуль :tgwsproxy — отдельный репозиторий (каталог tgwsproxy/ в .gitignore),
// поэтому в свежем клоне его нет. Проверяем явно, чтобы вместо непонятной
// ошибки конфигурации было понятное сообщение с инструкцией.
val tgwsproxyDir = file("tgwsproxy/app")
require(tgwsproxyDir.isDirectory) {
    "Модуль tgwsproxy не найден (${tgwsproxyDir.path}).\n" +
        "Это отдельный репозиторий. Клонируйте его в каталог tgwsproxy/:\n" +
        "  git clone https://github.com/amurcanov/tg-ws-proxy-android.git tgwsproxy\n" +
        "Подробности: docs/release_process.md (раздел «Модуль tgwsproxy»)."
}

include(":app", ":tgwsproxy")
project(":tgwsproxy").projectDir = tgwsproxyDir
