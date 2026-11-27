import { hashPassword } from './passwords.js';

const toUser = (r) => r && { id: r.id, name: r.name, email: r.email, passwordHash: r.password_hash, role: r.role };

export class UserRepository {
  constructor(db, now = Date.now) {
    this.db = db;
    this.now = now;
  }

  findByEmail(email) {
    return toUser(this.db.prepare('SELECT * FROM users WHERE email = ?').get(String(email).trim().toLowerCase()));
  }

  findById(id) {
    return toUser(this.db.prepare('SELECT * FROM users WHERE id = ?').get(id));
  }

  create(name, email, password, role = 'USER') {
    this.db.prepare('INSERT INTO users(name, email, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)')
      .run(name, email.trim().toLowerCase(), hashPassword(password), role, this.now());
    return this.findByEmail(email);
  }

  counts() {
    const r = this.db.prepare("SELECT COUNT(*) AS total, COALESCE(SUM(role = 'ADMIN'), 0) AS admins FROM users").get();
    return { usersCount: r.total, adminsCount: r.admins };
  }

  /** Демонстраційні облікові записи для захисту курсової (створюються один раз). */
  seedDemoUsers() {
    if (!this.findByEmail('admin@abitsearch.ua')) this.create('Адміністратор', 'admin@abitsearch.ua', 'Admin2026', 'ADMIN');
    if (!this.findByEmail('student@abitsearch.ua')) this.create('Олексій Павлов', 'student@abitsearch.ua', 'Student2026', 'USER');
  }
}

/** Ті самі правила, що й у формах застосунку (AuthValidator): сервер не довіряє клієнту. */
const EMAIL = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const NAME = /^\p{L}[\p{L} '’ʼ-]{1,59}$/u;

export function validateRegistration({ name, email, password } = {}) {
  if (typeof name !== 'string' || !NAME.test(name.trim())) return "Ім'я: від 2 до 60 літер";
  if (typeof email !== 'string' || !EMAIL.test(email.trim())) return 'Некоректний e-mail';
  if (typeof password !== 'string' || password.length < 8) return 'Пароль: мінімум 8 символів';
  if (!/\d/.test(password) || !/\p{L}/u.test(password)) return 'Пароль має містити літери й цифри';
  return null;
}
