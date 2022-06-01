

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("com.amazonaws:aws-lambda-java-core:1.2.1")
    api("com.amazonaws:aws-lambda-java-events:3.11.0")

    implementation("org.slf4j:slf4j-api:1.7.36")
    api("com.fasterxml.jackson.core:jackson-databind:2.13.2.2")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    api("com.google.guava:guava:31.1-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("io.mockk:mockk:1.12.4")
    testImplementation("ch.qos.logback:logback-classic:1.2.11")
}
