buildscript {
    repositories { google(); mavenCentral() }
    val kotlinVersion = file("gradle/libs.versions.toml").readLines()
        .first { it.startsWith("kotlin =") }.substringAfter('=').trim().trim('"')
    dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion") }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
