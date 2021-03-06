

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    compile("com.amazonaws:aws-lambda-java-core:1.2.1")
    compile("com.amazonaws:aws-lambda-java-events:3.7.0")
    
    compile("org.slf4j:slf4j-api:1.7.30")
    compile("com.fasterxml.jackson.core:jackson-databind:2.12.1")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")
    compile("com.google.guava:guava:30.1-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23.1")
    testImplementation("org.assertj:assertj-core:3.19.0")
    testImplementation("io.mockk:mockk:1.10.6")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
}