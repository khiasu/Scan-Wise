# ============================================================
# ScanWise ProGuard / R8 Rules
# ============================================================

# --- Tink / security-crypto (errorprone annotations not on classpath) ---
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# --- Keep Kotlin metadata so reflection still works ---
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# --- Room (keep entity/dao classes) ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# --- OkHttp / networking ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# --- WorkManager ---
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# --- Compose (keep necessary internals) ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- Keep data classes used in UI state ---
-keepclassmembers class com.khiasu.docscanai.data.** { *; }
