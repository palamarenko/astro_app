# План аналитики событий (Analytics Events Plan)

Астро-приложение (KMP: Android + iOS). Firebase уже подключён (Realtime DB + FCM),
но модуля Analytics нет — стоит только `firebase-messaging`. Этот документ описывает,
**что собирать** и **как внедрить**.

---

## 1. Рекомендация по технологии

Так как Firebase уже в проекте, а код общий (KMP), самый простой путь — **Firebase Analytics**
через мультиплатформенную обёртку GitLive:

```
implementation("dev.gitlive:firebase-analytics:<version>")
```

Она даёт единый API в `commonMain` и под капотом использует нативный Firebase Analytics
на Android и iOS. Альтернатива — самому написать `expect/actual` обёртку над нативными
SDK (Android: `firebase-analytics-ktx`, iOS: `FirebaseAnalytics` через CocoaPods).

Минимальная обёртка в `commonMain`:

```kotlin
object Analytics {
    fun log(event: String, params: Map<String, Any?> = emptyMap())
    fun setUserProperty(name: String, value: String?)
    fun setUserId(id: String?)
}
```

**Что Firebase считает автоматически** (эти события писать вручную НЕ нужно):
`first_open` (установки), `session_start` (ежедневные открытия/сессии),
`app_update`, `os_update`, `screen_view` (частично), `ad_impression`,
`user_engagement` (время в приложении). То есть «сколько установок» и
«сколько ежедневных открытий» из вопроса закрываются из коробки — нужно лишь
подключить модуль.

---

## 2. Соглашения об именовании

- События: `snake_case`, глагол/объект, латиница. Максимум 40 символов.
- Параметры: `snake_case`, до 25 параметров на событие.
- Не класть в события персональные данные (имя, точная дата рождения, координаты).
  Знак зодиака / пол / язык — допустимо (агрегаты).

---

## 3. События по разделам

### 3.1 Жизненный цикл (в основном авто)

| Событие | Когда | Параметры |
|---|---|---|
| `first_open` | установка (авто) | — |
| `session_start` | старт сессии (авто) | — |
| `app_open` *(опц.)* | ручной, если нужен свой счётчик открытий | `source` (icon / push / widget) |
| `screen_view` | открытие экрана | `screen_name` (horoscope, tarot, dream, compatibility, profile, admin) |

### 3.2 Онбординг (= регистрация профиля)

| Событие | Когда | Параметры |
|---|---|---|
| `onboarding_start` | показан первый шаг | — |
| `onboarding_step_complete` | завершён шаг | `step` (welcome, name, gender, date, time, place) |
| `onboarding_skip` | нажат «Пропустить» | `step` |
| `onboarding_complete` | профиль создан | `has_name`, `gender`, `has_birth_time`, `has_birth_place`, `zodiac_sign` |

`onboarding_complete` — ключевая конверсия (аналог `sign_up`). По воронке
`onboarding_start → step_complete(*) → onboarding_complete` видно, где отваливаются.

### 3.3 Гороскоп

| Событие | Когда | Параметры |
|---|---|---|
| `horoscope_sign_select` | выбор знака в карусели | `sign` |
| `horoscope_period_select` | таб периода | `period` (day/week/month/year) |
| `horoscope_view` | показан прогноз | `sign`, `period` |
| `horoscope_wizard_cta_click` | тап по CTA персонального прогноза | `sign`, `period` |
| `horoscope_wizard_generated` | персональный прогноз получен (после рекламы) | `sign`, `period` |

### 3.4 Таро

| Событие | Когда | Параметры |
|---|---|---|
| `tarot_period_select` | выбор расклада/периода | `period` |
| `tarot_draw` | нажата «Вытянуть карты» | `period` |
| `tarot_wizard_cta_click` | тап по CTA расширенного расклада | `period` |
| `tarot_reading_generated` | расклад/трактовка получены | `period`, `cards_count` |

### 3.5 Сны

| Событие | Когда | Параметры |
|---|---|---|
| `dream_decode_click` | нажата «Расшифровать» | `text_length` (bucket: 0-50/50-200/200+) |
| `dream_result_view` | показана трактовка | — |
| `dream_new` | «Новый сон» | — |

### 3.6 Совместимость (Coming Soon)

| Событие | Когда | Параметры |
|---|---|---|
| `compatibility_open` | открыт таб (фича ещё недоступна) | — |

Полезно для приоритизации: показывает спрос на пока не выпущенную функцию.

### 3.7 Профиль и настройки

| Событие | Когда | Параметры |
|---|---|---|
| `profile_edit` | изменено поле профиля | `field` (gender, birthdate, birthtime, birthplace) |
| `language_change` | смена языка | `from`, `to` |
| `admin_open` *(внутр.)* | вход в админку | — |

### 3.8 Уведомления (пуши)

| Событие | Когда | Параметры |
|---|---|---|
| `push_prompt_shown` | показан свой промпт-диалог | — |
| `push_permission_result` | ответ на системный запрос | `granted` (true/false) |
| `push_prompt_dismiss` | закрыт свой диалог («Позже») | — |
| `push_topic_subscribe` | подписка на топик | `topic` |
| `notification_open` | приложение открыто из пуша | `campaign` / `topic` |

### 3.9 Реклама (rewarded — ключевая монетизация)

Rewarded-видео гейтит персональный гороскоп и расширенное таро — это главный
источник дохода, мерить обязательно.

| Событие | Когда | Параметры |
|---|---|---|
| `ad_rewarded_request` | запрошен показ | `placement` (horoscope_wizard / tarot_wizard) |
| `ad_rewarded_shown` | реклама показана | `placement` |
| `ad_rewarded_completed` | досмотрел, награда выдана | `placement` |
| `ad_rewarded_failed` | не загрузилась / ошибка | `placement`, `reason` |

Плюс авто-событие `ad_impression`. Воронка `request → shown → completed`
показывает fill rate и долю досмотров по каждому месту.

### 3.10 AI-генерация (контроль расходов на Claude API)

Каждая генерация — прямые деньги. Стоит отдельно мерить объём и ошибки.

| Событие | Когда | Параметры |
|---|---|---|
| `ai_generation_request` | отправлен запрос | `feature` (horoscope/tarot/dream), `lang` |
| `ai_generation_success` | получен ответ | `feature`, `lang`, `latency_ms` |
| `ai_generation_error` | ошибка/таймаут | `feature`, `lang`, `reason` |

---

## 4. User Properties (свойства пользователя)

Задаются один раз и позволяют сегментировать любые события:

- `zodiac_sign` — знак зодиака
- `gender` — пол
- `app_language` — язык интерфейса
- `has_birth_time` — указано ли время рождения (влияет на точность)
- `has_birth_place` — указано ли место рождения
- `notifications_enabled` — включены ли пуши
- `onboarding_completed` — прошёл ли онбординг

---

## 5. Ключевые метрики, которые всё это закрывает

- **Установки** → `first_open` (авто).
- **Ежедневные открытия / DAU / retention** → `session_start` + `user_engagement` (авто).
- **Нажатия на функции** → события разделов 3.3–3.8.
- **Монетизация** → воронка rewarded-рекламы (3.9).
- **Себестоимость** → объём AI-генераций (3.10).
- **Где теряем юзеров** → воронка онбординга (3.2).

---

## 6. Порядок внедрения (предлагаемый)

1. Подключить `firebase-analytics` (GitLive) + `google-services` уже настроен для FCM.
2. Добавить обёртку `Analytics` в `commonMain/data`.
3. Проставить user properties в конце онбординга и при смене языка/пола.
4. Разложить события по экранам (по одному вызову на действие).
5. Проверить в Firebase DebugView, что события приходят с параметрами.
6. Собрать дашборд / воронки в Firebase (или экспорт в BigQuery для глубокой аналитики).
