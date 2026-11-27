/**
 * Клієнт відкритого API ЄДЕБО (https://registry.edbo.gov.ua/opendata/).
 * Лише публічні методи /api/opendata/, без авторизації. Відповіді - JSON-масиви
 * з ключами українською мовою; тут вони перетворюються на об'єкти з англійськими полями.
 */

/** Освітній ступінь і основа вступу - обов'язкові параметри qf та eb. */
export const LEVELS = { BACHELOR: { qf: 1, title: 'Бакалавр' }, MASTER: { qf: 2, title: 'Магістр' } };

/** Набори, що імпортуються: бакалавр на основі ПЗСО і магістр на основі бакалавра. */
export const TRACKS = [
  { level: 'BACHELOR', eb: 40, base: 'Повна загальна середня освіта' },
  { level: 'MASTER', eb: 620, base: 'Бакалавр' },
];

const int = (v) => Number(v) || 0;

export function mapUniversity(r) {
  return {
    id: Number(r['Код']),
    name: r['Назва закладу освіти'],
    parentId: r['Код головного закладу'],
    shortName: r['Коротка назва'],
    ownership: r['Форма власності'],
    region: r['Регіон (місцезнаходження)'],
    city: r['Населений пункт (місцезнаходження)'],
    address: r['Адреса (місцезнаходження)'],
    site: r['Веб-сайт'],
    blockedAt: r["Дата блокування суб'єкта освітньої діяльності в ЄДЕБО"],
  };
}

export function mapEntrant(r) {
  return {
    year: int(r['Рік вступу']),
    universityId: Number(r['Код']),
    parentId: r['Код головного закладу'],
    universityName: r['Назва закладу освіти'],
    region: r['Регіон'],
    ownership: r['Форма власності'],
    specialtyCode: r['Код спеціальності'],
    specialtyName: r['Назва спеціальності'],
    specialization: r['Спеціалізація'],
    program: r['Освітня програма'],
    dayBudget: int(r['Денна (бюджет)']),
    dayContract: int(r['Денна (контракт)']),
    partBudget: int(r['Заочна (бюджет)']),
    partContract: int(r['Заочна (контракт)']),
    eveningBudget: int(r['Вечірня (бюджет)']),
    eveningContract: int(r['Вечірня (контракт)']),
  };
}

export function mapLicense(r) {
  return {
    universityId: Number(r['Код']),
    parentId: r['Код головного закладу'],
    specialtyCode: r['Код спеціальності'],
    specialtyName: r['Назва спеціальності'],
    volumeYear: int(r['Обсяг (на рік)']),
    volumeDay: int(r['Обсяг (денна)']),
    volumePart: int(r['Обсяг (заочна)']),
    volumeEvening: int(r['Обсяг (вечірня)']),
    accredited: r['Акредитовано'],
    accreditationUntil: r['Дата закінчення дії'],
  };
}

export class EdboClient {
  constructor(baseUrl = 'https://registry.edbo.gov.ua') {
    this.baseUrl = baseUrl;
  }

  async universities() {
    return (await this.#get('/api/opendata/universities/?ut=1&exp=json')).map(mapUniversity);
  }

  async entrants(year, track) {
    const qf = LEVELS[track.level].qf;
    return (await this.#get(`/api/opendata/university-entrant/?y=${year}&qf=${qf}&eb=${track.eb}&st=1&exp=json`)).map(mapEntrant);
  }

  async licenses(level) {
    return (await this.#get(`/api/opendata/licenses-specialities/?qf=${LEVELS[level].qf}&exp=json`)).map(mapLicense);
  }

  async #get(path) {
    const response = await fetch(this.baseUrl + path, {
      headers: { Accept: 'application/json', 'User-Agent': 'AbitSearch/1.0 (coursework, open data import)' },
      signal: AbortSignal.timeout(120_000),
    });
    if (!response.ok) throw new Error(`ЄДЕБО повернув ${response.status} для ${path}`);
    const body = (await response.text()).trim();
    // Для порожньої вибірки API інколи повертає не масив, а об'єкт або порожній рядок.
    if (!body.startsWith('[')) return [];
    return JSON.parse(body);
  }
}
