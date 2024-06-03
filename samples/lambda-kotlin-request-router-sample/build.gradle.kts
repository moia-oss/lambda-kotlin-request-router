
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.util.concurrent.TimeUnit.SECONDS


buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    java
    kotlin("jvm") version "1.3.50"
    idea
    id("com.github.johnrengelman.shadow") version "5.1.0"
    id("org.jmailen.kotlinter") version "1.22.0"
}


group = "com.github.mduesterhoeft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("io.moia.lambda-kotlin-request-router:router:0.9.7")

    implementation("com.amazonaws:aws-lambda-java-core:1.2.0")
    implementation("com.amazonaws:aws-lambda-java-log4j2:1.1.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.0")
    implementation("com.google.guava:guava:23.0")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.slf4j:log4j-over-slf4j:1.7.26")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<ShadowJar> {
        archiveBaseName.set(project.name)
        archiveClassifier.set("")
        archiveVersion.set("")
        transform(Log4j2PluginsCacheFileTransformer::class.java)
    }

    val deploy by creating(Exec::class) {
        dependsOn("test", "shadowJar")
        commandLine("serverless", "deploy")
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, SECONDS)
}