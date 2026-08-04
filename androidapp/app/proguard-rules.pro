# Правила R8 для release-сборки.
# Базовый набор берётся из proguard-android-optimize.txt (см. build.gradle.kts).

# Модели, которые Gson десериализует через рефлексию: имена полей должны
# сохраниться, иначе JSON перестанет мапиться на классы.
-keep class com.cloudimny.models.** { *; }

# Retrofit: сигнатуры и аннотации интерфейсов сервисов
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# SSHJ и BouncyCastle подгружают провайдеры/алгоритмы рефлексией
-keep class net.schmizz.sshj.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn net.schmizz.sshj.**
-dontwarn org.bouncycastle.**
