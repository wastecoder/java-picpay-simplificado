plugins {
	java
	id("org.springframework.boot") version "3.2.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.wastecoder.picpay"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_21

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

extra["springCloudVersion"] = "2023.0.0"

dependencies {

	// ========================
	// IMPLEMENTATION
	// ========================

	// Spring Boot Framework
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// Validation
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Observability
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// Database / SQL
	implementation("org.flywaydb:flyway-core")

	// Resilience
	implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

	// Documentation
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")


	// ========================
	// COMPILE ONLY
	// ========================

	// Lombok
	compileOnly("org.projectlombok:lombok")


	// ========================
	// RUNTIME ONLY
	// ========================

	// Databases
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("com.h2database:h2")


	// ========================
	// DEVELOPMENT ONLY
	// ========================

	developmentOnly("org.springframework.boot:spring-boot-devtools")


	// ========================
	// ANNOTATION PROCESSOR
	// ========================

	annotationProcessor("org.projectlombok:lombok")


	// ========================
	// TEST
	// ========================

	testImplementation("org.springframework.boot:spring-boot-starter-test")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}