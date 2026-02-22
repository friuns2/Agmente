# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.agmente.**$$serializer { *; }
-keepclassmembers class com.agmente.** { *** Companion; }
-keepclasseswithmembers class com.agmente.** { kotlinx.serialization.KSerializer serializer(...); }
