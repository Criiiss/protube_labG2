export function validatePassword(value: string): string | undefined {
  if (value.length < 8 || new TextEncoder().encode(value).length > 72 || !/[A-Z]/.test(value) || !/[0-9]/.test(value))
    return 'Usa al menos 8 caracteres, una mayúscula y un número (máximo 72 bytes).';
}
export function hasMinimumAge(value: string, minimumAge: number, today = new Date()): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const birth = new Date(`${value}T00:00:00`);
  const cutoff = new Date(today.getFullYear() - minimumAge, today.getMonth(), today.getDate());
  return !Number.isNaN(birth.getTime()) && birth <= cutoff;
}
