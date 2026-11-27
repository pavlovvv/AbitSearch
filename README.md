# AbitSearch

Додаток для перегляду інформації вступної кампанії на основі відкритих даних ЄДЕБО:
заклади вищої освіти, освітні програми, кількість зарахованих за 2016-2025 роки
(бюджет і контракт, денна / заочна / вечірня форма), ліцензовані обсяги та акредитація.

## Архітектура

```
Android (app)  --HTTP/JSON + JWT-->  Сервер AbitSearch (server, Node.js)  --HTTPS-->  ЄДЕБО, відкриті дані
Room (кеш)                           SQLite                                       registry.edbo.gov.ua/opendata
```

- **app** - Kotlin, Jetpack Compose, MVVM, Room, Retrofit + Gson, DataStore, WorkManager.
- **server** - JavaScript, Node.js 22.5+, Express 4, JWT (jsonwebtoken, HS256), SQLite (вбудований модуль `node:sqlite`).
  Раз на добу імпортує з відкритого API ЄДЕБО реєстр закладів, статистику зарахування по освітніх
  програмах і ліцензовані спеціальності, нормалізує їх і віддає застосунку через REST API.

## Джерела даних (лише публічні методи `/api/opendata/`)

| Набір | Запит |
|---|---|
| Реєстр закладів вищої освіти | `GET /api/opendata/universities/?ut=1&exp=json` |
| Кількість осіб, зарахованих на навчання | `GET /api/opendata/university-entrant/?y=<рік>&qf=<ступінь>&eb=<основа>&st=1&exp=json` |
| Ліцензовані спеціальності | `GET /api/opendata/licenses-specialities/?qf=<ступінь>&exp=json` |

Рейтингові списки з ПІБ вступників у відкритих даних не публікуються, тому застосунок їх не показує.

## REST API сервера

| Метод | Шлях | Доступ |
|---|---|---|
| POST | `/auth/register`, `/auth/login` | усі |
| GET | `/auth/me` | JWT |
| GET | `/api/meta`, `/api/universities`, `/api/universities/{id}/programs` | JWT |
| GET | `/api/programs/{id}`, `/api/programs?specialty=&level=`, `/api/specialties?level=` | JWT |
| GET | `/admin/stats`; POST `/admin/import` | JWT, роль ADMIN |

Тестові користувачі (створюються при першому запуску сервера):
`admin@abitsearch.ua / Admin2026` (ADMIN), `student@abitsearch.ua / Student2026` (USER).

## Запуск

1. **Сервер.** `cd server && npm install && npm start` (або в Android Studio конфігурація
   `AbitSearch Server`, що викликає Gradle-задачу `:server:run`). Потрібен Node.js 22.5+.
   Сервер слухає `http://localhost:8080`, базу SQLite створює в `server/data/`. Перший імпорт ЄДЕБО
   триває приблизно хвилину. У Docker: `docker compose up --build`.
2. **Застосунок.** Конфігурація `app` на емуляторі: адреса сервера `http://10.0.2.2:8080/`
   (localhost комп'ютера з погляду емулятора). Для фізичного пристрою вкажіть IP комп'ютера
   у `gradle.properties`: `abitApiUrl=http://192.168.x.x:8080/` і додайте його в `network_security_config.xml`.

Тести: `cd server && npm test`; застосунок: `./gradlew :app:testDebugUnitTest`.

## Структура

```
app/src/main/java/ua/kpi/abitsearch/
├── domain/model        доменні моделі
├── data/remote/abit    Retrofit-клієнт API сервера
├── data/local          Room: сутності, DAO, база
├── data/repository     offline-first репозиторії (TTL-кеш)
├── data/auth           JWT, DataStore токена, інтерцептор
├── data/sync           фонове оновлення WorkManager
└── ui                  Compose: тема, компоненти, екрани, навігація
server/
├── src/index.js        запуск HTTP-сервера, планувальник імпорту
├── src/app.js          Express: middleware, маршрути, обробка помилок
├── src/edbo            клієнт відкритого API ЄДЕБО, нормалізація, імпорт
├── src/db              схема SQLite, транзакції, запити
├── src/auth            паролі (PBKDF2), JWT, користувачі, валідація
├── src/routes          REST-маршрути /auth, /api, /admin
└── test                node:test - модульні та інтеграційні тести
```
