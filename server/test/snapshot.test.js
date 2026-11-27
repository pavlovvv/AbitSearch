import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildSnapshot, clean, key, region, stableId } from '../src/edbo/snapshot.js';
import { mapEntrant } from '../src/edbo/client.js';
import { registry, batches, licenses } from './fixtures.js';

const s = buildSnapshot(registry, batches, licenses);

test('нормалізація прибирає лапки, зайві пробіли й різні апострофи', () => {
  assert.equal(clean('  "Київський   університет" '), 'Київський університет');
  assert.equal(key("Комп'ютерні науки"), key('Комп’ютерні  науки'));
  assert.equal(region('Львівська область'), 'Львівська');
});

test('ключі JSON українською мовою відображаються на поля', () => {
  const e = mapEntrant({ 'Рік вступу': 2025, 'Код': 282, 'Назва спеціальності': 'Початкова освіта', 'Денна (бюджет)': 7, 'Заочна (контракт)': 1 });
  assert.equal(e.year, 2025);
  assert.equal(e.universityId, 282);
  assert.equal(e.dayBudget, 7);
  assert.equal(e.partContract, 1);
  assert.equal(e.eveningBudget, 0);
});

test('стабільний id однаковий між запусками й безпечний для Number', () => {
  assert.equal(stableId('174|BACHELOR|a'), stableId('174|BACHELOR|a'));
  assert.ok(Number.isSafeInteger(stableId('x')));
});

test('філії приєднуються до головного закладу', () => {
  const ids = s.universities.map((u) => u.id);
  assert.ok(!ids.includes(900));
  assert.equal(s.universities.find((u) => u.id === 174).name, 'Національний технічний університет України «КПІ імені Ігоря Сікорського»');
});

test('історія програми не обривається на реформі кодів спеціальностей 2025 року', () => {
  const programs = s.programs.filter((p) => p.universityId === 174);
  assert.equal(programs.length, 1);
  assert.equal(programs[0].specialtyCode, 'F2');
  assert.equal(programs[0].lastYear, 2025);
  const byYear = Object.fromEntries(s.stats.filter((x) => x.programId === programs[0].id).map((x) => [x.year, x]));
  assert.equal(byYear[2024].dayBudget, 30);
  assert.equal(byYear[2025].dayBudget, 30); // 25 у головному закладі + 5 у філії
  assert.equal(byYear[2025].dayContract, 13);
});

test('невідомий заклад додається як неактивний, порожня назва програми береться зі спеціальності', () => {
  assert.equal(s.universities.find((u) => u.id === 777).active, false);
  assert.equal(s.programs.find((p) => p.universityId === 777).programName, "Комп'ютерні науки");
});

test('ліцензовані обсяги однієї спеціальності підсумовуються', () => {
  assert.equal(s.licenses.length, 1);
  assert.equal(s.licenses[0].volume, 250);
  assert.equal(s.licenses[0].accredited, true);
  assert.equal(s.licenses[0].accreditationUntil, '01.07.2028');
});
