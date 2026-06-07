# ByeBox app size audit

## Current findings

- Previous universal release APK: about 271 MB.
- After removing duplicate asset binaries and adding ABI splits:
  - `app-arm64-v8a-release-unsigned.apk`: about 61 MB.
  - `app-armeabi-v7a-release-unsigned.apk`: about 57 MB.
  - `app-x86_64-release-unsigned.apk`: about 64 MB.
  - `app-universal-release-unsigned.apk`: about 167 MB.
- Main weight sources:
  - `libbox.so` for four ABIs: about 210 MB uncompressed in APK.
  - Legacy `assets/sing-box/*/sing-box` binaries: about 167 MB in sources and about 53 MB compressed in APK.
  - Kotlin/Compose dex payload: about 7-8 MB compressed.
- The service currently starts VPN through `BoxService`/`libbox`, so asset `sing-box` binaries were duplicate runtime payload.

## Changes started

- Removed legacy asset `sing-box` binaries from packaging.
- Changed the `checkSingBox` Gradle task into a report-only legacy-assets check.
- Added ABI split APKs, matching Hiddify's release approach.
- Excluded old 32-bit `x86` from packaged release ABIs while keeping `arm64-v8a`, `armeabi-v7a`, and `x86_64`.

## Expected result

- Universal APK becomes smaller because duplicate asset binaries and `x86` native libs are no longer packaged.
- Phone-specific APKs become much smaller because each split contains only one `libbox.so`.

## Next candidates

- Build or obtain a stripped/reduced `libbox.aar` with only required sing-box features.
- Publish/install ABI-specific APK or AAB instead of universal APK for normal users.
- Enable R8/resource shrinking after adding keep rules for `io.nekohasekai.libbox`.
- Remove duplicated `src/main/jniLibs` if the AAR is the single source of native libraries.
