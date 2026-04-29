plugins {
	java
	jacoco
	id("org.springframework.boot") version "3.2.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("info.solidsoft.pitest") version "1.19.0-rc.1"
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

	// Client
	implementation("org.springframework.cloud:spring-cloud-starter-openfeign")


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
	// SECURITY
	// ========================

	// Authentication / Authorization
	implementation("org.springframework.boot:spring-boot-starter-security")
	testImplementation("org.springframework.boot:spring-boot-starter-security")

	// JWT
	implementation("com.auth0:java-jwt:4.5.1")


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

jacoco {
	toolVersion = "0.8.12"
}

tasks.test {
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
	classDirectories.setFrom(
		files(classDirectories.files.map {
			fileTree(it) {
				exclude(
					"**/adapter/controller/request/**",
					"**/adapter/controller/response/**",
					"**/domain/viewmodels/**",
					"**/adapter/repository/entity/**",
					"com/wastecoder/picpay/PicpaySimplificadoApplication.class",
					"**/JwtTokenConfiguration.class"
				)
			}
		})
	)
}

pitest {
	pitestVersion.set("1.17.4")
	junit5PluginVersion.set("1.2.1")

	targetClasses.set(setOf(
		"com.wastecoder.picpay.user.usecases.*",
		"com.wastecoder.picpay.transaction.usecases.*"
	))

	targetTests.set(setOf(
		"com.wastecoder.picpay.user.usecases.*Test",
		"com.wastecoder.picpay.transaction.usecases.*Test"
	))

	excludedClasses.set(setOf(
		"com.wastecoder.picpay.user.usecases.LoginUserUseCaseImpl",
		"com.wastecoder.picpay.transaction.usecases.TransferUseCaseImpl"
	))

	mutators.set(setOf("STRONGER"))

	timestampedReports.set(false)
	outputFormats.set(setOf("HTML", "XML"))

	mutationThreshold.set(80)
	coverageThreshold.set(80)

	failWhenNoMutations.set(false)
	verbose.set(false)
}

tasks.named("pitest") {
	dependsOn(tasks.test)
}

apply(from = "gradle/test-summary.gradle.kts")