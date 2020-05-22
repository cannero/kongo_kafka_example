plugins {
    application
}

dependencies {
    implementation("com.google.guava:guava:28.2-jre")
    implementation("org.apache.kafka:kafka-clients:2.5.0")
    //testImplementation("junit:junit:4.12")
}

application {
    mainClassName = "com.instaclustr.kongokafka1.KafkaRun"
}
