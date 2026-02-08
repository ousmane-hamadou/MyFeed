import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
}

group = "com.github.ousmane_hamadou.myfeed"
version = "1.0.0"
application {
    mainClass = "com.github.ousmane_hamadou.server.ApplicationKt"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin {
    val jdkVersion = libs.versions.jvm.tool.chain.get()

    jvmToolchain(jdkVersion.toInt())

    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget((jdkVersion)))
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.test.host)
}