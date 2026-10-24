import { useEffect, useRef, useState } from 'react'
import './Article.css'
import api from '../../api'

function Article({ profileDetails, profilePreview, hidden, onBack, onPublish }) {
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [media, setMedia] = useState(null)
  const [isAddMenuOpen, setIsAddMenuOpen] = useState(false)
  const fileInputRef = useRef(null)
  const mediaKindRef = useRef('')
  const mediaRef = useRef(null)
  const name = profileDetails.name.trim() || 'Your name'
  const initials = name === 'Your name' ? '?' : name.charAt(0).toUpperCase()

  useEffect(() => {
    mediaRef.current = media
  }, [media])

  useEffect(() => () => {
    if (mediaRef.current) URL.revokeObjectURL(mediaRef.current.url)
  }, [])

  function chooseMedia(kind) {
    const acceptByKind = {
      image: 'image/*',
      video: 'video/*',
      audio: 'audio/*',
      document: '.pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.rtf',
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

    if (mediaRef.current) URL.revokeObjectURL(mediaRef.current.url)
    const nextMedia = {
      file,
      name: file.name,
      url: URL.createObjectURL(file),
      kind: mediaKindRef.current,
    }
    mediaRef.current = nextMedia
    setMedia(nextMedia)
  }

  function removeMedia() {
    if (mediaRef.current) URL.revokeObjectURL(mediaRef.current.url)
    mediaRef.current = null
    setMedia(null)
  }

  function returnToHome() {
    removeMedia()
    setIsAddMenuOpen(false)
    onBack()
  }

  function publishArticle(event) {
    event.preventDefault()
    const articleTitle = title.trim()
    const articleContent = content.trim()
    if (!articleTitle || !articleContent) return

    onPublish({ title: articleTitle, content: articleContent, media })
    setTitle('')
    setContent('')
    mediaRef.current = null
    setMedia(null)
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
        <div className="article-editor-heading">
          <div className="article-author-avatar" aria-hidden="true">
            {profilePreview
              ? <img src={profilePreview} alt="" />
              : initials}
          </div>
          <div>
            <strong>{name}</strong>
            <span>Write an article</span>
          </div>
        </div>

        <section className={`article-cover-area${media ? ' has-media' : ''}`}>
          {media ? (
            <>
              {media.kind === 'image' && <img src={media.url} alt="Article cover" />}
              {media.kind === 'video' && <video src={media.url} controls />}
              {media.kind === 'audio' && <audio src={media.url} controls />}
              {media.kind === 'document' && (
                <span className="article-document-preview">Document added: {media.name}</span>
              )}
              <button className="article-remove-media" type="button" onClick={removeMedia}>
                Remove
              </button>
            </>
          ) : (
            <span className="article-cover-placeholder">Add a cover image, video, audio, or document</span>
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
                {['image', 'video', 'audio', 'document'].map((kind) => (
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
        </section>

        <label className="article-sr-only" htmlFor="article-title">Article title</label>
        <input
          id="article-title"
          className="article-title-input"
          type="text"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Add a title"
          maxLength={180}
          required
        />

        <label className="article-sr-only" htmlFor="article-content">Article content</label>
        <textarea
          id="article-content"
          className="article-content-input"
          value={content}
          onChange={resizeContent}
          placeholder="Tell your story..."
          rows={4}
          required
        />

        <footer className="article-editor-footer">
          <span>Your article will be shared on your Home feed.</span>
          <button
            className="article-publish-button"
            type="submit"
            disabled={!title.trim() || !content.trim()}
          >
            Publish article
          </button>
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
