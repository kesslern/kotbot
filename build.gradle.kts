import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
    application
}

group = "us.kesslern"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    implementation("io.ktor:ktor-network:1.1.4")
    implementation("io.github.microutils:kotlin-logging:1.6.24")
    implementation("ch.qos.logback:logback-classic:1.2.+")
    implementation("org.graalvm.sdk:graal-sdk:1.0.0-rc16")
    implementation("com.beust:klaxon:5.0.1")
    implementation(kotlin("stdlib-jdk8"))
}

repositories {
    mavenCentral()
    jcenter()
}

application {
    mainClassName = "us.kesslern.kotbot.MainKt"
    group = "us.kesslern"
    applicationName = "kotbot"
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}