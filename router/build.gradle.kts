

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("com.amazonaws:aws-lambda-java-core:1.2.1")
    api("com.amazonaws:aws-lambda-java-events:3.7.0")

    implementation("org.slf4j:slf4j-api:1.7.30")
    api("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")
    api("com.google.guava:guava:30.1.1-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("ch.qos.logback:logback-classic:1.2.6")
}
