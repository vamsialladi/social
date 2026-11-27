import { useEffect, useRef, useState } from 'react'
import './Notification.css'
import api from '../../api'
import ProfileBanner from './ProfileBanner.jsx'
import PromotionCard from './PromotionCard'

function Notifications({
  profileDetails,
  profilePreview,
  onViewProfile,
  onViewConnection,
  onManageNotifications,
  isActive,
}) {
  const [notifications, setNotifications] = useState([])
  const [contactRequests, setContactRequests] = useState([])
  const [subscriptionRequests, setSubscriptionRequests] = useState([])
  const [requestError, setRequestError] = useState('')
  const [respondingToRequestId, setRespondingToRequestId] = useState('')
  const processedSubscriptionDecisionIdsRef = useRef(new Set())
  const name = profileDetails.name.trim() || 'Your name'
  const languages = [
    ...profileDetails.languages.filter((language) => language !== 'Other'),
    ...(profileDetails.otherLanguage ? [profileDetails.otherLanguage] : []),
  ]
  const profileTags = [...languages, ...profileDetails.workTypes]

  useEffect(() => {
    if (!isActive) return undefined

    let isCurrent = true
    async function loadNotifications() {
      const [notificationsResult, requestsResult, subscriptionRequestsResult] = await Promise.allSettled([
        api.getNotifications(),
        api.getIncomingContactAccessRequests(),
        api.getOrganizationPageSubscriptionRequests(),
      ])
      if (!isCurrent) return

      if (notificationsResult.status === 'fulfilled' && Array.isArray(notificationsResult.value)) {
        setNotifications(notificationsResult.value)
        const newDecision = notificationsResult.value.find((notification) => (
          ['connection_page_subscription_accepted', 'connection_page_subscription_rejected']
            .includes(notification.type)
          && notification.id
          && !processedSubscriptionDecisionIdsRef.current.has(String(notification.id))
        ))
        if (newDecision) {
          processedSubscriptionDecisionIdsRef.current.add(String(newDecision.id))
          window.dispatchEvent(new CustomEvent('truehunt-organization-page-subscription-changed', {
            detail: { refresh: true },
          }))
        }
      } else if (notificationsResult.status === 'rejected') {
        console.warn('Backend notifications load error:', notificationsResult.reason)
      }

      if (requestsResult.status === 'fulfilled' && Array.isArray(requestsResult.value)) {
        setContactRequests(requestsResult.value)
      } else if (requestsResult.status === 'rejected') {
        console.warn('Contact access requests load error:', requestsResult.reason)
        setRequestError('Could not load contact requests. Please try again later.')
      }

      if (subscriptionRequestsResult.status === 'fulfilled' && Array.isArray(subscriptionRequestsResult.value)) {
        setSubscriptionRequests(subscriptionRequestsResult.value)
      } else if (subscriptionRequestsResult.status === 'rejected') {
        console.warn('Organization page subscription requests load error:', subscriptionRequestsResult.reason)
        setRequestError('Could not load page subscription requests. Please try again later.')
      }
    }
    loadNotifications()
    const refreshOnFocus = () => {
      if (!document.hidden) loadNotifications()
    }
    const refreshAfterConnectionRequestCancellation = (event) => {
      if (event.detail?.action === 'REQUEST_CANCELLED') loadNotifications()
    }
    const intervalId = window.setInterval(loadNotifications, 30_000)
    window.addEventListener('focus', refreshOnFocus)
    window.addEventListener('truehunt-settings-updated', loadNotifications)
    window.addEventListener('truehunt-connection-changed', refreshAfterConnectionRequestCancellation)
    return () => {
      isCurrent = false
      window.clearInterval(intervalId)
      window.removeEventListener('focus', refreshOnFocus)
      window.removeEventListener('truehunt-settings-updated', loadNotifications)
      window.removeEventListener('truehunt-connection-changed', refreshAfterConnectionRequestCancellation)
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

  async function respondToSubscriptionRequest(requestId, decision) {
    if (respondingToRequestId) return
    setRespondingToRequestId(requestId)
    setRequestError('')
    try {
      const response = await api.respondToOrganizationPageSubscriptionRequest(requestId, decision)
      if (response?.page) {
        window.dispatchEvent(new CustomEvent('truehunt-organization-page-subscription-changed', {
          detail: { page: response.page },
        }))
      }
      setSubscriptionRequests((current) => current.filter((request) => String(request.id) !== String(requestId)))
    } catch (error) {
      console.error(`Could not ${decision.toLowerCase()} page subscription request:`, error)
      setRequestError(error.message || 'Could not update this subscription request. Please try again.')
    } finally {
      setRespondingToRequestId('')
    }
  }

  async function openNotificationProfile(item) {
    if (item.type !== 'endorsement_request' || !item.senderProfileId || !onViewConnection) return

    if (item.id && !item.read) {
      try {
        await api.markNotificationAsRead(item.id)
        setNotifications((current) => current.map((notification) => (
          notification.id === item.id ? { ...notification, read: true } : notification
        )))
      } catch (error) {
        console.warn('Could not mark endorsement request as read:', error)
      }
    }

    const senderProfileId = String(item.senderProfileId)
    onViewConnection({
      id: senderProfileId,
      userId: senderProfileId,
      profileId: senderProfileId,
      name: item.senderName || 'Connection',
    }, { openEndorsementResponse: true })
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

        <PromotionCard ariaLabel="Sponsored movie industry promotion">
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
        </PromotionCard>
      </aside>

      <section className="notifications-content" aria-labelledby="notifications-heading">
        <h2 id="notifications-heading">Notifications</h2>
        {requestError && <p className="notifications-request-error" role="alert">{requestError}</p>}
        {notifications.length > 0 || contactRequests.length > 0 || subscriptionRequests.length > 0 ? (
          <div className="notifications-list">
            {subscriptionRequests.map((request) => (
              <article
                className="notification-card contact-access-notification"
                key={`subscription-${request.id}`}
              >
                <div>
                  <div className="notification-card-heading">
                    <strong>{request.pageName}</strong>
                    {request.createdAt && <small>{new Date(request.createdAt).toLocaleDateString()}</small>}
                  </div>
                  <p className="notification-message">
                    {request.requesterName} requested to subscribe to this page.
                  </p>
                </div>
                <div className="contact-access-actions">
                  <button
                    className="contact-access-accept"
                    type="button"
                    onClick={() => respondToSubscriptionRequest(request.id, 'ACCEPT')}
                    disabled={Boolean(respondingToRequestId)}
                  >
                    {String(respondingToRequestId) === String(request.id) ? 'Saving…' : 'Accept'}
                  </button>
                  <button
                    className="contact-access-reject"
                    type="button"
                    onClick={() => respondToSubscriptionRequest(request.id, 'REJECT')}
                    disabled={Boolean(respondingToRequestId)}
                  >
                    Reject
                  </button>
                </div>
              </article>
            ))}
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
            {notifications
              .filter((item) => !(
                item.type === 'connection_page_subscription_request'
                && subscriptionRequests.some((request) => (
                  String(item.message || '').includes(request.requesterName)
                  && String(item.message || '').includes(request.pageName)
                ))
              ))
              .map((item) => (
              <article
                className={`notification-card${item.type === 'endorsement_request' && item.senderProfileId ? ' is-actionable' : ''}`}
                key={`notification-${item.id}`}
                role={item.type === 'endorsement_request' && item.senderProfileId ? 'button' : undefined}
                tabIndex={item.type === 'endorsement_request' && item.senderProfileId ? 0 : undefined}
                onClick={() => openNotificationProfile(item)}
                onKeyDown={(event) => {
                  if (
                    (event.key === 'Enter' || event.key === ' ')
                    && item.type === 'endorsement_request'
                    && item.senderProfileId
                  ) {
                    event.preventDefault()
                    openNotificationProfile(item)
                  }
                }}
                aria-label={item.type === 'endorsement_request'
                  ? `Open profile of the connection who sent this request: ${item.message}`
                  : undefined}
              >
                <div className="notification-card-heading">
                  <strong>{item.title}</strong>
                  {item.createdAt && <small>{item.createdAt}</small>}
                </div>
                <p className="notification-message">{item.message}</p>
              </article>
            ))}
          </div>
        ) : (
          <div className="notifications-empty-space" aria-label="No notifications yet">
            <p>No notifications or contact requests yet.</p>
          </div>
        )}
      </section>
    </section>
  )
}

export default Notifications