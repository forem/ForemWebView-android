object AndroidSDK {
    const val compile = 30
    const val minimum = 21
    const val target = compile
    const val buildTools = "29.0.3"
}

object AndroidClient {
    const val applicationId = "com.forem.android"
    const val versionCode = 15
    const val versionName = "1.0.9"
    const val testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}

object BuildPlugins {
    val androidApplication by lazy {
        "com.android.application"
    }
    val androidGradlePlugin by lazy {
        "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"
    }
    val firebaseGradlePlugin by lazy {
        "com.google.gms:google-services:${Versions.firebaseGradlePlugin}"
    }
    val googleServicesGradlePlugin by lazy {
        "com.google.gms.google-services"
    }
    val hiltAndroid by lazy {
        "dagger.hilt.android.plugin"
    }
    val hiltGradlePlugin by lazy {
        "com.google.dagger:hilt-android-gradle-plugin:${Versions.hiltGradlePlugin}"
    }
    val kotlinAndroid by lazy {
        "kotlin-android"
    }
    val kotlinAndroidExtensions by lazy {
        "kotlin-android-extensions"
    }
    val kotlinKapt by lazy {
        "kotlin-kapt"
    }
    val kotlinGradlePlugin by lazy {
        "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlinGradlePlugin}"
    }
    val protobufGradlePlugin by lazy {
        "com.google.protobuf:protobuf-gradle-plugin:${Versions.protobufGradlePlugin}"
    }
}

object Modules {
    const val model = ":model"
    const val foremWebView = ":foremwebview"
}

object Libraries {
    val appCompat by lazy {
        "androidx.appcompat:appcompat:${Versions.appCompat}"
    }
    val browser by lazy {
        "androidx.browser:browser:${Versions.browser}"
    }
    val constraintLayout by lazy {
        "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"
    }
    val dataStore by lazy {
        "androidx.datastore:datastore:${Versions.dataStore}"
    }
    val exoplayerCore by lazy {
        "com.google.android.exoplayer:exoplayer-core:${Versions.exoplayer}"
    }
    val exoplayerHls by lazy {
        "com.google.android.exoplayer:exoplayer-hls:${Versions.exoplayer}"
    }
    val exoplayerMediaSession by lazy {
        "com.google.android.exoplayer:extension-mediasession:${Versions.exoplayer}"
    }
    val exoplayerUI by lazy {
        "com.google.android.exoplayer:exoplayer-ui:${Versions.exoplayer}"
    }
    val firebaseAnalytics by lazy {
        "com.google.firebase:firebase-analytics:${Versions.firebaseAnalytics}"
    }
    val firebaseMessaging by lazy {
        "com.google.firebase:firebase-messaging:${Versions.firebaseMessaging}"
    }
    val fragmentKtx by lazy {
        "androidx.fragment:fragment-ktx:${Versions.fragmentKtx}"
    }
    val glide by lazy {
        "com.github.bumptech.glide:glide:${Versions.glide}"
    }
    val glideCompiler by lazy {
        "com.github.bumptech.glide:compiler:${Versions.glide}"
    }
    val gson by lazy {
        "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
    }
    val hiltAndroid by lazy {
        "com.google.dagger:hilt-android:${Versions.hiltAndroid}"
    }
    val hiltAndroidCompiler by lazy {
        "com.google.dagger:hilt-android-compiler:${Versions.hiltAndroidCompiler}"
    }
    val ktxCore by lazy {
        "androidx.core:core-ktx:${Versions.ktxCore}"
    }
    val kotlinStandardLibrary by lazy {
        "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlinStandardLibrary}"
    }
    val lifecycleCommon by lazy {
        "androidx.lifecycle:lifecycle-common-java8:${Versions.lifecycle}"
    }
    val lifecycleExtensions by lazy {
        "androidx.lifecycle:lifecycle-extensions:${Versions.lifecycle}"
    }
    val lifecycleLiveData by lazy {
        "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycle}"
    }
    val lifecycleRuntime by lazy {
        "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}"
    }
    val lifecycleViewModel by lazy {
        "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
    }
    val material by lazy {
        "com.google.android.material:material:${Versions.material}"
    }
    val protobuf by lazy {
        "com.google.protobuf:protobuf-javalite:$${Versions.protobuf}"
    }
    val retrofit by lazy {
        "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    }
}
