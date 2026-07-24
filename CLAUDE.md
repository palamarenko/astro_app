# CLAUDE.md — Iruna (Astro / Horoscope KMP App)

Карта проекта для быстрого входа в контекст. Читается автоматически в начале каждой сессии.

## Что это

Мобильное приложение **Iruna** — гороскопы, таро, толкование снов, совместимость знаков. Контент генерируется через **Anthropic API (Claude)**. Kotlin Multiplatform + Compose Multiplatform, таргеты **Android + iOS**. Бэкенд — Firebase (Cloud Functions, FCM push, Analytics, Firestore).

- `rootProject.name = "Iruna"`, applicationId / namespace: **`com.iruna.app`**
- Firebase проект: **`zodiac-b23ce`** (europe-west1)
- Версия: versionName `1.1`, versionCode из `VERSION_CODE` env (fallback 4)

> Примечание: `README.md` частично устарел (упоминает старый пакет `com.astro.app` и модель `claude-haiku-4-5`). Актуальный пакет — `com.iruna.app`. При расхождениях доверять коду, а не README.

## Стек

| Слой | Технология |
|---|---|
| Язык | Kotlin `2.4.0` (Multiplatform) |
| UI | Compose Multiplatform `1.11.1` (Material3) |
| HTTP | Ktor Client `3.5.0` (android / darwin engines) |
| Сериализация | kotlinx.serialization `1.8.1` |
| Async | kotlinx.coroutines `1.11.0`, kotlinx.datetime `0.8.0` |
| ViewModel | jetbrains androidx lifecycle-viewmodel(-compose) `2.10.0` |
| Изображения | Coil3 `3.5.0` (coil-compose + coil-network-ktor3) |
| Реклама | Google Mobile Ads / AdMob `24.3.0` (Android), нативный мост на iOS |
| Firebase | BOM `34.15.0` — messaging + analytics |
| Backend | Firebase Cloud Functions (Node 20, firebase-functions v2) |
| Сборка | Gradle (AGP `9.1.1`), version catalog `gradle/libs.versions.toml` |
| SDK | minSdk 24, targetSdk 35, compileSdk 36, JVM target 11 |

## Структура репозитория

```
composeApp/          ← основной KMP-модуль (Android+iOS+общий код)
  src/commonMain/    ← вся общая логика и UI
  src/androidMain/   ← Android-специфика (actual-реализации, MainActivity, push)
  src/iosMain/       ← iOS-специфика (actual-реализации, MainViewController)
functions/           ← Firebase Cloud Functions (index.js) — авто-генерация контента, push
iosApp/              ← Xcode-проект-обёртка (iosApp.xcodeproj, Podfile)
public/              ← Firebase Hosting (privacy policy, иконки)
fastlane/            ← Fastfile для деплоя (Android/iOS)
.github/workflows/   ← deploy-android.yml, deploy-ios.yml (ручной запуск)
tarot_cards/         ← исходные PNG карт таро
goro/                ← ассеты для сторов (иконки, скрины, feature graphic)
gradle/libs.versions.toml  ← единый source of truth по версиям
```

### commonMain — ключевые пакеты (`com.iruna.app.*`)

- `App.kt` — корневой Composable, нижняя навигация. Табы: **HOROSCOPE, TAROT, DREAM, PROFILE** (см. `TAB_ORDER`). ViewModel-ы создаются один раз через `remember` и живут вне `key(lang)`.
- `data/` — модели и сервисы:
  - `Models.kt` — доменные модели (ZodiacSign, TarotCard, response-модели)
  - `ClaudeApiClient.kt` / `AiGenerationService.kt` — вызовы Anthropic API (провайдер `AnthropicAiProvider`)
  - `FirebaseService.kt`, `Analytics.kt` (expect/actual), `HttpClientFactory.kt` (expect/actual)
  - `UserStorage.kt`, `TarotStorage.kt` — локальное хранилище (expect/actual), `PlacesApiClient.kt`
- `ui/theme/Theme.kt` — **все дизайн-токены** (`AppColors`, spacing, radius, типографика). Начинать любые правки стилей отсюда.
- `ui/components/` — переиспользуемые компоненты (CommonComponents, NavIcons, StarfieldBackground, Haptics, ImageSharer, AppBackHandler)
- `ui/screens/` — экраны по фичам, каждая папка = Screen + ViewModel:
  - `horoscope/` (+ SignPickerScreen), `tarot/`, `dream/`, `compatibility/`, `profile/` (онбординг, место рождения), `admin/` (крупный админ-раздел: контент, push, биллинг, календарь, настройки)
- `i18n/` — своя система локализации. **7 языков**: ru, uk, en, es, de, fr, ar. Строки в `StringsXx.kt` + `AppStrings.kt`/`LanguageManager.kt`. НЕ хардкодить пользовательские строки — добавлять во все `Strings*.kt`.
- `notifications/` — push (FCM), топики, локальные тестовые пуши (expect/actual).
- `ads/` — `AdManager` (expect/actual), на iOS — `IosAdBridge`.

### expect/actual паттерн

Файлы вида `Foo.kt` (commonMain, `expect`) + `Foo.android.kt` + `Foo.ios.kt` (`actual`). При добавлении платформенной абстракции нужны все три. Примеры: `HttpClientFactory`, `Analytics`, `UserStorage`, `TarotStorage`, `AdManager`, `Platform`, `LocaleProvider`, `Haptics`, `ImageSharer`, `AppBackHandler`, push-хелперы.

## Команды

Из корня проекта. Обёртка — `./gradlew`.

```bash
# Android debug на устройство/эмулятор
./gradlew :composeApp:installDebug

# Сборка Android release AAB
./gradlew :composeApp:bundleRelease

# Компиляция общего кода / проверка
./gradlew :composeApp:compileKotlinIosSimulatorArm64   # проверить iOS-таргет
./gradlew build

# iOS: открыть iosApp/iosApp.xcodeproj в Xcode и запустить
#   (сначала: cd iosApp && pod install)

# Firebase Functions
cd functions && npm install
firebase deploy --only functions
firebase deploy --only hosting
```

Деплой в сторы — через GitHub Actions (`workflow_dispatch`, вкладка Actions) или fastlane. Android → Play beta/alpha/internal, iOS → TestFlight.

## Секреты и конфигурация

- **Ключи НЕ в git.** Берутся из `local.properties` (локально) или env-переменных (CI). Ключи: `ANTHROPIC_API_KEY`, `GOOGLE_MAPS_API_KEY`, AdMob-идентификаторы (`ADMOB_APP_ID`, `ADMOB_REWARDED_AD_UNIT_ID`, + `*_TEST` варианты, переключаются флагом `ADMOB_USE_TEST_ADS`).
- Прокидываются в код через `buildConfigField` (Android) и сгенерированный `GeneratedSecrets.kt` (iOS, таск `generateSecrets`, попадает в `build/`, gitignored).
- Firebase Functions используют секреты `ADMIN_SECRET`, `CLAUDE_API_KEY` (defineSecret).
- Подпись release: keystore и пароли только из env (`KEYSTORE_PATH`, `KEYSTORE_STORE_PASSWORD`, `KEYSTORE_KEY_ALIAS`, `KEYSTORE_KEY_PASSWORD`).

## Соглашения по коду

- Kotlin official code style (`kotlin.code.style=official`).
- Комментарии в коде на русском — язык проекта; можно продолжать в том же стиле.
- Строки UI — только через i18n (`Strings*.kt`), не хардкодить. Изменения строк дублировать во все 7 языков.
- Дизайн-токены — только из `ui/theme/Theme.kt`, не хардкодить цвета/отступы в экранах.
- Новая фича-экран = отдельная папка в `ui/screens/<feature>/` с парой `Screen` + `ViewModel`.
- **Новую фичу/компонент всегда выносить в отдельный файл, а не дописывать в существующий.** Новый CTA/кнопку/виджет — в свой `.kt` (напр. `ui/components/<Name>.kt` или `ui/screens/<feature>/<Name>.kt`), а на месте использования только вызывать. Не раздувать большие экранные файлы новыми composable-ами.
- Платформенная логика — через expect/actual, не через прямые `if (platform)`.

## Дизайн-токены (из Theme.kt)

- Фон: `#090910` (почти чёрный), карточки `#111119` / `#0D0D15`, surface `#171724`
- Акцент (золото): `#BE9A4A`, приглушённый `#7A6030`
- Текст: primary `#F2EEE6`, secondary `#B8B0A0`, muted `#666666`
- Стихии: Fire `#B8915A`, Earth `#8A7D62`, Air `#7A8FA0`, Water `#6A7EA8`
- Шрифты: Cormorant Garamond (заголовки, Light/Italic) + DM Sans (текст). Тёмный премиум-UI.

## Firebase Functions (functions/index.js)

- Node 20, firebase-functions v2 (onRequest + onSchedule).
- Авто-генерация гороскопов через Claude на 7 языков, 12 знаков. Конфиг в Firestore: `admin_config/push_schedule`, `admin_config/gen_schedule`, `admin_config/gen_langs`, `admin_config/prompt_{daily|weekly|monthly}`.
- RTDB: `zodiac-b23ce-default-rtdb.europe-west1`.
- Промпты: технический каркас (список знаков, JSON-схема, дата, язык) захардкожен, из Firestore берётся только стиль/тон. Есть детальные требования к качеству языка (`LANG_QUALITY`) — особое внимание к украинскому (без русизмов).

## Нюансы и подводные камни

- **README устарел** — пакет теперь `com.iruna.app`, доверять коду.
- ViewModel-ы намеренно вне `key(lang)` — иначе при смене языка пересоздаются и повторно триггерят init-блоки (напр. попап уведомлений). См. комментарий в `App.kt`.
- Смена языка не должна пересоздавать ViewModel-ы и терять состояние.
- При первом запуске (профиля нет) язык устройства сохраняется сразу, чтобы не потерялся до конца онбординга.
- iOS-секреты идут через сгенерированный `GeneratedSecrets.kt`, а не BuildConfig — при работе с ключами на iOS смотреть таск `generateSecrets` в `composeApp/build.gradle.kts`.
- Firebase BOM подключается вне блока `kotlin{}` (в обычном `dependencies{}`) — `platform()` в KMP-блоке не работает.
- AdMob: тестовые vs боевые unit-id переключаются флагом `ADMOB_USE_TEST_ADS` — не залить боевые объявления в дебаг.
- `android.builtInKotlin=false`, `android.newDsl=false` в gradle.properties — не менять без причины.
- Крупный `admin/` раздел (16 файлов) — внутриприложенная админка для управления контентом/push/биллингом; не путать с пользовательскими экранами.
- Есть каталог `worktrees/` (git worktrees) — рабочие копии, не основной код.
- Ассеты карт таро в `tarot_cards/`, ассеты сторов в `goro/` — вне модуля приложения.

## Аналитика

Список событий — в `ANALYTICS_EVENTS.md` (корень). Реализация — `data/Analytics.kt` (expect/actual, Firebase Analytics).
