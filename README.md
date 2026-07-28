# Markdown

[![](https://jitpack.io/v/wumiaojia/markdown.svg)](https://jitpack.io/#wumiaojia/markdown)

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
                includeGroup("com.github.wumiaojia")
            }
        }
    }
}
```

Add the `markdown` module dependency:

```kotlin
dependencies {
    implementation("com.github.wumiaojia:markdown:0.1.2")
}
```

## Requirements

- Android minSdk 29
- Java 17
- Jetpack Compose
