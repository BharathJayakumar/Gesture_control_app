// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20" apply false // ✅ FIXED
}

// Define versions as top-level variables
val composeVersion = "1.5.4"
val composeCompilerVersion = "1.5.4"

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
