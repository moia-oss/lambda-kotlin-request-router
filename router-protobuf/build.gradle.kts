import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.google.protobuf") version "0.8.19"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.slf4j:slf4j-api:2.0.12")
    api("com.google.protobuf:protobuf-java:3.25.3")
    api("com.google.protobuf:protobuf-java-util:4.26.1")
    implementation("com.google.guava:guava:33.1.0-jre")
    api(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.0")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")
}


protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
}