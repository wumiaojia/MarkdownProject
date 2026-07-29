plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

val androidAppPluginId = libs.plugins.android.application.get().pluginId
val androidLibPluginId = libs.plugins.android.library.get().pluginId

subprojects {
    plugins.withId(androidAppPluginId) {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
            compileSdk = libs.versions.sdkCompile.get().toInt()
            defaultConfig {
                minSdk = libs.versions.sdkMin.get().toInt()
                targetSdk = libs.versions.sdkTarget.get().toInt()
            }
            buildFeatures {
                compose = true
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }

    plugins.withId(androidLibPluginId) {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            compileSdk = libs.versions.sdkCompile.get().toInt()
            defaultConfig {
                minSdk = libs.versions.sdkMin.get().toInt()
                aarMetadata {
                    minCompileSdk = libs.versions.sdkCompile.get().toInt()
                }
            }
            buildFeatures {
                compose = true
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.javaVersion.get()))
            }
        }
    }
}
