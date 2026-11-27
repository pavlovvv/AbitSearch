import { Router } from 'express';
import { requireAuth, requireRole } from '../auth/jwt.js';

/** Адміністрування: перевіряється не лише підпис JWT, а й роль ADMIN у claims. */
export function adminRouter({ users, data, importer, config }) {
  const r = Router();
  r.use(requireAuth(config), requireRole('ADMIN'));

  r.get('/stats', (req, res) => {
    res.json({ ...users.counts(), lastImport: data.lastImport(), importRunning: importer.running });
  });

  r.post('/import', (req, res) => {
    if (importer.running) return res.status(409).json({ message: 'Імпорт уже виконується' });
    importer.run(); // виконується у фоні, відповідь не чекає завершення
    res.status(202).json({ message: 'Імпорт запущено' });
  });

  return r;
}
