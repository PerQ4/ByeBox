# ByeBox MD3E design system

## Цель

ByeBox должен ощущаться как современный Android VPN-клиент уровня V2RayTun/Happ: быстрый, плотный, понятный, нативный и визуально собранный. Интерфейс не должен выглядеть как набор карточек с одинаковым цветом. Основной стиль: Material 3 Expressive поверх Android dynamic color, с короткой плавающей навигацией, ясной иерархией, живыми состояниями и минимумом лишних рамок.

## Источники и ограничения

- Material 3 NavigationBar: нижняя навигация предназначена для 3-5 основных разделов, контейнер может быть прозрачным, а tonal elevation влияет на overlay поверхности.
- Android Material 3 Expressive guidance: акцент на dynamic color, более широкую тональную палитру, выразительные формы и motion.
- Android edge-to-edge: контент должен жить за системными bars, а не упираться в прямоугольные системные подложки.
- VPN-клиент - это рабочий инструмент. MD3E здесь не означает декоративность ради декоративности: плотность, читаемость, доступность и быстрые действия важнее крупных маркетинговых блоков.

## Принципы

1. Никаких полноширинных подложек под плавающими элементами. Если элемент называется островом, вокруг него должен быть виден реальный контент или фон.
2. Не использовать border как основной способ разделения. Разделять элементы цветовым тоном, размером, формой, отступом и elevation.
3. Dynamic color - основной источник цветов. Ручные темы Slate/Desert/Sage могут остаться как пресеты, но системная тема должна быть первой и самой нативной.
4. Соседние элементы не должны иметь один и тот же container color. Минимальная разница между соседними поверхностями: один тональный уровень.
5. Главный экран показывает состояние VPN и лучший следующий шаг. Экран прокси показывает подписки и узлы. Настройки не должны забирать функции, которые нужны каждый день.
6. Любая всплывашка снизу используется только для ошибок или редких подтверждений. Для обычного результата действия использовать inline-state, progress, badge или маленький status-chip.
7. Логи - отдельный раздел. Не дублировать кнопку логов в шапке и нижней навигации.

## Цветовые токены

Использовать только `MaterialTheme.colorScheme` и локальные semantic wrappers:

- `appBackground`: `background`.
- `surfaceBase`: `surface`.
- `surfaceRaised`: `surfaceContainer`.
- `surfaceIsland`: `surfaceContainerHigh`.
- `surfacePressed`: `surfaceContainerHighest`.
- `primaryAction`: `primaryContainer` + `onPrimaryContainer`.
- `secondaryAction`: `secondaryContainer` + `onSecondaryContainer`.
- `dangerAction`: `errorContainer` + `onErrorContainer`.
- `successState`: `primary` или отдельный semantic green только для ping/connected, но не для крупных контейнеров.
- `warningState`: `tertiary`.

Правила:

- Не ставить рядом два больших блока `surface`.
- Карточка подписки: `surfaceContainer`.
- Активный узел: `primaryContainer` с низкой насыщенностью или тональным акцентом слева.
- Кнопки внутри карточки: `secondaryContainer`, `tertiaryContainer`, `errorContainer` по смыслу.
- Нижний остров: непрозрачный `surfaceContainerHigh`.
- Bottom edge fade: маленький прозрачный градиент от `Transparent` к `background` alpha 0.25-0.35, высота 16-24dp.

## Формы

- Плавающий nav island: capsule 28-32dp radius, высота 52-56dp, ширина по содержимому.
- Активный nav item: capsule 20-24dp radius.
- Большие панели: 24-28dp radius.
- Списки узлов: 18-22dp radius, без border.
- Чипы протокола/ping: 12-16dp radius.
- Текстовые поля: 18-22dp radius, без outline в спокойном состоянии; outline только focus/error.
- FAB/быстрая кнопка: 22-28dp radius или circle для single-icon.

## Elevation и слои

Слои сверху вниз:

1. Dialog, modal sheet.
2. Toast/snackbar, но использовать редко.
3. Bottom nav island.
4. Floating action cluster.
5. Cards/panels.
6. Page background.

Правила:

- Нижний nav island не имеет родительской панели.
- Контент прокручивается за островом. Последние элементы списка получают `contentPadding(bottom = navHeight + gestureInset)`, но фон под островом не рисуется отдельным прямоугольником.
- Для dark theme тень острова слабая, потому что сильная тень выглядит как грязная плашка.

## Типографика

- App title: `titleLarge`, weight 800-900, letterSpacing 1.5-2sp.
- Section title: `titleMedium`, weight 700-800.
- Config name: `bodyLarge` или `titleSmall`, weight 700.
- Endpoint/description: `bodySmall`, max 1-2 lines, ellipsis.
- Protocol chip: 10-11sp, uppercase only для короткого протокола.
- Ping chip: 11sp, monospace или bold tabular если доступно.

Запреты:

- Не использовать hero-scale типографику внутри карточек.
- Не сжимать длинный русский текст в кнопке до нечитаемости. Если не помещается, менять layout: icon-only, перенос, overflow menu.

## Motion

Базовые параметры:

- Tab switch: crossfade + shared axis по X, 180-240ms.
- Nav item selection: shape/width/color morph, 220-280ms, easing emphasized.
- Button press: scale 0.96-0.98, 80-120ms down, 160-220ms up.
- Config card select: background tone morph + tiny leading accent, 180-240ms.
- List item insert/remove: fade + vertical expand/shrink, 180-260ms.
- Ping all: progress indication inline на source card, no snackbar spam.
- Connection button: state transition disconnected/connecting/connected через color + icon morph + progress ring.

Запреты:

- Не анимировать весь экран без причины.
- Не использовать бесконечные декоративные анимации в списках, где много узлов.
- Не показывать toast/snackbar на каждое успешное действие.

## Главная

Состав:

- Верхняя шапка: название, статус, без кнопки логов.
- Connection hero: состояние, активный узел, входящий/исходящий трафик, uptime.
- Quick actions: лучший узел, поделиться, VPN Android, плитка. Не больше 2 колонок.
- Active config preview: компактная строка с флагом/именем, endpoint, protocol/transport/security, ping, refresh.
- Routing summary: текущий профиль маршрутизации и приложения в обходе/прокси.

Что убрать:

- Слово Expressive на главной.
- Дубли логов.
- Всплывашки при штатных действиях.

## Прокси

Состав:

- Источники подписок группируются. Одна source card содержит описание, URL, число узлов, traffic info, updated time, действия.
- Узлы внутри источника компактные: флаг/иконка, имя, protocol stack, address preview, ping, overflow.
- Primary actions source card: обновить, пинг всех, сортировка.
- Secondary actions уходят в overflow: rename, delete, export, edit URL.

Данные узла:

- Protocol: VLESS/VMESS/Trojan/SS.
- Network: tcp/ws/grpc/httpupgrade/quic.
- Security: tls/reality/none.
- SNI/host/path/fingerprint при наличии.
- Subscription name/description при наличии.
- Last ping и дата проверки.

QoL:

- Ping all by source.
- Sort by source/ping/name/protocol/country/last used.
- Filter by resource availability.
- Copy link, QR, details/edit page.
- После 40+ конфигов список должен оставаться кликабельным и виртуализированным.

## Страница настройки конфига

Сделать отдельный экран вместо перегруза карточки:

- Header: back, title, save, overflow.
- Sections: Basic, Transport, Security, Routing, Test.
- Fields: name, address, port, id/password, flow, network, host, path, SNI, fingerprint, ALPN.
- Actions: test ping, test resource, duplicate, export, delete.

## Настройки

Состав:

- Theme: System first, then presets. Dynamic color toggle only where системная тема недоступна.
- Routing: clear profile cards without borders.
- DNS: compact selector plus advanced sheet.
- Android integration: VPN settings, tile setup, battery optimization, notification traffic.
- App routing profiles: profiles list, include/exclude mode, package picker, resource filter.

## Уведомление VPN

Должно показывать:

- Connection state.
- Active config name.
- Up/down speed.
- Total session traffic.
- Quick action disconnect.

Не должно:

- Всегда показывать 0 B/s при активном трафике.
- Дублировать длинные технические строки.

## Нижняя навигация

Текущие разделы:

- Главная.
- Прокси.
- Настройки.
- Логи.

Правила:

- Остров непрозрачный.
- Нет родительской панели.
- Контент виден по бокам и за островом.
- Активный item расширяется и показывает label.
- Неактивные items icon-only или короткий label только если хватает места.
- Нижний edge fade 16-24dp, прозрачный и не воспринимается как плашка.

## Диалоги и sheets

- Dialog только для подтверждения разрушительного действия или сложного выбора.
- Bottom sheet для выбора DNS, routing profile, app profile.
- Context menu/overflow для редких действий.
- Long press quick tile открывает главную, без диалога действий.

## Контраст и доступность

- Минимум WCAG AA для текста и иконок.
- Интерактивные targets минимум 48dp, кроме плотных списков, где визуальная высота может быть 44dp, но touch target расширяется.
- Цвет не единственный индикатор состояния: использовать текст/иконку/status chip.
- Русский текст проверять на маленьких экранах 360dp.

## План переработки

1. Navigation cleanup: прозрачная system bar, непрозрачный island, маленький fade, без дубля логов.
2. Token layer: вынести semantic colors/shapes/motion в отдельный файл темы.
3. Main screen: переписать hero/status/quick actions на MD3E blocks.
4. Proxy screen: source grouping, compact node rows, details page, overflow actions.
5. Settings screen: убрать одноцветные соседние blocks, заменить borders на tonal separation.
6. Motion pass: tab transition, card selection, button press, ping progress.
7. Android integration pass: notification traffic, quick tile, VPN settings, app routing profiles.
8. QA pass: 360dp/412dp/tablet, dark/light/system dynamic color, 40/100/500 configs.

## Definition of done

- Внизу нет прямоугольной панели под островом.
- На скриншоте остров читается как отдельный элемент.
- В каждом экране есть понятная tonal hierarchy.
- Нет соседних крупных элементов одного цвета.
- Нет обрезанного русского текста в кнопках и чипах.
- Нет лишних snackbars/toasts.
- Все заявленные actions работают или скрыты до реализации.
- Сборка проходит `assembleDebug`.
