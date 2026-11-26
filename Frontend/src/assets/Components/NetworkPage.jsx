import { useCallback, useEffect, useRef, useState } from 'react'
import api from '../../api'
import MediaSafetyStatus from '../../components/common/MediaSafetyStatus.jsx'
import { useMediaSafety } from '../../hooks/useMediaSafety.js'
import ProfileBanner from './ProfileBanner.jsx'
import {
  ArrowLeft,
  Check,
  Image,
  MessageCircle,
  MoreVertical,
  Network,
  Trash2,
  UserPlus,
  UserRound,
  UsersRound,
  UserX,
  X,
} from 'lucide-react'
import './NetworkPage.css'

const networkSections = [
  { label: 'Connections', Icon: UsersRound },
  { label: 'Following & followers', Icon: UserRound },
  { label: 'Requests', Icon: UserPlus },
  { label: 'Groups', Icon: Network },
  { label: 'Banner', Icon: Image },
]

const demoNetworkProfileIdentifiers = new Set([])
const groupPhotoMimeTypesByExtension = {
  avif: 'image/avif',
  bmp: 'image/bmp',
  gif: 'image/gif',
  jpeg: 'image/jpeg',
  jpg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp',
}
const MAX_GROUP_PHOTO_SIZE_BYTES = 10 * 1024 * 1024

function normalizeGroupPhotoFile(file) {
  const type = String(file.type || '').toLowerCase()
  const knownTypes = Object.values(groupPhotoMimeTypesByExtension)
  if (knownTypes.includes(type)) return file

  const extension = file.name.split('.').pop()?.toLowerCase() || ''
  const inferredType = groupPhotoMimeTypesByExtension[extension]
  const canInferType = !type || type === 'application/octet-stream'
    || ['image/pjpeg', 'image/jpg', 'image/x-png', 'image/x-ms-bmp'].includes(type)
  if (!canInferType || !inferredType) return null

  return new File([file], file.name, { type: inferredType, lastModified: file.lastModified })
}

function getProfileLocationSummary(profile) {
  const structuredParts = [profile?.district, profile?.state, profile?.country]
    .map((part) => String(part || '').trim())
    .filter(Boolean)

  if (structuredParts.length) return structuredParts.join(', ')

  const parts = String(profile?.location || profile?.address || '')
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
  if (parts.length < 2) return ''

  const summaryParts = parts.slice(-3)
  if (/\b(?:old village|street|road|lane|colony|apartment|flat|plot|door no|house no)\b|\d/i.test(summaryParts[0])) {
    summaryParts.shift()
  }
  return summaryParts.join(', ')
}

function getProfileKey(profileOrId) {
  const id = typeof profileOrId === 'object' && profileOrId !== null
    ? profileOrId.userId
      || profileOrId.id
      || profileOrId.profileId
      || profileOrId.senderId
      || profileOrId.targetProfileId
      || profileOrId.user_id
      || profileOrId._id
      || profileOrId.publicUserId
      || profileOrId.email
    : profileOrId
  return String(id || '').trim().toLowerCase()
}

function getProfileIdentityKeys(profile) {
  return Array.from(new Set([
    profile?.id,
    profile?.userId,
    profile?.user_id,
    profile?._id,
    profile?.referId,
    profile?.profileId,
    profile?.senderId,
    profile?.targetProfileId,
    profile?.publicUserId,
    profile?.email,
  ]
    .filter(Boolean)
    .map((identifier) => String(identifier).trim().toLowerCase())))
}

function profilesMatch(first, second) {
  const secondKeys = new Set(getProfileIdentityKeys(second))
  return getProfileIdentityKeys(first).some((key) => secondKeys.has(key))
}

function getUniqueProfiles(profiles) {
  return profiles.reduce((unique, profile) => {
    if (!profile || unique.some((existing) => profilesMatch(existing, profile))) return unique
    unique.push(profile)
    return unique
  }, [])
}

function isDemoNetworkProfile(profile) {
  return getProfileIdentityKeys(profile).some((key) => demoNetworkProfileIdentifiers.has(key))
}

function getProfileName(...profiles) {
  for (const profile of profiles) {
    const candidates = [
      profile?.name,
      profile?.fullName,
      profile?.displayName,
      profile?.userName,
      profile?.profile?.name,
      profile?.account?.name,
      profile?.user?.name,
    ]
    const name = candidates
      .map((candidate) => String(candidate || '').trim().replace(/\s+/g, ' '))
      .find((candidate) => candidate && !['truehunt member', 'cinema professional', 'member', 'profile'].includes(candidate.toLowerCase()))
    if (name) return name
  }
  return ''
}

function getMutualConnectionsLabel(value) {
  const label = String(value || '').trim()
  return ['truehunt member', 'cinema professional', 'member'].includes(label.toLowerCase())
    ? ''
    : label
}

function getNormalizedProfileName(profile) {
  return getProfileName(profile).toLowerCase()
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
  const [connectDialogProfile, setConnectDialogProfile] = useState(null)
  const [connectionNote, setConnectionNote] = useState('')
  const [followerProfiles, setFollowerProfiles] = useState([])
  const [catchUpUpdates, setCatchUpUpdates] = useState([])
  const [connectionGroups, setConnectionGroups] = useState([])
  const [loadingConnectionGroups, setLoadingConnectionGroups] = useState(false)
  const [groupLoadError, setGroupLoadError] = useState('')
  const [groupError, setGroupError] = useState('')
  const [addingGroupMembersId, setAddingGroupMembersId] = useState('')
  const [selectedGroupMemberIds, setSelectedGroupMemberIds] = useState([])
  const [isAddingGroupMembers, setIsAddingGroupMembers] = useState(false)
  const [groupMembersError, setGroupMembersError] = useState('')
  const [isGroupFormOpen, setIsGroupFormOpen] = useState(false)
  const [groupNameDraft, setGroupNameDraft] = useState('')
  const [isSavingGroup, setIsSavingGroup] = useState(false)
  const [editingGroupId, setEditingGroupId] = useState('')
  const [selectedGroupId, setSelectedGroupId] = useState('')
  const [viewingGroupFollowersId, setViewingGroupFollowersId] = useState('')
  const [groupPosts, setGroupPosts] = useState([])
  const [groupPostsPreview, setGroupPostsPreview] = useState(null)
  const [canViewGroupPosts, setCanViewGroupPosts] = useState(false)
  const [loadingGroupPosts, setLoadingGroupPosts] = useState(false)
  const [groupPostsError, setGroupPostsError] = useState('')
  const [groupPostText, setGroupPostText] = useState('')
  const [groupPostFile, setGroupPostFile] = useState(null)
  const [groupPostPreviewUrl, setGroupPostPreviewUrl] = useState('')
  const [isCreatingGroupPost, setIsCreatingGroupPost] = useState(false)
  const [deletingGroupPostId, setDeletingGroupPostId] = useState('')
  const [openGroupMenuId, setOpenGroupMenuId] = useState('')
  const [openingGroupConversationId, setOpeningGroupConversationId] = useState('')
  const [deletingGroupId, setDeletingGroupId] = useState('')
  const [updatingGroupFollowId, setUpdatingGroupFollowId] = useState('')
  const [groupPhotoFile, setGroupPhotoFile] = useState(null)
  const [groupPhotoUrl, setGroupPhotoUrl] = useState('')
  const [groupPhotoPreview, setGroupPhotoPreview] = useState('')
  const [groupPhotoError, setGroupPhotoError] = useState('')
  const [connectingWithOthersEnabled, setConnectingWithOthersEnabled] = useState(() => {
    try {
      const stored = localStorage.getItem('truehunt_user')
      const user = stored ? JSON.parse(stored) : null
      const email = user?.email || ''
      if (email) {
        const settings = localStorage.getItem(`truehunt-social-settings:${String(email).trim().toLowerCase()}`)
        if (settings) {
          const parsed = JSON.parse(settings)
          if (parsed.notifications) {
            if (parsed.notifications.connections === false || parsed.notifications['connecting-with-others'] === false) {
              return false
            }
          }
        }
      }
    } catch (error) {
      console.warn('Could not read cached connection notification preferences:', error)
    }
    return true
  })
  const relationshipOverrides = useRef(new Map())
  const connectionGroupsLoadVersion = useRef(0)
  const groupPhotoPreviewObjectUrl = useRef('')
  const groupPostPreviewObjectUrl = useRef('')
  const groupPostMediaSafety = useMediaSafety(groupPostFile)
  const currentUserIdentityKey = JSON.stringify(currentUserIdentifiers
    .filter(Boolean)
    .map((identifier) => String(identifier).trim().toLowerCase()))
  const isProfileConnected = (profile) => {
    const keys = getProfileIdentityKeys(profile)
    return keys.some((k) => connectedProfiles.includes(k)) || Boolean(profile.isConnected)
  }
  const isProfilePending = (profile) => {
    const keys = getProfileIdentityKeys(profile)
    return keys.some((k) => pendingProfiles.includes(k)) || Boolean(profile.isPending)
  }
  const isProfileFollowed = (profile) => (
    getProfileIdentityKeys(profile).some((key) => followedProfiles.includes(key))
    || Boolean(profile.isFollowing)
  )
  const connectionSectionProfiles = getUniqueProfiles(
    profiles.filter((profile) => isProfileConnected(profile)),
  )
  const followingProfiles = getUniqueProfiles(
    profiles.filter((profile) => isProfileFollowed(profile)),
  )
  const followingAndFollowerProfiles = getUniqueProfiles([
    ...followingProfiles,
    ...followerProfiles,
  ])
  const requestSectionProfiles = getUniqueProfiles(
    profiles.filter((profile) => isProfilePending(profile)),
  )
  const suggestionProfiles = !connectingWithOthersEnabled ? [] : getUniqueProfiles(profiles.filter((profile) => (
    !isProfileConnected(profile)
    && !isProfilePending(profile)
    && !isProfileFollowed(profile)
    && profile.canConnect !== false
    && profile.profileVisibility !== false
    && profile.publicProfile !== false
    && profile.isPublicProfile !== false
  )))
  const displayedProfiles = (
    activeSection === 'connections'
      ? connectionSectionProfiles
      : activeSection === 'following'
        ? followingAndFollowerProfiles
        : activeSection === 'requests'
          ? requestSectionProfiles
          : []
  )
  useEffect(() => {
    if (!isActive || isAdmin) return undefined

    let isCurrent = true
    let lastLoadStartedAt = 0
    async function loadGroups() {
      if (!isCurrent || Date.now() - lastLoadStartedAt < 1000) return
      lastLoadStartedAt = Date.now()
      setLoadingConnectionGroups(true)
      setGroupLoadError('')
      const loadVersion = ++connectionGroupsLoadVersion.current
      try {
        const groups = await api.getConnectionGroups()
        if (isCurrent && loadVersion === connectionGroupsLoadVersion.current) {
          setConnectionGroups(Array.isArray(groups) ? groups : [])
        }
      } catch (error) {
        console.warn('Could not load connection groups:', error)
        if (isCurrent && loadVersion === connectionGroupsLoadVersion.current) {
          setGroupLoadError(error.message || 'Could not load your groups. Please try again.')
        }
      } finally {
        if (isCurrent && loadVersion === connectionGroupsLoadVersion.current) {
          setLoadingConnectionGroups(false)
        }
      }
    }

    function refreshWhenVisible() {
      if (!document.hidden) void loadGroups()
    }

    void loadGroups()
    window.addEventListener('focus', refreshWhenVisible)
    document.addEventListener('visibilitychange', refreshWhenVisible)

    return () => {
      isCurrent = false
      window.removeEventListener('focus', refreshWhenVisible)
      document.removeEventListener('visibilitychange', refreshWhenVisible)
    }
  }, [currentUserIdentityKey, isActive, isAdmin])

  useEffect(() => () => {
    if (groupPhotoPreviewObjectUrl.current) {
      URL.revokeObjectURL(groupPhotoPreviewObjectUrl.current)
    }
    if (groupPostPreviewObjectUrl.current) {
      URL.revokeObjectURL(groupPostPreviewObjectUrl.current)
    }
  }, [])

  useEffect(() => {
    if (!openGroupMenuId) return undefined

    function closeGroupMenu(event) {
      if (event.type === 'keydown' && event.key !== 'Escape') return
      if (event.type === 'pointerdown' && event.target.closest('.network-group-card-actions')) return
      setOpenGroupMenuId('')
    }

    document.addEventListener('pointerdown', closeGroupMenu)
    document.addEventListener('keydown', closeGroupMenu)
    return () => {
      document.removeEventListener('pointerdown', closeGroupMenu)
      document.removeEventListener('keydown', closeGroupMenu)
    }
  }, [openGroupMenuId])

  const selectedConnectionGroup = connectionGroups.find(
    (group) => String(group.id) === selectedGroupId,
  )
  const selectedGroupProfiles = selectedConnectionGroup
    ? (selectedConnectionGroup.memberProfileIds || []).map((memberId) => {
      const profile = findProfileByIdentity(memberId)
      if (profile) return profile
      const isCurrentUser = currentUserIdentifiers.some((identifier) => (
        String(identifier).trim().toLowerCase() === String(memberId).trim().toLowerCase()
      ))
      return isCurrentUser ? { id: memberId, userId: memberId, name: 'You' } : null
    }).filter(Boolean)
    : []
  const selectedGroupMemberKeys = new Set(
    (selectedConnectionGroup?.memberProfileIds || []).map((memberId) => String(memberId).trim().toLowerCase()),
  )
  const currentUserIdentityKeys = new Set(currentUserIdentifiers
    .filter(Boolean)
    .map((identifier) => String(identifier).trim().toLowerCase()))
  const creatorConnectionKeys = new Set(connectionSectionProfiles.flatMap(getProfileIdentityKeys))
  const creatorFollowerKeys = new Set(followerProfiles.flatMap(getProfileIdentityKeys))
  const addGroupMemberProfiles = selectedConnectionGroup
    ? getUniqueProfiles([...connectionSectionProfiles, ...followerProfiles]).filter((profile) => {
      const identityKeys = getProfileIdentityKeys(profile)
      return !identityKeys.some((key) => (
        selectedGroupMemberKeys.has(key) || currentUserIdentityKeys.has(key)
      ))
    })
    : []
  const selectedGroupMembersKey = (selectedConnectionGroup?.memberProfileIds || []).join('|')
  const addingGroupMembers = connectionGroups.find(
    (group) => String(group.id) === addingGroupMembersId,
  )
  const viewingConnectionGroup = connectionGroups.find(
    (group) => String(group.id) === viewingGroupFollowersId,
  )

  useEffect(() => {
    if (!isActive || activeSection !== 'groups' || !selectedGroupId) return undefined

    let isCurrent = true
    let lastLoadStartedAt = 0
    async function loadGroupPosts() {
      if (!isCurrent || Date.now() - lastLoadStartedAt < 1000) return
      lastLoadStartedAt = Date.now()
      setLoadingGroupPosts(true)
      setGroupPostsError('')
      try {
        const feed = await api.getConnectionGroupPosts(selectedGroupId)
        if (isCurrent) {
          setCanViewGroupPosts(Boolean(feed?.canViewAll))
          setGroupPosts(Array.isArray(feed?.posts) ? feed.posts : [])
          setGroupPostsPreview(feed?.preview || null)
        }
      } catch (error) {
        console.error('Could not load group member posts:', error)
        if (isCurrent) setGroupPostsError(error.message || 'Could not load posts from this group.')
      } finally {
        if (isCurrent) setLoadingGroupPosts(false)
      }
    }

    function refreshWhenVisible() {
      if (!document.hidden) void loadGroupPosts()
    }

    void loadGroupPosts()
    window.addEventListener('focus', refreshWhenVisible)
    document.addEventListener('visibilitychange', refreshWhenVisible)

    return () => {
      isCurrent = false
      window.removeEventListener('focus', refreshWhenVisible)
      document.removeEventListener('visibilitychange', refreshWhenVisible)
    }
  }, [activeSection, currentUserIdentityKey, isActive, selectedGroupId, selectedGroupMembersKey])

  function handleGroupPostFileChange(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    const supportedType = file.type.startsWith('image/') || file.type.startsWith('video/')
    if (!supportedType) {
      setGroupError('Choose an image or video file.')
      return
    }
    if (file.size > 50 * 1024 * 1024) {
      setGroupError('Group post attachments must be no larger than 50 MB.')
      return
    }

    if (groupPostPreviewObjectUrl.current) {
      URL.revokeObjectURL(groupPostPreviewObjectUrl.current)
    }
    groupPostPreviewObjectUrl.current = URL.createObjectURL(file)
    setGroupPostFile(file)
    setGroupPostPreviewUrl(groupPostPreviewObjectUrl.current)
    setGroupError('')
  }

  function cancelGroupPostDraft() {
    if (groupPostPreviewObjectUrl.current) {
      URL.revokeObjectURL(groupPostPreviewObjectUrl.current)
      groupPostPreviewObjectUrl.current = ''
    }
    setGroupPostFile(null)
    setGroupPostPreviewUrl('')
    setGroupPostText('')
    setGroupError('')
  }

  async function handleCreateGroupPost(event) {
    event.preventDefault()
    if (!selectedConnectionGroup?.isOwner || isCreatingGroupPost || groupPostMediaSafety.isBlocked) return

    const content = groupPostText.trim()
    if (!content && !groupPostFile) {
      setGroupError('Write a message or attach an image or video.')
      return
    }

    setIsCreatingGroupPost(true)
    setGroupError('')
    try {
      let mediaUrl = ''
      let mediaType = ''
      if (groupPostFile) {
        const upload = await api.uploadMedia(groupPostFile, 'connection-group-post')
        if (!upload?.url) {
          throw new Error('The group attachment upload did not return a media URL.')
        }
        mediaUrl = upload.url
        mediaType = groupPostFile.type
      }

      const post = await api.createConnectionGroupPost(selectedConnectionGroup.id, {
        content,
        mediaUrl,
        mediaType,
      })
      setGroupPosts((current) => [post, ...current])
      setGroupPostsPreview(post.mediaType?.startsWith('image/')
        ? { imageUrl: post.mediaUrl, createdAt: post.createdAt }
        : groupPostsPreview)
      setGroupPostText('')
      setGroupPostFile(null)
      setGroupPostPreviewUrl('')
      if (groupPostPreviewObjectUrl.current) {
        URL.revokeObjectURL(groupPostPreviewObjectUrl.current)
        groupPostPreviewObjectUrl.current = ''
      }
    } catch (error) {
      console.error('Could not publish group post:', error)
      setGroupError(error.message || 'Could not publish this group post. Please try again.')
    } finally {
      setIsCreatingGroupPost(false)
    }
  }

  async function handleDeleteGroupPost(post) {
    if (!selectedConnectionGroup?.isOwner || deletingGroupPostId) return
    if (!window.confirm('Delete this group update and its text and attachment? This cannot be undone.')) return

    const postId = String(post.id)
    setDeletingGroupPostId(postId)
    setGroupError('')
    try {
      await api.deleteConnectionGroupPost(selectedConnectionGroup.id, post.id)
      setGroupPosts((current) => current.filter((item) => String(item.id) !== postId))
    } catch (error) {
      console.error('Could not delete group update:', error)
      setGroupError(error.message || 'Could not delete this group update. Please try again.')
    } finally {
      setDeletingGroupPostId('')
    }
  }

  function updateGroupPhotoSelection(file, savedImageUrl = '') {
    if (groupPhotoPreviewObjectUrl.current) {
      URL.revokeObjectURL(groupPhotoPreviewObjectUrl.current)
    }
    groupPhotoPreviewObjectUrl.current = file ? URL.createObjectURL(file) : ''
    setGroupPhotoFile(file)
    setGroupPhotoPreview(groupPhotoPreviewObjectUrl.current)
    setGroupPhotoUrl(file ? '' : savedImageUrl)
    setGroupPhotoError('')
  }

  function handleGroupPhotoChange(event) {
    const selectedFile = event.target.files?.[0]
    event.target.value = ''
    if (!selectedFile) return

    const file = normalizeGroupPhotoFile(selectedFile)
    if (!file) {
      const selectedType = selectedFile.type || selectedFile.name.split('.').pop()?.toUpperCase() || 'unknown'
      setGroupPhotoError(
        `The selected file type (${selectedType}) is not supported. Choose an AVIF, BMP, GIF, JPEG, PNG, or WebP image.`,
      )
      return
    }
    if (file.size > MAX_GROUP_PHOTO_SIZE_BYTES) {
      const sizeInMegabytes = (file.size / (1024 * 1024)).toFixed(2)
      setGroupPhotoError(`Group photos must be no larger than 10 MB. This file is ${sizeInMegabytes} MB.`)
      return
    }

    setGroupError('')
    setGroupPhotoError('')
    updateGroupPhotoSelection(file)
  }

  async function resolveGroupPhotoUrl() {
    if (!groupPhotoFile) return groupPhotoUrl

    const uploadedPhoto = await api.uploadMedia(groupPhotoFile, 'connection-group-photo')
    if (!uploadedPhoto?.url) {
      throw new Error('The group photo upload did not return an image URL.')
    }
    return uploadedPhoto.url
  }

  function renderGroupPhotoField(inputId) {
    const preview = groupPhotoPreview || groupPhotoUrl
    return (
      <div className="network-group-photo-field">
        <label htmlFor={inputId}>Group photo</label>
        <div className="network-group-photo-field-controls">
          {preview ? (
            <img className="network-group-photo-preview" src={preview} alt="Group photo preview" />
          ) : (
            <span className="network-group-photo-preview is-empty" aria-hidden="true">
              <Image size={20} />
            </span>
          )}
          <div>
            <input
              id={inputId}
              type="file"
              accept="image/*"
              onChange={handleGroupPhotoChange}
              disabled={isSavingGroup}
            />
            <small aria-live="polite">
              {groupPhotoFile ? `Selected: ${groupPhotoFile.name}` : 'Optional · maximum 10 MB'}
            </small>
            {groupPhotoError && <small className="network-connection-error" role="alert">{groupPhotoError}</small>}
            {preview && (
              <button
                type="button"
                className="network-group-photo-remove"
                disabled={isSavingGroup}
                onClick={() => updateGroupPhotoSelection(null)}
              >
                Remove photo
              </button>
            )}
          </div>
        </div>
      </div>
    )
  }

  async function handleCreateGroup(event) {
    event.preventDefault()
    if (isSavingGroup) return

    const name = groupNameDraft.trim()
    if (!name) {
      setGroupError('Enter a group name.')
      return
    }
    setIsSavingGroup(true)
    setGroupError('')
    try {
      const createdGroup = await api.createConnectionGroup(
        name,
        [],
        await resolveGroupPhotoUrl(),
      )
      setConnectionGroups((current) => [createdGroup, ...current])
      setGroupNameDraft('')
      updateGroupPhotoSelection(null)
      setIsGroupFormOpen(false)
    } catch (error) {
      console.error('Could not create connection group:', error)
      setGroupError(error.message || 'Could not create this group. Please try again.')
    } finally {
      setIsSavingGroup(false)
    }
  }

  async function handleUpdateGroup(event, group) {
    event.preventDefault()
    if (isSavingGroup) return

    const name = groupNameDraft.trim()
    if (!name) {
      setGroupError('Enter a group name.')
      return
    }

    setIsSavingGroup(true)
    setGroupError('')
    try {
      const updatedGroup = await api.updateConnectionGroup(
        group.id,
        name,
        group.memberProfileIds || [],
        await resolveGroupPhotoUrl(),
      )
      connectionGroupsLoadVersion.current += 1
      setLoadingConnectionGroups(false)
      setConnectionGroups((current) => current.map((item) => (
        String(item.id) === String(updatedGroup.id) ? updatedGroup : item
      )))
      setSelectedGroupId((current) => (String(current) === String(group.id) ? String(updatedGroup.id) : current))
      setEditingGroupId('')
      setGroupNameDraft('')
      updateGroupPhotoSelection(null)
    } catch (error) {
      console.error('Could not update connection group:', error)
      setGroupError(error.message || 'Could not update this group. Please try again.')
    } finally {
      setIsSavingGroup(false)
    }
  }

  function openGroupMemberPicker(group) {
    if (!group?.isOwner || isAddingGroupMembers) return
    setSelectedGroupId(String(group.id))
    setSelectedGroupMemberIds([])
    setGroupMembersError('')
    setAddingGroupMembersId(String(group.id))
  }

  async function handleAddGroupMembers() {
    const group = connectionGroups.find((item) => String(item.id) === addingGroupMembersId)
    if (!group?.isOwner || isAddingGroupMembers) return

    const memberProfileIds = addGroupMemberProfiles
      .filter((profile) => selectedGroupMemberIds.includes(getProfileKey(profile)))
      .map((profile) => getProfileKey(profile))
      .filter(Boolean)
    if (!memberProfileIds.length) {
      setGroupMembersError('Select at least one profile to add.')
      return
    }

    setIsAddingGroupMembers(true)
    setGroupMembersError('')
    try {
      const updatedGroup = await api.addConnectionGroupMembers(group.id, memberProfileIds)
      setConnectionGroups((current) => current.map((item) => (
        String(item.id) === String(updatedGroup.id) ? updatedGroup : item
      )))
      setAddingGroupMembersId('')
      setSelectedGroupMemberIds([])
    } catch (error) {
      console.error('Could not add group members:', error)
      setGroupMembersError(error.message || 'Could not add these members. Please try again.')
    } finally {
      setIsAddingGroupMembers(false)
    }
  }

  async function handleDeleteGroup(group) {
    if (deletingGroupId || isSavingGroup) return
    if (!window.confirm(`Delete "${group.name}" and all its members? This cannot be undone.`)) return

    const groupId = String(group.id)
    setDeletingGroupId(groupId)
    setGroupError('')
    try {
      await api.deleteConnectionGroup(group.id)
      setConnectionGroups((current) => current.filter((item) => String(item.id) !== groupId))
      setSelectedGroupId((current) => current === groupId ? '' : current)
      if (editingGroupId === groupId) {
        setEditingGroupId('')
        setGroupNameDraft('')
        updateGroupPhotoSelection(null)
      }
    } catch (error) {
      console.error('Could not delete connection group:', error)
      setGroupError(error.message || 'Could not delete this group. Please try again.')
    } finally {
      setDeletingGroupId('')
    }
  }

  async function toggleGroupFollow(group) {
    const groupId = String(group.id)
    if (updatingGroupFollowId) return

    setUpdatingGroupFollowId(groupId)
    setGroupError('')
    try {
      const updatedGroup = group.isFollowing
        ? await api.unfollowConnectionGroup(group.id)
        : await api.followConnectionGroup(group.id)
      setConnectionGroups((current) => current.map((item) => (
        String(item.id) === groupId ? updatedGroup : item
      )))
    } catch (error) {
      console.error('Could not update connection group follow status:', error)
      setGroupError(error.message || 'Could not update this group follow. Please try again.')
    } finally {
      setUpdatingGroupFollowId('')
    }
  }

  function getGroupFollowers(group) {
    const followerIds = new Set((group.followerUserIds || []).filter(Boolean).map(
      (id) => String(id).trim().toLowerCase(),
    ))
    const profileIds = Array.from(new Set([
      ...(group.memberProfileIds || []),
      ...(group.followerUserIds || []),
    ].filter(Boolean).map((id) => String(id).trim())))
    const profilesByKey = new Map()

    profileIds.forEach((profileId) => {
      const profile = findProfileByIdentity(profileId)
      const isCurrentUser = currentUserIdentifiers.some((identifier) => (
        String(identifier).trim().toLowerCase() === profileId.toLowerCase()
      ))
      const resolvedProfile = profile || {
        id: profileId,
        userId: profileId,
        name: isCurrentUser ? 'You' : 'Group member',
        isGroupProfilePlaceholder: true,
      }
      const key = getProfileKey(resolvedProfile) || profileId.toLowerCase()
      profilesByKey.set(key, {
        ...resolvedProfile,
        groupRelationship: followerIds.has(profileId.toLowerCase()) ? 'Follower' : 'Member',
      })
    })

    return Array.from(profilesByKey.values())
  }

  function startEditingGroup(group) {
    const groupId = String(group.id)
    setGroupError('')
    setIsGroupFormOpen(false)
    setOpenGroupMenuId('')
    setEditingGroupId(groupId)
    setGroupNameDraft(group.name)
    updateGroupPhotoSelection(null, group.imageUrl || '')
  }

  async function handleOpenGroupConversation(group) {
    if (openingGroupConversationId) return

    const groupId = String(group.id)
    setOpeningGroupConversationId(groupId)
    setGroupError('')
    try {
      const conversation = await api.openConnectionGroupConversation(group.id)
      if (!conversation?.id) {
        throw new Error('Could not open this group conversation. Please try again.')
      }
      onMessage?.(conversation)
    } catch (error) {
      console.error('Could not open the group conversation:', error)
      setGroupError(error.message || 'Could not open this group conversation. Please try again.')
    } finally {
      setOpeningGroupConversationId('')
    }
  }

  function renderGroupPhoto(imageUrl = '', isLarge = false) {
    if (imageUrl) {
      return (
        <span
          className={`network-group-photo is-custom${isLarge ? ' is-large' : ''}`}
          aria-hidden="true"
        >
          <img src={imageUrl} alt="" />
        </span>
      )
    }

    return (
      <span
        className={`network-group-photo${isLarge ? ' is-large' : ''}`}
        aria-hidden="true"
      >
        <UsersRound size={isLarge ? 30 : 24} aria-hidden="true" />
      </span>
    )
  }

  function openConversation(profile) {
    if (!profile?.name) return
    const profileKey = getProfileKey(profile)
    const isConnected = connectedProfiles.includes(profileKey) || Boolean(profile.isConnected)
    onMessage?.({
      ...profile,
      isConnected,
      networkMessageLimit: true,
    })
  }

  function findProfileByIdentity(profileOrId) {
    const identity = getProfileKey(profileOrId)
    return profiles.find((profile) => getProfileIdentityKeys(profile).includes(identity))
  }

  function getInvitationProfile(invitation) {
    const profile = invitation?.profile || {}
    const matchedProfile = profiles.find((candidate) => (
      profilesMatch(candidate, profile) || profilesMatch(candidate, invitation)
    ))
    const profileId = getProfileKey(profile)
      || getProfileKey(invitation?.profileId)
      || getProfileKey(invitation?.senderId)
      || getProfileKey(invitation?.userId)
      || getProfileKey(invitation)
    const name = getProfileName(profile, matchedProfile, invitation) || 'Profile'
    const profileImageUrl = profile.profileImageUrl
      || profile.avatar
      || matchedProfile?.profileImageUrl
      || matchedProfile?.avatar
      || ''
    return {
      ...matchedProfile,
      ...profile,
      id: matchedProfile?.id || profile.id || profile.userId || invitation?.profileId || invitation?.senderId || invitation?.userId || profileId,
      userId: matchedProfile?.userId || profile.userId || profile.id || invitation?.profileId || invitation?.senderId || invitation?.userId || profileId,
      name,
      initials: profile.initials || matchedProfile?.initials || name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'TH',
      profileImageUrl,
      avatar: profile.avatar || profileImageUrl,
      role: profile.role || matchedProfile?.role || matchedProfile?.headline || 'Cinema Professional',
      location: profile.location || matchedProfile?.location || matchedProfile?.address || '',
    }
  }

  const loadNetworkData = useCallback(async () => {
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
      const [registeredProfiles, serverConnected, suggestionsData, invitationsData, followersData, catchUpData] = await Promise.all([
        api.getProfileDirectory().catch(() => []),
        api.getConnectedProfiles().catch(() => []),
        api.getNetworkSuggestions().catch(() => []),
        api.getInvitations().catch(() => []),
        api.getFollowers().catch(() => []),
        api.getNetworkCatchUp().catch(() => []),
      ])
      setFollowerProfiles(Array.isArray(followersData) ? followersData : [])
      setCatchUpUpdates(Array.isArray(catchUpData) ? catchUpData : [])

      const networkProfiles = Array.isArray(suggestionsData)
        ? suggestionsData.filter((profile) => !isDemoNetworkProfile(profile))
        : []
      const incomingInvitations = Array.isArray(invitationsData)
        ? invitationsData.filter((invitation) => !isDemoNetworkProfile({
          ...(invitation.profile || {}),
          ...invitation,
        }))
        : []
      const connectedList = Array.isArray(serverConnected)
        ? serverConnected.filter((profile) => !isDemoNetworkProfile(profile))
        : []

      setInvitations(incomingInvitations)
      const directory = []
      const profilesToMerge = [
        ...(Array.isArray(registeredProfiles) ? registeredProfiles : []),
        ...(Array.isArray(connectedList) ? connectedList : []),
        ...(Array.isArray(networkProfiles) ? networkProfiles : []),
        ...(Array.isArray(incomingInvitations) ? incomingInvitations.map((inv) => ({
          ...(inv.profile || {}),
          ...inv,
          profile: inv.profile || {},
        })) : []),
      ]
      profilesToMerge.filter((profile) => profile && getProfileIdentityKeys(profile).length > 0).forEach((sourceProfile) => {
        const profileId = getProfileKey(sourceProfile)
        const profile = {
          ...sourceProfile,
          id: sourceProfile.id || sourceProfile.userId || sourceProfile.profileId || sourceProfile.senderId || profileId,
          userId: sourceProfile.userId || sourceProfile.id || sourceProfile.profileId || sourceProfile.senderId || profileId,
          name: getProfileName(sourceProfile),
        }
        const identityKeys = new Set(getProfileIdentityKeys(profile))
        const normalizedName = getNormalizedProfileName(profile)
        const isGenericName = !normalizedName
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
          const existingNameIsGeneric = !getNormalizedProfileName(existingProfile)
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

      const networkProfilesById = new Map()
      networkProfiles.forEach((profile) => {
        getProfileIdentityKeys(profile).forEach((key) => networkProfilesById.set(key, profile))
      })

      const serverConnectedKeys = new Set(
        connectedList.flatMap((profile) => profile ? getProfileIdentityKeys(profile) : []),
      )

      // Stored overrides can be cleared once fresh server data is loaded
      relationshipOverrides.current.clear()

      const allProfiles = directory
        .filter((profile) => profile && getProfileKey(profile) && profile.name)
        .filter((profile) => {
          const profileIdentifiers = getProfileIdentityKeys(profile)
          return !profileIdentifiers.some((identifier) => normalizedCurrentUserIdentifiers.has(identifier))
        })
        .filter((profile) => {
          if (profile.profileVisibility === false || profile.publicProfile === false || profile.isPublicProfile === false) {
            const profileIdentityKeys = getProfileIdentityKeys(profile)
            return profileIdentityKeys.some((key) => serverConnectedKeys.has(key))
          }
          return true
        })
        .map((profile, index) => {
          const profileIdentityKeys = getProfileIdentityKeys(profile)
          const registeredProfile = (Array.isArray(registeredProfiles) ? registeredProfiles : []).find((item) => (
            profilesMatch(item, profile)
          ))
          const profileImageUrl = registeredProfile?.profileImageUrl
            || registeredProfile?.avatar
            || profile.profileImageUrl
            || profile.avatar
            || ''
          const id = String(profile.id || profile.userId)
          const name = getProfileName(profile) || 'Profile'
          const nameParts = name.split(/\s+/)
          const connection = profileIdentityKeys
            .map((key) => networkProfilesById.get(key))
            .find(Boolean)
          const isConn = profileIdentityKeys.some((key) => serverConnectedKeys.has(key))
            || Boolean(connection?.isConnected)
          const isPend = !isConn && Boolean(connection?.isPending || connection?.isOutgoingPending || profile.isPending)

          const isOffline = profile.activeStatus === false
            || profile.activityStatus === false
            || connection?.activeStatus === false
            || connection?.activityStatus === false
            || connection?.status === 'offline'
            || profile.status === 'offline'
          const presenceStatus = isOffline ? 'offline' : 'active'

          return {
            ...profile,
            id,
            name,
            initials: profile.initials || nameParts.slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'TH',
            status: presenceStatus,
            activeStatus: !isOffline,
            activityStatus: !isOffline,
            role: profile.headline || profile.role || profile.primaryCraft || profile.category || '',
            location: profile.location || profile.address || '',
            mutualConnections: getMutualConnectionsLabel(connection?.mutualConnections),
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
        if (!cKey || getProfileIdentityKeys(conn).some((key) => normalizedCurrentUserIdentifiers.has(key))) continue
        const existingIdx = allProfiles.findIndex((profile) => profilesMatch(profile, conn))
        if (existingIdx !== -1) {
          allProfiles[existingIdx].isConnected = true
          allProfiles[existingIdx].isPending = false
        } else {
          const name = getProfileName(conn) || 'Profile'
          const nameParts = name.split(/\s+/)
          const isOffline = conn.activeStatus === false
            || conn.activityStatus === false
            || conn.status === 'offline'
          const presenceStatus = isOffline ? 'offline' : 'active'
          allProfiles.unshift({
            ...conn,
            id: conn.id || conn.userId || conn.profileId || cKey,
            userId: conn.userId || conn.id || conn.profileId || cKey,
            name,
            initials: conn.initials || nameParts.slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'TH',
            status: presenceStatus,
            activeStatus: !isOffline,
            activityStatus: !isOffline,
            role: conn.role || conn.headline || conn.primaryCraft || '',
            location: conn.location || conn.address || '',
            mutualConnections: getMutualConnectionsLabel(conn.mutualConnections),
            avatarClass: 'network-avatar-coral',
            profileImageUrl: conn.profileImageUrl || conn.avatar || '',
            coverImageUrl: conn.coverImageUrl || conn.coverImage || '',
            isConnected: true,
            isFollowing: Boolean(conn.isFollowing || conn.following),
            isPending: false,
          })
        }
      }

      const profilesWithPendingAvatars = await Promise.all(allProfiles.map(async (profile) => {
        if (!profile.isPending || profile.profileImageUrl || profile.avatar) return profile

        const profileId = profile.userId || profile.id
        if (!profileId) return profile

        try {
          const fullProfile = await api.getProfileById(profileId)
          const profileImageUrl = fullProfile?.profileImageUrl || fullProfile?.avatar || ''
          return profileImageUrl
            ? { ...profile, profileImageUrl, avatar: profileImageUrl }
            : profile
        } catch (error) {
          console.warn(`Could not load the profile image for ${profile.name}:`, error)
          return profile
        }
      }))

      setProfiles(profilesWithPendingAvatars)
      const allConnectedKeys = Array.from(new Set([
        ...connectedList.flatMap((p) => getProfileIdentityKeys(p)),
        ...allProfiles.filter((profile) => profile.isConnected).flatMap((profile) => getProfileIdentityKeys(profile)),
      ])).filter(Boolean)
      setConnectedProfiles(allConnectedKeys)
      setFollowedProfiles(profilesWithPendingAvatars.filter((profile) => profile.isFollowing).flatMap((profile) => getProfileIdentityKeys(profile)))
      setPendingProfiles(profilesWithPendingAvatars.filter((profile) => profile.isPending).flatMap((profile) => getProfileIdentityKeys(profile)))
    } catch (err) {
      console.warn('Network profiles load error:', err)
    } finally {
      setLoadingProfiles(false)
    }
  }, [currentUserIdentityKey, isAdmin])

  useEffect(() => {
    if (!isActive) return undefined

    let disposed = false
    Promise.resolve().then(() => {
      if (!disposed) loadNetworkData()
    })

    const handleFocus = () => {
      if (!document.hidden) loadNetworkData()
    }
    const handleVisibility = () => {
      if (!document.hidden) loadNetworkData()
    }

    window.addEventListener('focus', handleFocus)
    document.addEventListener('visibilitychange', handleVisibility)

    return () => {
      disposed = true
      window.removeEventListener('focus', handleFocus)
      document.removeEventListener('visibilitychange', handleVisibility)
    }
  }, [isActive, loadNetworkData])

  useEffect(() => {
    function showNetworkHome() {
      setActiveTab('grow')
      setActiveSection('grow')
    }

    function handleSettingsUpdate(event) {
      const notifs = event.detail?.notifications || event.detail?.notificationPreferences
      if (notifs) {
        if (notifs.connections === false || notifs['connecting-with-others'] === false) {
          setConnectingWithOthersEnabled(false)
        } else if (notifs.connections === true || notifs['connecting-with-others'] === true) {
          setConnectingWithOthersEnabled(true)
        }
      }
      if (event.detail?.privacyPreferences) {
        loadNetworkData()
      }
    }

    window.addEventListener('truehunt-network-navigate', showNetworkHome)
    window.addEventListener('truehunt-settings-updated', handleSettingsUpdate)
    return () => {
      window.removeEventListener('truehunt-network-navigate', showNetworkHome)
      window.removeEventListener('truehunt-settings-updated', handleSettingsUpdate)
    }
  }, [])

  useEffect(() => {
    function handleConnectionChanged(event) {
      if (isAdmin) return
      const detail = event.detail || {}
      const {
        action,
        connected: connectedValue,
        isConnected,
        pending: pendingValue,
        isPending,
        profile,
        senderProfile,
        receiverProfile,
        senderId,
        receiverId,
        profileId: eventProfileId,
      } = detail
      const connected = typeof connectedValue === 'boolean' ? connectedValue : Boolean(isConnected)
      const pending = typeof pendingValue === 'boolean' ? pendingValue
        : typeof isPending === 'boolean' ? isPending : undefined
      const currentIds = new Set(JSON.parse(currentUserIdentityKey || '[]'))
      const matchesCurrentUser = (value) => {
        const keys = typeof value === 'object' && value !== null
          ? getProfileIdentityKeys(value)
          : [String(value || '').trim().toLowerCase()]
        return keys.some((key) => currentIds.has(key))
      }
      const hasParticipants = Boolean(senderId || receiverId || senderProfile || receiverProfile)
      let targetProfile
      let isCancelledInvitationRecipient = false

      if (hasParticipants) {
        const isSender = matchesCurrentUser(senderProfile) || matchesCurrentUser(senderId)
        const isReceiver = matchesCurrentUser(receiverProfile) || matchesCurrentUser(receiverId)
        if (isSender === isReceiver) return
        isCancelledInvitationRecipient = action === 'REQUEST_CANCELLED' && isReceiver
        targetProfile = isSender
          ? receiverProfile || { id: receiverId }
          : senderProfile || { id: senderId }
      } else {
        targetProfile = profile || (eventProfileId ? { id: eventProfileId } : null)
      }
      if (!targetProfile) return
      if (isDemoNetworkProfile(targetProfile)) return

      loadNetworkData()
      const profileId = getProfileKey(targetProfile)
      if (!profileId) return
      const profileKey = getProfileKey(targetProfile)
      relationshipOverrides.current.set(profileKey, {
        ...relationshipOverrides.current.get(profileKey),
        connected,
        pending: typeof pending === 'boolean' ? pending : false,
      })

      const targetIdentityKeys = new Set(getProfileIdentityKeys(targetProfile))
      setConnectedProfiles((current) => (
        connected
          ? Array.from(new Set([...current, ...targetIdentityKeys]))
          : current.filter((id) => !targetIdentityKeys.has(id))
      ))
      if (connected) {
        setPendingProfiles((current) => current.filter((id) => !targetIdentityKeys.has(id)))
      } else if (typeof pending === 'boolean') {
        setPendingProfiles((current) => (
          pending
            ? Array.from(new Set([...current, ...targetIdentityKeys]))
            : current.filter((id) => !targetIdentityKeys.has(id))
        ))
      }
      if (connected || isCancelledInvitationRecipient) {
        setInvitations((current) => current.filter((inv) => !profilesMatch(inv, targetProfile)))
      }
      setProfiles((current) => {
        const exists = current.some((item) => profilesMatch(item, targetProfile))
        if (!exists) {
          const name = getProfileName(targetProfile) || 'Profile'
          const nameParts = name.split(/\s+/)
          return [{
            ...targetProfile,
            id: targetProfile.id || targetProfile.userId || profileId,
            userId: targetProfile.userId || targetProfile.id || profileId,
            name,
            initials: targetProfile.initials || nameParts.slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'TH',
            status: targetProfile.status || '',
            role: targetProfile.role || targetProfile.headline || '',
            location: targetProfile.location || '',
            mutualConnections: '',
            avatarClass: 'network-avatar-coral',
            profileImageUrl: targetProfile.profileImageUrl || targetProfile.avatar || '',
            coverImageUrl: targetProfile.coverImageUrl || targetProfile.coverImage || '',
            isConnected: Boolean(connected),
            isPending: typeof pending === 'boolean' ? Boolean(pending) : false,
          }, ...current]
        }
        return current.map((item) => (
          profilesMatch(item, targetProfile)
            ? {
                ...item,
                isConnected: connected,
                isPending: typeof pending === 'boolean' ? pending : (connected ? false : false),
              }
            : item
        ))
      })
    }

    function handleFollowChanged(event) {
      if (isAdmin) return
      const { following, profile } = event.detail || {}
      const profileId = getProfileKey(profile)
      if (!profileId || isDemoNetworkProfile(profile)) return
      const profileKey = getProfileKey(profile)
      relationshipOverrides.current.set(profileKey, {
        ...relationshipOverrides.current.get(profileKey),
        following: Boolean(following),
      })

      const targetIdentityKeys = new Set(getProfileIdentityKeys(profile))
      setFollowedProfiles((current) => (
        following
          ? Array.from(new Set([...current, ...targetIdentityKeys]))
          : current.filter((id) => !targetIdentityKeys.has(id))
      ))
      setProfiles((current) => {
        const hasProfile = current.some((item) => profilesMatch(item, profile))
        const updatedProfiles = current.map((item) => (
          profilesMatch(item, profile) ? { ...item, isFollowing: Boolean(following) } : item
        ))
        if (!following || hasProfile) return updatedProfiles

        const name = getProfileName(profile) || 'Profile'
        return [...updatedProfiles, {
          ...profile,
          id: profileId,
          userId: profile.userId || profileId,
          name,
          initials: profile.initials || name.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase(),
          status: profile.status || '',
          role: profile.role || profile.headline || profile.primaryCraft || profile.category || '',
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
  }, [currentUserIdentityKey, isAdmin, loadNetworkData])

  async function handleAcceptInvitation(profileId, invitation) {
    if (processingInvitationId) return
    const profileKey = getProfileKey(findProfileByIdentity(profileId) || invitation?.profile || invitation || profileId)
    setProcessingInvitationId(profileId)
    try {
      await api.acceptConnectionRequest(profileId)
      relationshipOverrides.current.set(profileKey, {
        ...relationshipOverrides.current.get(profileKey),
        connected: true,
        pending: false,
      })
      const profileData = findProfileByIdentity(profileId)
        || {
          ...(invitation?.profile || {}),
          id: invitation?.profileId || invitation?.profile?.id || profileId,
          userId: invitation?.profile?.userId || invitation?.profileId || profileId,
          name: getProfileName(invitation?.profile, invitation) || 'Profile',
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
    const profileKey = getProfileKey(findProfileByIdentity(profileId) || profileId)
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
    const profile = findProfileByIdentity(profileId)
    const profileKey = getProfileKey(profile || profileId)
    const targetProfile = profile || { id: profileId }
    const targetIdentityKeys = new Set(getProfileIdentityKeys(targetProfile))
    setConnectionError('')
    setUpdatingConnectionProfileId(profileId)
    try {
      await api.cancelConnectionRequest(profileId)
      relationshipOverrides.current.set(profileKey, {
        ...relationshipOverrides.current.get(profileKey),
        connected: false,
        pending: false,
      })
      setPendingProfiles((current) => current.filter((id) => !targetIdentityKeys.has(id)))
      setProfiles((current) => current.map((item) => (
        profilesMatch(item, targetProfile)
          ? { ...item, isConnected: false, isPending: false }
          : item
      )))
      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: { connected: false, pending: false, profile: targetProfile },
      }))
      setActiveSection('grow')
    } catch (err) {
      console.error('Cancel connection request error:', err)
      setConnectionError(err instanceof Error && err.message
        ? err.message
        : 'Could not remove connection request. Please try again.')
    } finally {
      setUpdatingConnectionProfileId('')
    }
  }

  async function toggleConnection(profileId) {
    if (updatingConnectionProfileId) return
    const profile = findProfileByIdentity(profileId)
    const profileKey = getProfileKey(profile || profileId)
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
        profile && profilesMatch(item, profile)
          ? { ...item, isConnected: connected, isPending: pending }
          : item
      )))
      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: { connected, profile },
      }))
    } catch (err) {
      console.error('Backend toggle connection error:', err)
      setConnectionError(err.message || 'Could not update this connection. Please try again.')
    } finally {
      setUpdatingConnectionProfileId('')
    }
  }

  async function sendConnectionRequest(withNote) {
    if (!connectDialogProfile || updatingConnectionProfileId) return

    const profile = connectDialogProfile
    const profileKey = getProfileKey(profile)
    setConnectionError('')
    setUpdatingConnectionProfileId(profile.id)
    try {
      const note = withNote ? connectionNote.trim() : ''
      const result = await api.sendConnectionRequest(profile.id, note)
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
        profilesMatch(item, profile) ? { ...item, isConnected: connected, isPending: pending } : item
      )))
      window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
        detail: { connected, pending, profile },
      }))
      setConnectDialogProfile(null)
      setConnectionNote('')
      loadNetworkData()
    } catch (error) {
      console.error('Could not send connection request:', error)
      setConnectionError(error.message || 'Could not send this connection request. Please try again.')
    } finally {
      setUpdatingConnectionProfileId('')
    }
  }

  async function toggleFollow(profileId) {
    if (updatingFollowProfileId) return
    const profile = findProfileByIdentity(profileId)
    const profileKey = getProfileKey(profile || profileId)
    const targetProfile = profile || { id: profileId }
    const targetIdentityKeys = new Set(getProfileIdentityKeys(targetProfile))
    const isFollowing = [...targetIdentityKeys].some((key) => followedProfiles.includes(key))
      || Boolean(profile?.isFollowing)
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
          ? Array.from(new Set([...current, ...targetIdentityKeys]))
          : current.filter((id) => !targetIdentityKeys.has(id))
      ))
      setProfiles((current) => current.map((item) => (
        profilesMatch(item, targetProfile) ? { ...item, isFollowing: following } : item
      )))
      window.dispatchEvent(new CustomEvent('truehunt-follow-changed', {
        detail: { following, profile: targetProfile },
      }))
      if (!following) setActiveSection('grow')
    } catch (err) {
      console.error('Backend toggle follow error:', err)
      setFollowError('Could not update this follow. Please try again.')
    } finally {
      setUpdatingFollowProfileId('')
    }
  }

  function renderProfileCard(profile, isRequestView = false, isConnectedView = false) {
    const isConnected = isProfileConnected(profile)
    const isPending = isProfilePending(profile)
    const isFollowing = getProfileIdentityKeys(profile).some((k) => followedProfiles.includes(k)) || Boolean(profile.isFollowing)

    return (
      <article className="network-profile-card" key={profile.id || profile.userId}>
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
            className={`network-presence-dot is-${(profile.activeStatus === false || profile.activityStatus === false || profile.status === 'offline') ? 'offline' : 'active'}`}
            role="img"
            aria-label={`${profile.name} is ${(profile.activeStatus === false || profile.activityStatus === false || profile.status === 'offline') ? 'offline' : 'active'}`}
          />
        </div>
        <div className="network-profile-content">
          <h3>{profile.name}</h3>
          <p className="network-profile-role">{profile.role}</p>
          {getProfileLocationSummary(profile) && (
            <p className="network-profile-location">{getProfileLocationSummary(profile)}</p>
          )}
          {profile.mutualConnections && (
            <p className="network-mutual-connections">
              <UsersRound size={14} aria-hidden="true" />
              {profile.mutualConnections}
            </p>
          )}
          <div className="network-profile-actions">
            <button
              className={`network-connect-button${isConnected ? ' is-connected' : isPending ? ' is-pending' : ''}`}
              type="button"
              disabled={Boolean(updatingConnectionProfileId) || isRequestView}
              aria-pressed={isConnected}
              onClick={() => {
                if (isConnected || isPending) {
                  toggleConnection(profile.id)
                  return
                }
                if (!connectingWithOthersEnabled) {
                  setConnectionError('You have turned off connecting with others in your settings.')
                  return
                }
                setConnectionNote('')
                setConnectionError('')
                setConnectDialogProfile(profile)
              }}
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
          {isRequestView && (
            <button
              className="network-remove-request-button"
              type="button"
              disabled={Boolean(updatingConnectionProfileId)}
              onClick={() => handleRemoveRequest(profile.id)}
            >
              <UserX size={15} aria-hidden="true" />
              {updatingConnectionProfileId === profile.id ? 'Removing request…' : 'Remove request'}
            </button>
          )}
          <div className="network-profile-contact-actions">
            {isConnectedView ? (
              <button
                className="network-message-button"
                type="button"
                onClick={() => openConversation(profile)}
              >
                <MessageCircle size={15} aria-hidden="true" />
                Message
              </button>
            ) : (
              <>
                <button
                  className="network-profile-view-button"
                  type="button"
                  onClick={() => onProfileSelect?.(profile)}
                >
                  <UserRound size={15} aria-hidden="true" />
                  View profile
                </button>
                {!isRequestView && (
                  <button
                    className="network-message-button"
                    type="button"
                    onClick={() => openConversation(profile)}
                  >
                    <MessageCircle size={15} aria-hidden="true" />
                    Message
                  </button>
                )}
              </>
            )}
          </div>
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
              {networkSections.map(({ label, Icon }) => {
                const isSelected = (
                  (label === 'Connections' && activeSection === 'connections')
                  || (label === 'Following & followers' && activeSection === 'following')
                  || (label === 'Requests' && activeSection === 'requests')
                  || (label === 'Groups' && activeSection === 'groups')
                  || (label === 'Banner' && activeSection === 'banner')
                )
                const displayCount = (
                  label === 'Connections'
                    ? connectionSectionProfiles.length
                    : label === 'Following & followers'
                      ? followingAndFollowerProfiles.length
                      : label === 'Requests'
                        ? requestSectionProfiles.length
                        : label === 'Groups'
                          ? connectionGroups.length
                          : ''
                )
                return (
                  <button
                    className={`network-manage-item${isSelected ? ' is-selected' : ''}`}
                    key={label}
                    type="button"
                    aria-pressed={isSelected}
                    onClick={() => {
                      setSelectedGroupId('')
                      setActiveSection(
                        label === 'Connections'
                          ? 'connections'
                          : label === 'Following & followers'
                            ? 'following'
                            : label === 'Requests'
                              ? 'requests'
                              : label === 'Groups'
                                ? 'groups'
                                : label === 'Banner'
                                  ? 'banner'
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
        </aside>

        <div className="network-main">
          {activeSection === 'groups' ? (
            selectedConnectionGroup ? (
              <section className="network-suggestions network-group-detail" aria-labelledby="network-group-detail-title">
                <button
                  className="network-connections-back"
                  type="button"
                  onClick={() => setSelectedGroupId('')}
                >
                  <ArrowLeft size={16} aria-hidden="true" />
                  Back to groups
                </button>
                <header className="network-group-detail-heading">
                  {renderGroupPhoto(
                    selectedConnectionGroup.imageUrl,
                    true,
                  )}
                  <div>
                    <span className="network-section-eyebrow">YOUR NETWORK</span>
                    <h2 id="network-group-detail-title">{selectedConnectionGroup.name}</h2>
                    <p>
                      {selectedGroupProfiles.length}{' '}
                      {selectedGroupProfiles.length === 1 ? 'profile' : 'profiles'} in this group
                    </p>
                  </div>
                  {selectedConnectionGroup.isOwner && (
                    <button
                      className="network-group-add-members-button"
                      type="button"
                      disabled={isAddingGroupMembers}
                      onClick={() => openGroupMemberPicker(selectedConnectionGroup)}
                    >
                      Add members
                    </button>
                  )}
                  {selectedConnectionGroup.isOwner && (
                    <button
                      className="network-group-edit-button"
                      type="button"
                      disabled={Boolean(deletingGroupId) || isSavingGroup}
                      onClick={() => {
                        setSelectedGroupId('')
                        startEditingGroup(selectedConnectionGroup)
                      }}
                    >
                      Edit group
                    </button>
                  )}
                  {selectedConnectionGroup.isOwner && (
                    <button
                      className="network-group-delete-button"
                      type="button"
                      disabled={Boolean(deletingGroupId)}
                      onClick={() => handleDeleteGroup(selectedConnectionGroup)}
                    >
                      {deletingGroupId === String(selectedConnectionGroup.id) ? 'Deleting…' : 'Delete group'}
                    </button>
                  )}
                  {!selectedConnectionGroup.isOwner && (
                    <button
                      className={`network-group-follow-button${selectedConnectionGroup.isFollowing ? ' is-following' : ''}`}
                      type="button"
                      disabled={Boolean(updatingGroupFollowId)}
                      aria-pressed={Boolean(selectedConnectionGroup.isFollowing)}
                      onClick={() => void toggleGroupFollow(selectedConnectionGroup)}
                    >
                      {updatingGroupFollowId === String(selectedConnectionGroup.id)
                        ? 'Updating…'
                        : selectedConnectionGroup.isFollowing ? 'Unfollow' : 'Follow'}
                    </button>
                  )}
                  {selectedConnectionGroup.isOwner && (
                    <button
                      className="network-group-followers-button"
                      type="button"
                      onClick={() => setViewingGroupFollowersId(String(selectedConnectionGroup.id))}
                    >
                      View followers ({selectedConnectionGroup.followerCount || 0})
                    </button>
                  )}
                </header>
                {groupError && <p className="network-connection-error" role="alert">{groupError}</p>}
                <section className="network-group-posts" aria-labelledby="network-group-posts-title">
                  <h3 id="network-group-posts-title">Group updates</h3>
                  {selectedConnectionGroup.isOwner && (
                    <form className="network-group-post-form" onSubmit={handleCreateGroupPost}>
                      <label htmlFor="network-group-post-text">Share an update with group followers</label>
                      <textarea
                        id="network-group-post-text"
                        value={groupPostText}
                        onChange={(event) => setGroupPostText(event.target.value)}
                        maxLength={10000}
                        rows={4}
                        placeholder="Write a group update..."
                        disabled={isCreatingGroupPost}
                      />
                      {groupPostPreviewUrl && groupPostFile && (
                        <div className="network-group-post-file-preview">
                          {groupPostFile.type.startsWith('video/')
                            ? <video src={groupPostPreviewUrl} controls />
                            : <img src={groupPostPreviewUrl} alt="Group post preview" />}
                          <button
                            type="button"
                            disabled={isCreatingGroupPost}
                            onClick={() => {
                              if (groupPostPreviewObjectUrl.current) {
                                URL.revokeObjectURL(groupPostPreviewObjectUrl.current)
                                groupPostPreviewObjectUrl.current = ''
                              }
                              setGroupPostFile(null)
                              setGroupPostPreviewUrl('')
                            }}
                          >
                            Remove attachment
                          </button>
                        </div>
                      )}
                      <MediaSafetyStatus
                        safety={groupPostMediaSafety}
                        blockedMessage={`This ${groupPostMediaSafety.kind} has high adult content, so we can't post this ${groupPostMediaSafety.kind}.`}
                        safeMessage="Safe content verified. Ready to post."
                      />
                      <div className="network-group-post-form-actions">
                        <label htmlFor="network-group-post-file">Attach image or video</label>
                        <input
                          id="network-group-post-file"
                          type="file"
                          accept="image/*,video/*"
                          onChange={handleGroupPostFileChange}
                          disabled={isCreatingGroupPost}
                        />
                        <button type="submit" disabled={isCreatingGroupPost || groupPostMediaSafety.isBlocked}>
                          {isCreatingGroupPost ? 'Posting…' : 'Post update'}
                        </button>
                        <button
                          type="button"
                          className="network-group-post-cancel"
                          disabled={isCreatingGroupPost || (!groupPostText.trim() && !groupPostFile)}
                          onClick={cancelGroupPostDraft}
                        >
                          Cancel
                        </button>
                      </div>
                    </form>
                  )}
                  {groupError && <p className="network-connection-error" role="alert">{groupError}</p>}
                  {groupPostsError ? (
                    <p className="network-connection-error" role="alert">{groupPostsError}</p>
                  ) : loadingGroupPosts ? (
                    <p role="status">Loading group updates…</p>
                  ) : canViewGroupPosts ? (
                    groupPosts.length ? (
                      <div className="network-group-post-list">
                        {groupPosts.map((post) => {
                          const author = selectedGroupProfiles.find((profile) => profilesMatch(
                            profile,
                            { id: post.authorId, userId: post.authorId, email: post.authorId },
                          )) || (currentUserIdentifiers.some((identifier) => (
                            getProfileIdentityKeys({ id: post.authorId, userId: post.authorId, email: post.authorId })
                              .includes(String(identifier).trim().toLowerCase())
                          )) ? { name: 'You' } : null)
                          const authorName = author?.name
                            || author?.fullName
                            || post.authorName
                            || 'Group creator'
                          const avatar = author?.profileImageUrl || author?.avatar || ''
                          const content = String(post.content || '').trim()
                          const mediaUrl = post.mediaUrl || ''
                          const isVideo = String(post.mediaType || '').startsWith('video/')

                          return (
                            <article className="network-group-post-card" key={post.id}>
                              <header>
                                <span className="network-group-post-author">
                                  {avatar
                                    ? <img src={avatar} alt="" />
                                    : <span aria-hidden="true">{authorName.slice(0, 1).toUpperCase()}</span>}
                                  <strong>{authorName}</strong>
                                </span>
                                <span className="network-group-post-meta">
                                  {post.createdAt && <time>{post.createdAt}</time>}
                                  {selectedConnectionGroup.isOwner
                                    && String(post.authorId) === String(selectedConnectionGroup.ownerId) && (
                                    <button
                                      className="network-group-post-delete"
                                      type="button"
                                      aria-label={`Delete update by ${authorName}`}
                                      disabled={Boolean(deletingGroupPostId)}
                                      onClick={() => void handleDeleteGroupPost(post)}
                                    >
                                      <Trash2 size={14} aria-hidden="true" />
                                      {deletingGroupPostId === String(post.id) ? 'Deleting…' : 'Delete'}
                                    </button>
                                  )}
                                </span>
                              </header>
                              {content && <p>{content}</p>}
                              {mediaUrl && (isVideo
                                ? <video src={mediaUrl} controls preload="metadata" />
                                : <img src={mediaUrl} alt="Group update" loading="lazy" />)}
                            </article>
                          )
                        })}
                      </div>
                    ) : (
                      <p className="network-group-posts-empty">No group updates yet.</p>
                    )
                  ) : (
                    <div className="network-group-preview-gate">
                      {groupPostsPreview?.imageUrl ? (
                        <figure>
                          <img src={groupPostsPreview.imageUrl} alt="Recent group photo preview" />
                          {groupPostsPreview.createdAt && <figcaption>Recent group photo</figcaption>}
                        </figure>
                      ) : (
                        <p>No recent group photo is available to preview.</p>
                      )}
                      <div>
                        <strong>Follow this group to view more</strong>
                        <p>Followers can see all group updates, including text, photos, and videos.</p>
                        <button
                          className="network-group-follow-button"
                          type="button"
                          disabled={Boolean(updatingGroupFollowId)}
                          onClick={() => void toggleGroupFollow(selectedConnectionGroup)}
                        >
                          {updatingGroupFollowId === String(selectedConnectionGroup.id)
                            ? 'Following…'
                            : 'Follow group to view more'}
                        </button>
                      </div>
                    </div>
                  )}
                </section>
              </section>
            ) : (
            <section className="network-suggestions network-groups-section" aria-labelledby="network-groups-title">
              <header className="network-section-heading">
                <div>
                  <span className="network-section-eyebrow">YOUR NETWORK</span>
                  <h2 id="network-groups-title">Groups</h2>
                  <p>Organize your connections into groups.</p>
                </div>
                <button
                  className="network-group-create-button"
                  type="button"
                  onClick={() => {
                    setGroupError('')
                    setEditingGroupId('')
                    updateGroupPhotoSelection(null)
                    setIsGroupFormOpen((isOpen) => !isOpen)
                  }}
                >
                  {isGroupFormOpen ? 'Close' : 'Create group'}
                </button>
              </header>
              {groupError && <p className="network-connection-error" role="alert">{groupError}</p>}
              {isGroupFormOpen && (
                <form className="network-group-form" onSubmit={handleCreateGroup}>
                  <label htmlFor="network-group-name">Group name</label>
                  <input
                    id="network-group-name"
                    type="text"
                    value={groupNameDraft}
                    onChange={(event) => setGroupNameDraft(event.target.value)}
                    maxLength={80}
                    placeholder="Enter a group name"
                    disabled={isSavingGroup}
                    required
                  />
                  {renderGroupPhotoField('network-group-photo-create')}
                  <div className="network-group-form-actions">
                    <button
                      type="button"
                      className="network-group-cancel-button"
                      disabled={isSavingGroup}
                      onClick={() => {
                        setIsGroupFormOpen(false)
                        setGroupNameDraft('')
                        updateGroupPhotoSelection(null)
                        setGroupError('')
                      }}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="network-group-save-button"
                      disabled={isSavingGroup || Boolean(groupPhotoError)}
                    >
                      {isSavingGroup ? 'Creating…' : 'Create group'}
                    </button>
                  </div>
                </form>
              )}
              {loadingConnectionGroups ? (
                <p role="status">Loading groups…</p>
              ) : groupLoadError ? (
                <p className="network-connection-error" role="alert">{groupLoadError}</p>
              ) : connectionGroups.length ? (
                <div className="network-group-list">
                  {connectionGroups.map((group) => {
                    const groupId = String(group.id)
                    const isEditing = group.isOwner && editingGroupId === groupId
                    const memberIds = group.memberProfileIds || []
                    const creator = findProfileByIdentity(group.ownerId)
                    const creatorName = group.isOwner
                      ? 'you'
                      : getProfileName(creator) || 'another member'

                    return (
                      <article className="network-group-card" key={group.id}>
                        <div className="network-group-card-heading">
                          <button
                            className="network-group-title-button"
                            type="button"
                            aria-label={`Open ${group.name} group`}
                            onClick={() => {
                              setEditingGroupId('')
                              setSelectedGroupId(groupId)
                            }}
                          >
                            {renderGroupPhoto(group.imageUrl)}
                            <span className="network-group-title-details">
                              <strong>{group.name}</strong>
                              <span className="network-group-creator">Created by {creatorName}</span>
                              <span className="network-group-member-count">
                                {memberIds.length}{' '}
                                {memberIds.length === 1 ? 'member' : 'members'}
                                {' · '}{group.followerCount || 0}{' '}
                                {(group.followerCount || 0) === 1 ? 'follower' : 'followers'}
                              </span>
                            </span>
                          </button>
                          <div className="network-group-card-actions">
                            {group.isOwner ? (
                              <>
                                <div className="network-group-menu-wrapper">
                                  <button
                                    className="network-group-menu-trigger"
                                    type="button"
                                    aria-label={`More actions for ${group.name}`}
                                    aria-haspopup="menu"
                                    aria-expanded={openGroupMenuId === groupId}
                                    onClick={() => setOpenGroupMenuId((current) => (
                                      current === groupId ? '' : groupId
                                    ))}
                                  >
                                    <MoreVertical size={18} aria-hidden="true" />
                                  </button>
                                  {openGroupMenuId === groupId && (
                                    <div className="network-group-menu" role="menu">
                                      <button
                                        type="button"
                                        role="menuitem"
                                        onClick={() => startEditingGroup(group)}
                                      >
                                        Edit
                                      </button>
                                      <button
                                        type="button"
                                        role="menuitem"
                                        disabled={Boolean(deletingGroupId) || isSavingGroup}
                                        onClick={() => {
                                          setOpenGroupMenuId('')
                                          void handleDeleteGroup(group)
                                        }}
                                      >
                                        {deletingGroupId === groupId ? 'Deleting…' : 'Delete'}
                                      </button>
                                    </div>
                                  )}
                                </div>
                                <button
                                  className="network-group-message-button"
                                  type="button"
                                  disabled={openingGroupConversationId === groupId}
                                  onClick={() => void handleOpenGroupConversation(group)}
                                >
                                  <MessageCircle size={14} aria-hidden="true" />
                                  {openingGroupConversationId === groupId ? 'Opening…' : 'Message'}
                                </button>
                                {group.isOwner && (
                                  <button
                                    className="network-group-followers-button"
                                    type="button"
                                    onClick={() => setViewingGroupFollowersId(groupId)}
                                  >
                                    Followers ({group.followerCount || 0})
                                  </button>
                                )}
                              </>
                            ) : (
                              <>
                                <button
                                  className={`network-group-follow-button${group.isFollowing ? ' is-following' : ''}`}
                                  type="button"
                                  disabled={Boolean(updatingGroupFollowId)}
                                  aria-pressed={Boolean(group.isFollowing)}
                                  onClick={() => void toggleGroupFollow(group)}
                                >
                                  {updatingGroupFollowId === groupId
                                    ? 'Updating…'
                                    : group.isFollowing ? 'Unfollow' : 'Follow'}
                                </button>
                              </>
                            )}
                          </div>
                        </div>
                        {isEditing ? (
                          <form
                            className="network-group-form"
                            onSubmit={(event) => handleUpdateGroup(event, group)}
                          >
                            <label htmlFor={`network-group-name-${groupId}`}>Group name</label>
                            <input
                              id={`network-group-name-${groupId}`}
                              type="text"
                              value={groupNameDraft}
                              onChange={(event) => setGroupNameDraft(event.target.value)}
                              maxLength={80}
                              disabled={isSavingGroup}
                              required
                            />
                            {renderGroupPhotoField(`network-group-photo-${groupId}`)}
                            <div className="network-group-form-actions">
                              <button
                                type="button"
                                className="network-group-cancel-button"
                                disabled={isSavingGroup}
                                onClick={() => {
                                  setEditingGroupId('')
                                  setGroupNameDraft('')
                                  updateGroupPhotoSelection(null)
                                  setGroupError('')
                                }}
                              >
                                Cancel
                              </button>
                              <button
                                type="submit"
                                className="network-group-save-button"
                                disabled={isSavingGroup || Boolean(groupPhotoError)}
                              >
                                {isSavingGroup ? 'Saving…' : 'Save changes'}
                              </button>
                            </div>
                          </form>
                        ) : null}
                      </article>
                    )
                  })}
                </div>
              ) : (
                <div className="network-connections-empty">
                  <div className="network-catch-up-icon"><UsersRound size={24} aria-hidden="true" /></div>
                  <h3>No groups yet</h3>
                  <p>Create a group and choose which of your connections to include.</p>
                </div>
              )}
            </section>
            )
          ) : activeSection === 'banner' ? (
            <section>
            </section>
          ) : activeSection === 'connections' || activeSection === 'following' || activeSection === 'requests' ? (
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
                      ? `${displayedProfiles.length} profiles`
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
                  {displayedProfiles.map((profile) => renderProfileCard(
                    profile,
                    activeSection === 'requests',
                    activeSection === 'connections',
                  ))}
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
                    {invitations.map((inv) => {
                      const senderProfile = getInvitationProfile(inv)
                      return (
                      <div className="network-invitation-item" key={inv.id || inv.profileId}>
                        <div className="network-invitation-sender">
                          <button
                            type="button"
                            className="network-invitation-avatar"
                            aria-label={`View ${senderProfile.name}'s profile`}
                            onClick={() => onProfileSelect?.(senderProfile)}
                          >
                            {senderProfile.avatar || senderProfile.profileImageUrl ? (
                              <img src={senderProfile.avatar || senderProfile.profileImageUrl} alt="" />
                            ) : (
                              <span>{senderProfile.initials || 'TH'}</span>
                            )}
                          </button>
                          <div className="network-invitation-details">
                            <h3
                              role="button"
                              tabIndex={0}
                              onClick={() => onProfileSelect?.(senderProfile)}
                              onKeyDown={(e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                  onProfileSelect?.(senderProfile)
                                }
                              }}
                            >
                              {senderProfile.name}
                            </h3>
                            <p className="network-invitation-role">{senderProfile.role}</p>
                            {getProfileLocationSummary(senderProfile) && (
                              <p className="network-invitation-location">
                                {getProfileLocationSummary(senderProfile)}
                              </p>
                            )}
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
                        {inv.message?.trim() && (
                          <div className="network-invitation-note" aria-label="Connection note">
                            <p>{inv.message}</p>
                          </div>
                        )}
                      </div>
                      )
                    })}
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
              {catchUpUpdates.length > 0 ? (
                <div style={{ width: '100%', textAlign: 'left', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <h2>Network updates</h2>
                  {catchUpUpdates.map((item) => (
                    <article key={item.id} className="network-invitation-card" style={{ padding: '14px', borderRadius: '10px', border: '1px solid var(--border-color, #e2e8f0)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
                        <strong>{item.name}</strong>
                        <small>{item.type === 'OPPORTUNITY' ? 'Opportunity' : 'Update'}</small>
                      </div>
                      {item.headline && <p style={{ margin: '4px 0', opacity: 0.8 }}>{item.headline}</p>}
                      <p style={{ margin: '6px 0 0' }}>{item.title}{item.summary ? ` — ${item.summary.slice(0, 140)}` : ''}</p>
                    </article>
                  ))}
                </div>
              ) : (
                <>
                  <div className="network-catch-up-icon"><UsersRound size={24} aria-hidden="true" /></div>
                  <h2>You’re all caught up</h2>
                  <p>New updates from your network will show up here.</p>
                  <button type="button" onClick={() => setActiveTab('grow')}>Explore people</button>
                </>
              )}
            </section>
          )}
            </>
          )}
        </div>
      </div>
      {connectDialogProfile && (
        <div
          className="network-connect-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !updatingConnectionProfileId) {
              setConnectDialogProfile(null)
              setConnectionNote('')
            }
          }}
        >
          <section
            className="network-connect-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="network-connect-dialog-title"
          >
            <header>
              <div>
                <h2 id="network-connect-dialog-title">Connect with {connectDialogProfile.name}</h2>
                <p>Add a note to your connection request, or send it without one.</p>
              </div>
              <button
                type="button"
                aria-label="Close connection request"
                disabled={Boolean(updatingConnectionProfileId)}
                onClick={() => {
                  setConnectDialogProfile(null)
                  setConnectionNote('')
                }}
              >
                <X size={20} aria-hidden="true" />
              </button>
            </header>
            <label htmlFor="network-connection-note">Note</label>
            <textarea
              id="network-connection-note"
              value={connectionNote}
              onChange={(event) => setConnectionNote(event.target.value)}
              maxLength={500}
              rows={5}
              placeholder="Write a short note..."
              disabled={Boolean(updatingConnectionProfileId)}
            />
            <div className="network-connect-dialog-footer">
              <span>{connectionNote.length}/500</span>
              <div>
                <button
                  type="button"
                  className="network-connect-with-note"
                  disabled={!connectionNote.trim() || Boolean(updatingConnectionProfileId)}
                  onClick={() => sendConnectionRequest(true)}
                >
                  {updatingConnectionProfileId ? 'Sending…' : 'Send with note'}
                </button>
                <button
                  type="button"
                  className="network-connect-without-note"
                  disabled={Boolean(updatingConnectionProfileId)}
                  onClick={() => sendConnectionRequest(false)}
                >
                  {updatingConnectionProfileId ? 'Sending…' : 'Send without note'}
                </button>
              </div>
            </div>
            {connectionError && <p className="network-connection-error" role="alert">{connectionError}</p>}
          </section>
        </div>
      )}
      {viewingConnectionGroup && (
        <div
          className="network-group-followers-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) setViewingGroupFollowersId('')
          }}
        >
          <section
            className="network-group-followers-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="network-group-followers-title"
          >
            <header>
              <div>
                <h2 id="network-group-followers-title">{viewingConnectionGroup.name} profiles</h2>
                <p>
                  {getGroupFollowers(viewingConnectionGroup).length} profiles ·{' '}
                  {viewingConnectionGroup.followerCount || 0} following this group
                </p>
              </div>
              <button
                type="button"
                aria-label="Close followers"
                onClick={() => setViewingGroupFollowersId('')}
              >
                <X size={20} aria-hidden="true" />
              </button>
            </header>
            {getGroupFollowers(viewingConnectionGroup).length ? (
              <ul className="network-group-followers-list">
                {getGroupFollowers(viewingConnectionGroup).map((follower) => {
                  const name = getProfileName(follower)
                  const image = follower.profileImageUrl || follower.avatar || ''
                  return (
                    <li key={getProfileKey(follower)}>
                      <button
                        type="button"
                        disabled={follower.isGroupProfilePlaceholder}
                        onClick={() => {
                          setViewingGroupFollowersId('')
                          onProfileSelect?.(follower)
                        }}
                      >
                        {image
                          ? <img src={image} alt="" />
                          : <span aria-hidden="true">{name.slice(0, 1).toUpperCase()}</span>}
                        <span className="network-group-follower-copy">
                          <strong>{name}</strong>
                          <small>{follower.groupRelationship}</small>
                        </span>
                      </button>
                    </li>
                  )
                })}
              </ul>
            ) : (
              <p className="network-group-followers-empty">No followers yet.</p>
            )}
          </section>
        </div>
      )}
      {addingGroupMembers && (
        <div
          className="network-group-followers-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !isAddingGroupMembers) {
              setAddingGroupMembersId('')
              setSelectedGroupMemberIds([])
            }
          }}
        >
          <section
            className="network-group-followers-dialog network-group-add-members-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="network-group-add-members-title"
          >
            <header>
              <div>
                <h2 id="network-group-add-members-title">Add members to {addingGroupMembers.name}</h2>
                <p>Select your connections or followers to add to this group.</p>
              </div>
              <button
                type="button"
                aria-label="Close add members"
                disabled={isAddingGroupMembers}
                onClick={() => {
                  setAddingGroupMembersId('')
                  setSelectedGroupMemberIds([])
                }}
              >
                <X size={20} aria-hidden="true" />
              </button>
            </header>
            {groupMembersError && <p className="network-connection-error" role="alert">{groupMembersError}</p>}
            {addGroupMemberProfiles.length ? (
              <ul className="network-group-add-members-list">
                {addGroupMemberProfiles.map((profile) => {
                  const profileId = getProfileKey(profile)
                  const name = getProfileName(profile) || 'TrueHunt Member'
                  const image = profile.profileImageUrl || profile.avatar || ''
                  const identityKeys = getProfileIdentityKeys(profile)
                  const isConnection = identityKeys.some((key) => creatorConnectionKeys.has(key))
                  const isFollower = identityKeys.some((key) => creatorFollowerKeys.has(key))
                  return (
                    <li key={profileId}>
                      <label className="network-group-add-member-option">
                        <input
                          type="checkbox"
                          checked={selectedGroupMemberIds.includes(profileId)}
                          disabled={isAddingGroupMembers}
                          onChange={(event) => {
                            setSelectedGroupMemberIds((current) => (
                              event.target.checked
                                ? [...current, profileId]
                                : current.filter((id) => id !== profileId)
                            ))
                          }}
                        />
                        {image
                          ? <img src={image} alt="" />
                          : <span className="network-group-add-member-avatar" aria-hidden="true">
                            {name.slice(0, 1).toUpperCase()}
                          </span>}
                        <span className="network-group-add-member-copy">
                          <strong>{name}</strong>
                          <small>
                            {isConnection && isFollower
                              ? 'Connection · Your follower'
                              : isConnection ? 'Connection' : 'Your follower'}
                          </small>
                        </span>
                      </label>
                    </li>
                  )
                })}
              </ul>
            ) : (
              <p className="network-group-followers-empty">
                No new connections or followers are available to add.
              </p>
            )}
            <footer className="network-group-add-members-actions">
              <button
                type="button"
                className="network-group-cancel-button"
                disabled={isAddingGroupMembers}
                onClick={() => {
                  setAddingGroupMembersId('')
                  setSelectedGroupMemberIds([])
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                className="network-group-save-button"
                disabled={isAddingGroupMembers || selectedGroupMemberIds.length === 0}
                onClick={() => void handleAddGroupMembers()}
              >
                {isAddingGroupMembers ? 'Adding…' : `Add selected (${selectedGroupMemberIds.length})`}
              </button>
            </footer>
          </section>
        </div>
      )}
    </main>
  )
}

export default NetworkPage