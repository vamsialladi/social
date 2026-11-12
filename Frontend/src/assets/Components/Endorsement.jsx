import { useEffect, useState } from 'react'
import './Endorsement.css'
import api from '../../api'
import ProfileBanner from './ProfileBanner.jsx'

function Notifications({
  profileDetails,
  profilePreview,
  onViewProfile,
  onManageNotifications,
  isActive,
}) {
  const [endorsements, setEndorsements] = useState([])
  const [contactRequests, setContactRequests] = useState([])
  const [requestError, setRequestError] = useState('')
  const [respondingToRequestId, setRespondingToRequestId] = useState('')
  const name = profileDetails.name.trim() || 'Your name'
  const languages = [
    ...profileDetails.languages.filter((language) => language !== 'Other'),
    ...(profileDetails.otherLanguage ? [profileDetails.otherLanguage] : []),
  ]
  const profileTags = [...languages, ...profileDetails.workTypes]

  useEffect(() => {
    if (!isActive) return undefined

    let isCurrent = true
    async function loadEndorsements() {
      const [notificationsResult, requestsResult] = await Promise.allSettled([
        api.getNotifications(),
        api.getIncomingContactAccessRequests(),
      ])
      if (!isCurrent) return

      if (notificationsResult.status === 'fulfilled' && Array.isArray(notificationsResult.value)) {
        setEndorsements(notificationsResult.value)
      } else if (notificationsResult.status === 'rejected') {
        console.warn('Backend notifications load error:', notificationsResult.reason)
      }

      if (requestsResult.status === 'fulfilled' && Array.isArray(requestsResult.value)) {
        setContactRequests(requestsResult.value)
      } else if (requestsResult.status === 'rejected') {
        console.warn('Contact access requests load error:', requestsResult.reason)
        setRequestError('Could not load contact requests. Please try again later.')
      }
    }
    loadEndorsements()
    const refreshOnFocus = () => {
      if (!document.hidden) loadEndorsements()
    }
    const intervalId = window.setInterval(loadEndorsements, 30_000)
    window.addEventListener('focus', refreshOnFocus)
    return () => {
      isCurrent = false
      window.clearInterval(intervalId)
      window.removeEventListener('focus', refreshOnFocus)
    }
  }, [isActive])

  async function respondToContactRequest(requestId, decision) {
    if (respondingToRequestId) return
    setRespondingToRequestId(requestId)
    setRequestError('')
    try {
      await api.respondToContactAccessRequest(requestId, decision)
      setContactRequests((current) => current.filter((request) => request.id !== requestId))
    } catch (error) {
      console.error(`Could not ${decision} contact access request:`, error)
      setRequestError(error.message || 'Could not update this request. Please try again.')
    } finally {
      setRespondingToRequestId('')
    }
  }

  return (
    <section className="notifications-page" aria-label="Notifications page">
      <aside className="notifications-sidebar" aria-label="Your profile summary">
        <section
          className="notifications-profile-card"
          role="button"
          tabIndex={0}
          aria-label={`View ${name}'s profile`}
          onClick={onViewProfile}
          onKeyDown={(event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault()
              onViewProfile()
            }
          }}
        >
          <div className="notifications-profile-cover">
            <ProfileBanner variant="card" />
          </div>
          <div className="notifications-profile-content">
            <div className="notifications-avatar" aria-hidden="true">
              {profilePreview
                ? <img src={profilePreview} alt="" />
                : (
                  <svg viewBox="0 0 100 100" focusable="false">
                    <circle cx="50" cy="50" r="50" fill="#e5e7e8" />
                    <circle cx="50" cy="35" r="21" fill="#adb3b6" />
                    <path d="M10 100c2-24 18-42 40-42s38 18 40 42H10Z" fill="#adb3b6" />
                  </svg>
                )}
            </div>
            <h1>{name}</h1>
            {profileDetails.talents.length > 0 && (
              <p className="notifications-profile-headline">{profileDetails.talents.join(' · ')}</p>
            )}
            {profileDetails.address && <p>{profileDetails.address}</p>}
            {profileDetails.experience && (
              <p>{profileDetails.experience} years of experience</p>
            )}
            {profileTags.length > 0 && (
              <div className="notifications-profile-tags">
                {profileTags.map((item) => <span key={item}>{item}</span>)}
              </div>
            )}
          </div>
        </section>

        <button
          className="notifications-manage-button"
          type="button"
          onClick={onManageNotifications}
        >
          Manage notifications
          <span aria-hidden="true">›</span>
        </button>

        <section className="home-promotion-card" aria-label="Sponsored movie industry promotion">
          <div className="home-promotion-art">
            <span className="home-promotion-label">PROMOTED</span>
            <span className="home-promotion-art-title">THE NEXT<br />BIG PICTURE</span>
            <span className="home-promotion-studio">NORTHSTAR FILMS</span>
          </div>
          <div className="home-promotion-content">
            <h2>Bring your next story to the screen</h2>
            <p>Northstar Films is meeting new writers, actors, and independent filmmakers for upcoming productions.</p>
            <span className="home-promotion-caption">Make your mark in the movies.</span>
            <button
              className="home-promotion-button"
              type="button"
              onClick={async () => {
                try {
                  const res = await api.clickPromotion("Endorsement Promo - Northstar Films")
                  alert(res.message || 'Inquiry registered with studio casting office.')
                } catch (e) {
                  console.warn(e)
                }
              }}
            >
              Explore film careers
            </button>
          </div>
        </section>
      </aside>

      <section className="notifications-content" aria-labelledby="notifications-heading">
        <h2 id="notifications-heading">Endorsement</h2>
        {requestError && <p className="notifications-request-error" role="alert">{requestError}</p>}
        {endorsements.length > 0 || contactRequests.length > 0 ? (
          <div className="notifications-list">
            {contactRequests.map((request) => (
              <article className="notification-card contact-access-notification" key={`contact-${request.id}`}>
                <p className="notification-message">{request.message}</p>
                <div className="contact-access-actions">
                  <button
                    className="contact-access-accept"
                    type="button"
                    onClick={() => respondToContactRequest(request.id, 'accept')}
                    disabled={Boolean(respondingToRequestId)}
                  >
                    {respondingToRequestId === request.id ? 'Saving…' : 'Accept'}
                  </button>
                  <button
                    className="contact-access-reject"
                    type="button"
                    onClick={() => respondToContactRequest(request.id, 'reject')}
                    disabled={Boolean(respondingToRequestId)}
                  >
                    Reject
                  </button>
                </div>
              </article>
            ))}
            {endorsements.map((item) => (
              <article className="notification-card" key={`notification-${item.id}`}>
                <div className="notification-card-heading">
                  <strong>{item.title}</strong>
                  {item.createdAt && <small>{item.createdAt}</small>}
                </div>
                <p className="notification-message">{item.message}</p>
              </article>
            ))}
          </div>
        ) : (
          <div className="notifications-empty-space" aria-label="No endorsements yet">
            <p>No endorsements or contact requests yet.</p>
          </div>
        )}
      </section>
    </section>
  )
}

export default Notifications