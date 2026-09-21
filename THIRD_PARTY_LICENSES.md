# Third-Party Licenses & Attributions

ByeBox is distributed under the **GNU General Public License v3.0** (see [LICENSE](LICENSE)).
It would not exist without the open-source projects below. Their code, libraries and
build tooling are used under the terms described here.

> If you redistribute ByeBox (source or binary), keep this file and the upstream
> license notices, and make the corresponding source available as required by the
> applicable licenses (notably **GPL-3.0** and **MPL-2.0**).

---

## 1. Source code reused in ByeBox

| Component | License | Upstream | How it is used |
|-----------|---------|----------|----------------|
| **v2rayNG** | GPL-3.0 | https://github.com/2dust/v2rayNG | Connection management, protocol parsing, format (`fmt`), MMKV handlers and utilities, packaged under `com.v2ray.ang`. ByeBox is a **derivative work** of v2rayNG. |
| **TG WS Proxy for Android** | GPL-3.0 | https://github.com/amurcanov/tg-ws-proxy-android | Local Telegram MTProto proxy, linked as the `:tgwsproxy` Gradle module (Rust + Kotlin). |
| **tg-ws-proxy** (original) | MIT | https://github.com/Flowseal/tg-ws-proxy | Upstream of the Android fork above. |

Because ByeBox includes GPL-3.0 code (v2rayNG), the project as a whole is licensed
under **GPL-3.0**. The complete corresponding source of every GPL-covered component
is available at the links above (and in this repository).

---

## 2. Bundled binaries / native code

| Component | License | Upstream | How it is used |
|-----------|---------|----------|----------------|
| **Xray-core** | MPL-2.0 | https://github.com/XTLS/Xray-core | Core proxy engine, bundled as `app/libs/libv2ray.aar` (built with gomobile). |
| **hev-socks5-tunnel** | MIT | https://github.com/heiher/hev-socks5-tunnel | `libhev-socks5-tunnel.so` — the *HevTun* (tun2socks) mode. |
| **gomobile / golang.org/x/mobile** | BSD-3-Clause | https://github.com/golang/mobile | Tooling used to generate the Xray AAR wrapper (see `gomobile-patch/`). |
| **MMKV** | BSD-3-Clause | https://github.com/Tencent/MMKV | Fast key-value storage (`mmkv-static`). |

Xray-core is licensed under the **Mozilla Public License 2.0**. The MPL-covered
sources are available at the upstream link above.

---

## 3. Runtime libraries

| Component | License | Upstream |
|-----------|---------|----------|
| AndroidX — Core, Activity, Lifecycle, Navigation 3, Compose (UI / Material 3), DataStore, WorkManager, Preference, Multidex | Apache-2.0 | https://github.com/androidx |
| CameraX | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/camera |
| Kotlin standard library, kotlinx.coroutines, kotlinx.serialization | Apache-2.0 | https://github.com/JetBrains/kotlin |
| OkHttp | Apache-2.0 | https://github.com/square/okhttp |
| Gson | Apache-2.0 | https://github.com/google/gson |
| ZXing ("Zebra Crossing") | Apache-2.0 | https://github.com/zxing/zxing |
| Haze (blur/glassmorphism) | Apache-2.0 | https://github.com/chrisbanes/haze |
| ML Kit Barcode Scanning | Proprietary (Google APIs Terms of Service) | https://developers.google.com/ml-kit |

---

## 4. Fonts, icons and artwork

- UI icons are provided by the **Material Symbols / Material Icons** set (Apache-2.0).
- Application name, logo and visual assets are original to ByeBox unless stated otherwise.

---

## 5. Trademarks

Telegram, Android, Google and other names are trademarks of their respective owners.
ByeBox is an independent project and is **not** affiliated with or endorsed by any of them.

---

*If you believe a component is missing or mis-attributed, please open an issue.*
