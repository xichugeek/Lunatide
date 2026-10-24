-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# SQLCipher uses JNI field/method lookup. Keep its Java bridge and native entry points.
-keep class net.zetetic.database.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }

# Room and serialization ship their own consumer rules; retain the generated Room implementation name.
-keep class com.xichugeek.lunatide.data.db.LunatideDatabase_Impl { *; }
