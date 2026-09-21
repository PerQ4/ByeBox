# ByeBox Release Process

Короткая инструкция, как выпускать релизы в новой схеме (см. `docs/version_naming_policy.md`).
Главное правило: **апдейтер сравнивает версии по `versionCode`**, поэтому каждый релиз обязан
нести машиночитаемый `versionCode`.

## 0. Подготовка версии

1. Открой `version.properties` в корне.
2. Подними `VERSION_CODE` на **+1** (обязательно).
3. При необходимости поменяй `VERSION_MAJOR` / `VERSION_MINOR` / `VERSION_PATCH`.
4. Для пре-релиза задай `VERSION_STAGE` (`alpha` | `beta` | `rc`) и `VERSION_STAGE_NUMBER`.
   Для стабильного релиза `VERSION_STAGE` пустой, `VERSION_STAGE_NUMBER=0`.
5. `versionName` собирается автоматически: `MAJOR.MINOR.PATCH[-STAGE.N]`.

Проверить, что получилось:

```bash
./gradlew :app:assembleRelease
grep -E "VERSION_NAME|VERSION_CODE" \
  app/build/generated/source/buildConfig/release/com/perqa/byebox/BuildConfig.java
```

## 1. Сборка APK

```bash
./gradlew :app:assembleRelease
```

Артефакты:

- `app/build/outputs/apk/release/byebox-universal-release.apk` — **основной** ассет для GitHub.
- `app/build/outputs/apk/release/byebox-arm64-v8a-release.apk` — байт-идентичный ABI-сплит.

## 2. Публикация на GitHub

Нужен токен с правами на **Contents** (создание релизов и загрузка ассетов).

```bash
TAG="v1.2.0"                       # = v + versionName из version.properties
CODE=21                            # = VERSION_CODE
TOKEN="<ваш PAT>"

# 2.1. Тело релиза. Маркер с versionCode — обязателен для апдейтера!
BODY="$(cat <<EOF
### ByeBox 1.2.0

- Что нового пункт 1
- Что нового пункт 2

<!-- byebox:versionCode=\${CODE} -->
EOF
)"

# 2.2. Создать релиз (для пре-релиза добавьте \"prerelease\": true)
curl -sS -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/PerQ4/ByeBox/releases \
  -d "{\"tag_name\":\"$TAG\",\"name\":\"ByeBox 1.2.0 ($CODE)\",\"body\":$(python3 -c 'import json,sys;print(json.dumps(sys.stdin.read()))' <<<"$BODY"),\"prerelease\":false}"

# 2.3. Загрузить APK в созданный релиз (подставьте upload_url из ответа)
UPLOAD_URL="https://uploads.github.com/repos/PerQ4/ByeBox/releases/<ID>/assets?name=byebox-universal-release.apk"
curl -sS -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary @app/build/outputs/apk/release/byebox-universal-release.apk \
  "$UPLOAD_URL"
```

> GitHub также кладёт в ассет поле `digest` (`sha256:...`) — апдейтер использует его для
> проверки целостности скачанного APK.

## 3. Чек-лист

- [ ] `VERSION_CODE` увеличен на 1 и уникален среди релизов.
- [ ] Тег `v<versionName>` совпадает с версией в APK.
- [ ] В описании релиза есть строка `<!-- byebox:versionCode=N -->`.
- [ ] Для alpha/beta/rc выставлен флаг **pre-release**.
- [ ] На релиз загружен `byebox-universal-release.apk`.
- [ ] Блок версии в `README.md` (Version / Build / Released / Download) обновлён.

## 4. Важно про переход 9.0 → 1.2.0

Старый апдейтер (в сборке `9.0`) сравнивал версии по имени, поэтому переход на `1.2.0`
он не увидит — это разовый ручной апдейт/переустановка. Все последующие релизы
(от `1.2.x` и выше) новый апдейтер находит корректно.
