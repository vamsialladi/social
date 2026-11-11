export function getProfileStorageKey(userId) {
  const accountId = String(userId || '').trim().toLowerCase()
  return accountId ? `truehunt-profile:${encodeURIComponent(accountId)}` : null
}
