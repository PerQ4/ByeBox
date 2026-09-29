# ByeBox — ProGuard/R8 rules (release builds)

# --- libv2ray / gomobile (native bridge) ---
# Эти правила дублируют proguard.txt из libv2ray.aar на случай пересборки AAR:
# Go-мост go.Seq использует отражение и не должен быть переименован/вырезан.
-keep class go.** { *; }
-keep class libv2ray.** { *; }

# --- Gson DTO (сериализуются в MMKV / настройки) ---
# R8 не должен переименовывать/вырезать эти классы,
# иначе Gson не может создать экземпляр (Abstract classes can't be instantiated).
-keep class com.v2ray.ang.dto.** { *; }

# --- Gson: служебные классы и атрибуты ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.stream.** { *; }

# --- Room (WorkManager database) ---
# Сгенерированные *_Impl-классы создаются через отражение;
# без keep-правил R8 удаляет конструкторы → NoSuchMethodException.
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- WorkManager ---
-keep class androidx.work.impl.** { *; }

# --- ML Kit (barcode/QR-сканер) ---
# Регистраторы компонентов находятся через ComponentDiscovery (рефлексия),
# имена классов должны пережить R8.
-keep class com.google.mlkit.** { *; }

# --- Serializable (передача сообщений между сервисом и UI) ---
# TestServiceMessage и подобные пересылаются через Intent/Bundle с сериализацией,
# поэтому поля и служебные методы должны пережить R8.
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# --- Рефлексия в UI ---
# Внутренние data-классы, с которыми работает сериализация предпочтений
# (prefs простые ключи, поля не рефлексируются Gson'ом) — keep не требуется.
# Кастомный Gson (JsonUtil) работает с JsonObject — безопасно для R8.