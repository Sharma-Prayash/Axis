# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified in
# getDefaultProguardFile('proguard-android-optimize.txt')
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Hilt / Dagger generated code
-keep class class_names { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
