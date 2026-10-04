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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Remote build cache (Gradle's native HttpBuildCache), off unless a URL is configured.
//
// A slow machine reuses task outputs another machine already produced — compiled classes,
// dexed code, merged resources — instead of rebuilding them. The intended setup: CI pushes,
// everyone else only reads. Configure per machine in ~/.gradle/gradle.properties (never here,
// the credentials are personal) or as environment variables:
//
//   gradleCacheUrl=https://<host>/private/gradle-cache/ # or GRADLE_CACHE_URL
//   gradleCacheUser=... / gradleCachePassword=...      # or GRADLE_CACHE_USER / _PASSWORD, optional
//   gradleCachePush=true                               # or GRADLE_CACHE_PUSH — CI only
//
// Any HTTP server that answers GET/PUT works. Ours is Reposilite deployed with Coolify (the official
// `gradle/build-cache-node` is deprecated and unavailable after 2026-12-31). Readers keep the local
// cache on and ask it first. The pusher turns it off: Gradle never copies a local hit to the remote,
// so CI, whose local cache setup-gradle restores between runs, would load every cacheable task from
// it and leave the remote empty. Server setup and verification: docs/BUILD-CACHE.md.
fun cacheSetting(property: String, env: String): String? =
    (providers.gradleProperty(property).orNull ?: providers.environmentVariable(env).orNull)
        ?.trim()?.takeIf { it.isNotEmpty() }

val remoteCacheUrl = cacheSetting("gradleCacheUrl", "GRADLE_CACHE_URL")
val pushToRemoteCache = remoteCacheUrl != null && cacheSetting("gradleCachePush", "GRADLE_CACHE_PUSH") == "true"

buildCache {
    local { isEnabled = !pushToRemoteCache }
    remoteCacheUrl?.let { cacheUrl ->
        remote<HttpBuildCache> {
            url = uri(cacheUrl)
            isAllowInsecureProtocol = cacheUrl.startsWith("http://")
            isPush = pushToRemoteCache
            val user = cacheSetting("gradleCacheUser", "GRADLE_CACHE_USER")
            val password = cacheSetting("gradleCachePassword", "GRADLE_CACHE_PASSWORD")
            if (user != null && password != null) {
                credentials {
                    username = user
                    this.password = password
                }
            }
        }
    }
}

rootProject.name = "Multi Band Radio Emulator"
include(":app")
 