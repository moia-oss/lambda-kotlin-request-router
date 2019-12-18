import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.google.protobuf") version "0.8.7"
}

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))

    compile("org.slf4j:slf4j-api:1.7.26")
    compile("com.google.protobuf:protobuf-java:3.11.1")
    compile("com.google.protobuf:protobuf-java-util:3.11.1")
    compile("com.google.guava:guava:23.0")
    compile(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.4.0")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.12")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("io.mockk:mockk:1.8.13.kotlin13")
    testImplementation("org.slf4j:slf4j-simple:1.7.26")
    testImplementation("com.jayway.jsonpath:json-path:2.4.0")
}


protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.6.1"
    }
}