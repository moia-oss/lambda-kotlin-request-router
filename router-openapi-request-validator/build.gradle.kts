repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    api("com.atlassian.oai:swagger-request-validator-core:2.44.1")
    api(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.12.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
}