const API_BASE = '/api'

function getAuthToken() {
  return localStorage.getItem('truehunt_token')
}

export function setAuthToken(token) {
  if (token) {
    localStorage.setItem('truehunt_token', token)
  } else {
    localStorage.removeItem('truehunt_token')
  }
}

async function request(endpoint, options = {}) {
  const token = getAuthToken()
  const headers = {
    ...options.headers,
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  if (!(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
    credentials: 'include',
  })

  let data
  const contentType = response.headers.get('content-type')
  if (contentType && contentType.includes('application/json')) {
    data = await response.json()
  } else {
    data = await response.text()
  }

  if (!response.ok) {
    const errorObj = (data && data.error) ? data.error : {
      code: 'API_ERROR',
      message: response.statusText || 'An error occurred while communicating with the server.',
    }
    const error = new Error(errorObj.message)
    error.code = errorObj.code
    error.fields = errorObj.fields
    error.status = response.status
    throw error
  }

  return data
}

// ==========================================
// AUTH API
// ==========================================
export const authApi = {
  async register({ email, password, name }) {
    const res = await request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, name }),
    })
    if (res.data?.token) {
      setAuthToken(res.data.token)
    }
    return res.data
  },

  async login({ email, password }) {
    const res = await request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
    if (res.data?.token) {
      setAuthToken(res.data.token)
    }
    return res.data
  },

  async logout() {
    try {
      await request('/auth/logout', { method: 'POST' })
    } finally {
      setAuthToken(null)
    }
  },

  async getMe() {
    const res = await request('/auth/me')
    return res.data
  },
}

// ==========================================
// PROFILE API
// ==========================================
export const profileApi = {
  async getProfile(userId) {
    const res = await request(`/profiles/${userId}`)
    return res.data
  },

  async updateMyProfile(profileData) {
    const res = await request('/profiles/me', {
      method: 'PATCH',
      body: JSON.stringify(profileData),
    })
    return res.data
  },
}

// ==========================================
// MEDIA STORAGE API
// ==========================================
export const mediaApi = {
  async upload(file, category) {
    const formData = new FormData()
    formData.append('file', file)
    if (category) {
      formData.append('category', category)
    }
    const res = await request('/media/upload', {
      method: 'POST',
      body: formData,
    })
    return res.data
  },

  getMediaUrl(fileKey) {
    if (!fileKey) return ''
    if (fileKey.startsWith('http://') || fileKey.startsWith('https://')) {
      return fileKey
    }
    return `${API_BASE}/media/${fileKey}`
  },
}

// ==========================================
// POSTS & OPPORTUNITIES API
// ==========================================
export const feedApi = {
  async getPosts(cursor, limit = 20) {
    const params = new URLSearchParams()
    if (cursor) params.append('cursor', cursor)
    if (limit) params.append('limit', limit)
    const queryString = params.toString() ? `?${params.toString()}` : ''
    const res = await request(`/posts${queryString}`)
    return res
  },

  async createPost({ type = 'post', title, content, attachments = [], visibility = 'public' }) {
    const res = await request('/posts', {
      method: 'POST',
      body: JSON.stringify({ type, title, content, attachments, visibility }),
    })
    return res.data
  },

  async getOpportunities(cursor, limit = 20) {
    const params = new URLSearchParams()
    if (cursor) params.append('cursor', cursor)
    if (limit) params.append('limit', limit)
    const queryString = params.toString() ? `?${params.toString()}` : ''
    const res = await request(`/opportunities${queryString}`)
    return res
  },

  async createOpportunity({ title, content, attachments = [], visibility = 'public' }) {
    const res = await request('/opportunities', {
      method: 'POST',
      body: JSON.stringify({ title, content, attachments, visibility }),
    })
    return res.data
  },
}

// ==========================================
// NETWORK & CONNECTIONS API
// ==========================================
export const connectionApi = {
  async getSuggestions() {
    const res = await request('/connections/suggestions')
    return res.data
  },

  async requestConnection(recipientId) {
    const res = await request('/connections', {
      method: 'POST',
      body: JSON.stringify({ recipientId }),
    })
    return res.data
  },

  async respondToConnection(connectionId, status) {
    const res = await request(`/connections/${connectionId}`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    })
    return res.data
  },

  async getAcceptedConnections() {
    const res = await request('/connections')
    return res.data
  },
}

// ==========================================
// MESSAGING API
// ==========================================
export const messageApi = {
  async getConversations() {
    const res = await request('/conversations')
    return res.data
  },

  async getOrCreateDirectConversation(otherUserId) {
    const res = await request(`/conversations/direct/${otherUserId}`, {
      method: 'POST',
    })
    return res.data
  },

  async getMessages(conversationId, cursor, limit = 30) {
    const params = new URLSearchParams()
    if (cursor) params.append('cursor', cursor)
    if (limit) params.append('limit', limit)
    const queryString = params.toString() ? `?${params.toString()}` : ''
    const res = await request(`/conversations/${conversationId}/messages${queryString}`)
    return res
  },

  async sendMessage(conversationId, { body, attachments = [] }) {
    const res = await request(`/conversations/${conversationId}/messages`, {
      method: 'POST',
      body: JSON.stringify({ body, attachments }),
    })
    return res.data
  },
}

// ==========================================
// NOTIFICATIONS API
// ==========================================
export const notificationApi = {
  async getNotifications(cursor, limit = 20) {
    const params = new URLSearchParams()
    if (cursor) params.append('cursor', cursor)
    if (limit) params.append('limit', limit)
    const queryString = params.toString() ? `?${params.toString()}` : ''
    const res = await request(`/notifications${queryString}`)
    return res
  },

  async markAsRead(notificationId) {
    const res = await request(`/notifications/${notificationId}/read`, {
      method: 'PATCH',
    })
    return res.data
  },

  async markBulkRead(notificationIds) {
    const res = await request('/notifications/read', {
      method: 'PATCH',
      body: JSON.stringify({ notificationIds }),
    })
    return res.data
  },
}
