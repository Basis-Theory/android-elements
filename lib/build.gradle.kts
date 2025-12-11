import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "com.basistheory.elements"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        val versionName = "2.5.0"
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    kotlinOptions {
        jvmTarget = "17"
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

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.commons.lang3)
    implementation(libs.threetenbp)
    implementation(libs.basistheory.javaSdk)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.jose.jwt)
    implementation(libs.cryptotink)
    testImplementation(libs.junit)
    testImplementation(libs.junitparams)
    testImplementation(libs.robolectric)
    testImplementation(libs.strikt.core)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.javafaker)
    testImplementation(libs.kotlinx.coroutines.test)
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
                "com.fasterxml.jackson.core:jackson-core" -> {
                    artifactSelection {
                        selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
                    }
                    useVersion("2.15.0-rc1")
                }
                "io.netty:netty-handler" -> {
                    useVersion("4.1.118.Final")
                }
            }
        }
    }
}
