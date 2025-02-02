plugins {
    id("com.google.protobuf") version "0.9.4"
}

repositories {
    mavenCentral()
}

val protoVersion = "4.29.3"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.slf4j:slf4j-api:2.0.16")
    api("com.google.protobuf:protobuf-java:$protoVersion")
    api("com.google.protobuf:protobuf-java-util:$protoVersion")
    implementation("com.google.guava:guava:33.4.0-jre")
    api(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    testImplementation("org.assertj:assertj-core:3.27.2")
    testImplementation("io.mockk:mockk:1.13.14")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")
}

protobuf {
    // Configure the protoc executable
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:$protoVersion"
    }
}