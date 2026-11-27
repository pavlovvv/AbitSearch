import { TRACKS } from '../src/edbo/client.js';

/** Тестові дані повторюють структуру реальних відповідей відкритого API ЄДЕБО (після mapXxx). */
export const registry = [
  { id: 174, name: '"Національний технічний університет України «КПІ імені Ігоря Сікорського»"', parentId: '', shortName: 'КПІ ім. Ігоря Сікорського', ownership: 'Державна', region: 'Київ', city: 'м. Київ', address: 'просп. Берестейський, 37', site: 'https://kpi.ua', blockedAt: '' },
  { id: 900, name: 'Філія КПІ', parentId: '174', shortName: 'Філія', ownership: 'Державна', region: 'Київ', city: 'м. Київ', address: '', site: '', blockedAt: '' },
  { id: 500, name: 'Закритий інститут', parentId: '', shortName: 'ЗІ', ownership: 'Приватна', region: 'Львівська', city: 'м. Львів', address: '', site: '', blockedAt: '01.01.2020' },
];

export const entrant = (year, universityId, specialtyCode, specialtyName, program, dayBudget, dayContract, parentId = '') => ({
  year, universityId, parentId, universityName: `Заклад ${universityId}`, region: 'Київ', ownership: 'Державна',
  specialtyCode, specialtyName, specialization: '', program,
  dayBudget, dayContract, partBudget: 0, partContract: 0, eveningBudget: 0, eveningContract: 0,
});

export const batches = [
  { year: 2024, track: TRACKS[0], rows: [entrant(2024, 174, '121', 'Інженерія програмного забезпечення', 'Інженерія програмного забезпечення в енергетиці', 30, 10)] },
  // 2025: новий код спеціальності, та сама назва; плюс зарахування у філії.
  { year: 2025, track: TRACKS[0], rows: [
    entrant(2025, 174, 'F2', 'Інженерія програмного забезпечення', 'Інженерія програмного забезпечення в енергетиці', 25, 12),
    entrant(2025, 900, 'F2', 'Інженерія програмного забезпечення', 'Інженерія програмного забезпечення в енергетиці', 5, 1, '174'),
    entrant(2025, 777, 'F3', "Комп'ютерні науки", '', 3, 4),
  ] },
];

export const licenses = {
  BACHELOR: [
    { universityId: 174, parentId: '', specialtyCode: '121', specialtyName: 'Інженерія програмного забезпечення', volumeYear: 200, volumeDay: 0, volumePart: 0, volumeEvening: 0, accredited: 'Так', accreditationUntil: '01.07.2028' },
    { universityId: 174, parentId: '', specialtyCode: '121', specialtyName: 'Інженерія програмного забезпечення', volumeYear: 50, volumeDay: 0, volumePart: 0, volumeEvening: 0, accredited: 'Ні', accreditationUntil: '' },
  ],
  MASTER: [],
};

/** Підміна ЄДЕБО без мережі. */
export class FakeEdbo {
  async universities() { return registry; }
  async entrants(year, track) { return batches.find((b) => b.year === year && b.track === track)?.rows ?? []; }
  async licenses(level) { return licenses[level] ?? []; }
}
