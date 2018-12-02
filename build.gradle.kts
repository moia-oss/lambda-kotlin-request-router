import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import com.google.protobuf.gradle.*

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    java
    kotlin("jvm") version "1.3.10"
    idea
    id("com.github.johnrengelman.shadow") version "4.0.3"
    id("org.jmailen.kotlinter") version "1.20.1"
    id("com.google.protobuf") version "0.8.7"
}

group = "com.md"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    compile("com.amazonaws:aws-lambda-java-core:1.2.0")
    compile("com.amazonaws:aws-lambda-java-log4j2:1.1.0")
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.5")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.5")
    compile("com.google.guava:guava:23.0")
    compile("com.google.protobuf:protobuf-java:3.5.1")
    compile("com.google.protobuf:protobuf-java-util:3.5.1")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.1")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.12")
    testImplementation("io.mockk:mockk:1.8.13.kotlin13")
}

tasks.withType<ShadowJar> {
    baseName = project.name
    classifier = ""
    version = ""
    transform(Log4j2PluginsCacheFileTransformer::class.java)
    minimize()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Test> {
        useJUnitPlatform()
    }

    val deploy by creating(Exec::class) {

        dependsOn("test", "shadowJar")
        commandLine("serverless", "deploy")
    }
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.6.1"
    }
}