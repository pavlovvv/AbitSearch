/**
 * Налаштування сервера зі змінних середовища: один і той самий код працює
 * локально (файл SQLite у server/data) і в Docker-контейнері.
 */
export function loadConfig(env = process.env) {
  return {
    port: Number(env.PORT ?? 8080),
    dbFile: env.DB_FILE ?? 'data/abitsearch.db',
    jwtSecret: env.JWT_SECRET ?? 'abitsearch-dev-secret-change-me',
    jwtIssuer: 'abitsearch',
    tokenTtlHours: Number(env.TOKEN_TTL_HOURS ?? 24),
    // Відкриті дані про зарахування є з 2016 року.
    fromYear: Number(env.IMPORT_FROM_YEAR ?? 2016),
    toYear: Number(env.IMPORT_TO_YEAR ?? 2025),
    importIntervalHours: Number(env.IMPORT_INTERVAL_HOURS ?? 24),
    // false - сервер не звертається до ЄДЕБО сам (для тестів).
    autoImport: (env.AUTO_IMPORT ?? 'true') !== 'false',
    edboBaseUrl: env.EDBO_BASE_URL ?? 'https://registry.edbo.gov.ua',
  };
}

/** Конфігурація для тестів: база в пам'яті, без звернень до ЄДЕБО. */
export function testConfig(overrides = {}) {
  return { ...loadConfig({}), dbFile: ':memory:', autoImport: false, ...overrides };
}
