plugins {
    java
    `java-test-fixtures`
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.4.0"
    id("com.google.protobuf") version "0.9.6"
    id("com.google.cloud.tools.jib") version "3.4.5"
}

jib {
    from {
        image = "docker://eclipse-temurin:25-jre-alpine"
    }
    to {
        image = "stablepay-backend"
        tags = setOf("latest", version.toString())
    }
    container {
        mainClass = "com.stablepay.Application"
        jvmFlags = listOf(
            "-XX:+UseG1GC",
            "-XX:MaxRAMPercentage=75.0",
            "-Djava.security.egd=file:/dev/./urandom"
        )
        ports = listOf("8080")
        environment = mapOf(
            "SPRING_PROFILES_ACTIVE" to "docker"
        )
        creationTime.set("USE_CURRENT_TIMESTAMP")
        user = "1000:1000"
    }
}

group = "com.stablepay"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

spotless {
    java {
        removeUnusedImports()
        importOrder("\\#", "java|javax", "jakarta", "org", "com", "")
        trimTrailingWhitespace()
        endWithNewline()
        targetExclude("build/generated/**")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.9"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.80.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

sourceSets {
    main {
        proto {
            srcDir("${rootProject.projectDir}/../mpc-sidecar/proto")
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.addAll(listOf(
        "-Amapstruct.defaultComponentModel=spring",
        "-Amapstruct.defaultInjectionStrategy=constructor",
        "-Amapstruct.unmappedTargetPolicy=ERROR"
    ))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val gitDir = file("${rootProject.projectDir}/../.git")

val installGitHooks by tasks.registering(Copy::class) {
    description = "Installs git hooks from backend/.githooks into .git/hooks"
    group = "setup"
    enabled = gitDir.isDirectory
    from("${projectDir}/.githooks")
    into("${gitDir}/hooks")
    filePermissions {
        unix("rwxr-xr-x")
    }
}

tasks.named("compileJava") {
    dependsOn(installGitHooks)
}

val integrationTest by sourceSets.creating {
    java.srcDir("src/integration-test/java")
    resources.srcDir("src/integration-test/resources")
    resources.srcDir("src/test/resources")
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())
configurations[integrationTest.compileOnlyConfigurationName].extendsFrom(configurations.testCompileOnly.get())
configurations[integrationTest.annotationProcessorConfigurationName].extendsFrom(configurations.testAnnotationProcessor.get())

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    useJUnitPlatform()
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.44")
    annotationProcessor("org.projectlombok:lombok:1.18.44")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    // Flyway
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core:12.3.0")
    implementation("org.flywaydb:flyway-database-postgresql:12.3.0")

    // Temporal
    implementation("io.temporal:temporal-spring-boot-starter:1.34.0")

    // gRPC
    implementation("io.grpc:grpc-netty-shaded:1.80.0")
    implementation("io.grpc:grpc-protobuf:1.80.0")
    implementation("io.grpc:grpc-stub:1.80.0")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // UUID v7
    implementation("com.github.f4b6a3:uuid-creator:6.1.1")

    // Solana SDK
    implementation("org.sol4k:sol4k:0.7.0")

    // Test
    testCompileOnly("org.projectlombok:lombok:1.18.44")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.44")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("io.temporal:temporal-testing:1.34.0")

    // Integration test dependencies
    "integrationTestImplementation"(testFixtures(project))
    "integrationTestImplementation"("org.wiremock:wiremock-standalone:3.13.2")
}
