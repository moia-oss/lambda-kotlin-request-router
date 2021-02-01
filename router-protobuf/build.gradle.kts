import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.google.protobuf") version "0.8.14"
}

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))

    compile("org.slf4j:slf4j-api:1.7.30")
    compile("com.google.protobuf:protobuf-java:3.14.0")
    compile("com.google.protobuf:protobuf-java-util:3.14.0")
    compile("com.google.guava:guava:30.1-jre")
    compile(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23")
    testImplementation("org.assertj:assertj-core:3.19.0")
    testImplementation("io.mockk:mockk:1.10.5")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")
    testImplementation("com.jayway.jsonpath:json-path:2.5.0")
}


protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.14.0"
    }
}