import { useEffect, useState } from 'react'
import { ArrowLeft, UsersRound } from 'lucide-react'
import api from '../../api'
import './StylePage.css'

function getProfileName(profile) {
  return String(profile?.name || profile?.fullName || profile?.displayName || '').trim() || 'Profile'
}

function getProfileId(profile) {
  return String(
    profile?.userId
      || profile?.publicUserId
      || profile?.profileId
      || profile?.id
      || profile?.user_id
      || profile?._id
      || profile?.email
      || '',
  )
}

function getInitials(profile) {
  if (profile?.initials) return profile.initials
  return getProfileName(profile)
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
}

export default function ProfileConnectionsPage({
  isActive,
  profileId,
  profileName,
  initialTab = 'followers',
  showMutualConnections = true,
  onBack,
  onMessage,
  onProfileSelect,
}) {
  const [activeTab, setActiveTab] = useState(initialTab === 'mutual' ? 'mutual' : 'followers')
  const [profilesByTab, setProfilesByTab] = useState({ followers: null, mutual: null })
  const [loadingTab, setLoadingTab] = useState('')
  const [errorsByTab, setErrorsByTab] = useState({ followers: '', mutual: '' })
  const profiles = profilesByTab[activeTab] || []
  const title = activeTab === 'followers' ? 'Followers' : 'Mutual connections'

  useEffect(() => {
    if (!isActive || !profileId || profilesByTab[activeTab] !== null) return undefined

    let isCurrent = true
    setLoadingTab(activeTab)
    setErrorsByTab((current) => ({ ...current, [activeTab]: '' }))
    const request = activeTab === 'followers'
      ? api.getProfileFollowers(profileId)
      : api.getProfileMutualConnections(profileId)

    request
      .then((result) => {
        if (!Array.isArray(result)) {
          throw new Error(`The ${title.toLowerCase()} response was invalid.`)
        }
        if (isCurrent) {
          setProfilesByTab((current) => ({ ...current, [activeTab]: result }))
        }
      })
      .catch((error) => {
        console.error(`Could not load profile ${title.toLowerCase()}:`, error)
        if (isCurrent) {
          setErrorsByTab((current) => ({
            ...current,
            [activeTab]: `Could not load ${title.toLowerCase()}. Please try again.`,
          }))
        }
      })
      .finally(() => {
        if (isCurrent) setLoadingTab('')
      })

    return () => {
      isCurrent = false
    }
  }, [activeTab, isActive, profileId, profilesByTab, title])

  function retryLoad() {
    setProfilesByTab((current) => ({ ...current, [activeTab]: null }))
    setErrorsByTab((current) => ({ ...current, [activeTab]: '' }))
  }

  return (
    <main className="social-connections-page profile-social-connections-page">
      <button className="social-connections-back" type="button" onClick={onBack}>
        <ArrowLeft size={17} aria-hidden="true" />
        Back to profile
      </button>
      <section className="social-connections-card" aria-labelledby="profile-social-connections-title">
        <header className="social-connections-header">
          <span className="social-connections-heading-icon">
            <UsersRound size={20} aria-hidden="true" />
          </span>
          <div>
            <h1 id="profile-social-connections-title">{title}</h1>
            <p>{profileName || 'Profile'}</p>
          </div>
        </header>
        <div className="profile-social-tabs" role="tablist" aria-label="Profile connections">
          <button
            id="profile-followers-tab"
            className={activeTab === 'followers' ? 'is-active' : ''}
            type="button"
            role="tab"
            aria-selected={activeTab === 'followers'}
            aria-controls="profile-social-connections-panel"
            onClick={() => setActiveTab('followers')}
          >
            Followers
            {profilesByTab.followers && <span>{profilesByTab.followers.length}</span>}
          </button>
          {showMutualConnections && (
            <button
              id="profile-mutual-tab"
              className={activeTab === 'mutual' ? 'is-active' : ''}
              type="button"
              role="tab"
              aria-selected={activeTab === 'mutual'}
              aria-controls="profile-social-connections-panel"
              onClick={() => setActiveTab('mutual')}
            >
              Mutual connections
              {profilesByTab.mutual && <span>{profilesByTab.mutual.length}</span>}
            </button>
          )}
        </div>
        <div
          id="profile-social-connections-panel"
          role="tabpanel"
          aria-labelledby={activeTab === 'followers' ? 'profile-followers-tab' : 'profile-mutual-tab'}
        >
          {errorsByTab[activeTab] ? (
            <div className="social-connections-message" role="alert">
              <p>{errorsByTab[activeTab]}</p>
              <button className="profile-social-retry-button" type="button" onClick={retryLoad}>
                Try again
              </button>
            </div>
          ) : loadingTab === activeTab ? (
            <p className="social-connections-message" role="status">Loading {title.toLowerCase()}…</p>
          ) : profiles.length ? (
            <ul className="social-connections-list">
              {profiles.map((profile) => {
                const name = getProfileName(profile)
                const id = getProfileId(profile) || name
                const image = profile.profileImageUrl || profile.avatar || ''
                const role = profile.role || profile.headline || profile.subCraft || profile.primaryCraft || ''
                return (
                  <li className="social-connection-item" key={id}>
                    <button
                      className={`social-connection-avatar social-connection-avatar-button${image ? '' : ' is-placeholder'}`}
                      type="button"
                      aria-label={`View ${name}'s profile`}
                      onClick={() => onProfileSelect?.(profile)}
                    >
                      {image ? <img src={image} alt="" /> : getInitials(profile)}
                    </button>
                    <button
                      className="social-connection-details profile-social-profile-button"
                      type="button"
                      onClick={() => onProfileSelect?.(profile)}
                    >
                      <strong>{name}</strong>
                      {role && <small>{role}</small>}
                    </button>
                    <div className="social-connection-actions">
                      <button
                        className="social-connection-action-button is-message"
                        type="button"
                        onClick={() => onMessage?.(profile)}
                      >
                        Message
                      </button>
                    </div>
                  </li>
                )
              })}
            </ul>
          ) : (
            <p className="social-connections-message">
              {activeTab === 'followers' ? 'No followers yet.' : 'No mutual connections yet.'}
            </p>
          )}
        </div>
      </section>
    </main>
  )
}
