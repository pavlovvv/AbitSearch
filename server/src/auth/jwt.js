import jwt from 'jsonwebtoken';

/**
 * Видача та перевірка JWT (HS256). Ключ підпису зберігається лише на сервері.
 * Claims: sub - id користувача (рядок), name, email, role, iat, exp.
 * Клієнт читає з них роль і термін дії.
 */
export function issueToken(user, config) {
  return jwt.sign(
    { name: user.name, email: user.email, role: user.role },
    config.jwtSecret,
    { algorithm: 'HS256', subject: String(user.id), issuer: config.jwtIssuer, expiresIn: `${config.tokenTtlHours}h` },
  );
}

/** Middleware Express: пропускає запит лише з дійсним токеном у заголовку Authorization. */
export function requireAuth(config) {
  return (req, res, next) => {
    const [scheme, token] = (req.get('Authorization') ?? '').split(' ');
    if (scheme !== 'Bearer' || !token) return res.status(401).json({ message: 'Токен відсутній або недійсний' });
    try {
      req.user = jwt.verify(token, config.jwtSecret, { algorithms: ['HS256'], issuer: config.jwtIssuer });
      next();
    } catch {
      res.status(401).json({ message: 'Токен відсутній або недійсний' });
    }
  };
}

/** Авторизація за роллю: роль береться з перевіреного токена, тож підробити її на клієнті неможливо. */
export function requireRole(role) {
  return (req, res, next) => (req.user?.role === role ? next() : res.status(403).json({ message: 'Доступ лише для адміністратора' }));
}
