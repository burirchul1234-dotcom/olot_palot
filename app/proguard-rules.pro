# Kotlinx serialization: keep route classes used by type-safe navigation.
-keepclassmembers class com.olotpalot.core.navigation.** { *; }
-keep,includedescriptorclasses class com.olotpalot.**$$serializer { *; }
-keepclassmembers class com.olotpalot.** { *** Companion; }
-keepclasseswithmembers class com.olotpalot.** { kotlinx.serialization.KSerializer serializer(...); }
