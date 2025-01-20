// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

subprojects {
    configurations.all {
        resolutionStrategy {
            // pin transitive dependencies of android gradle plugin with vulnerabilities
            force("com.google.protobuf:protobuf-java:4.29.3")
            force("io.netty:netty-codec-http2:4.1.100.Final")
        }
    }
}