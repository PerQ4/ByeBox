# ByeBox — План развития

## Текущее состояние (09.06.2026)

ByeBox — нативный Android VPN-клиент (Kotlin + Compose + Xray-core через libv2ray.aar).
Ядро: Xray-core (AndroidLibXrayLite). Поддерживаемые протоколы: VLESS (Reality/Vision), VMess, Trojan, Shadowsocks.
Протоколы TUIC, Hysteria2, WireGuard — не поддерживаются Xray-core, настройки скрыты.

Реализовано:
- ✅ Xray-core через `libv2ray.aar` (gomobile bind)
- ✅ VLESS, VMess, Trojan, Shadowsocks — парсинг + outbound
- ✅ XrayConfigGenerator (TUN inbound, routing, DNS, outbounds)
- ✅ ByeBoxVpnService (переименован из HiddifyVpnService, очищен от легаси)
- ✅ Подписочные ссылки (https://) с обновлением, деdup, userinfo
- ✅ Группировка конфигов по источнику (subscriptionSources)
- ✅ TCP latency probes, автовыбор лучшего
- ✅ 4 профиля маршрутизации, 4 DNS, IPv6, LAN bypass
- ✅ Quick Settings Tile — переключение VPN из шторки
- ✅ Foreground notification с кнопкой "Отключить"
- ✅ 4 темы (Dynamic + 3 кастомных)
- ✅ [ИСПРАВЛЕНО] FGS crash на Android 14+ — foregroundServiceType="specialUse"
- ✅ [ИСПРАВЛЕНО] Реальная статистика трафика из Xray Stats API
- ✅ CoreRuntimeState + Xray coreLogs → UI

---

## Фаза 1: Критичное

### 1.1 Deep Linking — импорт по URI schemes
Нужно для шаринга конфигов из браузера/Telegram.

- [ ] Intent-filters в AndroidManifest: vless://, vmess://, trojan://, ss://
- [ ] Обработка в MainActivity.onNewIntent() → viewModel.addConfigFromUrl()
- [ ] Обработка в MainActivity.onCreate() через intent?.data

### 1.2 Boot Receiver — Автозапуск
- [ ] BootReceiver.kt (RECEIVE_BOOT_COMPLETED)
- [ ] Permission + receiver в AndroidManifest
- [ ] Читать параметры из SharedPreferences (ByeBoxVpnService.PREFS_NAME)
- [ ] Toggle "Автозапуск" в UI настройках

### 1.3 QR-сканер
- [ ] Зависимость: com.journeyapps:zxing-android-embedded:4.3.0
- [ ] Кнопка QR на главном экране
- [ ] registerForActivityResult → viewModel.addConfigFromUrl(result)
- [ ] CAMERA permission + runtime request

---

## Фаза 2: UI/UX

### 2.1 Per-App Proxy
- [ ] Экран выбора приложений (PackageManager + иконки)
- [ ] addAllowedApplication() / addDisallowedApplication() в VpnService
- [ ] QUERY_ALL_PACKAGES permission (Android 11+)

### 2.2 Always-On VPN
- [ ] android:supportsAlwaysOn="true" в service manifest
- [ ] Обработка onRevoke() + reconnect

### 2.3 GeoIP / GeoSite
- [ ] geosite.dat + geoip.dat в assets (формат Xray)
- [ ] Извлекать в рабочую директорию core в runtime
- [ ] Routing пресеты: RU, CN, IR bypass
- [ ] Добавить в XrayConfigGenerator.routeSection()

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
### 3.4 Unit Tests (ConfigParser, XrayConfigGenerator)

---

## Порядок реализации

1. **1.1** — Deep Linking
2. **1.2** — Boot Receiver
3. **1.3** — QR-сканер
4. **2.1** — Per-App Proxy
5. **2.2** — Always-On VPN
6. **2.3** — GeoIP/GeoSite
7. **2.4** — Import/Export
8. Остальное по необходимости
