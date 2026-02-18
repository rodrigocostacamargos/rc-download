# Mantém modelos de dados usados pelo Gson/Retrofit
-keepclassmembers class com.rcdownload.data.api.models.** { *; }
-keepclassmembers class com.rcdownload.data.db.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
