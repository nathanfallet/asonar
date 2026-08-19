plugins {
    alias(libs.plugins.jvm) apply false
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.kover)
}

allprojects {
    group = "me.nathanfallet.asonar"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

dependencies {
    kover(projects.api)
    kover(projects.client)
    kover(projects.domain)
    kover(projects.infrastructure)
    kover(projects.presentation)
    kover(projects.app)
}
