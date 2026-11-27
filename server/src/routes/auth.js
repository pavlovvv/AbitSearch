import { Router } from 'express';
import { issueToken, requireAuth } from '../auth/jwt.js';
import { verifyPassword } from '../auth/passwords.js';
import { validateRegistration } from '../auth/users.js';

const toDto = (u) => ({ id: u.id, name: u.name, email: u.email, role: u.role });

/** POST /auth/register, POST /auth/login - відкриті; GET /auth/me - лише з дійсним JWT. */
export function authRouter({ users, config }) {
  const r = Router();

  r.post('/register', (req, res) => {
    const error = validateRegistration(req.body);
    if (error) return res.status(400).json({ message: error });
    const { name, email, password } = req.body;
    if (users.findByEmail(email)) return res.status(409).json({ message: 'Користувач з таким e-mail вже існує' });
    const user = users.create(name.trim(), email, password);
    res.status(201).json({ token: issueToken(user, config), user: toDto(user) });
  });

  r.post('/login', (req, res) => {
    const { email, password } = req.body ?? {};
    const user = typeof email === 'string' ? users.findByEmail(email) : null;
    if (!user || typeof password !== 'string' || !verifyPassword(password, user.passwordHash)) {
      return res.status(401).json({ message: 'Невірний e-mail або пароль' });
    }
    res.json({ token: issueToken(user, config), user: toDto(user) });
  });

  r.get('/me', requireAuth(config), (req, res) => {
    const user = users.findById(Number(req.user.sub));
    if (!user) return res.status(401).json({ message: 'Користувача не знайдено' });
    res.json(toDto(user));
  });

  return r;
}
