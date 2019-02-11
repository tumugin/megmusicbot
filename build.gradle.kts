import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
}

plugins {
    id("java")
    id("jarmonica") version Deps.harmonicaVersion apply false
    id("org.jetbrains.kotlin.jvm") version Deps.kotlinVersion
    id("org.jetbrains.kotlin.kapt") version Deps.kotlinVersion
    id("com.github.ben-manes.versions") version "0.20.0"
}

// workaround
// will cause "Could not find any convention object of type JavaPluginConvention"
apply(mapOf("plugin" to "jarmonica"))

version = "1.0-FAIRY_STARS"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

kapt {
    useBuildCache = true
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Deps.kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Deps.kotlinVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1")
    // Koin
    implementation("org.koin:koin-core:${Deps.koinVersion}")
    implementation("org.koin:koin-core-ext:${Deps.koinVersion}")
    testImplementation("org.koin:koin-test:${Deps.koinVersion}")
    implementation("org.koin:koin-java:${Deps.koinVersion}")
    // Other libs
    implementation("com.discord4j:Discord4J:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:3.12.1")
    implementation("com.squareup.okio:okio:2.2.1")
    implementation("info.picocli:picocli:3.9.2")
    // DB
    // don"t use it until bug fixes
    implementation("org.jetbrains.exposed:exposed:0.11.3-SNAPSHOT")
    implementation("com.github.KenjiOhtsuka:harmonica:${Deps.harmonicaVersion}")
    implementation("org.xerial:sqlite-jdbc:${Deps.sqliteVersion}")
    // JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+")
    // Music Tag
    implementation("net.jthink:jaudiotagger:2.2.5")
    // Test libs
    testImplementation("org.mockito:mockito-core:2.23.4")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")
    testImplementation("org.nanohttpd:nanohttpd:2.3.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Deps.junitJupiterVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Deps.junitJupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Deps.junitJupiterVersion}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${Deps.junitPlatformVersion}")
}

tasks.withType(KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.8"
}