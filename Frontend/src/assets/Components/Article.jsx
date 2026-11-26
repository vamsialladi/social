import { useEffect, useRef, useState } from 'react'
import './Article.css'
import api from '../../api'
import MediaSafetyStatus from '../../components/common/MediaSafetyStatus.jsx'
import { getMediaKind, useMediaSafety } from '../../hooks/useMediaSafety.js'

function Article({ hidden, onBack, onPublish }) {
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [media, setMedia] = useState(null)
  const [isAddMenuOpen, setIsAddMenuOpen] = useState(false)
  const [publishError, setPublishError] = useState('')
  const [isPublishing, setIsPublishing] = useState(false)
  const mediaSafety = useMediaSafety(media?.file)
  const isMediaBlocked = Boolean(media?.file && media.kind !== 'audio' && mediaSafety.isBlocked)
  const fileInputRef = useRef(null)
  const mediaKindRef = useRef('')
  const mediaRef = useRef(null)

  useEffect(() => {
    mediaRef.current = media
  }, [media])

  useEffect(() => () => {
    if (mediaRef.current) URL.revokeObjectURL(mediaRef.current.url)
  }, [])

  function chooseMedia(kind) {
    if (!['image', 'video', 'audio'].includes(kind)) {
      setPublishError('Choose an image, video, or audio attachment.')
      setIsAddMenuOpen(false)
      return
    }

    const acceptByKind = {
      image: 'image/*',
      video: 'video/*',
      audio: 'audio/*',
    }
    mediaKindRef.current = kind
    fileInputRef.current.accept = acceptByKind[kind]
    setIsAddMenuOpen(false)
    fileInputRef.current.click()
  }

  function handleMediaChange(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    const detectedKind = getMediaKind(file)
      || (file.type.toLowerCase().startsWith('audio/')
        || /\.(aac|flac|m4a|mp3|oga|ogg|wav|weba)$/i.test(file.name)
        ? 'audio'
        : '')
    if (!detectedKind || detectedKind !== mediaKindRef.current) {
      setPublishError('Choose a supported image, video, or audio file.')
      return
    }

    if (mediaRef.current) URL.revokeObjectURL(mediaRef.current.url)
    const nextMedia = {
      file,
      name: file.name,
      url: URL.createObjectURL(file),
      kind: detectedKind,
    }
    mediaRef.current = nextMedia
    setMedia(nextMedia)
    setPublishError('')
  }

  function removeMedia() {
    if (mediaRef.current) URL.revokeObjectURL(mediaRef.current.url)
    mediaRef.current = null
    setMedia(null)
    setPublishError('')
  }

  function returnToHome() {
    removeMedia()
    setTitle('')
    setContent('')
    setPublishError('')
    setIsAddMenuOpen(false)
    onBack()
  }

  async function publishArticle(event) {
    event.preventDefault()
    const articleTitle = title.trim()
    const articleContent = content.trim()
    if (!articleTitle || !articleContent || isPublishing) return
    if (isMediaBlocked) {
      setPublishError(mediaSafety.error || `Wait for ${media.kind} moderation to finish before publishing.`)
      return
    }

    setIsPublishing(true)
    setPublishError('')
    try {
      await onPublish({ title: articleTitle, content: articleContent, media })
      setTitle('')
      setContent('')
      if (mediaRef.current) URL.revokeObjectURL(mediaRef.current.url)
      mediaRef.current = null
      setMedia(null)
    } catch (error) {
      setPublishError(error instanceof Error ? error.message : 'The article could not be published. Please try again.')
    } finally {
      setIsPublishing(false)
    }
  }

  function resizeContent(event) {
    const textarea = event.currentTarget
    textarea.style.height = 'auto'
    textarea.style.height = `${textarea.scrollHeight}px`
    setContent(textarea.value)
  }

  return (
    <main className="article-page" hidden={hidden}>
      <header className="article-page-header">
        <button className="article-back-button" type="button" onClick={returnToHome}>
          <span aria-hidden="true">←</span> Back to home
        </button>
        <span className="article-brand">TrueHunt</span>
      </header>

      <form className="article-editor" onSubmit={publishArticle}>
        <section className={`article-cover-area${media ? ' has-media' : ''}`}>
          {media ? (
            <>
              {media.kind === 'image' && <img src={media.url} alt="Article cover" />}
              {media.kind === 'video' && <video src={media.url} controls />}
              {media.kind === 'audio' && <audio src={media.url} controls />}
              <button className="article-remove-media" type="button" onClick={removeMedia}>
                Remove
              </button>
            </>
          ) : (
            <span className="article-cover-placeholder">Add a cover image, video, or audio</span>
          )}
          <div className="article-add-control">
            <button
              className="article-add-button"
              type="button"
              aria-haspopup="menu"
              aria-expanded={isAddMenuOpen}
              onClick={() => setIsAddMenuOpen((open) => !open)}
            >
              <span aria-hidden="true">+</span> Add
            </button>
            {isAddMenuOpen && (
              <div className="article-add-menu" role="menu" aria-label="Choose attachment type">
                {['image', 'video', 'audio'].map((kind) => (
                  <button
                    key={kind}
                    type="button"
                    role="menuitem"
                    onClick={() => chooseMedia(kind)}
                  >
                    {kind.charAt(0).toUpperCase() + kind.slice(1)}
                  </button>
                ))}
              </div>
            )}
          </div>
          <input
            ref={fileInputRef}
            className="article-sr-only"
            type="file"
            aria-label="Choose article attachment"
            onChange={handleMediaChange}
          />
          {media?.file && media.kind !== 'audio' && (
            <MediaSafetyStatus
              safety={mediaSafety}
              blockedMessage={`This ${media.kind} has high adult content and cannot be published.`}
              safeMessage="Content safety check passed. Ready to publish."
            />
          )}
          {media?.kind === 'audio' && (
            <p className="media-safety" role="status">
              Audio moderation is unavailable. This audio will be published without a safety scan.
            </p>
          )}
        </section>

        <label className="article-sr-only" htmlFor="article-title">Article title</label>
        <input
          id="article-title"
          className="article-title-input"
          type="text"
          value={title}
          onChange={(event) => {
            setPublishError('')
            setTitle(event.target.value)
          }}
          placeholder="Add a title"
          maxLength={180}
          required
        />

        <label className="article-sr-only" htmlFor="article-content">Article content</label>
        <textarea
          id="article-content"
          className="article-content-input"
          value={content}
          onChange={(event) => {
            setPublishError('')
            resizeContent(event)
          }}
          placeholder="Tell your story..."
          rows={4}
          required
        />

        {publishError && <p className="article-publish-error" role="alert">{publishError}</p>}

        <footer className="article-editor-footer">
          <span>Your article will be shared on your Home feed.</span>
          <div className="article-editor-actions">
            <button
              className="article-cancel-button"
              type="button"
              onClick={returnToHome}
              disabled={isPublishing}
            >
              Cancel
            </button>
            <button
              className="article-publish-button"
              type="submit"
              disabled={isPublishing || isMediaBlocked || !title.trim() || !content.trim()}
            >
              {isPublishing ? 'Publishing…' : 'Publish article'}
            </button>
          </div>
        </footer>
      </form>
      <section className="home-promotion-card article-promotion-card" aria-label="Sponsored movie industry promotion">
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
                const res = await api.clickPromotion("Northstar Films Article Promo")
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
    </main>
  )
}

export default Article
