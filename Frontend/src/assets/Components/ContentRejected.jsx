import { useRef, useState } from 'react'
import {
  AlertTriangle,
  CheckCircle,
  Home,
  Image as ImageIcon,
  RefreshCw,
  ShieldAlert,
  XCircle,
} from 'lucide-react'
import './ContentRejected.css'

export default function ContentRejected({
  reason = 'Your image contains content that could not be approved.',
  onRetry,
  onHome,
}) {
  const fileRef = useRef(null)
  const [fileName, setFileName] = useState('')

  function handleRetry(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    setFileName(file.name)
    onRetry?.(file)
  }

  return (
    <main className="rejected-page">
      <section className="rejected-card" aria-labelledby="rejected-title">
        <div className="rejected-brand">
          <ShieldAlert size={32} aria-hidden="true" />
          <span>TRUEHUNT</span>
        </div>
        <div className="rejected-icon"><XCircle size={65} aria-hidden="true" /></div>
        <div className="rejected-status">CONTENT REJECTED</div>
        <h1 id="rejected-title">Post Cannot Be Published</h1>
        <p className="rejected-description">
          Our content moderation system detected content that violates TrueHunt community guidelines.
        </p>
        <div className="rejected-warning">
          <AlertTriangle size={22} aria-hidden="true" />
          <div>
            <strong>Reason for rejection</strong>
            <p>{reason}</p>
          </div>
        </div>
        <div className="rejected-info">
          <div className="rejected-info-item">
            <ShieldAlert size={20} aria-hidden="true" />
            <span>Content moderation completed</span>
            <CheckCircle className="success-icon" size={18} aria-hidden="true" />
          </div>
          <div className="rejected-info-item">
            <ImageIcon size={20} aria-hidden="true" />
            <span>Post not published</span>
            <XCircle className="error-icon" size={18} aria-hidden="true" />
          </div>
        </div>
        <div className="rejected-upload">
          <input
            ref={fileRef}
            type="file"
            accept="image/*,video/*"
            hidden
            onChange={handleRetry}
          />
          <button className="retry-button" type="button" onClick={() => fileRef.current?.click()}>
            <RefreshCw size={19} aria-hidden="true" />
            Upload Another File
          </button>
          {fileName && <p className="selected-file">Selected: {fileName}</p>}
        </div>
        <button className="home-button" type="button" onClick={onHome}>
          <Home size={18} aria-hidden="true" />
          Back to Home
        </button>
        <div className="rejected-footer">Need help? Contact TrueHunt Support.</div>
      </section>
    </main>
  )
}
