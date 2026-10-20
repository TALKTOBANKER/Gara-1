# Proguard and R8 optimization rules for Gara San Bao (Garage Storm Chase)

# Preserve WebView JavascriptInterface methods
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.example.WebAppInterface {
    public *;
}

# Preserve Google Mobile Ads (AdMob) classes and interfaces
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Keep line numbers for error reporting
-keepattributes SourceFile,LineNumberTable
