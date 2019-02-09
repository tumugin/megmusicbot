import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val harmonicaVersion by extra("1.1.12")
val kotlinVersion by extra("1.3.20")
val koinVersion by extra("1.0.2")
val junitPlatformVersion by extra("1.3.2")
val junitJupiterVersion by extra("5.3.2")
val sqliteVersion by extra("3.25.2")

buildscript {
}

plugins {
    id("java")
    id("jarmonica") version "1.1.12" apply false
    id("org.jetbrains.kotlin.jvm") version "1.3.20"
    id("org.jetbrains.kotlin.kapt") version "1.3.20"
    id("com.github.ben-manes.versions") version "0.20.0"
}

// workaround
// will cause "Could not find any convention object of type JavaPluginConvention"
apply(mapOf("plugin" to "jarmonica"))

version = "1.0-FAIRY_STARS"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(Wrapper::class) {
    gradleVersion = "5.2"
}

repositories {
    jcenter()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://dl.bintray.com/ijabz/maven")
}

tasks.withType(Test::class) {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1")
    // Koin
    implementation("org.koin:koin-core:$koinVersion")
    implementation("org.koin:koin-core-ext:$koinVersion")
    testImplementation("org.koin:koin-test:$koinVersion")
    implementation("org.koin:koin-java:$koinVersion")
    // Other libs
    implementation("com.discord4j:Discord4J:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:3.12.1")
    implementation("com.squareup.okio:okio:2.2.1")
    implementation("info.picocli:picocli:3.9.2")
    // DB
    // don"t use it until bug fixes
    implementation("org.jetbrains.exposed:exposed:0.11.3-SNAPSHOT")
    implementation("com.github.KenjiOhtsuka:harmonica:$harmonicaVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    // JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+")
    // Music Tag
    implementation("net.jthink:jaudiotagger:2.2.5")
    // Test libs
    testImplementation("org.mockito:mockito-core:2.23.4")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")
    testImplementation("org.nanohttpd:nanohttpd:2.3.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
}

tasks.withType(KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.8"
}