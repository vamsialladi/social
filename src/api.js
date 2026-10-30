const API_BASE = '/api'

async function fetchJson(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`
  const headers = { ...options.headers }
  try {
    const storedUser = localStorage.getItem('truehunt_user')
    const email = storedUser ? JSON.parse(storedUser)?.email : ''
    if (email) headers['X-User-Email'] = email
  } catch (error) {
    console.warn('Could not read the signed-in user for the profile request:', error)
  }
  if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
    options.body = JSON.stringify(options.body)
  }

  const response = await fetch(url, {
    headers,
    ...options,
  })

  if (!response.ok) {
    let errorMsg = `Request failed: ${response.status}`
    try {
      const errorData = await response.json()
      if (errorData?.error?.message) {
        errorMsg = errorData.error.message
      }
    } catch {
      // ignore
    }
    throw new Error(errorMsg)
  }

  const json = await response.json()
  return json.data !== undefined ? json.data : json
}

export const api = {
  // Profiles
  getProfile: () => fetchJson('/profiles/me'),
  getProfileById: (id) => fetchJson(`/profiles/${id}`),
  updateProfile: (profile) => fetchJson('/profiles/me', { method: 'PATCH', body: profile }),
  verifyReference: (referenceId) => fetchJson('/profiles/reference-check', { method: 'POST', body: { referenceId } }),

  // Media Upload
  uploadMedia: async (file, category = '') => {
    const formData = new FormData()
    formData.append('file', file)
    if (category) formData.append('category', category)

    const response = await fetch(`${API_BASE}/media/upload`, {
      method: 'POST',
      body: formData,
    })

    if (!response.ok) {
      let errorMsg = `Upload failed: ${response.status}`
      try {
        const err = await response.json()
        if (err?.error?.message) errorMsg = err.error.message
      } catch {
        // ignore
      }
      throw new Error(errorMsg)
    }

    const json = await response.json()
    return json.data
  },

  // Posts & Opportunities
  getPosts: (type = '') => fetchJson(`/posts${type ? `?type=${type}` : ''}`),
  getOpportunities: () => fetchJson('/opportunities'),
  createPost: (post) => fetchJson('/posts', { method: 'POST', body: post }),
  createOpportunity: (opp) => fetchJson('/opportunities', { method: 'POST', body: opp }),
  likePost: (id) => fetchJson(`/posts/${id}/like`, { method: 'POST' }),
  commentOnPost: (id, text) => fetchJson(`/posts/${id}/comments`, { method: 'POST', body: { text } }),
  applyToOpportunity: (id) => fetchJson(`/posts/${id}/apply`, { method: 'POST' }),
  deletePost: (id) => fetchJson(`/posts/${id}`, { method: 'DELETE' }),

  // Network & Connections
  getNetworkSuggestions: () => fetchJson('/connections/suggestions'),
  getConnectedProfiles: () => fetchJson('/connections'),
  toggleConnection: (profileId) => fetchJson('/connections/toggle', { method: 'POST', body: { profileId } }),
  toggleFollow: (profileId) => fetchJson('/connections/follow', { method: 'POST', body: { profileId } }),

  // Messaging & Conversations
  getConversations: (mailbox = '') => fetchJson(`/conversations${mailbox ? `?mailbox=${mailbox}` : ''}`),
  getConversation: (id) => fetchJson(`/conversations/${id}`),
  sendMessage: (conversationId, content, attachment = null) =>
    fetchJson(`/conversations/${conversationId}/messages`, {
      method: 'POST',
      body: { content, direction: 'outgoing', attachment },
    }),
  createConversation: (recipient, messageText) =>
    fetchJson('/conversations', { method: 'POST', body: { recipient, messageText } }),
  markConversationAsRead: (id) => fetchJson(`/conversations/${id}/read`, { method: 'PATCH' }),
  starConversation: (id) => fetchJson(`/conversations/${id}/star`, { method: 'PATCH' }),
  updateMailbox: (id, mailbox) => fetchJson(`/conversations/${id}/mailbox`, { method: 'PATCH', body: { mailbox } }),
  deleteConversation: (id) => fetchJson(`/conversations/${id}`, { method: 'DELETE' }),

  // Notifications / Endorsements
  getNotifications: () => fetchJson('/notifications'),
  markNotificationAsRead: (id) => fetchJson(`/notifications/${id}/read`, { method: 'PATCH' }),
  markAllNotificationsAsRead: () => fetchJson('/notifications/read-all', { method: 'PATCH' }),
  deleteNotification: (id) => fetchJson(`/notifications/${id}`, { method: 'DELETE' }),

  // Settings
  getSettings: () => fetchJson('/settings'),
  updateTheme: (theme) => fetchJson('/settings/theme', { method: 'PATCH', body: { theme } }),
  updateNotificationPreferences: (prefs) => fetchJson('/settings/notifications', { method: 'PATCH', body: prefs }),
  changePassword: (data) => fetchJson('/settings/change-password', { method: 'POST', body: data }),
  deleteAccount: () => fetchJson('/settings/delete-account', { method: 'POST' }),

  // Promotions
  clickPromotion: (campaign) => fetchJson('/promotions/click', { method: 'POST', body: { campaign } }),

  // Search
  search: (query) => fetchJson(`/search?q=${encodeURIComponent(query)}`),

  // Auth
  login: (credentials) => fetchJson('/auth/login', { method: 'POST', body: credentials }),
  register: (userData) => fetchJson('/auth/register', { method: 'POST', body: userData }),
  logout: () => fetchJson('/auth/logout', { method: 'POST' }),
  getMe: () => fetchJson('/auth/me'),
}

export default api
