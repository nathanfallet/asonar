plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kover)
}

kotlin {
    jvmToolchain(21)
    jvm {
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }

    applyDefaultHierarchyTemplate()
    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.uuid.ExperimentalUuidApi")
                optIn("kotlin.time.ExperimentalTime")
            }
        }
        val commonMain by getting {
            dependencies {
                api(projects.domain)

                api(libs.koin.ktor)
                api(libs.ktor.server.core)
                api(libs.ktor.server.content.negotiation)
                api(libs.ktor.serialization.kotlinx.json)
                api(libs.ktor.server.status.pages)
                api(libs.ktor.server.cors)
                api(libs.ktor.server.call.logging)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.bundles.ktor.server.tests)
            }
        }
    }
}
