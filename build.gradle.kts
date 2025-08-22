plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)



}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()

}

dependencies {
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.bcrypt)
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.1.4")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.1.4")
    implementation("io.ktor:ktor-client-cio:2.2.0")

    implementation("io.ktor:ktor-server-core:3.1.3")
    implementation("io.ktor:ktor-server-netty:3.1.3")
    implementation("io.ktor:ktor-server-html-builder:3.1.3")



}