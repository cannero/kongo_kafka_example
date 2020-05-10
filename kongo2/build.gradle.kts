plugins {
    application
}

dependencies {
    implementation("com.google.guava:guava:28.2-jre")

    testImplementation("junit:junit:4.12")
}

application {
    mainClassName = "com.instaclustr.kongo2.Simulate"
}
