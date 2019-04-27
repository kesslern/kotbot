import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
}
group = "us.kesslern"
version = "1.0-SNAPSHOT"
val ktor_version = "1.1.4"

dependencies {
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    compile("io.ktor:ktor-network:$ktor_version")
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