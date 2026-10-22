// API Client for Social Network Backend

const API_BASE = '/api';

export function getStoredUser() {
  try {
    const raw = localStorage.getItem('social_user');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setStoredUser(user, sessionId) {
  if (user) {
    localStorage.setItem('social_user', JSON.stringify(user));
  } else {
    localStorage.removeItem('social_user');
  }
  if (sessionId) {
    localStorage.setItem('social_session_id', sessionId);
  } else if (!user) {
    localStorage.removeItem('social_session_id');
  }
}

export function getAuthHeaders() {
  const headers = {};
  const user = getStoredUser();
  const sessionId = localStorage.getItem('social_session_id');
  if (sessionId) {
    headers['X-Session-Id'] = sessionId;
  }
  if (user?.id) {
    headers['X-User-Id'] = user.id;
  }
  return headers;
}

async function request(path, options = {}) {
  const headers = {
    ...getAuthHeaders(),
    ...(options.headers || {})
  };

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    credentials: 'include'
  });

  const contentType = response.headers.get('content-type') || '';
  let payload = null;
  if (contentType.includes('application/json')) {
    payload = await response.json();
  }

  if (!response.ok) {
    const errorMsg = payload?.error?.message || payload?.message || `Request failed with status ${response.status}`;
    const err = new Error(errorMsg);
    err.status = response.status;
    err.payload = payload;
    throw err;
  }

  return payload;
}

export const api = {
  // Auth
  async login(email, password) {
    const res = await request('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    if (res?.data?.user) {
      setStoredUser(res.data.user, res.data.sessionId);
    }
    return res.data;
  },

  async register(email, password, displayName) {
    const res = await request('/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, displayName })
    });
    if (res?.data?.user) {
      setStoredUser(res.data.user, res.data.sessionId);
    }
    return res.data;
  },

  async getCurrentUser() {
    try {
      const res = await request('/auth/me');
      if (res?.data?.user) {
        setStoredUser(res.data.user, res.data.sessionId);
      }
      return res.data;
    } catch (err) {
      if (err.status === 401) {
        setStoredUser(null, null);
      }
      throw err;
    }
  },

  async logout() {
    try {
      await request('/auth/logout', { method: 'POST' });
    } finally {
      setStoredUser(null, null);
    }
  },

  // Feed & Posts
  async getFeed(cursor, limit = 20) {
    const params = new URLSearchParams();
    if (cursor) params.set('cursor', cursor);
    if (limit) params.set('limit', limit);
    return request(`/posts?${params.toString()}`);
  },

  async createPost(content, mediaUrls = [], visibility = 'public') {
    return request('/posts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content, mediaUrls, visibility })
    });
  },

  // Opportunities
  async getOpportunities(cursor, limit = 20) {
    const params = new URLSearchParams();
    if (cursor) params.set('cursor', cursor);
    if (limit) params.set('limit', limit);
    return request(`/opportunities?${params.toString()}`);
  },

  async createOpportunity(data) {
    return request('/opportunities', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  },

  // Media
  async uploadMedia(file) {
    const formData = new FormData();
    formData.append('file', file);
    const headers = getAuthHeaders();
    const response = await fetch(`${API_BASE}/media/upload`, {
      method: 'POST',
      headers,
      body: formData,
      credentials: 'include'
    });
    if (!response.ok) {
      throw new Error(`Media upload failed: ${response.statusText}`);
    }
    return response.json();
  },

  // Profile
  async getProfile(userId) {
    return request(`/profiles/${userId}`);
  },

  async updateProfile(profileData) {
    return request('/profiles/me', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(profileData)
    });
  },

  // Connections
  async getSuggestions(limit = 10) {
    return request(`/connections/suggestions?limit=${limit}`);
  },

  async getConnections() {
    return request('/connections');
  },

  async requestConnection(recipientId) {
    return request('/connections', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ recipientId })
    });
  },

  // Conversations & Messages
  async getConversations() {
    return request('/conversations');
  },

  async getMessages(conversationId) {
    return request(`/conversations/${conversationId}/messages`);
  },

  async sendMessage(conversationId, content) {
    return request(`/conversations/${conversationId}/messages`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content })
    });
  },

  // Notifications
  async getNotifications() {
    return request('/notifications');
  },

  async markNotificationRead(id) {
    return request(`/notifications/${id}/read`, { method: 'PATCH' });
  }
};
