# ---------------------------------------------------------------------------------------------
# R8 rules. Keep this file SMALL: every broad "-keep ... { *; }" stops R8 from shrinking,
# optimizing and renaming those classes, and Play Console reports it as a low optimization /
# obfuscation / shrinking rate. Only add a rule when something is really looked up by name.
# ---------------------------------------------------------------------------------------------

# WireGuard: libwg-go.so binds to the native methods of GoBackend by name (JNI). Only that class's
# native methods need protecting -- the rest of the library is ordinary Java that R8 may shrink,
# optimize and rename. (The default Android rules already do this; stated here so it's explicit.)
# The VpnService subclass the library declares in its manifest is kept automatically from the manifest.
-keepclasseswithmembernames,includedescriptorclasses class com.wireguard.android.backend.GoBackend {
    native <methods>;
}

# The app's own data classes need no rule: JSON is parsed by hand (org.json) with string keys, and
# nothing uses reflection or serialization libraries.

# Generic signatures (used by Kotlin/coroutines/OkHttp internals).
-keepattributes Signature

# Readable crash reports in Play Console (line numbers survive, original file names are hidden)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# (-repackageclasses / -allowaccessmodification are deliberately not used: small gain, can break SDKs that look classes up by name.)

# Release builds don't need verbose/debug logging (warnings and errors are kept for crash diagnosis).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Yandex Mobile Ads SDK: keep its classes un-renamed; NativeAdSlot tells a failed native bind apart by the result's class
# name (AdBindingResult.Failure). Only remove after testing a signed release build.
-keep class com.yandex.mobile.ads.** { *; }
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Exceptions
