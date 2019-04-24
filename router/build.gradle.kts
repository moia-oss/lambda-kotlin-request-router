

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    compile("com.amazonaws:aws-lambda-java-core:1.2.0")
    compile("com.amazonaws:aws-lambda-java-events:2.2.5")
    
    compile("org.slf4j:slf4j-api:1.7.26")
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.8")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")
    compile("org.apache.httpcomponents:httpcore:4.4.11")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.0")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.12")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("io.mockk:mockk:1.8.13.kotlin13")
    testImplementation("org.slf4j:slf4j-simple:1.7.26")
}