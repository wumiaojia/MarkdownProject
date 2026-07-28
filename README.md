# MarkdownProject

[![](https://jitpack.io/v/wumiaojia/MarkdownProject.svg)](https://jitpack.io/#wumiaojia/MarkdownProject)

An Android Markdown editor library built with Kotlin and Jetpack Compose.

## Installation

Add JitPack to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") {
            content {
                includeGroup("com.github.wumiaojia.MarkdownProject")
            }
        }
    }
}
```

Add the `markdown` module dependency:

```kotlin
dependencies {
    implementation("com.github.wumiaojia.MarkdownProject:markdown:0.1.0")
}
```

## Requirements

- Android minSdk 29
- Java 17
- Jetpack Compose
