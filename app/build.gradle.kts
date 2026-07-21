plugins {
    alias(libs.plugins.android.application)
    id("io.freefair.lombok") version "9.1.0"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.the_coffe_coders.fastestlap"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.the_coffe_coders.fastestlap"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gson)
    implementation(libs.converter.gson)
    implementation(libs.retrofit)
    implementation(libs.converter.scalars)
    implementation(libs.threetenbp)
    implementation(libs.fragment.ktx)
    implementation(libs.logging.interceptor)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.room.common)
    implementation(libs.room.runtime)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.lifecycle.livedata)
    implementation(libs.swiperefreshlayout)
    compileOnly(libs.lombok.v11830)
    annotationProcessor(libs.lombok.v11830)
    testCompileOnly(libs.lombok.v11830)
    testAnnotationProcessor(libs.lombok.v11830)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    annotationProcessor(libs.room.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.commons.validator)
    implementation(libs.glide)
    implementation(libs.rome)
    annotationProcessor(libs.compiler)

}