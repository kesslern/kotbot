import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
}
group = "us.kesslern"
version = "1.0-SNAPSHOT"

dependencies {
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    compile("io.ktor:ktor-network:1.1.4")
    compile("io.github.microutils:kotlin-logging:1.6.24")
    compile("ch.qos.logback:logback-classic:1.2.+")
    implementation(kotlin("stdlib-jdk8"))
}

repositories {
    mavenCentral()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}