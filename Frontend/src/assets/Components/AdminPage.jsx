import { useEffect, useMemo, useState } from 'react'
import { api as socialApi } from '../../api'
import { api as directoryApi } from '../../services/api'
import {
  Activity,
  FileText,
  LayoutDashboard,
  LogOut,
  Search,
  ShieldCheck,
  UsersRound,
} from 'lucide-react'
import './AdminPage.css'

const adminSections = [
  { id: 'overview', label: 'Overview', Icon: LayoutDashboard },
  { id: 'members', label: 'Members', Icon: UsersRound },
  { id: 'posts', label: 'Posts', Icon: FileText },
]

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
  const [email, setEmail] = useState('truehunt@gmail.com')
  const [password, setPassword] = useState('truehunt@2026')
  const [loginError, setLoginError] = useState('')
  const [isSigningIn, setIsSigningIn] = useState(false)
  const [activeSection, setActiveSection] = useState('overview')
  const [profiles, setProfiles] = useState([])
  const [posts, setPosts] = useState([])
  const [search, setSearch] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    if (!adminUser) return undefined

    let isCurrent = true
    async function loadDashboard() {
      setIsLoading(true)
      setLoadError('')
      try {
        const [profileData, postData] = await Promise.all([
          socialApi.getProfileDirectory(),
          socialApi.getPosts(),
        ])
        if (!Array.isArray(profileData) || !Array.isArray(postData)) {
          throw new Error('The admin dashboard received an invalid response.')
        }
        if (isCurrent) {
          setProfiles(profileData)
          setPosts(postData)
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
            {loginError && <p className="admin-error" role="alert">{loginError}</p>}
            <button type="submit" disabled={isSigningIn}>
              {isSigningIn ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
          <p className="admin-login-note">Administrator access is verified by the sign-in service.</p>
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
          <button type="button" className="admin-signout" onClick={handleSignOut} aria-label="Sign out">
            <LogOut size={17} aria-hidden="true" />
          </button>
        </div>
      </aside>

      <section className="admin-content">
        <header className="admin-topbar">
          <div>
            <p className="admin-eyebrow">PLATFORM MANAGEMENT</p>
            <h1>{activeLabel}</h1>
          </div>
          <div className="admin-topbar-user">
            <span className="admin-avatar">{getInitials(getDisplayName(adminUser))}</span>
            <span>{getDisplayName(adminUser)}</span>
          </div>
        </header>

        <div className="admin-workspace">
          {loadError && (
            <div className="admin-error admin-data-error" role="alert">
              {loadError}
              <button type="button" onClick={() => setAdminUser({ ...adminUser })}>Retry</button>
            </div>
          )}
          {isLoading && <p className="admin-loading" role="status">Loading platform data…</p>}

          {activeSection === 'overview' && (
            <>
              <div className="admin-welcome">
                <div>
                  <h2>Social platform overview</h2>
                  <p>Review member profiles and recent community posts.</p>
                </div>
                <span><Activity size={17} aria-hidden="true" /> Read-only view</span>
              </div>
              <div className="admin-stats-grid">
                <article className="admin-stat-card">
                  <span className="admin-stat-icon is-violet"><UsersRound size={19} aria-hidden="true" /></span>
                  <p>Total members</p><strong>{profiles.length}</strong><small>Profiles in the directory</small>
                </article>
                <article className="admin-stat-card">
                  <span className="admin-stat-icon is-coral"><FileText size={19} aria-hidden="true" /></span>
                  <p>Community posts</p><strong>{posts.length}</strong><small>Posts available in the feed</small>
                </article>
                <article className="admin-stat-card">
                  <span className="admin-stat-icon is-green"><Activity size={19} aria-hidden="true" /></span>
                  <p>Account status</p><strong className="admin-status-value">Active</strong><small>Signed in as an administrator</small>
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
                <div><h2>Member directory</h2><p>{filteredProfiles.length} profiles</p></div>
                <label className="admin-search">
                  <Search size={16} aria-hidden="true" />
                  <input aria-label="Search members" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search members" />
                </label>
              </header>
              <div className="admin-table-wrap">
                <table className="admin-table">
                  <thead><tr><th>Member</th><th>Email</th><th>Craft / headline</th><th>Status</th></tr></thead>
                  <tbody>
                    {filteredProfiles.map((profile, index) => {
                      const name = getDisplayName(profile)
                      return (
                        <tr key={profile.userId || profile.id || profile.email || `${name}-${index}`}>
                          <td><span className="admin-table-person"><span className="admin-avatar">{getInitials(name)}</span><strong>{name}</strong></span></td>
                          <td>{profile.email || '—'}</td>
                          <td>{profile.headline || profile.primaryCraft || profile.role || '—'}</td>
                          <td><span className="admin-pill">{profile.status || profile.accountStatus || 'Member'}</span></td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
                {!filteredProfiles.length && <p className="admin-empty">No matching members.</p>}
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
                    <strong>{post.authorName || 'Community member'}</strong>
                    <p>{getPostText(post)}</p>
                    <small>{formatDate(post.createdAt || post.timestamp || post.date)}</small>
                  </article>
                ))}
                {!filteredPosts.length && <p className="admin-empty">No matching posts.</p>}
              </div>
            </section>
          )}
          <p className="admin-footer-note">This dashboard is read-only. User and post management actions require administrator endpoints on the server.</p>
        </div>
      </section>
    </main>
  )
}
