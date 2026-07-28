plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "com.wim.markdown"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.wumiaojia.MarkdownProject"
                artifactId = "markdown"
                version = System.getenv("VERSION") ?: "0.1.0"

                pom {
                    name.set("Markdown")
                    description.set("A Jetpack Compose Markdown editor library for Android.")
                    url.set("https://github.com/wumiaojia/MarkdownProject")

                    developers {
                        developer {
                            id.set("wumiaojia")
                            name.set("wim")
                            email.set("wimvogt@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/wumiaojia/MarkdownProject.git")
                        developerConnection.set("scm:git:ssh://github.com/wumiaojia/MarkdownProject.git")
                        url.set("https://github.com/wumiaojia/MarkdownProject")
                    }
                }
            }
        }
    }
}
