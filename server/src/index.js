import { loadConfig } from './config.js';
import { createApp } from './app.js';

const config = loadConfig();
const log = {
  info: (...a) => console.log(new Date().toTimeString().slice(0, 8), 'INFO ', ...a),
  error: (...a) => console.error(new Date().toTimeString().slice(0, 8), 'ERROR', ...a),
};
const { app, db, importer } = createApp(config, { log });

const server = app.listen(config.port, '0.0.0.0', () => log.info(`AbitSearch API: http://localhost:${config.port}`));
server.on('error', (e) => {
  log.error(e.code === 'EADDRINUSE' ? `Порт ${config.port} зайнятий: сервер уже запущено або порт використовує інша програма` : e);
  process.exit(1);
});

// Планувальник імпорту: при старті, якщо даних немає або вони застаріли, далі - щогодинна перевірка.
if (config.autoImport) {
  const tick = () => { if (importer.isDue()) importer.run(); };
  tick();
  setInterval(tick, 60 * 60 * 1000).unref();
}

const shutdown = () => server.close(() => { db.close(); process.exit(0); });
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
