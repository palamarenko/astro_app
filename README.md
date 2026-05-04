# Астро — Horoscope KMP App

Kotlin Multiplatform + Compose Multiplatform приложение гороскопов с тёмным UI и интеграцией Claude AI.

## Стек

| Слой | Технология |
|---|---|
| UI | Compose Multiplatform |
| Язык | Kotlin Multiplatform |
| HTTP | Ktor Client |
| Сериализация | kotlinx.serialization |
| VM | lifecycle-viewmodel-compose |
| AI | Anthropic API (claude-haiku-4-5) |

## Структура

```
composeApp/src/
├── commonMain/kotlin/com/astro/app/
│   ├── App.kt                        ← главный Composable + нижняя навигация
│   ├── data/
│   │   ├── Models.kt                 ← ZodiacSign, TarotCard, response models
│   │   └── ClaudeApiClient.kt        ← HTTP-клиент к Anthropic API
│   ├── ui/
│   │   ├── theme/Theme.kt            ← AppColors, Spacing, Radius, AppType
│   │   ├── components/CommonComponents.kt
│   │   └── screens/
│   │       ├── SignPickerScreen.kt
│   │       ├── HoroscopeScreen.kt
│   │       ├── TarotScreen.kt
│   │       ├── CompatibilityScreen.kt
│   │       └── ProfileScreen.kt
│   └── viewmodel/ViewModels.kt
├── androidMain/
│   ├── AndroidManifest.xml
│   └── kotlin/com/astro/app/MainActivity.kt
└── iosMain/
    └── kotlin/com/astro/app/MainViewController.kt
```

## Быстрый старт

### 1. Вставьте API ключ

Откройте `App.kt` и замените:
```kotlin
private const val API_KEY = "YOUR_ANTHROPIC_API_KEY"
```
Получить ключ: https://console.anthropic.com

Для продакшена используйте `local.properties`:
```properties
ANTHROPIC_API_KEY=sk-ant-...
```

И в `build.gradle.kts`:
```kotlin
buildConfigField("String", "ANTHROPIC_API_KEY",
    "\"${project.findProperty("ANTHROPIC_API_KEY") ?: ""}\"")
```

Тогда в `App.kt`:
```kotlin
private const val API_KEY = BuildConfig.ANTHROPIC_API_KEY
```

### 2. Android

```bash
./gradlew :composeApp:installDebug
```

### 3. iOS

Открыть `iosApp/iosApp.xcodeproj` в Xcode и запустить.

## Экраны

| Экран | Описание |
|---|---|
| Sign Picker | Сетка 12 знаков зодиака с анимированным появлением |
| Daily Horoscope | Гороскоп на день/неделю/месяц + scorecard 4 параметров |
| Tarot | Расклад «Три карты» с анимацией открытия |
| Compatibility | Совместимость двух знаков с AI-анализом |
| Profile | Личный знак + AI-характеристика + дата рождения |

## Дизайн-токены

Все цвета, отступы, размеры шрифтов — в `ui/theme/Theme.kt`.

Основной акцент: `#C9A84C` (золото)  
Фон: `#0A0A0F` (почти чёрный)  
Шрифт: Cormorant Garamond (Light, Italic) + DM Sans

## Шрифты (опционально)

Добавьте в `commonMain/resources/font/`:
- `cormorant_garamond_light.ttf`
- `cormorant_garamond_light_italic.ttf`
- `dm_sans_regular.ttf`

Скачать: https://fonts.google.com
