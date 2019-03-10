# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-forceprocessing
-optimizationpasses 5

# Produces useful obfuscated stack traces
# http://proguard.sourceforge.net/manual/examples.html#stacktrace
-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable
-keepattributes SourceFile
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Do not obfuscate the class files since open source
-dontobfuscate

# Support Library
-keep class !android.support.v7.internal.view.menu.*MenuBuilder*, android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

-dontwarn javax.**
-dontwarn org.apache.log4j.**
-dontwarn org.slf4j.**

# GOOGLE PROTOBUF - START
-dontwarn com.google.protobuf.**
# GOOGLE PROTOBUF - END

# GOOGLE TRANSIT REALTIME - START
-dontwarn com.google.transit.realtime.**
# GOOGLE TRANSIT REALTIME - END

# GOOGLE PLAY IN-APP BILLING - START
-keep class com.android.vending.billing.**
# GOOGLE PLAY IN-APP BILLING - END

# GOOGLE MOBILE ADS (ADMOB) - START
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}
-keep class com.google.ads.mediation.admob.AdMobAdapter {
    *;
}
-keep class com.google.ads.mediation.AdUrlAdapter {
    *;
}
# GOOGLE MOBILE ADS (ADMOB) - END

# FACEBOOK AUDIENCE NETWORK - START
-keep public class com.facebook.ads.** {
   public *;
}
-keep class com.google.ads.mediation.facebook.FacebookAdapter {
    *;
}
-dontwarn com.facebook.ads.internal.**
# FACEBOOK AUDIENCE NETWORK - END

# INMOBI - START
-keep class com.inmobi.** { *; }
# skip the Picasso library classes
-keep class com.squareup.picasso.** {*;}
-dontwarn com.squareup.picasso.**
-dontwarn com.squareup.okhttp.**
# skip Moat classes
-keep class com.moat.** {*;}
-dontwarn com.moat.**
# skip Google Play Services classes
-dontwarn com.google.android.gms.plus.**
# INMOBI - END

# CRASHLYTICS - START
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
# CRASHLYTICS - END
