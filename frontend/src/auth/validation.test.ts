import { hasMinimumAge, validatePassword } from './validation';
import { TextEncoder } from 'node:util';
Object.defineProperty(globalThis, 'TextEncoder', { value: TextEncoder });
describe('Registration validation', () => {
  it('enforces birthday rather than just birth year', () => {
    const today = new Date(2026, 9, 22);
    expect(hasMinimumAge('2013-10-22', 13, today)).toBe(true);
    expect(hasMinimumAge('2013-10-23', 13, today)).toBe(false);
    expect(hasMinimumAge('', 13, today)).toBe(false);
  });
  it('requires mixed passwords and prevents bcrypt truncation', () => {
    expect(validatePassword('strongpassword')).toBeDefined();
    expect(validatePassword('StrongPass1')).toBeUndefined();
    expect(validatePassword('A1' + 'é'.repeat(40))).toBeDefined();
  });
});
