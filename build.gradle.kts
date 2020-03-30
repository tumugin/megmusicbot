import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("io.github.cdimascio:java-dotenv:5.1.3")
    }
}

plugins {
    id("java")
    id("application")
    id("org.jetbrains.kotlin.jvm") version Deps.kotlinVersion
    id("com.github.ben-manes.versions") version "0.28.0"
    id("org.flywaydb.flyway") version Deps.flywayVersion
    id("jacoco")
}

application {
    mainClassName = "com.myskng.megmusicbot.main.MegmusicMain"
}

version = "1.0-FAIRY_STARS"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.wrapper {
    gradleVersion = "6.3"
}

repositories {
    jcenter()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://dl.bintray.com/ijabz/maven")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

tasks.test {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Deps.kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Deps.kotlinVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Deps.kotlinCoroutine}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Deps.kotlinCoroutine}")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.2.RELEASE")
    // Koin
    implementation("org.koin:koin-core:${Deps.koinVersion}")
    implementation("org.koin:koin-core-ext:${Deps.koinVersion}")
    testImplementation("org.koin:koin-test:${Deps.koinVersion}")
    // Other libs
    implementation("com.discord4j:discord4j-core:3.1.0-SNAPSHOT")
    implementation("com.squareup.okhttp3", "okhttp", "4.4.1")
    implementation("com.squareup.okio", "okio", "2.4.3")
    implementation("info.picocli:picocli:4.2.0")
    // DB
    implementation("org.jetbrains.exposed:exposed:0.17.7")
    implementation("com.github.KenjiOhtsuka:harmonica:${Deps.harmonicaVersion}")
    implementation("org.xerial:sqlite-jdbc:${Deps.sqliteVersion}")
    implementation("org.flywaydb:flyway-core:${Deps.flywayVersion}")
    // JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
    // Music Tag
    implementation("net.jthink:jaudiotagger:2.2.6-PATHRIK")
    // Opus
    implementation("net.java.dev.jna:jna-platform:5.5.0")
    implementation("club.minnced:opus-java:1.0.4")
    // dotenv
    implementation("io.github.cdimascio:java-dotenv:5.1.3")
    // Test libs
    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.mockito:mockito-core:3.3.3")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.nanohttpd:nanohttpd:2.3.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Deps.junitJupiterVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Deps.junitJupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Deps.junitJupiterVersion}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${Deps.junitPlatformVersion}")
}

tasks.withType(KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.8"
}

flyway {
    val dotEnvSetting = dotenv { ignoreIfMissing = true }
    baselineVersion = "0"
    url = dotEnvSetting["DB_CONNECTION"] ?: "jdbc:sqlite:megmusicbot.db"
    user = dotEnvSetting["DB_USER"] ?: ""
    password = dotEnvSetting["DB_PASSWORD"] ?: ""
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = true
    }
}
