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

export default function ConnectionsPage({ onBack }) {
  const [profiles, setProfiles] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

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
          <ul className="social-connections-list">
            {profiles.map((profile) => {
              const name = profile.name || 'TrueHunt member'
              const image = profile.profileImageUrl || profile.avatar || ''

              return (
                <li className="social-connection-item" key={profile.id || profile.userId || name}>
                  <span className={`social-connection-avatar${image ? '' : ' is-placeholder'}`}>
                    {image ? <img src={image} alt="" /> : getInitials(profile)}
                  </span>
                  <span className="social-connection-details">
                    <strong>{name}</strong>
                    {profile.role && <small>{profile.role}</small>}
                  </span>
                </li>
              )
            })}
          </ul>
        ) : (
          <p className="social-connections-message">You have no connections yet.</p>
        )}
      </section>
    </main>
  )
}
