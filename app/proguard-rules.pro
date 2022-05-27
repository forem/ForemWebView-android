# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class com.forem.webview.AndroidWebViewBridge {
   public *;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Exoplayer rules
# Reference: https://github.com/google/ExoPlayer/issues/6773
-keep class com.google.android.exoplayer2.** { *; }

# Protobuf rules
# Reference: https://stackoverflow.com/a/14118056/5860956
-keep public class * extends com.google.protobuf.GeneratedMessageLite { *; }
