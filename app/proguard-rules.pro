# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep MediaItem etc.
-keepclassmembers class * extends androidx.media3.common.MediaItem { *; }
