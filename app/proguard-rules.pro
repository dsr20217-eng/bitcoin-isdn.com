# CoreLens ProGuard / R8 Rules
-keepattributes *Annotation*
-keep class com.example.data.local.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
