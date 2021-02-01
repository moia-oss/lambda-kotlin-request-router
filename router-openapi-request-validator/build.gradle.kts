repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))

    compile("com.atlassian.oai:swagger-request-validator-core:2.11.2")
    compile(project(":router"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("org.assertj:assertj-core:3.18.1")
    testImplementation("io.mockk:mockk:1.10.5")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")
}