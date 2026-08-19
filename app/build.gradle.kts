plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kover)
    application
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

application {
    mainClass.set("me.nathanfallet.asonar.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_21)
        localImageName.set("asonar")
    }
}

dependencies {
    implementation(projects.domain)
    implementation(projects.infrastructure)
    implementation(projects.presentation)

    implementation(libs.logback.core)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.netty)

    // Runtime JDBC driver for the default local file database (zero external server needed).
    implementation(libs.h2)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.h2)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("jvmTest") {
    dependsOn("test")
}
