function getStorageKey(email) {
  const accountEmail = String(email || '').trim().toLowerCase()
  return accountEmail
    ? `truehunt-film-maker-visited:${encodeURIComponent(accountEmail)}`
    : ''
}

export function hasVisitedFilmMaker(email) {
  const key = getStorageKey(email)
  if (!key) return false
  try {
    return localStorage.getItem(key) === 'true'
  } catch (error) {
    console.warn('Could not read filmmaker access state:', error)
    return false
  }
}

export function markFilmMakerVisited(email) {
  const key = getStorageKey(email)
  if (!key) return
  try {
    localStorage.setItem(key, 'true')
  } catch (error) {
    console.warn('Could not save filmmaker access state:', error)
  }
}
