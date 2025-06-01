repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    api("com.atlassian.oai:swagger-request-validator-core:2.44.8")
    api(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.13.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
}