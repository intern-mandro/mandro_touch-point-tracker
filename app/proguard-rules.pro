# kotlinx.serialization — @Serializable 클래스의 생성된 serializer 를 지키기 위함.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.mandro.touchtracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.mandro.touchtracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}
