const API_BASE = "/api";

async function fetchJson(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint.startsWith("/") ? endpoint : `/${endpoint}`}`;
  const headers = { ...options.headers };
  try {
    const token = localStorage.getItem("truehunt_token");
    if (token) headers["Authorization"] = `Bearer ${token}`;
    const storedUser = localStorage.getItem("truehunt_user");
    if (storedUser) {
      const u = JSON.parse(storedUser);
      if (u.email) headers["X-User-Email"] = u.email;
      const uId = u.userId || u.publicUserId || u.id;
      if (uId) headers["X-User-Id"] = String(uId);
      if (u.name) headers["X-User-Name"] = u.name;
    }
  } catch (error) {
    console.warn(
      "Could not read the signed-in user for the profile request:",
      error,
    );
  }
  if (
    options.body &&
    typeof options.body === "object" &&
    !(options.body instanceof FormData)
  ) {
    headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(options.body);
  }

  const response = await fetch(url, {
    headers,
    ...options,
  });

  if (!response.ok) {
    let errorMsg = `Request failed: ${response.status}`;
    let errorData = null;
    try {
      errorData = await response.json();
      if (errorData?.error?.message) {
        errorMsg = errorData.error.message;
      }
    } catch {
      // ignore
    }
    const error = new Error(errorMsg);
    error.status = response.status;
    error.code = errorData?.error?.code || errorData?.code;
    error.details = errorData;
    throw error;
  }

  const json = await response.json();
  return json.data !== undefined ? json.data : json;
}

export const api = {
  // Profiles
  getProfile: () => fetchJson("/profiles/me"),
  getProfileById: (id) => fetchJson(`/profiles/${id}`),
  getProfileDirectory: () => fetchJson("/profiles/directory"),
  recordProfileView: (id) =>
    fetchJson(`/profiles/${encodeURIComponent(id)}/views`, { method: "POST" }),
  getProfileViewers: () => fetchJson("/profiles/me/viewers"),
  updateProfile: (profile) =>
    fetchJson("/profiles/me", { method: "PATCH", body: profile }),
  verifyReference: (referenceId) =>
    fetchJson("/profiles/reference-check", {
      method: "POST",
      body: { referenceId },
    }),

  // Media Upload
  uploadMedia: async (file, category = "") => {
    const formData = new FormData();
    formData.append("file", file);
    if (category) formData.append("category", category);

    const response = await fetch(`${API_BASE}/media/upload`, {
      method: "POST",
      body: formData,
    });

    if (!response.ok) {
      let errorMsg = `Upload failed: ${response.status}`;
      let errorData = null;
      try {
        errorData = await response.json();
        if (errorData?.error?.message) errorMsg = errorData.error.message;
      } catch {
        // ignore
      }
      const error = new Error(errorMsg);
      error.status = response.status;
      error.code = errorData?.error?.code || errorData?.code;
      error.details = errorData;
      throw error;
    }

    const json = await response.json();
    return json.data;
  },

  // Posts & Opportunities
  getPosts: (type = "") => fetchJson(`/posts${type ? `?type=${type}` : ""}`),
  getOpportunities: () => fetchJson("/opportunities"),
  createPost: (post) => fetchJson("/posts", { method: "POST", body: post }),
  createOpportunity: (opp) =>
    fetchJson("/opportunities", { method: "POST", body: opp }),
  updateOpportunity: (id, opportunity) =>
    fetchJson(`/posts/${id}`, { method: "PUT", body: opportunity }),
  updatePost: (id, post) =>
    fetchJson(`/posts/${id}`, { method: "PATCH", body: post }),
  likePost: (id) => fetchJson(`/posts/${id}/like`, { method: "POST" }),
  commentOnPost: (id, text) =>
    fetchJson(`/posts/${id}/comments`, { method: "POST", body: { text } }),
  applyToOpportunity: (id) =>
    fetchJson(`/posts/${id}/apply`, { method: "POST" }),
  cancelOpportunityApplication: (id) =>
    fetchJson(`/posts/${id}/apply`, { method: "DELETE" }),
  deletePost: async (id) => {
    const result = await fetchJson(`/posts/${id}`, { method: "DELETE" });
    if (result.investorProfileDeleted && typeof window !== "undefined") {
      window.dispatchEvent(
        new CustomEvent("truehunt-investor-profile-deleted", {
          detail: { feedPostId: String(id) },
        }),
      );
    }
    return result;
  },
  deleteInvestorListing: async (id) => {
    const result = await fetchJson(
      `/investor-access/listings/${encodeURIComponent(id)}`,
      { method: "DELETE" },
    );
    if (typeof window !== "undefined") {
      window.dispatchEvent(
        new CustomEvent("truehunt-investor-profile-deleted", {
          detail: {
            investorListingId: String(id),
            feedPostId: result.feedPostId ? String(result.feedPostId) : "",
          },
        }),
      );
    }
    return result;
  },
  getAdminAccounts: () => fetchJson("/admin/accounts"),
  deleteAdminAccount: (id) =>
    fetchJson(`/admin/accounts/${encodeURIComponent(id)}`, {
      method: "DELETE",
    }),
  deleteAdminPost: (id) =>
    fetchJson(`/admin/posts/${encodeURIComponent(id)}`, { method: "DELETE" }),
  getAdminInvestorRequests: () => fetchJson("/admin/investor-requests"),
  approveAdminInvestorRequest: (id) =>
    fetchJson(`/admin/investor-requests/${encodeURIComponent(id)}/approve`, {
      method: "POST",
    }),
  rejectAdminInvestorRequest: (id) =>
    fetchJson(`/admin/investor-requests/${encodeURIComponent(id)}/reject`, {
      method: "POST",
    }),
  submitInvestorAccessRequest: (request) =>
    fetchJson("/investor-access/requests", { method: "POST", body: request }),
  getInvestorAccessStatus: () => fetchJson("/investor-access/status"),
  getInvestorListings: () => fetchJson("/investor-access/listings"),

  // Network & Connections
  getNetworkSuggestions: () => fetchJson("/connections/suggestions"),
  getInvitations: () => fetchJson("/connections/invitations"),
  getConnectedProfiles: () => fetchJson("/connections"),
  sendConnectionRequest: (profileId, message = "") =>
    fetchJson("/connections/request", {
      method: "POST",
      body: { profileId, message },
    }),
  acceptConnectionRequest: (profileId) =>
    fetchJson("/connections/accept", { method: "POST", body: { profileId } }),
  rejectConnectionRequest: (profileId) =>
    fetchJson("/connections/reject", { method: "POST", body: { profileId } }),
  cancelConnectionRequest: (profileId) =>
    fetchJson("/connections/cancel", { method: "POST", body: { profileId } }),
  connectTalent: (talentId, message = "") =>
    fetchJson("/connections", { method: "POST", body: { talentId, message } }),
  toggleConnection: (profileId) =>
    fetchJson("/connections/toggle", { method: "POST", body: { profileId } }),
  toggleFollow: (profileId) =>
    fetchJson("/connections/follow", { method: "POST", body: { profileId } }),
  requestContactAccess: (profileId) =>
    fetchJson("/contact-access/requests", {
      method: "POST",
      body: { profileId },
    }),
  getContactAccessStatus: (profileId) =>
    fetchJson(`/contact-access/${encodeURIComponent(profileId)}/status`),
  getIncomingContactAccessRequests: () =>
    fetchJson("/contact-access/requests/incoming"),
  getOutgoingContactAccessRequests: () =>
    fetchJson("/contact-access/requests/outgoing"),
  respondToContactAccessRequest: (requestId, decision) =>
    fetchJson(
      `/contact-access/requests/${encodeURIComponent(requestId)}/${decision}`,
      { method: "POST" },
    ),

  // Messaging & Conversations
  getConversations: (mailbox = "") =>
    fetchJson(`/conversations${mailbox ? `?mailbox=${mailbox}` : ""}`),
  getConversation: (id) => fetchJson(`/conversations/${id}`),
  sendMessage: (conversationId, content, attachment = null) =>
    fetchJson(`/conversations/${conversationId}/messages`, {
      method: "POST",
      body: { content, direction: "outgoing", attachment },
    }),
  editMessage: (conversationId, messageId, content) =>
    fetchJson(
      `/conversations/${encodeURIComponent(conversationId)}/messages/${encodeURIComponent(messageId)}`,
      { method: "PATCH", body: { content } },
    ),
  clearConversationMessages: (conversationId) =>
    fetchJson(`/conversations/${encodeURIComponent(conversationId)}/messages`, {
      method: "DELETE",
    }),
  createConversation: (recipient, messageText) =>
    fetchJson("/conversations", {
      method: "POST",
      body: { recipient, messageText },
    }),
  markConversationAsRead: (id) =>
    fetchJson(`/conversations/${id}/read`, { method: "PATCH" }),
  starConversation: (id) =>
    fetchJson(`/conversations/${id}/star`, { method: "PATCH" }),
  updateMailbox: (id, mailbox) =>
    fetchJson(`/conversations/${id}/mailbox`, {
      method: "PATCH",
      body: { mailbox },
    }),
  deleteConversation: (id) =>
    fetchJson(`/conversations/${id}`, { method: "DELETE" }),

  // Notifications / Endorsements
  getNotifications: () => fetchJson("/notifications"),
  markNotificationAsRead: (id) =>
    fetchJson(`/notifications/${id}/read`, { method: "PATCH" }),
  markAllNotificationsAsRead: () =>
    fetchJson("/notifications/read-all", { method: "PATCH" }),
  deleteNotification: (id) =>
    fetchJson(`/notifications/${id}`, { method: "DELETE" }),

  // Settings
  getSettings: () => fetchJson("/settings"),
  updateTheme: (theme) =>
    fetchJson("/settings/theme", { method: "PATCH", body: { theme } }),
  updateNotificationPreferences: (prefs) =>
    fetchJson("/settings/notifications", { method: "PATCH", body: prefs }),
  changePassword: (data) =>
    fetchJson("/settings/change-password", { method: "POST", body: data }),
  deleteAccount: () =>
    fetchJson("/settings/delete-account", { method: "POST" }),

  // Promotions
  clickPromotion: (campaign) =>
    fetchJson("/promotions/click", { method: "POST", body: { campaign } }),

  // Search
  search: (query) => fetchJson(`/search?q=${encodeURIComponent(query)}`),

  // Auth
  login: (credentials) =>
    fetchJson("/auth/login", { method: "POST", body: credentials }),
  register: (userData) =>
    fetchJson("/auth/register", { method: "POST", body: userData }),
  logout: () => fetchJson("/auth/logout", { method: "POST" }),
  getMe: () => fetchJson("/auth/me"),
};

export default api;
