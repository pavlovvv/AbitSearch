import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { createApp } from '../src/app.js';
import { testConfig } from '../src/config.js';
import { FakeEdbo } from './fixtures.js';

const silent = { info() {}, error() {} };
let server, base, importer;

before(async () => {
  const created = createApp(testConfig({ fromYear: 2024, toYear: 2025 }), { edbo: new FakeEdbo(), log: silent, pauseMs: 0 });
  importer = created.importer;
  server = created.app.listen(0);
  await new Promise((r) => server.once('listening', r));
  base = `http://127.0.0.1:${server.address().port}`;
});
after(() => server.close());

const call = async (method, path, { token, body } = {}) => {
  const res = await fetch(base + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : typeof body === 'string' ? body : JSON.stringify(body),
  });
  return { status: res.status, json: await res.json() };
};
const login = async (email, password) => (await call('POST', '/auth/login', { body: { email, password } })).json.token;

test('реєстрація: валідація, конфлікт e-mail, видача токена', async () => {
  assert.equal((await call('POST', '/auth/register', { body: { name: 'О', email: 'x', password: '1' } })).status, 400);
  assert.equal((await call('POST', '/auth/register', { body: '{bad json' })).status, 400);
  assert.equal((await call('POST', '/auth/register', { body: { name: 'Тест', email: 'student@abitsearch.ua', password: 'Password1' } })).status, 409);
  const ok = await call('POST', '/auth/register', { body: { name: 'Марія Шевченко', email: 'maria@example.com', password: 'Password1' } });
  assert.equal(ok.status, 201);
  assert.equal(ok.json.token.split('.').length, 3);
  assert.equal(ok.json.user.role, 'USER');
});

test('вхід і розмежування доступу за роллю', async () => {
  assert.equal((await call('POST', '/auth/login', { body: { email: 'student@abitsearch.ua', password: 'wrong123' } })).status, 401);
  assert.equal((await call('GET', '/auth/me')).status, 401);
  assert.equal((await call('GET', '/auth/me', { token: 'abc.def.ghi' })).status, 401);
  const user = await login('student@abitsearch.ua', 'Student2026');
  const me = await call('GET', '/auth/me', { token: user });
  assert.equal(me.status, 200);
  assert.equal(me.json.email, 'student@abitsearch.ua');
  assert.equal((await call('GET', '/admin/stats', { token: user })).status, 403);
  const admin = await login('admin@abitsearch.ua', 'Admin2026');
  assert.equal((await call('GET', '/admin/stats', { token: admin })).status, 200);
});

test('імпорт через /admin/import і відповіді /api/*', async () => {
  const token = await login('admin@abitsearch.ua', 'Admin2026');
  assert.equal((await call('POST', '/admin/import', { token })).status, 202);
  while (importer.running) await new Promise((r) => setTimeout(r, 20));
  assert.equal((await call('GET', '/api/meta', { token })).json.status, 'READY');

  const universities = (await call('GET', '/api/universities', { token })).json;
  // 174 - діючий, 777 - має програму 2025 року; 500 закритий і без програм - не показується.
  assert.deepEqual(universities.map((u) => u.id).sort(), [174, 777]);

  const programs = (await call('GET', '/api/universities/174/programs', { token })).json;
  assert.equal(programs.length, 1);
  assert.equal(programs[0].budget, 30);
  const detail = (await call('GET', `/api/programs/${programs[0].id}`, { token })).json;
  assert.equal(detail.stats.length, 2);
  assert.equal(detail.license.volume, 250);

  const specialties = (await call('GET', '/api/specialties?level=BACHELOR', { token })).json;
  assert.equal(specialties.length, 2);
  const bySpecialty = (await call('GET', `/api/programs?specialty=${encodeURIComponent(specialties[0].key)}&level=BACHELOR`, { token })).json;
  assert.ok(bySpecialty.length >= 1);
  assert.equal((await call('GET', '/api/universities/999/programs', { token })).status, 404);
});
