# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the android.buildTypes.release.proguardFiles list.

# Standard JNI preservation
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve native JNI library references and callbacks
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep config models parsed from/to JSON (XrayProfilePersistence)
-keep class com.xprox.sentinel.config.XrayConfigManager$ServerProfile { *; }
-keep class com.xprox.sentinel.config.XrayConfigManager$LocalProxyCredentials { *; }

# Keep log models
-keep class com.xprox.sentinel.log.LogManager$LogEntry { *; }
-keep class com.xprox.sentinel.log.LogManager$SessionInfo { *; }

# Keep UI log and screen models
-keep class com.xprox.sentinel.ui.screens.VisualLogEntry { *; }

# Keep language manager enum and its methods used in reflection
-keep class com.xprox.sentinel.data.LanguageManager$Language {
    **[] values();
    ** valueOf(java.lang.String);
    *;
}

# Keep support classes for reflection in Java FileDescriptor
-keep class java.io.FileDescriptor {
    private int descriptor;
    *;
}

# Suppress warnings related to system library reflections
-dontwarn java.io.FileDescriptor
-dontwarn android.system.Os
