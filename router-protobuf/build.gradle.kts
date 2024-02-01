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

    implementation("org.slf4j:slf4j-api:2.0.9")
    api("com.google.protobuf:protobuf-java:3.25.1")
    api("com.google.protobuf:protobuf-java-util:3.25.1")
    implementation("com.google.guava:guava:32.1.3-jre")
    api(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.27.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.slf4j:slf4j-simple:2.0.9")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")
}


protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
}