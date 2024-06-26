import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.google.protobuf") version "0.8.19"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.slf4j:slf4j-api:2.0.13")
    api("com.google.protobuf:protobuf-java:3.25.3")
    api("com.google.protobuf:protobuf-java-util:3.25.3")
    implementation("com.google.guava:guava:33.2.1-jre")
    api(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.slf4j:slf4j-simple:2.0.13")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")
}


protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
}