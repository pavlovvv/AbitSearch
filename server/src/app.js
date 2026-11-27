import express from 'express';
import { openDatabase } from './db/database.js';
import { DataRepository } from './db/repository.js';
import { UserRepository } from './auth/users.js';
import { EdboClient } from './edbo/client.js';
import { ImportService } from './edbo/importService.js';
import { authRouter } from './routes/auth.js';
import { dataRouter } from './routes/data.js';
import { adminRouter } from './routes/admin.js';

/**
 * Збирає сервер: БД, сервіси, middleware і маршрути. Нічого не запускає,
 * тому той самий код використовують index.js і тести (edbo можна підмінити).
 */
export function createApp(config, { edbo = new EdboClient(config.edboBaseUrl), log = console, pauseMs } = {}) {
  const db = openDatabase(config.dbFile);
  const users = new UserRepository(db);
  users.seedDemoUsers();
  const data = new DataRepository(db);
  const importer = new ImportService(db, edbo, config, { log, pauseMs });
  const deps = { users, data, importer, config };

  const app = express();
  app.use(express.json({ limit: '100kb' }));
  // Журнал запитів: метод, шлях, код відповіді й тривалість.
  app.use((req, res, next) => {
    const start = Date.now();
    res.on('finish', () => log.info(`${res.statusCode} ${req.method} ${req.originalUrl} ${Date.now() - start} ms`));
    next();
  });

  app.get('/', (req, res) => res.json({ name: 'AbitSearch API', version: '1.0.0' }));
  app.use('/auth', authRouter(deps));
  app.use('/api', dataRouter(deps));
  app.use('/admin', adminRouter(deps));
  app.use((req, res) => res.status(404).json({ message: 'Ресурс не знайдено' }));

  // Помилки: некоректний JSON - 400, решта - 500 без технічних деталей для клієнта.
  app.use((err, req, res, next) => {
    if (err.type === 'entity.parse.failed') return res.status(400).json({ message: 'Некоректне тіло запиту' });
    log.error('Помилка обробки запиту:', err);
    res.status(500).json({ message: 'Внутрішня помилка сервера' });
  });

  return { app, db, importer };
}
