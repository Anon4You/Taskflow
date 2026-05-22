# ============================================================
# TaskFlow Pro - ProGuard Rules
# Aggressive compression and optimization
# ============================================================

# ---- General Optimization ----
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# ---- Remove Logging ----
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# ---- Keep Kotlin Metadata ----
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses,EnclosingMethod

# ---- Keep Gson ----
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---- Keep Data Classes for Gson ----
-keep class com.taskflow.app.data.** { *; }
-keepclassmembers class com.taskflow.app.data.** {
    <init>(...);
    <fields>;
}

# ---- Keep JNI / Native Methods ----
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---- Keep Android Components ----
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ---- Keep View Constructors ----
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ---- Keep Parcelable ----
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ---- Keep Serializable ----
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---- Keep R class ----
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ---- Keep Enum Values ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- AndroidX / Material ----
-keep class com.google.android.material.** { *; }
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn com.google.android.material.**
-dontwarn androidx.**

# ---- RecyclerView ----
-keep class androidx.recyclerview.widget.** { *; }
-keepclassmembers class * extends androidx.recyclerview.widget.RecyclerView$ViewHolder {
    public <init>(android.view.View);
}

# ---- ConstraintLayout ----
-keep class androidx.constraintlayout.widget.** { *; }

# ---- AppCompat ----
-keep class * extends androidx.appcompat.app.AppCompatActivity
-keep class * extends androidx.appcompat.app.ActionBar

# ---- Remove Debug Info ----
-renamesourcefileattribute SourceFile

# ---- Aggressive Size Reduction ----
-repackageclasses ''
-mergeinterfacesaggressively
-overloadaggressively

# ---- Remove Unused Code ----
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn java.lang.invoke.**
