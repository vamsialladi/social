import { useEffect, useRef, useState } from 'react'
import './Home.css'
import Article from './Article.jsx'

function Home({
  profileDetails,
  profilePreview,
  activeNavigation,
  setActiveNavigation,
  hidden,
  onViewProfile,
}) {
  const [posts, setPosts] = useState([])
  const [isWritingArticle, setIsWritingArticle] = useState(false)
  const [draft, setDraft] = useState('')
  const [draftType, setDraftType] = useState('post')
  const [draftMedia, setDraftMedia] = useState(null)
  const [isComposerOpen, setIsComposerOpen] = useState(false)
  const imageInputRef = useRef(null)
  const videoInputRef = useRef(null)
  const mediaUrls = useRef([])

  const name = profileDetails.name.trim() || 'Your name'
  const initials = name === 'Your name' ? '?' : name.charAt(0).toUpperCase()
  const languages = [
    ...profileDetails.languages.filter((language) => language !== 'Other'),
    ...(profileDetails.otherLanguage ? [profileDetails.otherLanguage] : []),
  ]

  useEffect(() => () => {
    mediaUrls.current.forEach((url) => URL.revokeObjectURL(url))
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

  function publishPost(event) {
    event.preventDefault()
    const content = draft.trim()
    if (!content && !draftMedia) return

    if (draftMedia) mediaUrls.current.push(draftMedia.url)

    setPosts((currentPosts) => [
      {
        id: `${Date.now()}-${currentPosts.length}`,
        content,
        type: draftType,
        media: draftMedia,
        createdAt: new Date().toLocaleString(),
      },
      ...currentPosts,
    ])
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

  function publishArticle(article) {
    setPosts((currentPosts) => [
      {
        id: `${Date.now()}-${currentPosts.length}`,
        ...article,
        type: 'article',
        createdAt: new Date().toLocaleString(),
      },
      ...currentPosts,
    ])
    setIsWritingArticle(false)
  }

  const visiblePosts = posts.filter((post) => (
    activeNavigation === 'opportunities'
      ? post.type === 'opportunity'
      : post.type !== 'opportunity'
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
              <div className="home-avatar home-profile-avatar">
                {profilePreview
                  ? <img src={profilePreview} alt={`${name}'s profile`} />
                  : initials}
              </div>
              <h1>{name}</h1>
              {profileDetails.talents.length > 0 && (
                <p className="home-profile-headline">{profileDetails.talents.join(' · ')}</p>
              )}
              {profileDetails.address && <p>{profileDetails.address}</p>}
              {profileDetails.experience && (
                <p>{profileDetails.experience} years of experience</p>
              )}
              {(languages.length > 0 || profileDetails.workTypes.length > 0) && (
                <div className="home-profile-tags">
                  {[...languages, ...profileDetails.workTypes].map((item) => (
                    <span key={item}>{item}</span>
                  ))}
                </div>
              )}
            </div>
          </section>
        </aside>

        <div className="home-feed-column">
          {activeNavigation === 'home' && (
          <section className="home-card home-composer" aria-label="Create a post">
            <div className="home-composer-top">
              <div className="home-avatar" aria-hidden="true">
                {profilePreview
                  ? <img src={profilePreview} alt="" />
                  : initials}
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
              {visiblePosts.map((post) => (
                <article className="home-card home-post" key={post.id}>
                  <header className="home-post-header">
                    <div className="home-avatar" aria-hidden="true">
                      {profilePreview
                        ? <img src={profilePreview} alt="" />
                        : initials}
                    </div>
                    <div>
                      <strong>{name}</strong>
                      {profileDetails.talents.length > 0 && (
                        <span>{profileDetails.talents.join(' · ')}</span>
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
                </article>
              ))}
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
              <button className="home-promotion-button" type="button">Explore film careers</button>
            </div>
          </section>
        </aside>
      </div>
    </section>
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