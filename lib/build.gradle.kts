import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.basistheory.elements"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        buildConfigField("String", "VERSION_NAME", "\"${defaultConfig.versionName}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    tasks.withType<Test> {
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.basistheory"
                artifactId = "android-elements"
                version = android.defaultConfig.versionName
            }
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("com.fasterxml.jackson.core:jackson-core:2.15.0-rc1")
    }
}


dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.commons.lang3)
    implementation(libs.threetenbp)
    implementation(libs.basistheory.java) {
        exclude(group = "javax.ws.rs", module = "javax.ws.rs-api")
    }
    implementation(libs.okhttp)
    implementation(libs.gson)
    testImplementation(libs.junit)
    testImplementation(libs.junitparams)
    testImplementation(libs.robolectric)
    testImplementation(libs.strikt.core)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.javafaker)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            when (requested.module.toString()) {
                "org.yaml:snakeyaml" -> {
                    artifactSelection {
                        selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
                    }
                    useVersion("2.2")
                }
                "org.bouncycastle:bcprov-jdk18on" -> {
                    artifactSelection {
                        selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
                    }
                    useVersion("1.78")
                }
            }
        }
    }
}
