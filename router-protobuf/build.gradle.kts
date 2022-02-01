import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.google.protobuf") version "0.8.18"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.slf4j:slf4j-api:1.7.35")
    api("com.google.protobuf:protobuf-java:3.19.1")
    api("com.google.protobuf:protobuf-java-util:3.19.1")
    implementation("com.google.guava:guava:31.0.1-jre")
    api(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("io.mockk:mockk:1.12.1")
    testImplementation("org.slf4j:slf4j-simple:1.7.35")
    testImplementation("com.jayway.jsonpath:json-path:2.7.0")
}


protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.19.4"
    }
}