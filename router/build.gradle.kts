

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("com.amazonaws:aws-lambda-java-core:1.2.3")
    api("com.amazonaws:aws-lambda-java-events:3.11.3")

    implementation("org.slf4j:slf4j-api:2.0.9")
    api("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
    api("com.google.guava:guava:32.1.3-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("ch.qos.logback:logback-classic:1.4.11")
}
