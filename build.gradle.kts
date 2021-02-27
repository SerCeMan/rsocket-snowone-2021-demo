plugins {
    java
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val rsocket = "1.0.3"
//    rsocket = '1.1.0'
    implementation("io.rsocket:rsocket-core:$rsocket")
    implementation("io.rsocket:rsocket-transport-netty:$rsocket")
    implementation("io.projectreactor:reactor-test:3.4.2")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.30")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
    useJUnitPlatform()
}
