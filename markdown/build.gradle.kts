plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "com.wim.markdown"

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.wumiaojia"
                artifactId = "markdown"
                version = System.getenv("VERSION") ?: "0.1.4"

                pom {
                    name.set("Markdown")
                    description.set("A Jetpack Compose Markdown editor library for Android.")
                    url.set("https://github.com/wumiaojia/markdown")

                    developers {
                        developer {
                            id.set("wumiaojia")
                            name.set("wim")
                            email.set("wimvogt@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/wumiaojia/markdown.git")
                        developerConnection.set("scm:git:ssh://github.com/wumiaojia/markdown.git")
                        url.set("https://github.com/wumiaojia/markdown")
                    }
                }
            }
        }
    }
}
