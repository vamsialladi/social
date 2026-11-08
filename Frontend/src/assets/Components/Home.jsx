import { useEffect, useRef, useState } from 'react'
import { Eye, UsersRound, X } from 'lucide-react'
import './Home.css'
import Article from './Article.jsx'
import api from '../../api'

function ProfileAvatarPlaceholder() {
  return (
    <svg className="home-avatar-placeholder" viewBox="0 0 100 100" aria-hidden="true">
      <circle cx="50" cy="50" r="50" style={{ fill: '#e5e7e8' }} />
      <circle cx="50" cy="35" r="21" />
      <path d="M10 100c2-24 18-42 40-42s38 18 40 42H10Z" />
    </svg>
  )
}

function getDismissedPostsStorageKey(userId) {
  const accountId = String(userId || 'guest').trim().toLowerCase()
  return `truehunt-not-interested-posts:${encodeURIComponent(accountId)}`
}

function readDismissedPostIds(storageKey) {
  try {
    const savedIds = localStorage.getItem(storageKey)
    const parsedIds = savedIds ? JSON.parse(savedIds) : []
    return Array.isArray(parsedIds) ? parsedIds.map(String) : []
  } catch (error) {
    console.warn('Could not read dismissed feed posts:', error)
    return []
  }
}

function Home({
  profileDetails,
  profilePreview,
  currentUserId,
  onOwnPostCreated,
  activeNavigation,
  setActiveNavigation,
  hidden,
  onViewProfile,
  onViewConnection,
}) {
  const [posts, setPosts] = useState([])
  const [dismissedPostIds, setDismissedPostIds] = useState(() => (
    readDismissedPostIds(getDismissedPostsStorageKey(currentUserId))
  ))
  const [profileDirectory, setProfileDirectory] = useState([])
  const [connectedProfiles, setConnectedProfiles] = useState([])
  const [profileViewers, setProfileViewers] = useState([])
  const [connectionsError, setConnectionsError] = useState('')
  const [profileViewersError, setProfileViewersError] = useState('')
  const [isConnectionsOpen, setIsConnectionsOpen] = useState(false)
  const [isProfileViewersOpen, setIsProfileViewersOpen] = useState(false)
  const [isWritingArticle, setIsWritingArticle] = useState(false)
  const [draft, setDraft] = useState('')
  const [draftType, setDraftType] = useState('post')
  const [draftMedia, setDraftMedia] = useState(null)
  const [isComposerOpen, setIsComposerOpen] = useState(false)
  const imageInputRef = useRef(null)
  const videoInputRef = useRef(null)
  const mediaUrls = useRef([])

  const name = profileDetails.name.trim() || 'Your name'
  const dismissedPostsStorageKey = getDismissedPostsStorageKey(currentUserId)
  const languages = [
    ...profileDetails.languages.filter((language) => language !== 'Other'),
    ...(profileDetails.otherLanguage ? [profileDetails.otherLanguage] : []),
  ]

  useEffect(() => () => {
    mediaUrls.current.forEach((url) => URL.revokeObjectURL(url))
  }, [])

  useEffect(() => {
    setDismissedPostIds(readDismissedPostIds(dismissedPostsStorageKey))
  }, [dismissedPostsStorageKey])

  useEffect(() => {
    async function loadPosts() {
      try {
        const data = activeNavigation === 'opportunities'
          ? await api.getOpportunities()
          : await api.getPosts()
        if (Array.isArray(data)) {
          const dismissedIds = new Set(readDismissedPostIds(dismissedPostsStorageKey))
          setDismissedPostIds([...dismissedIds])
          setPosts(data.filter((post) => !dismissedIds.has(String(post.id))))
        }
      } catch (err) {
        console.warn('Failed to load posts from API:', err)
      }
    }
    loadPosts()
  }, [activeNavigation, dismissedPostsStorageKey])

  useEffect(() => {
    let isCurrent = true
    async function loadProfileDirectory() {
      try {
        const directory = await api.getProfileDirectory()
        if (isCurrent && Array.isArray(directory)) {
          setProfileDirectory(directory)
        }
      } catch (error) {
        console.warn('Could not load profile photos for feed posts:', error)
      }
    }

    loadProfileDirectory()
    return () => {
      isCurrent = false
    }
  }, [])

  function getPostAuthorAvatar(post) {
    if (post.authorAvatar) return post.authorAvatar

    const authorId = String(post.authorId || '').trim().toLowerCase()
    const authorName = String(post.authorName || '').trim().toLowerCase()
    const authorProfile = profileDirectory.find((profile) => (
      authorId
      && String(profile.userId || profile.id || profile.referId || '').trim().toLowerCase() === authorId
    )) || profileDirectory.find((profile) => (
      authorName && String(profile.name || '').trim().toLowerCase() === authorName
    ))

    if (authorProfile) {
      return authorProfile.profileImageUrl || authorProfile.avatar || ''
    }

    const ownProfileIds = [currentUserId, profileDetails.userId, profileDetails.id]
      .filter(Boolean)
      .map((id) => String(id).trim().toLowerCase())
    return authorId && ownProfileIds.includes(authorId) ? profilePreview : ''
  }

  useEffect(() => {
    if (activeNavigation !== 'home') return undefined

    let isCurrent = true
    async function loadConnections() {
      setConnectionsError('')
      try {
        const data = await api.getConnectedProfiles()
        if (isCurrent) setConnectedProfiles(Array.isArray(data) ? data : [])
      } catch (error) {
        console.error('Failed to load connections:', error)
        if (isCurrent) setConnectionsError('Could not load your connections. Please try again.')
      }
    }

    loadConnections()
    return () => {
      isCurrent = false
    }
  }, [activeNavigation])

  useEffect(() => {
    if (activeNavigation !== 'home') return undefined

    let isCurrent = true
    async function loadProfileViewers() {
      setProfileViewersError('')
      try {
        const data = await api.getProfileViewers()
        if (!Array.isArray(data)) {
          throw new Error('Profile viewers returned an invalid response.')
        }
        if (isCurrent) setProfileViewers(data)
      } catch (error) {
        console.error('Failed to load profile viewers:', error)
        if (isCurrent) setProfileViewersError('Could not load profile viewers. Please try again.')
      }
    }

    loadProfileViewers()
    return () => {
      isCurrent = false
    }
  }, [activeNavigation, currentUserId])

  useEffect(() => {
    function handleConnectionChanged(event) {
      const { connected, profile } = event.detail || {}
      const profileId = String(
        profile?.userId || profile?.id || profile?.user_id || profile?._id || '',
      )
      if (!profileId) return

      setConnectedProfiles((current) => {
        const matchesProfile = (item) => String(
          item.userId || item.id || item.user_id || item._id || '',
        ) === profileId
        if (!connected) return current.filter((item) => !matchesProfile(item))
        if (current.some(matchesProfile)) return current
        return [...current, { ...profile, id: profileId, userId: profile.userId || profileId }]
      })
    }

    window.addEventListener('truehunt-connection-changed', handleConnectionChanged)
    return () => window.removeEventListener('truehunt-connection-changed', handleConnectionChanged)
  }, [])

  function selectMedia(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    if (draftMedia) URL.revokeObjectURL(draftMedia.url)
    setDraftMedia({
      file,
      url: URL.createObjectURL(file),
      kind: file.type.startsWith('video/') ? 'video' : 'image',
    })
    setIsComposerOpen(true)
  }

  function removeDraftMedia() {
    if (draftMedia) URL.revokeObjectURL(draftMedia.url)
    setDraftMedia(null)
  }

  async function publishPost(event) {
    event.preventDefault()
    const content = draft.trim()
    if (!content && !draftMedia) return

    let uploadedMedia = null
    if (draftMedia?.file) {
      try {
        const res = await api.uploadMedia(draftMedia.file)
        uploadedMedia = {
          url: res.url,
          fileName: res.fileName,
          contentType: res.contentType,
          kind: res.kind,
        }
      } catch (err) {
        console.warn('Failed to upload post media:', err)
      }
    }

    try {
      const response = await api.createPost({
        content,
        type: draftType,
        media: uploadedMedia,
      })
      const created = {
        ...response,
        authorId: response.authorId || currentUserId,
        authorAvatar: response.authorAvatar || profilePreview,
      }
      setPosts((currentPosts) => [created, ...currentPosts])
      onOwnPostCreated?.(created)
    } catch (err) {
      console.warn('Backend post creation note:', err)
      if (draftMedia) mediaUrls.current.push(draftMedia.url)
      const created = {
        id: `${Date.now()}`,
        authorId: currentUserId,
        authorAvatar: profilePreview,
        authorName: name,
        localOnly: true,
        content,
        type: draftType,
        media: draftMedia,
        createdAt: new Date().toLocaleString(),
      }
      setPosts((currentPosts) => [created, ...currentPosts])
      onOwnPostCreated?.(created)
    }

    setDraft('')
    setDraftMedia(null)
    setDraftType('post')
    setIsComposerOpen(false)
  }

  function startOpportunityPost() {
    setDraftType('opportunity')
    setIsComposerOpen(true)
    setActiveNavigation('opportunities')
  }

  function startPost() {
    setDraftType(activeNavigation === 'opportunities' ? 'opportunity' : 'post')
    setIsComposerOpen(true)
  }

  async function publishArticle(article) {
    let uploadedMedia = null
    if (article.media?.file) {
      try {
        const res = await api.uploadMedia(article.media.file)
        uploadedMedia = {
          url: res.url,
          fileName: res.fileName,
          contentType: res.contentType,
          kind: res.kind,
        }
      } catch (err) {
        console.warn('Failed to upload article media:', err)
      }
    }

    try {
      const response = await api.createPost({
        title: article.title,
        content: article.content,
        type: 'article',
        media: uploadedMedia,
      })
      const created = {
        ...response,
        authorId: response.authorId || currentUserId,
        authorAvatar: response.authorAvatar || profilePreview,
      }
      setPosts((currentPosts) => [created, ...currentPosts])
      onOwnPostCreated?.(created)
    } catch (err) {
      console.warn('Backend article creation note:', err)
      const created = {
        id: `${Date.now()}`,
        ...article,
        authorId: currentUserId,
        authorAvatar: profilePreview,
        authorName: name,
        localOnly: true,
        type: 'article',
        createdAt: new Date().toLocaleString(),
      }
      setPosts((currentPosts) => [created, ...currentPosts])
      onOwnPostCreated?.(created)
    }
    setIsWritingArticle(false)
  }

  const [openCommentsPostId, setOpenCommentsPostId] = useState(null)
  const [commentText, setCommentText] = useState('')

  async function handleLike(postId) {
    try {
      const updated = await api.likePost(postId)
      setPosts((current) => current.map((p) => (String(p.id) === String(postId) ? { ...p, ...updated } : p)))
    } catch (err) {
      console.warn('Like error:', err)
    }
  }

  async function handleApply(postId) {
    try {
      const updated = await api.applyToOpportunity(postId)
      setPosts((current) => current.map((p) => (String(p.id) === String(postId) ? { ...p, ...updated } : p)))
    } catch (err) {
      console.warn('Apply error:', err)
    }
  }

  async function handleDelete(postId) {
    try {
      const postToDelete = posts.find((post) => String(post.id) === String(postId))
      if (!postToDelete?.localOnly) {
        await api.deletePost(postId)
      }

      setPosts((current) => current.filter((p) => String(p.id) !== String(postId)))

      const ownerId = String(currentUserId || 'me').trim()
      const profilePostsStorageKey = `truehunt-social-posts:${encodeURIComponent(ownerId)}`
      try {
        const savedPosts = localStorage.getItem(profilePostsStorageKey)
        const parsedPosts = savedPosts ? JSON.parse(savedPosts) : []
        if (!Array.isArray(parsedPosts)) {
          throw new Error('Saved profile posts are not a list.')
        }
        localStorage.setItem(
          profilePostsStorageKey,
          JSON.stringify(parsedPosts.filter((post) => String(post.id) !== String(postId))),
        )
      } catch (error) {
        console.warn('Could not update saved profile posts after feed deletion:', error)
      }

      window.dispatchEvent(new CustomEvent('truehunt-social-post-deleted', {
        detail: { postId: String(postId), profileId: ownerId },
      }))
    } catch (err) {
      console.warn('Delete error:', err)
    }
  }

  function handleNotInterested(postId) {
    const postIdString = String(postId)
    const dismissedIds = new Set(readDismissedPostIds(dismissedPostsStorageKey))
    dismissedIds.add(postIdString)
    try {
      localStorage.setItem(dismissedPostsStorageKey, JSON.stringify([...dismissedIds]))
    } catch (error) {
      console.warn('Could not save dismissed feed post:', error)
    }
    setDismissedPostIds([...dismissedIds])
    setPosts((current) => current.filter((post) => String(post.id) !== postIdString))
  }

  function isOwnPost(post) {
    const authorId = String(post.authorId || '').trim().toLowerCase()
    const ownIds = [currentUserId, profileDetails.userId, profileDetails.id, profileDetails.email]
      .filter(Boolean)
      .map((id) => String(id).trim().toLowerCase())
    if (authorId && authorId !== 'me' && ownIds.includes(authorId)) return true

    const authorName = String(post.authorName || '').trim().toLowerCase()
    const ownName = String(profileDetails.name || '').trim().toLowerCase()
    return Boolean(authorName && ownName && authorName === ownName)
  }

  function viewPostAuthor(post) {
    if (isOwnPost(post)) {
      onViewProfile?.()
      return
    }

    const authorId = String(post.authorId || '').trim()
    const authorName = String(post.authorName || '').trim()
    const authorProfile = profileDirectory.find((profile) => (
      authorId && authorId.toLowerCase() !== 'me'
      && String(profile.userId || profile.id || profile.referId || '').trim().toLowerCase()
        === authorId.toLowerCase()
    )) || profileDirectory.find((profile) => (
      authorName && String(profile.name || '').trim().toLowerCase() === authorName.toLowerCase()
    ))
    const profileId = authorProfile?.userId
      || authorProfile?.id
      || authorProfile?.referId
      || (authorId.toLowerCase() === 'me' ? '' : authorId)
    const authorAvatar = authorProfile?.profileImageUrl
      || authorProfile?.avatar
      || post.authorAvatar
      || ''

    onViewConnection?.({
      ...authorProfile,
      id: authorProfile?.id || profileId,
      userId: profileId,
      name: authorProfile?.name || authorName || 'TrueHunt member',
      profileImageUrl: authorAvatar,
      avatar: authorAvatar,
    })
  }

  async function handleCommentSubmit(event, postId) {
    event.preventDefault()
    if (!commentText.trim()) return
    try {
      const updated = await api.commentOnPost(postId, commentText.trim())
      setPosts((current) => current.map((p) => (String(p.id) === String(postId) ? { ...p, ...updated } : p)))
      setCommentText('')
    } catch (err) {
      console.warn('Comment error:', err)
    }
  }

  async function handlePromoClick(campaign) {
    try {
      const res = await api.clickPromotion(campaign)
      alert(res.message || 'Inquiry registered with Northstar Films casting office.')
    } catch (err) {
      console.warn('Promo error:', err)
    }
  }

  const visiblePosts = posts.filter((post) => (
    !dismissedPostIds.includes(String(post.id))
    && (activeNavigation === 'opportunities'
      ? post.type === 'opportunity'
      : post.type !== 'opportunity')
  ))
  return (
    <>
      <section
        className="home-page"
        hidden={hidden || isWritingArticle}
        aria-label={activeNavigation === 'opportunities' ? 'Opportunities' : 'Home feed'}
      >
      <div className="home-layout">
        <aside className="home-sidebar" aria-label="Your profile summary">
          <section
            className="home-card home-profile-card"
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
            <div className="home-profile-cover" />
            <div className="home-profile-content">
              <div className={`home-avatar home-profile-avatar${profilePreview ? '' : ' is-placeholder'}`}>
                {profilePreview
                  ? <img src={profilePreview} alt={`${name}'s profile`} />
                  : <ProfileAvatarPlaceholder />}
              </div>
              <h1>{name}</h1>
              {profileDetails.talents.length > 0 && (
                <p className="home-profile-headline">{profileDetails.talents.join(' · ')}</p>
              )}
              {profileDetails.address && <p>{profileDetails.address}</p>}
              {profileDetails.experience && (
                <p>{profileDetails.experience.toLowerCase().includes('year') ? `${profileDetails.experience} experience` : `${profileDetails.experience} years of experience`}</p>
              )}
              {profileDetails.workTypes.length > 0 && (
                <div className="home-profile-tags">
                  {profileDetails.workTypes.map((workType) => (
                    <span key={workType}>
                      {workType.startsWith('Other: ')
                        ? `Others: ${workType.slice('Other: '.length)}`
                        : workType}
                    </span>
                  ))}
                </div>
              )}
              {languages.length > 0 && (
                <div className="home-profile-tags">
                  {languages.map((language) => (
                    <span key={language}>{language}</span>
                  ))}
                </div>
              )}
            </div>
          </section>
          <div className="home-card home-sidebar-stats">
            <button
              className="home-connections-card"
              type="button"
              onClick={() => {
                setIsProfileViewersOpen(false)
                setIsConnectionsOpen(true)
              }}
              aria-haspopup="dialog"
            >
              <span className="home-connections-icon"><UsersRound size={20} aria-hidden="true" /></span>
              <span className="home-connections-copy">
                <strong>Connections</strong>
                <small>View your professional network</small>
              </span>
              <span className="home-connections-count">{connectedProfiles.length}</span>
            </button>
            <button
              className="home-profile-viewers-card"
              type="button"
              onClick={() => {
                setIsConnectionsOpen(false)
                setIsProfileViewersOpen(true)
                api.getProfileViewers().then((data) => {
                  if (!Array.isArray(data)) {
                    throw new Error('Profile viewers returned an invalid response.')
                  }
                  setProfileViewers(data)
                  setProfileViewersError('')
                }).catch((error) => {
                  console.error('Failed to refresh profile viewers:', error)
                  setProfileViewersError('Could not load profile viewers. Please try again.')
                })
              }}
              aria-haspopup="dialog"
            >
              <span className="home-connections-icon"><Eye size={20} aria-hidden="true" /></span>
              <span className="home-connections-copy">
                <strong>Profile viewers</strong>
                <small>Profile visibility insights</small>
              </span>
              <span className="home-connections-count">{profileViewers.length}</span>
            </button>
          </div>
          {connectionsError && <p className="home-connections-error" role="alert">{connectionsError}</p>}
          {profileViewersError && <p className="home-connections-error" role="alert">{profileViewersError}</p>}
        </aside>

        <div className="home-feed-column">
          {activeNavigation === 'home' && (
          <section className="home-card home-composer" aria-label="Create a post">
            <div className="home-composer-top">
              <div className={`home-avatar${profilePreview ? '' : ' is-placeholder'}`} aria-hidden="true">
                {profilePreview
                  ? <img src={profilePreview} alt="" />
                  : <ProfileAvatarPlaceholder />}
              </div>
              <button
                className="home-start-post"
                type="button"
                onClick={startPost}
              >
                Start a post
              </button>
            </div>

            {isComposerOpen && (
              <form className="home-post-form" onSubmit={publishPost}>
                <label className="home-sr-only" htmlFor="home-post-content">
                  Write your post
                </label>
                <textarea
                  id="home-post-content"
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  placeholder={draftType === 'opportunity'
                    ? 'Describe the job or opportunity you are sharing'
                    : 'What would you like to share?'}
                  rows={4}
                  autoFocus
                />
                {draftMedia && (
                  <div className="home-media-preview">
                    {draftMedia.kind === 'video'
                      ? <video src={draftMedia.url} controls />
                      : <img src={draftMedia.url} alt="Selected post attachment" />}
                    <button type="button" onClick={removeDraftMedia}>Remove attachment</button>
                  </div>
                )}
                <div className="home-publish-row">
                  <span>{draftType === 'opportunity' ? 'Opportunity' : 'Post'}</span>
                  <button
                    className="home-publish-button"
                    type="submit"
                    disabled={!draft.trim() && !draftMedia}
                  >
                    {draftType === 'opportunity' ? 'Share' : 'Post'}
                  </button>
                </div>
              </form>
            )}

            <div className="home-composer-actions">
              <button type="button" onClick={() => videoInputRef.current?.click()}>
                <svg className="home-action-icon home-video-icon" viewBox="0 0 20 20" aria-hidden="true">
                  <rect x="1" y="3" width="18" height="14" rx="3" fill="currentColor" />
                  <path d="m8 6.5 5 3.5-5 3.5z" fill="#fff" />
                </svg>
                Video
              </button>
              <button type="button" onClick={() => imageInputRef.current?.click()}>
                <svg className="home-action-icon home-photo-icon" viewBox="0 0 20 20" aria-hidden="true">
                  <rect x="1.5" y="2.5" width="17" height="15" rx="2" fill="none" stroke="currentColor" strokeWidth="2" />
                  <circle cx="6.5" cy="7" r="1.5" fill="currentColor" />
                  <path d="m3.5 15 4.2-4.2 2.7 2.5 2.2-2 4 3.7z" fill="currentColor" />
                </svg>
                Photo
              </button>
              <button
                type="button"
                onClick={() => {
                  setIsWritingArticle(true)
                }}
              >
                <svg className="home-action-icon home-article-icon" viewBox="0 0 20 20" aria-hidden="true">
                  <path d="M1 3h18v2H1zm0 4h18v2H1zm0 4h18v2H1zm0 4h12v2H1z" fill="currentColor" />
                </svg>
                Write article
              </button>
              <input
                ref={videoInputRef}
                className="home-sr-only"
                type="file"
                accept="video/*"
                aria-label="Choose a video"
                onChange={selectMedia}
              />
              <input
                ref={imageInputRef}
                className="home-sr-only"
                type="file"
                accept="image/*"
                aria-label="Choose a photo"
                onChange={selectMedia}
              />
            </div>
          </section>
          )}

          <div className="home-feed-heading">
            <span>{activeNavigation === 'opportunities' ? 'Opportunities' : 'Posts'}</span>
            {activeNavigation === 'opportunities' ? (
              <button className="home-opportunity-button" type="button" onClick={startOpportunityPost}>
                Share an opportunity
              </button>
            ) : (
              <span className="home-feed-sort">Recent</span>
            )}
          </div>

          {visiblePosts.length === 0 ? (
            <section className="home-card home-empty-feed">
              <h2>{activeNavigation === 'opportunities'
                ? 'No opportunities shared yet'
                : 'Your feed starts here'}</h2>
              <p>{activeNavigation === 'opportunities'
                ? 'When someone shares a job or opportunity, it will appear here.'
                : 'Share an update, photo, or video to create your first post.'}</p>
            </section>
          ) : (
            <div className="home-post-list">
              {visiblePosts.map((post) => {
                const authorAvatar = getPostAuthorAvatar(post)
                const authorName = post.authorName || name
                return (
                <article className="home-card home-post" key={post.id}>
                  <header className="home-post-header">
                    <button
                      className="home-post-author-avatar"
                      type="button"
                      aria-label={`View ${authorName}'s profile`}
                      onClick={() => viewPostAuthor(post)}
                    >
                      <span className={`home-avatar${authorAvatar ? '' : ' is-placeholder'}`} aria-hidden="true">
                        {authorAvatar
                          ? <img src={authorAvatar} alt="" />
                          : <ProfileAvatarPlaceholder />}
                      </span>
                    </button>
                    <div>
                      <button
                        className="home-post-author-name"
                        type="button"
                        onClick={() => viewPostAuthor(post)}
                      >
                        <strong>{authorName}</strong>
                      </button>
                      {post.authorHeadline && (
                        <span>{post.authorHeadline}</span>
                      )}
                      <time>{post.createdAt}</time>
                    </div>
                  </header>
                  {post.title && <h2 className="home-post-title">{post.title}</h2>}
                  {post.content && <p className="home-post-content">{post.content}</p>}
                  {post.media?.kind === 'video'
                    ? <video className="home-post-media" src={post.media.url} controls />
                    : post.media && (
                      <img className="home-post-media" src={post.media.url} alt="Post attachment" />
                    )}
                  {post.type === 'article' && <span className="home-post-type">Article</span>}
                  {post.type === 'opportunity' && <span className="home-post-type">Opportunity</span>}

                  {/* Post interactive action buttons wired to backend APIs */}
                  <div className="home-post-action-bar" style={{ display: 'flex', gap: '8px', marginTop: '12px', borderTop: '1px solid var(--color-border, #eee)', paddingTop: '8px', flexWrap: 'wrap' }}>
                    <button
                      type="button"
                      onClick={() => handleLike(post.id)}
                      style={{
                        background: post.liked ? '#e8f0fe' : 'transparent',
                        color: post.liked ? '#1a73e8' : 'inherit',
                        border: '1px solid #ddd',
                        padding: '6px 14px',
                        borderRadius: '20px',
                        cursor: 'pointer',
                        fontWeight: '500',
                        fontSize: '13px'
                      }}
                    >
                      👍 {post.likesCount || 0} {post.liked ? 'Liked' : 'Like'}
                    </button>

                    <button
                      type="button"
                      onClick={() => setOpenCommentsPostId(openCommentsPostId === post.id ? null : post.id)}
                      style={{
                        background: 'transparent',
                        border: '1px solid #ddd',
                        padding: '6px 14px',
                        borderRadius: '20px',
                        cursor: 'pointer',
                        fontSize: '13px'
                      }}
                    >
                      💬 Comments ({post.comments?.length || 0})
                    </button>

                    {post.type === 'opportunity' && (
                      <button
                        type="button"
                        onClick={() => handleApply(post.id)}
                        style={{
                          background: post.applied ? '#e6f4ea' : '#1a73e8',
                          color: post.applied ? '#137333' : '#fff',
                          border: 'none',
                          padding: '6px 16px',
                          borderRadius: '20px',
                          cursor: 'pointer',
                          fontWeight: '600',
                          fontSize: '13px'
                        }}
                      >
                        {post.applied ? '✓ Applied' : '⚡ Apply Now'}
                      </button>
                    )}

                    {isOwnPost(post) ? (
                      <button
                        type="button"
                        onClick={() => handleDelete(post.id)}
                        style={{
                          marginLeft: 'auto',
                          background: 'transparent',
                          border: 'none',
                          color: '#d93025',
                          cursor: 'pointer',
                          fontSize: '12px'
                        }}
                        title="Delete post"
                      >
                        🗑️ Delete
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleNotInterested(post.id)}
                        style={{
                          marginLeft: 'auto',
                          background: 'transparent',
                          border: 'none',
                          color: 'var(--color-text-muted, #666)',
                          cursor: 'pointer',
                          fontSize: '12px'
                        }}
                        title="Hide this post from your feed"
                      >
                        Not interested
                      </button>
                    )}
                  </div>

                  {/* Comments section */}
                  {openCommentsPostId === post.id && (
                    <div style={{ marginTop: '12px', background: '#f9f9f9', padding: '12px', borderRadius: '8px' }}>
                      {post.comments && post.comments.length > 0 && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '12px' }}>
                          {post.comments.map((c) => (
                            <div key={c.id} style={{ fontSize: '13px', background: '#fff', padding: '8px 12px', borderRadius: '6px' }}>
                              <strong>{c.authorName}</strong> <small style={{ color: '#888' }}>({c.createdAt})</small>: {c.text}
                            </div>
                          ))}
                        </div>
                      )}
                      <form onSubmit={(e) => handleCommentSubmit(e, post.id)} style={{ display: 'flex', gap: '8px' }}>
                        <input
                          type="text"
                          value={commentText}
                          onChange={(e) => setCommentText(e.target.value)}
                          placeholder="Write a comment..."
                          style={{ flex: 1, padding: '8px 12px', borderRadius: '20px', border: '1px solid #ccc', fontSize: '13px' }}
                        />
                        <button
                          type="submit"
                          disabled={!commentText.trim()}
                          style={{ background: '#1a73e8', color: '#fff', border: 'none', padding: '8px 16px', borderRadius: '20px', cursor: 'pointer', fontSize: '13px' }}
                        >
                          Post
                        </button>
                      </form>
                    </div>
                  )}
                </article>
                )
              })}
            </div>
          )}
        </div>

        <aside className="home-news-column" aria-label="TrueHunt News">
          <section className="home-card home-news-card">
            <h2>TrueHunt News</h2>
          </section>
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
                onClick={() => handlePromoClick("Northstar Films Production")}
              >
                Explore film careers
              </button>
            </div>
          </section>
        </aside>
      </div>
    </section>
    {isConnectionsOpen && (
      <div
        className="home-connections-backdrop"
        onMouseDown={(event) => {
          if (event.target === event.currentTarget) setIsConnectionsOpen(false)
        }}
      >
        <section
          className="home-connections-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="home-connections-title"
        >
          <header className="home-connections-dialog-header">
            <div>
              <h2 id="home-connections-title">Your connections</h2>
              <p>{connectedProfiles.length} people in your network</p>
            </div>
            <button
              type="button"
              className="home-connections-close"
              aria-label="Close connections"
              onClick={() => setIsConnectionsOpen(false)}
            >
              <X size={20} aria-hidden="true" />
            </button>
          </header>
          {connectedProfiles.length ? (
            <ul className="home-connections-list">
              {connectedProfiles.map((profile) => {
                const profileName = profile.name || 'TrueHunt member'
                const profileImage = profile.profileImageUrl || profile.avatar || ''
                const initials = profile.initials || profileName
                  .split(/\s+/)
                  .map((part) => part[0])
                  .join('')
                  .slice(0, 2)

                return (
                  <li key={profile.id || profile.userId || profileName}>
                    <button
                      type="button"
                      className="home-connection-person"
                      onClick={() => {
                        setIsConnectionsOpen(false)
                        onViewConnection?.({
                          ...profile,
                          userId: profile.userId || profile.id || '',
                          profileImageUrl: profileImage,
                        })
                      }}
                    >
                      <span className={`home-avatar home-connection-avatar${profileImage ? '' : ' is-placeholder'}`}>
                        {profileImage ? <img src={profileImage} alt="" /> : initials}
                      </span>
                      <span className="home-connection-person-name">{profileName}</span>
                    </button>
                  </li>
                )
              })}
            </ul>
          ) : (
            <div className="home-connections-empty">
              <UsersRound size={24} aria-hidden="true" />
              <p>No connections yet.</p>
            </div>
          )}
        </section>
      </div>
    )}
    {isProfileViewersOpen && (
      <div
        className="home-connections-backdrop"
        onMouseDown={(event) => {
          if (event.target === event.currentTarget) setIsProfileViewersOpen(false)
        }}
      >
        <section
          className="home-connections-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="home-profile-viewers-title"
        >
          <header className="home-connections-dialog-header">
            <div>
              <h2 id="home-profile-viewers-title">Profile viewers</h2>
              <p>{profileViewers.length} people viewed your profile</p>
            </div>
            <button
              type="button"
              className="home-connections-close"
              aria-label="Close profile viewers"
              onClick={() => setIsProfileViewersOpen(false)}
            >
              <X size={20} aria-hidden="true" />
            </button>
          </header>
          {profileViewersError ? (
            <p className="home-profile-viewers-message" role="alert">{profileViewersError}</p>
          ) : profileViewers.length ? (
            <ul className="home-connections-list">
              {profileViewers.map((profile) => {
                const profileName = profile.name || 'TrueHunt member'
                const profileImage = profile.profileImageUrl || profile.avatar || ''
                const initials = profileName
                  .split(/\s+/)
                  .map((part) => part[0])
                  .join('')
                  .slice(0, 2)

                return (
                  <li key={profile.userId || profile.id || profileName}>
                    <button
                      type="button"
                      className="home-connection-person"
                      onClick={() => {
                        setIsProfileViewersOpen(false)
                        onViewConnection?.({
                          ...profile,
                          userId: profile.userId || profile.id || '',
                          profileImageUrl: profileImage,
                        })
                      }}
                    >
                      <span className={`home-avatar home-connection-avatar${profileImage ? '' : ' is-placeholder'}`}>
                        {profileImage ? <img src={profileImage} alt="" /> : initials}
                      </span>
                      <span className="home-connection-person-name">{profileName}</span>
                    </button>
                  </li>
                )
              })}
            </ul>
          ) : (
            <div className="home-connections-empty">
              <Eye size={24} aria-hidden="true" />
              <p>No profile viewers yet.</p>
            </div>
          )}
        </section>
      </div>
    )}
    <Article
      profileDetails={profileDetails}
      profilePreview={profilePreview}
      hidden={hidden || !isWritingArticle}
      onBack={() => setIsWritingArticle(false)}
      onPublish={publishArticle}
    />
    </>
  )
}

export default Home