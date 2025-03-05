
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
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
    kotlin("jvm") version "2.1.10"
    idea
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jmailen.kotlinter") version "5.0.1"
    id("com.google.protobuf") version "0.9.4"
}


group = "com.github.mduesterhoeft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

val proto = "4.29.3"
dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("io.moia.lambda-kotlin-request-router:router-protobuf:1.1.0")

    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-log4j2:1.6.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    implementation("com.google.guava:guava:23.0")
    implementation("com.google.protobuf:protobuf-java:$proto")
    implementation("com.google.protobuf:protobuf-java-util:$proto")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.12.0")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
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

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:$proto"
    }
}
