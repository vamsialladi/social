import { useEffect, useState } from 'react'
import { ArrowLeft, UsersRound } from 'lucide-react'
import api from '../../api'
import './StylePage.css'

function getInitials(profile) {
  if (profile.initials) return profile.initials
  return String(profile.name || 'Member')
    .split(/\s+/)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
}

export default function ConnectionsPage({ onBack, onProfileSelect }) {
  const [profiles, setProfiles] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [updatingProfileId, setUpdatingProfileId] = useState('')

  useEffect(() => {
    function handleConnectionChanged(event) {
      const { connected, profile } = event.detail || {}
      const profileId = String(
        profile?.userId || profile?.id || profile?.user_id || profile?._id || '',
      )
      if (!profileId) return

      setProfiles((currentProfiles) => {
        if (!connected) {
          return currentProfiles.filter((item) => String(
            item.userId || item.id || item.user_id || item._id || '',
          ) !== profileId)
        }
        if (currentProfiles.some((item) => String(
          item.userId || item.id || item.user_id || item._id || '',
        ) === profileId)) {
          return currentProfiles
        }
        return [...currentProfiles, profile]
      })
    }

    window.addEventListener('truehunt-connection-changed', handleConnectionChanged)
    return () => window.removeEventListener('truehunt-connection-changed', handleConnectionChanged)
  }, [])

  useEffect(() => {
    let isCurrent = true
    async function loadConnections() {
      try {
        const data = await api.getConnectedProfiles()
        if (isCurrent) setProfiles(Array.isArray(data) ? data : [])
      } catch (error) {
        console.error('Failed to load connections page:', error)
        if (isCurrent) setLoadError('Could not load your connections. Please try again later.')
      } finally {
        if (isCurrent) setIsLoading(false)
      }
    }

    loadConnections()
    return () => {
      isCurrent = false
    }
  }, [])

  async function disconnectProfile(profile) {
    const profileId = String(
      profile.userId || profile.id || profile.user_id || profile._id || '',
    )
    if (!profileId || updatingProfileId) return

    setActionError('')
    setUpdatingProfileId(profileId)
    try {
      await api.toggleConnection(profileId)
      setProfiles((currentProfiles) => currentProfiles.filter((item) => String(
        item.userId || item.id || item.user_id || item._id || '',
      ) !== profileId))
      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: { connected: false, profile: { ...profile, id: profileId } },
      }))
    } catch (error) {
      console.error('Failed to disconnect profile:', error)
      setActionError(`Could not disconnect from ${profile.name || 'this person'}. Please try again.`)
    } finally {
      setUpdatingProfileId('')
    }
  }

  return (
    <main className="social-connections-page">
      <button className="social-connections-back" type="button" onClick={onBack}>
        <ArrowLeft size={17} aria-hidden="true" />
        Back to profile
      </button>
      <section className="social-connections-card" aria-labelledby="social-connections-title">
        <header className="social-connections-header">
          <span className="social-connections-heading-icon">
            <UsersRound size={20} aria-hidden="true" />
          </span>
          <div>
            <h1 id="social-connections-title">Connections</h1>
            <p>{isLoading ? 'Loading your connections…' : `${profiles.length} connections`}</p>
          </div>
        </header>
        {loadError ? (
          <p className="social-connections-message" role="alert">{loadError}</p>
        ) : isLoading ? (
          <p className="social-connections-message" role="status">Loading your connections…</p>
        ) : profiles.length ? (
          <>
            {actionError && <p className="social-connections-message" role="alert">{actionError}</p>}
            <ul className="social-connections-list">
              {profiles.map((profile) => {
                const name = profile.name || 'TrueHunt member'
                const image = profile.profileImageUrl || profile.avatar || ''
                const profileId = String(
                  profile.userId || profile.id || profile.user_id || profile._id || '',
                )

                return (
                  <li className="social-connection-item" key={profileId || name}>
                    <button
                      className={`social-connection-avatar social-connection-avatar-button${image ? '' : ' is-placeholder'}`}
                      type="button"
                      aria-label={`View ${name}'s profile`}
                      onClick={() => onProfileSelect?.({
                        ...profile,
                        id: profileId,
                        userId: profile.userId || profile.user_id || profileId,
                        isConnected: true,
                      })}
                    >
                      {image ? <img src={image} alt="" /> : getInitials(profile)}
                    </button>
                    <span
                      className="social-connection-details"
                      style={{ cursor: 'pointer' }}
                      role="button"
                      tabIndex={0}
                      onClick={() => onProfileSelect?.({
                        ...profile,
                        id: profileId,
                        userId: profile.userId || profile.user_id || profileId,
                        isConnected: true,
                      })}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          onProfileSelect?.({
                            ...profile,
                            id: profileId,
                            userId: profile.userId || profile.user_id || profileId,
                            isConnected: true,
                          })
                        }
                      }}
                    >
                      <strong>{name}</strong>
                      {profile.role && <small>{profile.role}</small>}
                    </span>
                    <button
                      className="social-connection-connected-button"
                      type="button"
                      disabled={Boolean(updatingProfileId)}
                      onClick={() => disconnectProfile(profile)}
                    >
                      {updatingProfileId === profileId ? 'Updating…' : 'Remove Connection'}
                    </button>
                  </li>
                )
              })}
            </ul>
          </>
        ) : (
          <p className="social-connections-message">You have no connections yet.</p>
        )}
      </section>
    </main>
  )
}
