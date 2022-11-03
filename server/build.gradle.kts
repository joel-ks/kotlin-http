plugins {
    kotlin("jvm") version "1.7.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation(project(":protocol"))
    implementation(project(":utilities"))

    implementation("net.sf.jopt-simple:jopt-simple:6.0-alpha-3")
}

application {
    mainClass.set("jks.http.server.app.AppKt")
}
