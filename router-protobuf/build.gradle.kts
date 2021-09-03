import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.google.protobuf") version "0.8.17"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.slf4j:slf4j-api:1.7.30")
    api("com.google.protobuf:protobuf-java:3.17.3")
    api("com.google.protobuf:protobuf-java-util:3.17.3")
    implementation("com.google.guava:guava:30.1.1-jre")
    api(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.24")
    testImplementation("org.assertj:assertj-core:3.20.2")
    testImplementation("io.mockk:mockk:1.10.6")
    testImplementation("org.slf4j:slf4j-simple:1.7.32")
    testImplementation("com.jayway.jsonpath:json-path:2.6.0")
}


protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.17.3"
    }
}