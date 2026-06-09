# ByeBox MD3E design guidelines

Дата ревизии: 2026-06-09

## 1. Назначение

ByeBox должен ощущаться как современный нативный Android VPN-клиент: быстрый, плотный, понятный, с динамическими цветами системы и выразительными, но не декоративными Material 3 Expressive-паттернами.

MD3E для ByeBox не означает "много карточек, много скруглений и много всплывашек". Для VPN-клиента важнее:

- быстро понять состояние VPN;
- быстро выбрать рабочий узел;
- быстро увидеть источник, ping, протокол, транспорт и ошибку;
- не терять контент под плавающей навигацией;
- не видеть заявленных настроек, у которых нет backend-эффекта.

## 2. Источники

Основные источники для сверки:

- Material 3 components: https://m3.material.io/components
- Material 3 lists guidelines: https://m3.material.io/components/lists/guidelines
- Jetpack Compose Material components overview: https://developer.android.com/develop/ui/compose/components
- Compose Material 3 design system: https://developer.android.com/develop/ui/compose/designsystems/material3
- Compose app bars: https://developer.android.com/develop/ui/compose/components/app-bars
- Compose navigation bar: https://developer.android.com/develop/ui/compose/components/navigation-bar
- Compose Material insets: https://developer.android.com/develop/ui/compose/system/material-insets
- Compose Material 3 release notes for current component behavior: https://developer.android.com/jetpack/androidx/releases/compose-material3

Официальный сайт M3 рендерит guideline-страницы через JavaScript, поэтому этот документ фиксирует правила на уровне компонентной системы, а для Android-реализации опирается на актуальные Compose Material 3 API и официальный Android guidance.

## 3. Текущее состояние и главные проблемы

Проверенный код: `app/src/main/java/com/perqa/byebox/ui/main/MainScreen.kt`, `app/src/main/java/com/perqa/byebox/theme/*`.

### Что уже близко к MD3E

- Используется `MaterialTheme.colorScheme`, есть поддержка dynamic color.
- Нижняя навигация реализована как кастомная floating pill, а не стандартная full-width панель.
- Экран прокси уже использует группировку по источникам и sticky headers.
- У элементов прокси есть swipe actions, хаптик и частично прогрессивное скругление.
- Есть попытка tonal separation через `surfaceContainer`, `surfaceContainerHigh`, `primaryContainer`, `tertiaryContainer`.

### Что сейчас не соответствует

- Слишком много `Card` как универсального контейнера. В M3 lists список должен быть непрерывной вертикальной структурой однородных строк, а не набором самостоятельных карточек для каждого простого item.
- Ручные формы 4dp/6dp/28dp используются непоследовательно. Из-за этого группировки выглядят поломанными, особенно в списках прокси и настройках.
- В настройках многие группы похожи на вложенные карточки внутри карточек. Для M3 это визуально тяжелее, чем grouped list с tonal rows.
- В некоторых местах border используется как основной разделитель. Для MD3E предпочтительнее тональная иерархия, spacing, shape и state layer.
- Нижний остров должен быть самостоятельным floating element. Любая прямоугольная подложка, даже полупрозрачная, визуально отменяет "остров".
- Контент должен прокручиваться за островом, но иметь достаточный bottom padding, чтобы последний элемент можно было вытащить выше навигации.
- Toast/snackbar используются слишком часто для штатных действий. M3 snackbars предназначены для коротких важных обновлений процесса, а не для каждого клика.
- Списки прокси при 40+ узлах должны оставаться плавными: минимизировать recomposition, тяжелые `animate*AsState`, nested Column внутри sticky header, лишние shadows и бесконечные анимации.
- В строках конфигов сейчас смешаны действия, данные и декоративность. Для списка нужен читаемый one/two-line row: leading, headline, supporting, trailing.

## 4. Общие правила MD3E для ByeBox

1. Сначала нативность, потом выразительность.
2. Dynamic color является основой. Ручные темы Slate/Desert/Sage допустимы только как fallback/presets.
3. Рядом стоящие крупные элементы не должны иметь одинаковый container color.
4. Border не является стандартным способом группировки. Использовать border только для focus, error, selected outline или редкого high-emphasis состояния.
5. Поверхности различаются уровнем: `surface`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`.
6. Любой floating element должен быть визуально отделен от layout: вокруг виден фон/контент, нет родительской прямоугольной панели.
7. Большой текст только для экранных заголовков. Внутри cards/lists использовать compact type scale.
8. В списках не должно быть постоянных destructive buttons на каждой строке. Удаление: swipe в одну сторону, overflow, confirm при необходимости.
9. Штатный результат действия показывать inline: progress, badge, status chip, updated time. Toast только для ошибки или редкого подтверждения.
10. Все видимые настройки должны иметь backend-эффект. Если backend не готов, настройка скрывается или помечается disabled с честным supporting text.

## 5. Цвет и контраст

### Семантические роли

- App background: `colorScheme.background`.
- Main large panel: `surfaceContainerLow` или `surfaceContainer`.
- Group header/source header: `surfaceContainer`.
- Ordinary list row: `surfaceContainerLow` или прозрачный row на grouped surface.
- Active VPN/config row: `primaryContainer` с `onPrimaryContainer`.
- Secondary action: `secondaryContainer`.
- Test/ping/action accent: `tertiaryContainer`.
- Delete/destructive: `errorContainer`.
- Floating nav island: `surfaceContainerHigh` или `surfaceContainerHighest`.
- Bottom fade: transparent to `background` alpha 0.20-0.35, высота 20-32dp.

### Запреты

- Не ставить рядом две большие поверхности одного тона.
- Не красить весь экран одной цветовой семьей, особенно в dark theme.
- Не использовать `primary` как заливку для больших блоков, кроме явного connected/active состояния.
- Не использовать `onSurface.copy(alpha < 0.38f)` для важного текста, ping или endpoint, если это ухудшает читаемость.

## 6. Формы

Использовать стабильную шкалу форм, а не случайные значения.

- Full screen/page background: без shape.
- Large section container: 28dp.
- Grouped list container: 24-28dp outer, 4-8dp inner connected corners.
- List row standalone: 20-24dp.
- List row inside connected group:
  - first: top 24dp, bottom 4-8dp;
  - middle: 4-8dp;
  - last: top 4-8dp, bottom 24dp.
- Floating nav island: capsule 28-32dp, высота 52-58dp.
- Active nav item: capsule 20-24dp.
- Chips: 12-16dp.
- Text fields/search fields: 18-24dp, filled tonal container, outline только focus/error.
- FAB/context action: круг или expressive rounded square 20-28dp.

Важно: если элемент "отрывается" свайпом, его собственное скругление растет только в процессе отрыва. Соседние элементы слегка увеличивают corner radius и получают минимальный follow-offset.

## 7. Motion и хаптик

### Motion tokens

- Button press: scale 0.96-0.98, down 80-120ms, up 160-220ms.
- Nav selection: width + color + shape morph 220-280ms.
- Screen/tab switch: fade + shared axis X, 180-240ms.
- Sticky header collapse: height, alpha, shape 180-260ms, без резкого snap-back.
- List reorder/filter: `animateItemPlacement` или equivalent placement animation, 180-260ms.
- Swipe detach:
  - 0-14dp: resistance, item двигается примерно на 45-55% пальца;
  - 14-58dp: быстрый detach curve;
  - после detach: item следует за пальцем почти 1:1;
  - threshold 120-150dp: haptic tick;
  - release below threshold: spring/tween back 180-240ms;
  - release over threshold left: delete;
  - release over threshold right: open config settings.

### Хаптик

- Легкий tick: tab/nav/select row.
- Небольшой confirm tick: threshold reached, connect/disconnect, ping all started.
- Не использовать грубую вибрацию на каждом drag frame.
- На detach start нужен отдельный короткий feedback, но только один раз за gesture.

### Запреты

- Бесконечные декоративные анимации в больших списках.
- Запаздывающие corner animations, которые продолжаются после окончания жеста.
- Массовые recomposition при scroll из-за изменения глобальных mutable states на каждый пиксель.

## 8. Navigation и edge-to-edge

### Нижний остров

Требования:

- самостоятельный `Surface`/`Box` поверх контента;
- нет full-width parent panel;
- непрозрачная pill, чтобы текст под ней не просвечивал;
- по бокам и сверху видно контент/фон;
- расположен близко к gesture bar, но не перекрывает системную область;
- bottom fade рисуется отдельно и очень мягко;
- контент получает bottom padding: `navHeight + gestureInset + 32-48dp`.

Текущий риск: `BottomEdgeFade` и layout bottom padding могут выглядеть как прямоугольная подложка. Fade должен быть фоном края экрана, а не контейнером навигации.

### Секции навигации

Текущие разделы допустимы:

- Главная;
- Прокси;
- Настройки;
- Логи.

Если логи доступны снизу, не дублировать кнопку логов в top area.

## 9. Lists guidelines для ByeBox

M3 lists предназначены для вертикальных наборов однородных элементов. Основной паттерн ByeBox: grouped tonal list, а не card-per-row.

### Анатомия row

Каждая строка должна иметь:

- leading: flag/avatar/icon/status dot;
- headline: основное имя;
- supporting text: endpoint, package, protocol stack, description;
- trailing: ping, switch, check, overflow или одна главная action;
- state layer: pressed/selected/dragged;
- минимум 48dp touch target.

### Высоты

- One-line row: 56dp.
- Two-line row: 64-72dp.
- Three-line row: 88dp, только когда реально нужны три строки.
- Config row ByeBox: целевой диапазон 64-76dp.
- Settings row: 64-80dp в зависимости от supporting text.

### Разделение

- Предпочтение: tonal rows + spacing 2-6dp внутри группы.
- Dividers использовать редко, в основном inset divider после leading keyline.
- Не делать толстые gaps между каждым item в длинном списке.
- Не делать nested cards в каждой строке.

### Sticky headers

- На экране Прокси главный блок "Конфигурации" остается сверху и схлопывается.
- Header текущей подписки остается sticky при прокрутке ее узлов.
- Sticky header должен менять высоту/форму плавно и не дергаться при возвращении.
- Sticky header не должен содержать полный набор тяжелых controls в collapsed state.

### Производительность списков

- `LazyColumn` с `key` и `contentType` обязателен.
- Не рендерить configs внутри `SourceGroupCard`, если они уже рендерятся LazyColumn item-ами.
- Для 40/100/500 configs проверять scroll jank.
- Вынести derived summaries (`protocolSummary`, `endpointSummary`, flags, source counters) в memoized/derived data.
- Не писать в общие states на каждый drag pixel, если можно держать gesture state локально.

## 10. Component audit against Material components

### App bars

Material guidance: top app bars дают доступ к ключевым задачам и информации; bottom app bars/navigation используются для нижних действий и навигации.

ByeBox:

- Верхняя область главной сейчас скорее brand/status header, а не TopAppBar.
- При scroll верхний статус должен схлопываться до компактного centered state text: "Отключено", "Подключено", "Подключение".
- Не держать большую верхнюю плашку, если пользователь читает список.

### Navigation bar

Material guidance: navigation bar для 3-5 постоянных destinations на compact devices.

ByeBox:

- 4 destinations подходят.
- Floating island допустим как MD3E interpretation, но должен соблюдать edge-to-edge.
- Active item может расширяться, inactive можно делать icon-only при нехватке места.

### Buttons

ByeBox:

- Primary action на главной: connect/disconnect.
- Secondary actions: refresh, ping, share, VPN Android.
- В compact toolbars использовать icon buttons с tooltip/contentDescription.
- Текстовые кнопки с длинным русским label не сжимать до нечитаемости. Если не помещается: icon-only, overflow или split menu.

### Button groups / segmented buttons

ByeBox:

- Sort mode `Источник / Пинг / Имя` должен быть segmented button row, не набор обычных filled buttons.
- Theme presets можно сделать segmented/cards grid, но выбранное состояние через filled tonal + check/icon, без border как основного признака.
- Routing modes лучше grouped list с radio trailing/leading, а не большие карточки одного цвета.

### Cards

Material cards описывают один subject.

ByeBox:

- Status overview, source summary, active config preview могут быть cards.
- Config row, DNS row, app row, log row не должны быть тяжелыми cards.
- Не вкладывать card в card.

### Lists

ByeBox:

- Proxy nodes: grouped list rows.
- Settings: grouped list rows как Android Settings, с leading icon, headline, supporting, trailing switch/value.
- Logs: monospace list/log surface, controls в top compact toolbar.

### Text fields and search

ByeBox:

- Import URL/search fields: filled tonal field, 54-56dp, outline только focus/error.
- Placeholder не должен быть единственным описанием функции.
- Для поиска configs лучше dedicated SearchBar/expanded search, а не маленькое поле, которое съедает layout.

### Chips

ByeBox:

- Protocol, transport, security, country, ping-state, route summary.
- Chips не должны быть длиннее основного headline.
- Protocol chip: `VLESS / REALITY`, `VMESS / WS / TLS`, `TROJAN / TCP / TLS`.

### Progress indicators

ByeBox:

- Ping all: inline progress в source header и общий progress в control panel.
- Connection: progress ring/indicator внутри connect hero.
- Не использовать snackbar spam для каждого ping.

### Snackbars/toasts

ByeBox:

- Только ошибки, undo delete, редкие подтверждения.
- Refresh done, copied, selected, ping started лучше показывать inline/status chip.

### Dialogs and sheets

ByeBox:

- Диалог: destructive confirm, QR/details, serious choice.
- Bottom sheet: sort/filter, DNS picker, app profile picker.
- Config settings должен быть отдельным экраном или full-height sheet, не маленьким AlertDialog с таблицей.

### Menus

ByeBox:

- Overflow на source: rename, edit URL, export, delete.
- Overflow на config: copy link, QR, details/edit, duplicate, delete.

### Switches, radio, checkbox

ByeBox:

- Switch: binary settings.
- Radio: routing mode, DNS single choice.
- Checkbox: multi-select app packages.
- Не делать binary setting как обычную Card без понятного trailing switch/check.

## 11. Экран Главная

Цель: один экран отвечает на вопрос "VPN работает?" и дает следующий лучший шаг.

Структура:

1. Compact brand/status header.
2. Connection hero:
   - status;
   - active config;
   - connect/disconnect;
   - progress when connecting;
   - uptime when connected.
3. Traffic mini cards:
   - download speed;
   - upload speed;
   - optional total session traffic.
4. Quick action toolbar/button group:
   - лучший узел;
   - поделиться;
   - VPN Android;
   - tile setup.
5. Active config preview row.

Убрать:

- слово Expressive;
- дубли логов;
- постоянные toast на обычные действия;
- одинаковые соседние контейнеры.

## 12. Экран Прокси

Цель: быстро управлять подписками и узлами.

### Control panel "Конфигурации"

- Sticky сверху.
- Expanded state:
  - title + counts;
  - import field;
  - compact action toolbar;
  - segmented sort/filter row.
- Collapsed state:
  - title;
  - source/config count;
  - current sort/filter chips;
  - expand icon.
- Не занимать слишком много vertical space при scroll.

### Source header

- Sticky внутри текущей подписки.
- Содержит name, URL/description, node count, average ping, updated time, traffic info.
- Действия: refresh, ping source, overflow.
- Toggle collapse скрывает все configs этой подписки.

### Config row

Целевая структура:

- Leading: flag/globe/status.
- Headline: country/name, active marker.
- Supporting 1: `protocol / transport / security`.
- Supporting 2 optional: endpoint/description, ellipsis.
- Trailing: ping chip + overflow/copy.
- Swipe left: delete.
- Swipe right: settings/details.
- Long press: context menu/details.

Данные, которые нужно показывать при наличии:

- protocol;
- network/transport;
- security/TLS/Reality;
- SNI/host/path/fingerprint short;
- source;
- description from subscription/config;
- last ping and failure state.

## 13. Экран Настройки

Паттерн: Android Settings style grouped lists.

Структура:

- Page title.
- Groups as rounded containers.
- Rows:
  - leading icon in tonal circle;
  - headline;
  - supporting text;
  - trailing switch/value/chevron.

Рекомендуемые группы:

- Appearance:
  - system dynamic color;
  - dark theme mode;
  - reduce motion;
  - pure black.
- VPN behavior:
  - routing profile;
  - DNS;
  - IPv6 disabled until backend ready;
  - metered network;
  - allow Android VPN bypass.
- App routing:
  - profiles;
  - include/exclude mode;
  - app picker;
  - resource filter.
- Android integration:
  - Quick Settings tile;
  - battery optimization;
  - notification traffic;
  - VPN system settings.
- Diagnostics:
  - logs;
  - export debug bundle.

Что сейчас исправить:

- заменить крупные одноцветные cards на grouped list rows;
- убрать border у обычных rows;
- сделать leading icons;
- длинные тексты переносить в supporting, не в кнопки;
- hidden/disabled для backend-неготовых функций.

## 14. Экран Логи

Паттерн: diagnostics surface.

- Верхняя toolbar: search/filter level/export/clear.
- Логи в `LazyColumn`, monospace, selectable/copyable.
- Уровни логов цветом и label, но без радужного шума.
- Автоскролл только если пользователь уже внизу.
- Не держать отдельную кнопку логов на главной, если есть вкладка.

## 15. Config details/settings

AlertDialog текущего типа недостаточен для редактирования конфига.

Нужен отдельный screen или modal full-height sheet:

- Top app bar: back, title, save, overflow.
- Sections:
  - Basic: name, address, port, id/password.
  - Protocol: protocol, flow, encryption.
  - Transport: tcp/ws/grpc/httpupgrade/quic, host, path.
  - Security: TLS/Reality, SNI, fingerprint, ALPN, public key, short id.
  - Routing/test: ping, resource filter, app profile override.
- Actions:
  - duplicate;
  - export/copy;
  - QR;
  - delete.

## 16. Implementation checklist

### Immediate UI cleanup

- Убрать все оставшиеся full-width подложки под floating nav.
- Проверить bottom content padding на всех вкладках.
- Перевести settings rows на grouped-list pattern.
- Перевести sort bar на segmented button pattern.
- Уменьшить vertical footprint control panel "Конфигурации".
- Убрать toast для select/copy/ping success, заменить inline status.

### Proxy performance

- Проверить recomposition в `ServerItemCard`.
- Уменьшить глобальные state updates во время swipe.
- Не вычислять summaries в draw path без `remember`.
- Использовать stable keys/contentType везде.
- Проверить 40, 100, 500 configs.

### Motion pass

- Sticky headers должны двигаться и отпускаться без snap-back.
- Corner morph должен зависеть от gesture progress.
- Neighbor rows должны получать легкий follow offset и corner response.
- Haptic: light, threshold-based, не грубый.

### Component replacement

- `Card` for rows -> custom `Surface`/`ListRow`.
- Big action rows -> `ButtonGroup`/toolbar pattern where available.
- Dialog details -> details screen/full sheet.
- Text actions in tight places -> icon buttons + contentDescription.

## 17. Definition of Done

Экран считается MD3E-ready, если:

- все основные элементы используют Material color roles;
- соседние крупные поверхности различимы по tonal level;
- нет border как основного разделителя;
- нет card-in-card;
- floating nav не имеет прямоугольной подложки;
- последний элемент списка можно поднять выше nav island;
- строки списков читаются как M3 list rows;
- sticky/collapse/swipe motion плавные и не snap-back;
- нет лишних snackbars/toasts;
- русские labels не обрезаются в кнопках;
- 360dp width выглядит профессионально;
- 40+ configs остаются кликабельными и плавными;
- все видимые настройки реально работают или честно disabled.

## 18. Приоритеты переработки

1. Navigation/insets: окончательно убрать подложку, выровнять fade, проверить bottom padding.
2. Proxy list: list row component, source sticky header, compact control panel, performance.
3. Settings: grouped Android Settings-style rows.
4. Main: connection hero and compact quick actions.
5. Motion/haptics: swipe detach, sticky collapse, nav morph.
6. Dialogs/sheets: config details screen, overflow menus.
7. QA: dark/light/system dynamic, 360dp/412dp/tablet, 40/100/500 configs.
