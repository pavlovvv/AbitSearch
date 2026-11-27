import { pbkdf2Sync, randomBytes, timingSafeEqual } from 'node:crypto';

/**
 * Хешування паролів PBKDF2-HMAC-SHA256 з випадковою сіллю (модуль crypto Node.js).
 * Формат збереження: pbkdf2$ітерації$сіль$хеш (Base64). Пароль у відкритому вигляді не зберігається.
 */
const ITERATIONS = 120_000;
const KEY_BYTES = 32;

export function hashPassword(password) {
  const salt = randomBytes(16);
  const hash = pbkdf2Sync(password, salt, ITERATIONS, KEY_BYTES, 'sha256');
  return `pbkdf2$${ITERATIONS}$${salt.toString('base64')}$${hash.toString('base64')}`;
}

export function verifyPassword(password, stored) {
  const [scheme, iterations, salt, hash] = String(stored).split('$');
  if (scheme !== 'pbkdf2' || !hash) return false;
  const expected = Buffer.from(hash, 'base64');
  const actual = pbkdf2Sync(password, Buffer.from(salt, 'base64'), Number(iterations), expected.length, 'sha256');
  // Порівняння за сталий час, щоб не давати підказок через тривалість перевірки.
  return timingSafeEqual(expected, actual);
}
