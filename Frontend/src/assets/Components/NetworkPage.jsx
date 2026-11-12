import { useEffect, useRef, useState } from 'react'
import api from '../../api'
import { api as directoryApi } from '../../services/api'
import ProfileBanner from './ProfileBanner.jsx'
import {
  ArrowLeft,
  CalendarDays,
  Check,
  MessageCircle,
  UserPlus,
  UserRound,
  UsersRound,
  UserX,
  X,
} from 'lucide-react'
import './NetworkPage.css'

const networkSections = [
  { label: 'Connections', count: 0, Icon: UsersRound },
  { label: 'Following & followers', Icon: UserRound },
  { label: 'Requests', count: 0, Icon: UserPlus },
  { label: 'Events', count: 0, Icon: CalendarDays },
]

function getProfileKey(profileOrId) {
  const id = typeof profileOrId === 'object' && profileOrId !== null
    ? profileOrId.userId || profileOrId.id || profileOrId.user_id || profileOrId._id
    : profileOrId
  return String(id || '').trim().toLowerCase()
}

function getProfileIdentityKeys(profile) {
  return [
    profile.id,
    profile.userId,
    profile.user_id,
    profile._id,
    profile.referId,
    profile.email,
  ]
    .filter(Boolean)
    .map((identifier) => String(identifier).trim().toLowerCase())
}

function getNormalizedProfileName(profile) {
  return String(profile.name || '').trim().replace(/\s+/g, ' ').toLowerCase()
}

function NetworkPage({ onMessage, onProfileSelect, currentUserIdentifiers = [], isActive = true, isAdmin = false }) {
  const [activeTab, setActiveTab] = useState('grow')
  const [activeSection, setActiveSection] = useState('grow')
  const [profiles, setProfiles] = useState([])
  const [loadingProfiles, setLoadingProfiles] = useState(true)
  const [connectedProfiles, setConnectedProfiles] = useState([])
  const [followedProfiles, setFollowedProfiles] = useState([])
  const [pendingProfiles, setPendingProfiles] = useState([])
  const [invitations, setInvitations] = useState([])
  const [processingInvitationId, setProcessingInvitationId] = useState('')
  const [updatingConnectionProfileId, setUpdatingConnectionProfileId] = useState('')
  const [updatingFollowProfileId, setUpdatingFollowProfileId] = useState('')
  const [connectionError, setConnectionError] = useState('')
  const [followError, setFollowError] = useState('')
  const relationshipOverrides = useRef(new Map())
  const currentUserIdentityKey = JSON.stringify(currentUserIdentifiers
    .filter(Boolean)
    .map((identifier) => String(identifier).trim().toLowerCase()))
  const suggestionProfiles = profiles.filter((profile) => (
    !connectedProfiles.includes(getProfileKey(profile))
    && !followedProfiles.includes(getProfileKey(profile))
    && !pendingProfiles.includes(getProfileKey(profile))
  ))
  const displayedProfiles = (
    activeSection === 'connections'
      ? profiles.filter((profile) => connectedProfiles.includes(getProfileKey(profile)) || Boolean(profile.isConnected))
      : activeSection === 'following'
        ? profiles.filter((profile) => followedProfiles.includes(getProfileKey(profile)) || Boolean(profile.isFollowing))
        : activeSection === 'requests'
          ? profiles.filter((profile) => pendingProfiles.includes(getProfileKey(profile)) || Boolean(profile.isPending))
          : []
  )

  function openConversation(profile) {
    if (!profile?.name) return
    onMessage?.(profile)
  }

  async function loadNetworkData() {
    if (isAdmin) {
      setProfiles([])
      setConnectedProfiles([])
      setFollowedProfiles([])
      setPendingProfiles([])
      setInvitations([])
      setLoadingProfiles(false)
      return
    }

    const normalizedCurrentUserIdentifiers = new Set(JSON.parse(currentUserIdentityKey || '[]'))
    try {
      const [registeredProfiles, talentProfiles, serverConnected, suggestionsData, invitationsData] = await Promise.all([
        api.getProfileDirectory().catch(() => []),
        directoryApi.talents.getAll().catch(() => []),
        api.getConnectedProfiles().catch(() => []),
        api.getNetworkSuggestions().catch(() => []),
        api.getInvitations().catch(() => []),
      ])

      const networkProfiles = Array.isArray(suggestionsData) ? suggestionsData : []
      const incomingInvitations = Array.isArray(invitationsData) ? invitationsData : []
      const connectedList = Array.isArray(serverConnected) ? serverConnected : []

      setInvitations(incomingInvitations)
      const directory = []
      const profilesToMerge = [
        ...(Array.isArray(talentProfiles) ? talentProfiles : []),
        ...(Array.isArray(registeredProfiles) ? registeredProfiles : []),
        ...(Array.isArray(connectedList) ? connectedList : []),
        ...(Array.isArray(networkProfiles) ? networkProfiles : []),
        ...(Array.isArray(incomingInvitations) ? incomingInvitations.map((inv) => inv.profile || inv) : []),
      ]
      profilesToMerge.filter((profile) => profile && profile.name).forEach((profile) => {
        const identityKeys = new Set(getProfileIdentityKeys(profile))
        const normalizedName = getNormalizedProfileName(profile)
        const isGenericName = ['truehunt member', 'cinema professional', 'member', ''].includes(normalizedName)
        const existingIndex = directory.findIndex((existingProfile) => {
          const existingIdentityKeys = getProfileIdentityKeys(existingProfile)
          const hasMatchingKey = existingIdentityKeys.some((key) => identityKeys.has(key))
          if (hasMatchingKey) return true
          if (identityKeys.size > 0 && existingIdentityKeys.length > 0) return false
          return !isGenericName && getNormalizedProfileName(existingProfile) === normalizedName
        })

        if (existingIndex === -1) {
          directory.push(profile)
        } else {
          const existingProfile = directory[existingIndex]
          const existingNameIsGeneric = ['truehunt member', 'cinema professional', 'member', '']
            .includes(getNormalizedProfileName(existingProfile))
          const profileImageUrl = profile.profileImageUrl
            || profile.avatar
            || existingProfile.profileImageUrl
            || existingProfile.avatar
            || ''
          directory[existingIndex] = {
            ...existingProfile,
            ...profile,
            name: isGenericName && !existingNameIsGeneric ? existingProfile.name : profile.name,
            profileImageUrl,
            avatar: profileImageUrl,
          }
        }
      })

      const networkProfilesById = new Map(
        networkProfiles.map((profile) => [getProfileKey(profile), profile]),
      )

      const serverConnectedKeys = new Set(
        connectedList.map((p) => getProfileKey(p)),
      )

      // Stored overrides can be cleared once fresh server data is loaded
      relationshipOverrides.current.clear()

      const allProfiles = directory
        .filter((profile) => profile && (profile.id || profile.userId) && profile.name)
        .filter((profile) => {
          const profileIdentifiers = [
            profile.id,
            profile.userId,
            profile.referId,
            profile.email,
          ].filter(Boolean).map((identifier) => String(identifier).trim().toLowerCase())
          return !profileIdentifiers.some((identifier) => normalizedCurrentUserIdentifiers.has(identifier))
        })
        .map((profile, index) => {
          const profileIdentityKeys = getProfileIdentityKeys(profile)
          const registeredProfile = (Array.isArray(registeredProfiles) ? registeredProfiles : []).find((item) => (
            getProfileIdentityKeys(item).some((key) => profileIdentityKeys.includes(key))
          ))
          const profileImageUrl = registeredProfile
            ? registeredProfile.profileImageUrl || registeredProfile.avatar || ''
            : profile.profileImageUrl || profile.avatar || ''
          const id = String(profile.id || profile.userId)
          const name = (profile.name || 'TrueHunt Member').trim()
          const nameParts = name.split(/\s+/)
          const profileKey = getProfileKey(id)
          const connection = networkProfilesById.get(profileKey)
          const isConn = serverConnectedKeys.has(profileKey)
            || Boolean(connection?.isConnected)
            || Boolean(profile.isConnected)
            || Boolean(profile.connected)
          const isPend = !isConn && Boolean(connection?.isPending || connection?.isOutgoingPending || profile.isPending)

          return {
            ...profile,
            id,
            name,
            initials: profile.initials || nameParts.slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'TH',
            status: connection?.status || 'active',
            role: profile.headline || profile.role || profile.primaryCraft || profile.category || 'Cinema Professional',
            location: profile.location || profile.address || '',
            mutualConnections: connection?.mutualConnections || 'TrueHunt member',
            avatarClass: index % 2 === 0 ? 'network-avatar-coral' : 'network-avatar-blue',
            profileImageUrl,
            avatar: profileImageUrl,
            coverImageUrl: profile.coverImageUrl || profile.coverImage || '',
            isConnected: isConn,
            isFollowing: Boolean(connection?.isFollowing || profile.isFollowing || profile.following),
            isPending: isPend,
          }
        })

      // Ensure every profile in connectedList is explicitly present in allProfiles with isConnected = true
      for (const conn of connectedList) {
        if (!conn) continue
        const cKey = getProfileKey(conn)
        if (!cKey || normalizedCurrentUserIdentifiers.has(cKey)) continue
        const existingIdx = allProfiles.findIndex((p) => getProfileKey(p) === cKey)
        if (existingIdx !== -1) {
          allProfiles[existingIdx].isConnected = true
          allProfiles[existingIdx].isPending = false
        } else {
          const name = (conn.name || 'TrueHunt Member').trim()
          const nameParts = name.split(/\s+/)
          allProfiles.unshift({
            ...conn,
            id: conn.id || conn.userId || cKey,
            userId: conn.userId || conn.id || cKey,
            name,
            initials: conn.initials || nameParts.slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'TH',
            status: 'active',
            role: conn.role || conn.headline || conn.primaryCraft || 'Cinema Professional',
            location: conn.location || conn.address || '',
            mutualConnections: conn.mutualConnections || 'TrueHunt member',
            avatarClass: 'network-avatar-coral',
            profileImageUrl: conn.profileImageUrl || conn.avatar || '',
            coverImageUrl: conn.coverImageUrl || conn.coverImage || '',
            isConnected: true,
            isFollowing: Boolean(conn.isFollowing || conn.following),
            isPending: false,
          })
        }
      }

      setProfiles(allProfiles)
      const allConnectedKeys = Array.from(new Set([
        ...connectedList.map((p) => getProfileKey(p)),
        ...allProfiles.filter((profile) => profile.isConnected).map((profile) => getProfileKey(profile.id)),
      ])).filter(Boolean)
      setConnectedProfiles(allConnectedKeys)
      setFollowedProfiles(allProfiles.filter((profile) => profile.isFollowing).map((profile) => getProfileKey(profile.id)))
      setPendingProfiles(allProfiles.filter((profile) => profile.isPending).map((profile) => getProfileKey(profile.id)))
    } catch (err) {
      console.warn('Network profiles load error:', err)
    } finally {
      setLoadingProfiles(false)
    }
  }

  useEffect(() => {
    if (!isActive) return undefined

    loadNetworkData()

    const handleFocus = () => {
      if (!document.hidden) loadNetworkData()
    }
    const handleVisibility = () => {
      if (!document.hidden) loadNetworkData()
    }

    window.addEventListener('focus', handleFocus)
    document.addEventListener('visibilitychange', handleVisibility)

    return () => {
      window.removeEventListener('focus', handleFocus)
      document.removeEventListener('visibilitychange', handleVisibility)
    }
  }, [currentUserIdentityKey, isActive, isAdmin])

  useEffect(() => {
    function showNetworkHome() {
      setActiveTab('grow')
      setActiveSection('grow')
    }

    window.addEventListener('truehunt-network-navigate', showNetworkHome)
    return () => window.removeEventListener('truehunt-network-navigate', showNetworkHome)
  }, [])

  useEffect(() => {
    function handleConnectionChanged(event) {
      if (isAdmin) return
      loadNetworkData()
      const { connected, pending, profile, senderProfile, receiverProfile, senderId, receiverId } = event.detail || {}
      const targetProfile = profile || senderProfile || receiverProfile
      const profileId = String(
        targetProfile?.userId || targetProfile?.id || targetProfile?.user_id || targetProfile?._id || receiverId || senderId || '',
      )
      if (!profileId) return
      const profileKey = getProfileKey(profileId)
      relationshipOverrides.current.set(profileKey, {
        ...relationshipOverrides.current.get(profileKey),
        connected: Boolean(connected),
        pending: typeof pending === 'boolean' ? Boolean(pending) : false,
      })

      setConnectedProfiles((current) => (
        connected
          ? current.includes(profileKey) ? current : [...current, profileKey]
          : current.filter((id) => id !== profileKey)
      ))
      if (connected) {
        setPendingProfiles((current) => current.filter((id) => id !== profileKey))
        setInvitations((current) => current.filter((inv) => getProfileKey(inv.profileId) !== profileKey))
      } else if (typeof pending === 'boolean') {
        setPendingProfiles((current) => (
          pending
            ? current.includes(profileKey) ? current : [...current, profileKey]
            : current.filter((id) => id !== profileKey)
        ))
      }
      setProfiles((current) => {
        const exists = current.some((item) => getProfileKey(item) === profileKey)
        if (!exists && targetProfile && targetProfile.name) {
          const name = targetProfile.name.trim()
          const nameParts = name.split(/\s+/)
          return [{
            ...targetProfile,
            id: profileId,
            userId: profileId,
            name,
            initials: targetProfile.initials || nameParts.slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'TH',
            status: 'active',
            role: targetProfile.role || targetProfile.headline || 'Cinema Professional',
            location: targetProfile.location || '',
            mutualConnections: 'TrueHunt member',
            avatarClass: 'network-avatar-coral',
            profileImageUrl: targetProfile.profileImageUrl || targetProfile.avatar || '',
            coverImageUrl: targetProfile.coverImageUrl || targetProfile.coverImage || '',
            isConnected: Boolean(connected),
            isPending: typeof pending === 'boolean' ? Boolean(pending) : (connected ? false : false),
          }, ...current]
        }
        return current.map((item) => (
          getProfileKey(item) === profileKey
            ? {
                ...item,
                isConnected: Boolean(connected),
                isPending: typeof pending === 'boolean' ? Boolean(pending) : (connected ? false : item.isPending),
              }
            : item
        ))
      })
    }

    function handleFollowChanged(event) {
      if (isAdmin) return
      const { following, profile } = event.detail || {}
      const profileId = String(
        profile?.userId || profile?.id || profile?.user_id || profile?._id || '',
      )
      if (!profileId) return
      const profileKey = getProfileKey(profileId)
      relationshipOverrides.current.set(profileKey, {
        ...relationshipOverrides.current.get(profileKey),
        following: Boolean(following),
      })

      setFollowedProfiles((current) => (
        following
          ? current.includes(profileKey) ? current : [...current, profileKey]
          : current.filter((id) => id !== profileKey)
      ))
      setProfiles((current) => {
        const hasProfile = current.some((item) => getProfileKey(item) === profileKey)
        const updatedProfiles = current.map((item) => (
          getProfileKey(item) === profileKey ? { ...item, isFollowing: Boolean(following) } : item
        ))
        if (!following || hasProfile || !profile?.name) return updatedProfiles

        const name = String(profile.name).trim()
        return [...updatedProfiles, {
          ...profile,
          id: profileId,
          userId: profile.userId || profileId,
          name,
          initials: profile.initials || name.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase(),
          status: profile.status || 'active',
          role: profile.role || profile.headline || profile.primaryCraft || profile.category || 'Cinema Professional',
          location: profile.location || profile.address || '',
          avatarClass: profile.avatarClass || 'network-avatar-coral',
          profileImageUrl: profile.profileImageUrl || profile.avatar || '',
          coverImageUrl: profile.coverImageUrl || profile.coverImage || '',
          isConnected: Boolean(profile.isConnected),
          isFollowing: true,
        }]
      })
    }

    window.addEventListener('truehunt-connection-changed', handleConnectionChanged)
    window.addEventListener('truehunt-follow-changed', handleFollowChanged)
    return () => {
      window.removeEventListener('truehunt-connection-changed', handleConnectionChanged)
      window.removeEventListener('truehunt-follow-changed', handleFollowChanged)
    }
  }, [isAdmin])

  async function handleAcceptInvitation(profileId, invitation) {
    if (processingInvitationId) return
    const profileKey = getProfileKey(profileId)
    setProcessingInvitationId(profileId)
    try {
      const result = await api.acceptConnectionRequest(profileId)
      const connected = typeof result?.connected === 'boolean' ? result.connected : true
      relationshipOverrides.current.set(profileKey, {
        ...relationshipOverrides.current.get(profileKey),
        connected: true,
        pending: false,
      })
      const profileData = profiles.find((item) => getProfileKey(item) === profileKey)
        || {
          ...(invitation?.profile || {}),
          id: invitation?.profileId || invitation?.profile?.id || profileId,
          userId: invitation?.profile?.userId || invitation?.profileId || profileId,
          name: invitation?.name || invitation?.profile?.name || 'TrueHunt Member',
        }

      setProfiles((current) => {
        const exists = current.some((item) => getProfileKey(item) === profileKey)
        if (exists) {
          return current.map((item) => (
            getProfileKey(item) === profileKey ? { ...item, isConnected: true, isPending: false } : item
          ))
        }
        return [...current, { ...profileData, isConnected: true, isPending: false }]
      })
      setConnectedProfiles((current) => (current.includes(profileKey) ? current : [...current, profileKey]))
      setPendingProfiles((current) => current.filter((id) => id !== profileKey))
      setInvitations((current) => current.filter((inv) => getProfileKey(inv.profileId) !== profileKey))

      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: { connected: true, profile: profileData },
      }))
      loadNetworkData()
    } catch (err) {
      console.error('Accept invitation error:', err)
      setConnectionError('Could not accept this invitation. Please try again.')
    } finally {
      setProcessingInvitationId('')
    }
  }

  async function handleRejectInvitation(profileId) {
    if (processingInvitationId) return
    const profileKey = getProfileKey(profileId)
    setProcessingInvitationId(profileId)
    try {
      await api.rejectConnectionRequest(profileId)
      setInvitations((current) => current.filter((inv) => getProfileKey(inv.profileId) !== profileKey))
      setPendingProfiles((current) => current.filter((id) => id !== profileKey))
      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: { connected: false, pending: false, profileId },
      }))
    } catch (err) {
      console.error('Reject invitation error:', err)
      setConnectionError('Could not ignore this invitation. Please try again.')
    } finally {
      setProcessingInvitationId('')
    }
  }

  async function handleRemoveRequest(profileId) {
    if (updatingConnectionProfileId) return
    const profileKey = getProfileKey(profileId)
    const profile = profiles.find((item) => getProfileKey(item) === profileKey)
    setConnectionError('')
    setUpdatingConnectionProfileId(profileId)
    try {
      await api.cancelConnectionRequest(profileId)
      relationshipOverrides.current.set(profileKey, {
        ...relationshipOverrides.current.get(profileKey),
        connected: false,
        pending: false,
      })
      setPendingProfiles((current) => current.filter((id) => id !== profileKey))
      setProfiles((current) => current.map((item) => (
        getProfileKey(item) === profileKey
          ? { ...item, isConnected: false, isPending: false }
          : item
      )))
      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: { connected: false, pending: false, profile },
      }))
      loadNetworkData()
    } catch (err) {
      console.error('Cancel connection request error:', err)
      setConnectionError('Could not remove connection request. Please try again.')
    } finally {
      setUpdatingConnectionProfileId('')
    }
  }

  async function toggleConnection(profileId) {
    if (updatingConnectionProfileId) return
    const profileKey = getProfileKey(profileId)
    const isConnected = connectedProfiles.includes(profileKey)
    const isPending = pendingProfiles.includes(profileKey)
    const profile = profiles.find((item) => getProfileKey(item) === profileKey)
    setConnectionError('')
    setUpdatingConnectionProfileId(profileId)
    try {
      const result = await api.toggleConnection(profileId)
      const connected = Boolean(result?.connected)
      const pending = Boolean(result?.isPending)

      relationshipOverrides.current.set(profileKey, {
        ...relationshipOverrides.current.get(profileKey),
        connected,
        pending,
      })
      setConnectedProfiles((current) => (
        connected
          ? current.includes(profileKey) ? current : [...current, profileKey]
          : current.filter((id) => id !== profileKey)
      ))
      setPendingProfiles((current) => (
        pending
          ? current.includes(profileKey) ? current : [...current, profileKey]
          : current.filter((id) => id !== profileKey)
      ))
      setProfiles((current) => current.map((item) => (
        getProfileKey(item) === profileKey
          ? { ...item, isConnected: connected, isPending: pending }
          : item
      )))
      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: { connected, profile },
      }))
    } catch (err) {
      console.error('Backend toggle connection error:', err)
      setConnectionError('Could not update this connection. Please try again.')
    } finally {
      setUpdatingConnectionProfileId('')
    }
  }

  async function toggleFollow(profileId) {
    if (updatingFollowProfileId) return
    const profileKey = getProfileKey(profileId)
    const isFollowing = followedProfiles.includes(profileKey)
    setFollowError('')
    setUpdatingFollowProfileId(profileId)
    try {
      const result = await api.toggleFollow(profileId)
      const following = typeof result?.following === 'boolean' ? result.following : !isFollowing
      relationshipOverrides.current.set(profileKey, {
        ...relationshipOverrides.current.get(profileKey),
        following,
      })
      setFollowedProfiles((current) => (
        following
          ? current.includes(profileKey) ? current : [...current, profileKey]
          : current.filter((id) => id !== profileKey)
      ))
      const profile = profiles.find((item) => getProfileKey(item) === profileKey)
      setProfiles((current) => current.map((item) => (
        getProfileKey(item) === profileKey ? { ...item, isFollowing: following } : item
      )))
      window.dispatchEvent(new CustomEvent('truehunt-follow-changed', {
        detail: { following, profile },
      }))
    } catch (err) {
      console.error('Backend toggle follow error:', err)
      setFollowError('Could not update this follow. Please try again.')
    } finally {
      setUpdatingFollowProfileId('')
    }
  }

  function renderProfileCard(profile, isRequestView = false) {
    const profileKey = getProfileKey(profile)
    const isConnected = connectedProfiles.includes(profileKey)
    const isPending = pendingProfiles.includes(profileKey)
    const isFollowing = followedProfiles.includes(profileKey)

    return (
      <article className="network-profile-card" key={profile.id}>
        <div className={`network-profile-cover ${profile.avatarClass}`}>
          {profile.coverImageUrl && (
            <img src={profile.coverImageUrl} alt="" aria-hidden="true" />
          )}
          {!profile.coverImageUrl && (
            <ProfileBanner variant="card" />
          )}
        </div>
        <div className="network-profile-avatar-wrap">
          <button
            className={`network-profile-avatar ${profile.avatarClass}`}
            type="button"
            aria-label={`View ${profile.name}'s profile`}
            onClick={() => onProfileSelect?.(profile)}
          >
            {profile.profileImageUrl
              ? <img src={profile.profileImageUrl} alt="" />
              : profile.initials}
          </button>
          <span
            className={`network-presence-dot is-${profile.status}`}
            role="img"
            aria-label={`${profile.name} is ${profile.status}`}
          />
        </div>
        <div className="network-profile-content">
          <h3>{profile.name}</h3>
          <p className="network-profile-role">{profile.role}</p>
          <p className="network-profile-location">{profile.location}</p>
          <p className="network-mutual-connections">
            <UsersRound size={14} aria-hidden="true" />
            {profile.mutualConnections}
          </p>
          <div className="network-profile-actions">
            <button
              className={`network-connect-button${isConnected ? ' is-connected' : isPending ? ' is-pending' : ''}`}
              type="button"
              disabled={Boolean(updatingConnectionProfileId) || isRequestView}
              aria-pressed={isConnected}
              onClick={() => toggleConnection(profile.id)}
            >
              {updatingConnectionProfileId === profile.id
                ? (isConnected ? 'Removing…' : isPending ? 'Canceling…' : 'Connecting…')
                : (isConnected ? '✓ Connected' : isPending ? 'Waiting for response' : '+ Connect')}
            </button>
            <button
              className={`network-follow-button${isFollowing ? ' is-following' : ''}`}
              type="button"
              disabled={Boolean(updatingFollowProfileId)}
              aria-pressed={isFollowing}
              onClick={() => toggleFollow(profile.id)}
            >
              {isFollowing ? 'Unfollow' : 'Follow'}
            </button>
          </div>
          {isRequestView ? (
            <button
              className="network-remove-request-button"
              type="button"
              disabled={Boolean(updatingConnectionProfileId)}
              onClick={() => handleRemoveRequest(profile.id)}
            >
              <UserX size={15} aria-hidden="true" />
              {updatingConnectionProfileId === profile.id ? 'Removing request…' : 'Remove request'}
            </button>
          ) : (
            <button
              className="network-message-button"
              type="button"
              onClick={() => openConversation(profile)}
            >
              <MessageCircle size={15} aria-hidden="true" />
              Message
            </button>
          )}
        </div>
      </article>
    )
  }

  if (isAdmin) {
    return (
      <main className="network-page" id="network" aria-label="My network">
        <section className="network-catch-up">
          <div className="network-catch-up-icon"><UsersRound size={24} aria-hidden="true" /></div>
          <h2>Network is unavailable for admin accounts</h2>
          <p>Administrator profiles do not display member connections or suggestions.</p>
        </section>
      </main>
    )
  }

  return (
    <main className="network-page" id="network" aria-label="My network">
      <div className="network-layout">
        <aside className="network-sidebar">
          <section className="network-manage-card" aria-labelledby="network-manage-title">
            <h1 id="network-manage-title">Manage my network</h1>
            <nav aria-label="Network sections">
              {networkSections.map(({ label, count, Icon }) => {
                const isSelected = (
                  (label === 'Connections' && activeSection === 'connections')
                  || (label === 'Following & followers' && activeSection === 'following')
                  || (label === 'Requests' && activeSection === 'requests')
                )
                const displayCount = (
                  label === 'Connections'
                    ? Math.max(connectedProfiles.length, profiles.filter((p) => p.isConnected).length)
                    : label === 'Following & followers'
                      ? Math.max(followedProfiles.length, profiles.filter((p) => p.isFollowing).length)
                      : label === 'Requests'
                        ? Math.max(pendingProfiles.length, profiles.filter((p) => p.isPending).length)
                        : count ?? 0
                )
                return (
                  <button
                    className={`network-manage-item${isSelected ? ' is-selected' : ''}`}
                    key={label}
                    type="button"
                    aria-pressed={isSelected}
                    onClick={() => {
                      setActiveSection(
                        label === 'Connections'
                          ? 'connections'
                          : label === 'Following & followers'
                            ? 'following'
                            : label === 'Requests'
                              ? 'requests'
                              : 'grow',
                      )
                      loadNetworkData()
                    }}
                  >
                    <Icon size={19} strokeWidth={2.2} aria-hidden="true" />
                    <span>{label}</span>
                    <small>{displayCount}</small>
                  </button>
                )
              })}
            </nav>
          </section>
          <section className="network-tip-card">
            <span className="network-tip-eyebrow">YOUR CREATIVE CIRCLE</span>
            <h2>Good connections lead to great collaborations.</h2>
            <p>Meet more people working across film and entertainment.</p>
          </section>
          <section className="home-card home-promotion-card" aria-label="Sponsored movie industry promotion">
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
                    const res = await api.clickPromotion("Network Promo - Northstar Films")
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

        <div className="network-main">
          {activeSection === 'connections' || activeSection === 'following' || activeSection === 'requests' ? (
            <section className="network-suggestions" aria-labelledby="network-connections-title">
              <button
                className="network-connections-back"
                type="button"
                onClick={() => {
                  setActiveSection('grow')
                  loadNetworkData()
                }}
              >
                <ArrowLeft size={16} aria-hidden="true" />
                Back to Network
              </button>
              <header className="network-section-heading">
                <div>
                  <span className="network-section-eyebrow">YOUR NETWORK</span>
                  <h2 id="network-connections-title">
                    {activeSection === 'connections'
                      ? 'Connections'
                      : activeSection === 'following'
                        ? 'Following & followers'
                        : 'Requests'}
                  </h2>
                  <p>
                    {activeSection === 'connections'
                      ? 'People you’ve connected with.'
                      : activeSection === 'following'
                        ? 'Profiles you follow.'
                        : 'Connection requests you’ve sent. When accepted, they will show in your connections.'}
                  </p>
                </div>
                <span className="network-suggestion-count">
                  {activeSection === 'connections'
                    ? `${displayedProfiles.length} connections`
                    : activeSection === 'following'
                      ? `${displayedProfiles.length} following`
                      : `${displayedProfiles.length} requests`}
                </span>
              </header>
              {followError && activeSection === 'following' && (
                <p className="network-connection-error" role="alert">{followError}</p>
              )}
              {connectionError && (activeSection === 'connections' || activeSection === 'requests') && (
                <p className="network-connection-error" role="alert">{connectionError}</p>
              )}
              {displayedProfiles.length > 0 ? (
                <div className="network-profile-grid">
                  {displayedProfiles.map((profile) => renderProfileCard(profile, activeSection === 'requests'))}
                </div>
              ) : (
                <div className="network-connections-empty">
                  <div className="network-catch-up-icon">
                    {activeSection === 'requests'
                      ? <UserPlus size={24} aria-hidden="true" />
                      : <UsersRound size={24} aria-hidden="true" />}
                  </div>
                  <h3>
                    {activeSection === 'connections'
                      ? 'No connections yet'
                      : activeSection === 'following'
                        ? 'You’re not following anyone yet'
                        : 'No pending requests'}
                  </h3>
                  <p>
                    {activeSection === 'connections'
                      ? 'Explore suggested profiles and connect with people in your community.'
                      : activeSection === 'following'
                        ? 'Explore profiles and follow people to see them here.'
                        : 'You haven’t sent any pending connection requests.'}
                  </p>
                  <button type="button" onClick={() => setActiveSection('grow')}>Find people</button>
                </div>
              )}
            </section>
          ) : (
            <>
          <div className="network-tabs" role="tablist" aria-label="Network suggestions">
            <button
              className={`network-tab${activeTab === 'grow' ? ' is-selected' : ''}`}
              type="button"
              role="tab"
              aria-selected={activeTab === 'grow'}
              onClick={() => setActiveTab('grow')}
            >
              Grow
            </button>
            <button
              className={`network-tab${activeTab === 'catch-up' ? ' is-selected' : ''}`}
              type="button"
              role="tab"
              aria-selected={activeTab === 'catch-up'}
              onClick={() => setActiveTab('catch-up')}
            >
              Catch up
            </button>
          </div>

          {activeTab === 'grow' ? (
            <>
              {invitations.length > 0 ? (
                <section className="network-invitations-card" aria-labelledby="network-invitations-title">
                  <header className="network-invitations-card-header">
                    <h2 id="network-invitations-title">Invitations ({invitations.length})</h2>
                  </header>
                  <div className="network-invitations-list">
                    {invitations.map((inv) => (
                      <div className="network-invitation-item" key={inv.id || inv.profileId}>
                        <div className="network-invitation-sender">
                          <button
                            type="button"
                            className="network-invitation-avatar"
                            aria-label={`View ${inv.name}'s profile`}
                            onClick={() => onProfileSelect?.(inv.profile || { id: inv.profileId, name: inv.name })}
                          >
                            {inv.avatar || inv.profileImageUrl ? (
                              <img src={inv.avatar || inv.profileImageUrl} alt="" />
                            ) : (
                              <span>{inv.initials || 'TH'}</span>
                            )}
                          </button>
                          <div className="network-invitation-details">
                            <h3
                              role="button"
                              tabIndex={0}
                              onClick={() => onProfileSelect?.(inv.profile || { id: inv.profileId, name: inv.name })}
                              onKeyDown={(e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                  onProfileSelect?.(inv.profile || { id: inv.profileId, name: inv.name })
                                }
                              }}
                            >
                              {inv.name}
                            </h3>
                            <p className="network-invitation-role">{inv.role || 'Cinema Professional'}</p>
                            {inv.location && <p className="network-invitation-location">{inv.location}</p>}
                          </div>
                        </div>
                        <div className="network-invitation-actions">
                          <button
                            type="button"
                            className="network-invitation-reject"
                            disabled={Boolean(processingInvitationId)}
                            onClick={() => handleRejectInvitation(inv.profileId)}
                          >
                            <X size={14} aria-hidden="true" />
                            Reject
                          </button>
                          <button
                            type="button"
                            className="network-invitation-accept"
                            disabled={Boolean(processingInvitationId)}
                            onClick={() => handleAcceptInvitation(inv.profileId, inv)}
                          >
                            <Check size={14} aria-hidden="true" />
                            Accept
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </section>
              ) : (
                <section className="network-invitations">
                  <div>
                    <strong>No pending invitations</strong>
                    <p>Invitations you receive will appear here.</p>
                  </div>
                  <span aria-hidden="true">✓</span>
                </section>
              )}

              <section className="network-suggestions" aria-labelledby="network-suggestions-title">
                <header className="network-section-heading">
                  <div>
                    <span className="network-section-eyebrow">PEOPLE TO MEET</span>
                    <h2 id="network-suggestions-title">People you may know</h2>
                    <p>Connect with artists and professionals in your community.</p>
                  </div>
                  <span className="network-suggestion-count">
                    {suggestionProfiles.length} suggestions
                  </span>
                </header>
                {connectionError && <p className="network-connection-error" role="alert">{connectionError}</p>}
                {loadingProfiles
                  ? <p role="status">Loading profiles…</p>
                  : suggestionProfiles.length > 0
                    ? (
                      <div className="network-profile-grid">
                        {suggestionProfiles.map((profile) => renderProfileCard(profile))}
                      </div>
                    )
                    : <p>No profiles are available yet.</p>}
              </section>
            </>
          ) : (
            <section className="network-catch-up">
              <div className="network-catch-up-icon"><UsersRound size={24} aria-hidden="true" /></div>
              <h2>You’re all caught up</h2>
              <p>New updates from your network will show up here.</p>
              <button type="button" onClick={() => setActiveTab('grow')}>Explore people</button>
            </section>
          )}
            </>
          )}
        </div>
      </div>
    </main>
  )
}

export default NetworkPage