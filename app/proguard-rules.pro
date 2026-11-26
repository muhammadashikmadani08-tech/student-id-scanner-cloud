# Keep ML Kit / CameraX classes
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Firestore uses reflection on the model classes; keep them intact.
-keep class com.institution.idscanner.cloud.data.** { *; }
-keepclassmembers class com.institution.idscanner.cloud.data.** { *; }
