import { useEffect, useMemo, useState } from 'react'
import { api as socialApi } from '../../api'
import { api as directoryApi } from '../../services/api'
import {
  Activity,
  Check,
  FileText,
  LayoutDashboard,
  LogOut,
  Search,
  ShieldCheck,
  Trash2,
  UsersRound,
  X,
} from 'lucide-react'
import './AdminPage.css'

const adminSections = [
  { id: 'overview', label: 'Overview', Icon: LayoutDashboard },
  { id: 'investors', label: 'Investor requests', Icon: ShieldCheck },
  { id: 'members', label: 'Members', Icon: UsersRound },
  { id: 'posts', label: 'Posts', Icon: FileText },
]

const demoAdminCredentials = import.meta.env.DEV
  ? { email: 'truehunt@gmail.com', password: 'truehunt@2026' }
  : null

function getDisplayName(profile) {
  return String(profile?.name || profile?.authorName || 'Unnamed member')
}

function getInitials(name) {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'TH'
}

function getPostText(post) {
  return [post.content, post.text, post.body, post.caption, post.description, post.title]
    .find((value) => typeof value === 'string' && value.trim())?.trim() || 'No text content'
}

function formatDate(value) {
  if (!value) return 'Date unavailable'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? 'Date unavailable'
    : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date)
}

export default function AdminPage() {
  const [adminUser, setAdminUser] = useState(null)
  const [email, setEmail] = useState(demoAdminCredentials?.email || '')
  const [password, setPassword] = useState(demoAdminCredentials?.password || '')
  const [loginError, setLoginError] = useState('')
  const [isSigningIn, setIsSigningIn] = useState(false)
  const [activeSection, setActiveSection] = useState('overview')
  const [profiles, setProfiles] = useState([])
  const [posts, setPosts] = useState([])
  const [investorRequests, setInvestorRequests] = useState([])
  const [search, setSearch] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [deletingAccountId, setDeletingAccountId] = useState(null)
  const [deletingPostId, setDeletingPostId] = useState(null)
  const [reviewingInvestorRequestId, setReviewingInvestorRequestId] = useState(null)

  useEffect(() => {
    if (!adminUser) return undefined

    let isCurrent = true
    async function loadDashboard() {
      setIsLoading(true)
      setLoadError('')
      try {
        const [accountData, postData, investorRequestData] = await Promise.all([
          socialApi.getAdminAccounts(),
          socialApi.getPosts(),
          socialApi.getAdminInvestorRequests(),
        ])
        if (!Array.isArray(accountData) || !Array.isArray(postData) || !Array.isArray(investorRequestData)) {
          throw new Error('The admin dashboard received an invalid response.')
        }
        if (isCurrent) {
          setProfiles(accountData)
          setPosts(postData)
          setInvestorRequests(investorRequestData)
        }
      } catch (error) {
        console.error('Could not load admin dashboard data:', error)
        if (isCurrent) setLoadError(error.message || 'Could not load dashboard data. Please try again.')
      } finally {
        if (isCurrent) setIsLoading(false)
      }
    }

    loadDashboard()
    return () => {
      isCurrent = false
    }
  }, [adminUser])

  async function handleDeleteAccount(account) {
    if (!account?.id || !account.email) {
      setActionError('This account is missing the information required for deletion.')
      return
    }
    if (account.email.toLowerCase() === String(adminUser?.email || '').toLowerCase()) return
    if (!window.confirm(`Permanently delete ${account.name || account.email}'s account, profile, and posts?`)) return

    setActionError('')
    setDeletingAccountId(account.id)
    try {
      await socialApi.deleteAdminAccount(account.id)
      setProfiles((current) => current.filter((entry) => entry.id !== account.id))
    } catch (error) {
      console.error('Could not delete member account:', error)
      setActionError(error.message || 'Could not delete this account. Please try again.')
    } finally {
      setDeletingAccountId(null)
    }
  }

  async function handleDeletePost(post) {
    if (post?.id == null) {
      setActionError('This post is missing the information required for deletion.')
      return
    }
    if (!window.confirm('Permanently delete this post?')) return

    setActionError('')
    setDeletingPostId(post.id)
    try {
      await socialApi.deleteAdminPost(post.id)
      setPosts((current) => current.filter((entry) => String(entry.id) !== String(post.id)))
    } catch (error) {
      console.error('Could not delete post:', error)
      setActionError(error.message || 'Could not delete this post. Please try again.')
    } finally {
      setDeletingPostId(null)
    }
  }

  async function handleInvestorRequestReview(request, decision) {
    if (request?.id == null || reviewingInvestorRequestId != null) return

    setActionError('')
    setReviewingInvestorRequestId(request.id)
    try {
      const updatedRequest = decision === 'approve'
        ? await socialApi.approveAdminInvestorRequest(request.id)
        : await socialApi.rejectAdminInvestorRequest(request.id)
      setInvestorRequests((current) => current.map((entry) => (
        String(entry.id) === String(updatedRequest.id) ? updatedRequest : entry
      )))
    } catch (error) {
      console.error(`Could not ${decision} investor access request:`, error)
      setActionError(error.message || `Could not ${decision} this investor access request. Please try again.`)
    } finally {
      setReviewingInvestorRequestId(null)
    }
  }

  const filteredProfiles = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return profiles
    return profiles.filter((profile) => [
      profile.name,
      profile.email,
      profile.userId,
      profile.primaryCraft,
      profile.headline,
    ].some((value) => String(value || '').toLowerCase().includes(query)))
  }, [profiles, search])

  const filteredPosts = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return posts
    return posts.filter((post) => (
      getPostText(post).toLowerCase().includes(query)
      || String(post.authorName || '').toLowerCase().includes(query)
    ))
  }, [posts, search])

  const filteredInvestorRequests = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return investorRequests
    return investorRequests.filter((request) => [
      request.name,
      request.userId,
      request.email,
      request.mobileNumber,
      request.budget,
      request.role,
      request.description,
      request.status,
    ].some((value) => String(value || '').toLowerCase().includes(query)))
  }, [investorRequests, search])

  async function handleSignIn(event) {
    event.preventDefault()
    setLoginError('')
    setIsSigningIn(true)
    try {
      const response = await directoryApi.auth.login(email.trim(), password)
      const account = response?.data
      const signedInUser = account?.user || account
      const role = String(account?.role || signedInUser?.role || '').toUpperCase()
      if (!account?.token || role !== 'ADMIN') {
        setLoginError('This account does not have administrator access.')
        return
      }

      const user = {
        ...signedInUser,
        id: signedInUser.id || account.userId,
        userId: signedInUser.userId || account.userId,
        email: signedInUser.email || account.email || email.trim(),
        role,
      }
      localStorage.setItem('truehunt_token', account.token)
      localStorage.setItem('truehunt_user', JSON.stringify(user))
      setAdminUser(user)
      setPassword('')
    } catch (error) {
      setLoginError(error.message || 'Could not sign in. Please check your details and try again.')
    } finally {
      setIsSigningIn(false)
    }
  }

  function handleSignOut() {
    localStorage.removeItem('truehunt_token')
    localStorage.removeItem('truehunt_user')
    setAdminUser(null)
    setProfiles([])
    setPosts([])
    setSearch('')
    setPassword('')
  }

  if (!adminUser) {
    return (
      <main className="admin-login-page">
        <section className="admin-login-card" aria-labelledby="admin-login-title">
          <div className="admin-brand-mark"><ShieldCheck size={24} aria-hidden="true" /></div>
          <p className="admin-eyebrow">TRUEHUNT CONTROL CENTER</p>
          <h1 id="admin-login-title">Administrator sign in</h1>
          <p className="admin-login-description">Sign in with an administrator account to view platform activity.</p>
          <form className="admin-login-form" onSubmit={handleSignIn}>
            <label htmlFor="admin-email">Email address</label>
            <input
              id="admin-email"
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
            <label htmlFor="admin-password">Password</label>
            <input
              id="admin-password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
            {demoAdminCredentials && (
              <div className="admin-demo-credentials">
                <strong>Demo credentials (development only)</strong>
                <span>Email: {demoAdminCredentials.email}</span>
                <span>Password: {demoAdminCredentials.password}</span>
              </div>
            )}
            {loginError && <p className="admin-error" role="alert">{loginError}</p>}
            <button type="submit" disabled={isSigningIn}>
              {isSigningIn ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
          <p className="admin-login-note">
            {demoAdminCredentials
              ? 'The development admin account is created when the backend starts with the dev profile. Use npm run backend or mvn spring-boot:run.'
              : 'Administrator access is verified by the sign-in service.'}
          </p>
        </section>
      </main>
    )
  }

  const activeLabel = adminSections.find((section) => section.id === activeSection)?.label || 'Overview'
  const shownProfiles = activeSection === 'members' ? filteredProfiles : profiles.slice(0, 6)
  const shownPosts = activeSection === 'posts' ? filteredPosts : posts.slice(0, 5)

  return (
    <main className="admin-app">
      <aside className="admin-sidebar">
        <a className="admin-logo" href="/admin" aria-label="TrueHunt admin home">
          <span className="admin-logo-icon"><ShieldCheck size={21} aria-hidden="true" /></span>
          <span>TrueHunt<small>ADMIN</small></span>
        </a>
        <p className="admin-nav-heading">WORKSPACE</p>
        <nav className="admin-nav" aria-label="Admin sections">
          {adminSections.map(({ id, label, Icon }) => (
            <button
              className={`admin-nav-item${activeSection === id ? ' is-active' : ''}`}
              key={id}
              type="button"
              aria-current={activeSection === id ? 'page' : undefined}
              onClick={() => {
                setActiveSection(id)
                setSearch('')
              }}
            >
              <Icon size={18} aria-hidden="true" />
              {label}
            </button>
          ))}
        </nav>
        <div className="admin-sidebar-footer">
          <span className="admin-avatar">{getInitials(getDisplayName(adminUser))}</span>
          <span className="admin-user-label">
            <strong>{getDisplayName(adminUser)}</strong>
            <small>Administrator</small>
          </span>
        </div>
      </aside>

      <section className="admin-content">
        <header className="admin-topbar">
          <div>
            <p className="admin-eyebrow">PLATFORM MANAGEMENT</p>
            <h1>{activeLabel}</h1>
          </div>
          <div className="admin-topbar-user">
            <span className="admin-topbar-identity">
              <span className="admin-avatar">{getInitials(getDisplayName(adminUser))}</span>
              <span className="admin-topbar-user-name">{getDisplayName(adminUser)}</span>
            </span>
            <button type="button" className="admin-profile-logout" onClick={handleSignOut}>
              <LogOut size={15} aria-hidden="true" />
              <span>Logout</span>
            </button>
          </div>
        </header>

        <div className="admin-workspace">
          {loadError && (
            <div className="admin-error admin-data-error" role="alert">
              {loadError}
              <button type="button" onClick={() => setAdminUser({ ...adminUser })}>Retry</button>
            </div>
          )}
          {actionError && <p className="admin-error admin-action-error" role="alert">{actionError}</p>}
          {isLoading && <p className="admin-loading" role="status">Loading platform data…</p>}

          {activeSection === 'overview' && (
            <>
              <div className="admin-welcome">
                <div>
                  <h2>Social platform overview</h2>
                  <p>Review member profiles and recent community posts.</p>
                </div>
                <span><Activity size={17} aria-hidden="true" /> Management enabled</span>
              </div>
              <div className="admin-stats-grid">
                <article className="admin-stat-card">
                  <span className="admin-stat-icon is-violet"><UsersRound size={19} aria-hidden="true" /></span>
                  <p>Total members</p><strong>{profiles.length}</strong><small>Registered accounts</small>
                </article>
                <article className="admin-stat-card">
                  <span className="admin-stat-icon is-coral"><FileText size={19} aria-hidden="true" /></span>
                  <p>Community posts</p><strong>{posts.length}</strong><small>Posts available in the feed</small>
                </article>
                <article className="admin-stat-card">
                  <span className="admin-stat-icon is-green"><Activity size={19} aria-hidden="true" /></span>
                  <p>Account status</p><strong className="admin-status-value">Active</strong><small>Signed in as an administrator</small>
                </article>
                <article className="admin-stat-card">
                  <span className="admin-stat-icon is-violet"><ShieldCheck size={19} aria-hidden="true" /></span>
                  <p>Pending investor requests</p>
                  <strong>{investorRequests.filter((request) => request.status === 'PENDING').length}</strong>
                  <small>Waiting for review</small>
                </article>
              </div>
              <div className="admin-content-grid">
                <section className="admin-panel">
                  <header className="admin-panel-header">
                    <div><h2>Recent members</h2><p>Profiles from the member directory</p></div>
                    <button type="button" onClick={() => setActiveSection('members')}>View all</button>
                  </header>
                  <div className="admin-member-list">
                    {shownProfiles.length ? shownProfiles.map((profile, index) => {
                      const name = getDisplayName(profile)
                      return (
                        <article className="admin-member-row" key={profile.userId || profile.id || profile.email || `${name}-${index}`}>
                          <span className="admin-avatar">{getInitials(name)}</span>
                          <span className="admin-member-info"><strong>{name}</strong><small>{profile.email || profile.headline || profile.primaryCraft || 'Member'}</small></span>
                          <span className="admin-member-status">Member</span>
                        </article>
                      )
                    }) : <p className="admin-empty">No member profiles found.</p>}
                  </div>
                </section>
                <section className="admin-panel">
                  <header className="admin-panel-header">
                    <div><h2>Recent posts</h2><p>Latest community activity</p></div>
                    <button type="button" onClick={() => setActiveSection('posts')}>View all</button>
                  </header>
                  <div className="admin-post-list">
                    {shownPosts.length ? shownPosts.map((post, index) => (
                      <article className="admin-post-row" key={post.id || `${post.authorName}-${index}`}>
                        <strong>{post.authorName || 'Community member'}</strong>
                        <p>{getPostText(post)}</p>
                        <small>{formatDate(post.createdAt || post.timestamp || post.date)}</small>
                      </article>
                    )) : <p className="admin-empty">No community posts found.</p>}
                  </div>
                </section>
              </div>
            </>
          )}

          {activeSection === 'members' && (
            <section className="admin-panel admin-directory-panel">
              <header className="admin-panel-header">
                <div><h2>Member accounts</h2><p>{filteredProfiles.length} accounts</p></div>
                <label className="admin-search">
                  <Search size={16} aria-hidden="true" />
                  <input aria-label="Search members" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search members" />
                </label>
              </header>
              <div className="admin-table-wrap">
                <table className="admin-table">
                  <thead><tr><th>Member</th><th>Email</th><th>Craft / headline</th><th>Role</th><th>Actions</th></tr></thead>
                  <tbody>
                    {filteredProfiles.map((profile, index) => {
                      const name = getDisplayName(profile)
                      const isCurrentAdmin = String(profile.email || '').toLowerCase()
                        === String(adminUser.email || '').toLowerCase()
                      return (
                        <tr key={profile.id || profile.userId || profile.email || `${name}-${index}`}>
                          <td><span className="admin-table-person"><span className="admin-avatar">{getInitials(name)}</span><strong>{name}</strong></span></td>
                          <td>{profile.email || '—'}</td>
                          <td>{profile.primaryCraft || profile.role || '—'}</td>
                          <td><span className="admin-pill">{profile.role || 'Member'}</span></td>
                          <td>
                            {isCurrentAdmin ? <span className="admin-current-account">Current admin</span> : (
                              <button
                                type="button"
                                className="admin-delete-button"
                                onClick={() => handleDeleteAccount(profile)}
                                disabled={deletingAccountId === profile.id}
                                aria-label={`Delete ${name}'s account`}
                              >
                                <Trash2 size={14} aria-hidden="true" />
                                {deletingAccountId === profile.id ? 'Deleting…' : 'Delete'}
                              </button>
                            )}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
                {!filteredProfiles.length && <p className="admin-empty">No matching members.</p>}
              </div>
            </section>
          )}

          {activeSection === 'investors' && (
            <section className="admin-panel admin-directory-panel">
              <header className="admin-panel-header">
                <div><h2>Investor access requests</h2><p>{filteredInvestorRequests.length} requests</p></div>
                <label className="admin-search">
                  <Search size={16} aria-hidden="true" />
                  <input aria-label="Search investor requests" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search requests" />
                </label>
              </header>
              <div className="admin-table-wrap">
                <table className="admin-table">
                  <thead><tr><th>Applicant</th><th>Role</th><th>Description</th><th>User ID</th><th>Mobile</th><th>Budget</th><th>Submitted</th><th>Status / actions</th></tr></thead>
                  <tbody>
                    {filteredInvestorRequests.map((request) => (
                      <tr key={request.id}>
                        <td>
                          <strong>{request.name || 'Unnamed applicant'}</strong>
                          <br />
                          <small>{request.email || '—'}</small>
                        </td>
                        <td>{request.role || '—'}</td>
                        <td>{request.description || '—'}</td>
                        <td>{request.userId || '—'}</td>
                        <td>{request.mobileNumber || '—'}</td>
                        <td>{request.budget || '—'}</td>
                        <td>{formatDate(request.createdAt)}</td>
                        <td>
                          {request.status === 'PENDING' ? (
                            <span className="admin-investor-actions">
                              <button
                                type="button"
                                className="admin-investor-review-button is-approve"
                                onClick={() => handleInvestorRequestReview(request, 'approve')}
                                disabled={reviewingInvestorRequestId != null}
                              >
                                <Check size={14} aria-hidden="true" />
                                {reviewingInvestorRequestId === request.id ? 'Saving…' : 'Approve'}
                              </button>
                              <button
                                type="button"
                                className="admin-investor-review-button is-reject"
                                onClick={() => handleInvestorRequestReview(request, 'reject')}
                                disabled={reviewingInvestorRequestId != null}
                              >
                                <X size={14} aria-hidden="true" />
                                Reject
                              </button>
                            </span>
                          ) : <span className="admin-pill">{request.status}</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {!filteredInvestorRequests.length && <p className="admin-empty">No investor access requests found.</p>}
              </div>
            </section>
          )}

          {activeSection === 'posts' && (
            <section className="admin-panel admin-directory-panel">
              <header className="admin-panel-header">
                <div><h2>Community posts</h2><p>{filteredPosts.length} posts</p></div>
                <label className="admin-search">
                  <Search size={16} aria-hidden="true" />
                  <input aria-label="Search posts or authors" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search posts or authors" />
                </label>
              </header>
              <div className="admin-post-list admin-all-posts">
                {filteredPosts.map((post, index) => (
                  <article className="admin-post-row" key={post.id || `${post.authorName}-${index}`}>
                    <div className="admin-post-row-heading">
                      <strong>{post.authorName || 'Community member'}</strong>
                      <button
                        type="button"
                        className="admin-delete-button"
                        onClick={() => handleDeletePost(post)}
                        disabled={deletingPostId === post.id}
                        aria-label={`Delete post by ${post.authorName || 'community member'}`}
                      >
                        <Trash2 size={14} aria-hidden="true" />
                        {deletingPostId === post.id ? 'Deleting…' : 'Delete'}
                      </button>
                    </div>
                    <p>{getPostText(post)}</p>
                    <small>{formatDate(post.createdAt || post.timestamp || post.date)}</small>
                  </article>
                ))}
                {!filteredPosts.length && <p className="admin-empty">No matching posts.</p>}
              </div>
            </section>
          )}
          <p className="admin-footer-note">Account and post deletions are permanent. Investor access requests require administrator review.</p>
        </div>
      </section>
    </main>
  )
}
