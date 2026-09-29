# Архитектура ByeBox

> Документ описывает, как устроено приложение: модули, слои, состояние, хранилище
> и интеграцию с ядром Xray. Читать, если нужно понять, куда класть новый код или
> почему что-то работает именно так.

Проект — форк [v2rayNG](https://github.com/2dust/v2rayNG): унаследованные пакеты
`com.v2ray.ang` (протоколы, конфиги, сервисы) сохранены почти без изменений, а весь
пользовательский интерфейс, тема и продуктовая логика написаны заново в
`com.perqa.byebox` на Jetpack Compose.

## 1. Модули

Gradle-модулей два, оба собираются вместе (см. `settings.gradle.kts`):

| Модуль | Тип | Что это |
|---|---|---|
| `app` | application | Основное приложение: UI, конфиги, VPN-сервис, ядро Xray |
| `tgwsproxy` | android library | Telegram WS-прокси (Rust + gomobile), клонируется отдельно |

`tgwsproxy` — **не** часть основного репозитория: каталог `tgwsproxy/` в `.gitignore`,
его нужно склонировать отдельно, иначе `settings.gradle.kts` не отрезолвится.
См. раздел Build в `README.md`.

Модуль `tgwsproxy` устроен в три слоя:

- `tgwsproxy/src/*.rs` — ядро прокси на Rust (`cdylib`, зависит от `tokio`,
  `rustls`, `ring`, `aes`, `sha2`). Собирается в `libtgwsproxy.so`.
- `tgwsproxy/app/src/main/java/com/amurcanov/tgwsproxy/` — Android-обвязка: сервис
  (`ProxyTileService`), настройки, собственные экраны на Compose, локализация
  через `TProxyLocale`.
- `TgWsShared.kt` / `TgWsProxyUi.kt` в `app` — точка входа в TGWS из основного UI.

Prebuilt `.so` лежат в `tgwsproxy/app/src/main/jniLibs/` (только `arm64-v8a`
и `armeabi-v7a`), поэтому обычная сборка `./gradlew :app:assembleRelease` Rust
не трогает — пересборка нужна только при правке `*.rs`.

## 2. Слои исходников `app`

### 2.1 `com.perqa.byebox` — наш код

| Пакет | Файлов | Ответственность |
|---|---|---|
| `ui/main` | 19 | Экраны, диалоги, компоненты, локализация, ViewModel |
| `ui/main/dashboard` | 8 | Главный экран: кнопка подключения, карточки статуса, быстрые действия |
| `core` | 12 | Логирование, пинг, трафик, обновление, бэкап, генерация Xray-конфига |
| `data` | 5 | Модели (`ProxyConfig`), подписки, пресеты, ключи настроек |
| `service` | 3 | Плитки быстрых настроек, уведомление VPN |
| `theme` | 3 | Цвета, типографика, тема Material 3 |

### 2.2 `com.v2ray.ang` — унаследованное от v2rayNG

| Пакет | Файлов | Ответственность |
|---|---|---|
| `handler` | 8 | `AngConfigManager` (импорт/экспорт/подписки), `MmkvManager`, `SettingsManager` |
| `dto`, `dto/entities` | 14+9 | Сериализуемые модели ядра (`ProfileItem`, `V2rayConfig`, `SubscriptionItem`) |
| `service` | 9 | `CoreVpnService`, `CoreProxyOnlyService`, dialer-сервисы, `ProcessService` |
| `core` | 5 | Сборка конфига ядра, `CoreNativeManager`, `CoreServiceManager` |
| `fmt` | 9 | Парсеры и сериализаторы ссылок (`VlessFmt`, `VmessFmt`, `TrojanFmt`, …) |
| `util` | 11 | Логи, утилиты, расширения |
| `enums` | 9 | `EConfigType`, `NetworkType`, `Language`, `RoutingType` |
| `helper`, `receiver`, `contracts`, `extension` | 12 | Вспомогательное: пинги, `BootReceiver`, интерфейсы сервисов |

**Правило:** не переписываем `com.v2ray.ang`, а вызываем его из `com.perqa.byebox`.
Например UI не строит ссылки сам, а зовёт `AngConfigManager.getShareLink(guid)`.

## 3. Точки входа

Объявлены в `AndroidManifest.xml`:

| Компонент | Класс | Роль |
|---|---|---|
| Application | `ByeBoxApplication` | `MultiDexApplication` + `Configuration.Provider` для WorkManager |
| Activity | `MainActivity` | Единственная Compose-Activity, три вкладки |
| Activity | `QrScanActivity` | Сканирование QR с камеры (ZXing + ML Kit) |
| Service | `CoreVpnService` | Основной foreground-сервис VPN-туннеля |
| Service | `CoreProxyOnlyService` | Режим «только прокси», без VPN |
| Service | `ByeBoxTileService`, `ByeBoxProfileTileService` | Плитки быстрых настроек |
| Receiver | `BootReceiver` | Автозапуск при `BOOT_COMPLETED` |

`ByeBoxApplication.onCreate()` делает четыре вещи, порядок важен:

1. `MMKV.initialize(this)` — без этого нет хранилища.
2. `SettingsManager.initApp` + `initAssets` — дефолтные значения и загрузка geo-ассетов.
3. Выключает `PREF_USE_HEV_TUNNEL`, чтобы `CoreVpnService` поднимал нативный TIN.
4. `TgwsLang.sync(this)` — язык из настроек приложения применяется к строкам TGWS.

`WorkManagerInitializer` в манифесте отключён (мультипроцессная конфигурация), поэтому
приложение само отдаёт `workManagerConfiguration` — иначе любой вызов WorkManager
в основном процессе падал бы с `IllegalStateException`.

## 4. UI и состояние

### 4.1 Один Activity

`MainActivity` хостит `MainScreen`, внутри — нижняя навигация по вкладкам. Порядок и
состав вкладок настраиваются пользователем (`pref_tab_order`, `pref_default_tab_id`)
и читаются из DataStore. Базовый набор: `main` (Главная), `proxies` (Прокси),
`settings` (Настройки); часть вкладок скрывается в зависимости от `pref_app_mode`.

### 4.2 ViewModel и единое состояние

`MainScreenViewModel` — единственный крупный ViewModel приложения. Он держит
~40 отдельных `MutableStateFlow`, которые затем **склеиваются в один** `MainUiState`:

```
_configs ─┐
_subscriptionSources ─┤
_activeConfigId ─┤
_connectionStatus ─┼── combine(...) ──▶ MainUiState ──▶ collectAsStateWithLifecycle()
_downloadSpeed ─┤                            │
_vpnModeEnabled ─┘                            └──▶ UI перерисовывается целиком
```

`MainUiState` (в `MainScreenViewModel.kt`) — это `@Immutable data class` примерно
из 60 полей: список конфигов, активный конфиг, статус соединения, скорости, тема,
профиль маршрутизации, DNS, tun-стек, флаги фич и т.д. Такой «плоский» подход
означает, что любое изменение перерисовывает нужные экраны реактивно, без ручного
кэширования состояния в composable-функциях.

Записи в хранилище идут **мимо** потока состояния: сеттер вида
`_vpnModeEnabled.value = enabled` попутно вызывает
`MmkvManager.encodeSettings(AppConfig.PREF_MODE, ...)`. То есть ViewModel —
единая точка, где «состояние экрана» и «состояние на диске» расходятся.

### 4.3 Локализация

Отдельного ресурсного подхода нет: `Loc.kt` (2231 строка) хранит три словаря
(`ru`, `en`, `zh`) как `Map<String, String>`. Обращения вида `Loc.get("share", language)`.
При добавлении строки правится **все три** словаря — забытый ключ даёт пустой текст,
а не ошибку компиляции. У TGWS-слоя своя локаль: `TProxyLocale.wrap(context)`.

## 5. Хранилище

Две независимые системы, разделённые по ответственности:

| Система | Что хранит | Где |
|---|---|---|
| **MMKV** (`com.tencent:mmkv`) | Настройки ядра, профили v2rayNG, конфиги, подписки | Унаследованное от v2rayNG, доступ из нативного кода и воркеров |
| **SharedPreferences** | Продуктовые настройки ByeBox: порядок вкладок, режим приложения, онбординг, периодический пинг, тема, оформление | файл `byebox_settings`, читается только Kotlin-кодом приложения |

Зависимость `androidx.datastore.preferences` подключена в `build.gradle.kts`, но
в коде не используется — вся продуктовая обвязка идёт через
`appContext.getSharedPreferences("byebox_settings", MODE_PRIVATE)`. Это стоит
учитывать: искать настройки в DataStore бесполезно, их там нет.

Основной доступ к MMKV идёт через два объекта-шлюза:

- `MmkvManager` — CRUD над сущностями: `decodeServerConfig(guid)`,
  `encodeServerConfig(guid, config)`, `decodeSubscriptions()`, `decodeProfile()`.
  Внутри — JSON через Gson.
- `SettingsManager` — строковые/булевы настройки, миграции
  (`migrateHysteria2PinSHA256`, `migrateServerListToSubscriptions`),
  `ensureDefaultSettings()` при первом запуске.

MMKV открыт в `MULTI_PROCESS_MODE` — это нужно, потому к настройкам лезут
`RemoteWorkManagerService` и сервисы в отдельных процессах.

## 6. Ядро Xray

### 6.1 Упаковка

Ядро — Go-код Xray-core, собранный в Android-библиотеку через **gomobile**:
исходники и пропатченный gomobile лежат в `gomobile-patch/`, готовый артефакт —
`app/libs/libv2ray.aar` (55 МБ). Он даёт Java-классы `libv2ray.Libv2ray`,
`libv2ray.CoreController`, `libv2ray.CoreCallbackHandler`, а также пакет `go.Seq`
(рантайм gomobile). Пересобирать `.aar` нужно только при смене версии ядра —
обычная сборка приложения его не трогает.

> **Единственное активное ядро — Xray.** Раньше в проекте параллельно жил sing-box:
> `app/libs/libbox.aar`, `app/src/main/jniLibs/*/libbox.so` и
> `app/src/main/assets/sing-box/*/sing-box` для четырёх ABI. Из HEAD они удалены
> (в истории git они ещё лежат, поэтому `.git` занимает ~264 МБ). Кода, который
> грузил бы `libbox` или запускал `sing-box`, в проекте нет — `System.loadLibrary`
> вызывается только для `hev-socks5-tunnel`. Схема `sing-box://` в списке
> deep-link'ов `MainActivity` — это разбор ссылки на конфиг, а не запуск ядра.

### 6.2 Оборачивание

Прямой вызов натива наружу не торчит — всё проходит через тонкий синглтон:

```kotlin
object CoreNativeManager {           // app/src/main/java/com/v2ray/ang/core/
    fun initCoreEnv(context)          // Seq.setContext + Libv2ray.initCoreEnv
    fun getLibVersion(): String
    fun measureOutboundDelay(config, url): Long
    fun newCoreController(handler): CoreController
    fun reconcileBrowserDialer(addr)
}
```

`initCoreEnv` защищён `AtomicBoolean` — инициализация окружения гоняется только один
раз, при ошибке флаг сбрасывается, чтобы попытка могла повториться.

### 6.3 Цепочка запуска

Старт и остановку инициирует `MainActivity` (кнопка подключения на дашборде),
а не ViewModel:

```
MainActivity → toggleConnection()
  └─▶ CoreServiceManager.startVService(context, guid)
        ├─▶ CoreConfigManager.getV2rayConfig(context, guid)   // JSON для ядра
        │     ├─▶ XrayConfigGenerator.generate(...)           // наш код
        │     └─▶ AngConfigManager → ProfileItem              // унаследованное
        └─▶ старт foreground-сервиса CoreVpnService
              └─▶ CoreServiceManager.startCoreLoop(pfd)
                    └─▶ CoreNativeManager.newCoreController(handler)
                          └─▶ Libv2ray (Go) читает/пишет TUN
```

`MainScreenViewModel` в этой цепочке участвует только наблюдателем — он читает
`CoreServiceManager.isRunning()` и отражает состояние в `_connectionStatus`.

Тут же живёт `CoreConfigManager` (сборка финального JSON: логи, DNS, TUN-inbound,
outbound-цепочка, `CoreOutboundBuilder`), `CoreConfigContextBuilder` и
`XrayConfigGenerator` — наш генератор конфига, который умеет VLESS/VMess/Trojan/
Shadowsocks, reality, `xtls-rprx-vision`, TLS, WebSocket, gRPC.

### 6.4 Два режима работы

`PREF_MODE` (`"pref_mode"`) переключает, какой сервис поднимается:

- `"VPN"` → `CoreVpnService`, нативный TUN-инбаунд, перехват всего трафика.
- `"PROXY"` → `CoreProxyOnlyService`, локальный SOCKS-порт, без перехвата.

### 6.5 HevTunnel и настройка tunStack

В `ByeBoxApplication.onCreate()` принудительно выставляется
`PREF_USE_HEV_TUNNEL = false`: иначе v2rayNG поднял бы туннель через
HevSocks5Tunnel вместо нативного TUN Xray.

Отсюда важный нюанс: в UI есть настройка «стек TUN» со значением `gvisor`
(`MainUiState.tunStack`, дефолт `TunStack.GVISOR`, прокидывается в
`XrayConfigGenerator` как строка `"gvisor"`), и `ProfilePresetManager` маппит
`GVISOR → PREF_USE_HEV_TUNNEL = true`. То есть UI-значение и фактическое
поведение расходятся: выбор gvisor в настройках перезаписывается при старте
приложения на `false`. Если будете чинить эту настройку — начинать надо здесь.

Библиотека `libhev-socks5-tunnel.so` в `app/src/main/jniLibs/` остаётся как
fallback; при неудачной загрузке `isHevLibAvailable()` пишет `false` и
переключается на TUN ядра.

## 7. Форматирование и обмен конфигами

Отдельный слой `com.v2ray.ang.fmt` — по классу на протокол
(`VlessFmt`, `VmessFmt`, `TrojanFmt`, `ShadowsocksFmt`, `Hysteria2Fmt`,
`WireguardFmt`, `SocksFmt`, `CustomFmt`). У каждого свой `toUri` / парсер.

Правило обмена ссылками: **источник истины — `AngConfigManager.getShareLink(guid)`**,
он строит ссылку штатным форматтером (`EConfigType` → `XxxFmt.toUri`).
`ProxyConfig.toConfigLink()` — самодельный генератор, оставлен как fallback:
он нужен для виртуальных пресетов, которых нет в хранилище, и для CUSTOM-конфигов,
которые штатный экспортёр отдаёт пустым. UI вызывает единую функцию:

```kotlin
internal fun ProxyConfig.effectiveShareLink(): String =
    AngConfigManager.getShareLink(id).ifBlank { toConfigLink() }
```

Пустой результат (CUSTOM/HTTP) — это не ошибка, а ожидаемый исход: UI показывает
сообщение `share_empty`, а не битый QR-код.

Две модели конфига живут параллельно и намеренно не слиты:

- `ProxyConfig` (`com.perqa.byebox.data`) — плоская, для UI и пресетов: `protocol`,
  `address`, `port`, `uuid`, `sni`, `pbk`, `sid`, `wsPath`, `grpcServiceName`.
- `ProfileItem` (`com.v2ray.ang.dto.entities`) — сложная, вложенная, для ядра
  (`configType`, `vnext`, `streamSettings`, reality и т.д.).

## 8. Фоновые задачи

WorkManager подключён в мультипроцессном режиме (`work-multiprocess`,
`RemoteWorkManagerService` в манифесте) — это нужно, чтобы воркеры из подписок
и апдейтов не конфликтовали с записью настроек в MMKV из основного процесса.

Воркеров в коде два:

- `SubscriptionUpdater` — обновление подписок; сам наследует `CoroutineWorker`
  и ставит себя через `RemoteWorkManager.getInstance(context)`.
- `UpdateCheckScheduler` — периодическая проверка апдейтов
  (`PeriodicWorkRequestBuilder`, `enqueueUniquePeriodicWork` с
  `ExistingPeriodicWorkPolicy.KEEP`, чтобы не плодить дубли).

`UpdateDownloader` — обычный `suspend`-класс, а не воркер: его зовёт UI-слой,
он качает APK и вызывает `install(context, file)`.

`RealPingWorkerService` из v2rayNG — это foreground-сервис пинга, а не WorkManager-воркер;
в текущей кодовой базе пинг конфигов инициируется из UI.

`UpdateChecker` разбирает тело релиза по маркеру `<!-- byebox:versionCode=N -->`
и сравнивает версии **по `versionCode`**, попутно отбрасывая пре-релизы, если
пользователь не подписан на канал STAGE. Формат версий и публикация — см.
`docs/version_naming_policy.md` и `docs/release_process.md`.

## 9. Прочее

- **QR:** ZXing для генерации (`QRCodeWriter` в `QrCodeDialog`), ML Kit Barcode
  Scanning для распознавания (`QrScanActivity`).
- **Пинг:** `PingProbe` — `probeConfigs()` / `probeConfigLatency()` через
  `measureOutboundDelay` ядра, плюс `probeTcpLatency()` как дешёвый запасной вариант.
- **Бэкап настроек:** `SettingsBackup` — zip-архив каталога настроек,
  `exportToDownloads()` / `importFromUri()`. Отдельно `WebDavManager` (унаследованный)
  умеет выгружать/забирать этот архив на WebDAV.
- **Форматирование:** `JsonConfigEditorDialog` — просмотр/правка сырого JSON конфига.
- **Профили настроек:** `ProfilePresetManager` — именованные пресеты с
  `applyProfile`, `switchActiveProfile`, `reconcileAssignments`.
- **Сборка:** `version.properties` → `BuildConfig`; release с `isMinifyEnabled = true`
  и правилами в `app/proguard-rules.pro`: keep для `go.**` / `libv2ray.**`
  (Go-мост gomobile), DTO `com.v2ray.ang.dto.**` (Gson создаёт их рефлексией),
  `androidx.work.impl.**`, `com.google.mlkit.**` (регистры через
  ComponentDiscovery) и `Serializable`. Правило для Room нужно, потому что Room
  приходит транзитивно вместе с WorkManager, и его сгенерированные `*_Impl`-классы
  создаются рефлексией — самого Room в коде приложения нет.
- **Тесты:** `app/src/test` — 6 файлов, в основном на `XrayConfigGenerator`,
  `ProxyConfig.toConfigLink`, round-trip форматтеров, `SemVer`, пресеты.

## 10. Известные расхождения

То, что стоит держать в голове при работе с кодом:

1. **`tunStack = gvisor` не работает как задумано.** UI пишет
   `PREF_USE_HEV_TUNNEL = true`, а `ByeBoxApplication` на старте сбрасывает его в
   `false`. Подробнее — 6.5.
2. **`androidx.datastore.preferences` подключён, но не используется.** Продуктовые
   настройки лежат в `SharedPreferences("byebox_settings")`.
3. **Sing-box в репозитории не активен.** Его бинарники и AAR убраны из HEAD,
   но остались в истории git (отсюда ~264 МБ в `.git`) — см. 6.1.
4. **Две модели конфига.** `ProxyConfig` и `ProfileItem` не синхронизируются
   автоматически; конвертация живёт в `AngConfigManager` — см. 7.
