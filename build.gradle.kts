plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("org.openapi.generator") version "7.25.0"
}

group = "com.sber"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$projectDir/src/main/openapi/meeting-rooms.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    apiPackage.set("com.sber.meetingrooms.generated.api")
    modelPackage.set("com.sber.meetingrooms.generated.model")
    invokerPackage.set("com.sber.meetingrooms.generated")
    globalProperties.set(mapOf(
        "apis" to "",
        "models" to "",
        "supportingFiles" to "ApiUtil.java"
    ))
    configOptions.set(mapOf(
        "additionalNotNullAnnotations" to "true",
        "annotationLibrary" to "none",
        "dateLibrary" to "java8",
        "documentationProvider" to "none",
        "hideGenerationTimestamp" to "true",
        "interfaceOnly" to "true",
        "openApiNullable" to "false",
        "skipDefaultInterface" to "true",
        "useBeanValidation" to "true",
        "useSpringBoot3" to "true",
        "useTags" to "true"
    ))
}

sourceSets.main {
    java.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

openApiValidate {
    inputSpec.set("$projectDir/src/main/openapi/meeting-rooms.yaml")
}

tasks.processResources {
    from("src/main/openapi") {
        into("static")
        rename("meeting-rooms.yaml", "openapi.yaml")
    }
}

tasks.check {
    dependsOn(tasks.openApiValidate)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("resolveDependencies") {
    group = "build setup"
    description = "Downloads all resolvable dependency configurations for the Docker cache layer."
    doLast {
        configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
    }
}

tasks.bootJar {
    archiveFileName.set("meeting-rooms.jar")
}

tasks.jar {
    enabled = false
}
