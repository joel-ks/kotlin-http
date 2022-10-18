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
}

application {
    mainClass.set("jks.http.client.AppKt")
}

