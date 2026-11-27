import { Router } from 'express';
import { requireAuth } from '../auth/jwt.js';

const LEVELS = ['BACHELOR', 'MASTER'];
const levelOf = (q) => (LEVELS.includes(q) ? q : 'BACHELOR');

/** Дані для застосунку. Доступні лише автентифікованим користувачам. */
export function dataRouter({ data, importer, config }) {
  const r = Router();
  r.use(requireAuth(config));

  r.get('/meta', (req, res) => res.json(data.meta(importer.running)));

  r.get('/universities', (req, res) => res.json(data.universities()));

  r.get('/universities/:id/programs', (req, res) => {
    const id = Number(req.params.id);
    if (!Number.isInteger(id)) return res.status(400).json({ message: 'Некоректний id' });
    if (!data.universityExists(id)) return res.status(404).json({ message: 'Заклад не знайдено' });
    res.json(data.programsOfUniversity(id));
  });

  r.get('/programs/:id', (req, res) => {
    const id = Number(req.params.id);
    if (!Number.isInteger(id)) return res.status(400).json({ message: 'Некоректний id' });
    const program = data.program(id);
    if (!program) return res.status(404).json({ message: 'Програму не знайдено' });
    res.json(program);
  });

  // GET /api/programs?specialty=<ключ>&level=BACHELOR - програми спеціальності в усіх закладах.
  r.get('/programs', (req, res) => {
    const { specialty, level } = req.query;
    if (typeof specialty !== 'string' || !specialty) return res.status(400).json({ message: 'Потрібен параметр specialty' });
    res.json(data.programsOfSpecialty(specialty, levelOf(level)));
  });

  r.get('/specialties', (req, res) => res.json(data.specialties(levelOf(req.query.level))));

  return r;
}
