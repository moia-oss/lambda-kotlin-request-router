repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    api("com.atlassian.oai:swagger-request-validator-core:2.27.1")
    api(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("io.mockk:mockk:1.12.5")
    testImplementation("org.slf4j:slf4j-simple:1.7.36")
}