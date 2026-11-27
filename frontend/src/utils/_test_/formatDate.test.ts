import { formatVideoDate } from '../formatDate';

describe('formatVideoDate', () => {
  it('shows the publication date as dd/mm/yyyy', () => {
    expect(formatVideoDate({ createdAt: '2026-02-01T12:00:00Z', published: 'hace 1 año' })).toBe('01/02/2026');
  });

  it('falls back to the text sent by the API when there is no valid date', () => {
    expect(formatVideoDate({ published: 'hace 2 días' })).toBe('hace 2 días');
    expect(formatVideoDate({ createdAt: 'not a date', published: 'hoy' })).toBe('hoy');
    expect(formatVideoDate({})).toBe('');
  });
});
