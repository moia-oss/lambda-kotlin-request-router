

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    api("com.amazonaws:aws-lambda-java-core:1.4.0")
    api("com.amazonaws:aws-lambda-java-events:3.16.1")

    implementation("org.slf4j:slf4j-api:2.0.17")
    api("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.1")
    api("com.google.guava:guava:33.5.0-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.0.1")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("ch.qos.logback:logback-classic:1.5.20")
}
