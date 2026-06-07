# ByeBox — План приближения к функционалу Hiddify

## Текущее состояние (07.06.2026)

ByeBox — нативный Android VPN-клиент (Kotlin + Compose + sing-box core). Реализовано:
- ✅ VLESS, VMess, Trojan, Shadowsocks, TUIC, Hysteria2, WireGuard — парсинг + outbound
- ✅ Подписочные ссылки (https://) с обновлением, деdup, userinfo
- ✅ Группировка конфигов по источнику (subscriptionSources)
- ✅ TCP latency probes, автовыбор лучшего
- ✅ 4 профиля маршрутизации, 4 DNS, IPv6, LAN bypass
- ✅ Quick Settings Tile — переключение VPN из шторки
- ✅ Foreground notification с кнопкой "Отключить"
- ✅ 4 темы (Dynamic + 3 кастомных)
- ✅ sing-box runtime management (c fallback на чистый TUN)
- ✅ [ИСПРАВЛЕНО] FGS crash на Android 14+ — foregroundServiceType="specialUse"
- ✅ [ИСПРАВЛЕНО] Реальная статистика трафика из sing-box Stats API (убрана симуляция)
- ✅ CoreRuntimeState + sing-box coreLogs → UI

---

## Фаза 1: Критичное — без этого приложение неполноценное

### 1.1 Bundle sing-box binary ⚡ КРИТИЧНО
Без этого sing-box не запускается (MISSING state), работает только TUN fallback без реального прокси.

- [ ] Скачать sing-box для arm64-v8a из https://github.com/SagerNet/sing-box/releases
- [ ] Разместить как `app/src/main/assets/sing-box/arm64-v8a/sing-box`
- [ ] Дополнительно для armeabi-v7a и x86_64
- [ ] Проверить SingBoxRuntime.prepareExecutable() — CoreRuntimeState → RUNNING

### 1.2 Deep Linking — импорт по URI schemes
Нужно для шаринга конфигов из браузера/Telegram.

- [ ] Intent-filters в AndroidManifest: vless://, vmess://, trojan://, ss://, tuic://, hysteria2://, wg://, hiddify://
- [ ] Обработка в MainActivity.onNewIntent() → viewModel.addConfigFromUrl()
- [ ] Обработка в MainActivity.onCreate() через intent?.data

### 1.3 Boot Receiver — Автозапуск
- [ ] BootReceiver.kt (RECEIVE_BOOT_COMPLETED)
- [ ] Permission + receiver в AndroidManifest
- [ ] Читать параметры из SharedPreferences (HiddifyVpnService.PREFS_NAME)
- [ ] Toggle "Автозапуск" в UI настройках

### 1.4 QR-сканер
- [ ] Зависимость: com.journeyapps:zxing-android-embedded:4.3.0
- [ ] Кнопка QR на главном экране
- [ ] registerForActivityResult → viewModel.addConfigFromUrl(result)
- [ ] CAMERA permission + runtime request

---

## Фаза 2: UI/UX до уровня Hiddify

### 2.1 Per-App Proxy
- [ ] Экран выбора приложений (PackageManager + иконки)
- [ ] addAllowedApplication() / addDisallowedApplication() в VpnService
- [ ] QUERY_ALL_PACKAGES permission (Android 11+)

### 2.2 Always-On VPN
- [ ] android:supportsAlwaysOn="true" в service manifest
- [ ] Обработка onRevoke() + reconnect

### 2.3 GeoIP / GeoSite
- [ ] geosite.db + geoip.db в assets
- [ ] Извлекать рядом с sing-box binary в runtime
- [ ] Routing пресеты: RU, CN, IR bypass
- [ ] Добавить в SingBoxConfigGenerator.routeSection()

### 2.4 Import / Export backup
- [ ] Экспорт конфигов + подписок + настроек в JSON
- [ ] Импорт из JSON (SAF file picker)

### 2.5 Локализация (i18n)
- [ ] strings.xml (RU) + values-en/strings.xml (EN)
- [ ] AppCompatDelegate.setApplicationLocales()

---

## Фаза 3: Полировка

### 3.1 Custom Routing Rules Editor
### 3.2 Chain Profiles
### 3.3 Network Change Monitoring
### 3.4 Unit Tests (ConfigParser, SingBoxConfigGenerator)

---

## Порядок реализации

1. **1.1** — Bundle sing-box binary (КРИТИЧНО для реального прокси)
2. **1.2** — Deep Linking
3. **1.3** — Boot Receiver
4. **1.4** — QR-сканер
5. **2.1** — Per-App Proxy
6. **2.2** — Always-On VPN
7. **2.3** — GeoIP/GeoSite
8. **2.4** — Import/Export
9. Остальное по необходимости
