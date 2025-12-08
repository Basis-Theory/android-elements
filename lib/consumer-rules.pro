# Keep generic signatures
-keepattributes Signature

# Keep annotations and their values (used by Gson and Basis Theory)
-keepattributes *Annotation*,EnclosingMethod

# Keep Gson classes and methods
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# Keep Basis Theory SDK classes and their members
-keep class com.basistheory.** { *; }
-keep interface com.basistheory.** { *; }
-keepclassmembers class com.basistheory.** { *; }

# Keep Kotlin anonymous classes and their fields (for your payload object)
-keep class **$* { *; }
-keepclassmembers class **$* {
    *;
}

# Prevent obfuscation of fields in serialized objects
-keepclassmembers class * {
    private <fields>;
    public <fields>;
    protected <fields>;
}

# Suppress warnings for optional dependencies
-dontwarn javax.validation.**
-dontwarn jakarta.validation.**
-dontwarn aQute.bnd.annotation.**
