

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    api("com.amazonaws:aws-lambda-java-core:1.3.0")
    api("com.amazonaws:aws-lambda-java-events:3.15.0")

    implementation("org.slf4j:slf4j-api:2.0.17")
    api("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
    api("com.google.guava:guava:33.4.8-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.13.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.0")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
}
