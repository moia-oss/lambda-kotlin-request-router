repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    api("com.atlassian.oai:swagger-request-validator-core:2.46.0")
    api(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.2")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
}