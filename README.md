# ByeBox 📦

[English](#english) | [Русский](#русский)

---

## English

ByeBox is a modern, high-performance Android VPN client built with Jetpack Compose and designed using the Material You (Material 3) design system. Under the hood, it utilizes a custom Xray/v2ray/sing-box core structure for robust, secure, and flexible proxy routing.

### Features ✨

- **Material You (MD3) Interface**: Harmonious colors, dark mode support, fluid micro-animations, and a clean layout.
- **VpnService Integration**: Built-in, system-level VPN support with robust lifecycle handling.
- **Quick Settings Tile**: Control your connection status directly from your system's quick settings shade.
- **Status Notifications**: Centered, non-distorting status notifications displaying real-time connection info.
- **Routing & DNS Customization**: Native support for geoip/geosite routing and local DNS configurations.
- **Multi-protocol Support**: Handles VLESS, VMESS, Trojan, Shadowsocks and more.

### Getting Started 🚀

#### Prerequisites
- Android Studio Koala / Ladybug or newer.
- Android SDK 36 (minSdk 24).
- JDK 17.

#### Building and Running
1. Clone the repository:
   ```bash
   git clone https://github.com/PerQ4/ByeBox.git
   ```
2. Open the project in Android Studio.
3. Build the debug version:
   ```bash
   ./gradlew assembleDebug
   ```
4. Build the signed release version:
   ```bash
   ./gradlew assembleRelease
   ```
   *Note: Release builds are configured to automatically sign with the standard debug certificate (`debug.keystore`) for ease of deployment.*

### Project Structure 📁

- `app/` - The main Android application code (written in Kotlin and Jetpack Compose).
  - `src/main/res/drawable/` - Vector graphics assets (including the updated non-stretching notification drawables).
  - `src/main/java/com/perqa/byebox/` - Core UI screens, ViewModels, and Tile Service logic.
  - `src/main/java/com/v2ray/ang/` - Legacy protocol handling, MMKV management, and utility classes.
- `xray-build/` - Custom Go core implementation and AAR wrapper building scripts.
- `scripts/` - Automated setup and management scripts.
- `reference/` - Local references and external modules (ignored by git).

### License 📄
This project is licensed under the Apache License 2.0. See the LICENSE files for details.

---

## Русский

ByeBox - это современный и высокопроизводительный VPN-клиент для Android, созданный на базе Jetpack Compose с поддержкой системы дизайна Material You (Material 3). В качестве бэкенда используется кастомное ядро Xray/v2ray/sing-box, обеспечивающее надежную, безопасную и гибкую маршрутизацию трафика.

### Возможности ✨

- **Интерфейс Material You (MD3)**: динамические цвета, поддержка темной темы, плавные микроанимации и чистый макет.
- **Интеграция с VpnService**: встроенная поддержка системного VPN с корректной обработкой жизненного цикла.
- **Плитка быстрых настроек**: управление подключением прямо из панели быстрых настроек системы.
- **Уведомления о статусе**: центрированные значки уведомлений без искажения пропорций, показывающие текущее состояние подключения.
- **Кастомизация маршрутов и DNS**: встроенная поддержка правил geoip/geosite и локальных конфигураций DNS.
- **Поддержка множества протоколов**: обработка VLESS, VMESS, Trojan, Shadowsocks и других.

### Начало работы 🚀

#### Требования
- Android Studio Koala / Ladybug или новее.
- Android SDK 36 (minSdk 24).
- JDK 17.

#### Сборка и запуск
1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/PerQ4/ByeBox.git
   ```
2. Откройте проект в Android Studio.
3. Сборка отладочной (debug) версии:
   ```bash
   ./gradlew assembleDebug
   ```
4. Сборка подписанной релизной версии:
   ```bash
   ./gradlew assembleRelease
   ```
   *Примечание: релизные сборки автоматически подписываются стандартным отладочным ключом (debug.keystore) для упрощения установки.*

### Структура проекта 📁

- `app/` - исходный код Android-приложения (Kotlin и Jetpack Compose).
  - `src/main/res/drawable/` - графические ресурсы (включая исправленные значки уведомлений).
  - `src/main/java/com/perqa/byebox/` - экраны интерфейса, ViewModels и логика работы службы плитки.
  - `src/main/java/com/v2ray/ang/` - обработка протоколов, менеджер MMKV и вспомогательные утилиты.
- `xray-build/` - реализация кастомного ядра на Go и скрипты сборки библиотеки AAR.
- `scripts/` - автоматизированные скрипты для настройки и управления.
- `reference/` - локальные ссылки и внешние модули (игнорируются git).

### Лицензия 📄
Проект распространяется под лицензией Apache License 2.0. Подробности смотрите в файле LICENSE.
