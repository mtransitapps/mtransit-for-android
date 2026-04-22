# Producing useful obfuscated stack traces
# https://www.guardsquare.com/manual/configuration/examples#stacktrace
-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable
-keepattributes SourceFile
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Do not obfuscate the class files since open source (DEBUG only)
-dontobfuscate

# CRASHLYTICS - START
# https://firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports#config-r8-proguard-dexguard
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.
# CRASHLYTICS - END

# AUDIENCE NETWORK - START
# https://developers.facebook.com/docs/audience-network/setting-up/android/
-keep class com.facebook.ads.AudienceNetworkAds { *; }
-keep class com.facebook.ads.internal.dynamicloading.** { *; }
-keep class com.google.ads.mediation.facebook.FacebookMediationAdapter {
    *;
}
-dontwarn com.facebook.ads.internal.**
-dontwarn com.facebook.infer.annotation.*
# AUDIENCE NETWORK - END

# VUNGLE - START
-dontwarn com.vungle.ads.**
-keepclassmembers class com.vungle.ads.** {
  *;
}
# VUNGLE - END

-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
-dontwarn android.media.LoudnessCodecController
