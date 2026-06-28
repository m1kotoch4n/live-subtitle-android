# ProGuard rules for LiveBuddy Android release builds.
# Keep Kotlin metadata for reflection used by Service / View binding.

-keep class com.livebuddy.android.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-keep class okhttp3.** { *; }
