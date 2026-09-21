# ProGuard / R8 Rules for Varalakshmi Portfolio

# Keep Portfolio Data Models for JSON parsing and serialization
-keepclassmembers class com.example.varalakshmiportfolio.model.** { *; }
-keep class com.example.varalakshmiportfolio.model.** { *; }

# Keep UI state models
-keepclassmembers class com.example.varalakshmiportfolio.ui.** { *; }
-keep class com.example.varalakshmiportfolio.ui.** { *; }

# Keep Kotlinx Coroutines and Flow
-keepnames class kotlinx.coroutines.** { *; }

# Jetpack Compose Rules
-keepattributes *Annotation*
-dontwarn androidx.compose.**
