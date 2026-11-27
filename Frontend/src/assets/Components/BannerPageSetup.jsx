import { useCallback, useEffect, useRef, useState } from 'react'
import { ArrowLeft, ChevronDown, ChevronRight, FileText, Image, MessageCircle, MoreVertical, Pencil, Plus, Search, Shield, ThumbsUp, Trash2, Upload, UsersRound, Video, X } from 'lucide-react'
import { api } from '../../api.js'
import MediaSafetyStatus from '../../components/common/MediaSafetyStatus.jsx'
import { useMediaSafety } from '../../hooks/useMediaSafety.js'
import './BannerPageSetup.css'

const emptyPage = {
  name: '',
  publicUrl: '',
  website: '',
  industry: '',
  tagline: '',
  logo: '',
  banner: '',
  isCreated: false,
}

function readPageDraft(storageKey) {
  if (!storageKey) return emptyPage

  try {
    const savedPage = window.localStorage.getItem(storageKey)
    if (!savedPage) return emptyPage
    const parsedPage = JSON.parse(savedPage)
    return {
      ...emptyPage,
      name: parsedPage.name || '',
      publicUrl: parsedPage.publicUrl || '',
      website: parsedPage.website || '',
      industry: parsedPage.industry || '',
      tagline: parsedPage.tagline || '',
      logo: parsedPage.logo || '',
      banner: parsedPage.banner || '',
      isCreated: parsedPage.isCreated === true,
    }
  } catch (error) {
    console.warn('Could not read the organization page draft:', error)
    return emptyPage
  }
}

function mapOrganizationPage(page) {
  return {
    ...emptyPage,
    id: page.id,
    ownerId: page.ownerId,
    ownerName: page.ownerName || '',
    name: page.name || '',
    publicUrl: page.publicUrl || '',
    website: page.website || '',
    industry: page.industry || '',
    tagline: page.tagline || '',
    logo: page.imageUrl || '',
    banner: page.bannerUrl || '',
    isCreated: true,
    isOwner: Boolean(page.isOwner),
    isFollowing: Boolean(page.isFollowing),
    isSubscribed: Boolean(page.isSubscribed),
    isSubscriptionPending: Boolean(page.isSubscriptionPending),
    followerCount: page.followerCount || 0,
    subscriberCount: page.subscriberCount || 0,
    subscriberUserIds: Array.isArray(page.subscriberUserIds) ? page.subscriberUserIds : [],
    subscriberNames: Array.isArray(page.subscriberNames) ? page.subscriberNames : [],
  }
}

function getSubscriberNamesPreview(page) {
  const names = page.subscriberNames || []
  if (!names.length) return ''
  const preview = names.slice(0, 3).join(', ')
  return names.length > 3 ? `${preview} +${names.length - 3} more` : preview
}

function mapOrganizationPost(post) {
  const mediaType = String(post.mediaType || '')
  const createdAt = String(post.createdAt || '')
  const parsedDate = new Date(createdAt)
  return {
    id: String(post.id),
    title: '',
    text: post.content || '',
    media: post.mediaUrl
      ? { type: mediaType.startsWith('video/') ? 'video' : 'photo', source: post.mediaUrl }
      : null,
    createdAt: Number.isNaN(parsedDate.getTime()) ? createdAt : parsedDate.toLocaleString(),
    createdAtIso: Number.isNaN(parsedDate.getTime()) ? '' : parsedDate.toISOString(),
    reactions: Array.isArray(post.reactions) ? post.reactions : [],
    comments: Array.isArray(post.comments)
      ? post.comments.map((comment) => ({ ...comment, text: comment.text || comment.content || '' }))
      : [],
    impressions: post.impressions ?? post.impressionCount ?? 0,
    reposts: post.reposts ?? post.repostCount ?? 0,
    viewerReacted: Boolean(post.viewerReacted),
    viewerReposted: Boolean(post.viewerReposted),
  }
}

function getLocalDateKey(value) {
  const date = value instanceof Date ? value : new Date(value || '')
  if (Number.isNaN(date.getTime())) return ''
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function getDailyPostMetric(post, metric, dayKey) {
  const source = post[metric]
  const events = Array.isArray(source)
    ? source
    : Array.isArray(post[`${metric}Events`])
      ? post[`${metric}Events`]
      : null

  if (events) {
    return events.filter((event) => getLocalDateKey(event?.createdAt || event?.date) === dayKey).length
  }

  const count = Number(post[`${metric}Count`] ?? source ?? 0)
  return getLocalDateKey(post.createdAtIso || post.createdAt) === dayKey && Number.isFinite(count)
    ? count
    : 0
}

function PublicOrganizationPageView({ page, onBack, onPageUpdate }) {
  const [posts, setPosts] = useState([])
  const [commentDrafts, setCommentDrafts] = useState({})
  const [activePostId, setActivePostId] = useState('')
  const [isLoadingPosts, setIsLoadingPosts] = useState(true)
  const [isUpdating, setIsUpdating] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let isCurrent = true
    api.getConnectionGroupPosts(page.id)
      .then((feed) => {
        const loadedPosts = (feed?.posts || []).map(mapOrganizationPost)
        if (isCurrent) setPosts(loadedPosts)
        loadedPosts.forEach((post) => {
          void api.recordConnectionGroupPostActivity(page.id, post.id, { type: 'IMPRESSION' })
            .catch((activityError) => console.error(
              'Could not record organization page post impression:',
              activityError,
            ))
        })
      })
      .catch((loadError) => {
        console.error('Could not load public organization page posts:', loadError)
        if (isCurrent) setError(loadError.message || 'Could not load page posts. Please try again.')
      })
      .finally(() => {
        if (isCurrent) setIsLoadingPosts(false)
      })

    return () => {
      isCurrent = false
    }
  }, [page.id])

  async function submitPostActivity(postId, type, content) {
    if (activePostId) return false
    setActivePostId(postId)
    setError('')
    try {
      const updatedPost = await api.recordConnectionGroupPostActivity(page.id, postId, { type, content })
      setPosts((currentPosts) => currentPosts.map((post) => (
        post.id === postId ? mapOrganizationPost(updatedPost) : post
      )))
      return true
    } catch (activityError) {
      console.error(`Could not record organization page post ${type.toLowerCase()}:`, activityError)
      setError(activityError.message || 'Could not update this post. Please try again.')
      return false
    } finally {
      setActivePostId('')
    }
  }

  async function addPostComment(event, postId) {
    event.preventDefault()
    const text = (commentDrafts[postId] || '').trim()
    if (!text) return
    if (await submitPostActivity(postId, 'COMMENT', text)) {
      setCommentDrafts((drafts) => ({ ...drafts, [postId]: '' }))
    }
  }

  async function toggleFollow() {
    if (isUpdating) return
    setIsUpdating(true)
    setError('')
    try {
      const updatedPage = page.isFollowing
        ? await api.unfollowConnectionGroup(page.id)
        : await api.followConnectionGroup(page.id)
      onPageUpdate(mapOrganizationPage(updatedPage))
    } catch (followError) {
      console.error('Could not update organization page follow status:', followError)
      setError(followError.message || 'Could not update the follow status. Please try again.')
    } finally {
      setIsUpdating(false)
    }
  }

  async function toggleSubscribe() {
    if (isUpdating) return
    setIsUpdating(true)
    setError('')
    try {
      const updatedPage = page.isSubscribed
        ? await api.unsubscribeFromOrganizationPage(page.id)
        : await api.subscribeToOrganizationPage(page.id)
      onPageUpdate(mapOrganizationPage(updatedPage))
    } catch (subscribeError) {
      console.error('Could not update organization page subscription:', subscribeError)
      setError(subscribeError.message || 'Could not update the subscription. Please try again.')
    } finally {
      setIsUpdating(false)
    }
  }

  return (
    <section className="network-page-public-view" aria-label={`${page.name} public page`}>
      <button className="network-connections-back" type="button" onClick={onBack}>
        <ArrowLeft size={16} aria-hidden="true" /> Back to banners
      </button>
      <header className="network-page-public-header">
        {page.banner && (
          <img className="network-page-public-cover" src={page.banner} alt="" />
        )}
        <div className="network-page-banner-card-identity">
          <span className="network-page-banner-card-logo" aria-hidden="true">
            {page.logo ? <img src={page.logo} alt="" /> : <UsersRound size={28} />}
          </span>
          <div>
            <strong>{page.name}</strong>
            <small>
              {page.followerCount} {page.followerCount === 1 ? 'follower' : 'followers'}
              {' · '}
              {page.subscriberCount} {page.subscriberCount === 1 ? 'subscriber' : 'subscribers'}
            </small>
            {page.isOwner && getSubscriberNamesPreview(page) && (
              <small>Subscribers: {getSubscriberNamesPreview(page)}</small>
            )}
          </div>
        </div>
        <div className="network-page-public-actions">
          <button type="button" disabled={isUpdating} onClick={() => void toggleFollow()}>
            {page.isFollowing ? 'Following' : 'Follow'}
          </button>
          <button
            type="button"
            disabled={isUpdating || page.isSubscriptionPending}
            onClick={() => void toggleSubscribe()}
          >
            {page.isSubscribed ? 'Subscribed' : page.isSubscriptionPending ? 'Request pending' : 'Subscribe'}
          </button>
        </div>
      </header>
      <section className="network-page-public-posts" aria-labelledby="network-page-public-posts-title">
        <h2 id="network-page-public-posts-title">Page posts</h2>
        {error && <p className="network-connection-error" role="alert">{error}</p>}
        {isLoadingPosts ? (
          <p role="status">Loading page posts…</p>
        ) : posts.length ? (
          <div className="network-page-posts-list">
            {posts.map((post) => (
              <article className="network-page-post" key={post.id}>
                <header>
                  <div className="network-page-posts-avatar">
                    {page.logo
                      ? <img src={page.logo} alt="" />
                      : <span className="network-page-preview-logo-placeholder" aria-hidden="true"><i /><i /><i /></span>}
                  </div>
                  <div>
                    <strong>{page.name}</strong>
                    <time dateTime={post.createdAtIso}>{post.createdAt}</time>
                  </div>
                </header>
                {post.text && <p>{post.text}</p>}
                {post.media?.type === 'video' && <video src={post.media.source} controls />}
                {post.media?.type === 'photo' && <img src={post.media.source} alt="Page post attachment" />}
                <div className="network-page-member-engagement-summary">
                  <span>{post.reactions.length} reactions</span>
                  <span>{post.comments.length} comments</span>
                  <span>{Array.isArray(post.reposts) ? post.reposts.length : post.reposts || 0} reposts</span>
                </div>
                <div className="network-page-member-actions">
                  <button
                    type="button"
                    className={post.viewerReacted ? 'is-liked' : ''}
                    disabled={activePostId === post.id}
                    aria-pressed={post.viewerReacted}
                    onClick={() => void submitPostActivity(post.id, 'REACTION')}
                  >
                    <ThumbsUp size={16} aria-hidden="true" /> {post.viewerReacted ? 'Liked' : 'Like'}
                  </button>
                  <button
                    type="button"
                    disabled={activePostId === post.id || post.viewerReposted}
                    aria-pressed={post.viewerReposted}
                    onClick={() => void submitPostActivity(post.id, 'REPOST')}
                  >
                    {post.viewerReposted ? 'Reposted' : 'Repost'}
                  </button>
                </div>
                {post.comments.map((comment) => (
                  <p className="network-page-member-comment" key={comment.id}>
                    {comment.authorName ? `${comment.authorName}: ` : ''}{comment.text}
                  </p>
                ))}
                <form className="network-page-member-comment-form" onSubmit={(event) => addPostComment(event, post.id)}>
                  <input
                    id={`comment-${post.id}`}
                    value={commentDrafts[post.id] || ''}
                    onChange={(event) => setCommentDrafts((drafts) => ({ ...drafts, [post.id]: event.target.value }))}
                    placeholder="Write a comment..."
                    aria-label={`Comment on ${page.name} post`}
                    maxLength={1000}
                  />
                  <button type="submit" disabled={activePostId === post.id || !(commentDrafts[post.id] || '').trim()}>
                    Post
                  </button>
                </form>
              </article>
            ))}
          </div>
        ) : (
          <p className="network-page-member-feed-empty">There are no posts to show yet.</p>
        )}
      </section>
    </section>
  )
}

function PagePostsView({ page, posts, setPosts, postsError }) {
  const [composerMode, setComposerMode] = useState('')
  const [postText, setPostText] = useState('')
  const [articleTitle, setArticleTitle] = useState('')
  const [selectedMedia, setSelectedMedia] = useState(null)
  const [mediaError, setMediaError] = useState('')
  const [isPublishing, setIsPublishing] = useState(false)
  const mediaSafety = useMediaSafety(selectedMedia?.file || null)
  const adultContentScore = mediaSafety.predictions
    .filter((prediction) => ['Porn', 'Hentai'].includes(prediction.className))
    .reduce((highest, prediction) => Math.max(
      highest,
      prediction.probability || 0,
      prediction.maxProbability || 0,
    ), 0)
  const videoInputRef = useRef(null)
  const photoInputRef = useRef(null)

  function openComposer(mode = 'post') {
    setComposerMode(mode)
    setMediaError('')
  }

  function closeComposer() {
    setComposerMode('')
    setPostText('')
    setArticleTitle('')
    setSelectedMedia(null)
    setMediaError('')
  }

  function handleMediaChange(event, type) {
    const file = event.target.files?.[0]
    if (!file) return

    if (file.size > 10 * 1024 * 1024) {
      setMediaError('Choose a file smaller than 10 MB.')
      event.target.value = ''
      return
    }

    const reader = new FileReader()
    reader.onload = () => {
      setSelectedMedia({
        type,
        name: file.name,
        source: String(reader.result || ''),
        file,
      })
      setComposerMode('post')
      setMediaError('')
    }
    reader.onerror = () => setMediaError('Could not read that file. Please try another.')
    reader.readAsDataURL(file)
  }

  async function publishPost(event) {
    event.preventDefault()
    if ((!postText.trim() && !articleTitle.trim() && !selectedMedia) || isPublishing) return
    if (!page.id) {
      setMediaError('This page is not connected to the server yet. Refresh and try again.')
      return
    }

    setIsPublishing(true)
    setMediaError('')
    try {
      let mediaUrl = ''
      let mediaType = ''
      if (selectedMedia?.file) {
        const upload = await api.uploadMedia(selectedMedia.file, 'connection-group-post')
        if (!upload?.url) throw new Error('The page post upload did not return a media URL.')
        mediaUrl = upload.url
        mediaType = selectedMedia.file.type
      }
      const content = [articleTitle.trim(), postText.trim()].filter(Boolean).join('\n\n')
      const post = await api.createConnectionGroupPost(page.id, { content, mediaUrl, mediaType })
      setPosts((currentPosts) => [mapOrganizationPost(post), ...currentPosts])
      closeComposer()
    } catch (error) {
      console.error('Could not publish organization page post:', error)
      setMediaError(error.message || 'Could not publish this post. Please try again.')
    } finally {
      setIsPublishing(false)
    }
  }

  return (
    <div className="network-page-posts-view">
      <section className="network-page-posts-composer" aria-label="Create a page post">
        <div className="network-page-posts-composer-top">
          <div className="network-page-posts-avatar">
            {page.logo
              ? <img src={page.logo} alt="" />
              : <span className="network-page-preview-logo-placeholder" aria-hidden="true"><i /><i /><i /></span>}
          </div>
          <button
            className="network-page-posts-start"
            type="button"
            aria-expanded={Boolean(composerMode)}
            onClick={() => openComposer()}
          >
            Start a post
          </button>
        </div>
        <div className="network-page-posts-actions">
          <button type="button" onClick={() => videoInputRef.current?.click()}>
            <Video size={18} aria-hidden="true" /> Video
          </button>
          <button type="button" onClick={() => photoInputRef.current?.click()}>
            <Image size={18} aria-hidden="true" /> Photo
          </button>
          <button type="button" onClick={() => openComposer('article')}>
            <FileText size={18} aria-hidden="true" /> Write article
          </button>
        </div>
        <input
          ref={videoInputRef}
          className="network-page-posts-file-input"
          type="file"
          accept="video/*"
          aria-label="Choose a video"
          onChange={(event) => handleMediaChange(event, 'video')}
        />
        <input
          ref={photoInputRef}
          className="network-page-posts-file-input"
          type="file"
          accept="image/*"
          aria-label="Choose a photo"
          onChange={(event) => handleMediaChange(event, 'photo')}
        />
        {composerMode && (
          <form className="network-page-posts-form" onSubmit={publishPost}>
            {composerMode === 'article' && (
              <label>
                Article title
                <input
                  type="text"
                  value={articleTitle}
                  onChange={(event) => setArticleTitle(event.target.value)}
                  placeholder="Add a title for your article"
                  maxLength={120}
                />
              </label>
            )}
            <label className="network-page-posts-sr-only" htmlFor="network-page-post-content">
              {composerMode === 'article' ? 'Article content' : 'Post content'}
            </label>
            <textarea
              id="network-page-post-content"
              value={postText}
              onChange={(event) => setPostText(event.target.value)}
              placeholder={composerMode === 'article' ? 'Write your article...' : 'What would you like to share?'}
              rows={4}
            />
            {selectedMedia && (
              <div className="network-page-posts-media-preview">
                {selectedMedia.type === 'video'
                  ? <video src={selectedMedia.source} controls />
                  : <img src={selectedMedia.source} alt="Selected post attachment" />}
                <MediaSafetyStatus
                  safety={mediaSafety}
                  blockedMessage={`This ${mediaSafety.kind} has a high adult-content score and cannot be posted.`}
                  safeMessage="Content moderation passed."
                />
                {mediaSafety.predictions.length > 0 && (
                  <p className={`network-page-posts-moderation-score${mediaSafety.isRejected ? ' is-flagged' : ''}`}>
                    Adult-content probability: {Math.round(adultContentScore * 100)}%
                  </p>
                )}
                <button type="button" onClick={() => setSelectedMedia(null)}>Remove attachment</button>
              </div>
            )}
            {mediaError && <p className="network-page-posts-error" role="alert">{mediaError}</p>}
            <div className="network-page-posts-form-actions">
              <button type="button" onClick={closeComposer}>Cancel</button>
              <button
                type="submit"
                disabled={isPublishing
                  || mediaSafety.isBlocked
                  || (!postText.trim() && !articleTitle.trim() && !selectedMedia)}
              >
                {isPublishing ? 'Posting…' : 'Post'}
              </button>
            </div>
          </form>
        )}
      </section>
      {postsError && <p className="network-page-posts-error" role="alert">{postsError}</p>}

      {posts.length ? (
        <div className="network-page-posts-list">
          {posts.map((post) => (
            <article className="network-page-post" key={post.id}>
              <header>
                <div className="network-page-posts-avatar">
                  {page.logo
                    ? <img src={page.logo} alt="" />
                    : <span className="network-page-preview-logo-placeholder" aria-hidden="true"><i /><i /><i /></span>}
                </div>
                <div>
                  <strong>{page.name}</strong>
                  <time dateTime={post.createdAtIso}>{post.createdAt}</time>
                </div>
              </header>
              {post.title && <h2>{post.title}</h2>}
              {post.text && <p>{post.text}</p>}
              {post.media?.type === 'video' && <video src={post.media.source} controls />}
              {post.media?.type === 'photo' && <img src={post.media.source} alt="Post attachment" />}
              <div className="network-page-post-engagement">
                <span>{post.reactions?.length || 0} reactions</span>
                <span>{post.comments?.length || 0} comments</span>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <section className="network-page-posts-empty">
          <div className="network-page-posts-empty-art" aria-hidden="true">
            <div className="network-page-posts-window">
              <span />
              <span />
              <span />
            </div>
            <div className="network-page-posts-person">
              <i />
              <b />
            </div>
            <div className="network-page-posts-desk" />
          </div>
          <h2>Your page doesn’t have any posts yet</h2>
          <p>Pages that post 2x a week grow 5x faster</p>
          <button type="button" onClick={() => openComposer()}>
            <Plus size={18} aria-hidden="true" /> Start a post
          </button>
        </section>
      )}
    </div>
  )
}

function OrganizationPageEdit({ page, onSave, onCancel }) {
  const [draft, setDraft] = useState({ ...page })
  const [uploadError, setUploadError] = useState('')
  const [saveError, setSaveError] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  function handleImageUpload(event, field) {
    const file = event.target.files?.[0]
    if (!file) return

    if (!file.type.startsWith('image/')) {
      setUploadError('Choose an image file.')
      event.target.value = ''
      return
    }

    if (file.size > 5 * 1024 * 1024) {
      setUploadError('Image files must be 5 MB or smaller.')
      event.target.value = ''
      return
    }

    const reader = new FileReader()
    reader.onload = () => {
      setDraft((currentDraft) => ({ ...currentDraft, [field]: String(reader.result || '') }))
      setUploadError('')
    }
    reader.onerror = () => setUploadError('Could not read that image. Please try another.')
    reader.readAsDataURL(file)
  }

  async function submitEdit(event) {
    event.preventDefault()
    if (isSaving) return
    setIsSaving(true)
    try {
      const error = await onSave(draft)
      if (error) setSaveError(error)
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="network-page-edit" aria-labelledby="network-page-edit-title">
      <header className="network-page-edit-heading">
        <h2 id="network-page-edit-title">Page info</h2>
        <p>* indicates required</p>
      </header>

      <form className="network-page-edit-form" onSubmit={submitEdit}>
        <div className="network-page-edit-images">
          <div className="network-page-edit-logo-field">
            <span>Logo</span>
            <div className="network-page-edit-logo">
              {draft.logo
                ? <img src={draft.logo} alt="Organization logo" />
                : <span className="network-page-preview-logo-placeholder" aria-hidden="true"><i /><i /><i /></span>}
              <label className="network-page-edit-logo-button" aria-label="Change logo">
                <Pencil size={14} aria-hidden="true" />
                <input type="file" accept="image/*" onChange={(event) => handleImageUpload(event, 'logo')} />
              </label>
            </div>
          </div>

          <div className="network-page-edit-banner-field">
            <span>Banner</span>
            <div className="network-page-edit-banner" aria-label={draft.banner ? 'Custom page banner' : 'Default page banner'}>
              {draft.banner && <img src={draft.banner} alt="Custom page banner" />}
            </div>
            {draft.banner ? (
              <button
                className="network-page-edit-banner-remove"
                type="button"
                onClick={() => {
                  setDraft((currentDraft) => ({ ...currentDraft, banner: '' }))
                  setSaveError('')
                }}
              >
                Remove banner
              </button>
            ) : (
              <label className="network-page-edit-banner-add">
                <Upload size={14} aria-hidden="true" />
                Add banner
                <input
                  type="file"
                  accept="image/*"
                  onChange={(event) => handleImageUpload(event, 'banner')}
                />
              </label>
            )}
          </div>
        </div>

        <label className="network-page-setup-field">
          <span>Name <strong>*</strong></span>
          <input
            type="text"
            value={draft.name}
            onChange={(event) => setDraft((currentDraft) => ({ ...currentDraft, name: event.target.value }))}
            maxLength={100}
            required
          />
          <small className="network-page-edit-counter">{draft.name.length}/100</small>
        </label>

        <label className="network-page-setup-field">
          <span>LinkedIn public URL <strong>*</strong> <span className="network-page-edit-info" title="Your organization page's public address">ⓘ</span></span>
          <div className="network-page-url-input">
            <span>linkedin.com/company</span>
            <input
              type="text"
              value={draft.publicUrl}
              onChange={(event) => setDraft((currentDraft) => ({ ...currentDraft, publicUrl: event.target.value }))}
              pattern="[A-Za-z0-9][A-Za-z0-9-]{0,61}[A-Za-z0-9]"
              title="Use 2-63 letters, numbers, or hyphens."
              maxLength={63}
              required
            />
          </div>
        </label>

        <label className="network-page-setup-field">
          <span>Tagline</span>
          <textarea
            value={draft.tagline}
            onChange={(event) => setDraft((currentDraft) => ({ ...currentDraft, tagline: event.target.value }))}
            placeholder="Add your slogan or mission statement"
            maxLength={120}
            rows={3}
          />
          <small className="network-page-edit-counter">{draft.tagline.length}/120</small>
        </label>

        {(uploadError || saveError) && (
          <p className="network-page-field-error" role="alert">{uploadError || saveError}</p>
        )}
        <div className="network-page-edit-actions">
          <button type="button" onClick={onCancel} disabled={isSaving}>Cancel</button>
          <button type="submit" disabled={isSaving}>{isSaving ? 'Saving…' : 'Save changes'}</button>
        </div>
      </form>
    </section>
  )
}

// ─── Manage Admins View ───────────────────────────────────────────────────────
function ManageAdminsView({ page, onBack }) {
  const [activeTab, setActiveTab] = useState('page')
  const [bannerDismissed, setBannerDismissed] = useState(false)

  const tabs = [
    { key: 'page', label: 'Page admins', count: 1 },
    { key: 'paid', label: 'Paid media admins', count: 0 },
    { key: 'integrations', label: 'Integrations', count: 0 },
  ]

  return (
    <div className="bps-manage-admins">
      <div className="bps-manage-admins-header">
        <button type="button" className="bps-back-btn" onClick={onBack}>
          <ArrowLeft size={18} aria-hidden="true" />
        </button>
        <h2>Manage Admins</h2>
      </div>

      {!bannerDismissed && (
        <div className="bps-admins-info-banner">
          <span className="bps-admins-info-icon" aria-hidden="true">
            <Shield size={28} />
          </span>
          <div>
            <strong>Managing admins just got better!</strong>
            <p>
              You can now assign various roles, and add admins who aren&apos;t 1st degree
              connections.{' '}
              <a href="#learn-more" onClick={(e) => e.preventDefault()}>
                Learn more
              </a>
            </p>
          </div>
          <button
            type="button"
            className="bps-admins-info-close"
            aria-label="Dismiss"
            onClick={() => setBannerDismissed(true)}
          >
            <X size={16} />
          </button>
        </div>
      )}

      <div className="bps-admins-tabs">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            className={`bps-admins-tab${activeTab === tab.key ? ' is-active' : ''}`}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label} ({tab.count})
          </button>
        ))}
      </div>

      {activeTab === 'page' && (
        <div className="bps-admins-body">
          <div className="bps-admins-info-row">
            <p>
              All page admins have access to admin view, with different permissions based on their
              role.{' '}
              <a href="#learn-more" onClick={(e) => e.preventDefault()}>
                Learn more
              </a>
            </p>
            <button type="button" className="bps-add-admin-btn">
              <Plus size={14} aria-hidden="true" /> Add admin
            </button>
          </div>

          <table className="bps-admins-table">
            <thead>
              <tr>
                <th>Profile</th>
                <th>
                  Roles{' '}
                  <span className="bps-info-circle" title="Role information" aria-label="Role information">
                    ⓘ
                  </span>
                </th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td className="bps-admins-profile">
                  {page.logo ? (
                    <img src={page.logo} alt="" className="bps-admins-avatar" />
                  ) : (
                    <span className="bps-admins-avatar-placeholder" aria-hidden="true">
                      <i /><i /><i />
                    </span>
                  )}
                  <div className="bps-admins-profile-info">
                    <strong>{page.name || 'You'} · You</strong>
                    <span>{page.tagline || 'Full Stack Developer | Java Developer'}</span>
                  </div>
                </td>
                <td>
                  <span className="bps-role-badge">Super admin</span>
                </td>
                <td className="bps-admins-actions">
                  <button type="button" aria-label="Edit admin role">
                    <Pencil size={16} />
                  </button>
                  <button type="button" aria-label="Remove admin">
                    <Trash2 size={16} />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      )}

      {activeTab !== 'page' && (
        <div className="bps-admins-empty">
          <p>No {tabs.find((t) => t.key === activeTab)?.label.toLowerCase()} yet.</p>
        </div>
      )}
    </div>
  )
}

// ─── Blocked Members View ───────────────────────────────────────────────────
function BlockedMembersView({ onBack }) {
  return (
    <div className="bps-blocked-view">
      <div className="bps-settings-subheader">
        <button type="button" className="bps-back-btn" onClick={onBack} aria-label="Back to settings">
          <ArrowLeft size={18} aria-hidden="true" />
        </button>
        <h2>Blocked members and pages</h2>
      </div>
      <div className="bps-blocked-empty">
        <div className="bps-blocked-illustration" aria-hidden="true">
          <span className="bps-blocked-person">●</span>
          <span className="bps-blocked-office">
            <i /><i /><i />
          </span>
          <span className="bps-blocked-paper" />
        </div>
        <h3>No blocked members or<br />pages</h3>
        <p>
          To block a member or page, go to their<br />
          profile and select Block from More. <a href="#learn-more" onClick={(event) => event.preventDefault()}>Learn<br />more about blocking</a>
        </p>
      </div>
    </div>
  )
}

// ─── Following Pages View ──────────────────────────────────────────────────
function FollowingView({ onClose }) {
  const [activeTab, setActiveTab] = useState('following')

  return (
    <div className="bps-following-overlay" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose()
    }}>
      <section className="bps-following-modal" role="dialog" aria-modal="true" aria-labelledby="bps-following-title">
        <header className="bps-following-header">
          <h2 id="bps-following-title">Find pages to follow</h2>
          <button type="button" onClick={onClose} aria-label="Close">
            <X size={20} />
          </button>
        </header>
        <div className="bps-following-search">
          <input type="search" aria-label="Add pages to follow" placeholder="Add pages to follow" />
        </div>
        <div className="bps-following-tabs" role="tablist" aria-label="Following pages">
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'following'}
            className={activeTab === 'following' ? 'is-active' : ''}
            onClick={() => setActiveTab('following')}
          >
            Following (0)
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'recommended'}
            className={activeTab === 'recommended' ? 'is-active' : ''}
            onClick={() => setActiveTab('recommended')}
          >
            Recommended
          </button>
        </div>
        <div className="bps-following-empty">
          <div className="bps-following-illustration" aria-hidden="true">
            <span className="bps-following-person" />
            <span className="bps-following-desk" />
            <span className="bps-following-monitor" />
            <span className="bps-following-plant">♧</span>
          </div>
          <h3>You&apos;re not following any<br />pages</h3>
          <p>
            Follow pages to see their posts in your feed.<br />
            Browse recommendations or search for<br />
            organizations in your industry to get started.
          </p>
          <a href="#learn-more" onClick={(event) => event.preventDefault()}>Learn more</a>
        </div>
        <footer className="bps-following-footer">
          <button type="button" onClick={onClose}>Done</button>
        </footer>
      </section>
    </div>
  )
}

// ─── Events Admin View ─────────────────────────────────────────────────────
function EventsAdminView() {
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState('Last modified')

  return (
    <div className="bps-events-admin">
      <header className="bps-events-header">
        <div>
          <h2>Events Admin</h2>
          <span>0 events</span>
        </div>
        <label className="bps-events-search">
          <Search size={16} aria-hidden="true" />
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search events"
            aria-label="Search events"
          />
        </label>
      </header>

      <div className="bps-events-toolbar">
        <button type="button" className="bps-events-all">All</button>
        <label className="bps-events-sort">
          <select
            value={sort}
            onChange={(event) => setSort(event.target.value)}
            aria-label="Sort events"
          >
            <option>Last modified</option>
            <option>Newest first</option>
            <option>Oldest first</option>
          </select>
          <ChevronDown size={15} aria-hidden="true" />
        </label>
      </div>

      <div className="bps-events-empty">
        <div className="bps-events-illustration" aria-hidden="true">
          <span className="bps-events-person" />
          <span className="bps-events-desk" />
          <span className="bps-events-monitor" />
          <span className="bps-events-plant">♧</span>
        </div>
        <h3>No events yet</h3>
        <p>Create an event to get started.</p>
        <button type="button" onClick={() => setQuery('')}>Create event</button>
      </div>
    </div>
  )
}

function PageMemberFeedView({ page, posts, setPosts }) {
  const [commentDrafts, setCommentDrafts] = useState({})
  const [isFollowing, setIsFollowing] = useState(false)
  const [isSubscribed, setIsSubscribed] = useState(false)

  function toggleReaction(post) {
    setPosts((currentPosts) => currentPosts.map((currentPost) => {
      if (currentPost.id !== post.id) return currentPost
      const reactions = currentPost.reactions || []
      const existingReactionIndex = reactions.findIndex((reaction) => reaction.id === 'page-visitor')
      if (existingReactionIndex >= 0) {
        return { ...currentPost, reactions: reactions.filter((_, index) => index !== existingReactionIndex) }
      }
      return {
        ...currentPost,
        reactions: [...reactions, { id: 'page-visitor', createdAt: new Date().toISOString() }],
      }
    }))
  }

  function addComment(event, postId) {
    event.preventDefault()
    const text = (commentDrafts[postId] || '').trim()
    if (!text) return

    setPosts((currentPosts) => currentPosts.map((post) => (
      post.id === postId
        ? {
          ...post,
          comments: [...(post.comments || []), {
            id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
            text,
            createdAt: new Date().toISOString(),
          }],
        }
        : post
    )))
    setCommentDrafts((drafts) => ({ ...drafts, [postId]: '' }))
  }

  return (
    <div className="network-page-member-feed">
      <header>
        <div className="network-page-member-page-identity">
          <div className="network-page-posts-avatar">
            {page.logo
              ? <img src={page.logo} alt="" />
              : <span className="network-page-preview-logo-placeholder" aria-hidden="true"><i /><i /><i /></span>}
          </div>
          <div>
            <h2>{page.name}</h2>
            {page.tagline && <p>{page.tagline}</p>}
            <small>{isFollowing ? '1 follower' : '0 followers'}</small>
          </div>
        </div>
        <div className="network-page-member-page-actions">
          <button
            type="button"
            className={isFollowing ? 'is-active' : ''}
            aria-pressed={isFollowing}
            onClick={() => setIsFollowing((following) => !following)}
          >
            {isFollowing ? 'Following' : 'Follow'}
          </button>
          <button
            type="button"
            className={isSubscribed ? 'is-active' : ''}
            aria-pressed={isSubscribed}
            onClick={() => setIsSubscribed((subscribed) => !subscribed)}
          >
            {isSubscribed ? 'Subscribed' : 'Subscribe'}
          </button>
        </div>
      </header>
      {posts.length ? posts.map((post) => {
        const reactions = post.reactions || []
        const comments = post.comments || []
        const isLiked = reactions.some((reaction) => reaction.id === 'page-visitor')
        return (
          <article className="network-page-post" key={post.id}>
            <header>
              <div className="network-page-posts-avatar">
                {page.logo
                  ? <img src={page.logo} alt="" />
                  : <span className="network-page-preview-logo-placeholder" aria-hidden="true"><i /><i /><i /></span>}
              </div>
              <div>
                <strong>{page.name}</strong>
                <time dateTime={post.createdAtIso}>{post.createdAt}</time>
              </div>
            </header>
            {post.title && <h2>{post.title}</h2>}
            {post.text && <p>{post.text}</p>}
            {post.media?.type === 'video' && <video src={post.media.source} controls />}
            {post.media?.type === 'photo' && <img src={post.media.source} alt="Post attachment" />}
            <div className="network-page-member-engagement-summary">
              <span>{reactions.length} reactions</span>
              <span>{comments.length} comments</span>
            </div>
            <div className="network-page-member-actions">
              <button type="button" className={isLiked ? 'is-liked' : ''} onClick={() => toggleReaction(post)}>
                <ThumbsUp size={16} aria-hidden="true" /> {isLiked ? 'Liked' : 'Like'}
              </button>
              <a href={`#comment-${post.id}`} onClick={(event) => {
                event.preventDefault()
                document.getElementById(`comment-${post.id}`)?.focus()
              }}>
                <MessageCircle size={16} aria-hidden="true" /> Comment
              </a>
            </div>
            {comments.map((comment) => (
              <p className="network-page-member-comment" key={comment.id}>{comment.text}</p>
            ))}
            <form className="network-page-member-comment-form" onSubmit={(event) => addComment(event, post.id)}>
              <input
                id={`comment-${post.id}`}
                value={commentDrafts[post.id] || ''}
                onChange={(event) => setCommentDrafts((drafts) => ({ ...drafts, [post.id]: event.target.value }))}
                placeholder="Write a comment..."
                aria-label={`Comment on ${post.title || 'page post'}`}
              />
              <button type="submit" disabled={!(commentDrafts[post.id] || '').trim()}>Post</button>
            </form>
          </article>
        )
      }) : (
        <div className="network-page-member-feed-empty">There are no posts to show yet.</div>
      )}
    </div>
  )
}

function PageAnalyticsView({ posts }) {
  const [activeTab, setActiveTab] = useState('Content')
  const [selectedMetric, setSelectedMetric] = useState('Impressions')
  const [rangeEnd] = useState(() => new Date())
  const rangeWeeks = 5
  const currentWeekStart = new Date(rangeEnd)
  currentWeekStart.setHours(0, 0, 0, 0)
  currentWeekStart.setDate(currentWeekStart.getDate() - ((currentWeekStart.getDay() + 6) % 7))
  const rangeStart = new Date(currentWeekStart)
  rangeStart.setDate(rangeStart.getDate() - ((rangeWeeks - 1) * 7))

  const dailyMetrics = []
  for (let day = new Date(rangeStart); day <= rangeEnd; day.setDate(day.getDate() + 1)) {
    const metricDay = new Date(day)
    const dayKey = getLocalDateKey(day)
    dailyMetrics.push({
      day: metricDay,
      impressions: posts.reduce((count, post) => count + getDailyPostMetric(post, 'impressions', dayKey), 0),
      reactions: posts.reduce((count, post) => count + getDailyPostMetric(post, 'reactions', dayKey), 0),
      comments: posts.reduce((count, post) => count + getDailyPostMetric(post, 'comments', dayKey), 0),
      reposts: posts.reduce((count, post) => count + getDailyPostMetric(post, 'reposts', dayKey), 0),
    })
  }
  const weeklyMetrics = Array.from({ length: rangeWeeks }, (_, weekIndex) => {
    const weekStart = new Date(rangeStart)
    weekStart.setDate(weekStart.getDate() + (weekIndex * 7))
    const days = dailyMetrics.slice(weekIndex * 7, (weekIndex + 1) * 7)
    return {
      day: weekStart,
      impressions: days.reduce((sum, metric) => sum + metric.impressions, 0),
      reactions: days.reduce((sum, metric) => sum + metric.reactions, 0),
      comments: days.reduce((sum, metric) => sum + metric.comments, 0),
      reposts: days.reduce((sum, metric) => sum + metric.reposts, 0),
    }
  })
  const totals = dailyMetrics.reduce((summary, metric) => ({
    impressions: summary.impressions + metric.impressions,
    reactions: summary.reactions + metric.reactions,
    comments: summary.comments + metric.comments,
    reposts: summary.reposts + metric.reposts,
  }), { impressions: 0, reactions: 0, comments: 0, reposts: 0 })

  const metricValues = {
    Impressions: weeklyMetrics.map((metric) => metric.impressions),
    Reactions: weeklyMetrics.map((metric) => metric.reactions),
    Comments: weeklyMetrics.map((metric) => metric.comments),
    Reposts: weeklyMetrics.map((metric) => metric.reposts),
  }
  const chartValues = metricValues[selectedMetric]
  const chartMax = Math.max(4, Math.ceil(Math.max(...chartValues) / 4) * 4)
  const yAxisTicks = Array.from({ length: 5 }, (_, index) => chartMax * (4 - index) / 4)
  const chartPoints = chartValues.map((value, index) => {
    const x = chartValues.length > 1 ? 48 + (index / (chartValues.length - 1)) * 676 : 386
    const y = 120 - (value / chartMax) * 108
    return `${x},${y}`
  }).join(' ')
  const highlightStats = [
    { label: 'Impressions', value: totals.impressions },
    { label: 'Reactions', value: totals.reactions },
    { label: 'Comments', value: totals.comments },
    { label: 'Reposts', value: totals.reposts },
  ]

  const tabs = ['Content', 'Visitors', 'Followers', 'Page appearances']
  return (
    <div className="bps-analytics">
      <section className="bps-analytics-heading">
        <h2>Analytics</h2>
        <nav aria-label="Analytics sections">
          {tabs.map((tab) => (
            <button
              type="button"
              key={tab}
              className={activeTab === tab ? 'is-active' : ''}
              onClick={() => setActiveTab(tab)}
            >
              {tab}
            </button>
          ))}
        </nav>
      </section>

      <section className="bps-analytics-highlights">
        <h3>Highlights</h3>
        <p>Engagement from your page posts · Last 5 weeks</p>
        <div className="bps-analytics-stats">
          {highlightStats.map((stat) => (
            <article key={stat.label}>
              <strong>{stat.value}</strong>
              <span>{stat.label}</span>
              <small>{stat.label === 'Reactions' ? 'Post likes' : stat.label === 'Comments' ? 'Post comments' : stat.label === 'Impressions' ? 'Post impressions' : 'Post reposts'}</small>
            </article>
          ))}
        </div>
      </section>

      <section className="bps-analytics-metrics">
        <h3>Metrics</h3>
        <label className="bps-analytics-metric-select">
          <span className="bps-analytics-sr-only">Chart metric</span>
          <select value={selectedMetric} onChange={(event) => setSelectedMetric(event.target.value)}>
            {['Impressions', 'Reactions', 'Comments', 'Reposts'].map((metric) => <option key={metric}>{metric}</option>)}
          </select>
          <ChevronDown size={14} aria-hidden="true" />
        </label>
        <div className="bps-analytics-chart">
          <svg viewBox="0 0 732 140" preserveAspectRatio="none" role="img" aria-label={`${selectedMetric} over the selected date range`}>
            {yAxisTicks.map((tick, index) => {
              const y = 12 + index * 27
              return (
                <g key={tick}>
                  <line className="bps-analytics-grid-line" x1="48" y1={y} x2="730" y2={y} />
                  <text className="bps-analytics-axis-label" x="40" y={y + 3} textAnchor="end">
                    {tick}
                  </text>
                </g>
              )
            })}
            <line className="bps-analytics-y-axis-line" x1="48" y1="12" x2="48" y2="120" />
            <polyline points={chartPoints} />
            {chartValues.map((value, index) => {
              if (!value) return null
              const x = 48 + (index / (chartValues.length - 1)) * 676
              const y = 120 - (value / chartMax) * 108
              return (
                <circle
                  key={getLocalDateKey(weeklyMetrics[index].day)}
                  className="bps-analytics-point"
                  cx={x}
                  cy={y}
                  r="2.5"
                />
              )
            })}
          </svg>
          <div className="bps-analytics-chart-labels">
            {weeklyMetrics.map((metric) => (
              <span key={getLocalDateKey(metric.day)}>
                {metric.day.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
              </span>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}

// ─── Settings View ────────────────────────────────────────────────────────────
function SettingsView({ onNavigate }) {
  const settingsItems = [
    {
      key: 'manage-admins',
      title: 'Manage admins',
      description: 'Control who manages your page',
    },
    {
      key: 'blocked',
      title: 'Manage blocked members and pages',
      description: 'See all the blocked members and pages',
    },
    {
      key: 'following',
      title: 'Manage following',
      description: 'See all the pages your page follows',
    },
    {
      key: 'inbox',
      title: 'Inbox settings',
      description: 'Choose whether members can message the page and select conversation topics',
    },
    {
      key: 'deactivate',
      title: 'Deactivate page',
      description: 'Take your page down',
    },
  ]

  return (
    <div className="bps-settings">
      <h2 className="bps-settings-title">Settings</h2>
      <ul className="bps-settings-list">
        {settingsItems.map((item) => (
          <li key={item.key}>
            <button
              type="button"
              className="bps-settings-item"
              onClick={() => onNavigate(item.key)}
            >
              <div className="bps-settings-item-text">
                <strong>{item.title}</strong>
                <span>{item.description}</span>
              </div>
              <ChevronRight size={18} className="bps-settings-chevron" aria-hidden="true" />
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}

function DeactivatePageView({ onBack, onConfirm }) {
  const [error, setError] = useState('')

  async function deactivatePage() {
    const result = await onConfirm()
    if (result) setError(result)
  }

  return (
    <section className="bps-deactivate-page" aria-labelledby="bps-deactivate-title">
      <header className="bps-settings-subheader">
        <button type="button" className="bps-back-btn" onClick={onBack} aria-label="Back to settings">
          <ArrowLeft size={18} aria-hidden="true" />
        </button>
        <h2 id="bps-deactivate-title">Deactivate page</h2>
      </header>
      <div className="bps-deactivate-content">
        <h3>Are you sure you want to deactivate this page?</h3>
        <p>Deactivating will take your organization page down and delete its saved page information, including its banner and logo.</p>
        {error && <p className="bps-deactivate-error" role="alert">{error}</p>}
        <div className="bps-deactivate-actions">
          <button type="button" onClick={onBack}>Cancel</button>
          <button type="button" onClick={deactivatePage}>Deactivate page</button>
        </div>
      </div>
    </section>
  )
}

// ─── Organization Page Dashboard ──────────────────────────────────────────────
function OrganizationPageDashboard({ page, onEditPage, onDeactivatePage }) {
  const [activeItem, setActiveItem] = useState('Dashboard')
  const [settingsPage, setSettingsPage] = useState(null)
  const [pagePosts, setPagePosts] = useState([])
  const [pagePostsErrorState, setPagePostsErrorState] = useState({ pageId: null, message: '' })
  const [dismissedTips, setDismissedTips] = useState([])
  const [isOfferHidden, setIsOfferHidden] = useState(false)
  const [isCreateMenuOpen, setIsCreateMenuOpen] = useState(false)
  const [isViewingAsMember, setIsViewingAsMember] = useState(false)

  const menuItems = ['Dashboard', 'Page posts', 'Analytics', 'Feed', 'Activity', 'Inbox']
  const tips = [
    { id: 'website', title: 'Add website URL', detail: 'Add a URL to drive more page visitors to your website.' },
    { id: 'description', title: 'Add description', detail: 'Add a quick description to get your page discovered in search results.' },
  ]

  useEffect(() => {
    if (!page.id) return undefined

    let isCurrent = true
    api.getConnectionGroupPosts(page.id)
      .then((feed) => {
        if (isCurrent) {
          setPagePosts((feed?.posts || []).map(mapOrganizationPost))
          setPagePostsErrorState({ pageId: page.id, message: '' })
        }
      })
      .catch((error) => {
        console.error('Could not load organization page posts:', error)
        if (isCurrent) {
          setPagePostsErrorState({
            pageId: page.id,
            message: error.message || 'Could not load page posts. Please try again.',
          })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [page.id])

  useEffect(() => {
    if (!page.id || activeItem !== 'Analytics') return undefined

    let isCurrent = true
    const refreshAnalyticsPosts = () => {
      api.getConnectionGroupPosts(page.id)
        .then((feed) => {
          if (isCurrent) setPagePosts((feed?.posts || []).map(mapOrganizationPost))
        })
        .catch((error) => {
          console.error('Could not refresh organization page analytics:', error)
          if (isCurrent) {
            setPagePostsErrorState({
              pageId: page.id,
              message: error.message || 'Could not refresh page analytics. Please try again.',
            })
          }
        })
    }

    refreshAnalyticsPosts()
    const refreshInterval = window.setInterval(refreshAnalyticsPosts, 30000)
    return () => {
      isCurrent = false
      window.clearInterval(refreshInterval)
    }
  }, [activeItem, page.id])

  const pagePostsError = pagePostsErrorState.pageId === page.id ? pagePostsErrorState.message : ''

  return (
    <section
      className={`network-page-dashboard${isViewingAsMember ? ' is-member-preview' : ''}`}
      aria-label={isViewingAsMember ? `${page.name} public page preview` : `${page.name} page dashboard`}
    >
      {!isViewingAsMember && <aside className="network-page-dashboard-sidebar">
        <div
          className="network-page-dashboard-cover"
          style={page.banner
            ? { backgroundImage: `url("${page.banner}")`, backgroundPosition: 'center', backgroundSize: 'cover' }
            : undefined}
        />
        <div className="network-page-dashboard-identity">
          <div className="network-page-dashboard-logo">
            {page.logo
              ? <img src={page.logo} alt="" />
              : <span className="network-page-preview-logo-placeholder" aria-hidden="true"><i /><i /><i /></span>}
          </div>
          <strong>{page.name}</strong>
          <small>{page.subscriberCount} subscribers</small>
          {getSubscriberNamesPreview(page) && (
            <small>Members: {getSubscriberNamesPreview(page)}</small>
          )}
          <div className="network-page-dashboard-create-wrap">
            <button
              className="network-page-dashboard-create"
              type="button"
              aria-expanded={isCreateMenuOpen}
              onClick={() => setIsCreateMenuOpen((open) => !open)}
            >
              <Plus size={14} aria-hidden="true" /> Create
            </button>
            {isCreateMenuOpen && (
              <div className="network-page-dashboard-create-menu">
                {['Post', 'Event'].map((item) => (
                  <button
                    key={item}
                    type="button"
                    onClick={() => {
                      setActiveItem(item === 'Post' ? 'Page posts' : item)
                      setIsCreateMenuOpen(false)
                      setIsViewingAsMember(false)
                    }}
                  >
                    Create {item.toLowerCase()}
                  </button>
                ))}
              </div>
            )}
          </div>
          <button
            className="network-page-dashboard-member-view"
            type="button"
            aria-pressed={isViewingAsMember}
            onClick={() => setIsViewingAsMember((viewing) => !viewing)}
          >
            {isViewingAsMember ? 'Exit member view' : 'View as member'}
          </button>
        </div>

        <nav className="network-page-dashboard-nav" aria-label="Organization page">
          {menuItems.map((item) => (
            <button
              className={activeItem === item ? 'is-active' : ''}
              type="button"
              key={item}
              onClick={() => { setActiveItem(item); setSettingsPage(null); setIsViewingAsMember(false) }}
            >
              {item}
            </button>
          ))}
          <button className="network-page-dashboard-edit" type="button" onClick={onEditPage}>
            Edit page
          </button>
          <div className="network-page-dashboard-nav-divider" />
          {['Events'].map((item) => (
            <button
              className={activeItem === item ? 'is-active' : ''}
              type="button"
              key={item}
              onClick={() => { setActiveItem(item); setSettingsPage(null); setIsViewingAsMember(false) }}
            >
              {item}
            </button>
          ))}
          <div className="network-page-dashboard-nav-divider" />
          {['Settings', 'Chat with support'].map((item) => (
            <button
              className={activeItem === item ? 'is-active' : ''}
              type="button"
              key={item}
              onClick={() => {
                setActiveItem(item)
                setIsViewingAsMember(false)
                if (item === 'Settings') setSettingsPage('settings')
              }}
            >
              {item}
            </button>
          ))}
        </nav>
      </aside>}

      <div className="network-page-dashboard-content">
        {isViewingAsMember ? (
          <div className="network-page-member-preview">
            <button type="button" onClick={() => setIsViewingAsMember(false)}>Exit member view</button>
            <PageMemberFeedView page={page} posts={pagePosts} setPosts={setPagePosts} />
          </div>
        ) : activeItem === 'Settings' ? (
          settingsPage === 'manage-admins' ? (
            <ManageAdminsView page={page} onBack={() => setSettingsPage('settings')} />
          ) : settingsPage === 'blocked' ? (
            <BlockedMembersView onBack={() => setSettingsPage('settings')} />
          ) : settingsPage === 'following' ? (
            <FollowingView onClose={() => setSettingsPage('settings')} />
          ) : settingsPage === 'deactivate' ? (
            <DeactivatePageView
              onBack={() => setSettingsPage('settings')}
              onConfirm={onDeactivatePage}
            />
          ) : (
            <SettingsView
              onNavigate={(key) => setSettingsPage(key)}
            />
          )
        ) : activeItem === 'Events' ? (
          <EventsAdminView />
        ) : activeItem === 'Analytics' ? (
          <>
            {pagePostsError && <p className="network-connection-error" role="alert">{pagePostsError}</p>}
            <PageAnalyticsView posts={pagePosts} />
          </>
        ) : activeItem === 'Page posts' ? (
          <PagePostsView page={page} posts={pagePosts} setPosts={setPagePosts} postsError={pagePostsError} />
        ) : (
          <>
        <section className="network-page-dashboard-panel network-page-growth-panel">
          <header>
            <h2>Grow your followers 4x faster</h2>
            <button type="button" aria-label="Growth tips information" title="Suggestions to help grow your page">
              ⓘ
            </button>
          </header>
          {tips.filter((tip) => !dismissedTips.includes(tip.id)).map((tip) => (
            <div className="network-page-growth-tip" key={tip.id}>
              <div>
                <strong>{tip.title}</strong>
                <p>{tip.detail} <button type="button" onClick={onEditPage}>Add</button></p>
              </div>
              <button
                type="button"
                aria-label={`Dismiss ${tip.title} suggestion`}
                onClick={() => setDismissedTips((current) => [...current, tip.id])}
              >
                <X size={15} aria-hidden="true" />
              </button>
            </div>
          ))}
        </section>

        <section className="network-page-dashboard-panel network-page-performance-panel">
          <h2>Track performance</h2>
          <p>Grow your page 3x faster by leveraging insights and analytics</p>
          <div className="network-page-metrics">
            {['Page appearances', 'New followers', 'Post impressions', 'Page visitors'].map((metric) => (
              <article key={metric}>
                <strong>0</strong>
                <a href="#network-page-analytics" onClick={(event) => {
                  event.preventDefault()
                  setActiveItem('Analytics')
                }}>{metric}</a>
                <small>N/A</small>
              </article>
            ))}
          </div>
        </section>

        {!isOfferHidden && (
          <section className="network-page-dashboard-panel network-page-premium-panel">
            <div>
              <h2>Try the new Premium AI tools</h2>
              <p>Reactivate Premium and get a discount for 2 months</p>
              <button type="button" onClick={() => setIsOfferHidden(true)}>Claim offer now</button>
            </div>
            <div className="network-page-premium-art" aria-hidden="true">
              <span />
              <span />
              <span />
            </div>
            <button
              className="network-page-premium-menu"
              type="button"
              aria-label="Dismiss Premium offer"
              onClick={() => setIsOfferHidden(true)}
            >
              <MoreVertical size={17} aria-hidden="true" />
            </button>
          </section>
        )}

        <section className="network-page-dashboard-panel network-page-recent-posts">
          <h2>Manage recent posts</h2>
          <p>Manage your page’s content and amplify your reach with boosting. <button type="button" onClick={() => setActiveItem('Page posts')}>Learn more</button></p>
          <div className="network-page-posts-empty-art" aria-hidden="true">
            <div className="network-page-posts-window">
              <span />
              <span />
              <span />
            </div>
            <div className="network-page-posts-person">
              <i />
              <b />
            </div>
            <div className="network-page-posts-desk" />
          </div>
        </section>
          </>
        )}
      </div>
    </section>
  )
}

export default function BannerPageSetup({ storageKey }) {
  const [page, setPage] = useState(() => readPageDraft(storageKey))
  const [organizationPages, setOrganizationPages] = useState([])
  const [selectedPublicPage, setSelectedPublicPage] = useState(null)
  const [isEditingPage, setIsEditingPage] = useState(false)
  const [isCreatePageFormOpen, setIsCreatePageFormOpen] = useState(false)
  const [isDashboardOpen, setIsDashboardOpen] = useState(false)
  const [isAuthorized, setIsAuthorized] = useState(false)
  const [isFollowingPreview, setIsFollowingPreview] = useState(false)
  const [isLoadingPages, setIsLoadingPages] = useState(true)
  const [isSavingPage, setIsSavingPage] = useState(false)
  const [isUpdatingPublicPageId, setIsUpdatingPublicPageId] = useState('')
  const [pageLoadError, setPageLoadError] = useState('')
  const [logoError, setLogoError] = useState('')
  const [saveMessage, setSaveMessage] = useState('')
  const canCreatePage = Boolean(
    page.name.trim()
    && page.publicUrl.trim()
    && page.industry.trim()
    && isAuthorized,
  )
  const cachePageLocally = useCallback((updatedPage) => {
    if (!storageKey) return
    try {
      window.localStorage.setItem(storageKey, JSON.stringify(updatedPage))
    } catch (error) {
      console.warn('Could not cache the organization page locally:', error)
    }
  }, [storageKey])

  useEffect(() => {
    let isCurrent = true

    async function loadOrganizationPages() {
      setIsLoadingPages(true)
      setPageLoadError('')
      try {
        let publicPages = await api.getOrganizationPages()
        if (!Array.isArray(publicPages)) publicPages = []

        let ownedPage = publicPages.find((item) => item.isOwner)
        const legacyPage = storageKey ? readPageDraft(storageKey) : emptyPage
        if (!ownedPage && legacyPage.isCreated && storageKey) {
          try {
            ownedPage = await api.createOrganizationPage({
              name: legacyPage.name,
              publicUrl: legacyPage.publicUrl,
              website: legacyPage.website,
              industry: legacyPage.industry,
              tagline: legacyPage.tagline,
              imageUrl: legacyPage.logo,
              bannerUrl: legacyPage.banner,
            })
            publicPages = [ownedPage, ...publicPages]
          } catch (migrationError) {
            publicPages = await api.getOrganizationPages()
            ownedPage = publicPages.find((item) => item.isOwner)
            if (!ownedPage) throw migrationError
          }
        }

        if (!isCurrent) return
        const mappedPages = publicPages.map(mapOrganizationPage)
        setOrganizationPages(mappedPages)
        if (ownedPage) {
          const mappedOwnedPage = mapOrganizationPage(ownedPage)
          setPage(mappedOwnedPage)
          if (storageKey) {
            try {
              window.localStorage.setItem(storageKey, JSON.stringify(mappedOwnedPage))
            } catch (cacheError) {
              console.warn('Could not cache the organization page locally:', cacheError)
            }
          }
        }
      } catch (error) {
        console.error('Could not load organization pages:', error)
        if (isCurrent) setPageLoadError(error.message || 'Could not load banners. Please try again.')
      } finally {
        if (isCurrent) setIsLoadingPages(false)
      }
    }

    void loadOrganizationPages()
    return () => {
      isCurrent = false
    }
  }, [storageKey])

  function getPagePayload(updatedPage) {
    return {
      name: updatedPage.name,
      publicUrl: updatedPage.publicUrl,
      website: updatedPage.website,
      industry: updatedPage.industry,
      tagline: updatedPage.tagline,
      imageUrl: updatedPage.logo,
      bannerUrl: updatedPage.banner,
    }
  }

  useEffect(() => {
    async function updatePageAfterSubscriptionDecision(event) {
      const pageData = event.detail?.page
      if (pageData?.id) {
        const updatedPage = mapOrganizationPage(pageData)
        setOrganizationPages((currentPages) => currentPages.map((currentPage) => (
          String(currentPage.id) === String(updatedPage.id) ? updatedPage : currentPage
        )))
        setPage((currentPage) => (
          String(currentPage.id) === String(updatedPage.id) ? updatedPage : currentPage
        ))
        setSelectedPublicPage((currentPage) => (
          String(currentPage?.id) === String(updatedPage.id) ? updatedPage : currentPage
        ))
        if (updatedPage.isOwner) cachePageLocally(updatedPage)
        return
      }

      try {
        const refreshedPages = (await api.getOrganizationPages()).map(mapOrganizationPage)
        setOrganizationPages(refreshedPages)
        const ownedPage = refreshedPages.find((currentPage) => currentPage.isOwner)
        if (ownedPage) {
          setPage(ownedPage)
          cachePageLocally(ownedPage)
        }
        setSelectedPublicPage((currentPage) => (
          refreshedPages.find((pageItem) => String(pageItem.id) === String(currentPage?.id))
          || currentPage
        ))
      } catch (error) {
        console.error('Could not refresh page subscription status:', error)
        setPageLoadError(error.message || 'Could not refresh page subscription status.')
      }
    }

    window.addEventListener('truehunt-organization-page-subscription-changed', updatePageAfterSubscriptionDecision)
    return () => {
      window.removeEventListener('truehunt-organization-page-subscription-changed', updatePageAfterSubscriptionDecision)
    }
  }, [cachePageLocally])

  async function updatePublicPageRelationship(publicPage, relationship) {
    if (isUpdatingPublicPageId) return
    setIsUpdatingPublicPageId(String(publicPage.id))
    setPageLoadError('')
    try {
      const updatedPage = relationship === 'follow'
        ? publicPage.isFollowing
          ? await api.unfollowConnectionGroup(publicPage.id)
          : await api.followConnectionGroup(publicPage.id)
        : publicPage.isSubscribed
          ? await api.unsubscribeFromOrganizationPage(publicPage.id)
          : await api.subscribeToOrganizationPage(publicPage.id)
      const mappedPage = mapOrganizationPage(updatedPage)
      setOrganizationPages((currentPages) => currentPages.map((currentPage) => (
        String(currentPage.id) === String(mappedPage.id) ? mappedPage : currentPage
      )))
      setSelectedPublicPage((currentPage) => (
        String(currentPage?.id) === String(mappedPage.id) ? mappedPage : currentPage
      ))
    } catch (error) {
      console.error(`Could not update organization page ${relationship} status:`, error)
      setPageLoadError(error.message || `Could not update the page ${relationship} status. Please try again.`)
    } finally {
      setIsUpdatingPublicPageId('')
    }
  }

  function updateField(field, value) {
    setPage((currentPage) => ({ ...currentPage, [field]: value }))
    setSaveMessage('')
  }

  function handleLogoChange(event) {
    const file = event.target.files?.[0]
    if (!file) return

    if (!file.type.startsWith('image/')) {
      setLogoError('Choose an image file for your logo.')
      event.target.value = ''
      return
    }

    if (file.size > 2 * 1024 * 1024) {
      setLogoError('Logo files must be 2 MB or smaller.')
      event.target.value = ''
      return
    }

    const reader = new FileReader()
    reader.onload = () => {
      updateField('logo', String(reader.result || ''))
      setLogoError('')
    }
    reader.onerror = () => setLogoError('Could not read that logo file. Please try another image.')
    reader.readAsDataURL(file)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!storageKey || isSavingPage) {
      if (!storageKey) setSaveMessage('Sign in to save this page setup to your account.')
      return
    }

    setIsSavingPage(true)
    setSaveMessage('')
    try {
      const savedPage = mapOrganizationPage(await api.createOrganizationPage(getPagePayload(page)))
      cachePageLocally(savedPage)
      setPage(savedPage)
      setOrganizationPages((currentPages) => [savedPage, ...currentPages.filter((currentPage) => currentPage.isOwner !== true)])
      setIsCreatePageFormOpen(false)
    } catch (error) {
      console.error('Could not save the organization page setup:', error)
      setSaveMessage(error.message || 'Could not save this page setup. Please try again.')
    } finally {
      setIsSavingPage(false)
    }
  }

  function handleEditPage() {
    setIsEditingPage(true)
  }

  async function savePageChanges(updatedPage) {
    if (!storageKey) return 'Sign in to save changes to this page.'
    if (!page.id) return 'This page is not saved to the server yet. Refresh and try again.'

    try {
      const savedPage = mapOrganizationPage(await api.updateOrganizationPage(
        page.id,
        getPagePayload(updatedPage),
      ))
      cachePageLocally(savedPage)
      setPage(savedPage)
      setOrganizationPages((currentPages) => currentPages.map((currentPage) => (
        String(currentPage.id) === String(savedPage.id) ? savedPage : currentPage
      )))
      setIsEditingPage(false)
      return ''
    } catch (error) {
      console.error('Could not save organization page changes:', error)
      return error.message || 'Could not save changes. Please try again.'
    }
  }

  async function deactivatePage() {
    if (!storageKey) return 'Sign in to deactivate this page.'
    if (!page.id) return 'This page is not saved to the server yet. Refresh and try again.'

    try {
      await api.deleteOrganizationPage(page.id)
      try {
        window.localStorage.removeItem(storageKey)
      } catch (cacheError) {
        console.warn('Could not remove the cached organization page:', cacheError)
      }
      setPage({ ...emptyPage })
      setOrganizationPages((currentPages) => currentPages.filter(
        (currentPage) => String(currentPage.id) !== String(page.id),
      ))
      setIsEditingPage(false)
      setIsDashboardOpen(false)
      setIsCreatePageFormOpen(false)
      return ''
    } catch (error) {
      console.error('Could not deactivate the organization page:', error)
      return error.message || 'Could not deactivate this page. Please try again.'
    }
  }

  if (selectedPublicPage) {
    return (
      <PublicOrganizationPageView
        page={selectedPublicPage}
        onBack={() => setSelectedPublicPage(null)}
        onPageUpdate={(updatedPage) => {
          setSelectedPublicPage(updatedPage)
          setOrganizationPages((currentPages) => currentPages.map((currentPage) => (
            String(currentPage.id) === String(updatedPage.id) ? updatedPage : currentPage
          )))
        }}
      />
    )
  }

  if (page.isCreated && isEditingPage) {
    return (
      <OrganizationPageEdit
        page={page}
        onSave={savePageChanges}
        onCancel={() => setIsEditingPage(false)}
      />
    )
  }

  if (page.isCreated && isDashboardOpen) {
    return (
      <OrganizationPageDashboard
        page={page}
        onEditPage={handleEditPage}
        onDeactivatePage={deactivatePage}
      />
    )
  }

  if (page.isCreated || !isCreatePageFormOpen) {
    return (
      <section className="network-page-banner-hub" aria-labelledby="network-page-banner-title">
        <header className="network-section-heading">
          <div>
            <span className="network-section-eyebrow">YOUR NETWORK</span>
            <h2 id="network-page-banner-title">Banners</h2>
            <p>Create an organization banner to share posts with your network.</p>
          </div>
          {!page.isCreated && (
            <button
              className="network-group-create-button"
              type="button"
              disabled={isLoadingPages}
              onClick={() => setIsCreatePageFormOpen(true)}
            >
              Create your banner
            </button>
          )}
        </header>
        {pageLoadError && <p className="network-connection-error" role="alert">{pageLoadError}</p>}
        {page.isCreated ? (
          <article className="network-page-banner-card">
            <div className="network-page-banner-card-identity">
              <span className="network-page-banner-card-logo" aria-hidden="true">
                {page.logo
                  ? <img src={page.logo} alt="" />
                  : <UsersRound size={28} />}
              </span>
              <div>
                <strong>{page.name}</strong>
                <span>Created by you</span>
                <small>
                  {page.subscriberCount} {page.subscriberCount === 1 ? 'subscriber' : 'subscribers'}
                </small>
                {getSubscriberNamesPreview(page) && (
                  <small>Subscribers: {getSubscriberNamesPreview(page)}</small>
                )}
              </div>
            </div>
            <button type="button" onClick={() => setIsDashboardOpen(true)}>Manage page</button>
          </article>
        ) : !isLoadingPages && !isCreatePageFormOpen ? (
          <div className="network-page-banner-empty">
            <span className="network-page-banner-card-logo" aria-hidden="true"><UsersRound size={28} /></span>
            <h3>No banners yet</h3>
            <p>Create your banner to share updates with your network.</p>
          </div>
        ) : isLoadingPages ? (
          <p role="status">Loading banners…</p>
        ) : null}
        {organizationPages.some((publicPage) => !publicPage.isOwner) && (
          <section className="network-page-public-list" aria-labelledby="network-page-public-list-title">
            <h3 id="network-page-public-list-title">Pages from your network</h3>
            <div className="network-page-public-list-items">
              {organizationPages.filter((publicPage) => !publicPage.isOwner).map((publicPage) => (
                <article className="network-page-banner-card" key={publicPage.id}>
                  <button
                    className="network-page-banner-card-open"
                    type="button"
                    onClick={() => setSelectedPublicPage(publicPage)}
                  >
                    <span className="network-page-banner-card-identity">
                      <span className="network-page-banner-card-logo" aria-hidden="true">
                        {publicPage.logo
                          ? <img src={publicPage.logo} alt="" />
                          : <UsersRound size={28} />}
                      </span>
                      <span>
                        <strong>{publicPage.name}</strong>
                        <span>Created by {publicPage.ownerName || 'a member'}</span>
                        <small>
                          {publicPage.subscriberCount} {publicPage.subscriberCount === 1 ? 'subscriber' : 'subscribers'}
                        </small>
                      </span>
                    </span>
                  </button>
                  <div className="network-page-public-list-actions">
                    <button
                      type="button"
                      disabled={Boolean(isUpdatingPublicPageId)}
                      onClick={() => void updatePublicPageRelationship(publicPage, 'follow')}
                    >
                      {publicPage.isFollowing ? 'Following' : 'Follow'}
                    </button>
                    <button
                      type="button"
                      disabled={Boolean(isUpdatingPublicPageId) || publicPage.isSubscriptionPending}
                      onClick={() => void updatePublicPageRelationship(publicPage, 'subscribe')}
                    >
                      {publicPage.isSubscribed
                        ? 'Subscribed'
                        : publicPage.isSubscriptionPending
                          ? 'Request pending'
                          : 'Subscribe'}
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </section>
        )}
      </section>
    )
  }

  return (
    <section className="network-page-setup" aria-labelledby="network-page-setup-title">
      <header className="network-page-setup-heading">
        <div>
          <span className="network-section-eyebrow">ORGANIZATION PAGE</span>
          <h2 id="network-page-setup-title">Create a page</h2>
          <p>Introduce your organization to people across your network.</p>
        </div>
        <button type="button" onClick={() => setIsCreatePageFormOpen(false)}>Cancel</button>
      </header>

      <div className="network-page-setup-layout">
        <form className="network-page-setup-form" onSubmit={handleSubmit}>
          <label className="network-page-setup-field">
            <span>Name <strong>*</strong></span>
            <input
              type="text"
              value={page.name}
              onChange={(event) => updateField('name', event.target.value)}
              placeholder="Add your organization's name"
              maxLength={100}
              required
            />
          </label>

          <label className="network-page-setup-field">
            <span>company/ <strong>*</strong></span>
            <div className="network-page-url-input">
              <input
                type="text"
                value={page.publicUrl}
                onChange={(event) => updateField('publicUrl', event.target.value)}
                placeholder="Add your unique LinkedIn address"
                pattern="[A-Za-z0-9][A-Za-z0-9-]{0,61}[A-Za-z0-9]"
                title="Use 2-63 letters, numbers, or hyphens."
                maxLength={63}
                required
              />
            </div>
            <a
              className="network-page-setup-link"
              href="https://www.linkedin.com/help/linkedin/answer/a553761"
              target="_blank"
              rel="noreferrer"
            >
              Learn more about the Page Public URL
            </a>
          </label>

          <label className="network-page-setup-field">
            <span>Website</span>
            <input
              type="text"
              inputMode="url"
              value={page.website}
              onChange={(event) => updateField('website', event.target.value)}
              placeholder="Begin with http://, https:// or www."
              pattern="(https?://)?(www[.])?[A-Za-z0-9-]+([.][A-Za-z0-9-]+)+(/.*)?"
              title="Enter a valid website address."
            />
          </label>

          <label className="network-page-setup-field">
            <span>Industry <strong>*</strong></span>
            <input
              type="text"
              value={page.industry}
              onChange={(event) => updateField('industry', event.target.value)}
              placeholder="ex: Information Services"
              maxLength={100}
              required
            />
          </label>

          <div className="network-page-setup-field">
            <span>Logo</span>
            <label className="network-page-logo-upload">
              <input type="file" accept="image/*" onChange={handleLogoChange} />
              <Upload size={17} aria-hidden="true" />
              <strong>Choose file</strong>
              <small>Upload to see preview</small>
            </label>
            <small className="network-page-field-help">300 × 300px recommended. JPG, JPEG, and PNG supported.</small>
            {logoError && <small className="network-page-field-error" role="alert">{logoError}</small>}
          </div>

          <label className="network-page-setup-field">
            <span>Tagline</span>
            <textarea
              value={page.tagline}
              onChange={(event) => updateField('tagline', event.target.value)}
              placeholder="ex: An information services firm helping small businesses succeed."
              maxLength={120}
              rows={3}
            />
            <span className="network-page-tagline-help">
              <small>Use your tagline to briefly describe what your organization does.</small>
              <small>{page.tagline.length}/120</small>
            </span>
          </label>

          <label className="network-page-authorization">
            <input
              type="checkbox"
              checked={isAuthorized}
              onChange={(event) => {
                setIsAuthorized(event.target.checked)
                setSaveMessage('')
              }}
              required
            />
            <span>
              I verify that I am an authorized representative of this organization and have the right to act on its behalf in the creation and management of this page.
            </span>
          </label>

          <a
            className="network-page-setup-link"
            rel="noreferrer"
          >
            Read the cinema Pages Terms
          </a>

          {saveMessage && (
            <p className="network-page-setup-status" role="status">{saveMessage}</p>
          )}
          <button className="network-page-create-button" type="submit" disabled={!canCreatePage}>
            Create page
          </button>
        </form>

        <aside className="network-page-preview-panel" aria-label="Organization page preview">
          <h3>Page preview <span aria-hidden="true">ⓘ</span></h3>
          <article className="network-page-preview-card">
            <div className="network-page-preview-logo">
              {page.logo
                ? <img src={page.logo} alt={`${page.name || 'Organization'} logo preview`} />
                : (
                  <span className="network-page-preview-logo-placeholder" aria-hidden="true">
                    <i />
                    <i />
                    <i />
                  </span>
                )}
            </div>
            <h4>{page.name.trim() || 'Company name'}</h4>
            <p>{page.tagline.trim() || 'Tagline'}</p>
            <small>{page.industry.trim() || 'Industry'}</small>
            <button
              className={`network-page-preview-follow${isFollowingPreview ? ' is-following' : ''}`}
              type="button"
              aria-pressed={isFollowingPreview}
              onClick={() => setIsFollowingPreview((following) => !following)}
            >
              {isFollowingPreview ? <span aria-hidden="true">✓</span> : <Plus size={14} aria-hidden="true" />}
              {isFollowingPreview ? 'Following' : 'Follow'}
            </button>
          </article>
        </aside>
      </div>
    </section>
  )
}
