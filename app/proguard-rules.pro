# Add project specific ProGuard rules here.
-keep class com.vovaplusexp.gpscontroller.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# OSMDroid
-dontwarn org.osmdroid.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
