import com.vanniktech.maven.publish.SonatypeHost.S01
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `maven-publish`
  `kotlin-dsl`
  `java-gradle-plugin`
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.binaryCompatibilityValidator)
}

buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "com.dropbox"
      artifactId = "focus"
      version = "1.0.0-pax"
      from(components["java"])
    }
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = "1.8"
    // Because Gradle's Kotlin handling is stupid, this falls out of date quickly
    apiVersion = "1.5"
    languageVersion = "1.5"

    // We use class SAM conversions because lambdas compiled into invokedynamic are not
    // Serializable, which causes accidental headaches with Gradle configuration caching. It's
    // easier for us to just use the previous anonymous classes behavior
    @Suppress("SuspiciousCollectionReassignment")
    freeCompilerArgs += "-Xsam-conversion=class"
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(8)
}

kotlin {
  explicitApi()
}

gradlePlugin {
  plugins {
    plugins.create("focus") {
      id = "com.dropbox.focus"
      implementationClass = "com.dropbox.focus.FocusPlugin"
    }
  }
}

mavenPublish {
  sonatypeHost = S01
}

dependencies {
  compileOnly(gradleApi())
  implementation(platform(libs.kotlin.bom))
  // Don't impose our version of KGP on consumers
  compileOnly(libs.kotlin.plugin)

  testImplementation(gradleTestKit())
  testImplementation(platform(libs.kotlin.bom))
  testImplementation(libs.kotlin.plugin)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

tasks.register("printVersionName") {
  doLast {
    val VERSION_NAME: String by project
    println(VERSION_NAME)
  }
}
