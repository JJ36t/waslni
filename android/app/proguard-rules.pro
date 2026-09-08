# Keep Hilt generated code
-keep,allowobfuscation,allowshrinking class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * { @androidx.room.Dao *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.waslni.driver.**$$serializer { *; }
-keepclassmembers class com.waslni.driver.** {
    *** Companion;
}
-keepclasseswithmembers class com.waslni.driver.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Mapbox
-dontwarn com.mapbox.**
-keep class com.mapbox.** { *; }

# Firebase Crashlytics
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Application class
-keep class com.waslni.driver.WaselApp { *; }
