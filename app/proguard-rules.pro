# Keep only the JavaScript bridge surface that WebView calls by name.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep the runtime entry points referenced from AndroidManifest.xml.
-keep class com.noor.prism.MainActivity { *; }
-keep class com.noor.prism.PlaybackKeepAliveService { *; }
