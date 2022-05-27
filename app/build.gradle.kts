plugins {
    id(BuildPlugins.androidApplication)
    id(BuildPlugins.kotlinAndroid)
    id(BuildPlugins.kotlinAndroidExtensions)
    id(BuildPlugins.kotlinKapt)
    id(BuildPlugins.hiltAndroid)
    id(BuildPlugins.googleServicesGradlePlugin)
}

android {
    compileSdkVersion(AndroidSDK.compile)
    buildToolsVersion(AndroidSDK.buildTools)

    buildFeatures {
        dataBinding = true
    }

    defaultConfig {
        applicationId(AndroidClient.applicationId)
        minSdkVersion(AndroidSDK.minimum)
        targetSdkVersion(AndroidSDK.target)
        versionCode(AndroidClient.versionCode)
        versionName(AndroidClient.versionName)

        buildConfigField("String", "FOREM_DISCOVER_URL", getProperty("FOREM_DISCOVER_URL"))
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(Libraries.appCompat)
    implementation(Libraries.constraintLayout)
    implementation(Libraries.kotlinStandardLibrary)
    implementation(Libraries.ktxCore)
    implementation(Libraries.material)

    implementation(Libraries.fragmentKtx)

    // WebView
    implementation(Libraries.browser)

    // Lifecycle
    implementation(Libraries.lifecycleCommon)
    implementation(Libraries.lifecycleExtensions)
    implementation(Libraries.lifecycleLiveData)
    implementation(Libraries.lifecycleViewModel)
    implementation(Libraries.lifecycleRuntime)

    // Exoplayer
    implementation(Libraries.exoplayerCore)
    implementation(Libraries.exoplayerHls)
    implementation(Libraries.exoplayerMediaSession)
    implementation(Libraries.exoplayerUI)

    // Proto DataStore
    implementation(Libraries.dataStore)
    implementation(Libraries.protobuf)

    // Retrofit
    implementation(Libraries.retrofit)
    implementation(Libraries.gson)

    // Dagger Hilt
    implementation(Libraries.hiltAndroid)
    kapt(Libraries.hiltAndroidCompiler)

    // Push notifications - FCM
    implementation(Libraries.firebaseAnalytics)
    implementation(Libraries.firebaseMessaging)

    // Glide
    implementation(Libraries.glide)
    annotationProcessor(Libraries.glideCompiler)

    // Model module
    implementation(project(Modules.model))
    testImplementation(project(Modules.model))
    androidTestImplementation(project(Modules.model))

    implementation(project(Modules.foremWebView))
}

kapt {
    correctErrorTypes = true
}

// We are using this function because using java.io.FileInputStream to read file was not working.
// Reference: https://stackoverflow.com/a/59053039
// This function returns value of key stored in `keystore.properties`
fun getProperty(key: String): String {
    val items = HashMap<String, String>()
    val keystorePropertyFile = rootProject.file("keystore.properties")
    (keystorePropertyFile.exists()).let {
        keystorePropertyFile.forEachLine {
            items[it.split("=")[0]] = it.split("=")[1]
        }
    }
    return items[key]!!
}
