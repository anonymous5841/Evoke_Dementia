import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)                       // NEW (needed for Room)
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.example.evoke"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.evoke"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "W8jnNFpds9I1z1xkRCdn", "\"${localProperties["W8jnNFpds9I1z1xkRCdn"]}\"")

        // [GROQ API KEY] transcription + summary (transcription/GroqClient.kt).
        // Read from local.properties (GROQ_API_KEY=...), never committed to Git.
        buildConfigField("String", "GROQ_API_KEY", "\"${localProperties["GROQ_API_KEY"] ?: ""}\"")
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
    buildFeatures {                               // NEW (main has none)
        compose = true
        buildConfig = true
    }
    androidResources {
        // don't let AGP compress the models in the APK (face: tflite, voice: onnx)
        noCompress += listOf("tflite", "onnx")
    }
//    packaging {
//        jniLibs {
//            pickFirsts += "**/libonnxruntime.so"
//        }
//    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.airbnb.android:lottie-compose:6.3.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")


    // Room + SQLCipher
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)

    // Google sign-in + Drive authorization
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Argon2 / crypto
    implementation("org.bouncycastle:bcprov-jdk18on:1.85.2")

    //camerapreviewasync
    implementation("io.coil-kt:coil-compose:2.6.0")

    //camera screen
    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Emergency Call + Send Message (EmergencyAlertService / LocationShareService)
    // FusedLocationProviderClient for the live location sent by SMS
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // DataStore for the emergency-call escalation state (data/EmergencyContactState.kt)
    implementation("androidx.datastore:datastore-preferences:1.1.1")


    //mobilefacenetmodel
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Voice pipeline (voice/): Silero VAD + CAM++ speaker model via sherpa-onnx.
    // Bundles its own libonnxruntime.so; models are in assets/ (root, not a sub-folder).
    //TTS (Piper)
    implementation(files("libs/sherpa_onnx_test.aar"))

    //NLLB
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}


// ---------------------------------------------------------
// AUTOMATIC NLLB MODEL DEPLOYMENT
// Runs after Android Studio installs the Debug app.
// Copies missing or outdated ONNX models to connected devices.
// ---------------------------------------------------------

val deployNllbModels = tasks.register("deployNllbModels") {

    group = "evoke"
    description = "Automatically transfers NLLB ONNX models to connected Android devices."

    doLast {

        // Folder containing the original models on your laptop.
        val localModelDir = File(rootProject.projectDir, "models/nllb/int8")

        // Android SDK location from local.properties.
        val sdkPath = localProperties.getProperty("sdk.dir")
            ?: System.getenv("ANDROID_HOME")
            ?: "${System.getenv("LOCALAPPDATA")}\\Android\\Sdk"

        val adb = File(sdkPath, "platform-tools/adb.exe")

        // Destination used by NLLBTranslator.getExternalFilesDir(null).
        val remoteDir =
            "/sdcard/Android/data/com.example.evoke/files/models/nllb/int8"

        // All three models required by NLLBTranslator.
        val modelFiles = listOf(
            "encoder_model.onnx",
            "decoder_model_embedded.onnx",
            "decoder_with_past_model_embedded.onnx"
        )

        // Run an ADB command and return its output and exit code.
        fun runAdb(vararg arguments: String): Pair<Int, String> {

            val process = ProcessBuilder(
                listOf(adb.absolutePath) + arguments
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            return Pair(exitCode, output.trim())
        }

        // Check that ADB is available.
        if (!adb.exists()) {
            throw GradleException("ADB not found: ${adb.absolutePath}")
        }

        // Check that the local model folder exists.
        if (!localModelDir.exists()) {
            throw GradleException("NLLB model folder not found: $localModelDir")
        }

        // Check that all three local model files exist.
        for (model in modelFiles) {
            if (!File(localModelDir, model).isFile) {
                throw GradleException("Missing local NLLB model: $model")
            }
        }

        println("\n========== NLLB MODEL DEPLOYMENT ==========")

        // Find all connected and authorized Android devices.
        val (deviceExitCode, deviceOutput) = runAdb("devices")

        if (deviceExitCode != 0) {
            throw GradleException("Unable to communicate with ADB: $deviceOutput")
        }

        val devices = deviceOutput.lines()
            .drop(1)
            .mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"))

                if (parts.size >= 2 && parts[1] == "device") {
                    parts[0]
                } else {
                    null
                }
            }

        if (devices.isEmpty()) {
            throw GradleException(
                "No authorized Android device found. Connect your phone and enable USB debugging."
            )
        }

        println("Connected devices: ${devices.size}")

        // Process every connected device.
        for (serial in devices) {

            println("\nDevice: $serial")

            // Create the destination directory on the phone.
            val (mkdirCode, mkdirOutput) = runAdb(
                "-s", serial, "shell", "mkdir", "-p", remoteDir
            )

            if (mkdirCode != 0) {
                throw GradleException(
                    "Could not create NLLB directory on $serial: $mkdirOutput"
                )
            }

            // Transfer only missing or outdated models.
            for (model in modelFiles) {

                val localFile = File(localModelDir, model)
                val remoteFile = "$remoteDir/$model"

                val localSize = localFile.length().toString()

                // Check whether the remote file exists and has the correct size.
                val (statCode, remoteSize) = runAdb(
                    "-s", serial,
                    "shell", "stat", "-c", "%s", remoteFile
                )

                if (statCode == 0 && remoteSize == localSize) {

                    println("Already up to date: $model")

                } else {

                    println("Transferring: $model")

                    val (pushCode, pushOutput) = runAdb(
                        "-s", serial,
                        "push",
                        localFile.absolutePath,
                        remoteFile
                    )

                    if (pushCode != 0) {
                        throw GradleException(
                            "Failed to transfer $model to $serial: $pushOutput"
                        )
                    }

                    // Verify that the transferred file exists and is non-empty.
                    val (verifyCode, _) = runAdb(
                        "-s", serial, "shell", "test", "-s", remoteFile
                    )

                    if (verifyCode != 0) {
                        throw GradleException(
                            "Verification failed for $model on $serial"
                        )
                    }

                    println("Successfully transferred: $model")
                }
            }

            println("NLLB models ready on device: $serial")
        }

        println("\n========== NLLB DEPLOYMENT COMPLETE ==========\n")
    }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    dependsOn(deployNllbModels)
}