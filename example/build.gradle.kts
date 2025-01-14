import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

android {
    namespace = "com.basistheory.elements.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.basistheory.elements.example"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "BASIS_THEORY_API_URL", tryFindProperty("com.basistheory.elements.example.apiUrl"))
        buildConfigField("String", "BASIS_THEORY_API_KEY", tryFindProperty("com.basistheory.elements.example.apiKey"))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    dataBinding {
        enable = true
    }
}

dependencies {
    implementation(project(":lib"))

    implementation(libs.androidx.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.gson)
    implementation(libs.threetenbp)
    implementation(libs.android.gif.drawable)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.javafaker)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

configurations.configureEach {
    resolutionStrategy {
        eachDependency {
            if (requested.module.toString() == "org.yaml:snakeyaml") {
                artifactSelection {
                    selectArtifact(DependencyArtifact.DEFAULT_TYPE, null, null)
                }
                useVersion("2.2")
            }
        }
    }
}

fun tryFindProperty(name: String): String {
    val localProperties = tryReadLocalProperties()
    val propertyValue = localProperties[name] ?: findProperty(name)

    return if (propertyValue != null) "\"$propertyValue\"" else "null"
}

fun tryReadLocalProperties(): Properties {
    val properties = Properties()

    try {
        val propFile = rootProject.file("./local.properties")
        properties.load(FileInputStream(propFile))
    } catch (ignored: Exception) {
    }

    return properties
}
