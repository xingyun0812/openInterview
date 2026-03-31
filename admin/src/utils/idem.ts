export function idemKey(prefix = 'web'): string {
  const id =
    typeof crypto !== 'undefined' && 'randomUUID' in crypto ? (crypto as any).randomUUID() : String(Date.now())
  return `${prefix}-${id}`
}

