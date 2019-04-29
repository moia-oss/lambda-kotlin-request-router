
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    java
    kotlin("jvm") version "1.3.21"
    `maven-publish`
    jacoco
    id("org.jmailen.kotlinter") version "1.22.0"
}

group = "com.github.moia-dev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
}

subprojects {
    repositories {
        mavenCentral()
    }
    
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jmailen.kotlinter")

    tasks {
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }

        withType<Test> {
            useJUnitPlatform()
            testLogging.showStandardStreams = true
        }

    }
    
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}

val jacocoRootReport by tasks.creating(JacocoReport::class) {
    description = "Generates an aggregate report from all subprojects"
    group = "Coverage reports"
    sourceSets(*subprojects.map { it.sourceSets["main"] }.toTypedArray())
    executionData(fileTree(project.rootDir.absolutePath).include("**/build/jacoco/*.exec"))

    reports {
        html.isEnabled = true
        xml.isEnabled = true
        xml.setDestination(File(project.buildDir, "reports/jacoco/report.xml"))
    }
}