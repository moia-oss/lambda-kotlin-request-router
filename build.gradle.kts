
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.kt3k.gradle.plugin.CoverallsPluginExtension

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    java
    kotlin("jvm") version "1.6.10"
    `maven-publish`
    jacoco
    id("com.github.kt3k.coveralls") version "2.12.0"
    id("org.jmailen.kotlinter") version "3.10.0"
}

group = "com.github.moia-dev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
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
            kotlinOptions.jvmTarget = "11"
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
                pom {
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }
    }
}

configure<CoverallsPluginExtension> {
    sourceDirs = subprojects.flatMap { it.sourceSets["main"].allSource.srcDirs }.filter { it.exists() }.map { it.path }
    jacocoReportPath = "$buildDir/reports/jacoco/report.xml"
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