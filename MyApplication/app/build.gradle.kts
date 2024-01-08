plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

}

android {
    namespace = "com.example.myapplication"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation("com.google.android.gms:play-services-mlkit-face-detection:17.0.1")


    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.navigation:navigation-fragment:2.5.1")
    implementation("androidx.navigation:navigation-ui:2.5.1")

    implementation("com.airbnb.android:lottie:4.2.2")
    implementation ("com.google.guava:guava:30.1-jre")
    implementation("com.google.code.gson:gson:2.8.9")

    implementation("androidx.camera:camera-view:1.2.0-alpha04")
    implementation("androidx.camera:camera-camera2:1.2.0-alpha04")
    implementation("androidx.camera:camera-core:1.2.0-alpha04")
    implementation("androidx.camera:camera-lifecycle:1.2.0-alpha04")

    implementation("org.tensorflow:tensorflow-lite:0.0.0-nightly-SNAPSHOT")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.3.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.3.0")

    implementation("androidx.room:room-runtime:2.4.0")

}