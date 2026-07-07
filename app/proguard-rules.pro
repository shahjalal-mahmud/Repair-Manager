# Preserve annotations, generics, and inner-class metadata that Firestore's
# reflection-based POJO mapper needs at runtime (without this, R8 strips
# @DocumentId / @ServerTimestamp / @PropertyName in release builds).
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep every field/member of Firestore model classes untouched (name +
# existence). Broader than get/set/<init> because Firestore's mapper also
# inspects backing fields and Kotlin's synthetic default-value constructors.
-keepclassmembers class com.appriyo.repairmanager.data.model.** {
    *;
}

# Keep the model classes themselves from being removed if the shrinker
# thinks they're otherwise unused (defensive - toObject() type params can
# be erased at the call site).
-keep class com.appriyo.repairmanager.data.model.** { *; }