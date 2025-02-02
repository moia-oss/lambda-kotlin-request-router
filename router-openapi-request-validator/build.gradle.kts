repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    api("com.atlassian.oai:swagger-request-validator-core:2.44.1")
    api(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.2")
    testImplementation("io.mockk:mockk:1.13.14")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
}