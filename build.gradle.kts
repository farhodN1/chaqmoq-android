// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0") // Already existing classpath for Android Gradle plugin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10") // Already existing classpath for Kotlin plugin

        // Add this for Safe Args
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.0")
    }
}

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
//    id("androidx.navigation.safeargs.kotlin") version "2.5.0" apply true
}