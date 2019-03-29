import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import java.net.URI
import java.util.concurrent.TimeUnit.SECONDS
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc


buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    java
    kotlin("jvm") version "1.3.21"
    idea
    id("com.github.johnrengelman.shadow") version "4.0.3"
    id("org.jmailen.kotlinter") version "1.22.0"
    id("com.google.protobuf") version "0.8.7"
}


group = "com.github.mduesterhoeft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

val proto = "3.6.1"
dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("com.github.moia-dev.lambda-kotlin-request-router:router-protobuf:0.3.0")

    implementation("com.amazonaws:aws-lambda-java-core:1.2.0")
    implementation("com.amazonaws:aws-lambda-java-log4j2:1.1.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.5")
    implementation("com.google.guava:guava:23.0")
    implementation("com.google.protobuf:protobuf-java:$proto")
    implementation("com.google.protobuf:protobuf-java-util:$proto")

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
        baseName = project.name
        classifier = ""
        version = ""
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

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:$proto"
    }
}